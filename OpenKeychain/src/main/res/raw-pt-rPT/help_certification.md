[//]: # (NOTA: por favor coloque cada frase na sua própria linha. O Transifex coloca todas as linhas no seu próprio campo de tradução!)

## Confirmação da chave
Sem a confirmação, não pode ter a certeza que uma chave corresponde realmente a uma pessoa específica.
A maneira mais simples de confirmar a chave é fazendo a leitura de um QR Code ou trocá-lo via NFC.
Para confirmar chaves entre mais de duas pessoas, sugerimos utilizar o método de troca de chave disponível para as suas chaves.

## Estado da chave

<img src="status_signature_verified_cutout_24dp"/>  
Confirmada: já confirmou esta chave ao, por exemplo, lê-la num QR Code.  
<img src="status_signature_unverified_cutout_24dp"/>  
Não confirmada: esta chave ainda não foi confirmada. Não pode ter certeza que a chave corresponde realmente a uma pessoa específica.  
<img src="status_signature_expired_cutout_24dp"/>  
Expirada: esta chave já não é válida. Apenas o proprietário dela pode prolongar a sua validade.  
<img src="status_signature_revoked_cutout_24dp"/>  
Revogada: esta chave já não é válida. Ela foi revogada pelo seu proprietário.

## Informações avançadas
A "confirmação de chave" no OpenKeychain é implementada através da criação de uma certificação, de acordo com o padrão OpenPGP.
Esta certificação é uma ["certificação genérica (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) como descrito na norma:
"O emissor deste certificado não faz qualquer afirmação a sobre quão bem o certificador verificou que o proprietário da chave é, de facto, a pessoa descrita na identificação do utilizador."

Tradicionalmente, as certificações (também com níveis mais elevados de certificação, como "certificações positivas" (0x13)) são organizadas na Teia de Confiança OpenPGP.
O nosso modelo confirmação de chave é um conceito muito mais simples para evitar problemas de usabilidade comuns relacionados com a Teia de Confiança.
Nós assumimos que as chaves são verificadas apenas a um determinado grau, que ainda é útil o suficiente para ser executada "no momento".
Também não implementamos assinaturas (potencialmente transitivas) de confiança ou um de base de dados de confiança como no GnuPG.
Além disso, as chaves que contenham pelo menos uma identificação de utilizador certificada por uma chave de confiança, serão marcadas como "confirmadas" na lista de chaves.