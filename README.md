# OpenKeychain (for Android)

OpenKeychain is an OpenPGP implementation for Android.
The development began as a fork of Android Privacy Guard (APG).

see http://sufficientlysecure.org/keychain

## How to help the project?

### Translate the application

Translations are managed at Transifex, please contribute there at https://www.transifex.com/projects/p/openpgp-keychain/

### Contribute Code

1. Join the development mailinglist at http://groups.google.com/d/forum/openpgp-keychain-dev
2. Lookout for interesting issues on our issue page at Github: https://github.com/openpgp-keychain/openpgp-keychain/issues
3. Tell us about your plans on the mailinglist
4. Read this README, especially the notes about coding style
5. Fork OpenKeychain and contribute code (the best part ;) )
6. Open a pull request on Github. I will help with occuring problems and merge your changes back into the main project.

I am happy about every code contribution and appreciate your effort to help us developing OpenKeychain!

## Development

Development mailinglist at http://groups.google.com/d/forum/openpgp-keychain-dev

### Build with Gradle

1. Have Android SDK "tools", "platform-tools", and "build-tools" directories in your PATH (http://developer.android.com/sdk/index.html)
2. Open the Android SDK Manager (shell command: ``android``).  
Expand the Tools directory and select "Android SDK Build-tools" newest version.  
Expand the Extras directory and install "Android Support Repository"  
Select everything for the newest SDK
3. Export ANDROID_HOME pointing to your Android SDK
4. Execute ``./gradlew build``
5. You can install the app with ``adb install -r OpenPGP-Keychain/build/apk/OpenPGP-Keychain-debug-unaligned.apk``

### Build API Demo with Gradle

1. Follow 1-3 from above
2. Change to API Demo directory ``cd OpenPGP-Keychain-API``
3. Execute ``./gradlew build``

### Development with Android Studio

I am using the newest [Android Studio](http://developer.android.com/sdk/installing/studio.html) for development. Development with Eclipse is currently not possible because I am using the new [project structure](http://developer.android.com/sdk/installing/studio-tips.html).

1. Clone the project from github
2. From Android Studio: File -> Import Project ->  ...
  * Select the cloned top folder if you want to develop on the main project
  * Select the "OpenPGP-Keychain-API" folder if you want to develop on the API example
3. Import project from external model -> choose Gradle

## Keychain API

### Intent API
All Intents require user interaction, e.g. to finally encrypt the user needs to press the "Encrypt" button.
To do automatic encryption/decryption/sign/verify use the OpenPGP Remote API.

#### Android Intent actions:

* ``android.intent.action.VIEW`` connected to .gpg and .asc files: Import Key and Decrypt
* ``android.intent.action.SEND`` connected to all mime types (text/plain and every binary data like files and images): Encrypt and Decrypt

#### OpenKeychain Intent actions:

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
* ``org.sufficientlysecure.keychain.action.IMPORT_KEY_FROM_KEYSERVER``
  * Extras: ``query`` (type: ``String``)
  * or ``fingerprint`` (type: ``String``)
* ``org.sufficientlysecure.keychain.action.IMPORT_KEY_FROM_QR_CODE``
  * without extras, starts Barcode Scanner to get QR Code
  
#### OpenKeychain special registered Intents:
* ``android.intent.action.VIEW`` with URIs following the ``openpgp4fpr`` schema. For example: ``openpgp4fpr:73EE2314F65FA92EC2390D3A718C070100012282``. This is used in QR Codes, but could also be embedded into your website. (compatible with Monkeysphere's and Guardian Project's QR Codes)
* NFC (``android.nfc.action.NDEF_DISCOVERED``) on mime type ``application/pgp-keys`` (as specified in http://tools.ietf.org/html/rfc3156, section 7)

### OpenPGP Remote API
To do fast encryption/decryption/sign/verify operations without user interaction bind to the OpenPGP remote service.

#### Try out the API
Keychain: https://play.google.com/store/apps/details?id=org.sufficientlysecure.keychain  
API Demo: https://play.google.com/store/apps/details?id=org.sufficientlysecure.keychain.demo

#### Design
All apps wanting to use this generic API
just need to include the AIDL files and connect to the service. Other
OpenPGP apps can implement a service based on this AIDL definition.

The API is designed to be as easy as possible to use by apps like K-9 Mail.
The service definition defines sign, encrypt, signAndEncrypt, decryptAndVerify, and getKeyIds.

As can be seen in the API Demo, the apps themselves never need to handle key ids directly.
You can use user ids (emails) to define recipients.
If more than one public key exists for an email, OpenKeychain will handle the problem by showing a selection screen. Additionally, it is also possible to use key ids.

Also app devs never need to fiddle with private keys.
On first operation, OpenKeychain shows an activity to allow or disallow access, while also allowing to choose the private key used for this app.
Please try the Demo app out to see how it works.

#### Integration
Copy the api library from "libraries/keychain-api-library" to your project and add it as an dependency to your gradle build.
Inspect the ode found in "OpenPGP-Keychain-API" to understand how to use the API.


## Libraries


### ZXing Barcode Scanner Android Integration

Classes can be found under "libraries/zxing-android-integration/".

1. Checkout their SVN (see http://code.google.com/p/zxing/source/checkout)
2. Copy all classes from their android-integration folder to our library folder

### ZXing

Classes can be found under "libraries/zxing/".
ZXing classes were extracted from the ZXing library (https://github.com/zxing/zxing).
Only classes related to QR Code generation are utilized.

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
- No precompiled libraries. All libraries should be provided as sourcecode in "libraries" folder
- No dependencies from Maven (also a soft requirement for inclusion in F-Droid)
- Always use a fixed Android Gradle plugin version not a dynamic one, e.g. ``0.7.3`` instead of ``0.7.+``
- Commit the corresponding gradle wrapper version to the repository

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

* key.svg  
  http://rrze-icon-set.berlios.de/  
  Creative Commons Attribution Share-Alike licence 3.0
  
* Menu icons  
  http://developer.android.com/design/downloads/index.html#action-bar-icon-pack


