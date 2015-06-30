[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## Подтверждение ключей
Без подтверждения Вы не можете быть уверены, что ключ принадлежит определенному человеку.
Простейший способ подтвердить - отсканировать QR код или получить ключ через NFC.
Для подтверждения ключей более чем двух человек, мы рекомендуем использовать один из доступных методов обмена ключами.

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
"Подтверждение ключей" в OpenKeychain реализовано методом сертификации, согласно стандарту OpenPGP.
Эта сертификация представляет собой ["generic certification (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) , описанной в стандарте:
"Издатель такой подписи (поручитель) никак не оговаривает, что провёл какую-то проверку ключа и его связь с лицом, чьё имя указано в сертификате."

Traditionally, certifications (also with higher certification levels, such as "positive certifications" (0x13)) are organized in OpenPGP's Web of Trust.
Our model of key confirmation is a much simpler concept to avoid common usability problems related to this Web of Trust.
We assume that keys are verified only to a certain degree that is still usable enough to be executed "on the go".
We also do not implement (potentially transitive) trust signatures or an ownertrust database like in GnuPG.
Furthermore, keys which contain at least one user ID certified by a trusted key will be marked as "confirmed" in the key listings.