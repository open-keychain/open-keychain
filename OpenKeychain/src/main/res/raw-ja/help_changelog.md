[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## 3.2beta2

  * マテリアルデザイン
  * QRスキャナの統合 (新しいパーミッションを必要とします)
  * 鍵生成ウィザードの改善
  * 同期後に連絡先を見失う問題の修正
  * Android 4を必要とします
  * 鍵画面の再デザイン
  * 暗号の設定をシンプル化、より良いセキュアな暗号の選択方法
  * API: 分離署名、署名する鍵の選択がフリーとなる、...
  * 修正: いくつかの正しい鍵が破棄もしくは期限切れとして表示される
  * 副鍵が期限切れもしくは破棄されている場合に署名を受け入れない
  * 拡張ビューでのKeybase.ioのサポート


## 3.1.2

  * 鍵のファイルへのエクスポートの修正 (現実的になりました)


## 3.1.1

  * 鍵のファイルへのエクスポートの修正 (部分的に修正)
  * Android 2.3でのクラッシュ修正


## 3.1

  * Android 5でのクラッシュ修正
  * 新しい検証画面
  * セキュアな鍵リストの直接交換(SafeSlinger ライブラリ)
  * 新しいQRコードのプログラムフロー
  * 復号化画面の再デザイン
  * 新しいアイコン利用とカラー
  * Symantec Encryption Desktopから秘密鍵をインポート時の問題修正
  * Yubikeyでの副鍵IDを正くチェックするようになりました


## 3.0.1

  * 巨大な鍵のインポートのより良い取り扱い
  * 副鍵選択の改善


## 3.0

  * Yubikeyでの署名生成と復号化のフルサポート
  * Propose installable compatible apps in apps list
  * New design for decryption screens
  * Many fixes for key import, also fixes stripped keys
  * Honor and display key authenticate flags
  * User interface to generate custom keys
  * Fixing user id revocation certificates
  * New cloud search (searches over traditional keyservers and keybase.io)
  * OpenKeychain内で鍵をストリップするのをサポートしました


## 2.9.2

  * 2.9.1での鍵破壊問題修正
  * API経由でYubikeyの復号処理が動くようになった


## 2.9.1

  * 暗号化スクリーンを2つに分割
  * 鍵のフラグ管理を修正 (現在Mailvelope 0.7 鍵をサポート)
  * パスフレーズの取り回しを改善
  * SafeSlingerでの鍵の共有
  * Yubikey: 設定で他のPINを受け付け、現在OpenPGP API経由での署名しか動きません、OpenKeychainの内部ではないため
  * ストリップした鍵の利用法を修正
  * 互換性のためデフォルトをSHA256に
  * インテント API を変更しました、以下参照 https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * OpenPGP API は現在破棄/期限切れの鍵を扱えるようになり、またすべてのユーザIDを返すようになりました


## 2.9

  * v2.8 から発生したクラッシュ問題をFix
  * 実験的にECCをサポート
  * 実験的にYubikeyをサポート(インポート済みの鍵での署名のみ)


## 2.8

  * そして主要な新しい機能を主眼としたこのリリースでたくさんのバグが修正されました
  * 鍵編集: 新しいすごいデザイン、鍵の破棄
  * 鍵インポート: 新しいすごいデザイン、hkps経由での鍵サーバとの安全な接続、そしてDNS SRVレコードによる鍵サーバの解決
  * 新しい初回表示
  * 新しい鍵生成画面: Androidのあなたの個人アカウントをベースとした名前とメールの自動補完
  * ファイル暗号化: 新しいすごいデザイン、複数ファイルの暗号化をサポートする
  * 鍵のステータス表示の新しいアイコン(Brennan Novak提供)
  * 重要なバグ修正: 巨大な鍵コレクションをファイルからインポートするのが可能になりました
  * キャッシュしたパスフレーズの通知表示
  * 鍵のアドレスをAndroidの連絡先と連携するようにした

Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfiharの働きなくしてはこのリリースはありませんでした

## 2.7

  * Purple! (Dominik, Vincent)
  * 新しい鍵のビューのデザイン (Dominik, Vincent)
  * 新しいフラットな Android ボタン (Dominik, Vincent)
  * API のフィックス (Dominik)
  * Keybase.io からのインポート (Tim Bray)


## 2.6.1

  * いくつかのリグレッションバグ修正


## 2.6

  * 鍵証明 (ありがとうVincent Breitmoser)
  * Support for GnuPG partial secret keys (thanks to Vincent Breitmoser)
  * New design for signature verification
  * Custom key length (thanks to Greg Witczak)
  * Fix share-functionality from other apps


## 2.5

  * Fix decryption of symmetric OpenPGP messages/files
  * Refactored key edit screen (thanks to Ash Hughes)
  * New modern design for encrypt/decrypt screens
  * OpenPGP API version 3 (multiple api accounts, internal fixes, key lookup)


## 2.4
Thanks to all applicants of Google Summer of Code 2014 who made this release feature rich and bug free!
Besides several small patches, a notable number of patches are made by the following people (in alphabetical order):
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser.

  * New unified key list
  * Colorized key fingerprint
  * Support for keyserver ports
  * Deactivate possibility to generate weak keys
  * Much more internal work on the API
  * Certify user ids
  * Keyserver query based on machine-readable output
  * Lock navigation drawer on tablets
  * Suggestions for emails on creation of keys
  * Search in public key lists
  * And much more improvements and fixes…


## 2.3.1

  * Hotfix for crash when upgrading from old versions


## 2.3

  * Remove unnecessary export of public keys when exporting secret key (thanks to Ash Hughes)
  * Fix setting expiry dates on keys (thanks to Ash Hughes)
  * More internal fixes when editing keys (thanks to Ash Hughes)
  * Querying keyservers directly from the import screen
  * Fix layout and dialog style on Android 2.2-3.0
  * Fix crash on keys with empty user ids
  * Fix crash and empty lists when coming back from signing screen
  * Bouncy Castle (cryptography library) updated from 1.47 to 1.50 and build from source
  * Fix upload of key from signing screen


## 2.2

  * New design with navigation drawer
  * New public key list design
  * New public key view
  * Bug fixes for importing of keys
  * Key cross-certification (thanks to Ash Hughes)
  * Handle UTF-8 passwords properly (thanks to Ash Hughes)
  * First version with new languages (thanks to the contributors on Transifex)
  * Sharing of keys via QR Codes fixed and improved
  * Package signature verification for API


## 2.1.1

  * API Updates, preparation for K-9 Mail integration


## 2.1

  * Lots of bug fixes
  * New API for developers
  * PRNG bug fix by Google


## 2.0

  * Complete redesign
  * Share public keys via QR codes, NFC beam
  * Sign keys
  * Upload keys to server
  * Fixes import issues
  * New AIDL API


## 1.0.8

  * Basic keyserver support
  * App2sd
  * More choices for passphrase cache: 1, 2, 4, 8, hours
  * Translations: Norwegian (thanks, Sander Danielsen), Chinese (thanks, Zhang Fredrick)
  * Bugfixes
  * Optimizations


## 1.0.7

  * Fixed problem with signature verification of texts with trailing newline
  * More options for passphrase cache time to live (20, 40, 60 mins)


## 1.0.6

  * Account adding crash on Froyo fixed
  * Secure file deletion
  * Option to delete key file after import
  * Stream encryption/decryption (gallery, etc.)
  * New options (language, force v3 signatures)
  * Interface changes
  * Bugfixes


## 1.0.5

  * German and Italian translation
  * Much smaller package, due to reduced BC sources
  * New preferences GUI
  * Layout adjustment for localization
  * Signature bugfix


## 1.0.4

  * Fixed another crash caused by some SDK bug with query builder


## 1.0.3

  * Fixed crashes during encryption/signing and possibly key export


## 1.0.2

  * Filterable key lists
  * Smarter pre-selection of encryption keys
  * New Intent handling for VIEW and SEND, allows files to be encrypted/decrypted out of file managers
  * Fixes and additional features (key preselection) for K-9 Mail, new beta build available


## 1.0.1

  * GMail account listing was broken in 1.0.0, fixed again


## 1.0.0

  * K-9 Mail integration, APG supporting beta build of K-9 Mail
  * Support of more file managers (including ASTRO)
  * Slovenian translation
  * New database, much faster, less memory usage
  * Defined Intents and content provider for other apps
  * Bugfixes