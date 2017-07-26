[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

##金鑰確認
未經驗證之前，你無法確定這個金鑰是否真的屬於特定人。
認證金鑰的最簡單方式就是透過掃描QR code或是經由NFC交換。
To confirm keys between more than two persons, we suggest using the key exchange method available for your keys.

## 金鑰狀態

<img src="status_signature_verified_cutout_24dp"/>  
已驗證：你已經確認了這個金鑰，例如透過掃描QR Code。  
<img src="status_signature_unverified_cutout_24dp"/>  
未驗證：這個金鑰尚未經過確認，你無法確定這個金鑰是否真實屬於特定人。  
<img src="status_signature_expired_cutout_24dp"/>  
已過期：這個金鑰不再有效。只有金鑰擁有者可以展延有效期限。  
<img src="status_signature_revoked_cutout_24dp"/>  
已撤銷：這個金鑰已經被擁有者撤銷而失效。

##進階資訊
在OpenKeychain中，｢驗證金鑰」是透過製作一個符合OpenPGP標準的憑證來達成。
這是一個“[一般憑證 (0x10)](http://tools.ietf.org/html/rfc4880#section-5.2.1)”，其標準規範描述如下：
“憑證的發行者對於金鑰持有人與金鑰所示身份是否相符一事未做出任何主張。”

傳統上，憑證們（包含更高等級的憑證，例如｢證實憑證」(0x13)）在內），是依據OpenPGP信任網來組織架構。
我們的金鑰驗證模型採用一套簡單許多的概念，以避免信任網在實際操作上的常見困難。
我們假設金鑰只驗證到足以隨時隨地使用的程度。
同時我們（也許是暫時的）沒有導入像是GnuPG的信任簽章或主觀信任資料庫。
此外，如果某金鑰含有至少一個被已信任金鑰所驗證的身分時，這把金鑰將被標記為｢已驗證」。