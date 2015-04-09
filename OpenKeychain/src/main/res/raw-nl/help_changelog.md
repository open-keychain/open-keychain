
## 3.2beta2

  * Material design
  * Integratie van QR-scanner (nieuwe machtigingen vereist)
  * Sleutelaanmaakwizard verbeterd
  * Probleem met ontbrekende contacten na synchronisatie opgelost
  * Vereist Android 4
  * Nieuw design voor sleutelscherm
  * Cryptovoorkeuren vereenvoudigd, betere selectie van veilige ciphers
  * API: ondertekeningen ontkoppeld, vrije selectie van ondertekeningssleutel, ...
  * Oplossing voor probleem waarbij sommige geldige sleutels weergegeven werden als ingetrokken of verlopen
  * Aanvaard geen ondertekeningen door verlopen of ingetrokken subsleutels
  * Ondersteuning voor Keybase.io in geavanceerde modus


## 3.1.2

  * Oplossing voor exporteren van sleutels naar bestanden (deze keer echt)


## 3.1.1

  * Oplossing voor exporteren van sleutels naar bestanden (ze werden maar gedeeltelijk geschreven)
  * Oplossing voor crash op Android 2.3


## 3.1

  * Oplossing voor crash op Android 5
  * New certify screen
  * Secure Exchange directly from key list (SafeSlinger library)
  * New QR Code program flow
  * Redesigned decrypt screen
  * New icon usage and colors
  * Fix import of secret keys from Symantec Encryption Desktop
  * Subkey IDs on Yubikeys are now checked correctly


## 3.0.1

  * Grote sleutelimportaties worden beter behandeld
  * Subsleutelselectie verbeterd


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

  * Oplossing voor gebroken sleutels in 2.9.1
  * Yubikey-ontsleuteling werkt nu via API


## 2.9.1

  * Split encrypt screen into two
  * Fix key flags handling (now supporting Mailvelope 0.7 keys)
  * Improved passphrase handling
  * Key sharing via SafeSlinger
  * Yubikey: preference to allow other PINs, currently only signing via the OpenPGP API works, not inside of OpenKeychain
  * Fix usage of stripped keys
  * SHA256 as default for compatibility
  * Intent API has changed, see https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * OpenPGP API now handles revoked/expired keys and returns all user ids


## 2.9

  * Oplossing voor crashes geïntroduceerd in v2.8
  * Experimentele ondersteuning voor ECC
  * Experimentele ondersteuning voor Yubikey (alleen ondertekenen met geïmporteerde sleutels)


## 2.8

  * So many bugs have been fixed in this release that we focus on the main new features
  * Key edit: awesome new design, key revocation
  * Key import: awesome new design, secure keyserver connections via hkps, keyserver resolving via DNS SRV records
  * New first time screen
  * New key creation screen: autocompletion of name and email based on your personal Android accounts
  * File encryption: awesome new design, support for encrypting multiple files
  * New icons to show status of key (by Brennan Novak)
  * Important bug fix: Importing of large key collections from a file is now possible
  * Notification showing cached passphrases
  * Keys are connected to Android's contacts

<p>This release wouldn't be possible without the work of Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar</p>

## 2.7

  * Purple! (Dominik, Vincent)
  * New key view design (Dominik, Vincent)
  * New flat Android buttons (Dominik, Vincent)
  * API fixes (Dominik)
  * Keybase.io import (Tim Bray)


## 2.6.1

  * Enkele oplossingen voor regressies


## 2.6

  * Key certifications (thanks to Vincent Breitmoser)
  * Support for GnuPG partial secret keys (thanks to Vincent Breitmoser)
  * New design for signature verification
  * Custom key length (thanks to Greg Witczak)
  * Fix share-functionality from other apps


## 2.5

  * Fix decryption of symmetric OpenPGP messages/files
  * Refactored key edit screen (thanks to Ash Hughes)
  * New modern design for encrypt/decrypt screens
  * OpenPGP API version 3 (multiple api accounts, internal fixes, key lookup)


## 2.4
<p>Thanks to all applicants of Google Summer of Code 2014 who made this release feature rich and bug free!
Besides several small patches, a notable number of patches are made by the following people (in alphabetical order):
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser.</p>

  * New unified key list
  * Colorized key fingerprint
  * Support for keyserver ports
  * Deactivate possibility to generate weak keys
  * Much more internal work on the API
  * Certify user ids
  * Keyserver query based on machine-readable output
  * Lock navigation drawer on tablets
  * Suggestions for emails on creation of keys
  * Search in public key lists
  * And much more improvements and fixes…


## 2.3.1

  * Hotfix voor crash bij upgraden van oude versies


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
  * New public key view
  * Bug fixes for importing of keys
  * Key cross-certification (thanks to Ash Hughes)
  * Handle UTF-8 passwords properly (thanks to Ash Hughes)
  * First version with new languages (thanks to the contributors on Transifex)
  * Sharing of keys via QR Codes fixed and improved
  * Package signature verification for API


## 2.1.1

  * API updates, voorbereiding voor K-9 Mail integratie


## 2.1

  * Veel bugfixes
  * Nieuwe API voor ontwikkelaars
  * PRNG bug fix door Google


## 2.0

  * Complete redesign
  * Share public keys via QR codes, NFC beam
  * Sign keys
  * Upload keys to server
  * Fixes import issues
  * New AIDL API


## 1.0.8

  * Basic keyserver support
  * App2sd
  * More choices for passphrase cache: 1, 2, 4, 8, hours
  * Translations: Norwegian (thanks, Sander Danielsen), Chinese (thanks, Zhang Fredrick)
  * Bugfixes
  * Optimizations


## 1.0.7

  * Probleem met ondertekeningsverificatie van text met achterlopende newline opgelost
  * Meer opties voor wachtwoord cachetijd (20, 40, 60 min.)


## 1.0.6

  * Account adding crash on Froyo fixed
  * Secure file deletion
  * Option to delete key file after import
  * Stream encryption/decryption (gallery, etc.)
  * New options (language, force v3 signatures)
  * Interface changes
  * Bugfixes


## 1.0.5

  * German and Italian translation
  * Much smaller package, due to reduced BC sources
  * New preferences GUI
  * Layout adjustment for localization
  * Signature bugfix


## 1.0.4

  * Een andere crash veroorzaakt door een SDK-bug met de query builder opgelost


## 1.0.3

  * Crashes tijdens versleuteling/ondertekenen en mogelijk sleutelexportatie opgelost


## 1.0.2

  * Filterable key lists
  * Smarter pre-selection of encryption keys
  * New Intent handling for VIEW and SEND, allows files to be encrypted/decrypted out of file managers
  * Fixes and additional features (key preselection) for K-9 Mail, new beta build available


## 1.0.1

  * GMail account lijsten was stuk in 1.0.0, weer opgelost


## 1.0.0

  * K-9 Mail integration, APG supporting beta build of K-9 Mail
  * Support of more file managers (including ASTRO)
  * Slovenian translation
  * New database, much faster, less memory usage
  * Defined Intents and content provider for other apps
  * Bugfixes