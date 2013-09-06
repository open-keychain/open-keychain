# OpenPGP Keychain (for Android)

OpenPGP Keychain is a EXPERIMENTAL fork of Android Privacy Guard (APG)

# Contribute

Fork OpenPGP Keychain and do a merge request. I will merge your changes back into the main project.

# Build

## Build with Gradle

1. Have Android SDK "tools", "platform-tools", and "build-tools" directories in your PATH (http://developer.android.com/sdk/index.html)
2. Export ANDROID_HOME pointing to your Android SDK
3. Install gradle
4. Execute ``gradle wrapper`` (http://www.gradle.org/docs/current/userguide/gradle_wrapper.html)
5. Execute ``./gradlew assemble``

## Build with Ant

1. Have Android SDK "tools" directory in your PATH (http://developer.android.com/sdk/index.html)
2. Execute ``android update project -p OpenPGP-Keychain`` and  ``android update project -p libraries/ActionBarSherlock``
3. Execute ``cd OpenPGP-Kechain``, ``ant debug``

## Build with Eclipse

1. File -> Import -> Android -> Existing Android Code Into Workspace, choose "libraries/ActionBarSherlock"
2. File -> Import -> Android -> Existing Android Code Into Workspace, choose "OpenPGP-Keychain"
3. OpenPGP-Kechain can now be build

# Libraries

All JAR-Libraries are provided in this repository under "libs", all Android Library projects are under "libraries".

* ActionBarSherlock to provide an ActionBar for Android < 3.0
* forked Spongy Castle Crypto Lib (Android version of Bouncy Castle)
* android-support-v4.jar: Compatibility Lib
* barcodescanner-android-integration-supportv4.jar: Barcode Scanner Integration

## Build Barcode Scanner Integration

1. Checkout their SVN (see http://code.google.com/p/zxing/source/checkout)
2. Change android-home variable in "build.properties" in the main directory to point to your Android SDK
3. Change directory to android-integration
4. Build using ``ant build``
5. We use "android-integration-supportv4.jar"

On error see: http://code.google.com/p/zxing/issues/detail?id=1207

## Build Spongy Castle

Spongy Castle is the stock Bouncy Castle libraries with a couple of small changes to make it work on Android. OpenPGP-Keychain uses a forked version with some small changes to improve key import speed. These changes have been sent to Bouncy Castle, and Spongy Castle will be used again when they have filtered down.

see
* http://rtyley.github.com/spongycastle/
* https://github.com/ashh87/spongycastle


# Notes

## Eclipse: "GC overhead limit exceeded"

If you have problems starting OpenPGP Kechain from Eclipse, consider increasing the memory limits in eclipse.ini.
See http://docs.oseems.com/general/application/eclipse/fix-gc-overhead-limit-exceeded for more information.

## Generate pressed dashboard icons

1. Open svg file in Inkscape
2. Extensions -> Color -> darker (2 times!)

# Coding Style

## Code
* Indentation: 4 spaces, no tabs
* Maximum line width for code and comments: 100
* Opening braces don't go on their own line
* Field names: Non-public, non-static fields start with m.
* Acronyms are words: Treat acronyms as words in names, yielding !XmlHttpRequest, getUrl(), etc.

See http://source.android.com/source/code-style.html

## XML Eclipse Settings
* XML Maximum line width 999
* XML: Split multiple attributes each on a new line (Eclipse: Properties -> XML -> XML Files -> Editor)
* XML: Indent using spaces with Indention size 4 (Eclipse: Properties -> XML -> XML Files -> Editor)

See http://www.androidpolice.com/2009/11/04/auto-formatting-android-xml-files-with-eclipse/

# Licenses
OpenPGP Kechain is licensed under Apache License v2.

## Libraries
* ActionBarSherlock  
  http://actionbarsherlock.com/  
  Apache License v2

* SpongyCastle  
  https://github.com/rtyley/spongycastle  
  MIT X11 License

* ZXing QRCode Integration  
  http://code.google.com/p/zxing/  
  Apache License v2

* HTMLCleaner  
  http://htmlcleaner.sourceforge.net/  
  BSD License

* HtmlSpanner  
  Apache License v2


## Images
* icon.svg  
  modified version of kgpg_key2_kopete.svgz

* dashboard_manage_keys.svg, dashboard_my_keys.svg, key.svg  
  http://rrze-icon-set.berlios.de/  
  Creative Commons Attribution Share-Alike licence 3.0

* dashboard_decrypt.svg, dashboard_encrypt.svg, dashboard_help.svg  
  http://tango.freedesktop.org/  
  Public Domain

* dashboard_scan_qrcode.svg  
  New creation for OpenPGP Kechain  
  Apache License v2

