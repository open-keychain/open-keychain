[//]: # (NOTA: Por avor escriba cada frase na súa propia liña, Transifex pon cada liña no seu propio campo da tradución!)

## Confirmación de chave
Sen confirmación non podes estar seguro de se a chave corresponde realmente a unha persoa concreta.
A forma máis simple para confirmar unha chave é escaneando o código QR ou intercambiándoa vía NFC.
Para confirmar chaves entre máis de dúas persoas, suxerimos usar o método de intercambio de chaves disponíbel para as túas chaves.

## Estado da chave

<img src="status_signature_verified_cutout_24dp"/>  
Confirmada: Xa confirmaches esta chave, por exemplo, escaneando o código QR.  
<img src="status_signature_unverified_cutout_24dp"/>  
Sen confirmar: Esta chave aínda non foi confirmada. Non pode estar segura de que esta chave corresponde realmente a unha persoa concreta.  
<img src="status_signature_expired_cutout_24dp"/>  
Expirada: Esta chave xa non é válida. Só o dono pode extender a súa validez.  
<img src="status_signature_revoked_cutout_24dp"/>  
Revogada: Esta chave xa non é válida. Foi revogada polo seu dono.

## Información avanzada
As "confirmacións de chave" en OpenKeychain impleméntanse mediante a creación dunha certificación de acordo co estándar OpenPGP.
Esta certificación é unha  ["certificación xenérica (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) descrita no estándar por:
"O emisor desta certificación non fai ningunha afirmación particular sobre a corrección coa que o certificador fixo a comprobación de que o propietario da clave é de feito a persoa descrita polo ID de usuario."

Tradicionalmente, as certificacións (tamén con niveis de certificación máis altos, como as "certificacións positivas" (0x13)) están organizadas na Web de Confianza de OpenPGP.
O noso modelo de confirmación de chave é moito máis simple en concepto para evitar os problemas de usabilidade relacionada con esta Web de Confianza.
Asumimos que as chaves son verificadas só até certo grao que mantén a usabilidade suficiente para ser executada "sobre a marcha".
Tampouco implementamos sinaturas de confianza (potencialmente transitivas) ou unha base de datos de confianza de donos como en GnuPG.
Ademais, as chaves que conteñan polo menos un ID de usuario certificado por unha chave de confianza serán marcadas como "confirmadas" nas listaxes de chaves.