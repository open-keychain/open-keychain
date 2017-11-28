# Documentation for ProGuard:
#   http://developer.android.com/guide/developing/tools/proguard.html
#   http://proguard.sourceforge.net/

#-dontshrink # shrinking enabled, see below
#-dontobfuscate # obfuscation enabled for one class (see below)
-dontoptimize
-dontpreverify
-keepattributes **
-dontwarn **
-dontnote **

# Rules are defined as negation filters!
# (! = negation filter, ** = all subpackages)
# Keep everything (** {*;}) except...

# * Obfuscate android.support.v7.view.menu.** to fix Samsung Android 4.2 bug
#   https://code.google.com/p/android/issues/detail?id=78377
# * Remove unneeded Bouncy Castle packages to be under 64K limit
#   http://developer.android.com/tools/building/multidex.html
-keep class !android.support.v7.view.menu.**,!org.bouncycastle.crypto.tls.**,!org.bouncycastle.pqc.**,!org.bouncycastle.x509.**,!org.bouncycastle.jcajce.provider.asymmetric.dstu.**,!org.bouncycastle.jcajce.provider.asymmetric.ecgost.**,!org.bouncycastle.jcajce.provider.asymmetric.gost.**,!org.bouncycastle.jcajce.provider.asymmetric.DSTU4145,!org.bouncycastle.jcajce.provider.asymmetric.ECGOST,!org.bouncycastle.jcajce.provider.asymmetric.GOST,!org.bouncycastle.asn1.cmp.**,!org.bouncycastle.asn1.crmf.**,!org.bouncycastle.asn1.dvcs.**,!org.bouncycastle.asn1.smime.**,!org.bouncycastle.asn1.tsp.**,** {*;}
