[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## Anahtar Onaylama
Onaylama olmadan, bir anahtarın gerçekten bir kişiye karşılık gelip gelmediğinden emin olamazsınız.
Bir anahtarı onaylamanın en basit yolu, QR Kodunu taramak ya da NFC yoluyla değiş tokuş etmektir.
İkiden fazla kişi arasındaki anahtarları onaylamak için kullanılabilir anahtarlarınız için anahtar değiş tokuşu yöntemini öneriyoruz.

## Anahtar Durumu

<img src="status_signature_verified_cutout_24dp"/>  
Onaylandı: Bu anahtarı örn. QR Kodu tarayarak zaten onayladınız.  
<img src="status_signature_unverified_cutout_24dp"/>  
Onaylanmadı: Bu anahtar henüz onaylanmadı. Anahtarın gerçekten bir kişiye karşılık gelip gelmediğinden emin olamazsınız.  
<img src="status_signature_expired_cutout_24dp"/>  
Süresi doldu: Bu anahtar artık geçerli değil. Yalnızca sahibi onun geçerliliğini uzatabilir.  
<img src="status_signature_revoked_cutout_24dp"/>  
Geri alındı: Bu anahtar artık geçerli değil. Sahibi onu geri aldı.

## Gelişmiş Bilgiler
OpenKeychain'de bir "anahtar onaylama", OpenPGP standardına göre bir sertifikalandırma oluşturarak uygulanır.
Bu sertifikalandırma, standartta şu şekilde açıklanan ["genel bir sertifikalandırmadır (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1):
"The issuer of this certification does not make any particular assertion as to how well the certifier has checked that the owner of the key is in fact the person described by the User ID."

Traditionally, certifications (also with higher certification levels, such as "positive certifications" (0x13)) are organized in OpenPGP's Web of Trust.
Our model of key confirmation is a much simpler concept to avoid common usability problems related to this Web of Trust.
We assume that keys are verified only to a certain degree that is still usable enough to be executed "on the go".
We also do not implement (potentially transitive) trust signatures or an ownertrust database like in GnuPG.
Furthermore, keys which contain at least one user ID certified by a trusted key will be marked as "confirmed" in the key listings.