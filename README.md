# Testcontainers FoundationDB Module

Helps running [FoundationDB](https://www.foundationdb.org/) using Testcontainers.

It's based on the [docker images](https://hub.docker.com/r/foundationdb/foundationdb) provided by FoundationDB
Community.

_Note: This module is INCUBATING. While it is ready for use and operational in the current version, it is possible that
it may receive breaking changes in the future._


## Adding this module to your project dependencies

Add the following dependency to your `pom.xml` / `build.gradle` file:

**Gradle**
```groovy
testImplementation "io.githbu.aleris.testcontainers:foundationdb:{{latest_version}}"
```

**Maven**
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>foundationdb</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
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

## Caveats

- FDB requires the native client libraries be installed separately for the java dependency to work. Install the
  libraries before using the java FDB client.
- Also, it might have issues working on newer macOS with the java bindings, try using java 8 and
  `export DYLD_LIBRARY_PATH=/usr/local/lib` in environment variables after installing FDB clients locally.
