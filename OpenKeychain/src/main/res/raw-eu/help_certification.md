[//]: # (OHARRA: Meseez jarri esaldi bakoitza bere lerroan, Transifex-ek lerroak bere itzulpen eremuan jartzen ditu!)

## Giltza Baieztapena
Baieztapenik gabe, ezin zara zihur egon giltza bat egitan norbanako zehatz batena den.
Giltza bat baieztatzeko bide arruntena QR Kodea eskaneatzea edo hura NFC bidez trukatzea da.
Giltzak bi norbanako baino gehiagoren artean baieztatzeko, zure giltzentzat eskuragarrai dagoen giltza trukea metodoa erabiltzea gomendatzen dugu.

## Giltza Egoera

<img src="status_signature_verified_cutout_24dp"/>  
Baieztatuta: Giltza hau jadanik baieztatuta duzu, adib. QR Kodea eskaneatuz.  
<img src="status_signature_unverified_cutout_24dp"/>  
Baieztatugabe: Giltza hau oraindik ez da baieztatu. Ezin zara zihur egon giltza egitan norbanako zehatz batena den.  
<img src="status_signature_expired_cutout_24dp"/>  
Iraungitua: Giltza hau aurrerantzean ez da baliozkoa. Jabeak bakarrik luzatu dezake bere baliozkotasuna.  
<img src="status_signature_revoked_cutout_24dp"/>  
Ukatua: Giltza hau aurrerantzean ez da baliozkoa. Bere jabeak ukatua izan da.

## Argibide Aurreratuak
OpenKeychain-en "giltza baieztapen" bat OpenPGP estandarraren araberako egiaztagiri bat sortuz egokitzen da.
Egiaztapen hau da ["egiaztapen generikoa (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) estandarrean azaltzen duena honek:
"The issuer of this certification does not make any particular assertion as to how well the certifier has checked that the owner of the key is in fact the person described by the User ID."

Traditionally, certifications (also with higher certification levels, such as "positive certifications" (0x13)) are organized in OpenPGP's Web of Trust.
Our model of key confirmation is a much simpler concept to avoid common usability problems related to this Web of Trust.
We assume that keys are verified only to a certain degree that is still usable enough to be executed "on the go".
We also do not implement (potentially transitive) trust signatures or an ownertrust database like in GnuPG.
Furthermore, keys which contain at least one user ID certified by a trusted key will be marked as "confirmed" in the key listings.