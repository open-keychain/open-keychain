[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## Oppsett av nøkkel
Uten bekreftelse, kan du ikke være sikker på at en nøkkel virkelig samsvarer med en gitt person.
Den enkleste måten å bekrefte en nøkkel er ved å skanne QR-koden eller bytte via NFC.
For bekreftelse av nøkler mellom fler enn to personer seg imellom, anbefaler vi nøkkelutvekslingsmetoden tilgjengelig for dine nøkler.

## Status for nøkkel

<img src="status_signature_verified_cutout_24dp"/>  
Bekreftet: Du har allerede bekreftet denne nøkkelen, eksempelvis ved å skanne QR-koden.  
<img src="status_signature_unverified_cutout_24dp"/>  
Ubekreftet: Denne nøkkelen har ikke blitt bekreftet enda. Du kan ikke være sikker på at nøkkelen tilhører rett person.  
<img src="status_signature_expired_cutout_24dp"/>  
Utløpt: Denne nøkkelen er ikke lenger gyldig. Bare eieren kan forlenge dens gyldighet.  
<img src="status_signature_revoked_cutout_24dp"/>  
Utløpt: Denne nøkkelen er ikke lenger gyldig. Den har blitt tilbakekalt av eieren.

## Avansert informasjon
En "nøkkel-bekreftelse" i OpenKeychain er implementert ved å opprette et sertifikat i henhold til OpenPGP-standarden.
Denne sertifiseringen er en ["vanlig sertifisering (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) beskrevet i standarden av:
"Utstederen av denne sertifiseringen gjør ingen antagelser om hvorvidt og hvor grundig sertifikatoren har sjekket at nøkkelens eier faktisk er personen beskrevet i bruker-ID-en.

Tradisjonelt, er sertifiseringer (også de høyere sertifikasjonsnivåene, som "positive sertifiseringer" (0x13)) organisert i OpenPGP sitt tillitsvev (Web of Trust).
Vår modell for nøkkelbekreftelse er et mye enklere konsept for å unngå vanlige problemer relatert til denne tillitsveven (Web of Trust).
Vi antar at nøklene er bekreftet nok til at de fremdeles er brukbare nok til å kunne kjøres "på sparket".
Vi implementerer heller ikke (potensielt transitive) tillitssignaturer eller en eierskapstillitsdatabase som i GnuPG.
Videre, nøkler som inneholder minst en bruker-ID sertifisert av en betrodd nøkkel vil bli merket som "bekreftet" i nøkkellistene.