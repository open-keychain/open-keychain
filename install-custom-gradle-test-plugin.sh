#!/bin/bash

mkdir temp
cd temp

  git clone https://github.com/nenick/gradle-android-test-plugin.git
  cd gradle-android-test-plugin

    echo "rootProject.name = 'gradle-android-test-plugin-parent'" > settings.gradle
    echo "include ':gradle-android-test-plugin'" >> settings.gradle

    ./gradlew :gradle-android-test-plugin:install

  cd ..
cd ..