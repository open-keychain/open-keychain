[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## 키 확인
확인이 없이 정말 키가 특정 사람에게 속하는 지 확신할 수 없습니다.
가장 쉽게 키를 확인하는 방법은 QR 코드를 스캔하거나 NFC를 통해 키를 교환하는 것 입니다.
둘 이상의 사람 사이에서 키를 확인하려면, 키에 사용할 수있는 키 교환 방법을 사용하길 권합니다.

## 키 

<img src="status_signature_verified_cutout_24dp"/>  
확인됨: 당신은 이미 이 키를 확인했습니다. (예를 들어서 QR 코드를 스캔해서)  
<img src="status_signature_unverified_cutout_24dp"/>  
확인되지 않음: 이 키는 아직 확인되지 않았습니다. 당신은 이 키가 정말로 특정한 사람에게 속하는 지 확신할 수 없습니다.  
<img src="status_signature_expired_cutout_24dp"/>  
만료됨: 이 키는 더이상 유효하지 않습니다. 오직 소유자만이 키의 유효기간을 연장할 수 있습니다.  
<img src="status_signature_revoked_cutout_24dp"/>  
무효화됨: 이 키는 더이상 유효하지 않습니다. 이 키는 소유자에 의해 무효화 되었습니다.

## 자세한 정보
OpenKeychain의 "키 확인" 은 OpenPGP 표준에 따라 인증을 만드는 방식으로 구현되었습니다.
이 인증은 ["generic certification (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) 이며, 다음과 같이 표준에 정의되었습니다:
"인증의 발행인은 인증자가 얼마나 잘 키의 소유자가 유저 id에 묘사된 사람인지 확인했다는 특별한 주장을 하지 않습니다."

전통적으로, 인증은 ("positive certifications" (0x13)와 같은 더 높은 인증 레벨에서도) OpenPGP의 Web of Trust에 의해 조직되어 있습니다.
우리의 키 확인 모델은 Web of Trust와 관련된 일반적인 사용성 문제를 해결하기 위한 더 간단한 개념입니다.
우리는 키가 "계속 진행되어" 실행되기에 여전히 충분한 특정한 정도로만 확인되었다고 가정합니다.
우리는 또한 (잠재적으로 믿을 수 있는) 신뢰 서명이나 소유자 신뢰 데이터베이스를 GnuPG처럼 구현하지 않습니다.
더 나아가, 최소 하나의 유저 ID에 의해 인증된 키를 포함하는 키들은 키 목록에 "확인됨" 이라고 표시 될 것입니다.