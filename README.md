# APG

This is a fork of Android Privacy Guard (APG)

I will try to reintegrate the various forks and develope a new user interface and API via AIDL and build a new stable version.

# Contribute

Fork APG and do a merge request. I will merge your changes back into the main project.

# Build

## Build with Ant

1. Add a file called local.properties in org_apg folder with the following lines, altered to your locations of the SDK: ``sdk.dir=/opt/android-sdk``
2. execute "ant release"

## Build with Eclipse

1. File -> Import -> Android -> Existing Android Code Into Workspace, choose com_actionbarsherlock 
2. File -> Import -> Android -> Existing Android Code Into Workspace, choose org_apg
3. Add com_actionbarsherlock as Android Lib (Properties of org_apg -> Android -> Library -> add)
5. APG can now be build

# Libraries

The Libraries are provided in the git repository.

* ActionBarSherlock to provide an ActionBar for Android < 3.0
* Spongy Castle Crypto Lib (Android version of Bouncy Castle)
* android-support-v4.jar: Compatibility Lib
* barcodescanner-android-integration-supportv4.jar: Barcode Scanner Integration

## Build Barcode Scanner Integration

1. Checkout their SVN (see http://code.google.com/p/zxing/source/checkout)
2. Change android-home variable in "build.properties" in the main directory to point to your Android SDK
3. Change directory to android-integration
4. Build using "ant build"
5. We use "android-integration-supportv4.jar"

On error see: http://code.google.com/p/zxing/issues/detail?id=1207

## Build Spongy Castle

Spongy Castle is the stock Bouncy Castle libraries with a couple of small changes to make it work on Android.

see http://rtyley.github.com/spongycastle/

# Notes

## Eclipse: "GC overhead limit exceeded"

If you have problems starting APG from Eclipse, consider increasing the memory limits in eclipse.ini.
See http://docs.oseems.com/general/application/eclipse/fix-gc-overhead-limit-exceeded for more information.

## Generate pressed dashboard icons

1. Open svg file in Inkscape
2. Extensions -> Color -> darker (2 times!)

# Security Concept

## Basic goals

* Never (even with permissions) give out actual PGPSecretKey/PGPSecretKeyRing blobs
* Intents without permissions should only work based on user interaction (e.g. click a button in a dialog)

Android primitives to exchange data: Intent, Intent with return values, Send (also an Intent), Content Provider, AIDL

## Intents

### Without permission

* android.intent.action.VIEW connected to .gpg and .asc files: Import Key and Decrypt
* android.intent.action.SEND connected to all mime types (text/plain and every binary data like files and images): Encrypt and Decrypt
* IMPORT
* EDIT_KEY
* SELECT_PUBLIC_KEYS
* SELECT_SECRET_KEY
* ENCRYPT
* ENCRYPT_FILE
* DECRYPT
* DECRYPT_FILE

### With permission

* CREATE_KEY
* ENCRYPT_AND_RETURN
* GENERATE_SIGNATURE
* DECRYPT_AND_RETURN

## Content Provider

* The whole content provider requires a permission (only read)
* Don't give out blobs
* Make an internal and external content provider (or pathes with <path-permission>)
* Look at android:grantUriPermissions especially for ApgServiceBlobProvider
* Only give out android:readPermission

## Remote Service

* The whole service requires a permission

## Resulting permission

* Read key information (not the actual keys)(content provider)
* Encrypt/Sign/Decrypt/Create keys (intents, remote service) without user interaction