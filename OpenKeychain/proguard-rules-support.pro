# Obfuscate android.support.v7.view.menu.** to fix Samsung Android 4.2 bug
# https://code.google.com/p/android/issues/detail?id=78377
-keepnames class !android.support.v7.view.menu.**, ** { *; }

-keep class android.support.v7.widget.SearchView { *; }
