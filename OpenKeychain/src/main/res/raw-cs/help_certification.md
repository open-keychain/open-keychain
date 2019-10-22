[//]: # (Poznámka: Prosím, vložte každou větu na zvláštní řádek, Transifex vkládá každý řádek do vlastního překladového pole!)

Potvrzení klíče
Bez potvrzení si nemůžete být jisti, zda se klíč vskutku vztahuje k jisté osobě.
Nejjednodušší způsob potvrzení klíče je sken QR kódu nebo výměna skrze NFC.
K potvrzení klíče mezi více než dvěma osobami navrhujeme použít metodu výměny klíčů dostupnou pro Vaše klíče.

## Stav klíče

<img src="status_signature_verified_cutout_24dp"/>  
Potvrzeno: Tento klíč jste již potvrdili, např. skenem QR kódu.  
<img src="status_signature_unverified_cutout_24dp"/>  
Nepotvrzeno: Tento klíč nebyl dosud potvrzen. Nemůžete si být jisti, zda se klíč vskutku vztahuje k jisté osobě.  
<img src="status_signature_expired_cutout_24dp"/>  
Propadlý: Tento klíč už není platný. Jen majitel může prodloužit jeho platnost.  
<img src="status_signature_revoked_cutout_24dp"/>  
Zrušen: Tento klíč už není platný. Byl zrušen majitelem.

## Pokročilé informace
"Potvrzení klíče" v OpenKeychain je implementováno založením osvědčení podle standardu OpenPGP.
Toto osvědčení ["obecné osvědčení (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) je popsáno standardem takto:
"Vydavatel tohoto osvědčení neportvrzuje nějakým zvláštním způsobem, jak dobře osvědčující prověřil, že majitel klíče je vskutku tou osobou charakterizovanou daným uživatelským ID."

Tradičně se osvědčení (také s vyšší certifikační úrovní, např. "pozitivní osvědčení" (0x13)) organizují v OpenPGP Web of Trust.
Náš model potvrzení klíče je mnohem jednodušší koncept, a to abychom se vyhnuli běžným problémům užitnosti související s Web of Trust.
Předpokládáme, že klíče jsou ověřeny jen do jisté míry, která je stále dost praktická, aby byla provedena "v chodu".
Navíc neimplementujeme (potenciálně přechodné) podpisy důvěry nebo databázi důvěry k vlastníkovi jako GnuPG.
Navíc klíče, které obsahují nejméně jedno uživatelské ID osvědčené důvěryhodným klíčem, budou označeny jako "potvrzené" v seznamech klíčů.