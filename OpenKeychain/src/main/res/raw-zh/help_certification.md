[//]: # (注意: 请把每个句子放在其本行中, Transifex把每一行放在它自己的位置！)

## 密钥确认
在没有进行确认之前,你无法确保一个密钥与特定的人的密钥是相符的
确认密钥相符的最简单方式是扫描二维码或者通过NFC交换
为了确认多于2个人的密钥是相符的, 我们建议使用密钥交换的方法进行确认.

## 密钥状态

<img src="status_signature_verified_cutout_24dp"/>  
已确认: 你已经通过例如二维码扫描这种方式确认了这个密钥  
<img src="status_signature_unverified_cutout_24dp"/>  
未确认: 这个密钥尚未被确认. 你无法确保这个密钥与指定的人的密钥是相同的.  
<img src="status_signature_expired_cutout_24dp"/>  
已过期: 这个密钥不再有效. 只有它的拥有者能扩展它的正确性.  
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