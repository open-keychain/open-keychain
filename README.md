# APG+

This is a fork of Android Privacy Guard (APG) named APG+

I will try to reintegrate the various forks and build a new stable version, which can be released to the market.

# Contribute

Fork APG+ and do a merge request. I will merge your changes back into the main project.

# Build using Ant

1. Execute "ant -Dsdk.dir=/opt/android-sdk/ -Dndk.dir=/opt/android-ndk/ release" in the folder org_adaway with the appropriate paths. 

1. Alternatively you could add a file local.properties in org_adaway folder with the following lines, altered to your locations of the SDK and NDK:

    sdk.dir=/opt/android-sdk
    ndk.dir=/opt/android-ndk

    and execute "ant release" 

2. Build

APG+ is using ActionBarSherlock to provide backwards compatibility.
ActionBarSherlock

1. New -> Android Project -> Create project from existing source, choose com_actionbarsherlock 

# Build using Eclipse

1. New -> Android Project -> Create project from existing source, choose org_apg
2. Add com_actionbarsherlock as Android Lib (Properties of org_apg -> Android -> Library -> add)
3. Add Java libs (Properties of org_apg -> Java Build Path -> Libraries -> add all libraries from libs folder in org_apg)
4. Now APG+ can be build 

# Libraries

The Libraries are provided in the git repository.

* ActionBarSherlock to provide an ActionBar for Android < 3.0
* Spongy Castle as the main Crypto Lib
* android-support-v4.jar: Compatibility Lib
* android-integration-supportv4.jar: Barcode Scanner Integration

# Build XZing Integration Library

1. Checkout their SVN (see http://code.google.com/p/zxing/source/checkout)
2. Change android-home variable in "build.properties" in the main directory to point to your Android SDK
3. Change directory to android-integration
4. Build using "ant build"
5. We use "android-integration-supportv4.jar"

On error see: http://code.google.com/p/zxing/issues/detail?id=1207

# Build Spongy Castle

see https://github.com/rtyley/spongycastle

# Generate pressed dashboard icons 

1. Open svg in Inkscape
2. Extensions -> Color -> darker (2 times!)