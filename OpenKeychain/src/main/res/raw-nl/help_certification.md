[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## Sleutelbevestiging
Zonder bevestiging kan je niet zeker zijn dat een sleutel echt overeenkomt met een bepaald persoon.
De meest eenvoudige manier om een sleutel te bevestigen is door de QR-code te scannen of de sleutel uit te wisselen via NFC.
Om sleutels tussen meer dan twee personen te bevestigen stellen we voor om de sleuteluitwisselingsmethode beschikbaar voor je sleutels te gebruiken.

## Sleutelstatus

<img src="status_signature_verified_cutout_24dp"/>  
Bevestigd: je hebt deze sleutel al bevestigd, bijvoorbeeld door de QR-code te scannen.  
<img src="status_signature_unverified_cutout_24dp"/>  
Niet bevestigd: deze sleutel is nog niet bevestigd. Je kan niet zeker zijn dat de sleutel echt overeenkomt met een bepaald persoon.  
<img src="status_signature_expired_cutout_24dp"/>  
Verlopen: deze sleutel is niet meer geldig. Enkel de eigenaar kan de geldigheid verlengen.  
<img src="status_signature_revoked_cutout_24dp"/>  
Ingetrokken: deze sleutel is niet meer geldig. Ze is door de eigenaar ingetrokken.

## Geavanceerde informatie
Een "sleutelbevestiging" in OpenKeychain is geïmplementeerd door een certificatie aan te maken volgens de OpenPGP-standaard.
Deze certificatie is een ["generische certificatie (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) omschreven in de standaard als:
"De uitgever van deze certificatie maakt geen specifieke aanname over hoe goed de certificeerder heeft nagegaan dat de eigenaar van sleutel effectief de persoon is beschreven door het gebruikers-ID."

Traditioneel worden certificaties (ook met hogere certificatieniveau's, zoals "positieve certificaties" (0x13)) georganiseerd in OpenPGP's Web of Trust.
Ons model van sleutelbevestiging is een veel eenvoudige concept om veel voorkomende gebruiksproblemen gerelateerd aan dit Web of Trust te vermijden.
We nemen aan dat sleutels slechts geverifieerd zijn tot een bepaalde graad die nog steeds bruikbaar genoeg is om "onderweg" uitgevoerd te worden.
We implementeren ook geen (mogelijk transitieve) vertrouwensondertekeningen of een gebruikersvertrouwendatabase zoals in GnuPG.
Bovendien worden sleutels die minstens een gebruikers-ID gecertificeerd door een vertrouwde sleutel bevatten aangeduid als "bevestigd" in de sleutellijsten.