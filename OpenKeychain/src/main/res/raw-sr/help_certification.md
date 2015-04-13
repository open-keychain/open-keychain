[//]: #

## Потврда кључа
Без потврде не можете бити сигурни да ли кључ заиста одговара одређеној особи.
Најједноставнији начин да потврдите кључ је очитавањем бар-кôда или разменом преко НФЦ.
Да бисте потврдили кључеве између две или више особа, предлажемо да користите методу размене кључева која је доступна за ваш кључ.

## Стање кључа

<img src="status_signature_verified_cutout_24dp"/>  
Потврђен: Већ сте потврдили овај кључ, нпр. очитавањем бар-кôда.  
<img src="status_signature_unverified_cutout_24dp"/>  
Непотврђен: овај кључ још није потврђен. Не можете бити сигурни да ли кључ заиста одговара одређеној особи.  
<img src="status_signature_expired_cutout_24dp"/>  
Истекао: овај кључ више није исправан. Само му власник може продужити ваљаност.  
<img src="status_signature_revoked_cutout_24dp"/>  
Опозван: овај кључ више није исправан. Власник је опозвао кључ.

## Напредни подаци
„Потврда кључа“ у Отвореном кључарнику се реализује прављењем сертификације по ОпенПГП стандарду.
Ова сертификација је [„општа сертификација (0x10)“](http://tools.ietf.org/html/rfc4880#section-5.2.1) описана стандардом у:
"The issuer of this certification does not make any particular assertion as to how well the certifier has checked that the owner of the key is in fact the person described by the User ID."

Traditionally, certifications (also with higher certification levels, such as "positive certifications" (0x13)) are organized in OpenPGP's Web of Trust.
Our model of key confirmation is a much simpler concept to avoid common usability problems related to this Web of Trust.
We assume that keys are verified only to a certain degree that is still usable enough to be executed "on the go".
We also do not implement (potentially transitive) trust signatures or an ownertrust database like in GnuPG.
Furthermore, keys which contain at least one user ID certified by a trusted key will be marked as "confirmed" in the key listings.