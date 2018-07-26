[//]: # (NOTA: coloque cada sentença em sua própria linha, o Transifex coloca cada linha em seu próprio campo de tradução!)

## Confirmação de Chave
Sem a confirmação, você não pode estar seguro de que uma chave realmente corresponde a uma pessoa específica.
A maneira mais simples para confirmar a chave é fazendo a leitura de um QR Code ou trocá-la via NFC.
Para confirmação de chaves entre mais de duas pessoas, sugerimos utilizar o método de troca de chave disponível para suas chaves.

## Estado da Chave

<img src="status_signature_verified_cutout_24dp"/>  
Confirmado: Você já confirmou esta chave ao, por exemplo, lê-la de um QR Code.  
<img src="status_signature_unverified_cutout_24dp"/>  
Não confirmado: Esta chave ainda não foi confirmada. Você não pode ter certeza que a chave realmente corresponde a uma pessoa específica.  
<img src="status_signature_expired_cutout_24dp"/>  
Expirada: Esta chave não é mais válida. Somente o proprietário pode extender a sua validade.  
<img src="status_signature_revoked_cutout_24dp"/>  
Revogada: Esta chave não é mais válida. Ela foi revogada pelo seu proprietário.

## Informações Avançadas
A "confirmação de chave" no OpenKeychain é implementada através da criação de uma certificação, de acordo com o padrão OpenPGP.
Esta certificação é uma ["certificação genérica (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) como descrito na norma:
"O emissor deste certificado não faz qualquer afirmação a respeito de quão bem o certificador verificou que o proprietário da chave é, de fato, a pessoa descrita na ID de usuário."

Tradicionalmente, certificações (também com níveis mais elevados de certificação, como "certificações positivas" (0x13)) são organizadas na Teia de Confiança OpenPGP.
Nosso modelo de chave confirmação é um conceito muito mais simples para evitar problemas de usabilidade comuns relacionados com a Teia de Confiança.
Nós assumimos que as chaves são verificadas apenas a um determinado grau que ainda é útil o suficiente para ser executada "em movimento".
Nós também não implementamos assinaturas (potencialmente transitivas) de confiança ou um de banco de dados de confiança como no GnuPG.
Além disso, chaves que contenham pelo menos um ID de usuário certificado por uma chave de confiança, serão marcadas como "confirmadas" na lista de chaves.