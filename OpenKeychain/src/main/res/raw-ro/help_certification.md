[//]: # (NOTĂ: Vă rugăm să puneți fiecare frază pe o linie separată, Transifex pune fiecare linie in câmpul ei de traducere!)

## Confirmarea cheii
Fără confirmare, nu puteți fi sigur că o cheie corespunde cu adevărat unei anumite persoane.
Cel mai simplu mod de a confirma o cheie este scanarea codului QR sau schimbul prin NFC.
Pentru a confirma cheile între mai mult de două persoane, vă sugerăm să folosiți metoda de schimb de chei disponibilă pentru cheile dumneavoastră.

## Starea cheii

<img src="status_signature_verified_cutout_24dp"/>  
Confirmat: Ați confirmat deja această cheie, de exemplu, prin scanarea codului QR.  
<img src="status_signature_unverified_cutout_24dp"/>  
Neconfirmat: Această cheie nu a fost confirmată încă. Nu puteți fi sigur că această cheie corespunde într-adevăr unei anumite persoane.  
<img src="status_signature_expired_cutout_24dp"/>  
A expirat: Această cheie nu mai este valabilă. Numai proprietarul îi poate extinde valabilitatea.  
<img src="status_signature_revoked_cutout_24dp"/>  
Revocată: Această cheie nu mai este valabilă. Ea a fost revocată de către proprietarul său.

## Informații avansate
O "confirmare a cheii" în OpenKeychain este implementată prin crearea unei certificări în conformitate cu standardul OpenPGP.
Această certificare este o ["certificare generică (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) descrisă în standard prin:
"Emitentul acestei certificări nu face nicio afirmație specială cu privire la cât de bine a verificat certificatorul că proprietarul cheii este de fapt persoana descrisă de ID-ul utilizatorului."

În mod tradițional, certificările (de asemenea, cu niveluri de certificare mai înalte, cum ar fi "certificările pozitive" (0x13)) sunt organizate în Web of Trust al OpenPGP.
Modelul nostru de confirmare a cheilor este un concept mult mai simplu pentru a evita problemele comune de utilizare legate de această rețea de încredere.
Presupunem că cheile sunt verificate doar până la un anumit grad care este încă suficient de utilizabil pentru a fi executat "în mișcare".
De asemenea, nu implementăm semnături de încredere (potențial tranzitive) sau o bază de date de încredere proprie ca în GnuPG.
În plus, cheile care conțin cel puțin un ID de utilizator certificat de o cheie de încredere vor fi marcate ca fiind "confirmate" în listele de chei.