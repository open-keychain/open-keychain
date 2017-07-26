[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## Avaimen varmennus
Ilman vahvistusta et voi olla varma, että avain oikeasti kuuluu tietylle henkilölle.
Yksinkertaisin tapa avaimen varmentamiseksi on skannata QR-koodi tai vaihtaa se NFC:n kautta.
To confirm keys between more than two persons, we suggest using the key exchange method available for your keys.

## Avaimen tila

<img src="status_signature_verified_cutout_24dp"/>  
Varmennettu: Sinä olet jo varmentanut tämän avaimen, esim. skannaamalla QR-koodin.  
<img src="status_signature_unverified_cutout_24dp"/>  
Varmentamaton: Tätä avainta ei ole vielä varmennettu. Et voi olla varma, että avain oikeasti kuuluu tietylle henkilölle.  
<img src="status_signature_expired_cutout_24dp"/>  
Vanhentunut: Tämä avain ei ole enää voimassa. Vain avaimen omistaja voi jatkaa sen voimassaoloa.  
<img src="status_signature_revoked_cutout_24dp"/>  
Kumottu: Tämä avain ei ole enää voimassa. Avaimen omistaja on kumonnut sen. 

## Lisätietoa
A "key confirmation" in OpenKeychain is implemented by creating a certification according to the OpenPGP standard.
This certification is a ["generic certification (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) described in the standard by:
"The issuer of this certification does not make any particular assertion as to how well the certifier has checked that the owner of the key is in fact the person described by the User ID."

Traditionally, certifications (also with higher certification levels, such as "positive certifications" (0x13)) are organized in OpenPGP's Web of Trust.
Our model of key confirmation is a much simpler concept to avoid common usability problems related to this Web of Trust.
We assume that keys are verified only to a certain degree that is still usable enough to be executed "on the go".
We also do not implement (potentially transitive) trust signatures or an ownertrust database like in GnuPG.
Furthermore, keys which contain at least one user ID certified by a trusted key will be marked as "confirmed" in the key listings.