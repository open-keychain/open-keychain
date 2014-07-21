#!/bin/bash

# This script installs a plugin which is necessary to run OpenKeychain's tests
# into the local maven repository, then puts a line to include the -Test
# subproject into settings.gradle

echo "checking jdk runtime.."
if ! java -version 2>&1 | grep OpenJDK; then
    echo "tests will only run on openjdk, see readme for details!" >&2
    return
fi

tmpdir="$(mktemp -d)"
(
  cd "$tmpdir";
  git clone https://github.com/nenick/gradle-android-test-plugin.git
  cd gradle-android-test-plugin
  echo "rootProject.name = 'gradle-android-test-plugin-parent'" > settings.gradle
  echo "include ':gradle-android-test-plugin'" >> settings.gradle
  ./gradlew :gradle-android-test-plugin:install
)
rm -rf "$tmpdir"

echo -n "ok, adding tests to include list.. "
if grep OpenKeychain-Test settings.gradle >/dev/null ; then
    echo " already in."
else
    echo "include ':OpenKeychain-Test'" >> settings.gradle
    echo "ok"
fi
