[//]: # (MEGJEGYZÉS: minden mondatot külön sorba írjon, a Transifex minden sort a saját fordítási mezőjébe tesz!)

## 5.2
  * Továbbfejlesztett kulcsimportálás a vágólapról

## 5.1
  * Ledger Nano S támogatás
  * Web Key Directory (WKD) keresésének támogatása
  * Lehetséges API biztonsági probléma javítása

## 5.0
  * Továbbfejlesztett Autocrypt támogatás

## 4.9

  * Curve25519 támogatás
  * Továbbfejlesztett támogatás a biztonsági tokenekhez

## 4.8

  * Továbbfejlesztett támogatás az USB tokenekhez: Gnuk, Nitrokey modellek, YubiKey 4 modellek
  * Funkció az eszköz NFC-olvasója helyzetének megtalálásához

## 4.7

  * Továbbfejlesztett importálás a vágólapról
  * Új kulcslétrehozás varázsló a biztonsági tokenekhez
  * Eltávolításra került a jelszógyorsítótár „time to live” beállítása.


## 4.6

  * Kulcsok importálása az új biztonságos Wi-Fi átviteli mechanizmus használatával


## 4.5

  * Biztonsági problémák részletes leírása
  * Kulcskiszolgáló állapotának megjelenítése kulcsonként
  * EdDSA támogatás
  * A pgp.mit.edu javítása (új tanúsítvány)


## 4.4

  * Az új kulcsállapot részletes információt jelenít meg, hogy egy kulcs miért tekintendő nem biztonságosnak vagy hiányosnak


## 4.3

  * Jobb támogatás a nagy kulcsokhoz
  * Törött kódolással rendelkező Gpg4win fájlok importálásának javítása


## 4.2

  * Kísérleti támogatás az elliptikus görbe titkosításhoz biztonsági tokenekkel
  * Újratervezett kulcsimportálás képernyő
  * Megjelenítési javítások a kulcslistákhoz
  * Támogatás a kulcskiszolgáló onion címeihez


## 4.1

  * E-mailek és egyéb tartalmak jobb felismerése megnyitáskor


## 4.0

  * Kísérleti támogatás az USB-n keresztüli biztonsági tokenekhez
  * Feldarabolt kulcsok jelszóváltoztatásának lehetővé tétele


## 3.9

  * Szövegadatok felismerése és kezelése
  * Teljesítményjavítások
  * Felhasználói felület javítások a biztonsági token kezelésénél


## 3.8

  * Újratervezett kulcsszerkesztés
  * Megjegyzési idő kiválasztása egyénileg, amikor jelszavakat írnak be
  * Facebook kulcs importálás


## 3.7

  * Továbbfejlesztett Android 6 támogatás (jogosultságok, integráció a szövegkijelölésbe)
  * API: 10-es verzió


## 3.6

  * Titkosított biztonsági mentések
  * Biztonsági javítások külső biztonsági vizsgálat alapján
  * YubiKey NEO kulcslétrehozási varázsló
  * Alapszintű belső MIME-támogatás
  * Automatikus kulcsszinkronizáció
  * Kísérleti funkció: kulcsok összekapcsolása a Github és Twitter fiókokkal
  * Kísérleti funkció: kulcsmegerősítés kifejezéseken keresztül
  * Kísérleti funkció: sötét téma
  * API: 9-es verzió


## 3.5

  * Kulcsvisszavonás a kulcs törlésekor
  * Továbbfejlesztett ellenőrzések a nem biztonságos kriptográfiánál
  * Javítás: ne zárja be az OpenKeychain alkalmazást, miután ez első alkalommal futó varázsló sikeresen végzett
  * API: 8-as verzió


## 3.4

  * Névtelen kulcsletöltés Tor hálózaton keresztül
  * Proxytámogatás
  * Jobb YubiKey hibakezelés


## 3.3

  * Új visszafejtés képernyő
  * Egyszerre több fájl visszafejtése
  * A YubiKey hibák jobb kezelése


## 3.2

  * Első verzió teljes YubiKey támogatással, amely elérhető a felhasználói felületről: kulcsok szerkesztése, YubiKey kötése kulcsokhoz, stb.
  * Material felületterv
  * QR-kód beolvasásának integrálása (új jogosultságok szükségesek)
  * Továbbfejlesztett kulcslétrehozás varázsló
  * Szinkronizálás után hiányzó partnerek javítása
  * Követelmény: Android 4
  * Újratervezett kulcs képernyő
  * Titkosítás beállításainak egyszerűsítése, biztonságos titkosítók jobb kiválasztása
  * API: aláírások leválasztása, aláíró kulcsok szabad kiválasztása, stb.
  * Javítás: néhány érvényes kulcs visszavontként vagy lejártként volt megjelenítve
  * Ne fogadjon el aláírásokat lejárt vagy visszavont alkulcsoktól
  * Keybase.io támogatás a speciális nézetben
  * Módszer az összes kulcs egyszerre történő frissítésére


## 3.1.2

  * Javítva a kulcs exportálása fájlba (most már valóban)


## 3.1.1

  * Javítva a kulcs exportálása fájlba (részlegesen lettek kiírva)
  * Javítva az összeomlás Android 2.3-on


## 3.1

  * Javítva az összeomlás Android 5-ön
  * Új tanúsítás képernyő
  * Biztonságos kicserélés közvetlenül a kulcslistáról (SafeSlinger programkönyvtár)
  * Új QR-kód programfolyam
  * Újratervezett visszafejtés képernyő
  * Új ikonhasználat és színek
  * Javítva a titkos kulcsok importálása a Symantec Encryption Desktop programból
  * Kísérleti YubiKey támogatás: az alkulcs azonosítói mostantól helyesen ellenőrzöttek


## 3.0.1

  * Nagy kulcsok importálásainak jobb kezelése
  * Továbbfejlesztett alkulcs kiválasztás


## 3.0

  * Telepíthető kompatibilis alkalmazások javaslata az alkalmazáslistában
  * Új felhasználói felület a visszafejtés képernyőknél
  * Számos javítás a kulcsimportálásnál, feldarabolt kulcsok javítása is
  * Kulcshitelesítési jelzők figyelembe vétele és megjelenítése
  * Felhasználói felület egyéni kulcsok létrehozásához
  * Javítva a felhasználói azonosító visszavonás tanúsítványai
  * Új felhőkeresés (keresés a hagyományos kulcskiszolgálókon és a keybase.io oldalon)
  * Kulcsok feldarabolásának támogatása az OpenKeychainen belül
  * Kísérleti YubiKey támogatás: támogatás az aláírás-előállításhoz és visszafejtéshez


## 2.9.2

  * Javítva a 2.9.1-ben eltörött kulcsok
  * Kísérleti YubiKey támogatás: a visszafejtés mostantól működik API-n keresztül


## 2.9.1

  * Titkosítás képernyő kettévágása
  * Javítva a kulcs jelzőinak kezelése (mostantól támogatottak a Mailvelope 0.7 kulcsok)
  * Továbbfejlesztett jelszókezelés
  * Kulcsmegosztás SafeSlinger használatáva
  * Kísérleti YubiKey támogatás: beállítás egyéb PIN-kódok engedélyezésére, jelenleg csak az aláírás működik OpenPGP használatával, nem az OpenKeychainen belül
  * Javítva a feldarabolt kulcsok használata
  * SHA256 alapértelmezettként a kompatibilitáshoz
  * Intent API megváltoztatva, lásd: https://github.com/open-keychain/open-keychain/wiki/Intent-API
  Az OpenPGP API mostantól kezeli a visszavont vagy lejárt kulcsokat, és visszaadja az összes felhasználói azonosítót


## 2.9

  * Javítva a 2.8-as verzióban bekerült összeomlások
  * Kísérleti ECC támogatás
  * Kísérleti YubiKey támogatás: csak aláírás importált kulcsokkal


## 2.8

  * Olyan sok hiba lett javítva ebben a kiadásban, hogy a főbb új funkciókra összpontosítunk
  * Kulcsszerkesztés: fantasztikus új megjelenés, kulcsvisszavonás
  * Kulcsimportálás: fantasztikus új megjelenés, biztonságos kulcskiszolgáló kapcsolatok hkps-en keresztül, kulcskiszolgáló feloldás DNS SRV rekordokon keresztül
  * Új első alkalmi képernyő
  * Új kulcslétrehozás képernyő: név és e-mail automatikus kiegészítése a személyes Android fiókok alapján
  * Fájltitkosítás: fantasztikus új megjelenés, több fájl titkosításának támogatása
  * Új ikonok a kulcs állapotának megjelenítéséhez (Brennan Novak jóvoltából)
  * Fontos hibajavítás: mostantól lehetséges nagy kulcsgyűjtemények importálása fájlból
  * Az értesítés megjeleníti a gyorsítótárazott jelszavakat
  * A kulcsok az Android partnerekhez vannak kapcsolva

Ez a kiadás nem jött volna létre Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar munkája nélkül

## 2.7

  * Lila! (Dominik, Vincent)
  * Új kulcsmegtekintés megjelenés (Dominik, Vincent)
  * Új lapos Android gombok (Dominik, Vincent)
  * API javítások (Dominik)
  * Keybase.io importálás (Tim Bray)


## 2.6.1

  * Néhány javítás a regressziós hibáknál


## 2.6

  * Kulcstanúsítványok (köszönet: Vincent Breitmoser)
  * Támogatás a GnuPG részleges titkos kulcsokhoz (köszönet: Vincent Breitmoser)
  * Új felhasználói felület az aláírás ellenőrzéséhez
  * Egyéni kulcshossz (köszönet: Greg Witczak)
  * Javított megosztási funkcionalitás más alkalmazásokkal


## 2.5

  * Javítva a szimmetrikus OpenPGP üzenetek vagy fájlok visszafejtése
  * Átalakított kulcsszerkesztés képernyő (köszönet: Ash Hughes)
  * Új modern felhasználói felület a titkosítás és visszafejtés képernyőknél
  * OpenPGP API 3-as verzió (több API fiók, belső javítások, kulcskeresés)


## 2.4
Köszönet az összes Google Summer of Code 2014 pályázójának, akik ezt a kiadást funkciógazdaggá és hibamentessé tették!
Számos kis javítócsomag mellett a következő személyek jelentős mennyiségű javítócsomagot készítenek (betűrendben):
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser.

  * Új egységes kulcslista
  * Színezett kulcsujjlenyomat
  * Támogatás a kulcskiszolgáló portokhoz
  * Annak a lehetőségnek a kikapcsolása, hogy gyenge kulcsokat állítson elő
  * Sokkal több belső munka az API-n
  * Felhasználói azonosítók tanúsítása
  * Kulcskiszolgáló lekérdezések a számítógép által olvasható kimenet alapján
  * Navigációs oldalablak zárolása táblagépeken
  * Javaslatok az e-mailekre kulcsok létrehozásánál
  * Keresés a nyilvános kulcslistákban
  * És sokkal több továbbfejlesztés és javítás…


## 2.3.1

  * Gyorsjavítás az összeomláshoz, amikor régi verziókról frissít


## 2.3

  * Nyilvános kulcsok szükségtelen exportálásának eltávolítása, amikor titkos kulcsot exportál (köszönet: Ash Hughes)
  * Javítva a lejárati dátumok beállítása a kulcsoknál (köszönet: Ash Hughes)
  * Több belső javítás, amikor kulcsokat szerkeszt (köszönet: Ash Hughes)
  * Kulcskiszolgálók közvetlen lekérdezése az importálás képernyőről
  * Elrendezés és párbeszédablak stílusának javítása Android 2.2-3.0 verziókon
  * Javítva az összeomlás az üres felhasználói azonosítókkal rendelkező kulcsoknál
  * Összeomlás és üres listák javítása, amikor visszatér az aláírás képernyőről
  * Bouncy Castle (kriptográfiai programkönyvtár) frissítve 1.47-ről 1.50-re, és lefordítás forrásból
  * Javítva a kulcs feltöltése az aláíró képernyőről


## 2.2

  * Új felhasználói felület navigációs oldalablakkal
  * Új nyilvános kulcs lista felhasználói felület
  * Új nyilvános kulcs nézet
  * Hibajavítások kulcsok importálásánál
  * Kulcs kereszttanúsítványa (köszönet: Ash Hughes)
  * UTF-8 jelszavak megfelelő kezelése (köszönet: Ash Hughes)
  * Első verzió új nyelvekkel (köszönet a Transifex közreműködőinek)
  * Kulcsok QR-kódokon keresztüli megosztásának javítása és továbbfejlesztése
  * Csomagaláírás ellenőrzése az API-hoz


## 2.1.1

  * API frissítések, előkészítés a K-9 Mail integrációhoz


## 2.1

  * Rengeteg hibajavítás
  * Új API a fejlesztőknek
  * PRNG hibajavítás a Google-tól


## 2.0

  * Teljes újratervezés
  * Nyilvános kulcsok megosztása QR-kódokon és NFC jelen keresztül
  * Kulcsok aláírása
  * Kulcsok feltöltése a kiszolgálóra
  * Importálási problémák javítása
  * Új AIDL API


## 1.0.8

  * Alapszintű kulcskiszolgáló támogatás
  * App2sd
  * Több lehetőség a jelszógyorsítótárra: 1, 2, 4, 8 óra
  * Fordítások: norvég bokmål (Köszönet: Sander Danielsen), kínai (köszönet: Zhang Fredrick)
  * Hibajavítások
  * Optimalizálások


## 1.0.7

  * Javítva a lezáró új sorral rendelkező szövegek aláírás-ellenőrzésének problémája
  * Több lehetőség a jelszógyorsítótár élettartamára (20, 40, 60 perc)


## 1.0.6

  * Javítva a fiókhozzáadási összeomlás Froyon
  * Biztonságos fájltörlés
  * Lehetőség a kulcsfájl törlésére importálás után
  * Adatfolyam titkosítása és visszafejtése (galéria, stb.)
  * Új beállítások (nyelv, v3 aláírások kényszerítése)
  * Felületi változások
  * Hibajavítások


## 1.0.5

  * Német és olasz fordítás
  * Sokkal kisebb csomag a csökkentett BC források miatt
  * Új beállítások felhasználói felület
  * Elrendezésjavítás a honosításhoz
  * Aláírás hibajavítás


## 1.0.4

  * Javítva egy másik összeomlás, amelyet néhány SDK hiba okozott a lekérdezés-összeállítóval


## 1.0.3

  * Javítva a titkosítás vagy visszafejtés alatti összeomlások és lehetséges kulcsimportálás


## 1.0.2

  * Szűrhető kulcslisták
  * Titkosító kulcsok intelligensebb előre kiválasztása
  * Új Intent kezelés VIEW és SEND esetén, lehetővé téve a fájlok titkosítását vagy visszafejtését a fájlkezelőkön kívül
  * Javítások és további funkciók (kulcs előre kiválasztása) a K-9 Mailnél, új béta verzió érhető el


## 1.0.1

  * A GMail fiók felsorolása törött volt az 1.0.0-ban, újra javítva


## 1.0.0

  * K-9 Mail integráció, a K-9 Mail APG támogatott béta verziója
  * Több fájlkezelő támogatása (beleértve az ASTRO fájlkezelőt)
  * Szlovén fordítás
  * Új adatbázis: sokkal gyorsabb, kevesebb memóriahasználat
  * Intentek és tartalomszolgáltató meghatározás más alkalmazásokhoz
  * Hibajavítások