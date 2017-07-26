[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## Potrjevanje ključev
Brez overitve ne morete vedeti, če nek ključ zares pripada določeni osebi.
Najbolj preprost način za overitev ključa je skeniranje kode QR ali izmenjava ključev preko NFC.
To confirm keys between more than two persons, we suggest using the key exchange method available for your keys.

## Status ključa

<img src="status_signature_verified_cutout_24dp"/>  
Ključ je potrjen: ta ključ ste že potrdili, npr. s skeniranjem kode QR.  
<img src="status_signature_unverified_cutout_24dp"/>  
Ključ ni potrjen: Ta ključ še ni bil overjen, zato ne morete vedeti, če res pripada osebi, ki naj bi jo predstavljal.  
<img src="status_signature_expired_cutout_24dp"/>  
Ključ je potekel: Ta ključ ni več veljaven. Samo lastnik lahko podaljša veljavnost svojega ključa.  
<img src="status_signature_revoked_cutout_24dp"/>  
Ključ preklican: Ta ključ ni več veljaven. Lastnik ga je preklical.

## Dodatne informacije
"Overitev ključa" se v aplikaciji OpenKeychain izvede s stvaritvijo potrdila v skladu s standardom OpenPGP.
To potrdilo je ["generično potrdilo (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1), kot je zapisano v standardu:
"Izdajatelj tega potrdila ne jamči za kakovost preverjanja overitelja, če je lastnik ključa dejansko oseba, označena v identifikaciji (ID) ključa."

Navadno so potrdila (tudi na višji stopnji overjanja, kot so "pozitivna pravila" (0x13)) organizirana v Omrežje zaupanja OpenPGP.
Naš način overjanja je mnogo bolj preprost, s čimer se želimo izogniti pogostim težavam povezanim z Omrežjem zaupanja.
Domnevamo, da so ključi preverjeni do mere, ki je še uporabna, glede na mobilno naravo aplikacije.
Tudi ne ponujamo (potencialno prenosljivih) t.i. podpisov zaupanja kot jih ponuja GnuPG.
Nadalje; ključi, ki vsebujejo vsaj en ID potrjen s strani zaupanja vrednega ključa, bodo na seznamu označeni kot "overjeni".