[//]: # (NOTA: coloque cada frase em sua própria linha, o Transifex coloca cada linha em seu próprio campo de tradução!)

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

  *Melhor suporte para chaves grandes
  * Corrigido importação de arquivos Gpg4win com quebra de codificação


## 4.2

  * Suporte experimental para Criptografia de Curvas Elípticas com Tokens de Segurança
  *Melhoria na tela de importação de chave
  *Melhoria no projeto para as listas de chave
  * Support for keyserver onion addresses


## 4.1

  * Melhor detecção de emails e outros conteúdos quando aberto


## 4.0

  * Suporte experimental a Tokens de Segurança por USB
  * Permissão de mudança de senha de chaves estirpadas


## 3.9

  * Detecção e tratamento de dados de texto
  * Melhorias de desempenho
  * UI melhorias para o manuseio do Token de Segurança


## 3.8

  * Edição de chave reestruturada
  * Escolher individualmente lembretes quando digitar senhas
  * Importar chave para o Facebook


## 3.7

  * Melhoria no suporte ao Android 6 (permissões, integração na seleção de texto)
  * API: Versão 10


## 3.6

  * Cópias de Segurança encriptadas
  * Correções de segurança baseadas em auditoria de segurança externa
  * Assistente de criação de chave YubiKey NEO
  * Suporte interno básico a MIME
  * Sincronização automática de chaves
  * Funcionalidades experimentais: associar chaves a contas do GitHub, Twitter
  * Funcionalidade experimental: confirmação de chaves via frases
  * Funcionalidade experimental: tema escuro
  * API: Versão 9


## 3.5

  * Revogação de chave na exclusão
  * Melhoria das checagens de criptografia insegura
  * Correção: Não feche o OpenKeychain após o sucesso do assistente inicial
  * API: Versão 8


## 3.4

  * Download anônimo de chaves por Tor
  * Suporte a proxy
  * Melhor tratamento de erros do YubiKey


## 3.3

  * Nova tela de decriptação
  * Decriptação de múltiplos arquivos de uma só vez
  * Melhor tratamento de erros do YubiKey


## 3.2

  * Primeira versão com suporte total ao YubiKey disponíveis pela interface do usuário: Editar chaves, vincular YubiKey a chaves, etc....
  * Material design
  * Integração com leitura por QR Code (Novas permissões requeridas)
  * Melhoria no assistente de criação de chaves
  * Corrigidos contatos desaparecidos após sincronismo
  * Requer Android 4
  * Tela de chaves redesenhada
  * Simplificação de preferências criptográficas, melhor seleção de cifras seguras
  * API: Assinaturas destacáveis, seleção livre de chave para assinatura, etc...
  * Correção: algumas chaves válidas se mostravam como revogadas ou expiradas
  * Não aceitar assinaturar por subchaves expiradas ou revogadas
  * Suporte ao Keybase.io na vista avançada
  * Método para atualizar todas as chaves de uma só vez


## 3.1.2

  * Correção na exportação de chaves para arquivos (agora vai)


## 3.1.1

  * Correção na exportação de chaves para arquivos (eram gravadas parcialmente)
  * Corrigido um crash no Android 2.3


## 3.1

  * Corrigido um crash no Android 5
  * Nova tela de certificado
  * Troca Segura diretamente da lista de chaves (biblioteca SafeSlinger)
  * Novo fluxo de programa para QR Code
  * Tela de decriptação redesenhada
  * Uso de novos ícones e cores
  * Corrigida a importação de chaves secretas do Symantec Encryption Desktop
  * Suporte experimental a YubiKey: ID's de subchaves agora são verificadas corretamente


## 3.0.1

  *Melhoria no manuseio da importação de chave grande
  * Improved subkey selection


## 3.0

  * Propose installable compatible apps in apps list
  * New design for decryption screens
  * Many fixes for key import, also fixes stripped keys
  * Honor and display key authenticate flags
  * User interface to generate custom keys
  * Fixing user ID revocation certificates
  * New cloud search (searches over traditional keyservers and keybase.io)
  * Support for stripping keys inside OpenKeychain
  * Experimental YubiKey support: Support for signature generation and decryption


## 2.9.2

  Chaves corrigidas em 2.9.1
  * Experimental YubiKey support: Decryption now working via API


## 2.9.1

  Dividir a tela de cifragem em duas
  * Fix key flags handling (now supporting Mailvelope 0.7 keys)
  * Improved passphrase handling
  Compartilhamento de chave via SafeSlinger
  * Experimental YubiKey support: Preference to allow other PINs, currently only signing via the OpenPGP API works, not inside of OpenKeychain
  * Fix usage of stripped keys
  SHA256 como padrão para compatibilidade
  * Intent API has changed, see https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * OpenPGP API now handles revoked/expired keys and returns all user ids


## 2.9

  * Fixing crashes introduced in v2.8
  Suporte experimental a ECC
  * Experimental YubiKey support: Only signing with imported keys


## 2.8

  * So many bugs have been fixed in this release that we focus on the main new features
  * Key edit: awesome new design, key revocation
  * Key import: awesome new design, secure keyserver connections via hkps, keyserver resolving via DNS SRV records
  * New first time screen
  * New key creation screen: autocompletion of name and email based on your personal Android accounts
  * File encryption: awesome new design, support for encrypting multiple files
  * New icons to show status of key (by Brennan Novak)
  Importante correção de erro: agora é possível a importação de múltiplas chaves grandes de um arquivo
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
  *API OpenPGP versão 3 (múltiplas contas api, correções internas, chave de pesquisa)


## 2.4
Thanks to all applicants of Google Summer of Code 2014 who made this release feature rich and bug free!
Além das pequenas retificações, um número notável de retificações foram feitas pelas seguintes pessoas (em ordem alfabética):
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser.

  Nova lista de chaves unificadas
  Impressão digital colorizada
  * Support for keyserver ports
  * Deactivate possibility to generate weak keys
  * Much more internal work on the API
  * Certify user ids
  * Keyserver query based on machine-readable output
  * Lock navigation drawer on tablets
  Sugestões para e-mails na criação de chaves
  Pesquisa em listas de chave pública
  E muito mais melhorias e correções...


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
  * Translations: Norwegian Bokmål (thanks, Sander Danielsen), Chinese (thanks, Zhang Fredrick)
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