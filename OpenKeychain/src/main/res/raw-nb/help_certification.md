[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## Oppsett av nøkkel
Without confirmation, you cannot be sure if a key really corresponds to a specific person.
Den enkleste måten å bekrefte en nøkkel er ved å skanne QR-koden eller bytte via NFC.
For bekreftelse av nøkler mellom fler enn to personer seg imellom anbefaler vi nøkkelutvekslingsmetoden tilgjengelig for deres nøkler.

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
"The issuer of this certification does not make any particular assertion as to how well the certifier has checked that the owner of the key is in fact the person described by the User ID."

Traditionally, certifications (also with higher certification levels, such as "positive certifications" (0x13)) are organized in OpenPGP's Web of Trust.
Our model of key confirmation is a much simpler concept to avoid common usability problems related to this Web of Trust.
Vi antar at nøklene er bekreftet nok til at de fremdeles er brukbare nok til å kunne kjøres "på sparket".
We also do not implement (potentially transitive) trust signatures or an ownertrust database like in GnuPG.
Furthermore, keys which contain at least one user ID certified by a trusted key will be marked as "confirmed" in the key listings.