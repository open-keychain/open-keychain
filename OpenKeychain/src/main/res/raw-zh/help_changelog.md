[//]: # (注意：请把每个句子放在单独一行中， Transifex 将把每一行放置在独立的翻译表单内！)

## 5.1
  * Support for Ledger Nano S
  * Support Web Key Directory (WKD) search
  * Fixed potential API security issue

## 5.0
  * 改善 Autocrypt 支持

## 4.9

  * 支持 Curve25519
  * 改善对安全令牌的支持

## 4.8

  * 改善对 USB 安全令牌的支持：Gnuk、Nitrokey 型号、YubiKey 4 型号
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

  * 改进启动时对电子邮件和其他内容的检测


## 4.0

  * USB安全令牌的试验性支持
  * 允许修改剥离密钥的密码


## 3.9

  * 检测并处理文本数据
  * 性能提升
  * 提升处理安全令牌的 UI 界面


## 3.8

  * 重新设计了密钥编辑操作
  * 键入密码时单独选择记忆时间
  * 导入 Facebook 密钥


## 3.7

  * 改善对安卓6的支持 (权限，文本选择)
  * API: 第10版


## 3.6

  * 加密的备份
  * 安全性修复基于外部安全性审查
  * YubiKey NEO key 创建向导
  * 支持基本的内部 MIME
  * 自动同步密钥
  * 试验性功能: 关联密钥到Github, Twitter账户
  * 试验性功能: 通过短语确认密钥
  * 实验性功能：深色主题
  * API: 第9版


## 3.5

  * 密钥删除同时撤销
  * 改进了不安全的加密方法
  * 修复: 第一次向导成功后OpenKeychain不关闭
  * API: 第8版


## 3.4

  * 通过Tor下载匿名密钥
  * 支持代理
  * 更好的YubiKey错误处理


## 3.3

  * 新的解密屏幕
  * 一次解密多个文件
  * 更好的处理 YubiKey 错误


## 3.2

  * 第一个版本完整的支持YubiKey。有用的用户界面：编辑密钥，绑定YubiKey到密钥等。
  * 元素设计
  * 集成二维码扫描（需要新权限）
  * 改进创建向导
  * 修复同步后丢失联系人
  * 需要安卓4
  * 重新设计密钥界面
  * 简化加密选项，更好的选择加密算法
  * 分离签名，任意选择签名密钥
  * 修复：密钥有效却被显示为作废或者到期的问题
  * 不受理过期或者作废的子密钥签名
  * 在高级界面中显示支持Keybase.io
  * 一次更新所有密钥的功能


## 3.1.2

  * 修复导出密钥到文件（已实现）


## 3.1.1

  * 修复导出密钥到文件（已经写了部分）
  * 修复在安卓2.3上崩溃


## 3.1

  * 修复在安卓5上崩溃
  * 新验证界面
  * 直接从密钥列表安全的交换（SafeSlinger库）
  * 新的二维码程序流
  * 重新设计解密界面
  * 使用新图标和配色
  * 修复从Symantec Encryption Desktop导入密钥
  * 试验性的YubiKey支持：子密钥ID可以正确的检查。


## 3.0.1

  * 更好的处理大密钥的导入
  * 改进子密钥选中


## 3.0

  * 在应用列表中推荐可安装兼容的app
  * 解密界面启用新设计
  * 修复密钥导入的众多问题，其中有剥离密的钥问题
  * 头衔和显示密钥验证的旗帜标记
  * 产生自定义密钥的用户界面
  * Fixing user ID revocation certificates
  * 新的云搜索（通过传统的密钥服务器和keybase.io搜索）
  * OpenKeychain支持剥离密钥
  * 试验性的YubiKey支持：支持生成签名和解密


## 2.9.2

  * 修复2.9.1的密钥破损
  * 实验性的YubiKey支持：解密现在通过API工作


## 2.9.1

  * 分割加密界面分为两个
  * 修正处理密钥标志（现已支持Mailvelope 0.7密钥）
  * 改进的密码处理
  * 通过SafeSlinger密钥共享
  * 实验性的YubiKey支持：优先允许其他PIN，目前只能通过OpenPGP的API签署工作，而不是OpenKeychain内部的
  * 修正剥离密钥的用法
  *  默认兼容SHA256
  * Intent API发生了变化，请看https://github.com/open-keychain/open-keychain/wiki/Intent-API
  *  OpenPGP的API现在处理撤销/到期密钥和返回所有用户ID


## 2.9

  * 正在修复v2.8里的崩溃问题
  * 实验性的ECC支持
  * 实验性的YubiKey支持：只签订导入的密钥


## 2.8

  * 在此版本中很多的错误已得到修复，我们重点关注的主要新特性
  * 密钥编辑：超棒的新设计，密钥撤销
  * 密钥导入：超棒的新设计，通过hkps协议连接安全密钥服务器，密钥服务器解析通过DNS SRV记录
  * 新的首次使用界面
  * 新的密钥创建界面：根据您的Andr​​oid个人账户自动完成姓名和电子邮件
  文件加密：超棒的新设计，支持加密多个文件
  * 密钥状态显示新的图标（by Brennan Novak）
  * 重要的bug修复：现在可以从文件导入大型密钥集合
  * 通知中显示缓存口令
  * 密钥关联到Android的联系人

这个版本离不开Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar的辛勤工作

## 2.7

  * 紫色！（Dominik, Vincent）
  * 新密钥视图设计（Dominik, Vincent）
  * 新扁平的Android按钮（Dominik, Vincent）
  *  API修正（Dominik）
  *  Keybase.io导入（Tim Bray）


## 2.6.1

  * 一些修复回归bug


## 2.6

  * 密钥认证（感谢Vincent Breitmoser）
  * 支持GnuPG的部分密钥（感谢Vincent Breitmoser)
  * 新设计的签名验证
  * 自定义密钥长度（感谢格Greg Witczak）
  * 修正与其他应用程序共享功能


## 2.5

  * 修正了对称的OpenPGP消息/文件解密
  * 重构密钥编辑界面（感谢Ash Hughes）
  * 加密/解密界面新的现代设计
  *  OpenPGP的API版本3（多个API账户，内部修复，密钥查找）


## 2.4
感谢所有谷歌编程之夏2014的参与者他们制作了这个问题少而功能多的版本！
除了几个小补丁，大量的补丁由下面的人做（排名不分先后）：
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser。

  * 新的统一的密钥列表
  * 彩色密钥指纹
  * 支持密钥服务器端口
  * 停用可能产生弱密钥
  * Much more internal work on the API
  * 证明用户ID
  * 基于机器可读的输出密钥服务器查询
  * 在平板上的锁屏导航抽屉
  * 建议电子邮件在创建密钥时
  * 在公共密钥列表搜索
  * 还有更多的改进和修正...


## 2.3.1

  * 修补升级后崩溃的问题


## 2.3

  * 当导出私钥时删除不需要的导出公钥（感谢Ash Hughes）
  * 修复密钥到期日期设置（感谢Ash Hughes）
  当编辑密钥时更多的内部修复（感谢Ash Hughes）
  * 直接从导入屏幕界面中查询密钥服务器
  * 修正布局和对话风格在Android 2.2-3.0
  * 修复密钥中存在空使用者ID的崩溃情况
  * 修复从签署中界面返回中时的崩溃和空表情况
  * Bouncy Castle（密码库）从1.47升级到1.50，并从源中建立
  * 修正从签署中界面上传密钥


## 2.2

  * 抽屉式导航的全新设计
  * 新的公钥表设计
  * 新的公共密钥视图
  *  导入密钥中的Bug修复
  * 密钥交叉认证（感谢Ash Hughes）
  * 正确处理UTF-8密码（感谢Ash Hughes）
  * 第一版本，新的语言（感谢在Transifex贡献者）
  * 修复和改进了QR码键分享
  * 为API包签名验证


## 2.1.1

  *  API更新，K-9邮件集成准备


## 2.1

  * 很多bug修复
  * 为开发者提供新的API
  *  PRNG漏洞被谷歌修复


## 2.0

  * 完全重新设计
  * 通过QR码，NFC beam分享公共密钥，
  * 签署密钥
  * 上传密钥到服务器
  * 修正了导入问题
  * 新的AIDL API


## 1.0.8

  * 基本密钥服务器支持
  *  APP2SD
  * 密码缓存时间提供更多的选择：1，2，4，8，小时
  * 翻译：挪威语（感谢 Sander Danielsen），中文（感谢 Zhang Fredrick）
  *  Bug修复
  * 优化


## 1.0.7

  * 修复包含换行符的文本的签名验证问题
  * 密码缓存时间提供更多的选择（20，40，60分钟）


## 1.0.6

  * 修复Froyo中账户添加中的崩溃现象
  * 安全文件删除
  * 可选择导入密钥文件后删除
  * 流加密/解密（画廊等）
  * 新的选项（语言，强制V3签名）
  * 界面的变化
  *  Bug修复


## 1.0.5

  * 德语和意大利语翻译
  * 由于缩减了BC源，包大大减小了
  * 新偏好的GUI
  * 局部布局调整
  * 签名漏洞修复


## 1.0.4

  * 修正另一个因某些SDK中查询生成器的bug引起的崩溃


## 1.0.3

  * 修复在加密/签名中和导出时可能的崩溃现象


## 1.0.2

  * 可筛选密钥列表
  * 更智能的预选加密密钥
  * 查看和发送使用新的目的处理，允许文件在文件管理器之外加密/解密
  * 为K-9邮件修补问题和附加功能（预选密钥），可获得新的beta版


## 1.0.1

  * Gmail帐户列表在1.0.0中被破坏，再次修复


## 1.0.0

  *  K-9邮件整合，APG支持beta版K-9邮件
  * 支持更多的文件管理器（包括ASTRO）
  * 斯洛文尼亚语翻译
  * 新的数据库，速度更快，使用更少的内存
  * 为其他应用程序定义的用途和内容提供商
  *  Bug修复