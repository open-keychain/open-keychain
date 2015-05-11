[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## 3.2

  * Material-Design
  * QR-Scanner-Integration (benötigt neue Berechtigungen)
  * Schlüsselerzeugungsassistent verbessert
  * Fehlende Kontakte nach Synchronisierung behoben
  * Benötigt Android 4
  * Redesigned key screen
  * Simplify crypto preferences, better selection of secure ciphers
  * API: Detached signatures, free selection of signing key,...
  * Fix: Some valid keys were shown revoked or expired
  * Don't accept signatures by expired or revoked subkeys
  * Keybase.io support in advanced view


## 3.1.2

  * Fix key export to files (now for real)


## 3.1.1

  * Fix key export to files (they were written partially)
  * Absturz unter Android 2.3 behoben


## 3.1

  * Absturz unter Android 5 behoben
  * Neuer Beglaubigungsbildschirm
  * Secure Exchange directly from key list (SafeSlinger library)
  * Neuer Programmablauf für QR-Codes
  * Redesigned decrypt screen
  * Verwendung neuer Symbole und Farben
  * Fix import of secret keys from Symantec Encryption Desktop
  * Subkey IDs on Yubikeys are now checked correctly


## 3.0.1

  * Better handling of large key imports
  * Improved subkey selection


## 3.0

  * Full support for Yubikey signature generation and decryption!
  * Propose installable compatible apps in apps list
  * New design for decryption screens
  * Many fixes for key import, also fixes stripped keys
  * Honor and display key authenticate flags
  * User interface to generate custom keys
  * Fixing user id revocation certificates
  * New cloud search (searches over traditional keyservers and keybase.io)
  * Support for stripping keys inside OpenKeychain


## 2.9.2

  * Fix keys broken in 2.9.1
  * Yubikey decryption now working via API


## 2.9.1

  * Split encrypt screen into two
  * Fix key flags handling (now supporting Mailvelope 0.7 keys)
  * Improved passphrase handling
  * Key sharing via SafeSlinger
  * Yubikey: preference to allow other PINs, currently only signing via the OpenPGP API works, not inside of OpenKeychain
  * Fix usage of stripped keys
  * Standardmäßig SHA256 aufgrund von Kompatibilität
  * Intent API has changed, see https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * OpenPGP API now handles revoked/expired keys and returns all user ids


## 2.9

  * Fixing crashes introduced in v2.8
  * Experimentelle ECC-Unterstützung
  * Experimental Yubikey support (signing-only with imported keys)


## 2.8

  * So many bugs have been fixed in this release that we focus on the main new features
  * Key edit: awesome new design, key revocation
  * Key import: awesome new design, secure keyserver connections via hkps, keyserver resolving via DNS SRV records
  * Neuer Bildschirm bei der ersten Öffnung
  * New key creation screen: autocompletion of name and email based on your personal Android accounts
  * File encryption: awesome new design, support for encrypting multiple files
  * New icons to show status of key (by Brennan Novak)
  * Important bug fix: Importing of large key collections from a file is now possible
  * Notification showing cached passphrases
  * Keys are connected to Android's contacts

This release wouldn't be possible without the work of Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar

## 2.7

  * Lila! (Dominik, Vincent)
  * New key view design (Dominik, Vincent)
  * New flat Android buttons (Dominik, Vincent)
  * API-Korrekturen (Dominik)
  * Import aus keybase.io (Tim Bray)


## 2.6.1

  * Some fixes for regression bugs


## 2.6

  * Key certifications (thanks to Vincent Breitmoser)
  * Support for GnuPG partial secret keys (thanks to Vincent Breitmoser)
  * New design for signature verification
  * Benutzerdefinierte Schlüssellänge (Dank an Greg Witczak)
  * Fix share-functionality from other apps


## 2.5

  * Fix decryption of symmetric OpenPGP messages/files
  * Refactored key edit screen (thanks to Ash Hughes)
  * New modern design for encrypt/decrypt screens
  * OpenPGP API version 3 (multiple api accounts, internal fixes, key lookup)


## 2.4
Thanks to all applicants of Google Summer of Code 2014 who made this release feature rich and bug free!
Besides several small patches, a notable number of patches are made by the following people (in alphabetical order):
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser.

  * New unified key list
  * Colorized key fingerprint
  * Support for keyserver ports
  * Deactivate possibility to generate weak keys
  * Much more internal work on the API
  * Certify user ids
  * Keyserver query based on machine-readable output
  * Lock navigation drawer on tablets
  * Suggestions for emails on creation of keys
  * Suchen in öffentlichen Schlüssellisten
  * And much more improvements and fixes…


## 2.3.1

  * Hotfix for crash when upgrading from old versions


## 2.3

  * Remove unnecessary export of public keys when exporting secret key (thanks to Ash Hughes)
  * Fix setting expiry dates on keys (thanks to Ash Hughes)
  * More internal fixes when editing keys (thanks to Ash Hughes)
  * Querying keyservers directly from the import screen
  * Fix layout and dialog style on Android 2.2-3.0
  * Fix crash on keys with empty user ids
  * Fix crash and empty lists when coming back from signing screen
  * Bouncy Castle (cryptography library) updated from 1.47 to 1.50 and build from source
  * Fix upload of key from signing screen


## 2.2

  * New design with navigation drawer
  * New public key list design
  * Neue Ansicht für öffentliche Schlüssel
  * Bug fixes for importing of keys
  * Key cross-certification (thanks to Ash Hughes)
  * Handle UTF-8 passwords properly (thanks to Ash Hughes)
  * First version with new languages (thanks to the contributors on Transifex)
  * Sharing of keys via QR Codes fixed and improved
  * Package signature verification for API


## 2.1.1

  * API Updates, preparation for K-9 Mail integration


## 2.1

  * Lots of bug fixes
  * Neue API für Entwickler
  * PRNG bug fix by Google


## 2.0

  * Complete redesign
  * Share public keys via QR codes, NFC beam
  * Schlüssel unterschreiben
  * Schlüssel auf den Server hochladen
  * Fixes import issues
  * Neue AIDL-API


## 1.0.8

  * Grundlegende Schlüsselserverunterstützung
  * App2sd
  * More choices for passphrase cache: 1, 2, 4, 8, hours
  * Translations: Norwegian (thanks, Sander Danielsen), Chinese (thanks, Zhang Fredrick)
  * Fehlerbehebungen
  * Optimierungen


## 1.0.7

  * Fixed problem with signature verification of texts with trailing newline
  * More options for passphrase cache time to live (20, 40, 60 mins)


## 1.0.6

  * Account adding crash on Froyo fixed
  * Sichere Dateilöschung
  * Option to delete key file after import
  * Streamverschlüsselung/-entschlüsselung (Galerie, usw.)
  * New options (language, force v3 signatures)
  * Oberflächenänderungen
  * Fehlerbehebungen


## 1.0.5

  * Deutsche und Italienische Übersetzung
  * Much smaller package, due to reduced BC sources
  * Neues Einstellungen-Benutzeroberfläche
  * Layout adjustment for localization
  * Signature bugfix


## 1.0.4

  * Fixed another crash caused by some SDK bug with query builder


## 1.0.3

  * Fixed crashes during encryption/signing and possibly key export


## 1.0.2

  * Filterbare Schlüsselliste
  * Smarter pre-selection of encryption keys
  * New Intent handling for VIEW and SEND, allows files to be encrypted/decrypted out of file managers
  * Fixes and additional features (key preselection) for K-9 Mail, new beta build available


## 1.0.1

  * GMail account listing was broken in 1.0.0, fixed again


## 1.0.0

  * K-9 Mail integration, APG supporting beta build of K-9 Mail
  * Unterstützung von mehr Dateimanagern (einschließlich ASTRO)
  * Slowenische Übersetzung
  * Neue Datenbank, viel schneller, weniger Speicherbelegung
  * Defined Intents and content provider for other apps
  * Fehlerbehebungen