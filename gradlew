#!/bin/sh
set -eu
GRADLE_VERSION=9.2.1
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/labychat-gradle-$GRADLE_VERSION"
DIST="$CACHE_DIR/gradle-$GRADLE_VERSION"
ZIP="$CACHE_DIR/gradle-$GRADLE_VERSION-bin.zip"
if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$CACHE_DIR"
  URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "$URL"
  else
    echo "curl or wget is required to bootstrap Gradle" >&2
    exit 1
  fi
  rm -rf "$DIST"
  unzip -q "$ZIP" -d "$CACHE_DIR"
fi
exec "$DIST/bin/gradle" -p "$BASE_DIR" "$@"
