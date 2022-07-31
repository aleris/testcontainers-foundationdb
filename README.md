# Testcontainers FoundationDB Module

Helps running [FoundationDB](https://www.foundationdb.org/) using [Testcontainers](https://www.testcontainers.org/).

It's based on the [docker images](https://hub.docker.com/r/foundationdb/foundationdb) provided by FoundationDB
Community.

_Note: This module is INCUBATING. While it is ready for use and operational in the current version, it is possible that
it may receive breaking changes in the future._


## Adding this module to your project dependencies

Add [Testcontainers](https://www.testcontainers.org/quickstart/junit_5_quickstart/) dependency and then add the 
following dependency to your `pom.xml` / `build.gradle` file:

**Maven**
```xml
<dependency>
    <groupId>io.github.aleris</groupId>
    <artifactId>testcontainers-foundationdb</artifactId>
    <version>0.0.3</version>
    <scope>test</scope>
</dependency>
```

**Gradle**
```groovy
testImplementation "io.github.aleris:testcontainers-foundationdb:0.0.3"
```

## Usage example

You can start a FoundationDB container instance from a Java application by using:

```java
try (final FoundationDBContainer foundationDBContainer = new FoundationDBContainer()) {
    foundationDBContainer.start();

    final Path clusterFilePath = Files.createTempFile("fdb", ".cluster");
    Files.write(clusterFilePath, foundationDBContainer.getConnectionString().getBytes(StandardCharsets.UTF_8));

    try (Database db = fdb.open(clusterFilePath.toString())) {
        db.run(tr -> {
            tr.set(Tuple.from("hello").pack(), Tuple.from("world").pack());
            return null;
        });
    }
}
```

To start with a specific version use:

```java
final FoundationDBContainer foundationDBContainer = new FoundationDBContainer(
        DockerImageName.parse("foundationdb/foundationdb:6.3.23")
)
```

See also the tests for other examples.

## Caveats

- FDB requires the native client libraries be installed separately for the java dependency to work. Install the
  libraries before using the java FDB client.
- Also, it might have issues working on newer macOS with the java bindings, try using java 8 and
  `export DYLD_LIBRARY_PATH=/usr/local/lib` in environment variables after installing FDB clients locally.

## Dev

To publish a new version, increment the version in [build.gradle](./build.gradle), set the following:

```sh
export ORG_GRADLE_PROJECT_sonatypeUsername=sonatypeUsername
export ORG_GRADLE_PROJECT_sonatypePassword='sonatypePassword'
```

And then run `sh ./scripts/publish.sh`.
