[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

金鑰認證
這個身分識別尚未經過認證，你不能確認這個身分識別是否屬於真的某個人。
最簡單確認金鑰的方式就是透過掃描QR code或是經由NFC交換。
To confirm keys between more than two persons, we suggest to use the key exchange method available for your keys.

## 金鑰狀態

<img src="status_signature_verified_cutout_24dp"/>  
已認證： 你已經認證了這個金鑰，例如透過掃描QR Code。  
<img src="status_signature_unverified_cutout_24dp"/>  
未確認：這個身分識別尚未經過認證，你不能確認這個身分識別是否屬於真的某個人。  
<img src="status_signature_expired_cutout_24dp"/>  
已過期：這個金鑰因超過有效期限而失效。只有金鑰擁有者可以改變有效期限。  
<img src="status_signature_revoked_cutout_24dp"/>  
已撤銷：這個金鑰已經被擁有者撤銷而失效。

## Advanced Information
在OpenKeychain中，透過根據標準OpenPGP所建立的證書可以簡單的認證一個金鑰。
This certification is a ["generic certification (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) described in the standard by:
"The issuer of this certification does not make any particular assertion as to how well the certifier has checked that the owner of the key is in fact the person described by the User ID."

Traditionally, certifications (also with higher certification levels, such as "positive certifications" (0x13)) are organized in OpenPGP's Web of Trust.
Our model of key confirmation is a much simpler concept to avoid common usability problems related to this Web of Trust.
We assume that keys are verified only to a certain degree that is still usable enough to be executed "on the go".
We also do not implement (potentially transitive) trust signatures or an ownertrust database like in GnuPG.
Furthermore, keys which contain at least one user ID certified by a trusted key will be marked as "confirmed" in the key listings.