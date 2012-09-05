# APG

This is a fork of Android Privacy Guard (APG)

I will try to reintegrate the various forks and develope a new user interface and API via AIDL and build a new stable version.

# Contribute

Fork APG and do a merge request. I will merge your changes back into the main project.

# Build using Ant

## Command Line

1. Execute "ant -Dsdk.dir=/opt/android-sdk/ release" in the folder org_apg with the appropriate paths. 

## Local.properties

1. Alternatively you could add a file local.properties in org_apg folder with the following lines, altered to your locations of the SDK:

    sdk.dir=/opt/android-sdk

2. execute "ant release"

# Build using Eclipse

1. New -> Android Project -> Create project from existing source, choose com_actionbarsherlock 
2. New -> Android Project -> Create project from existing source, choose org_apg
3. Add com_actionbarsherlock as Android Lib (Properties of org_apg -> Android -> Library -> add)
5. Now APG+ can be build

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

see https://github.com/rtyley/spongycastle

# Notes

## Eclipse: "GC overhead limit exceeded"

If you have problems starting APG from Eclipse, consider increasing the memory limits in eclipse.ini.
See http://docs.oseems.com/general/application/eclipse/fix-gc-overhead-limit-exceeded for more information.

## Generate pressed dashboard icons

1. Open svg file in Inkscape
2. Extensions -> Color -> darker (2 times!)