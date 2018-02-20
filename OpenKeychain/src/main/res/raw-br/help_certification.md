[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## Key Confirmation
Without confirmation, you cannot be sure if a key really corresponds to a specific person.
The simplest way to confirm a key is by scanning the QR Code or exchanging it via NFC.
To confirm keys between more than two persons, we suggest using the key exchange method available for your keys.

## Key Status

<img src="status_signature_verified_cutout_24dp"/>  
Confirmed: You have already confirmed this key, e.g., by scanning the QR Code.  
<img src="status_signature_unverified_cutout_24dp"/>  
Unconfirmed: This key has not been confirmed yet. You cannot be sure if the key really corresponds to a specific person.  
<img src="status_signature_expired_cutout_24dp"/>  
Expired: This key is no longer valid. Only the owner can extend its validity.  
<img src="status_signature_revoked_cutout_24dp"/>  
Revoked: This key is no longer valid. It has been revoked by its owner.

## Advanced Information
A "key confirmation" in OpenKeychain is implemented by creating a certification according to the OpenPGP standard.
This certification is a ["generic certification (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) described in the standard by:
"The issuer of this certification does not make any particular assertion as to how well the certifier has checked that the owner of the key is in fact the person described by the User ID."

Traditionally, certifications (also with higher certification levels, such as "positive certifications" (0x13)) are organized in OpenPGP's Web of Trust.
Our model of key confirmation is a much simpler concept to avoid common usability problems related to this Web of Trust.
We assume that keys are verified only to a certain degree that is still usable enough to be executed "on the go".
We also do not implement (potentially transitive) trust signatures or an ownertrust database like in GnuPG.
Furthermore, keys which contain at least one user ID certified by a trusted key will be marked as "confirmed" in the key listings.