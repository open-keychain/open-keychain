# Documentation for ProGuard:
#   http://developer.android.com/guide/developing/tools/proguard.html
#   http://proguard.sourceforge.net/

-dontoptimize
-dontpreverify

-dontnote **

-keepattributes **

-keep class org.sufficientlysecure.keychain.** { *; }

# * Obfuscate android.support.v7.view.menu.** to fix Samsung Android 4.2 bug
#   https://code.google.com/p/android/issues/detail?id=78377
# * Disable obfuscation for all other classes
-keepnames class !android.support.v7.view.menu.**, ** { *; }
