[//]: # (NOTA: por favor coloque cada frase na sua própria linha. O Transifex coloca todas as linhas no seu próprio campo de tradução!)

## 5.7
  * Fixes for Curve25519
  * IDEA cipher is now considered insecure

## 5.6
  * Compatibility with Android 10 and higher
  * Several bug fixes

## 5.5
  * Fix decryption from clipboard on Android 10

## 5.4
  * Add WKD Advanced method
  * Add COTECH Security Key Shop

## 5.3
  * Use keys.openpgp.org as default keyserver

## 5.2
  * Melhorias na importação de chaves da área de transferência

## 5.1
  * Suporte para o Ledger Nano S
  * Suporte para procura de Web Key Directory (WKD)
  * Corrigido um problema potencial de segurança na API

## 5.0
  * Melhorias no suporte a Autocrypt

## 4.9

  * Suporte a Curve25519
  * Melhorias no suporte a tokens de segurança

## 4.8

  * Melhorias no suporte a tokens USB: modelos Gnuk, modelos Nitrokey, YubiKey 4
  * Funcionalidade para encontrar a posição do leitor NFC do dispositivo

## 4.7

  * Melhorias no suporte à importação da área de transferência
  * Novo assistente de criação de chaves para Tokens de Segurança
  * Removida a configuração "tempo de vida" da cache de palavras-passe


## 4.6

  * Importe as suas chaves utilizando o novo mecanismo de Transferência Segura por Wi-Fi


## 4.5

  * Descrição detalhada de problemas de segurança
  * Visualização do estado do servidor de chaves por chave
  * Suporte a EdDSA
  * Correção de pgp.mit.edu (novo certificado)


## 4.4

  * O novo estado da chave mostra informações detalhadas sobre a razão da chave ser considerada insegura ou defeituosa


## 4.3

  * Melhor suporte para chaves grandes
  * Corrigida importação de ficheiros Gpg4win com quebra de codificações


## 4.2

  * Suporte experimental para Criptografia de Curvas Elípticas com Tokens de Segurança
  * Melhoria no ecrã de importar chave
  * Melhorias nas listas de chaves
  * Suporte a endereços onion de servidores de chaves


## 4.1

  * Melhor deteção de emails e outros conteúdos quando aberto


## 4.0

  * Suporte experimental a Tokens de Segurança por USB
  * Permitir alteração de palavra-passe de chaves desemparelhadas


## 3.9

  * Deteção e manipulação de dados de texto
  * Melhorias de desempenho
  * Melhorias na interface para manipular o Token de Segurança


## 3.8

  * Edição de chave reestruturada
  * Escolher memorizações individualmente ao introduzir palavras-passe
  * Importar chave do Facebook


## 3.7

  * Melhoria no suporte ao Android 6 (permissões, integração na seleção de texto)
  * API: versão 10


## 3.6

  * Cópias de segurança encriptadas
  * Correções de segurança baseadas na auditoria de segurança externa
  * Assistente de criação de chave YubiKey NEO
  * Suporte interno básico a MIME
  * Sincronização automática de chaves
  * Funcionalidades experimentais: associar chaves a contas do GitHub, Twitter
  * Funcionalidade experimental: confirmação de chaves via frases
  * Funcionalidade experimental: tema escuro
  * API: versão 9


## 3.5

  * Revogação da chave ao eliminar chave
  * Melhoria das verificações de criptografia insegura
  * Correção: não fechar o OpenKeychain após o assistente inicial bem sucedido
  * API: versão 8


## 3.4

  * Descarregar chaves de forma anónima pelo Tor
  * Suporte a proxy
  * Melhor manipulação de erros do YubiKey


## 3.3

  * Novo ecrã de desencriptação
  * Desencriptação de vários ficheiros de uma só vez
  * Melhor manipulação de erros do YubiKey


## 3.2

  * Primeira versão com suporte total ao YubiKey disponíveis pela interface do utilizador: editar chaves, vincular YubiKey a chaves, etc.
  * Material design
  * Integração com leitura por QR Code (necessárias novas permissões)
  * Melhoria no assistente de criação de chaves
  * Corrigidos contactos desaparecidos após sincronização
  * Necessita do Android 4
  * Ecrã de chaves redesenhado
  * Simplificação de preferências criptográficas, melhor seleção de cifras seguras
  * API: assinaturas avulsas, seleção livre de chave para assinatura, etc.
  * Correção: algumas chaves válidas eram mostradas como revogadas ou expiradas
  * Não aceitar assinaturas por sub-chaves expiradas ou revogadas
  * Suporte ao Keybase.io na visualização avançada
  * Método para atualizar todas as chaves de uma só vez


## 3.1.2

  * Correção na exportação de chaves para ficheiro (agora sim)


## 3.1.1

  * Correção na exportação de chaves para ficheiros (eram gravadas parcialmente)
  * Correção de um crash no Android 2.3


## 3.1

  * Correção de um crash no Android 5
  * Novo ecrã de certificado
  * Troca Segura diretamente da lista de chaves (biblioteca SafeSlinger)
  * Novo fluxo de programa para QR Code
  * Ecrã de desencriptação redesenhado
  * Novos ícones e cores
  * Corrigida a importação de chaves secretas do Symantec Encryption Desktop
  * Suporte experimental a YubiKey: os ID's de sub-chaves são agora verificados corretamente


## 3.0.1

  * Melhoria na manipulação da importação de chave grande
  * Melhorias na seleção de sub-chaves


## 3.0

  * Sugestões de aplicações instaláveis e compatíveis na lista de aplicações
  * Nova interface para os ecrãs de desencriptação
  * Várias correções na importação de chaves e correções em chaves desemparelhadas
  * Honrar e mostrar sinalizadores de autenticação de chave
  * Interface de utilizador para gerar chaves personalizadas
  * Correção nos certificados de revogação de IDs de utilizador
  * Nova procura na nuvem (procura em servidores de chaves tradicionais e keybase.io)
  * Suporte a desemparelhar chaves dentro do OpenKeychain
  * Suporte experimental do YubiKey: suporte da geração de assinaturas e desencriptação


## 2.9.2

  * Correção de chaves corrompidas na versão 2.9.1
  * Suporte experimental do YubiKey: desencriptação agora funcional via API


## 2.9.1

  * Divisão do ecrã de encriptar em dois
  * Correção na manipulação de sinalizadores de chaves (suporta agora chaves do Mailvelope 0.7)
  * Melhorias na manipulação de frases-senha
  * Partilha de chave via SafeSlinger
  * Suporte experimental do YubiKey: preferência para permitir outros PINs, atualmente apenas está funcional a assinatura via a API OpenPGP, não dentro do OpenKeychain
  * Correção na utilização de chaves desemparelhadas
  * SHA256 como padrão para compatibilidade
  * A API Intent foi alterada, consulte https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * A API OpenPGP agora manipula chaves revogadas/expiradas e retorna todos os IDs de utilizadores


## 2.9

  * Corrigidos crashes introduzidos na versão v2.8
  * Suporte experimental de ECC
  * Suporte experimental do YubiKey: apenas assinar com chaves importadas


## 2.8

  * Foram corrigidos tantos erros neste lançamento que nos focamos nas novas funcionalidades principais
  * Edição de chaves: fantástico novo design, revogação de chaves
  * Importação de chave: fantástico novo design, conexões seguras aos servidores de chaves via hkps, resolução de servidores via registos DNS SRV
  * Novo ecrã de primeiro acesso
  * Novo ecrã de criação de chave: preencher automaticamente o nome e email baseado nas suas contas pessoais Android
  * Encriptação de ficheiro: fantástico novo design, suporte para encriptar vários ficheiros
  * Novos ícones para mostrar o estado das chaves (por Brennan Novak)
  * Correção de erro importante: agora é possível importar várias chaves grandes de um ficheiro
  * Notificação a mostrar frases-senha em cache
  * As chaves são associadas aos contactos do Android

Este lançamento não seria possível sem o trabalho de Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar

## 2.7

  * Roxo! (Dominik, Vincent)
  * Novo design para visualização de chave (Dominik, Vincent)
  * Novos botões planos do Android (Dominik, Vincent)
  * Correções na API (Dominik)
  * Importação Keybase.io (Tim Bray)


## 2.6.1

  * Algumas correções em erros regressivos


## 2.6

  * Certificações de chaves (obrigado a Vincent Breitmoser)
  * Suporte a chaves secretas parciais do GnuPG (obrigado a Vincent Breitmoser)
  * Novo design na verificação de assinaturas
  * Comprimento de chaves personalizado  (obrigado a Greg Witczak)
  * Corrigida a funcionalidade de partilha de outras aplicações


## 2.5

  * Corrigida a desencriptação de mensagens/ficheiros OpenPGP simétricos
  * Refatoração do ecrã de edição de chaves (obrigado a Ash Hughes)
  * Novo design moderno para ecrãs de encriptação/desencriptação
  *API OpenPGP versão 3 (várias contas API, correções internas, chave de pesquisa)


## 2.4
Obrigado a todos os candidatos do Google Summer of Code 2014 que fizeram este lançamento cheio de funcionalidades e livre de erros!
Além de pequenas retificações, foi feito um número notável de retificações pelas seguintes pessoas (em ordem alfabética):
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser.

  * Nova lista de chaves unificadas
  * Impressão digital de chave colorizada
  * Suporte a portas do servidor de chaves
  * Desativada a possibilidade de gerar chaves fracas
  * Mais trabalho interno na API
  * Certificação de IDs de utilizadores
  * Consulta aos servidores de chaves baseada em saída legível por máquinas
  * Bloqueio da gaveta de navegação em tablets
  * Sugestões para e-mails na criação de chaves
  * Pesquisa em listas de chaves públicas
  * E muito mais melhorias e correções…


## 2.3.1

  * Hotfix para um crash ao atualizar de versões antigas


## 2.3

  * Removida a exportação desnecessária de chaves públicas ao exportar a chave privada (obrigado a Ash Hughes)
  * Corrigida a definição de datas de expiração em chaves (obrigado a Ash Hughes)
  * Mais correções internas ao editar chaves (obrigado a Ash Hughes)
  * Consultar servidores de chaves diretamente no ecrã de importação
  * Correção de layout e estilo dos diálogos no Android 2.2-3.0
  * Correção de crash nas chaves com IDs vazios de utilizadores
  * Correção de crash e listas vazias ao voltar do ecrã de assinatura
  * Bouncy Castle (biblioteca criptográfica) atualizada de 1.47 para 1.50 e compilada da fonte
  * Correção no envio da chave no ecrã de assinatura


## 2.2

  * Novo design com gavetas de navegação
  * Novo design na lista de chaves públicas
  * Nova visualização de chave pública
  * Correções para importação de chaves
  * Certificação cruzada de chaves (obrigado a Ash Hughes)
  * Tratamento apropriado de palavras-passe UTF-8 (obrigado Ash Hughes)
  * Primeira versão com novos idiomas (obrigado aos colaboradores no Transifex)
  * Partilha de chaves via QR Codes corrigida e melhorada
  * Verificação de pacotes de assinatura para API


## 2.1.1

  * Atualizações na API, preparação para integração no K-9 Mail.


## 2.1

  * Várias correções
  * Nova API para programadores
  * Correção PRNG pelo Google


## 2.0

  * Redesenho completo
  * Partilhar chaves públicas por QR Codes, NFC beam
  * Assinar chaves
  * Enviar chaves para servidor
  * Correção de problemas ao importar
  * Nova API AIDL


## 1.0.8

  * Suporte básico a servidores de chaves
  * App2sd
  * Mais opções para a cache da frase-senha: 1, 2, 4, 8 horas
  * Traduções: Norueguês Bokmål (obrigado Sander Danielsen), Chinês (obrigado Zhang Fredrick)
  * Correções
  * Otimizações


## 1.0.7

  * Correção de um problema com a verificação de assinaturas de textos com caractere de nova linha no final
  * Mais opções para o tempo de vida da cache de frase-senha (20, 40, 60 minutos)


## 1.0.6

  * Crash ao adicionar conta no Froyo corrigido
  * Eliminação segura de ficheiros
  * Opção para eliminar ficheiro de chave após a importação
  * Fluxo de encriptação/desencriptação (galeria, etc.)
  * Novas opções (idioma, forçar assinaturas v3)
  * Mudanças na interface
  * Correções


## 1.0.5

  * Traduções para Alemão e Italiano
  * Pacote bem menor, graças à redução das fontes do BC
  * Nova interface nas preferências
  * Ajustes no layout para localização
  * Correção na assinatura


## 1.0.4

  * Corrigido outro crash causado por algum bug no SDK com o query builder


## 1.0.3

  * Correção de crashes durante a encriptação/desencriptação e possivelmente exportação de chaves


## 1.0.2

  * Listas de chaves com filtros
  * Pré-seleção de chaves de encriptação mais inteligente
  * Nova manipulação Intent para VIEW e SEND, permite que os ficheiros sejam encriptados/desencriptados fora de gestor de ficheiros
  * Correções e funcionalidades adicionais (pré-seleção de chaves) para K-9 Mail, novo beta disponível


## 1.0.1

  * A listagem de contas GMail estava disfuncional no 1.0.0, corrigido outra vez


## 1.0.0

  * Integração no K-9 Mail, APG suportando o build beta do K-9 Mail
  * Suporte a mais gestores de ficheiros (incluindo o ASTRO)
  * Tradução Eslovena
  * Nova base de dados, mais rápida e menor consumo de memória
  * Definido Intents e fornecedor de conteúdo para outras aplicações
  * Correções