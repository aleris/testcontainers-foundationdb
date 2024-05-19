package earth.adi.testcontainers.containers;

import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.SocatContainer;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Constructs a single in memory <a href="https://www.foundationdb.org/">FoundationDB</a> database
 * for testing transactions.
 *
 * <p>Uses foundationdb:7.1.61 by default</p>
 *
 * <p>Other docker images can be used from
 * <a href="https://hub.docker.com/r/foundationdb/foundationdb">foundationdb docker hub</a>.
 * The FoundationDB API version must be aligned with the docker version used
 * (e.g., for 7.1.61 use api version 710).</p>
 *
 * <p>FDB requires the native client libraries be installed separately from the java bindings.
 * Install the libraries before using the java FDB client. Also, it might have issues working
 * on macOS with the java bindings, try using `export DYLD_LIBRARY_PATH=/usr/local/lib` in
 * environment variables after installing FDB clients locally.
 * </p>
 */
@Slf4j
public class FoundationDBContainer extends GenericContainer<FoundationDBContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME =
        DockerImageName.parse("foundationdb/foundationdb");

    private static final String DEFAULT_TAG = "7.1.61";

    private static final int CONTAINER_EXIT_CODE_OK = 0;

    private static final int INTERNAL_PORT = 4500;

    private SocatContainer proxy;
    private int bindPort;

    private final String networkAlias;

    private Path clusterFilePath;

    /**
     * Creates a {@link FoundationDBContainer} with the default version 7.1.61
     */
    public FoundationDBContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * Creates a {@link FoundationDBContainer} with a specific docker image version.
     * @param dockerImageName the docker image with the desired version.
     */
    @SneakyThrows
    public FoundationDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withCreateContainerCmdModifier(cmd ->
            cmd.withName(String.format("testcontainers-fdb-%s", Base58.randomString(8)))
        );

        withEnv("FDB_NETWORKING_MODE", "host");

        withNetwork(Network.newNetwork());
        networkAlias = "fdb-" + Base58.randomString(16);
        withNetworkAliases(networkAlias);

        this.waitStrategy = Wait.forLogMessage(".*FDBD joined cluster.*\\n", 1);
    }

    /**
     * @return the cluster string that can be used for making a connection
     */
    public String getConnectionString() {
        return String.format("docker:docker@%s:%d", getHost(), bindPort);
    }

    /**
     * Returns a temporary file path, containing the cluster connection string, and that can be used by the
     * Foundation DB client to connect to the cluster server. The file is only created if requests. The file is
     * deleted when the container is stopped.
     * @return a Foundation DB cluster file path
     */
    @SneakyThrows
    public String getClusterFilePath() {
        if (clusterFilePath != null) {
            return clusterFilePath.toString();
        }
        clusterFilePath = Files.createTempFile("fdb_", ".cluster");
        Files.write(clusterFilePath, getConnectionString().getBytes(StandardCharsets.UTF_8));
        log.debug("Using cluster file {}", clusterFilePath);
        return clusterFilePath.toString();
    }

    @SneakyThrows
    @Override
    public void doStart() {
        // FDB server port and the port seen by FDB clients must match,
        // otherwise the FDB server will crash with errors like
        // "Assertion pkt.canonicalRemotePort == peerAddress.port failed..."
        proxy =
            new SocatContainer()
                .withNetwork(getNetwork())
                // the real port we want to proxy to FDB,
                // will get the binding port after socat proxy container is starting
                .withExposedPorts(INTERNAL_PORT)
                // unused port, needed for the container to start before mapping the real port
                // to the bind port of FDB
                // cannot use this directly as the mapped port will be known after
                // the proxy container starts
                .withTarget(INTERNAL_PORT + 1, networkAlias)
                .withReuse(this.isShouldBeReused());
        // set so it does not wait for the check on the INTERNAL_PORT also
        proxy.setWaitStrategy(null);
        proxy.start();
        // now socat proxy has started, will use the mapped port as the FDB port,
        // so it will match with the
        // socat exposed port which is "seen" by FDB clients
        bindPort = proxy.getMappedPort(INTERNAL_PORT);
        // FDB server bind port can be set on env before starting the FDB container
        this.addEnv("FDB_PORT", String.valueOf(bindPort));

        // start FDB container, before proxying the bind port with socat
        super.doStart();

        // FDB container has started, so now proxy the real bind port
        proxyBindPort(INTERNAL_PORT, bindPort);
    }

    @SneakyThrows
    private void proxyBindPort(final int listenPort, final int mappedPort) {
        final ExecCreateCmdResponse createCmdResponse = dockerClient
            .execCreateCmd(proxy.getContainerId())
            .withCmd(
                "socat",
                "TCP-LISTEN:" + listenPort + ",fork,reuseaddr",
                "TCP:" + networkAlias + ":" + mappedPort
            )
            .exec();

        final ToStringConsumer stdOutConsumer = new ToStringConsumer();
        final ToStringConsumer stdErrConsumer = new ToStringConsumer();

        try (final FrameConsumerResultCallback callback = new FrameConsumerResultCallback()) {
            callback.addConsumer(OutputFrame.OutputType.STDOUT, stdOutConsumer);
            callback.addConsumer(OutputFrame.OutputType.STDERR, stdErrConsumer);
            dockerClient.execStartCmd(createCmdResponse.getId()).exec(callback);
        }
        final String stdOut = stdOutConsumer.toString(StandardCharsets.UTF_8);
        if (!stdOut.isEmpty()) {
            log.info("{}", stdOut);
        }
        final String stdErr = stdErrConsumer.toString(StandardCharsets.UTF_8);
        if (!stdErr.isEmpty()) {
            final String errorMessage = String
                .format("Error when attempting to bind port with socat: %s", stdErr);
            log.error("{}", errorMessage);
            throw new ProxyInitializationException(errorMessage);
        }
    }

    @Override
    protected void containerIsStarted(final InspectContainerResponse containerInfo) {
        // is faster when not checking if the database is initialized,
        // and this is only necessary if reusing container
        if (isShouldBeReused()) {
            if (!isDatabaseInitialized()) {
                initDatabaseSingleInMemory();
            }
        } else {
            initDatabaseSingleInMemory();
        }
    }

    @SneakyThrows
    @Override
    protected void containerIsStopping(InspectContainerResponse containerInfo) {
        if (clusterFilePath != null) {
            Files.deleteIfExists(clusterFilePath);
        }
        super.containerIsStopping(containerInfo);
    }

    @SneakyThrows
    private boolean isDatabaseInitialized() {
        final String output = runCliExecOutput("status minimal");
        return output.contains("The database is available");
    }

    @SneakyThrows
    private void initDatabaseSingleInMemory() {
        log.debug("Initializing a single in memory database...");
        final String output = runCliExecOutput("configure new single memory");
        if (!output.contains("Database created")) {
            final String errorMessage = String.format(
                "Database not created when attempting to initialize " +
                    "a new single memory database. Output was: %s",
                output
            );
            log.error(errorMessage);
            throw new DatabaseInitializationException(errorMessage);
        }
        log.debug("Initialized successfully with a single in memory database.");
    }

    @SneakyThrows
    private String runCliExecOutput(final String command) {
        final ExecResult execResult = execInContainer("/usr/bin/fdbcli", "--exec", command);
        log.debug("fdbcli output: {}", execResult.getStdout().trim());
        if (execResult.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            final String errorMessage = String.format(
                "Exit code %s when attempting to run fdbcli command %s: %s",
                execResult.getExitCode(),
                command,
                execResult.getStdout()
            );
            log.error(errorMessage);
            throw new DatabaseInitializationException(errorMessage);
        }
        return execResult.getStdout();
    }

    /**
     * Exception thrown when there is an issue initializing the database.
     */
    public static class DatabaseInitializationException extends RuntimeException {
        DatabaseInitializationException(final String errorMessage) {
            super(errorMessage);
        }
    }

    /**
     * Exception thrown when there is an issue initializing the proxy to the database.
     */
    public static class ProxyInitializationException extends RuntimeException {
        ProxyInitializationException(final String errorMessage) {
            super(errorMessage);
        }
    }
}
