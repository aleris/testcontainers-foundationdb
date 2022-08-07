#!/bin/sh

VERSION=$(sed -n "s/^version *'\([0-9]\.[0-9]\.[0-9]\)'/\1/p" < build.gradle)

echo "Replacing version $VERSION in README.md..."
sed -i -e 's/io\.github\.aleris:testcontainers-foundationdb:[0-9]+\.[0-9]+\.[0-9]+/io\.github\.aleris:testcontainers-foundationdb:'"${VERSION}"'/' README.md
sed -i -e 's/version>[0-9]+\.[0-9]+\.[0-9]+/version>'"${VERSION}"'/' README.md

echo "Publishing $VERSION to sonatype..."
gradle publishToSonatype closeAndReleaseSonatypeStagingRepository

echo "Publish completed"
