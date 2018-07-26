[//]: # (NOTA: coloque cada frase em sua própria linha, o Transifex coloca cada linha em seu próprio campo de tradução!)

## 5.1
  * Suporte para Ledger Nano S
  * Suporte para busca Web Key Directory (WKD)
  * Corrigida uma potencial questão de segurança na API

## 5.0
  * Melhorias no suporte a Autocrypt

## 4.9

  * Suporte a Curve25519
  * Melhorias no suporte a tokens de segurança

## 4.8

  * Melhorias no suporte a tokens USB: modelos Gnuk, Nitrokey models, YubiKey 4
  * Funcionalidade para encontrar a posição do leitor NFC do dispositivo

## 4.7

  * Melhorias no suporte à importação da área de transferência
  * Novo assistente de criação de chaves para Tokens de Segurança
  * Remoção da configuração "tempo de vida" do cache de senhas


## 4.6

  * Importe suas chaves utilizando nosso novo mecanismo de Transferência Segura Wi-Fi


## 4.5

  * Descrição detalhada de problemas de segurança
  * Exibição de estado do servidor de chaves por chave
  * Suporte a EdDSA
  * Correção para o pgp.mit.edu (novo certificado)


## 4.4

  * O novo estado da chave exibe informações detalhadas de porque a chave é considerada insegura ou defeituosa


## 4.3

  *Melhor suporte para chaves grandes
  * Corrigido importação de arquivos Gpg4win com quebra de codificação


## 4.2

  * Suporte experimental para Criptografia de Curvas Elípticas com Tokens de Segurança
  *Melhoria na tela de importação de chave
  *Melhoria no projeto para as listas de chave
  * Suporte para endereços onion de servidores de chaves


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
  * Melhorias na seleção de subchaves


## 3.0

  * Sugestões de apps instaláveis e compatíveis na lista de apps
  * Nova interface para as telas de decriptação
  * Várias correções para importação de chaves, e correções para chaves extirpadas
  * Honra e exibição de opções de autenticação de chave
  * Interface de usuário para gerar chaves personalizadas
  * Correção dos certificados de revogação de IDs de usuário
  * Nova busca na nuvem (procura em servidores de chaves tradicionais e keybase.io)
  * Suporte a extirpar chaves dentro do OpenKeychain
  * Suporte experimental ao YubiKey: Suporte para geração de assinaturas e decriptação


## 2.9.2

  Chaves corrigidas em 2.9.1
  * Suporte experimental ao YubiKey: Decriptação agora funcional via API


## 2.9.1

  Dividir a tela de cifragem em duas
  * Correção do manejo de opções de chaves (agora suportando chaves do Mailvelope 0.7)
  * Melhorias no manejo de senhas
  Compartilhamento de chave via SafeSlinger
  * Suporte experimental ao YubiKey: Preferência para permitir outros PINs, atualmente apenas a assinatura via a API OpenPGP está funcional, não dentro do OpenKeychain
  * Correção no uso de chaves extirpadas
  SHA256 como padrão para compatibilidade
  * A API Intent foi modificada, consulte https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * A API OpenPGP agora trata chaves revogadas/expiradas e retorna todos os IDs de usuários


## 2.9

  * Corrigidos crashes introduzidos na v2.8
  Suporte experimental a ECC
  * Suporte experimental ao YubiKey: Apenas assinaturas com chaves importadas


## 2.8

  * Tantos bugs foram corrigidos neste lançamento que nós nos focamos nas funcionalidades principais
  * Edição de chaves: fantástico novo design, revogação de chave
  * Importação de chave: fantástico novo design, conexões seguras aos servidores de chave via hkps, resolução de servidores via registros DNS SRV
  * Nova tela de primeiro acesso
  * Nova tela de criação de chave: autocompletar nome e email baseado em suas contas pessoais Android
  * Encriptação de arquivo: fantástico novo design, suporte para encriptar múltiplos arquivos
  * Novos ícones para exibir o estado das chaves (por Brennan Novak)
  Importante correção de erro: agora é possível a importação de múltiplas chaves grandes de um arquivo
  * Notificação exibindo senhas em cache
  * Chaves são associadas aos contatos do Android

Este lançamento não seria possível sem o trabalho de Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar

## 2.7

  * Roxo! (Dominik, Vincent)
  * Novo design para ver chave (Dominik, Vincent)
  * Novos botões planos do Android (Dominik, Vincent)
  * Correções na API (Dominik)
  * Importação Keybase.io (Tim Bray)


## 2.6.1

  * Algumas correções para bugs de regressão


## 2.6

  * Certificações de chaves (obrigado a Vincent Breitmoser)
  * Suporte a chaves secretas parciais do GnuPG (obrigado a Vincent Breitmoser)
  * Novo design para verificação de assinaturas
  * Comprimento personalizado de chaves (thanks to Greg Witczak)
  * Corrigida a funcionalidade de compartilhamento de outras aplicações


## 2.5

  * Corrigida a decriptação de mensagens/arquivos OpenPGP simétricos
  * Refatoração da tela de edição de chaves (obrigado a Ash Hughes)
  * Novo design moderno para telas de encriptação/decriptação
  *API OpenPGP versão 3 (múltiplas contas api, correções internas, chave de pesquisa)


## 2.4
Obrigado a todos os candidatos do Google Summer of Code 2014 que fizeram este release cheio de funcionalidades e livre de bugs!
Além das pequenas retificações, um número notável de retificações foram feitas pelas seguintes pessoas (em ordem alfabética):
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser.

  Nova lista de chaves unificadas
  Impressão digital colorizada
  * Suporte a portas do servidor de chaves
  * Desativada a possibilidade de gerar chaves fracas
  * Mais trabalho interno na API
  * Certificação de IDs de usuários
  * Consulta aos servidores de chaves baseada em saída "machine-readable"
  * Travada a gaveta de navegação em tablets
  Sugestões para e-mails na criação de chaves
  Pesquisa em listas de chave pública
  E muito mais melhorias e correções...


## 2.3.1

  * Hotfix para um crash quando atualizando de versões antigas


## 2.3

  * Removida a exportação desnecessárias de chaves públicas quando exportando chave privada (obrigado a Ash Hughes)
  * Corrigida a definição de datas de expiração em chaves (obrigado a Ash Hughes)
  * Mais correções internas quando editando chaves (obrigado a Ash Hughes)
  * Consultar servidores de chaves diretamente da tela de importação
  * Correção de layout e estilo dos diálogos no Android 2.2-3.0
  * Corrigido crash com chaves com IDs de usuário vazios
  * Corrigido crash e listas vazias quando retornando da tela de assinatura
  * Bouncy Castle (biblioteca criptográfica) atualizada de 1.47 para 1.50 e compilada da fonte
  * Corrigido o upload de chave da tela de assinatura


## 2.2

  * Novo design com gavetas de navegação
  * Novo design a lista de chaves públicas
  * Nova visualização de chave pública
  * Correções para importação de chaves
  * Certificação cruzada de chaves (obrigado a Ash Hughes)
  * Tratamento apropriado de senhas UTF-8 (obrigado Ash Hughes)
  * Primeira versão com novos idiomas (obrigado aos contribuidores no Transifex)
  * Compartilhamento de chaves via QR Codes corrigido e melhorado
  * Verificação de pacotes de assinatura para API


## 2.1.1

  * Atualizações na API, preparação para integração com o K-9 Mail.


## 2.1

  * Várias correções
  * Nova API para desenvolvedores
  * Correção PRNG pelo Google


## 2.0

  * Redesenho completo
  * Compartilhar chaves públicas por QR Codes, NFC beam
  * Assinar chaves
  * Enviar chaves para servidor
  * Corrigidos problemas na importação
  * Nova API AIDL


## 1.0.8

  * Suporte básico a servidores de chaves
  * App2sd
  * Mais opções para o cache da senha: 1, 2, 4, 8 horas
  * Traduções: Norueguês Bokmål (obrigado, Sander Danielsen), Chinês (obrigado, Zhang Fredrick)
  * Correções
  * Otimizações


## 1.0.7

  * Corrigido um problema com a verificação de assinaturas de textos com caractere de nova linha no final
  * Mais opções para o tempo de vida do cache de senha (20, 40, 60 minutos)


## 1.0.6

  * Crash ao adicionar conta no Froyo corrigido
  * Exclusão segura de arquivos
  * Opção para excluir arquivo de chave após a importação
  * Fluxo de encriptação/decriptação (galeria, etc.)
  * Novas opções (idioma, forçar assinaturas v3)
  * Mudanças na interface
  * Correções


## 1.0.5

  * Traduções para Alemão e Italiano
  * Pacote bem menor, graças à redução das fontes do BC
  * Nova interface de preferências
  * Ajustes no layout para localização
  * Correção na assinatura


## 1.0.4

  * Corrigido outro crash causado por algum bug no SDK com o query builder


## 1.0.3

  * Corrigido crashes durante a encriptação/decriptação e possivelmente exportação de chaves


## 1.0.2

  * Listas de chaves com filtros
  * Pré-seleção de chaves de encriptação mais inteligente
  * Novo tratamento Intent para VIEW e SEND, permite que arquivos sejam encriptados/decriptados fora de gerenciadores de arquivos
  * Correções e funcionalidades adicionais (pré-seleção de chaves) para K-9 Mail, novo beta disponível


## 1.0.1

  * A listagem de contas GMail quebrou no 1.0.0, corrigido de novo


## 1.0.0

  Integra~c"ao com K-9 Mail, APG suportando o build beta do K-9 Mail
  * Suporte a mais gerenciadores de arquivos (incluindo ASTRO)
  * Tradução Eslovena
  * Novo banco de dados, mais rápido e menor consumo de memória
  * Definido Intents e provedor de conteúdo para outras aplicações
  * Correções