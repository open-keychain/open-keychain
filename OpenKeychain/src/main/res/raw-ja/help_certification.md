[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## 鍵の検証
検証しなければ、鍵が本当に特定の人物に対応するかわかりません。
鍵検証の最もシンプルな方法は QR コードのスキャンもしくは NFC での鍵交換です。
2 人以上の間で鍵を確認するため、お使いの鍵で使用できる鍵交換方法を使用することをお勧めします。

## 鍵ステータス

<img src="status_signature_verified_cutout_24dp"/>  
検証済み: すでに鍵を検証しました。e.g. QR コードをスキャン済み。  
<img src="status_signature_unverified_cutout_24dp"/>  
未検証: 鍵がまだ検証されていません。この鍵が特定の個人と結び付くか確定できません。  
<img src="status_signature_expired_cutout_24dp"/>  
有効期限切れ: 鍵はすでに無効になりました。鍵の主だけが鍵の有効期間を延長できます。  
<img src="status_signature_revoked_cutout_24dp"/>  
失効: 鍵はすでに無効になりました。鍵は所有者によって無効化されたようです。

## 詳細情報
OpenKeychain での「鍵の検証」は OpenPGP 標準に準拠した証明書を生成するよう実装されています。
標準では、この証明書は ["Generic certification (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) として以下のように記述されています:
"The issuer of this certification does not make any particular assertion as to how well the certifier has checked that the owner of the key is in fact the person described by the User ID. (この証明書の発行者は、認証者が鍵の所有者を実際にユーザー ID が示す人物であったことをどの程度確認したかについて、特別に主張しません)" 

歴史的に、証明 (または、"Positive certifications" (0x13) のような高度な証明) は OpenPGP による Web of Trust (信頼の輪) として組織されます。
私たちの鍵の証明モデルはとてもシンプルなコンセプトによって、信頼の輪に起因する一般的なユーザビリティの問題を回避します。
私たちは、「外出先」で実行するのに十分な程度の鍵の検証を想定しています。
また、GnuPG のような (他動的になりうる) 信頼署名や独自の信頼データベースを実装しません。
さらに、信頼された鍵によって証明された少なくとも 1 つのユーザー ID を含む鍵は、鍵リストで「確認済み」と表示します。