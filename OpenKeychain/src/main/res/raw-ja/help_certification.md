[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## 鍵の検証
キーが本当に特定の人物に対応するか検証しないで、あなたは使うことができません。
鍵の検証の最も単純な方法はQRコードのスキャンもしくはNFCでの交換をすることです。
2人以上の間で鍵の検証するなら、あなたの鍵にある鍵交換メソッドで使えるものを提案します。

## 鍵ステータス

<img src="status_signature_verified_cutout_24dp"/>  
検証済み:あなたは既に鍵を検証しています、e.g.QRコードのスキャン。  
<img src="status_signature_unverified_cutout_24dp"/>  
未検証: この鍵はまだ検証されていません。あなたはこの鍵が特定の個人と結び付くとして利用することができません。  
<img src="status_signature_expired_cutout_24dp"/>  
期限切れ: この鍵はすでに有効ではありません。鍵の主だけが鍵の有効期間を拡大することができます。  
<img src="status_signature_revoked_cutout_24dp"/>  
破棄済み: この鍵は有効ではありません。鍵の主がすでに破棄しています。

## 詳細情報
OpenKeychainでの"鍵の検証"はOpenPGP標準に準拠した証明を生成する実装がなされています。
この証明は ["汎用証明 (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) として標準に以下として記述されています:
"The issuer of this certification does not make any particular assertion as to how well the certifier has checked that the owner of the key is in fact the person described by the User ID."

歴史的に、証明(またより高いレベルの証明、"肯定的な証明" (0x13))  は OpenPGPによるWeb of Trustとして組織化されます。
われわれの鍵の証明モデルはとてもシンプルなコンセプトによって関連する一般的なユーザビリティの問題を回避する概念です。
We assume that keys are verified only to a certain degree that is still usable enough to be executed "on the go".
We also do not implement (potentially transitive) trust signatures or an ownertrust database like in GnuPG.
Furthermore, keys which contain at least one user ID certified by a trusted key will be marked as "confirmed" in the key listings.