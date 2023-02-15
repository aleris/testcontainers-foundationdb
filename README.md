# Testcontainers FoundationDB Module

[![ci](https://github.com/aleris/testcontainers-foundationdb/actions/workflows/ci.yml/badge.svg)](https://github.com/aleris/testcontainers-foundationdb/actions/workflows/ci.yml)

Helps running [FoundationDB](https://www.foundationdb.org/) using [Testcontainers](https://www.testcontainers.org/).

It's based on the [docker images](https://hub.docker.com/r/foundationdb/foundationdb) provided by FoundationDB
Community.

## Adding this module to your project dependencies

1. Add Foundation DB java client dependency, for example:

```groovy
implementation("org.foundationdb:fdb-java:7.1.23")
```

```xml
<dependency>
    <groupId>org.foundationdb</groupId>
    <artifactId>fdb-java</artifactId>
    <version>7.1.23</version>
</dependency>
```

Note that the FDB client requires the native client libraries to be installed:
- https://apple.github.io/foundationdb/downloads.html
- https://github.com/apple/foundationdb/releases

2. Add [Testcontainers](https://www.testcontainers.org/quickstart/junit_5_quickstart/) dependency, for example: 

```groovy
testImplementation "org.testcontainers:testcontainers:1.17.6"
```

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.17.6</version>
    <scope>test</scope>
</dependency>
```

3. Finally add the module dependency to your `build.gradle` / `pom.xml` file:

```groovy
testImplementation "io.github.aleris:testcontainers-foundationdb:1.0.0"
```

```xml
<dependency>
    <groupId>io.github.aleris</groupId>
    <artifactId>testcontainers-foundationdb</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```


## Usage example

You can start a FoundationDB container instance from a Java application by using:

```java
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
```

To start with a specific version use:

```java
final FoundationDBContainer foundationDBContainer = new FoundationDBContainer(
        DockerImageName.parse("foundationdb/foundationdb:7.1.23")
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
