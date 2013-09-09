# OpenPGP Keychain (for Android)

OpenPGP Keychain is a EXPERIMENTAL fork of Android Privacy Guard (APG)

see http://sufficientlysecure.org/keychain


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

# Contribute

Fork OpenPGP Keychain and do a merge request. I will merge your changes back into the main project.

## Development with Eclipse

Android Studio is currently not supported or recommended!

1. File -> Import -> Android -> Existing Android Code Into Workspace, choose "libraries/ActionBarSherlock"
1. File -> Import -> Android -> Existing Android Code Into Workspace, choose "libraries/HtmlTextView"
2. File -> Import -> Android -> Existing Android Code Into Workspace, choose "OpenPGP-Keychain"
3. OpenPGP-Kechain can now be build

# Keychain API

## Basic goals

* Intents invoked by apps that are not registered by Keychain's App API must require user interaction (e.g. click a button in a dialog to actually encrypt!)

## API without registering the app

### Intent Actions
These Intents require user interaction!

* ``android.intent.action.VIEW`` connected to .gpg and .asc files: Import Key and Decrypt
* ``android.intent.action.SEND`` connected to all mime types (text/plain and every binary data like files and images): Encrypt and Decrypt

All Intents start with ``org.sufficientlysecure.keychain.action.``

* ``ENCRYPT``
  * TODO: explain extras (see source)
* ``ENCRYPT_FILE``
* ``DECRYPT``
  * TODO: explain extras (see source)
* ``DECRYPT_FILE``
* ``IMPORT_KEY``
  * Extras: ``keyring_bytes`` (type: ``byte[]``)
  * or Uri in data with file schema
* ``IMPORT_KEY_FROM_QR_CODE``
  * without extras

TODO:
- new intent REGISTER_APP?

## App API
TODO. See Demo App!

# Libraries

All JAR-Libraries are provided in this repository under "libs", all Android Library projects are under "libraries".

* ActionBarSherlock to provide an ActionBar for Android < 3.0
* HtmlTextView for non-crashing TextViews with HTML content
* forked Spongy Castle Crypto Lib (Android version of Bouncy Castle)
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
OpenPGP Kechain is licensed under GPLv3+.
Some parts (older parts and some libraries are Apache License v2, MIT X11 License)

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

