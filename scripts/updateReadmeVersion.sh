#!/bin/sh

# Replace the version in README.md with the version from build.gradle.kts
# Usage:
#   ./scripts/updateReadmeVersion.sh

SED="sed"
if ! command -v gsed &> /dev/null
then
  # brew install gnu-sed
  SED="gsed"
fi

VERSION=$(SED -n "s/^version\s*'\([0-9]\.[0-9]\.[0-9]\)'/\1/p" < build.gradle)

echo "Updating to version $VERSION in README.md..."
SED -i -E 's/earth.adi:testcontainers-foundationdb:[0-9]+\.[0-9]+\.[0-9]+/earth.adi:testcontainers-foundationdb:'"${VERSION}"'/' README.md
sed -i -E 's|testcontainers-foundationdb</artifactId>\n    <version>[0-9]+\.[0-9]+\.[0-9]+|testcontainers-foundationdb</artifactId>\n    <version>'"${VERSION}"'|' README.md
