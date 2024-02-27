[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## Potwiedzenie klucza
Bez potwierdzenia nie masz pewności czy klucz odpowiada danej osobie.
Najłatwiejszą drogą potwierdzenia klucza jest zeskanowanie kodu QR lub wysłanie go przez NFC
W celu potwierdzenia kluczy pomiędzy więcej niż dwoma osobami, sugerujemy skorzystanie z dostępnej metody wymiany dla twoich kluczy.

## Stan klucza

<img src="status_signature_verified_cutout_24dp"/>  
Potwierdzony: Potwierdzono już ten klucz, np. skanując kod QR.  
<img src="status_signature_unverified_cutout_24dp"/>  
Niepotwierdzony: Ten klucz nie został jeszcze potwierdzony. Nie ma pewności, czy klucz rzeczywiście odpowiada konkretnej osobie.  
<img src="status_signature_expired_cutout_24dp"/>  
Wygaśnięty: Ten klucz nie jest już ważny. Tylko właściciel może przedłużyć jego ważność.  
<img src="status_signature_revoked_cutout_24dp"/>  
Wycofany: Ten klucz nie jest już ważny. Został odwołany przez swojego właściciela.

## Zaawansowane informacje
"Potwierdzenie klucza" w OpenKeychain jest realizowane poprzez stworzenie certyfikatu zgodnie ze standardem OpenPGP.
Ten certyfikat to ["ogólny certyfikat (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) opisanym w standardzie przez:
"Wystawca tego certyfikatu nie składa żadnego szczególnego zapewnienia co do tego, jak dobrze certyfikujący sprawdził, że właścicielem klucza jest w rzeczywistości osoba opisana przez ID użytkownika."

Tradycyjnie certyfikaty (również o wyższych poziomach certyfikacji, takich jak "pozytywne certyfikaty" (0x13)) są zorganizowane w OpenPGP's Web of Trust.
Nasz model potwierdzania kluczy jest znacznie prostszą koncepcją, która pozwala uniknąć powszechnych problemów użyteczności związanych z tym Web of Trust.
Zakładamy, że klucze są weryfikowane tylko do pewnego stopnia, który jest jeszcze na tyle użyteczny, że można go wykonać "w biegu".
Nie implementujemy również (potencjalnie przechodnich) podpisów zaufania ani bazy zaufania właściciela, jak w GnuPG.
Ponadto klucze, które zawierają co najmniej jeden identyfikator użytkownika poświadczony przez zaufany klucz, zostaną oznaczone w zestawieniach kluczy jako "potwierdzone".