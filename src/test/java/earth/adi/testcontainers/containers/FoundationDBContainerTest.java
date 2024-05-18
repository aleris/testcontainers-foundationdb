package earth.adi.testcontainers.containers;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.tuple.Tuple;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class FoundationDBContainerTest {

    private final FDB fdb = FDB.selectAPIVersion(710);

    @Test void createsClusterFile() {
        final Path clusterFilePath;
        try (final FoundationDBContainer foundationDBContainer = new FoundationDBContainer()) {
            foundationDBContainer.start();

            clusterFilePath = Paths.get(foundationDBContainer.getClusterFilePath());
            assertTrue(Files.exists(clusterFilePath), "Cluster file created container start");
        }
        assertFalse(Files.exists(clusterFilePath), "Cluster file deleted after container stop");
    }

    @SneakyThrows
    @Test
    public void shouldExecuteTransactions() {
        try (final FoundationDBContainer foundationDBContainer = new FoundationDBContainer()) {
            foundationDBContainer.start();

            log.debug("Using connection string {}", foundationDBContainer.getConnectionString());

            try (Database db = fdb.open(foundationDBContainer.getClusterFilePath())) {
                db.run(tr -> {
                    byte[] resultBytes = new byte[0];
                    try {
                        resultBytes = tr.get("bla".getBytes(StandardCharsets.UTF_8)).get(5, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                    assertNull(resultBytes);
                    return null;
                });
            }
            try (Database db = fdb.open(foundationDBContainer.getClusterFilePath())) {
                // Run an operation on the database
                db.run(tr -> {
                    tr.set(Tuple.from("hello").pack(), Tuple.from("world").pack());
                    return null;
                });

                // Get the value of key 'hello' from the database
                String resultValue = db.run(tr -> {
                    byte[] result = tr.get(Tuple.from("hello").pack()).join();
                    return Tuple.fromBytes(result).getString(0);
                });
                assertEquals("world", resultValue);
            }
        }
    }

    @SneakyThrows
    @Test
    public void shouldWorkWithReuse() {
        try (final FoundationDBContainer foundationDBContainer = new FoundationDBContainer().withReuse(true)) {
            foundationDBContainer.start();

            log.debug("Using connection string {}", foundationDBContainer.getConnectionString());

            try (Database db = fdb.open(foundationDBContainer.getClusterFilePath())) {
                db.run(tr -> {
                    tr.set(Tuple.from("hello").pack(), Tuple.from("world").pack());
                    return null;
                });
            }
        } catch (Exception ex) {
            fail(ex);
        }
    }

    @SneakyThrows
    @Test
    public void shouldRunWithSpecificVersion() {
        try (
                final FoundationDBContainer foundationDBContainer = new FoundationDBContainer(
                        DockerImageName.parse("foundationdb/foundationdb:7.1.61")
                )
        ) {
            foundationDBContainer.start();

            log.debug("Using connection string {}", foundationDBContainer.getConnectionString());

            try (Database db = fdb.open(foundationDBContainer.getClusterFilePath())) {
                assertNotNull(db);
                // does not actually work to run a transaction with an older version of the API, as only one version can
                // be selected with FDB.selectAPIVersion for the lifetime of the JVM
            }
        }
    }

    @SneakyThrows
    @Test
    public void example() {
        try (final FoundationDBContainer foundationDBContainer = new FoundationDBContainer()) {
            foundationDBContainer.start();

            final FDB fdb = FDB.selectAPIVersion(710);

            try (final Database db = fdb.open(foundationDBContainer.getClusterFilePath())) {
                db.run(tr -> {
                    tr.set(Tuple.from("hello").pack(), Tuple.from("world").pack());
                    return null;
                });
            }
        }
    }
}
