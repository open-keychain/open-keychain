# OpenPGP Keychain (for Android)

OpenPGP Keychain is an OpenPGP implementation for Android.
The development began as a fork of Android Privacy Guard (APG).

see http://sufficientlysecure.org/keychain

## How to help the project?

### Translate the application

Translations are managed at Transifex, please contribute there at https://www.transifex.com/projects/p/openpgp-keychain/

### Contribute Code

1. Join the development mailinglist at http://groups.google.com/d/forum/openpgp-keychain-dev
2. Lookout for interesting issues on our issue page at Github: https://github.com/dschuermann/openpgp-keychain/issues
3. Tell us about your plans on the mailinglist
4. Read this README, especially the notes about coding style
5. Fork OpenPGP Keychain and contribute code (the best part ;) )
6. Open a pull request on Github. I will help with occuring problems and merge your changes back into the main project.

I am happy about every code contribution and appreciate your effort to help us developing OpenPGP Keychain!

## Development

Development mailinglist at http://groups.google.com/d/forum/openpgp-keychain-dev

### Build with Gradle

1. Have Android SDK "tools", "platform-tools", and "build-tools" directories in your PATH (http://developer.android.com/sdk/index.html)
2. Open the Android SDK Manager (shell command: ``android``). Expand the Extras directory and install "Android Support Repository"
3. Export ANDROID_HOME pointing to your Android SDK
4. Execute ``./gradlew build``

### Development with Eclipse

Android Studio is currently not supported or recommended!

1. File -> Import -> Android -> Existing Android Code Into Workspace, choose "libraries/ActionBarSherlock"
2. Repeat step 1 with "libraries/HtmlTextView", "libraries/StickyListHeaders/library", "libraries/AndroidBootstrap", "libraries/zxing", "libraries/zxing-android-integration", "OpenPGP-Keychain"
3. Now all required source files are available in Eclipse

## Keychain API

### Intent API
All Intents require user interaction, e.g. to finally encrypt the user needs to press the "Encrypt" button.
To do automatic encryption/decryption/sign/verify use the OpenPGP Remote API.

#### Android Intent actions provided by OpenPGP Keychain:

* ``android.intent.action.VIEW`` connected to .gpg and .asc files: Import Key and Decrypt
* ``android.intent.action.SEND`` connected to all mime types (text/plain and every binary data like files and images): Encrypt and Decrypt

#### OpenPGP Keychain specific Intent actions:

* ``org.sufficientlysecure.keychain.action.ENCRYPT``
  * To encrypt or sign text, use extra ``text`` (type: ``String``)
  * or set data ``Uri`` (``intent.setData()``) pointing to a file
  * Enable ASCII Armor for file encryption (encoding to Radix-64, 33% overhead) by adding the extra ``ascii_armor`` with value ``true``
* ``org.sufficientlysecure.keychain.action.DECRYPT``
  * To decrypt or verify text, use extra ``text`` (type: ``String``)
  * or set data ``Uri`` (``intent.setData()``) pointing to a file
* ``org.sufficientlysecure.keychain.action.IMPORT_KEY``
  * Extras: ``key_bytes`` (type: ``byte[]``)
  * or set data ``Uri`` (``intent.setData()``) pointing to a file
* ``org.sufficientlysecure.keychain.action.IMPORT_KEY_FROM_QR_CODE``
  * without extras, starts Barcode Scanner to get QR Code

### OpenPGP Remote API
To do asyncronous fast encryption/decryption/sign/verify operations bind to the OpenPGP remote service.
The API Demo contains all required AIDL files and a demo activity.

#### Try out the API
Keychain: https://play.google.com/store/apps/details?id=org.sufficientlysecure.keychain  
API Demo: https://play.google.com/store/apps/details?id=org.sufficientlysecure.keychain.demo

#### Design
All apps wanting to use this generic API
just need to include the AIDL files and connect to the service. Other
OpenPGP apps can implement a service based on this AIDL definition.

The API is designed to be as easy as possible to use by apps like
K-9 Mail. The service definition defines
sign/encrypt/signAndEncrypt/decryptAndVerify [1].

As can be seen the apps themselves never need handle key ids directly.
Only user ids (emails) are used to define recipients. If more than one
pub key exists for an email, OpenPGP Keychain will handle the problem by
showing a selection screen.

Also app devs never need to fiddle with private keys. On first
operation, OpenPGP Keychain shows an activity to allow or disallow
access, while also allowing to choose the private key used for this app.
Please try the Demo app out to see how it works [4].

#### Integration
The API is defined as AIDL interfaces in org.openintents.openpgp packge
[2]. All files from [2] needs to be included in the project.

Using the OpenPgpServiceConnection.java [3] you can choose to which
OpenPGP provider you want to connect (other pgp apps can implement the
interfaces). They can be queried as shown in the demo app (see [3] how
to query). If other OpenPGP apps implement the service, no additional
code is required in k9mail per provider. See [3] for a complete example
for integration.

[1] https://github.com/dschuermann/openpgp-keychain/blob/master/OpenPGP-Keychain-API-Demo/src/org/openintents/openpgp/IOpenPgpService.aidl  
[2] https://github.com/dschuermann/openpgp-keychain/tree/master/OpenPGP-Keychain-API-Demo/src/org/openintents/openpgp  
[3] https://github.com/dschuermann/openpgp-keychain/blob/master/OpenPGP-Keychain-API-Demo/src/org/openintents/openpgp/OpenPgpServiceConnection.java  
[3] https://github.com/dschuermann/openpgp-keychain/blob/master/OpenPGP-Keychain-API-Demo/src/org/sufficientlysecure/keychain/demo/OpenPgpProviderActivity.java  
[4] https://play.google.com/store/apps/details?id=org.sufficientlysecure.keychain.demo

## Extended Remote API

TODO

## Libraries



### ZXing Barcode Scanner Android Integration

Classes can be found under "libraries/zxing-android-integration/".

1. Checkout their SVN (see http://code.google.com/p/zxing/source/checkout)
2. Copy all classes from their android-integration folder to our library folder

### ZXing

Classes can be found under "libraries/zxing/".
ZXing classes were extracted from the ZXing library (http://code.google.com/p/zxing/).
Only classes related to QR Code generation are utilized.

### Bouncy Castle

#### Spongy Castle

Spongy Castle is the stock Bouncy Castle libraries with a couple of small changes to make it work on Android. OpenPGP Keychain uses a forked version with some small changes to improve key import speed. These changes have been sent to Bouncy Castle, and Spongy Castle will be used again when they have filtered down.

see
* Spongy Castle: https://github.com/rtyley/spongycastle-old and http://rtyley.github.com/spongycastle/
* Fork: https://github.com/ashh87/spongycastle

#### Bouncy Castle resources

* Repository: https://github.com/bcgit/bc-java
* Issue tracker: http://www.bouncycastle.org/jira/browse/BJA

#### Documentation
* Documentation project at http://www.cryptoworkshop.com/guide/
* Tests in https://github.com/bcgit/bc-java/tree/master/pg/src/test/java/org/bouncycastle/openpgp/test
* Mailinglist Archive at http://bouncy-castle.1462172.n4.nabble.com/Bouncy-Castle-Dev-f1462173.html


## Notes

### Eclipse: "GC overhead limit exceeded"

If you have problems starting OpenPGP Kechain from Eclipse, consider increasing the memory limits in eclipse.ini.
See http://docs.oseems.com/general/application/eclipse/fix-gc-overhead-limit-exceeded for more information.

### Gradle Build System

We try to make our builds as [reproducible/deterministic](https://blog.torproject.org/blog/deterministic-builds-part-one-cyberwar-and-global-compromise) as possible.
This is also a key requirement to be part of F-Droid.
When changing build files or dependencies, respect the following requirements:
- No precompiled libraries. All libraries should be provided as sourcecode in "libraries" folder
- No dependencies from Maven
- Always use a fixed Android Gradle plugin version not a dynamic one, e.g. ``0.7.3`` instead of ``0.7.+``
- Commit the corresponding gradle wrapper version to the repository

TODO:
- include support lib as source
- include Spongy Castle as source
- resolve lint errors (currently abortOnError is false in some build.gradle files of main project and libraries)

## Coding Style

### Code
* Indentation: 4 spaces, no tabs
* Maximum line width for code and comments: 100
* Opening braces don't go on their own line
* Field names: Non-public, non-static fields start with m.
* Acronyms are words: Treat acronyms as words in names, yielding !XmlHttpRequest, getUrl(), etc.

See http://source.android.com/source/code-style.html

### XML Eclipse Settings
* XML Maximum line width 999
* XML: Split multiple attributes each on a new line (Eclipse: Properties -> XML -> XML Files -> Editor)
* XML: Indent using spaces with Indention size 4 (Eclipse: Properties -> XML -> XML Files -> Editor)

See http://www.androidpolice.com/2009/11/04/auto-formatting-android-xml-files-with-eclipse/

## Licenses
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


### Libraries

* SpongyCastle  
  https://github.com/rtyley/spongycastle  
  MIT X11 License

* ActionBarSherlock  
  http://actionbarsherlock.com/  
  Apache License v2

* HtmlTextView  
  https://github.com/dschuermann/html-textview  
  Apache License v2

* ZXing  
  http://code.google.com/p/zxing/  
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

* key.svg  
  http://rrze-icon-set.berlios.de/  
  Creative Commons Attribution Share-Alike licence 3.0


