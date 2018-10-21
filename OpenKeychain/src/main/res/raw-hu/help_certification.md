[//]: # (MEGJEGYZÉS: minden mondatot külön sorba írjon, a Transifex minden sort a saját fordítási mezőjébe tesz!)

## Kulcs megerősítése
Megerősítés nélkül nem lehet biztos abban, hogy egy kulcs valóban a megfelelő személyhez tartozik-e.
A legegyszerűbb módja egy kulcs megerősítésének, ha beolvas egy QR-kódot vagy kicseréli NFC-n keresztül.
Kettő vagy több személy közötti kulcs megerősítéséhez azt javasoljuk, hogy használja a kulcscsere módszert, amely elérhető a kulcsainál.

## Kulcs állapota

<img src="status_signature_verified_cutout_24dp"/>  
Megerősítve: már megerősítette ezt a kulcsot, például a QR-kód beolvasásával.  
<img src="status_signature_unverified_cutout_24dp"/>  
Megerősítetlen: ez a kulcs még nem lett megerősítve. Nem lehet biztos abban, hogy a kulcs valóban a megfelelő személyhez tartozik-e.  
<img src="status_signature_expired_cutout_24dp"/>  
Lejárt: ez a kulcs többé nem érvényes. Csak a tulajdonos tudja meghosszabbítani az érvényességét.  
<img src="status_signature_revoked_cutout_24dp"/>  
Visszavont: ez a kulcs többé nem érvényes. A tulajdonosa visszavonta azt.

## Speciális információk
Egy „kulcs megerősítése” az OpenKeychain alkalmazásban az OpenPGP szabvány szerinti tanúsítvány létrehozásával van megvalósítva.
A tanúsítvány egy [„általános tanúsítvány (0x10)”](http://tools.ietf.org/html/rfc4880#section-5.2.1), amelyet a következő szabvány ír le:
„A tanúsítvány kibocsátója nem tesz különösebb állítást arra vonatkozóan, hogy a tanúsító mennyire jól ellenőrizte, hogy a kulcs tulajdonosa valóban a felhasználói azonosító által leírt személy.”

Hagyományosan a tanúsítványok (magasabb tanúsítványszintekkel is, például „pozitív tanúsítványok” (0x13)) az OpenPGP bizalmi hálózatában vannak megszervezve.
A kulcsmegerősítési modellünk egy sokkal egyszerűbb elgondolás, hogy elkerülje a bizalmi hálózathoz kapcsolódó gyakori használhatósági problémákat.
Feltételezzük, hogy a kulcsokat csak egy bizonyos fokig ellenőrzik, ami még eléggé használható ahhoz, hogy „menet közben” végre lehessen hajtani.
Nem is valósítunk meg (potenciálisan átmeneti) bizalmi aláírásokat vagy tulajdonosbizalmi adatbázisokat, mint ami a GnuPG-ben van.
Továbbá az olyan kulcsok, amelyek egy megbízható kulcs által tanúsított legalább egy felhasználói azonosítót tartalmaznak, a kulcslistákban „megerősítve” jelzésűként lesznek jelölve.