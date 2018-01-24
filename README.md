# OpenKeychain (for Android)

OpenKeychain is an OpenPGP implementation for Android.  
For a more detailed description and installation instructions go to https://www.openkeychain.org .

<a href="https://f-droid.org/repository/browse/?fdid=org.sufficientlysecure.keychain" target="_blank">
<img src=/graphics/get-it-on-f-droid.png alt="Get it on F-Droid" height="80"/></a>
<a href="https://play.google.com/store/apps/details?id=org.sufficientlysecure.keychain" target="_blank">
<img src=/graphics/get-it-on-google-play.png alt="Get it on Google Play" height="80"/></a>

### Branches
* The development of OpenKeychain happens in the "master" branch.
* For every release a new branch, e.g., "3.2-fixes" is created to backport fixes from "master"

### Travis CI Build Status of master branch

[![Build Status](https://travis-ci.org/open-keychain/open-keychain.svg?branch=master)](https://travis-ci.org/open-keychain/open-keychain)

## How to help the project?

### Translate the application

Translations are managed at Transifex, please contribute there at https://www.transifex.com/projects/p/open-keychain/

### Contribute Code

1. Lookout for interesting issues on Github. We have tagged issues were we explicitly like to see contributions: https://github.com/open-keychain/open-keychain/labels/help-wanted
2. Read this README, especially the notes about coding style
3. Fork OpenKeychain and contribute code (the best part :sunglasses: )
4. Open a pull request on Github. We will help with occurring problems and merge your changes back into the main project.
5. PROFIT

### For bigger changes

1. Join the development mailinglist at https://lists.riseup.net/www/subscribe/openkeychain
2. Propose bigger changes and discuss the consequences

I am happy about every code contribution and appreciate your effort to help us developing OpenKeychain!

## Development

Development mailinglist at https://lists.riseup.net/www/subscribe/openkeychain

### Build with Gradle

1. Clone the project from GitHub
2. Get all external submodules with ``git submodule update --init --recursive``
3. Have Android SDK "tools", "platform-tools", and "build-tools" directories in your PATH (http://developer.android.com/sdk/index.html)
4. Open the Android SDK Manager (shell command: ``android``).
Expand the Tools directory and select "Android SDK Build-tools (Version 27.0.3)".
Expand the Extras directory and install "Android Support Library", as well as "Local Maven repository for Support Libraries"
Select SDK Platform for API levels 27.
5. Export ANDROID_HOME pointing to your Android SDK
6. Execute ``./gradlew assembleFdroidDebug``
7. You can install the app with ``adb install -r OpenKeychain/build/outputs/apk/OpenKeychain-fdroid-debug.apk``

The "google" flavor is only used to include donations via Play Store, for development the "fdroid" flavor should be used.

### Run Tests
1. Use OpenJDK instead of Oracle JDK
2. Execute ``./gradlew clean testFdroidDebugUnitTest --continue``

### Run Jacoco Test Coverage
1. Use OpenJDK instead of Oracle JDK
2. Execute ``./gradlew clean testFdroidDebugUnitTest jacocoTestReport``
3. Report is here: OpenKeychain/build/reports/jacoco/jacocoTestReport/html/index.html

### Development with Android Studio

We are using the newest [Android Studio](http://developer.android.com/sdk/installing/studio.html) for development. Development with Eclipse is currently not possible because we are using the new [project structure](http://developer.android.com/sdk/installing/studio-tips.html).

1. Clone the project from Github
2. Get all external submodules with ``git submodule update --init --recursive``
3. From Android Studio: File -> Import Project ->  Select the cloned top folder

## Libraries

### Bouncy Castle

OpenKeychain uses a forked version with some small changes. These changes will been sent to Bouncy Castle.

see
* Fork: https://github.com/open-keychain/bouncycastle

#### Bouncy Castle resources

* Repository: https://github.com/bcgit/bc-java
* Issue tracker: http://www.bouncycastle.org/jira/browse/BJA

#### Documentation
* Documentation project at http://www.cryptoworkshop.com/guide/
* Tests in https://github.com/bcgit/bc-java/tree/master/pg/src/test/java/org/bouncycastle/openpgp/test
* Examples in https://github.com/bcgit/bc-java/tree/master/pg/src/main/java/org/bouncycastle/openpgp/examples
* Mailinglist Archive at http://bouncy-castle.1462172.n4.nabble.com/Bouncy-Castle-Dev-f1462173.html
* Commit changelog of pg subpackage: https://github.com/bcgit/bc-java/commits/master/pg

## Build System

We try to make our builds as [reproducible/deterministic](https://blog.torproject.org/blog/deterministic-builds-part-one-cyberwar-and-global-compromise) as possible.  

#### Update Gradle version
* Always use a fixed Android Gradle plugin version not a dynamic one, e.g. ``0.7.3`` instead of ``0.7.+`` (allows offline builds without lookups for new versions, also some minor Android plugin versions had serious issues, i.e. [0.7.2 and 0.8.1](http://tools.android.com/tech-docs/new-build-system))
* Update every build.gradle file with the new gradle version and/or gradle plugin version
    * build.gradle
    * OpenKeychain/build.gradle
* run ./gradlew wrapper twice to update gradle and download the new gradle jar file
* commit the corresponding [Gradle wrapper](http://www.gradle.org/docs/current/userguide/gradle_wrapper.html) to the repository (allows easy building for new contributors without the need to install the required Gradle version using a package manager)
  
#### Update SDK and Build Tools
* Open build.gradle and change:
```
ext {
    compileSdkVersion = 21
    buildToolsVersion = '21.1.2'
}
```
* Change SDK and Build Tools in git submodules "openkeychain-api-lib" and "openpgp-api-lib" manually. They should also build on their own without the ext variables.

#### Update library
* You can check for library updates with ``./gradlew dependencyUpdates -Drevision=release

#### Add new library
* You can add the library as a Maven dependency or as a git submodule (if patches are required) in the "extern" folder.
* You can get all transitive dependencies with ``./gradlew -q dependencies OpenKeychain:dependencies``
* If added as a Maven dependency, pin the library using [Gradle Witness](https://github.com/WhisperSystems/gradle-witness) (Do ``./gradlew -q calculateChecksums`` for Trust on First Use)
* If added as a git submodule, change the ``compileSdkVersion`` and ``buildToolsVersion`` in build.gradle to use the variables from the root project:
```
android {
    compileSdkVersion rootProject.ext.compileSdkVersion
    buildToolsVersion rootProject.ext.buildToolsVersion
}
```
* You can check for wrong ``compileSdkVersion`` by ``find -name build.gradle | xargs grep compileSdkVersion``

#### Slow Gradle?

* https://www.timroes.de/2013/09/12/speed-up-gradle/
* Disable Lint checking if it is enabled in build.gradle

#### Error:Configuration with name 'default' not found.

Gradle project dependencies are missing. Do a ``git submodule init && git submodule update``

#### Build on Mac OS X fails?

Try exporting JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

## Translations

Translations are hosted on Transifex, which is configured by ".tx/config".

1. To pull newest translations install transifex client (e.g. ``apt-get install transifex-client``)
2. Config Transifex client with "~/.transifexrc"
3. Go into root folder of git repo
4. execute ``tx pull -af --skip``

see http://help.transifex.net/features/client/index.html#user-client

## Coding Style

### Code
* Indentation: 4 spaces, no tabs.
* Maximum line width for code and comments: 100.
* Opening braces don't go on their own line.
* Field names: Non-public, non-static fields start with m.
* Acronyms are words: Treat acronyms as words in names, yielding !XmlHttpRequest, getUrl(), etc.
* Fully Qualify Imports: Do *not* use wildcard-imports such as ``import foo.*;``
* Android Studio warnings should be fixed, or suppressed if they are incorrect.

The full coding style can be found at http://source.android.com/source/code-style.html

### Automated syntax check with CheckStyle

#### Linux
1. Paste the `tools/checkstyle.xml` file to `~/.AndroidStudioPreview/config/codestyles/`
2. Go to Settings > Code Style > Java, select OpenPgpChecker, as well as Code Style > XML and select OpenPgpChecker again.
3. Start code inspection and see the results by selecting Analyze > Inspect Code from Android-Studio or you can directly run checkstyle via cli with `.tools/checkstyle`. Make sure it's executable first.

#### Mac OSX
1. Paste the `tools/checkstyle.xml` file to `~/Library/Preferences/AndroidStudioPreview/codestyles`
2. Go to Preferences > Code Style > Java, select OpenPgpChecker, as well as Code Style > XML and select OpenPgpChecker again.
3. Start code inspection and see the results by selecting Analyze > Inspect Code from Android-Studio or you can directly run checkstyle via cli with `.tools/checkstyle`. Make sure it's executable first.

#### Windows
1. Paste the `tools/checkstyle.xml` file to `C:\Users\<UserName>\.AndroidStudioPreview\config\codestyles`
2. Go to File > Settings > Code Style > Java, select OpenPgpChecker, as well as Code Style > XML and select OpenPgpChecker again.
3. Start code inspection and see the results by selecting Analyze > Inspect Code from Android-Studio.

## Licenses

Copyright 2017 Schürmann & Breitmoser GbR

Licensed under the [GPLv3](https://github.com/open-keychain/open-keychain/blob/HEAD/LICENSE).

Google Play and the Google Play logo are trademarks of Google Inc.
