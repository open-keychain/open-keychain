[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## 3.4

  * Anonymous key download over Tor
  * Proxy support
  * Better YubiKey error handling

## 3.3

  * Neuer Entschlüsselungsbildschirm
  * Entschlüsselung mehrerer Dateien gleichzeitig
  * Bessere Behandlung von YubiKey-Fehlern

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
  * Intent-API hat sich geändert, siehe https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * OpenPGP-API bearbeitet ab jetzt widerrufene/abgelaufene Schlüssel und gibt alle Benutzer-IDs zurück


## 2.9

  * Behebt Abstürze aus v2.8
  * Experimentelle ECC-Unterstützung
  * Experimentelle YubiKey-Unterstützung (nur Unterschreiben mit importierten Schlüsseln)


## 2.8

  * In diesem Release wurden so viele Fehler behoben, dass wir uns lieber auf die neuen Funktionen konzentrieren
  * Schlüsselbearbeitung: Fantastisches neues Design, Schlüsselwiderruf
  * Schlüsselimport: Fantastisches neues Design, sichere Verbindungen zu Keyservern über hkps, Namensauflösung der Schlüsselserver über DNS SRV-Einträge
  * Neuer Bildschirm bei der ersten Öffnung
  * Neuer Schlüsselerstellungsbildschirm: Autovervollständigung von Name und E-Mail basierend auf Deinen persönlichen Android-Konten
  * Dateiverschlüsselung: fantastisches neues Design, Unterstützung für die Verschlüsselung mehrerer Dateien
  * Neue Symbile zum Anzeigen des Schlüsselstatus (von Brennan Novak)
  * Wichtiger Fehler behoben: Import großer Schlüsselsammlungen aus einer Datei ist nun möglich
  * Benachrichtigung, die zwischengespeicherte Passwörter anzeigt
  * Schlüssel sind mit den Kontakten verbunden

Dieser Release wäre ohne die Arbeit von Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray und Thialfihar nicht möglich gewesen

## 2.7

  * Lila! (Dominik, Vincent)
  * Neues Schlüsselansichtsdesign (Dominik, Vincent)
  * Neue flache Android-Knöpfe (Dominik, Vincent)
  * API-Korrekturen (Dominik)
  * Import aus keybase.io (Tim Bray)


## 2.6.1

  * Some fixes for regression bugs


## 2.6

  * Schlüsselbeglaubigungen (dank Vincent Breitmoser)
  * Unterstützung für GnuPG-Teilschlüssel (danke an Vincent Breitmoser)
  * Neues Design für Signaturprüfung
  * Benutzerdefinierte Schlüssellänge (Dank an Greg Witczak)
  * Fehler bei der Teilen-Funktionalität aus anderen Apps behoben


## 2.5

  * Fehler bei der Entschlüsselung symmetrischer OpenPGP-Nachrichten/Dateien behoben
  * Umgestaltung des Schlüsselbearbeitungsbildschirms (Dank an Ash Hughes)
  * Neues modernes Design für Verschlüsselungs-/Entschlüsselungsbildschirme
  * OpenPGP-API Version 3 (mehrere API-Konten, interne Fehlerbehebungen, Schlüsselsuche)


## 2.4
Dank an alle Bewerber bei Google Summer of Code 2014, welche diesen Release funktionsreich und fehlerfrei gemacht haben!
Neben einigen kleinen Fehlerbehebungen wurden bemerkenswert viele Patches durch die folgenden Personen beigesteuert (alphabetisch sortiert):
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser.

  * Neue einheitliche Schlüsselliste
  * Eingefärbter Schlüsselfingerabdruck
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