[//]: # (NOTA: coloque cada frase em sua própria linha, Transifex coloca cada linha em seu próprio campo de tradução!)

## Confirmação de chave
Sem confirmação, você não pode ter certeza se uma chave realmente corresponde a uma pessoa específica.
A maneira mais simples para confirmar a chave é fazendo a leitura de um código QR ou trocá-lo via NFC.
Para confirmação de chaves entre mais de duas pessoas, sugerimos utilizar o método de troca de chave disponível para suas chaves.

## Estado de chave

<img src="status_signature_verified_cutout_24dp"/>  
Confirmado: Você já confirmou esta chave, por por exemplo, fazendo a leitura de um código QR.  
<img src="status_signature_unverified_cutout_24dp"/>  
Não confirmado: Esta chave ainda não foi confirmada. Você não pode ter a certeza se a chave realmente corresponde a uma pessoa específica.  
<img src="status_signature_expired_cutout_24dp"/>  
Expirada: Esta chave não é mais válida. Somente o proprietário pode extender a sua validade.  
<img src="status_signature_revoked_cutout_24dp"/>  
Revogada: Esta chave não é mais válida. Ela foi revogada pelo seu proprietário.

## Informações avançadas
A "confirmação chave" no OpenKeychain é implementada através da criação de uma certificação, de acordo com o padrão OpenPGP.
Esta certificação é uma ["certificação genérica (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) como descrito na norma:
"O emissor deste certificado não faz qualquer afirmação em particular quanto à forma, que a certificadora verificou que o proprietário da chave é na verdade a pessoa descrita pelo ID de usuário."

Tradicionalmente, certificações (também com níveis mais elevados de certificação, como "certificações positivas" (0x13)) são organizadas em OpenPGP na  Web of Trust.
Nosso modelo de chave confirmação é um conceito muito mais simples para evitar problemas de usabilidade comuns relacionados com Web of Trust.
Nós assumimos que as chaves são verificadas apenas a um determinado grau que ainda, é útil o suficiente para ser executado como "em movimento".
Nós também não implementamos assinaturas (potencialmente transitivas) de confiança ou um de banco de dados de confiança proprietário como no GnuPG.
Além disso, as teclas que contêm, pelo menos, um ID de utilizador certificado por uma chave de confiança, serão marcadas como "confirmado" na listagem de chaves.