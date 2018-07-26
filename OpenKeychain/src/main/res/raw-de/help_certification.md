[//]: # (Beachte: Bitte schreibe jeden Satz in eine eigene Zeile, Transifex wird jede Zeile in ein eigenes Übesetzungsfeld setzen!)

## Schlüsselbestätigung
Ohne Bestätigung können sie nicht wissen, ob ein Schlüssel wirklich zu einer bestimmten Person gehört.
Der einfachste Weg einen Schlüssel zu bestätigen, ist den QR Code zu scannen oder ihn mit NFC auszutauschen.
Um Schlüssel zwischen mehr als zwei Personen zu bestätigen, empfehlen wir die Schlüssel-Tausch Funktion zu benutzen, die für deine Schlüssel verfügbar sind. 

## Schlüsselstatus

<img src="status_signature_verified_cutout_24dp"/>  
Bestätigt: Sie haben bereits den Schlüssel bestätigt, zum Beispiel durch das Scannen eines QR Codes.  
<img src="status_signature_unverified_cutout_24dp"/>  
Unbestätigt: Diese Key wurde noch nicht beglaubigt. Sie können nicht sicher sein, ob dieser Key wirklich zu einer bestimmten Person gehört.  
<img src="status_signature_expired_cutout_24dp"/>  
Abgelaufen: Der Key ist nicht mehr gültig. Nur der Besitzer kann die Gültigkeit des Schlüssels verlängern.  
<img src="status_signature_revoked_cutout_24dp"/>  
Widerrufen: Dieser Schlüssel ist nicht mehr gültig. Er wurde von dem Besitzer widerrufen.

## Erweiterte Informationen
Eine "Schlüsselbestätigung" in OpenKeychain ist implementiert mit dem Erstellen eines Zertifikats bei Beachtung von dem OpenPGP Standard.
Diese Zertifizierung ist eine ["generische Zertifizierung (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1), im Standard beschrieben durch:
"Der Ersteller dieser Zertifizierung macht keine Behauptungen darüber, wie gut der Zertifizierer geprüft hat, dass der Schlüsselinhaber tatsächlich die Person ist, die in der Benutzer-ID beschrieben wird."

Normalerweise sind Zertifizierungen (auch höherer Zertifizierungsniveaus, wie z.B. "positive Zertifizierungen" (0x13)) im OpenPGP-Web of Trust organisiert.
Unser Modell der Schlüsselbestätigung ist ein viel einfacheres Konzept, um allgemeine Nutzbarkeitsprobleme zu vermeiden, die mit diesem Web of Trust verknüpft sind.
Wir nehmen an, dass Schlüssel nur bis zu einem gewissen Maß bestätigt werden, dass immer noch nutzbar genug ist, um es unterwegs benutzen zu können.
Wir implementieren auch nicht (möglicherweise transitive) Vertrauenssignaturen oder eine Besitzervertrauensdatenbank wie in GnuPG.
Weiter werden Schlüssel, die mindestens eine Benutzer-ID enthalten, die von einem vertrauten Schlüssel unterschrieben ist, in der Schlüsselliste als "bestätigt" markiert.