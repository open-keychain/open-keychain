[//]: # (OHARRA: Meseez jarri esaldi bakoitza bere lerroan, Transifex-ek lerroak bere itzulpen eremuan jartzen ditu!)

## Giltza Baieztapena
Baieztapenik gabe, ezin zara zihur egon giltza bat egitan norbanako zehatz batena den.
Giltza bat baieztatzeko bide arruntena QR Kodea eskaneatzea edo hura NFC bidez trukatzea da.
Giltzak bi norbanako baino gehiagoren artean baieztatzeko, zure giltzentzat eskuragarri dagoen giltza truke metodoa erabiltzea gomendatzen dugu.

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
"Egiaztagiri honen jaulkitzaileak ez du inolako baieztapen berezirik egin egiaztatzaileak zein ongi egiaztatu duen Erabiltzaile ID-ak azaltzen duen norbanakoa egitan den giltzaren jabea."

Arrunt, egiaztagiriak (baita egiaztagiritze maila handienekoak, "egiaztagiritze positiboak" bezalakoak (0x13)) OpenPGP-ren Fidagarritasun Webean daude antolatuta.
Gure giltza baieztapena adigai askoz errazagoa da Fidagarritasun Webaren erabiltze arrunteko arazoak saihesteko.
Onartzen dugu giltzak maila batean bakarrik daudela egiaztatuta oraindik nahikoa erabilgarria dena "joanean" exekutatuak izateko.
Ez dugu ezartzen ere (potentzialki transitiboa) sinadura fidagarriak edo jabe-fidagarritasuneko datubase bat GnuPG-n bezala.
Gainera, gutxienez giltza fidagarri batek egiaztatuta dauden erabiltzaile ID egiaztagiritu bat duten giltzak "baieztatua" bezala markatuko dira giltza zerrendan.