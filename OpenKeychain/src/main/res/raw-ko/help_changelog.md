[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## 5.1
  * Support for Ledger Nano S
  * Support Web Key Directory (WKD) search
  * Fixed potential API security issue

## 5.0
  * Improved Autocrypt support

## 4.9

  * Curve25519 support
  * Improved support for security tokens

## 4.8

  * USB 토큰 지원 개선: Gnuk, Nitrokey 모델, YubiKey 4 모델
  * 기기의 NFC 판독기 위치를 찾는 기능

## 4.7

  * 클립보드로 부터 가져오기 개선
  * 보안 토큰용 새로운 키 생성 마법사
  * 패스워드 캐시 "동작 시간" 설정 제거


## 4.6

  * 새로운 보안 Wi-Fi 전송 메커니즘을 사용해 키 가져오기


## 4.5

  * 보안 문제에 대한 자세한 설명
  * 키 당 키서버 상태 표시
  * EdDSA 지원
  * pgp.mit.edu (새로운 인증서) 수정


## 4.4

  * New key status displays detailed information why a key is considered insecure or defective


## 4.3

  * 대형 키 지원 향상
  * Fix import of Gpg4win files with broken encodings


## 4.2

  * Experimental support for Elliptic Curve Encryption with Security Tokens
  * Redesigned key import screen
  * Design improvements to key lists
  * Support for keyserver onion addresses


## 4.1

  * Better detection of emails and other content when opened


## 4.0

  * Experimental support for Security Tokens over USB
  * Allow password changing of stripped keys


## 3.9

  * Detection and handling of text data
  * Performance improvements
  * UI improvements for Security Token handling


## 3.8

  * 키 편집 재설계
  * Choose remember time individually when entering passwords
  * Facebook 키 들여오기


## 3.7

  * Android 6 지원 개선 (사용 권한, 텍스트 선택 통합)
  * API: 버전 10


## 3.6

  * 백업 암호화
  * 외부 보안 감사를 바탕으로 한 보안 수정
  * YubiKey NEO 키 생성 마법사
  * 기본적인 내장 MIME 지원
  * 자동 키 동기화
  * 실험 기능: Github, Twitter 계정에 키 연결
  * 실험 기능: 문장을 통한 키 확인
  * 실험 기능: 어두운 테마
  * API: 버전 9


## 3.5

  * 키 삭제 시 키 무효화
  * 안전하지 않은 보안 방식에 대한 체크 강화
  *수정: 첫 실행 마법사가 성공한 후 OpenKeychain 닫지 않기
  * API: 버전 8


## 3.4

  * Tor를 통한 익명 키 다운로드
  * 프록시 지원
  * YubiKey 에러 처리 보완


## 3.3

  * 새로운 복호화 화면
  * 여러개의 파일을 한번에 복호화
  * YubiKey 에러 처리 보완


## 3.2

  * 유저 인터페이스에서 완전한 YubiKey를 지원하는 첫번째 버전: 키 수정, YubiKey를 키에 연결, 등...
  * Material 디자인
  * QR 코드 스캔 기능 내장 (새로운 권한 필요)
  * 키 생성 마법사 보완
  * 동기화 후 연락처 유실 수정
  * 안드로이드 4 필요
  * 키 화면 재디자인
  * 암호화 설정 간소화, 암호화 방식 선택 보완
  * API: 분리된 서명, 서명 키 선택 자유화, 등...
  * 수정: 일부 유효한 키가 만료되었거나 무효화 된 것으로 표시됨
  * 만료되었거나 무효화 된 서브키의 서명을 무효화
  * 자세히 보기에서 Keybase.io 지원
  * 모든 키를 한번에 업데이트 하는 방법


## 3.1.2

  * 키를 파일로 추출하는 기능을 수정 (이번엔 진짜로)


## 3.1.1

  * 키를 파일로 추출하는 기능을 수정 (부분만 구현되어 있었음)
  * 안드로이드 2.3에서의 강제 종료 수정


## 3.1

  * 안드로이드 5에서의 강제 종료 수정
  * 새로운 증명 화면
  * 키 리스트에서 바로 안전한 키 교환 (SafeSlinger 라이브러리)
  * 새로운 QR 코드 프로그램 구조
  * 복호화 화면 재디자인
  * 새로운 아이콘과 색상 사용
  * Symantec Encryption Desktop에서 비밀키 가져오기 수정
  * YubiKey 실험적 지원: 서브키의 ID가 이제 제대로 체크됩니다.


## 3.0.1

  * 큰 키 가져오기 개선
  * 서브키 선택 개선


## 3.0

  * 앱 목록에 설치 가능한 호환되는 앱 제안
  * 복호화 화면 새롭게 디자인
  * 키 가져오기 관련 많은 부분 수정, 간략화된 키 관련 수정
  * 키 인증 플래그 강조
  * 커스텀 키 생성을 위한 유저 인터페이스
  * 사용자 ID 해지 인증서 수정
  * 새로운 클라우드 검색 (기존의 키서버와 keybase.io에서 검색)
  * OpenKeychain 안에서 키 간략화 지원
  * YubiKey 실험적 지원: 서명 생성과 복호화 지원


## 2.9.2

  * 2.9.1에서 망가진 키 복구
  * YubiKey 실험적 지원: API를 통한 복호화 지원


## 2.9.1

  * 암호화 화면을 둘로 나누기
  * 키 플래그 다루기 수정 (Mailvelope 0.7 키 지원)
  * 암호 문구 다루기 개선
  * SafeSlinger를 통한 키 공유
  * YubiKey 실험적 지원: 다른 PIN을 추가할 수 있는 설정, 현재는 OpenKeychain 내부가 아닌 OpenPGP API를 통한 서명만 지원
  * 간략화 된 키 사용 수정
  * 호환성을 위해 SHA256를 기본값으로 변경
  * Intent API 변경, https://github.com/open-keychain/open-keychain/wiki/Intent-API 참조
  * OpenPGP API에서 이제 무효화/만료된 키를 다루고 모든 유저 id를 반환합니다.


## 2.9

  * 버전 2.8에서 생긴 강제종료 수정
  * 실험적인 ECC 지원
  * YubiKey 실험적 지원: 가져온 키로만 서명


## 2.8

  * 많은 버그가 이 릴리즈에서 수정되었기 때문에 새로운 기능에 대해서 집중하겠습니다.
  * 키 수정: 대단한 새 디자인, 키 무효화
  * 키 가져오기: 대단한 새 디자인, hkps를 통한 안전한 키서버 연결, DNS SRV 레코드를 통한 키서버 찾기
  * 새로운 첫 시작 화면
  * 새로운 키 생성 화면: 개인 안드로이드 개정에 따른 이름과 이메일 주소 자동완성
  * 파일 암호화: 대단한 새 디자인, 여러개의 파일 암호화 지원
  * 키 상태 표시에 새로운 아이콘 적용 (Brennan Novak 제작)
  * 중요한 버그 수정: 파일에서 큰 키 묶음 가져오기가 이제 가능합니다.
  * 캐시된 암호문구를 표시하는 알림
  * 키가 이제 안드로이드 연락처에 연동됩니다.

이 릴리즈는 Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar의 공로가 없이는 가능하지 못했을 것입니다.

## 2.7

  * 보라색! (Dominik, Vincent)
  * 새 키 보기 디자인 (Dominik, Vincent)
  * 새 납작한 안드로이드 버튼 (Dominik, Vincent)
  * API 수정 (Dominik)
  * Keybase.io 에서 가져오기 (Tim Bray)


## 2.6.1

  * 다시 나타난 버그 일부 수정


## 2.6

  * 키 인증 (Vincent Breitmoser씨 감사합니다)
  * GnuPG 부분 비밀키 지원 (Vincent Breitmoser씨 감사합니다)
  * 서명 검증 화면에 새로운 디자인 적용
  * 커스텀 키 길이 (Greg Witczak씨 감사합니다)
  * 다른 앱에서의 공유 기능 수정


## 2.5

  * 대칭 암호화 OpenPGP 메세지/파일 복호화 지원
  * 키 수정 화면 다시 설계 (Ash Hughes씨 감사합니다)
  * 암호화/복호화 화면에 새 모던 디자인 적용
  * OpenPGP API 버전 3 (여러개의 api 계정, 내부적 수정, 키 검색)


## 2.4
이 릴리즈의 기능을 풍부하게, 그리고 버그가 없게 만들어 주신 Google Summer of Code 2014 지원자들 모두에게 감사드립니다.
여러개의 작은 패치를 제외하고, 주목할 만한 숫자의 패치가 다음 사람들에 의해 만들어졌습니다 (알파벳 순서로):
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser.

  * 새로운 통합 키 리스트
  * 키 지문 표시에 색상 사용
  * 키서버 포트 설정 지원
  * 약한 키를 생성할 가능성을 비활성화
  * API에 아주 많은 내부적인 작업
  * 유저 id 검증
  * 기계가 읽을 수 있는 출력값에 기반한 키서버 검색
  * 태블릿에서 네비게이션 드로워 잠금
  * 키 생성 시 이메일 주소 제안
  * 공개 키 리스트에서 검색
  * 그리고 더 많은 개선과 수정들...


## 2.3.1

  * 오래된 버전에서 업그레이드 시 강제 종료되는 문제에 대한 핫픽스


## 2.3

  * 비밀키를 내보낼 때 필요하지 않은 공개키 내보내기 삭제 (Ash Hughes씨 감사합니다)
  * 키에 만료 날짜 설정하기 수정 (Ash Hughes씨 감사합니다)
  * 키 편집에 대한 더 많은 내부적 수정 (Ash Hughes씨 감사합니다)
  * 가져오기 화면에서 바로 키서버 검색
  * 안드로이드 2.2-3.0에서의 레이아웃과 다이얼로그 스타일 수정
  * 빈 유저 id를 가진 키를 처리할 때의 강제종료 수정
  * 서명 화면에서 돌아올 때 빈 리스트와 강제종료 수정
  * Bouncy Castle (암호화 라이브러리) 버전 1.47에서 1.50으로 업데이트와 소스에서 빌드
  * 서명 화면에서 키 업로드 수정


## 2.2

  * 네비게이션 드로워에 새로운 디자인 적용
  * 새로운 공개 키 리스트 디자인
  * 새로운 공개 키 보기
  * 키 가져오기에 대한 버그 수정
  * 키 교차 검증 (Ash Hughes씨 감사합니다)
  * UTF-8 비밀번호 제대로 다루기 (Ash Hughes씨 감사합니다)
  * 새로운 언어와 함께하는 첫번째 버전 (Transifex의 기여자님들 감사합니다)
  * QR 코드를 통한 키 공유 수정 및 개선
  * API를 위한 패키지 서명 검증


## 2.1.1

  * API 업데이트, K-9 Mail 통합을 위한 준비


## 2.1

  * 많은 버그 수정
  * 개발자들을 위한 새 API
  * 구글에 의한 PRNG (난수생성기) 버그 수정 반영


## 2.0

  * 완전한 재디자인
  * QR 코드와 NFC 빔을 통한 공개키 공유
  * 키 서명
  * 키를 서버에 업로드
  * 가져오기 문제 수정
  * 새로운 AIDL API


## 1.0.8

  * 기본적인 키서버 지원
  * App2sd 지원
  * 암호구 캐시 시간 설정: 1, 2, 4, 8시간
  * 번역본: 노르웨이어 Bokmål (Sander Danielsen님께 감사), 중국어 (Zhang Fredrick님께 감사)
  * 버그 수정
  * 최적화


## 1.0.7

  * 개행이 따라오는 텍스트의 서명 검증에 관한 문제 수정
  * 더 많은 암호구 캐시 시간 설정 옵션 (20, 40, 60분)


## 1.0.6

  * Froyo에서 계정 추가 강제종료 수정
  * 안전한 파일 삭제
  * 가져오기 후 파일 지우기 옵션
  * 스트림 암호화/복호화 (갤러리, 등)
  * 새 옵션 (언어, v3 서명 강제화)
  * 인터페이스 변경
  * 버그 수정


## 1.0.5

  * 독일어와 이탈리아어 번역
  * BountyCastle 소스 크기 축소로 인한 더 작은 패키지 크기
  * 새로운 설정 GUI
  * 지역화를 위한 레이아웃 조정
  * 서명 버그수정


## 1.0.4

  * 검색 생성자에 의해 발생하는 SDK 버그에 의한 다른 강제종료 수정


## 1.0.3

  * 암호화/서명과 가능한 키 내보내기 중 발생하는 강제종료 수정


## 1.0.2

  * 필터링 가능한 키 리스트
  * 더 똑똑한 암호화 키 미리 선택
  * VIEW와 SEND Intent 핸들링 지원, 파일 관리자에서 파일들이 암호화/복호화 되는 걸 지원합니다.
  * K-9 Mail을 위한 추가 기능 (키 미리 선택)과 버그 수정, 새 배타 빌드 사용 가능


## 1.0.1

  GMail 계정 리스트가 1.0.0에서 망가짐, 다시 고침


## 1.0.0

  * K-9 Mail 연동, APG를 지원하는 K-9 Mail의 베타 빌드
  * 더 많은 파일 관리자 지원 (ASTRO 포함)
  * 슬로바키아어 번역
  * 새로운 데이터베이스, 더 빠르고 더 적은 메모리 사용
  * 다른 앱을 위한 컨텐츠 제공자와 Intents 정의
  * 버그 수정