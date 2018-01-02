# Documentation for ProGuard:
#   http://developer.android.com/guide/developing/tools/proguard.html
#   http://proguard.sourceforge.net/

-dontoptimize
-dontpreverify

-dontnote **

-keepattributes **

-keep class org.sufficientlysecure.keychain.** { *; }
