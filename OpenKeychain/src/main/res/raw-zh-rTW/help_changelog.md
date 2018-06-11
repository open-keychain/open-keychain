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

  * Improved support for USB tokens: Gnuk, Nitrokey models, YubiKey 4 models
  * Feature to find the position of the device's NFC reader

## 4.7

  * Improved import from clipboard
  * New key creation wizard for Security Tokens
  * Removed password cache "time to live" setting


## 4.6

  * Import your keys using our new Secure Wi-Fi Transfer mechanism


## 4.5

  * Detailed description of security problems
  * Display keyserver status per key
  * Support for EdDSA
  * Fix pgp.mit.edu (new certificate)


## 4.4

  * New key status displays detailed information why a key is considered insecure or defective


## 4.3

  * Better support for large keys
  * Fix import of Gpg4win files with broken encodings


## 4.2

  * Experimental support for Elliptic Curve Encryption with Security Tokens
  * Redesigned key import screen
  * Design improvements to key lists
  * Support for keyserver onion addresses


## 4.1

  * Better detection of emails and other content when opened


## 4.0

  * 實驗性的支援：經由USB給予安全口令
  * Allow password changing of stripped keys


## 3.9

  * Detection and handling of text data
  *改善性能
  * UI improvements for Security Token handling


## 3.8

  * Redesigned key editing
  * Choose remember time individually when entering passwords
  * Facebook key import


## 3.7

  *改進對於Android 6的支援 (權限, 整合在文字選單中)
  * API: Version 10


## 3.6

  * Encrypted backups
  * Security fixes based on external security audit
  * YubiKey NEO key creation wizard
  *支援基本的檔案表頭
  *自動金鑰同步
  * 實驗性的功能：連結金鑰到Github, Twitter的帳戶
  * Experimental feature: key confirmation via phrases
  * 實驗性的功能：暗色主題
  * API: Version 9


## 3.5

  *可以在刪除金鑰時撤銷金鑰
  *改進對加密安全性進行檢查
  *修正：在OpenKeychain第一次執行精靈成功後不自動關閉程式
  * API: Version 8


## 3.4

  *支援下載匿名的要使經由Tor路由
  *支援代理伺服器
  * 更好的處理YubiKey 的錯誤


## 3.3

  *新的解密畫面
  *可同時解密多個檔案
  * 更好的處理YubiKey 的錯誤


## 3.2

  * 首次完整支援Yubikey：修改金鑰、連結Yubikey與金鑰、...
  * 素材設計
  * 整合QR條碼掃描 (需要新權限)
  * 改進的金鑰建立精靈
  * 修正同步後遺失聯絡人
  * 最低要求 Android 4
  * 重新設計的金鑰畫面
  * 簡化的加密參數，更佳的安全加密算法選項
  * API：分離的簽名、自由選擇用來簽名的金鑰、...
  * 修正：部分有效的金鑰被顯示為已撤銷或過期
  * 不接受來自已撤銷或過期子金鑰的簽名
  * 進階檢視中的Keybase.io支援
  * 可以一次更新所有的金鑰了


## 3.1.2

  * 修正匯出金鑰到檔案的功能（這次是真的了）


## 3.1.1

  * 修正匯出金鑰到檔案功能（一部分）
  * 修正在Android 2.3上的崩潰


## 3.1

  * 修正在Android 5上的崩潰
  * 新的認證畫面
  * 直接在金鑰清單進行安全金鑰交換（使用SafeSlinger）
  * 新的QR條碼作業流程
  * 重新設計的解密畫面
  * 新的圖示和配色
  * 修正從Symantec Encryption Desktop匯入密鑰的問題
  * 實驗性的YubiKey支援：子金鑰ID現在可以正確的檢查了


## 3.0.1

  * 更好的大型金鑰匯入處理
  * 改良的子金鑰選取


## 3.0

  * 在應用列表中建議相容的應用
  * 新設計的解密畫面
  * 許多匯入金鑰的修正，並修正剝離的金鑰
  * Honor and display key authenticate flags
  * 產生自訂金鑰的介面
  * Fixing user ID revocation certificates
  * 新的雲端檢索功能（同時搜尋傳統的金鑰伺服器以及keybase.io資料庫）
  * 現在OpenKeychain可以支援剝離的金鑰了
  * 實驗性的YubiKey支援：支援簽名和解密


## 2.9.2

  * 修正2.9.1導致金鑰破損的問題
  * 實驗性的YubiKey支援：現在可以透過API進行解密


## 2.9.1

  * 加密畫面現在一分為二
  * Fix key flags handling (now supporting Mailvelope 0.7 keys)
  * Improved passphrase handling
  * Key sharing via SafeSlinger
  * Experimental YubiKey support: Preference to allow other PINs, currently only signing via the OpenPGP API works, not inside of OpenKeychain
  * Fix usage of stripped keys
  *支援將SHA256設為預設
  * Intent API has changed, see https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * OpenPGP API now handles revoked/expired keys and returns all user ids


## 2.9

  * Fixing crashes introduced in v2.8
  * Experimental ECC support
  * Experimental YubiKey support: Only signing with imported keys


## 2.8

  * So many bugs have been fixed in this release that we focus on the main new features
  * Key edit: awesome new design, key revocation
  * Key import: awesome new design, secure keyserver connections via hkps, keyserver resolving via DNS SRV records
  * 新的初始畫面
  * New key creation screen: autocompletion of name and email based on your personal Android accounts
  * File encryption: awesome new design, support for encrypting multiple files
  * New icons to show status of key (by Brennan Novak)
  * Important bug fix: Importing of large key collections from a file is now possible
  * Notification showing cached passphrases
  * Keys are connected to Android's contacts

This release wouldn't be possible without the work of Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar

## 2.7

  * Purple! (Dominik, Vincent)
  * New key view design (Dominik, Vincent)
  * New flat Android buttons (Dominik, Vincent)
  * API fixes (Dominik)
  * Keybase.io import (Tim Bray)


## 2.6.1

  * Some fixes for regression bugs


## 2.6

  * Key certifications (thanks to Vincent Breitmoser)
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
  * 改善及修正經由 QR codes分享公鑰的功能
  *新增驗證打包的簽章的API


## 2.1.1

  * API Updates, preparation for K-9 Mail integration


## 2.1

  *修復多個錯誤
  *提供開發者新的API
  * PRNG bug fix by Google


## 2.0

  *徹底的重新設計
  * 經由 QR codes, NFC beam分享公鑰
  *簽署金鑰
  *上傳到金鑰伺服器
  * Fixes import issues
  * New AIDL API


## 1.0.8

  *支援基本的金鑰伺服器
  * App2sd
  * More choices for passphrase cache: 1, 2, 4, 8, hours
  * Translations: Norwegian Bokmål (thanks, Sander Danielsen), Chinese (thanks, Zhang Fredrick)
  *錯誤修正
  * 最佳化


## 1.0.7

  * Fixed problem with signature verification of texts with trailing newline
  * More options for passphrase cache time to live (20, 40, 60 mins)


## 1.0.6

  * Account adding crash on Froyo fixed
  * Secure file deletion
  * Option to delete key file after import
  * Stream encryption/decryption (gallery, etc.)
  * New options (language, force v3 signatures)
  *介面改變
  *錯誤修正


## 1.0.5

  * 德語和義大利文翻譯
  * Much smaller package, due to reduced BC sources
  * New preferences GUI
  * Layout adjustment for localization
  *修正驗證簽名的錯誤


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

  *再次修正在1.0.0版GMail帳戶清單破損的問題


## 1.0.0

  * K-9 Mail integration, APG supporting beta build of K-9 Mail
  *支援更多檔案管理員(像是ASTRO)
  * 斯洛文尼亞語翻譯
  *新的資料庫，更快，使用較少的記憶體
  * Defined Intents and content provider for other apps
  *錯誤修正