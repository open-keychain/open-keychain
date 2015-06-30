[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)


## 3.3

  * New decryption screen
  * Decryption of multiple files at once
  * Better handling of YubiKey errors

## 3.2

  * Erste Version mit kompletter YubiKey-Unterstützung in der Benutzeroberfläche: Schlüssel bearbeiten, YubiKey mit Schlüsseln verbinden,…
  * Material-Design
  * QR-Scanner-Integration (benötigt neue Berechtigungen)
  * Schlüsselerzeugungsassistent verbessert
  * Fehlende Kontakte nach Synchronisierung behoben
  * Benötigt Android 4
  * Neuer Schlüsselbildschirm
  * Krypto-Einstellungen vereinfacht, bessere Auswahl sicherer Verschlüsselungsverfahren
  * API: abgetrennte Signaturen, freie Wahl des Signaturschlüssels,...
  * Behoben: Einige gültige Schlüssel wurden als widerrufen oder abgelaufen angezeigt
  * Akzeptiert keine Signaturen abgelaufener oder widerrufener Unterschlüssel
  * Keybase.io-Unterstützung in der erweiterten Ansicht
  * Möglichkeit, alle Schlüssel auf einmal zu aktualisieren


## 3.1.2

  * Behoben: Schlüsselexport zu Datei (jetzt wirklich)


## 3.1.1

  * Schlüsselexport zu Datei repariert (sie wurden nur teilweise geschrieben)
  * Absturz unter Android 2.3 behoben


## 3.1

  * Absturz unter Android 5 behoben
  * Neuer Beglaubigungsbildschirm
  * Sicherer Austausch direkt aus der Schlüsselliste (SafeSlinger-Bibliothek)
  * Neuer Programmablauf für QR-Codes
  * Neugestalteter Entschlüsselungsbildschirm
  * Verwendung neuer Symbole und Farben
  * Behoben: Import geheimer Schlüssel aus Symantec Encryption Desktop
  * Experimentelle YubiKey-Unterstützung: Unterschlüssel-IDs werden jetzt richtig geprüft


## 3.0.1

  * Bessere Verarbeitung von großen Schlüsselimporten
  * Verbesserte Unterschlüsselauswahl


## 3.0

  * Kompatible, installierbare Apps in der App-Liste vorschlagen
  * Neues Design für Entschlüsselungsbildschirme
  * Viele Fehler beim Schlüsselimport behoben, auch bei gekürzten Schlüsseln
  * Schlüsselauthentifikations-Attribute berücksichtigen und anzeigen
  * Benutzeroberfläche zum Erzeugen benutzerdefinierter Schlüssel
  * Benutzer-ID-Widerrufszertifikate repariert
  * Neue Cloud-Suche (sucht über traditionelle Schlüsselserver und über keybase.io)
  * Unterstützung für das Kürzen von Schlüsseln innerhalb von OpenKeychain
  * Experimentelle YubiKey-Unterstützung: Unterstützung für Signaturerzeugung und Entschlüsselung


## 2.9.2

  * Repariere Schlüssel, die in 2.9.1 beschädigt wurden
  * Experimentelle YubiKey-Unterstützung: Entschlüsselung funktioniert nun via API


## 2.9.1

  * Verschlüsselungsbildschirm in zwei Bildschirme aufgeteilt
  * Behoben: Fehler bei Handhabung von Schlüsselattributen (unterstützt nun Schlüssel aus Mailvelope 0.7)
  * Handhabung von Passwörtern verbessert
  * Schlüsselaustausch mit SafeSlinger
  * Experimental YubiKey support: Preference to allow other PINs, currently only signing via the OpenPGP API works, not inside of OpenKeychain
  * Nutzung gekürzter Schlüssel repariert
  * Standardmäßig SHA256 aufgrund von Kompatibilität
  * Intent API has changed, see https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * OpenPGP API now handles revoked/expired keys and returns all user ids


## 2.9

  * Fixing crashes introduced in v2.8
  * Experimentelle ECC-Unterstützung
  * Experimental YubiKey support: Only signing with imported keys


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
  * App2SD
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