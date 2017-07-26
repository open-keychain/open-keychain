[//]: # (NOTA: ¡Por favor ponga cada frase en su propia línea, Transifex pone cada línea en su propio campo de traducción!)

## Confirmación de clave
Sin confirmación, no puede estar seguro de si una clave corresponde realmente a una persona específica.
La forma más sencilla de confirmar una clave es escaneando el código QR o intercambiándolo vía NFC.
Para confirmar las claves entre más de dos personas, sugerimos usar el método de intercambio de clave disponible para sus claves.

## Estado de la clave

<img src="status_signature_verified_cutout_24dp"/>  
Confirmada: Ya ha confirmado esta clave, ej. al escanear el código QR.  
<img src="status_signature_unverified_cutout_24dp"/>  
No confirmada: Esta clave no ha sido confirmada aún. No puede estar seguro de si la clave corresponde realmente a una persona específica.  
<img src="status_signature_expired_cutout_24dp"/>  
Caducada: Esta clave ya no es válida. Sólo el propietario puede extender su validez.  
<img src="status_signature_revoked_cutout_24dp"/>  
Revocada: Esta clave ya no es válida. Ha sido revocada por su propietario.

## Información avanzada
Una "confirmación de clave" en OpenKeychain se implementa al crear una certificación de acuerdo al estándar OpenPGP.
Esta certificación es una ["certificación genérica (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) descrita en el estándar por:
"El emisor de esta certificación no hace ninguna afirmación particular acerca de lo bien o mal que el certificador ha comprobado que el propietario de la clave sea de hecho la persona descrita por la identificación del usuario."

Tradicionalmente, las certificaciones (también con niveles más altos de certificación, tales como "certificaciones positivas" (0x13)) se organizan en la Web of Trust (web de confianza) de OpenPGP.
Nuestro modelo de confirmación de clave es un concepto mucho más simple para evitar problemas comunes de usabilidad relacionados con esta Web of Trust.
Asumimos que las claves están verificadas sólo hasta cierto grado, que no obstante es suficientemente usable para ejecutarse "sobre la marcha".
Tampoco implementamos firmas de confianza (potencialmente transitivas) o una base de datos ownertrust (de valores de confianza subjetivos del propietario) como en GnuPG.
Más aún, las claves que contengan al menos una identificación de usuario certificada por una clave de confianza se marcarán como "confirmadas" en los listados de claves.