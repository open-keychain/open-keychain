Icons
=====

A collection of icons representing PGP's implementation in graphical interfaces. Icons are grouped together according to Encryption, Key, and Signature.


Projects
========

* [Mailpile](https://github.com/pagekite/Mailpile) - A fast modern webmail client for Mac, Linux, and Windows
* [OpenKeychain](https://github.com/open-keychain/open-keychain) - An Android app for managing PGP keys
* [GPGTools](https://github.com/GPGTools) - A well designed GUI interface for managing PGP keys on Mac OS


Encryption
==========

### Open Lock

The open lock icon SHOULD be used to represent a message or data that is going to be sent "unencrypted"

![Unncrypted Icon](https://raw.githubusercontent.com/ModernPGP/icons/master/encryption/lock-open.png)


### Closed Lock

The closed lock icon is used to represent data that WAS encrypted but HAS BEEN successfully decrypted or used for outgoing message or data that WILL BE encrypted.

![Encrypted Icon](https://raw.githubusercontent.com/ModernPGP/icons/master/encryption/lock-closed.png)


### Error Lock

The error lock icon represents data that IS encrypted and COULD NOT be decrypted.

![Error Icon](https://raw.githubusercontent.com/ModernPGP/icons/master/encryption/lock-error.png)


Keys
====

### Fingerprint
Use this icon to represent fingrprint of a PGP key

![Fingerprint Icon](https://raw.githubusercontent.com/ModernPGP/icons/master/keys/icon-fingerprint.png)

### Key
Use this icon to represent a PGP key or Key ID 

![Key Icon](https://raw.githubusercontent.com/ModernPGP/icons/master/keys/icon-key.png)


Signatures
==========

Here are the signature state names + description that Mailpile is currently using.


### Invalid
The signature was invalid or bad

![Invalid Signature Icon](https://raw.githubusercontent.com/ModernPGP/icons/master/signatures/signature-invalid-cutout.png)


### Revoked
Watch out, the signature was made with a key that has been revoked- this is not a good thing

![Revoked Signature Icon](https://raw.githubusercontent.com/ModernPGP/icons/master/signatures/signature-revoked-cutout.png)


### Expired
The signature was made with an expired key

![Expired Signature Icon](https://raw.githubusercontent.com/ModernPGP/icons/master/signatures/signature-expired-cutout.png)


### Unknown
The signature was made with an unknown key, so we can not verify it

![Unknown Signature Icon](https://raw.githubusercontent.com/ModernPGP/icons/master/signatures/signature-unknown-cutout.png)


### Unverified
The signature was good but it came from a key that is not verified yet

![Unverified Signature Icon](https://raw.githubusercontent.com/ModernPGP/icons/master/signatures/signature-unverified-cutout.png)


### Verified
The signature was good and came from a verified key, w00t!

![Verified Signature Icon](https://raw.githubusercontent.com/ModernPGP/icons/master/signatures/signature-verified-cutout.png)


### Error & None
The "error" state exists when there was a weird programatic error trying to analyze this signature. The "none" state is what some API's return when there is no signature of a message. There is no icons for either of these states.

