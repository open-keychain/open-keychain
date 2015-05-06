[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## Подтверждение ключей
Без подтверждения Вы не можете быть уверены, что ключ принадлежит определенному человеку.
Простейший способ подтвердить - отсканировать QR код или получить ключ через NFC.
To confirm keys between more than two persons, we suggest to use the key exchange method available for your keys.

## Статус ключей

<img src="status_signature_verified_cutout_24dp"/>  
Подтверждён: Вы уже подтвердили этот ключ, напр. отсканировав QR код.  
<img src="status_signature_unverified_cutout_24dp"/>  
Не подтверждён: Этот ключ ещё не прошел проверку. Вы не можете быть уверены, что ключ принадлежит определенному человеку.  
<img src="status_signature_expired_cutout_24dp"/>  
Просрочен: Срок годности ключа истёк. Только его владелец может продлить срок годности.  
<img src="status_signature_revoked_cutout_24dp"/>  
Отозван: Этот ключ больше не действителен. Владелец ключа отозвал его.

## Подробная информация
A "key confirmation" in OpenKeychain is implemented by creating a certification according to the OpenPGP standard.
This certification is a ["generic certification (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) described in the standard by:
"The issuer of this certification does not make any particular assertion as to how well the certifier has checked that the owner of the key is in fact the person described by the User ID."

Traditionally, certifications (also with higher certification levels, such as "positive certifications" (0x13)) are organized in OpenPGP's Web of Trust.
Our model of key confirmation is a much simpler concept to avoid common usability problems related to this Web of Trust.
We assume that keys are verified only to a certain degree that is still usable enough to be executed "on the go".
We also do not implement (potentially transitive) trust signatures or an ownertrust database like in GnuPG.
Furthermore, keys which contain at least one user ID certified by a trusted key will be marked as "confirmed" in the key listings.