[//]: # (Beachte: Bitte schreibe jeden Satz in eine eigene Zeile, Transifex wird jede Zeile in ein eigenes Übesetzungsfeld setzen!)

## 5.1
  * Support für Ledger Nano S
  * Unterstützung für Suche in Web Key Directory (WKD)
  * Mögliche API-Sicherheitslücke behoben

## 5.0
  * Unterstützung für Autocrypt verbessert

## 4.9

  Curve25519 Support
  Verbesserter Support für Security-Tokens

## 4.8

  * Unterstützung für USB-Tokens verbessert: Gnuk, Nitrokey Modelle, YubiKey 4 Modelle
  Feature zum Finden des NFC Lesers des Geräts

## 4.7

  * Die Möglichkeit, aus der Zwischenablage zu Importieren wurde verbessert
  * Neuer Schlüsselgenerierungsassistent für Security-Tokens
  Passwortcache-Einstellung "time to live" entfernt


## 4.6

  Importieren Sie Ihre Schlüssel mit unserem neuen Secure Wi-Fi Transfer Mechanismus


## 4.5

  Detaillierte Beschreibung von Sicherheitsproblemen
  Status des Keyservers je Schlüssel anzeigen
  * Unterstützung von EdDSA
  Fix pgp.mit.edu (neues Zertifikat)


## 4.4

  Neuer Schlüsselstatus zeigt detaillierte Informationen dazu an, warum ein Schlüssel als unsicher oder beschädigt angesehen wird


## 4.3

  * Bessere Unterstützung von längeren Schlüsseln
  Fix zum Importieren von Gpg4win-Dateien mit beschädigter Kodierung


## 4.2

  Experimenteller Support für Elliptic Curve Verschlüsselung mit Security-Tokens
  Anzeige zum Import von Schlüsseln neu gestaltet
  Verbesserungen im Design der Schlüssellisten
  Support für Keyserver-Onion-Adressen


## 4.1

  Bessere Erkennung von E-Mails und anderen Inhalten beim Öffnen


## 4.0

  * Experimentelle Unterstützung für Smartcards über USB
  * Erlaube Passwortänderung von gekürzten Schlüsseln


## 3.9

  * Erkennung und Bearbeitung von Textdaten
  * Performanceverbesserungen
  * Verbesserung der Benutzeroberfläche zur Handhabung von Smartcards


## 3.8

  * Bearbeiten von Schlüsseln überarbeitet
  * Wähle die Zeit wie lange dein Passwort erinnert wird beim jedem Eingeben
  Facebook Schlüsselimport


## 3.7

  * Verbesserte Unterstützung für Android 6 (Berechtigungen, Integration in Textauswahl)
  * API: Version 10


## 3.6

  * Verschlüsselte Sicherheitskopien
  * Sicherheitsfixes basierend auf Resultaten des externen Sicherheitsaudits
  * YubiKey NEO Schlüsselerzeugungsassistent verbessert
  * Grundlegende interne MIME-Unterstützung
  * Automatische Schlüsselsynchronisierung
  * Experimentelles Feature: Verknüpfen von Schlüssel mit Github- und Twitter-Accounts
  * Experimentelles Feature: Schlüsselbestätigung mithilfe von Passphrasen
  * Experimentelles Feature: Dunkles Design
  * API: Version 9


## 3.5

  * Schlüsselwiderruf bei Schlüssellöschung
  * Verbesserte Kontrollen für unsichere Verschlüsselung
  * Behoben: OpenKeychain nach Abschluss des Anfängerassistenten nicht schließen
  * API: Version 8


## 3.4

  * Anonymer Schlüsseldownload über Tor
  * Proxyunterstützung
  * Bessere YubiKey Fehlerbehandlung


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
  Fix für User ID Revocation Certificates
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
  * Experimentelle YubiKey-Unterstützung: Einstellung zum Erlauben anderer PINs, derzeit funktioniert nur die Beglaubigung über die OpenPGP API, nicht innerhalb von OpenKeychain
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

  * Einige Korrekturen für Regressionsfehler


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
  * Unterstützt Schlüsselserver ports
  * Deaktiviere Möglichkeit unsichere Schlüssel zu erstellen
  * Viel mehr interne Arbeit an der API
  * Benutzerkennungen beglaubigen
  * Schlüsselserver-Suchanfrage basierend auf maschinenlesbarer Ausgabe
  * "Navigation Drawer" auf Tablets sperren
  * Vorschläge für E-Mails bei Schlüsselerzeugung
  * Suchen in öffentlichen Schlüssellisten
  * Und viele weitere Verbesserungen und Fehlerbehebungen...


## 2.3.1

  * Hotfix für Absturz beim Aktualisieren von alten Versionen


## 2.3

  * Kein unnötiger Export öffentlicher Schlüssel beim Export der geheimen Schlüssel (Dank an Ash Hughes)
  * Behoben: Setzen des Schlüsselablaufdatums (Dank an Ash Hughes)
  * Weitere interne Fehlerbehebungen für das Editieren von Schlüsseln (Dank an Ash Hughes)
  * Schlüsselserverabfrage direkt aus dem Importierungsbildschirm
  * Behoben: Layout und Dialogstil auf Android 2.2-3.0
  * Behoben: Absturz bei leeren Benutzer-IDs
  * Absturz und leere Listen nach der Rückkehr vom Signierbildschirm behoben
  * Bouncy Castle (Kryptographie-Bibliothek) von 1.47 auf 1.50 aktualisiert und aus Quellcode kompiliert
  * Hochladen des Schlüssels aus dem Signierbildschirm behoben


## 2.2

  * Neues Design mit "Navigation Drawer"
  * Neues Design der Liste öffentlicher Schlüssel
  * Neue Ansicht für öffentliche Schlüssel
  * Fehler beim Importieren von Schlüsseln behoben
  * Schlüsselbeglaubigung über Kreuz (Dank an Ash Hughes)
  * Korrekte Verarbeitung von UTF-8-Passwörtern (Dank an Ash Hughes)
  * Erste Version mit neuen Sprachen (Dank an die Unterstützer auf Transifex)
  * Teilen von Schlüsseln über QR-Codes behoben und verbessert
  * Paket-Signaturprüfung für API


## 2.1.1

  * API-Aktualisierungen, Vorbereitung für die K-9-Mail-Integration


## 2.1

  * Viele Fehlerbehebungen
  * Neue API für Entwickler
  * PRNG-Fehlerbehebung von Google


## 2.0

  * Komplette Neugestaltung
  * Öffentliche Schlüssel über QR-Codes oder NFC-Beam teilen
  * Schlüssel unterschreiben
  * Schlüssel auf den Server hochladen
  * Behebt Importprobleme
  * Neue AIDL-API


## 1.0.8

  * Grundlegende Schlüsselserverunterstützung
  * App2SD
  * Mehr Auswahlmöglichkeiten für den Passwortzwischenspeicher: 1, 2, 4, 8, Stunden
  * Übersetzungen: Norwegisch (Dank an Sander Danielsen), Chinesisch (Dank an Zhang Fredrick)
  * Fehlerbehebungen
  * Optimierungen


## 1.0.7

  * Problem mit Signaturprüfung von Texten mit angehängtem Zeilenvorschub behoben
  * Mehr Optionen für die Länge der Passwortzwischenspeicherung (20, 40, 60 Minuten)


## 1.0.6

  * Absturz bei Kontoerstellung unter Froyo behoben
  * Sichere Dateilöschung
  * Option zum Löschen der Schlüsseldatei nach dem Import
  * Streamverschlüsselung/-entschlüsselung (Galerie, usw.)
  * Neue Optionen (Sprache, v3-Signaturen erzwingen)
  * Oberflächenänderungen
  * Fehlerbehebungen


## 1.0.5

  * Deutsche und Italienische Übersetzung
  * Viel kleineres Paket durch reduzierte BC-Quellen
  * Neues Einstellungen-Benutzeroberfläche
  * Anpassung der Anordnung für Übersetzungen
  Fehler in der Signatur behoben


## 1.0.4

  * Weiteren Absturz durch einen SDK-Fehler mit "query builder" behoben


## 1.0.3

  * Abstürze während Verschlüsselung/Beglaubigung und möglicherweise Schlüsselexport behoben


## 1.0.2

  * Filterbare Schlüsselliste
  * Intelligentere Vorauswahl von Verschlüsselungsschlüsseln
  * Neue Absichtsbehandlung für VIEW und SEND, ermöglicht das Ver-/Entschlüsseln von Dateien aus Dateimanagern
  * Fehlerbehebungen und zusätzliche Funktionen (Schlüsselvorauswahl) für K-9 Mail, neuer Beta-Build verfügbar


## 1.0.1

  * GMail-Konto-Auflistung war fehlerhaft in 1.0.0, erneut behoben


## 1.0.0

  * K-9Mail-Integration, APG-unterstützendes Beta-Build von K-9 Mail
  * Unterstützung von mehr Dateimanagern (einschließlich ASTRO)
  * Slowenische Übersetzung
  * Neue Datenbank, viel schneller, weniger Speicherbelegung
  * Definierte Absichten und Inhaltsanbieter für andere Apps
  * Fehlerbehebungen