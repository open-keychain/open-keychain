# OpenKeychain (for Android)

OpenKeychain is an OpenPGP implementation for Android.  
For a more detailed description and installation instructions go to http://www.openkeychain.org .

### Travis CI Build Status

[![Build Status](https://travis-ci.org/open-keychain/open-keychain.png?branch=master)](https://travis-ci.org/open-keychain/open-keychain)

## How to help the project?

### Translate the application

Translations are managed at Transifex, please contribute there at https://www.transifex.com/projects/p/open-keychain/

### Contribute Code

1. Join the development mailinglist at http://groups.google.com/d/forum/openpgp-keychain-dev
2. Lookout for interesting issues on our issue page at Github: https://github.com/open-keychain/open-keychain/issues
3. Tell us about your plans on the mailinglist
4. Read this README, especially the notes about coding style
5. Fork OpenKeychain and contribute code (the best part ;) )
6. Open a pull request on Github. I will help with occuring problems and merge your changes back into the main project.

I am happy about every code contribution and appreciate your effort to help us developing OpenKeychain!

## Development

Development mailinglist at http://groups.google.com/d/forum/openpgp-keychain-dev

### Build with Gradle

1. Get all external submodules with ``git submodule update --init --recursive``
2. Have Android SDK "tools", "platform-tools", and "build-tools" directories in your PATH (http://developer.android.com/sdk/index.html)
3. Open the Android SDK Manager (shell command: ``android``).  
Expand the Tools directory and select "Android SDK Build-tools (Version 19.1)".  
Expand the Extras directory and install "Android Support Repository"  
Select everything for the newest SDK Platform (API-Level 19)
4. Export ANDROID_HOME pointing to your Android SDK
5. Execute ``./gradlew build``
6. You can install the app with ``adb install -r OpenKeychain/build/outputs/apk/OpenKeychain-debug-unaligned.apk``

### Run Tests
1. Use OpenJDK instead of Oracle JDK
2. Execute ``./prepare-tests.sh``
3. Execute ``./gradlew build``

### Build API Demo with Gradle

1. Follow 1-4 from above
2. The example code is available at https://github.com/open-keychain/api-example
3. Execute ``./gradlew build``

### Development with Android Studio

We are using the newest [Android Studio](http://developer.android.com/sdk/installing/studio.html) for development. Development with Eclipse is currently not possible because we are using the new [project structure](http://developer.android.com/sdk/installing/studio-tips.html).

1. Clone the project from Github
2. From Android Studio: File -> Import Project ->  Select the cloned top folder

## OpenKeychain's API

OpenKeychain provides two APIs, namely the Intent API and the Remote OpenPGP API.
The Intent API can be used without permissions to start OpenKeychain's activities for cryptographic operations, import of keys, etc.
However, it always requires user input, so that no malicious application can use this API without user intervention.  
The Remote OpenPGP API is more sophisticated and allows to to operations without user interaction in the background.
When utilizing this API, OpenKeychain asks the user on first use to grant access for the calling client application.

More technical information and examples about these APIs can be found in the project's wiki:  
* [Intent API](https://github.com/open-keychain/open-keychain/wiki/Intent-API)
* [Remote OpenPGP API](https://github.com/open-keychain/open-keychain/wiki/OpenPGP-API)


## Libraries


### ZXing Barcode Scanner Android Integration

Classes can be found under https://github.com/open-keychain/zxing-android-integration.

1. Copy all classes from https://github.com/zxing/zxing/tree/master/android-integration folder to our git repository.

### ZXing QR-Code Classes

Classes can be found under https://github.com/open-keychain/zxing-qr-code.
All QR Code related classes were extracted from the ZXing library (https://github.com/zxing/zxing).

### Bouncy Castle

#### Spongy Castle

Spongy Castle is the stock Bouncy Castle libraries with a couple of small changes to make it work on Android. OpenKeychain uses a forked version with some small changes. These changes will been sent to Bouncy Castle, and Spongy Castle will be used again when they have filtered down.

see
* Fork: https://github.com/openpgp-keychain/spongycastle
* Spongy Castle: http://rtyley.github.com/spongycastle/

#### Bouncy Castle resources

* Repository: https://github.com/bcgit/bc-java
* Issue tracker: http://www.bouncycastle.org/jira/browse/BJA

#### Documentation
* Documentation project at http://www.cryptoworkshop.com/guide/
* Tests in https://github.com/bcgit/bc-java/tree/master/pg/src/test/java/org/bouncycastle/openpgp/test
* Examples in https://github.com/bcgit/bc-java/tree/master/pg/src/main/java/org/bouncycastle/openpgp/examples
* Mailinglist Archive at http://bouncy-castle.1462172.n4.nabble.com/Bouncy-Castle-Dev-f1462173.html


## Notes

### Gradle Build System

We try to make our builds as [reproducible/deterministic](https://blog.torproject.org/blog/deterministic-builds-part-one-cyberwar-and-global-compromise) as possible.  
When changing build files or dependencies, respect the following requirements:
* No precompiled libraries (you never know what pre-compiled jar files really contain!). All libraries should be forked into the open-keychain Github project and then provided as git submodules in the "extern" folder.
* No dependencies from Maven (also a soft requirement for inclusion in [F-Droid](https://f-droid.org))
* Always use a fixed Android Gradle plugin version not a dynamic one, e.g. ``0.7.3`` instead of ``0.7.+`` (allows offline builds without lookups for new versions, also some minor Android plugin versions had serious issues, i.e. [0.7.2 and 0.8.1](http://tools.android.com/tech-docs/new-build-system))
* Commit the corresponding [Gradle wrapper](http://www.gradle.org/docs/current/userguide/gradle_wrapper.html) to the repository (allows easy building for new contributors without the need to install the required Gradle version using a package manager)
* In order to update the build system to a newer gradle version you need to:
  * Update every build.gradle file with the new gradle version and/or gradle plugin version
    * build.gradle
    * OpenKeychain/build.gradle
    * OpenKeychain-API/build.gradle
    * OpenKeychain-API/example-app/build.gradle
    * OpenKeychain-API/libraries/keychain-api-library/build.gradle
  * run ./gradlew wrapper twice to update gradle and download the new gradle jar file
  * commit the new gradle jar and property files

### Slow Gradle?

* https://www.timroes.de/2013/09/12/speed-up-gradle/
* Disable Lint checking if it is enabled in build.gradle

### Error:Configuration with name 'default' not found.

Gradle project dependencies are missing. Do a ``git submodule init && git submodule update``

### Translations

Translations are hosted on Transifex, which is configured by ".tx/config".

1. To pull newest translations install transifex client (e.g. ``apt-get install transifex-client``)
2. Config Transifex client with "~/.transifexrc"
3. Go into root folder of git repo
4. execute ``tx pull`` (``tx pull -a`` to get all languages)

see http://help.transifex.net/features/client/index.html#user-client

## Coding Style

### Code
* Indentation: 4 spaces, no tabs
* Maximum line width for code and comments: 100
* Opening braces don't go on their own line
* Field names: Non-public, non-static fields start with m.
* Acronyms are words: Treat acronyms as words in names, yielding !XmlHttpRequest, getUrl(), etc.
* Fully Qualify Imports: Do *not* use wildcard-imports such as ``import foo.*;``

The full coding style can be found at http://source.android.com/source/code-style.html

### Automated syntax check with CheckStyle

####Linux
1. Paste the `tools/checkstyle.xml` file to `~/.AndroidStudioPreview/config/codestyles/`
2. Go to Settings > Code Style > Java, select OpenPgpChecker, as well as Code Style > XML and select OpenPgpChecker again.
3. Start code inspection and see the results by selecting Analyze > Inspect Code from Android-Studio or you can directly run checkstyle via cli with `.tools/checkstyle`. Make sure it's executable first.

####Mac OSX
1. Paste the `tools/checkstyle.xml` file to `~/Library/Preferences/AndroidStudioPreview/codestyles`
2. Go to Preferences > Code Style > Java, select OpenPgpChecker, as well as Code Style > XML and select OpenPgpChecker again.
3. Start code inspection and see the results by selecting Analyze > Inspect Code from Android-Studio or you can directly run checkstyle via cli with `.tools/checkstyle`. Make sure it's executable first.

####Windows
1. Paste the `tools/checkstyle.xml` file to `C:\Users\<UserName>\.AndroidStudioPreview\config\codestyles`
2. Go to File > Settings > Code Style > Java, select OpenPgpChecker, as well as Code Style > XML and select OpenPgpChecker again.
3. Start code inspection and see the results by selecting Analyze > Inspect Code from Android-Studio.

## Licenses
OpenKechain is licensed under GPLv3+.
The full license text can be found in the [LICENSE file](https://github.com/open-keychain/open-keychain/blob/master/LICENSE).
Some parts and some libraries are Apache License v2, MIT X11 License (see below).

> This program is free software: you can redistribute it and/or modify
> it under the terms of the GNU General Public License as published by
> the Free Software Foundation, either version 3 of the License, or
> (at your option) any later version.
> 
> This program is distributed in the hope that it will be useful,
> but WITHOUT ANY WARRANTY; without even the implied warranty of
> MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
> GNU General Public License for more details.
> 
> You should have received a copy of the GNU General Public License
> along with this program.  If not, see <http://www.gnu.org/licenses/>.


### Libraries

* SpongyCastle  
  https://github.com/rtyley/spongycastle  
  MIT X11 License

* Android Support Library v4  
  http://developer.android.com/tools/support-library/index.html  
  Apache License v2
  
* Android Support Library v7 'appcompat'  
  http://developer.android.com/tools/support-library/index.html  
  Apache License v2

* HtmlTextView  
  https://github.com/dschuermann/html-textview  
  Apache License v2

* ZXing  
  https://github.com/zxing/zxing  
  Apache License v2
  
* StickyListHeaders  
  https://github.com/emilsjolander/StickyListHeaders  
  Apache License v2
  
* Android-Bootstrap  
  https://github.com/Bearded-Hen/Android-Bootstrap  
  MIT License

### Images
* icon.svg  
  modified version of kgpg_key2_kopete.svgz
  
* Actionbar icons  
  http://developer.android.com/design/downloads/index.html#action-bar-icon-pack
  
* QR Code Actionbar icon  
  https://github.com/openintents/openintents/blob/master/extensions/qrcode_ext/icons/ic_menu_qr_code/ic_menu_qr_code_holo_light/ic_menu_qr_code.svg

* Purple color scheme  
  http://android-holo-colors.com/
