[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

##金鑰確認
在經過確認之前，你無法確定這個金鑰是否真實屬於某個人。
確認金鑰的最簡單方式就是透過掃描QR code或是經由NFC交換。
要在多於兩個人之間確認彼此的金鑰，我們建議使用金鑰交換方式。

## 金鑰狀態

<img src="status_signature_verified_cutout_24dp"/>  
已確認：你已經確認了這個金鑰，例如透過掃描QR Code。  
<img src="status_signature_unverified_cutout_24dp"/>  
未確認：這個金鑰尚未經過確認，你無法確定這個金鑰是否真實屬於某個人。  
<img src="status_signature_expired_cutout_24dp"/>  
已過期：這個金鑰不再有效。只有金鑰擁有者可以展延有效期限。  
<img src="status_signature_revoked_cutout_24dp"/>  
已撤銷：這個金鑰已經被擁有者撤銷而失效。

##進階資訊
在OpenKeychain中，確認金鑰是透過根據OpenPGP標準簽發認證來達成。
這是一個“[一般認證 (0x10)](http://tools.ietf.org/html/rfc4880#section-5.2.1)”，規範描述如下：
“認證的發行者不就妥善檢查金鑰持有人與金鑰所示身份相符與否一事做出任何允諾。”

傳統上，包含更高等級的（像是主動型認證(0x13)）認證在內，認證是有組織的存在於OpenPGP信任網當中。
我們的金鑰確認模型採用一套相對簡單的概念，以避開普遍存在的信任網相關可用性問題。
我們假設金鑰只驗證到足以隨時隨地使用的程度。
同時我們暫時也不打算導入像是GnuPG的信任簽章或主觀信任資料庫。
此外，如果某金鑰含有至少一個被信任金鑰所認證的身分時，這把金鑰將被標記為已確認。