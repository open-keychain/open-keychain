[//]: # (Observe: ¡Por favor ingrese cada enunciado en su propia línea, Transifex coloca cada línea en su propio campo de traducción!)

## Confirmación de clave
Sin confirmación, no puedes estar seguro si una clave es realmente de quien dice ser.
La forma más sencilla de confirmar una clave es escaneando el código QR o intercambiándola por NFC.
To confirm keys between more than two persons, we suggest using the key exchange method available for your keys.

## Estado de la clave

<img src="status_signature_verified_cutout_24dp"/>  
Confirmado: ya ha confirmado esta clave, por ejemplo, al escanear el código QR.  
<img src="status_signature_unverified_cutout_24dp"/>  
Sin confirmar: esta clave no ha sido confirmada aún. No puede estar seguro de que la clave es de quien dice ser.  
<img src="status_signature_expired_cutout_24dp"/>  
Expirada: Esta clave ya no es válida. Sólo el propietario puede extender su validez.  
<img src="status_signature_revoked_cutout_24dp"/>  
Revocada: Esta clave ya no es válida. Ha sido revocada por su propietario.

## Información avanzada
Una "confirmación de clave" en OpenKeychain está implementada mediante la creación de un certificado de acuerdo al estándar OpenPGP.
Este certificado es un ["certificado genérico (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) descrito en el estándar por:
"El emisor de este certificado no hace ninguna afirmación en particular en cuanto a qué tan bien el certificador ha comprobado que el propietario de la clave es de hecho la persona descrita por el ID de usuario."

Tradicionalmente, los certificador (incluso con niveles altos de certificación, como las "certificaciones positivas" (0x13)) se organizan en la web de confianza de OpenPGP.
Nuestro modelo de confirmación de clave es un concepto mucho más simple para evitar problemas comunes de usabilidad relacionados con esta web de confianza.
Suponemos que las claves se verifican sólo hasta cierto punto donde todavía es lo suficientemente útil para ser ejecutado "al vuelo".
Tampoco implementamos firmas de confianza (potencialmente transitivo) o una base de datos como en GnuPG.
Además, las claves que contienen al menos un ID de usuario certificado por una clave de confianza serán marcados como "confirmado" en las listas de claves.