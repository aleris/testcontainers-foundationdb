# Testcontainers FoundationDB Module

[![Build](https://github.com/aleris/testcontainers-foundationdb/actions/workflows/build-on-push.yml/badge.svg)](https://github.com/aleris/testcontainers-foundationdb/actions/workflows/build-on-push.yml)

Helps running [FoundationDB](https://www.foundationdb.org/) using [Testcontainers](https://www.testcontainers.org/).

It's based on the [docker images](https://hub.docker.com/r/foundationdb/foundationdb) provided by FoundationDB
Community.


## Adding this module to your project dependencies

1. Add Foundation DB java client dependency, for example:

```groovy
implementation("org.foundationdb:fdb-java:7.1.61")
```

```xml
<dependency>
    <groupId>org.foundationdb</groupId>
    <artifactId>fdb-java</artifactId>
    <version>7.1.61</version>
</dependency>
```

Note that the FDB client requires the native client libraries to be installed:
- https://apple.github.io/foundationdb/downloads.html
- https://github.com/apple/foundationdb/releases

2. Add [Testcontainers](https://www.testcontainers.org/quickstart/junit_5_quickstart/) dependency, for example: 

```groovy
testImplementation "org.testcontainers:testcontainers:1.19.8"
```

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
```

3. Finally add the module dependency to your `build.gradle` / `pom.xml` file:

```groovy
testImplementation "earth.adi:testcontainers-foundationdb:1.1.0"
```

```xml
<dependency>
    <groupId>earth.adi</groupId>
    <artifactId>testcontainers-foundationdb</artifactId>
    <version>1.1.0</version>
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
        DockerImageName.parse("foundationdb/foundationdb:7.1.61")
)
```

See also the tests for other examples.


## Caveats

- FDB requires the native client libraries be installed separately for the java dependency to work. Install the
  libraries before using the java FDB client.
- On MacOS, try setting `export DYLD_LIBRARY_PATH=/usr/local/lib` in environment variables 
  after installing FDB clients locally if you encounter issues.


## Releasing
 <details>
    <summary>Details</summary>

```console
~$ cd testcontainers-foundationdb
# Update version in build.gradle.kts
~/testcontainers-foundationdb ./gradlew updateReadmeVersion # updates the version in README.md from build.gradle.kts
~/testcontainers-foundationdb ./gradlew jreleaserConfig # just to double check the configuration
~/testcontainers-foundationdb ./gradlew clean
~/testcontainers-foundationdb ./gradlew publish
~/testcontainers-foundationdb ./gradlew jreleaserFullRelease
```
</details>
