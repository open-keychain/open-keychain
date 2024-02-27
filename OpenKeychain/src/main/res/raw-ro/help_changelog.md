[//]: # (NOTĂ: Vă rugăm să puneți fiecare frază pe o linie separată, Transifex pune fiecare linie in câmpul ei de traducere!)

## 5.7
  * Corecții pentru Curve25519
  * Cifrul IDEA este acum considerat nesigur

## 5.6
  * Compatibilitate cu Android 10 și superior
  * Mai multe remedieri de erori

## 5.5
  * Rezolvă decriptarea din clipboard pe Android 10

## 5.4
  * Adăugați metoda WKD Advanced
  * Adăugați COTECH Security Key Shop

## 5.3
  * Utilizați keys.openpgp.org ca server de chei implicit

## 5.2
  * Îmbunătățirea importului de chei din clipboard

## 5.1
  * Suport pentru Ledger Nano S
  * Sprijină căutarea în Web Key Directory (WKD)
  * A rezolvat o potențială problemă de securitate API

## 5.0
  * Suport îmbunătățit pentru Autocrypt

## 4.9

  * Suportul Curve25519
  * Suport îmbunătățit pentru jetoane de securitate

## 4.8

  * Suport îmbunătățit pentru jetoane USB: Gnuk, modelele Nitrokey, modelele YubiKey 4
  * Funcție pentru a găsi poziția cititorului NFC al dispozitivului

## 4.7

  * Îmbunătățirea importului din clipboard
  * Noul asistent de creare a cheilor pentru jetoane de securitate
  * A eliminat setarea "time to live" a cache-ului de parole


## 4.6

  * Importă-ți cheile folosind noul nostru mecanism de transfer Wi-Fi securizat


## 4.5

  * Descrierea detaliată a problemelor de securitate
  * Afișați starea serverului de chei pentru fiecare cheie
  * Sprijin pentru EdDSA
  * Corectarea pgp.mit.edu (certificat nou)


## 4.4

  * Starea cheii noi afișează informații detaliate despre motivul pentru care o cheie este considerată nesigură sau defectuoasă.


## 4.3

  * Suport mai bun pentru chei mari
  * Corectarea importului de fișiere Gpg4win cu codificări rupte


## 4.2

  * Suport experimental pentru criptarea cu curbă eliptică cu jetoane de securitate
  * Ecranul de import al cheilor a fost reproiectat
  * Îmbunătățiri de design pentru listele cheie
  * Suport pentru adresele onion ale serverului de chei


## 4.1

  * O mai bună detectare a e-mailurilor și a altor conținuturi atunci când sunt deschise


## 4.0

  * Suport experimental pentru jetoane de securitate prin USB
  * Permiteți schimbarea parolei de chei decapate


## 3.9

  * Detectarea și manipularea datelor text
  * Îmbunătățiri de performanță
  * Îmbunătățiri ale interfeței pentru manipularea unui jeton de securitate


## 3.8

  * Reproiectat editare cheie
  * Alegeți să vă amintiți timpul individual atunci când introduceți parolele
  * Import de chei Facebook


## 3.7

  * Suport îmbunătățit pentru Android 6 (permisiuni, integrare în selecția de text)
  * API: Versiunea 10


## 3.6

  * Copii de rezervă criptate
  * Corecții de securitate bazate pe un audit de securitate extern
  * Expertul de creare a cheilor YubiKey NEO
  * Suport MIME intern de bază
  * Sincronizare automată a cheilor
  * Caracteristică experimentală: chei de legătură cu Github, conturi Twitter
  * Caracteristică experimentală: confirmarea cheilor prin fraze
  * Caracteristică experimentală: temă întunecată
  * API: Versiunea 9


## 3.5

  * Revocarea cheii la ștergerea cheii
  * Verificări îmbunătățite pentru criptografie nesigură
  * Corecție: Nu închideți OpenKeychain după prima dată când expertul reușește
  * API: Versiunea 8


## 3.4

  * Transfer anonim de chei prin Tor
  * Suport pentru Proxy
  * O mai bună gestionare a erorilor YubiKey


## 3.3

  * Un nou ecran de decriptare
  * Decriptarea mai multor fișiere în același timp
  * O mai bună gestionare a erorilor YubiKey


## 3.2

  * Prima versiune cu suport complet pentru YubiKey disponibil din interfața de utilizator: Editarea tastelor, legarea YubiKey la taste,...
  * Design material
  * Integrarea scanării codurilor QR (sunt necesare permisiuni noi)
  * Îmbunătățirea asistentului de creare a cheilor
  * Corectați contactele lipsă după sincronizare
  * Necesită Android 4
  * Ecranul cheie reproiectat
  * Simplificarea preferințelor criptografice, o selecție mai bună a cifrelor sigure
  * API: Semnături detașate, selecție liberă a cheii de semnare,...
  * Corectare: Unele chei valide au fost afișate revocate sau expirate
  * Nu acceptați semnături cu subchei expirate sau revocate
  * Suport Keybase.io în vizualizarea avansată
  * Metoda de a actualiza toate cheile deodată


## 3.1.2

  * Corectați exportul de chei în fișiere (acum pe bune)


## 3.1.1

  * Corectarea exportului de chei către fișiere (au fost scrise parțial)
  * Corectați crash-ul pe Android 2.3


## 3.1

  * Corectați crash-ul pe Android 5
  * Un nou ecran de certificare
  * Schimb securizat direct din lista de chei (bibliotecă SafeSlinger)
  * Noul QR Code program flow
  * Ecran de decriptare reproiectat
  * Noua pictogramă de utilizare și culori
  * Corectarea importului de chei secrete de la Symantec Encryption Desktop
  * Suport experimental pentru YubiKey: ID-urile subcheie sunt acum verificate corect


## 3.0.1

  * O mai bună gestionare a importurilor de chei mari
  * Îmbunătățirea selecției subcheie


## 3.0

  * Propuneți aplicații compatibile instalabile în lista de aplicații
  * Design nou pentru ecranele de decriptare
  * Multe corecții pentru importul de chei, de asemenea, corectează cheile dezbrăcate
  * Onorează și afișează indicatoarele de autentificare a cheilor
  * Interfață utilizator pentru a genera chei personalizate
  * Corectarea certificatelor de revocare a ID-ului de utilizator
  * Noua căutare în cloud (căutări peste keyserverele tradiționale și keybase.io)
  * Sprijin pentru scoaterea cheilor în interiorul OpenKeychain
  * Suport experimental pentru YubiKey: Suport pentru generarea și decriptarea semnăturilor


## 2.9.2

  * Reparați cheile sparte în 2.9.1
  * Suport experimental pentru YubiKey: Decriptarea funcționează acum prin API


## 2.9.1

  * Împărțiți ecranul de criptare în două
  * Corectarea manipulării indicatoarelor cheie (acum suportă cheile Mailvelope 0.7)
  * Manipulare îmbunătățită a frazei de trecere
  * Partajarea cheilor prin SafeSlinger
  * Suport experimental pentru YubiKey: Preferinta de a permite alte PIN-uri, în prezent funcționează doar semnarea prin API OpenPGP, nu în interiorul OpenKeychain.
  * Corectați utilizarea de chei decapate
  * SHA256 ca implicit pentru compatibilitate
  * API-ul de intenție s-a schimbat, consultați https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * OpenPGP API se ocupă acum de cheile revocate/expirate și returnează toate ID-urile de utilizator.


## 2.9

  * Rezolvarea erorilor introduse în v2.8
  * Suport ECC experimental
  * Suport experimental pentru YubiKey: Doar semnarea cu chei importate


## 2.8

  * Atât de multe bug-uri au fost corectate în această versiune încât ne concentrăm pe principalele noutăți
  * Editare cheie: un nou design minunat, revocarea cheii
  * Importul de chei: un nou design minunat, conexiuni sigure la serverul de chei prin hkps, rezolvarea serverului de chei prin înregistrări DNS SRV.
  * Un nou ecran pentru prima dată
  * Ecran nou de creare a cheii: autocompletare a numelui și a e-mailului pe baza conturilor personale Android
  * Criptarea fișierelor: un nou design minunat, suport pentru criptarea mai multor fișiere
  * Noi pictograme pentru a arăta starea cheii (de Brennan Novak)
  * Corecție importantă de erori: Importul colecțiilor mari de chei dintr-un fișier este acum posibil.
  * Notificare care arată frazele de acces în memoria cache
  * Cheile sunt conectate la contactele Android

Această versiune nu ar fi fost posibilă fără munca lui Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar

## 2.7

  * Purpuriu! (Dominik, Vincent)
  * Noul design de vizualizare a cheilor (Dominik, Vincent)
  * Noi butoane plate pentru Android (Dominik, Vincent)
  * Corecții API (Dominik)
  * Importare Keybase.io (Tim Bray)


## 2.6.1

  * Unele corecții pentru bug-uri de regresie


## 2.6

  * Certificări cheie (mulțumiri lui Vincent Breitmoser)
  * Suport pentru chei secrete parțiale GnuPG (mulțumiri lui Vincent Breitmoser)
  * Un nou design pentru verificarea semnăturii
  * Lungime cheie personalizată (mulțumiri lui Greg Witczak)
  * Corectează funcționalitatea de partajare din alte aplicații


## 2.5

  * Corectă decriptarea mesajelor/fișierelor OpenPGP simetrice
  * Refacerea ecranului de editare a cheilor (mulțumiri lui Ash Hughes)
  * Un nou design modern pentru ecranele de criptare/decriptare
  * OpenPGP API versiunea 3 (mai multe conturi API, corecturi interne, căutare de chei)


## 2.4
Mulțumim tuturor solicitanților de la Google Summer of Code 2014 care au făcut ca această versiune să fie bogată în funcții și fără erori!
Pe lângă câteva patch-uri mici, un număr important de patch-uri sunt realizate de următoarele persoane (în ordine alfabetică):
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser.

  * Noua listă de chei unificată
  * Amprenta cheie colorată
  * Suport pentru porturile serverului de chei
  * Dezactivați posibilitatea de a genera chei slabe
  * Mult mai multă muncă internă pe API
  * Certificarea ID-urilor de utilizator
  * Interogare a serverului de chei bazată pe o ieșire care poate fi citită automat
  * Blocați sertarul de navigare pe tablete
  * Sugestii pentru e-mailuri privind crearea de chei
  * Căutare în listele de chei publice
  * Și mult mai multe îmbunătățiri și corecturi...


## 2.3.1

  * Hotfix pentru crash la actualizarea de la versiuni vechi


## 2.3

  * Eliminați exportul inutil de chei publice atunci când exportați cheia secretă (mulțumiri lui Ash Hughes)
  * Fixarea datelor de expirare pe chei (mulțumiri lui Ash Hughes)
  * Mai multe corecturi interne la editarea tastelor (mulțumiri lui Ash Hughes)
  * Interogarea serverelor de chei direct din ecranul de import
  * Corectarea aspectului și a stilului de dialog pe Android 2.2-3.0
  * Reparați blocarea pe chei cu ID-uri de utilizator goale
  * Reparați blocarea și listele goale atunci când reveniți de pe ecranul de semnare
  * Bouncy Castle (bibliotecă criptografică) actualizată de la 1.47 la 1.50 și construită din sursă
  * Reparați încărcarea cheii de pe ecranul de semnare


## 2.2

  * Design nou cu sertar de navigare
  * Noul design al listei de chei publice
  * Noua vizualizare a cheilor publice
  * Corecții de erori pentru importul de chei
  * Certificare încrucișată cheie (mulțumiri lui Ash Hughes)
  * Gestionați parolele UTF-8 în mod corespunzător (mulțumiri lui Ash Hughes)
  * Prima versiune cu limbi noi (mulțumită contribuitorilor de pe Transifex)
  * Împărtășirea cheilor prin coduri QR corectate și îmbunătățite
  * Verificarea semnăturii pachetului pentru API


## 2.1.1

  * Actualizări API, pregătire pentru integrarea K-9 Mail


## 2.1

  * O mulțime de remedieri de erori
  * Noul API pentru dezvoltatori
  * PRNG bug fix de la Google


## 2.0

  * Reproiectare completă
  * Partajați cheile publice prin coduri QR, fascicul NFC
  * Chei de semnalizare
  * Încărcați chei pe server
  * Corectează problemele de import
  * Nou AIDL API


## 1.0.8

  * Suport de bază pentru serverul de chei
  * App2sd
  * Mai multe opțiuni pentru memoria cache a frazei de acces: 1, 2, 4, 8, ore
  * Traduceri: norvegiană Bokmål (mulțumiri, Sander Danielsen), chineză (mulțumiri, Zhang Fredrick)
  * Corecții de erori
  * Optimizări


## 1.0.7

  * A fost rezolvată o problemă cu verificarea semnăturii în cazul textelor cu linie nouă la sfârșit.
  * Mai multe opțiuni pentru timpul de viață al memoriei cache a frazei de acces (20, 40, 60 de minute)


## 1.0.6

  * Cont adăugând crash pe Froyo reparat
  * Ștergerea sigură a fișierelor
  * Opțiunea de a șterge fișierul cheie după import
  * Criptarea/decriptarea fluxurilor (galerie, etc.)
  * Noi opțiuni (limbă, forțați semnăturile v3)
  * Modificări ale interfeței
  * Corecții de erori


## 1.0.5

  * Traducere în germană și italiană
  * Pachet mult mai mic, datorită reducerii surselor BC
  * Noua interfață grafică a preferințelor
  * Ajustarea layout-ului pentru localizare
  * Semnătura corectării de erori


## 1.0.4

  * A rezolvat un alt crash cauzat de un bug SDK cu constructorul de interogări


## 1.0.3

  * A corectat erori în timpul criptării/semnării și, eventual, a exportului de chei.


## 1.0.2

  * Liste de chei filtrabile
  * O preselecție mai inteligentă a cheilor de criptare
  * Noua manipulare a intențiilor pentru VIEW și SEND, permite ca fișierele să fie criptate/decriptate în afara managerilor de fișiere.
  * Corecții și caracteristici suplimentare (preselecția tastelor) pentru K-9 Mail, este disponibilă o nouă versiune beta.


## 1.0.1

  * Listarea conturilor GMail a fost stricată în 1.0.0, reparată din nou


## 1.0.0

  * Integrare K-9 Mail, APG suportă versiunea beta a K-9 Mail
  * Suport pentru mai mulți manageri de fișiere (inclusiv ASTRO)
  * Traducere în slovenă
  * Noua bază de date, mult mai rapidă, mai puțină utilizare a memoriei
  * Defined Intents și furnizor de conținut pentru alte aplicații
  * Corecții de erori