[//]: # (NOTERING: Var vänlig och sätt varje mening på sin egen rad, Transifex sätter varje rad i sitt eget fält för översättningar!)

## Nyckelbekräftelse
Utan bekräftelse kan du inte vara säker på om en nyckel verkligen motsvarar en viss person.
Det enklaste sättet att bekräfta en nyckel är genom att skanna QR-koden eller att byta ut den via NFC.
För att bekräfta nycklar mellan fler än två personer så föreslår vi att du använder utbytesmetoden som är passande för dina nycklar.

## Nyckelstatus

<img src="status_signature_verified_cutout_24dp"/>  
Bekräftat: Du har redan bekräftat den här nyckeln, t.ex genom att skanna QR-koden.  
<img src="status_signature_unverified_cutout_24dp"/>  
Obekräftad: Denna nyckel har inte bekräftats ännu. Du kan inte vara säker på om nyckeln verkligen motsvarar en viss person.  
<img src="status_signature_expired_cutout_24dp"/>  
Utgången: Den här nyckeln är inte längre giltig. Endast ägaren kan förlänga dess giltighet.  
<img src="status_signature_revoked_cutout_24dp"/>  
Återkallats: Denna nyckel är inte längre giltigt. Den har återkallats av dess ägare.

## Avancerad information
En "nyckel bekräftelse" i OpenKeychain genomförs genom att skapa en certifiering enligt OpenPGP standard.
This certification is a ["generic certification (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) described in the standard by:
"The issuer of this certification does not make any particular assertion as to how well the certifier has checked that the owner of the key is in fact the person described by the User ID."

Traditionellt, certifieringar (även med högre certifieringsnivåer, såsom "positiva certifieringar" (0x13)) är organiserade i OpenPGP:s Web of Trust.
Vår modell av nyckel bekräftelse är en mycket enklare begrepp att undvika vanliga användbarhetsproblem i samband med Web of Trust.
Vi antar att nycklar verifieras endast till en viss grad som är fortfarande användbar nog att exekveras "on the go".
We also do not implement (potentially transitive) trust signatures or an ownertrust database like in GnuPG.
Furthermore, keys which contain at least one user ID certified by a trusted key will be marked as "confirmed" in the key listings.