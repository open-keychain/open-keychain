[//]: # (NOTERING: Var vänlig och sätt varje mening på sin egen rad, Transifex sätter varje rad i sitt eget fält för översättningar!)

## Nyckelbekräftelse
Utan bekräftelse kan du inte vara säker på om en nyckel verkligen motsvarar en viss person.
Det enklaste sättet att bekräfta en nyckel är genom att skanna QR-koden eller att byta ut den via NFC.
För att bekräfta nycklar mellan fler än två personer så föreslår vi att du använder utbytesmetoden som är passande för dina nycklar.

## Status för nyckel

<img src="status_signature_verified_cutout_24dp"/>  
Bekräftat: Du har redan bekräftat den här nyckeln, t.ex genom att skanna QR-koden.  
<img src="status_signature_unverified_cutout_24dp"/>  
Obekräftad: Denna nyckel har inte bekräftats ännu. Du kan inte vara säker på om nyckeln verkligen motsvarar en viss person.  
<img src="status_signature_expired_cutout_24dp"/>  
Utgången: Den här nyckeln är inte längre giltig. Endast ägaren kan förlänga dess giltighet.  
<img src="status_signature_revoked_cutout_24dp"/>  
Återkallats: Denna nyckel är inte längre giltigt. Den har återkallats av dess ägare.

## Avancerad information
A "key confirmation" in OpenKeychain is implemented by creating a certification according to the OpenPGP standard.
This certification is a ["generic certification (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) described in the standard by:
"The issuer of this certification does not make any particular assertion as to how well the certifier has checked that the owner of the key is in fact the person described by the User ID."

Traditionally, certifications (also with higher certification levels, such as "positive certifications" (0x13)) are organized in OpenPGP's Web of Trust.
Our model of key confirmation is a much simpler concept to avoid common usability problems related to this Web of Trust.
We assume that keys are verified only to a certain degree that is still usable enough to be executed "on the go".
We also do not implement (potentially transitive) trust signatures or an ownertrust database like in GnuPG.
Furthermore, keys which contain at least one user ID certified by a trusted key will be marked as "confirmed" in the key listings.