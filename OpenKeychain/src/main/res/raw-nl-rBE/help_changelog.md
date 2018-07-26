[//]: # (Ter info: Zet iedere zin op een eigen regel, Transifex zet iedere regel in zijn eigen vertalingsveld!)

## 5.1
  * Ondersteuning voor Ledger Nano S
  * Ondersteuning voor Web Key Directory (WKD) naslag
  * Potentieel API-beveiligingsfout opgelost

## 5.0
  * Verbeterde ondersteuning voor Autocrypt

## 4.9

  * Curve25519-ondersteuning
  * Verbeterde ondersteuning voor Security Tokens

## 4.8

  * Verbeterde ondersteuning voor USB-tokens: Gnuk, Nitrokey-modellen, YubiKey 4-modellen
  * Functie om de positie van de NFC-lezer van het apparaat te vinden

## 4.7

  * Verbeterde ondersteuning voor importeren uit klembord
  * Nieuwe sleutelaanmaakwizard voor Security Tokens
  * Instelling voor wachtwoordcachetijd verwijderd


## 4.6

  *Importeer je sleutels met onze nieuwe beveiligde wifi-overdracht


## 4.5

  * Gedetailleerde beschrijving van beveiligingsproblemen
  * Weergeven van sleutelstatus per sleutel
  * Ondersteuning voor EdDSA
  * Oplossing voor pgp.mit.edu (nieuw certificaat)


## 4.4

  * Nieuw overzicht met gedetailleerde informatie over sleutelstatus, waarom een sleutel beschouwd wordt als onveilig of defect


## 4.3

  * Betere ondersteuning voor grote sleutels
  * Importeren van Gpg4win-bestanden met beschadigde coderingen hersteld


## 4.2

  * Experimentele ondersteuning voor Elliptic Curve Encryption met Security Tokens
  * Nieuw design voor sleutelimporteringsscherm
  * Verbeterd design voor sleutellijsten
  * Ondersteuning voor sleutelserver-.onion-adressen


## 4.1

  * Betere herkenning van e-mails en andere inhoud bij openen


## 4.0

  * Experimentele ondersteuning voor Security Tokens via USB
  * Sta wachtwoordverandering to van gestripte sleutels


## 3.9

  * Detectie en afhandeling van tekstuele data
  * Snelheidsverbeteringen
  * Verbeteringen aan de gebruikersinterface voor afhandeling van het Security Token


## 3.8

  * Sleutelbewerking opnieuw ontworpen
  * Kies tijdsduur voor het onthouden van wachtwoorden individueel
  * Importeren van Facebook-sleutels


## 3.7

  * Verbetering van ondersteuning voor Android 6 (permissies, integratie in tekstselectie)
  * API: Versie 10


## 3.6

  * Versleutelde backups
  * Beveiligingsoplossingen gebaseerd op externe audit
  * Sleutelaanmaakwizard voor YubiKey NEO 
  * Basisondersteuning interne MIME
  * Automatische sleutelsynchronisatie
  * Experimentele functionaliteit: koppel sleutels aan accounts op Github, Twitter.
  * Experimentele functionaliteit: sleutelbevestiging via zinnen
  * Experimentele functionaliteit: donker thema
  * API: Versie 9


## 3.5

  * Sleutel intrekken bij verwijderen
  * Verbeterde controles voor onveilige cryptografie
  * Opgelost: Sluit OpenKeychain niet na eerste gebruik
  * API: versie 8


## 3.4

  * Anoniem sleutels downloaden via Tor
  * Proxy-ondersteuning
  * Beter verwerken van YubiKey-fouten


## 3.3

  * Nieuw ontsleutelingsscherm
  * Ontsleuteling van meerdere bestanden tegelijk
  * Beter verwerken van YubiKey-fouten


## 3.2

  * Eerste versie waarin YubiKey volledig wordt ondersteund vanuit de gebruikersinterface: sleutels aanmaken, YubiKey binden aan sleutels, …
  * Material design
  * Integratie van QR-code scannen (nieuwe permissies vereist)
  * Sleutelaanmaakwizard verbeterd
  * Probleem met ontbrekende contacten na synchronisatie opgelost
  * Vereist Android 4
  * Nieuw design voor sleutelscherm
  * Cryptovoorkeuren vereenvoudigd, betere selectie van veilige ciphers
  * API: ondertekeningen ontkoppeld, vrije selectie van ondertekeningssleutel, …
  * Oplossing voor probleem waarbij sommige geldige sleutels weergegeven werden als ingetrokken of verlopen
  * Aanvaard geen ondertekeningen door verlopen of ingetrokken subsleutels
  * Ondersteuning voor Keybase.io in geavanceerde modus
  * Methode om alle sleutels tegelijk bij te werken


## 3.1.2

  * Oplossing voor exporteren van sleutels naar bestanden (deze keer echt)


## 3.1.1

  * Oplossing voor exporteren van sleutels naar bestanden (ze werden maar gedeeltelijk geschreven)
  * Oplossing voor crash op Android 2.3


## 3.1

  * Oplossing voor crash op Android 5
  * Nieuw certificeerscherm
  * Veilig uitwisselen vanuit sleutellijst (SafeSlinger bibliotheek)
  * Nieuwe QR code programma flow
  * Nieuw design voor ontcijferingsscherm
  * Nieuw icoon en kleuren
  * Oplossing voor importeren van geheime sleutels van Symantec Encryption Desktop
  * Experimentele ondersteuning voor YubiKey: subsleutel-ID's worden nu correct gecontroleerd


## 3.0.1

  * Grote sleutelimportaties worden beter behandeld
  * Subsleutelselectie verbeterd


## 3.0

  * Stel installeerbare compatibele apps voor in apps-lijst
  * Nieuw design voor ontcijferingsschermen
  * Veel oplossingen voor sleutelimporteren, lost ook gestripte sleutels op
  * Eer en toon sleutelauthenticatievlaggen
  * Gebruikersinterface om eigen sleutels aan te maken
  * Oplossing voor gebruikers-ID-intrekkingscertificaten
  * Nieuwe cloud search (zoekt op traditionele sleutelservers en keybase.io)
  * Ondersteuning voor strippen van sleutels in OpenKeychain
  * Experimentele ondersteuning voor YubiKey: ondersteuning voor aanmaken en ontsleutelen van ondertekeningen


## 2.9.2

  * Oplossing voor gebroken sleutels in 2.9.1
  * Experimentele ondersteuning voor YubiKey: ontsleuteling werkt nu via API


## 2.9.1

  * Deel versleutelingsscherm in twee
  * Oplossing voor sleutelvlaggen (ondersteunt nu Mailvelope 0.7 sleutels)
  * Verbeterde behandeling van wachtwoorden
  * Sleutels delen via SafeSlinger
  * Experimentele ondersteuning voor YubiKey: instelling om andere pincodes toe te laten, momenteel werkt ondertekenen enkel via de OpenPGP-API, niet binnen OpenKeychain
  * Oplossing voor gestripte sleutels
  * SHA256 als standaard voor compatibiliteit
  * Intent API is veranderd, zie https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * OpenPGP API behandelt nu ingetrokken/verlopen sleutels en geeft alle gebruikers-ID's weer


## 2.9

  * Oplossing voor crashes geïntroduceerd in v2.8
  * Experimentele ondersteuning voor ECC
  * Experimentele ondersteuning voor YubiKey: enkel ondertekenen met geïmporteerde sleutels


## 2.8

  * Er zijn zoveel bugs opgelost in deze release dat we kunnen focussen op de belangrijke nieuwe mogelijkheden
  * Sleutels wijzigen: nieuw design, sleutels intrekken
  * Sleutels importeren: nieuw design, veilige verbindingen met sleutelservers via hkps, sleutelserver resolving via DNS SRV records
  * Nieuw eerste gebruiksscherm
  * Nieuw scherm voor sleutels aanmaken: automatisch aanvullen van naam en e-mailadres gebaseerd op persoonlijke Android-accounts
  * Bestandsversleuteling: nieuw design, ondersteuning voor versleutelen van meerdere bestanden
  * Nieuwe iconen om sleutelstatus weer te geven (door Brennan Novak)
  * Belangrijke bugfix: importeren van grote sleutelbossen uit een bestand is nu mogelijk
  * Melding om gecachete wachtwoorden weer te geven
  * Sleutels worden verbonden aan Android's contacten

Deze release zou niet mogelijk zijn zonder het werkt van Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar

## 2.7

  * Paars! (Dominik, Vincent)
  * Nieuw sleutel scherm design (Dominik, Vincent)
  * Nieuwe platte Android toetsen (Dominik, Vincent)
  * API fixes (Dominik)
  * Keybase.io import (Tim Bray)


## 2.6.1

  * Enkele oplossingen voor regressies


## 2.6

  * Sleutelcertificaties (dank aan Vincent Breitmoser)
  * Ondersteuning voor GnuPG gedeeltelijke geheime sleutels (dank aan Vincent Breitmoser)
  * Nieuw design voor ondertekeningsverificatie
  * Aangepaste sleutellengte (dank aan Greg Witczak)
  * Oplossing voor delen vanuit andere apps


## 2.5

  * Oplossing voor ontsleuteling van symmetrische OpenPGP berichten/bestanden
  * Nieuw ontwerp voor sleutelbewerkingsscherm (dank aan Ash Hughes)
  * Nieuw modern design voor versleutelings-/ontsleutelingsschermen
  * OpenPGP API versie 3 (meerdere api accounts, interne fixes, sleutel lookup)


## 2.4
Bedankt aan alle deelnemers van Google Summer of Code 2014 die deze release vol met nieuwe mogelijkheden en vrij van bugs gemaakt hebben!
Naast vele kleine patches werden ook een aanzienlijk aantal patches gemaakt door de volgende personen (in alfabetische volgorde):
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser.

  * Nieuwe geünificeerde sleutellijst
  * Gekleurde sleutelvingerafdruk
  * Ondersteuning voor sleutelserver-poorten
  * Zet mogelijkheid om zwakke sleutels aan te maken uit
  * Veel meer intern werk aan API
  * Certificeer gebruikers-ID's
  * Sleutelserverzoekopdracht gebaseerd op machine-leesbare output
  * Zet navigatiedrawer op tablets vast
  * Suggesties voor e-mailadress bij aanmaken van sleutels
  * Zoeken in publieke sleutellijsten
  * En veel meer verbeteringen en oplossingen…


## 2.3.1

  * Hotfix voor crash bij upgraden van oude versies


## 2.3

  * Verwijder onnodige export van publieke sleutels bij exporteren van geheime sleutel (dank aan Ash Hughes)
  * Oplossing voor verloopdata op sleutels (dank aan Ash Hughes)
  * Meer interne fixes by bewerken van sleutels (dank aan Ash Hughes)
  * Zoeken op sleutelservers rechtstreeks van importscherm
  * Oplossing voor layout en dialoogstijl op Android 2.2-3.0
  * Oplossing voor crash op sleutels met lege gebruikers-ID's
  * Oplossing voor crash en lege lijsten bij terugkeren van ondertekeningsscherm
  * Bouncy Castle (cryptografie bibliotheek) bijgewerkt van 1.47 naar 1.50 en versie van bron
  * Oplossing voor uploaden van sleutel van ondertekeningsscherm


## 2.2

  * Nieuw design met navigatiebalk
  * Nieuw publieke sleutellijst design
  * Nieuwe publieke sleutel view
  * Bugfixes voor importeren van sleutels
  * Sleutel certificatie (dank aan Ash Hughes)
  * Behandel UTF-8 wachtwoorden correct (dank aan Ash Hughes)
  * Eerste versie met nieuwe talen (dank aan de medewerkers op Transifex)
  * Delen van sleutels via QR codes opgelost en verbeterd
  * Pakketondertekeningsverificatie voor API


## 2.1.1

  * API updates, voorbereiding voor K-9 Mail integratie


## 2.1

  * Veel bugfixes
  * Nieuwe API voor ontwikkelaars
  * PRNG bug fix door Google


## 2.0

  * Volledig nieuw design
  * Publieke sleutels delen via QR codes, NFC beam
  * Sleutels ondertekenen
  * Upload sleutels naar server
  * Importeerproblemen opgelost
  * Nieuwe AIDL API


## 1.0.8

  * Basisondersteuning voor sleutelservers
  * App2SD
  * Meer keuzes voor wachtwoordcache: 1, 2, 4, 8 uur
  * Vertalingen: Noors (Bokmål) (bedankt Sander Danielsen), Chinees (bedankt Zhang Fredrick)
  * Bugfixes
  * Optimalisaties


## 1.0.7

  * Probleem met ondertekeningsverificatie van text met achterlopende newline opgelost
  * Meer opties voor wachtwoord cachetijd (20, 40, 60 min.)


## 1.0.6

  * Crash bij toevoegen van account op Froyo opgelost
  * Veilige bestandsverwijdering
  * Optie om sleutelbestand na importeren te verwijderen
  * Stream versleuteling/ontsleuteling (galerij, enz.)
  * Nieuwe opties (taal, forceer v3 ondertekeningen)
  * Wijzigingen in interface
  * Bugfixes


## 1.0.5

  * Duitse en Italiaanse vertaling
  * Veel kleiner pakket, door verminderde BC bronnen
  * Nieuwe GUI voor voorkeuren
  * Wijzigingen in opmaak voor lokalisatie
  * Bugfix voor ondertekeningen


## 1.0.4

  * Een andere crash veroorzaakt door een SDK-bug met de query builder opgelost


## 1.0.3

  * Crashes tijdens versleuteling/ondertekenen en mogelijk sleutelexportatie opgelost


## 1.0.2

  * Filterbare sleutellijsten
  * Slimmere preselectie van cryptosleutels
  * Nieuwe intents voor VIEW en SEND, laat toe bestanden te versleutelen/ontsleutelen vanuit bestandsbeheerders
  * Oplossingen en nieuwe functies (sleutel preselectie) voor K-9 Mail, nieuwe beta build beschikbaar


## 1.0.1

  * GMail account lijsten was stuk in 1.0.0, weer opgelost


## 1.0.0

  * K-9 Mail integratie, APG ondersteunende beta versie van K-9 Mail
  * Ondersteuning voor meer bestandsbeheerders (waaronder ASTRO)
  * Sloveense vertaling
  * Nieuwe database, veel sneller, minder geheugengebruik
  * Intents en content provider voor andere apps gedefinieerd
  * Bugfixes