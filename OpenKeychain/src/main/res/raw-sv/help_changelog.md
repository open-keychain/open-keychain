[//]: # (NOTERING: Var vänlig och sätt varje mening på sin egen rad, Transifex sätter varje rad i sitt eget fält för översättningar!)

## 5.1
  * Stöd för Ledger Nano S
  * Stöd för Web Key Directory (WKD) sökning
  * Åtgärdat potentiellt API-säkerhetsproblem

## 5.0
  * Förbättrat stöd för Autocrypt

## 4.9

  * Curve25519 stöd
  * Förbättrat stöd för säkerhetstoken

## 4.8

  * Förbättrat stöd för USB-tokens: Gnuk, Nitrokey-modeller, YubiKey 4-modeller
  * Funktion för att hitta platsen för enhetens NFC-läsare

## 4.7

  * Förbättrad import från urklipp
  * Ny nyckelskaparguide för säkerhetstoken
  * Removed password cache "time to live" setting


## 4.6

  * Import your keys using our new Secure Wi-Fi Transfer mechanism


## 4.5

  * Detaljerad beskrivning av säkerhetsproblem
  * Visa keyserverstatus per nyckel
  * Stöd för EdDSA
  * Åtgärda pgp.mit.edu (nytt certifikat)


## 4.4

  * Ny nyckelstatus visar detaljerad information varför en nyckel anses vara osäker eller defekt


## 4.3

  * Bättre stöd för stora nycklar
  * Åtgärdar import av Gpg4win-filer med brutna kodningar


## 4.2

  * Experimental support for Elliptic Curve Encryption with Security Tokens
  * Redesigned key import screen
  * Design improvements to key lists
  * Support for keyserver onion addresses


## 4.1

  * Bättre detektering av e-post och annat innehåll när den öppnas


## 4.0

  * Experimentellt stöd för säkerhetstoken via USB
  * Tillåt lösenord byte av avskalade nycklar


## 3.9

  * Detektering och hantering av textdata
  * Prestandaförbättringar 
  * UI-förbättringar för hantering av säkerhetstoken


## 3.8

  * Gjort om nyckelredigering
  * Välj kom ihåg tid individuellt när du skriver lösenord
  * Facebook nyckelimport


## 3.7

  * Förbättrat stöd för Android 6 (behörigheter, integration i textmarkeringen )
  * API: Version 10


## 3.6

  * Krypterade säkerhetskopior
  * Säkerhetskorrigeringar baserade på extern säkerhetsrevision
  * YubiKey NEO-guide för att skapa nyckel
  * Grundläggande internt MIME-stöd
  * Automatisk nyckelsynkronisering
  * Experimentell funktion: länka nycklar till Github- och Twitter-konton
  * Experimentell funktion: nyckelbekräftelse via fraser
  * Experimentell funktion: mörkt tema
  * API: Version 9


## 3.5

  * Nyckelåterkallelse vid nyckelradering
  * Förbättrade kontroller för osäker kryptografi
  * Fix: Don't close OpenKeychain after first time wizard succeeds
  * API: Version 8


## 3.4

  * Anonym nyckelhämtning över Tor
  * Proxy stöd
  * Better YubiKey error handling


## 3.3

  * Ny dekrypteringsfönster
  * Decryption of multiple files at once
  * Better handling of YubiKey errors


## 3.2

  * Första versionen med fullt YubiKey-stöd tillgängligt från användargränssnittet: Redigera nycklar. binda YubiKey till nycklar,...
  * Materialkonstruktion
  * Integrering av skanning av QR-kod (Nya behörigheter krävs)
  * Förbättrade guiden för skapande av nyckel
  * Åtgärda kontakter som saknas efter synkronisering
  * Kräver Android 4
  * Redesigned key screen
  * Simplify crypto preferences, better selection of secure ciphers
  * API: Detached signatures, free selection of signing key,...
  * Korrigering: Vissa giltiga nycklar visades som återkallade eller förfallna
  * Acceptera inte signaturer av undernycklar som har förfallit eller återkallats
  * Keybase.io-stöd i avancerad vy
  * Metod för att uppdatera alla nycklar på en gång


## 3.1.2

  * Fix key export to files (now for real)


## 3.1.1

  * Fix key export to files (they were written partially)
  * Åtgärda krasch i Android 2.3


## 3.1

  * Åtgärda krasch i Android 5
  Ny certifieringsskärm
  * Secure Exchange directly from key list (SafeSlinger library)
  * New QR Code program flow
  * Redesigned decrypt screen
  * New icon usage and colors
  * Fix import of secret keys from Symantec Encryption Desktop
  * Experimental YubiKey support: Subkey IDs are now checked correctly


## 3.0.1

  * Better handling of large key imports
  * Improved subkey selection


## 3.0

  * Propose installable compatible apps in apps list
  * New design for decryption screens
  * Many fixes for key import, also fixes stripped keys
  * Honor and display key authenticate flags
  * User interface to generate custom keys
  * Fixing user ID revocation certificates
  * Ny moln sökning (sökningar över traditionella nyckelservrar och keybase.io)
  * Support for stripping keys inside OpenKeychain
  * Experimental YubiKey support: Support for signature generation and decryption


## 2.9.2

  Åtgärda trasiga nycklar i 2.9.1
  * Experimental YubiKey support: Decryption now working via API


## 2.9.1

  * Split encrypt screen into two
  * Fix key flags handling (now supporting Mailvelope 0.7 keys)
  * Improved passphrase handling
  * Key sharing via SafeSlinger
  * Experimental YubiKey support: Preference to allow other PINs, currently only signing via the OpenPGP API works, not inside of OpenKeychain
  * Fix usage of stripped keys
  * SHA256 as default for compatibility
  * Intent API has changed, see https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * OpenPGP API hanterar nu återkallade/utgångna nycklar och returnerar alla användar-ids


## 2.9

  * Fixing crashes introduced in v2.8
  * Experimental ECC support
  * Experimental YubiKey support: Only signing with imported keys


## 2.8

  * So many bugs have been fixed in this release that we focus on the main new features
  * Key edit: awesome new design, key revocation
  * Key import: awesome new design, secure keyserver connections via hkps, keyserver resolving via DNS SRV records
  * New first time screen
  * New key creation screen: autocompletion of name and email based on your personal Android accounts
  * Filkryptering: maffig ny design, stöd för kryptering av flera filer
  * Nya ikoner för att visa status för nyckeln (av Brennan Novak)
  * Important bug fix: Importing of large key collections from a file is now possible
  * Notification showing cached passphrases
  * Keys are connected to Android's contacts

This release wouldn't be possible without the work of Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar

## 2.7

  * Purple! (Dominik, Vincent)
  * New key view design (Dominik, Vincent)
  * New flat Android buttons (Dominik, Vincent)
  * API fixes (Dominik)
  * Keybase.io import (Tim Bray)


## 2.6.1

  * Some fixes for regression bugs


## 2.6

  * Key certifications (thanks to Vincent Breitmoser)
  * Support for GnuPG partial secret keys (thanks to Vincent Breitmoser)
  * New design for signature verification
  * Custom key length (thanks to Greg Witczak)
  * Fix share-functionality from other apps


## 2.5

  * Åtgärda dekryptering av symmetriska OpenPGP-meddelanden/filer
  * Refactored key edit screen (thanks to Ash Hughes)
  * Ny modern design för kryptera/dekryptera skärmar
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
  * Sök i offentliga nyckellistor
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
  * New public key view
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
  * New API for developers
  * PRNG bug fix by Google


## 2.0

  * Complete redesign
  * Share public keys via QR codes, NFC beam
  * Sign keys
  * Skicka nycklar till server
  * Fixes import issues
  * New AIDL API


## 1.0.8

  * Basic keyserver support
  * App2sd
  * More choices for passphrase cache: 1, 2, 4, 8, hours
  * Translations: Norwegian Bokmål (thanks, Sander Danielsen), Chinese (thanks, Zhang Fredrick)
  * Bugfixes
  * Optimizations


## 1.0.7

  * Fixed problem with signature verification of texts with trailing newline
  * More options for passphrase cache time to live (20, 40, 60 mins)


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

  * Fixed another crash caused by some SDK bug with query builder


## 1.0.3

  * Fixed crashes during encryption/signing and possibly key export


## 1.0.2

  * Filterable key lists
  * Smarter pre-selection of encryption keys
  * New Intent handling for VIEW and SEND, allows files to be encrypted/decrypted out of file managers
  * Fixes and additional features (key preselection) for K-9 Mail, new beta build available


## 1.0.1

  * GMail account listing was broken in 1.0.0, fixed again


## 1.0.0

  * K-9 Mail integration, APG supporting beta build of K-9 Mail
  * Support of more file managers (including ASTRO)
  * Slovenian translation
  * New database, much faster, less memory usage
  * Defined Intents and content provider for other apps
  * Bugfixes