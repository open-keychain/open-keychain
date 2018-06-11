[//]: # (NOTA: Si prega di mettere ogni frase in una propria linea, Transifex mette ogni riga nel proprio campo di traduzione!)

## 5.1
  * Support for Ledger Nano S
  * Support Web Key Directory (WKD) search
  * Fixed potential API security issue

## 5.0
  * Improved Autocrypt support

## 4.9

  * Curve25519 support
  * Improved support for security tokens

## 4.8

  * Improved support for USB tokens: Gnuk, Nitrokey models, YubiKey 4 models
  * Feature to find the position of the device's NFC reader

## 4.7

  * Improved import from clipboard
  * New key creation wizard for Security Tokens
  * Removed password cache "time to live" setting


## 4.6

  * Import your keys using our new Secure Wi-Fi Transfer mechanism


## 4.5

  * Detailed description of security problems
  * Display keyserver status per key
  * Support for EdDSA
  * Fix pgp.mit.edu (new certificate)


## 4.4

  * New key status displays detailed information why a key is considered insecure or defective


## 4.3

  * Better support for large keys
  * Fix import of Gpg4win files with broken encodings


## 4.2

  * Experimental support for Elliptic Curve Encryption with Security Tokens
  * Redesigned key import screen
  * Design improvements to key lists
  * Support for keyserver onion addresses


## 4.1

  * Better detection of emails and other content when opened


## 4.0

  * Supporto sperimentale dei Token USB
  * Allow password changing of stripped keys


## 3.9

  * Detection and handling of text data
  * Miglioramento delle prestazioni
  * Miglioramento interfaccia di gestione dei Token


## 3.8

  * Redesigned key editing
  Scegli il tempo di ricordo individualmente quando inserisci una password
  * Importazione delle password di Facebook


## 3.7

  Migliorato il supporto ad Android 6 (permessi, integrazione nella selezione del testo)
  * API: Versione 10


## 3.6

  * Copie di backup crittografate
  * Security fixes based on external security audit
  * YubiKey NEO key creation wizard
  * Supporto base a MIME
  * Sincronizzazione automatica delle chiavi
  * Experimental feature: link keys to Github, Twitter accounts
  * Experimental feature: key confirmation via phrases
  * Caratteristica sperimentale: Tema scuro
  * API: Versione 9


## 3.5

  * Key revocation on key deletion
  * Improved checks for insecure cryptography
  * Fix: Don't close OpenKeychain after first time wizard succeeds
  * API: Versione 8


## 3.4

  * Anonymous key download over Tor
  * Supporto proxy
  * Better YubiKey error handling


## 3.3

  * New decryption screen
  * Decryption of multiple files at once
  * Migliore gestione errori di YubiKey


## 3.2

  * First version with full YubiKey support available from the user interface: Edit keys, bind YubiKey to keys,...
  * Material design
  * Integrazione della Scasione codici QR (Nuovi permessi richiesti)
  * Migliorato wizard della creazione di chiavi
  * Corretto contatti persi dopo sincronizzazione
  * Richiede Android 4
  * Schermata chiavi riprogettata
  * Simplify crypto preferences, better selection of secure ciphers
  * API: Detached signatures, free selection of signing key,...
  * Fix: Some valid keys were shown revoked or expired
  * Don't accept signatures by expired or revoked subkeys
  * Keybase.io support in advanced view
  * Method to update all keys at once


## 3.1.2

  * Corretta esportazione chiavi su file (davvero questa volta)


## 3.1.1

  * Corretta esportazione chiavi su file (la scrittura era parziale)
  * Risolto un crash in Android 2.3


## 3.1

  * Corretto crash su Android 5
  * Nuova schermata di certificazione
  * Secure Exchange directly from key list (SafeSlinger library)
  * New QR Code program flow
  * Redesigned decrypt screen
  * New icon usage and colors
  * Fix import of secret keys from Symantec Encryption Desktop
  * Experimental YubiKey support: Subkey IDs are now checked correctly


## 3.0.1

  * Gestione migliorata dell'importazioni di chiavi di grandi dimensioni
  * Migliorata la selezione delle sottochiavi


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
  * Gestione delle password migliorata
  * Condivisione chiavi via SafeSlinger
  * Experimental YubiKey support: Preference to allow other PINs, currently only signing via the OpenPGP API works, not inside of OpenKeychain
  * Fix usage of stripped keys
  * SHA256 predefinito per compatibilità
  * Intent API has changed, see https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * OpenPGP API now handles revoked/expired keys and returns all user ids


## 2.9

   * Correzione crash introdotti nella versione 2.8
  * Supporto sperimentale ad ECC
  * Experimental YubiKey support: Only signing with imported keys


## 2.8

  * So many bugs have been fixed in this release that we focus on the main new features
  * Key edit: awesome new design, key revocation
  * Key import: awesome new design, secure keyserver connections via hkps, keyserver resolving via DNS SRV records
  * Nuova schermata di introduzione
  * New key creation screen: autocompletion of name and email based on your personal Android accounts
  * File encryption: awesome new design, support for encrypting multiple files
  * New icons to show status of key (by Brennan Novak)
  * Important bug fix: Importing of large key collections from a file is now possible
  * Notification showing cached passphrases
  * Le chiavi sono collegate ai contatti di Android

Questo rilascio non sarebbe stato possibile senza il lavoro di Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar

## 2.7

  * Purple! (Dominik, Vincent)
  * New key view design (Dominik, Vincent)
  * Nuovi pulsanti piatti su Android (Dominik, Vincent)
  * Diverse correzioni alle API (Dominik)
  * Keybase.io import (Tim Bray)


## 2.6.1

  * Correzione di bug di regressione


## 2.6

  * Key certifications (thanks to Vincent Breitmoser)
  * Support for GnuPG partial secret keys (thanks to Vincent Breitmoser)
  * New design for signature verification
  * Lunghezza personalizzata chiave (grazie a Greg Witczak)
  * Fix share-functionality from other apps


## 2.5

  * Fix decryption of symmetric OpenPGP messages/files
  * Refactored key edit screen (thanks to Ash Hughes)
  * New modern design for encrypt/decrypt screens
  * API OpenPGP versione 3 (account api multipli, correzioni interne, ricerca chiavi)


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
  * Ricerca nelle liste di chiavi pubbliche
  * E molti altri miglioramenti e correzioni...


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
  * Correzione bug importazione chiavi
  * Key cross-certification (thanks to Ash Hughes)
  * Gestione corretta password UTF-8 (grazie ad Ash Hughes)
  * Prima versione multilingua (grazie ai collaboratori su Transifex)
  * Condivisione di chiavi attraverso Codici QR corretta e migliorata
  * Package signature verification for API


## 2.1.1

  * API Updates, preparation for K-9 Mail integration


## 2.1

  * Correzione di molti bug
  * Nuove API per sviluppatori
  * PRNG bug fix by Google


## 2.0

  * Complete redesign
  * Share public keys via QR codes, NFC beam
  * Firma chiavi
  * Caricamento chiavi su server
  * Fixes import issues
  * New AIDL API


## 1.0.8

  * Basic keyserver support
  * App2sd
  * More choices for passphrase cache: 1, 2, 4, 8, hours
  * Translations: Norwegian Bokmål (thanks, Sander Danielsen), Chinese (thanks, Zhang Fredrick)
  * Correzione di bug
  * Ottimizzazioni


## 1.0.7

  * Fixed problem with signature verification of texts with trailing newline
  * More options for passphrase cache time to live (20, 40, 60 mins)


## 1.0.6

  * Account adding crash on Froyo fixed
  * Cancellazione sicura dei file
  * Option to delete key file after import
  * Stream encryption/decryption (gallery, etc.)
  * New options (language, force v3 signatures)
  * Modifiche all'interfaccia utente
  * Correzione di bug


## 1.0.5

  * Traduzione in Italiano e Tedesco
  * Much smaller package, due to reduced BC sources
  * Nuove preferenze per GUI
  * Layout adjustment for localization
  * Correzione bug firma


## 1.0.4

  * Fixed another crash caused by some SDK bug with query builder


## 1.0.3

  * Fixed crashes during encryption/signing and possibly key export


## 1.0.2

  * Liste chiavi filtrabili
  * Smarter pre-selection of encryption keys
  * New Intent handling for VIEW and SEND, allows files to be encrypted/decrypted out of file managers
  * Fixes and additional features (key preselection) for K-9 Mail, new beta build available


## 1.0.1

  * GMail account listing was broken in 1.0.0, fixed again


## 1.0.0

  * Integrazione K-9 Mail, APG supporta build beta di K-9 Mail
  * Supporto per altri file manager (incluso ASTRO)
  * Traduzione in Sloveno
  * Nuovo database, più veloce, con utilizzo di memoria ridotto
  * Defined Intents and content provider for other apps
  * Correzione di bug