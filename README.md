# OpenPGP Keychain (for Android)

OpenPGP Keychain is a EXPERIMENTAL fork of Android Privacy Guard (APG)

# Contribute

Fork OpenPGP Keychain and do a merge request. I will merge your changes back into the main project.

# Build

## Build with Ant

1. Have Android SDK "tools" directory in your PATH (http://developer.android.com/sdk/index.html)
2. Change to "OpenPGP-Kechain" directory with ``cd OpenPGP-Kechain``
3. Execute ``android update project -p .`` and  ``android update project -p android-libs/ActionBarSherlock``
4. Execute ``ant debug``

## Build with Eclipse

1. File -> Import -> Android -> Existing Android Code Into Workspace, choose "OpenPGP-Keychain/android-libs/ActionBarSherlock"
2. File -> Import -> Android -> Existing Android Code Into Workspace, choose "OpenPGP-Keychain"
3. OpenPGP-Kechain can now be build

# Libraries

All JAR-Libraries are provided in this repository under "libs", all Android Library projects are under "android-libs".

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

# Security Model

## Basic goals

* Intents without permissions should only work based on user interaction (e.g. click a button in a dialog)

Android primitives to exchange data: Intent, Intent with return values, Send (also an Intent), Content Provider, AIDL

## Possible Permissions

* ACCESS_API: Encrypt/Sign/Decrypt/Create keys without user interaction (intents, remote service), Read key information (not the actual keys)(content provider)
* ACCESS_KEYS: get and import actual public and secret keys (remote service)

## Without Permissions

### Intents
All Intents start with org.sufficientlysecure.keychain.action.

* android.intent.action.VIEW connected to .gpg and .asc files: Import Key and Decrypt
* android.intent.action.SEND connected to all mime types (text/plain and every binary data like files and images): Encrypt and Decrypt
* IMPORT
* IMPORT_FROM_FILE
* IMPORT_FROM_QR_CODE
* IMPORT_FROM_NFC
* SHARE_KEYRING
* SHARE_KEYRING_WITH_QR_CODE
* SHARE_KEYRING_WITH_NFC
* EDIT_KEYRING
* SELECT_PUBLIC_KEYRINGS
* SELECT_SECRET_KEYRING
* ENCRYPT
* ENCRYPT_FILE
* DECRYPT
* DECRYPT_FILE

## With permission ACCESS_API

### Intents

* CREATE_KEYRING
* ENCRYPT_AND_RETURN
* ENCRYPT_STREAM_AND_RETURN
* GENERATE_SIGNATURE_AND_RETURN
* DECRYPT_AND_RETURN
* DECRYPT_STREAM_AND_RETURN

### Broadcast Receiver
On change of database the following broadcast is send.
* DATABASE_CHANGE

### Content Provider

* The whole content provider requires a permission (only read)
* Don't give out blobs (keys can be accessed by ACCESS_KEYS via remote service)
* Make an internal and external content provider (or pathes with <path-permission>)
* Look at android:grantUriPermissions especially for ApgServiceBlobProvider
* Only give out android:readPermission

### ApgApiService (Remote Service)
AIDL service

## With permission ACCESS_KEYS

### ApgKeyService (Remote Service)
AIDL service to access actual private keyring objects

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

