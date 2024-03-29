[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## 5.7
  * Fixes for Curve25519
  * IDEA cipher is now considered insecure

## 5.6
  * Compatibility with Android 10 and higher
  * Several bug fixes

## 5.5
  * Fikser dekryptering fra utklippstavlen på Android 10

## 5.4
  * Legger til WKD Advanced metoden
  * Legger til COTECH Sikkerhetsnøkkelbutikk

## 5.3
  * Bruk keys.openpgp.org som standard keyserver

## 5.2
  * Forbedret nøkkelimport fra utiklippstavle

## 5.1
  * Støtte for Ledge Nano S
  * Støtte for Web Key Directory (WKD) søk
  * Fikset et mulig API sikkerhets problem

## 5.0
  * Forbedret Autocrypt-støtte

## 4.9

  * Curve25519-støtte
  * Forbedret støtte for sikkerhetssymbol

## 4.8

  * Forbedret støtte for USB nøkler: Gnuk, Nitrokey modeller, YubiKey 4 modeller
  * Funksjon for å finne enhetens NFC-leserplassering

## 4.7

  * Forbedret import fra utklippstavle
  * Ny nøkkelopprettelsesveiviser for sikkerhetssymbol
  * Fjernet passord-cache foreldelsesinnstilling


## 4.6

  * Importer nøklene dine ved bruk av vår nye sikre Wi-Fi  overføringsmekanisme


## 4.5

  * Detaljert beskrivelse av sikkerhetsproblemer
  * Vis nøkkelstatus per nøkkel
  * Støtte for EdDSA
  * Fiks for pgp.mit.edu (nytt sertifikat)


## 4.4

  * New key status displays detailed information why a key is considered insecure or defective


## 4.3

  * Bedre støtte for store nøkler
  * Fix import of Gpg4win files with broken encodings


## 4.2

  * Eksperimentell støtte for elliptisk kurve-kryptering med sikkerhetssymboler
  * Ny design for nøkkelimporteringsgrensesnittet
  * Designforbedringer av nøkkellister
  * Støtte for nøkkeltjener på løk-adresse


## 4.1

  * Bedre oppdaging av e-poster og annet innhold når åpnet


## 4.0

  * Eksperimentell støtte for sikkerhetssymboler over USB
  * Allow password changing of stripped keys


## 3.9

  * Detection and handling of text data
  * Ytelsesforbedringer
  * Brukergrensesnittforbedringer for behandling av sikkerhetssymboler


## 3.8

  * Re-designet nøkkelredigering
  * Choose remember time individually when entering passwords
  * Nøkkelimport fra Facebook


## 3.7

  * Forbedret Android 6 støtte (tillatelser, ingegrering i tekstfeltet)
  * API: Versjon 10


## 3.6

  * Krypterte sikkerhetskopier
  * Sikkerhetsutbedringer basert på eksterne sikkerhetsgjennomganger
  * YubiKey NEO key creation wizard
  * Grunnleggende intern MIME-støtte
  * Automatisk nøkkelsynkronisering
  * Experimental feature: link keys to Github, Twitter accounts
  * Eksperimentell funksjon: nøkkelbekreftelse via fraser
  * Eksperimentell funksjon: mørk drakt
  * API: Versjon 9


## 3.5

  * Key revocation on key deletion
  * Forbedret sjekking av usikker kryptografi
  * Fix: Ikke lukke OpenKeychain etter at førstegangsveiviseren fullføres
  * API: Versjon 8


## 3.4

  * Anonym nøkkel-nedlasting over Tor
  * Mellomtjener-støtte
  * Better YubiKey error handling


## 3.3

  * Ny dekrypteringsoversikt
  * Dekryptering av flere filer samtidig
  * Bedre håndtering av YubiKey-feil


## 3.2

  * First version with full YubiKey support available from the user interface: Edit keys, bind YubiKey to keys,...
  * Materielt design
  * Integrasjon av QR-kodeskanning (Nye tilganger kreves)
  * Forbedret veiviser for nøkkelopprettelse
  * Fikser manglende kontakter etter synkronisering
  * Krever Android 4
  * Re-designet nøkkel-oversikt
  * Simplify crypto preferences, better selection of secure ciphers
  * API: Detached signatures, free selection of signing key,...
  * Fix: Some valid keys were shown revoked or expired
  * Don't accept signatures by expired or revoked subkeys
  * Keybase.io support in advanced view
  * Metode for å oppdatere alle nøklene på én gang


## 3.1.2

  * Fix key export to files (now for real)


## 3.1.1

  * Fix key export to files (they were written partially)
  * Fikser kræsj på Android 2.3


## 3.1

  * Fikset kræsj på Android 5
  * Ny bekreftelsesskjerm
  * Secure Exchange directly from key list (SafeSlinger library)
  * New QR Code program flow
  * Nytt design for dekrypteringsoversikt
  * Ny ikonbruk og farger
  * Fix import of secret keys from Symantec Encryption Desktop
  * Experimental YubiKey support: Subkey IDs are now checked correctly


## 3.0.1

  * Bedre håndtering av store nøkkelimporter
  * Improved subkey selection


## 3.0

  * Propose installable compatible apps in apps list
  * New design for decryption screens
  * Many fixes for key import, also fixes stripped keys
  * Honor and display key authenticate flags
  * User interface to generate custom keys
  * Fixing user ID revocation certificates
  * New cloud search (searches over traditional keyservers and keybase.io)
  * Support for stripping keys inside OpenKeychain
  * Experimental YubiKey support: Support for signature generation and decryption


## 2.9.2

  * Fix keys broken in 2.9.1
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
  * OpenPGP API now handles revoked/expired keys and returns all user ids


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
  * File encryption: awesome new design, support for encrypting multiple files
  * New icons to show status of key (by Brennan Novak)
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
  * Search in public key lists
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
  * Første versjon med de nye språkene (takket være bidragsytere på Transifex)
  * Sharing of keys via QR Codes fixed and improved
  * Package signature verification for API


## 2.1.1

  * API Updates, preparation for K-9 Mail integration


## 2.1

  * Lots of bug fixes
  * New API for developers
  * PRNG bug fix by Google


## 2.0

  * Fullstendig redesign
  * Del offentlige nøkler via QR-koder eller NFC
  * Signer nøkler
  * Last opp nøkler til tjener
  * Fikser importproblemer
  * New AIDL API


## 1.0.8

  * Enkel nøkkeltjener-støtte
  * App2sd
  * More choices for passphrase cache: 1, 2, 4, 8, hours
  * Translations: Norwegian Bokmål (thanks, Sander Danielsen), Chinese (thanks, Zhang Fredrick)
  * Feilretting
  * Optimering


## 1.0.7

  * Fixed problem with signature verification of texts with trailing newline
  * More options for passphrase cache time to live (20, 40, 60 mins)


## 1.0.6

  * Account adding crash on Froyo fixed
  * Sikker filsletting
  * Valg for sletting av nøkkelfil etter import
  * Stream encryption/decryption (gallery, etc.)
  * New options (language, force v3 signatures)
  * Grensesnittsendringer
  * Feilretting


## 1.0.5

  * Tysk og Italiensk oversettelse ved
  * Much smaller package, due to reduced BC sources
  * Nytt grafisk grensesnitt for innstillinger
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
  * Slovensk oversettelse
  * Ny database, mye raskere, mindre minnebruk
  * Defined Intents and content provider for other apps
  * Feilretting