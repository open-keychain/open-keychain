[//]: # (NOTA: ¡Por favor ponga cada frase en su propia línea, Transifex pone cada línea en su propio campo de traducción!)

## 5.1
  * Soporte para Ledger Nano S
  * Soporte para busqueda Web Key Directory (WKD)
  * Se corrigió potencial problema de seguridad de la API

## 5.0
  * Compatibilidad con Autocrypt mejorada

## 4.9

  * Soporte para Curve25519
  * Soporte mejorado para tokens de seguridad

## 4.8

  * Soporte mejorado para tokens USB: Gnuk, modelos de Nitrokey, 4 modelos de Yubikey
  * Característica para encontrar la posición del lector NFC del dispositivo

## 4.7

  * Se mejoró la importación desde portapapeles
  * Asistente de nueva creación para Tokens de Seguridad
  * Se eliminó el ajuste "tiempo de vida" de la caché de contraseña


## 4.6

  * Importe sus claves usando nuestro nuevo mecanismo de Transferencia Wi-Fi Segura


## 4.5

  * Descripción detallada de problemas de seguridad
  * Muestra el estado del servidor de claves para cada clave
  * Soporte para EdDSA
  * Corrige pgp.mit.edu (nuevo certificado)


## 4.4

  * El nuevo estado de la clave muestra información detallada de por qué se considera una clave insegura o defectuosa.


## 4.3

  * Mejor soporte para claves grandes
  * Corrige la importación de ficheros de Gpg4win con codificaciones rotas


## 4.2

  * Soporte experimental para cifrado de curva elíptica con tokens de seguridad
  * Se rediseñó la pantalla de importación de clave
  * Mejoras de diseño para listas de claves
  * Soporte para direcciones onion de sevidor de claves


## 4.1

  * Mejor detección de correos electrónicos y otros contenidos cuando está abierto


## 4.0

  * Soporte experimental para tokens de seguridad sobre USB
  * Permite el cambio de contraseña de claves desnudas


## 3.9

  * Detección y manejo de datos de texto
  * Mejoras de rendimiento
  * Mejoras de la interfaz de usuario para el manejo de tokens de seguridad


## 3.8

  * Edición de clave rediseñada
  * Escoger recordar hora individualmente al introducir contraseñas
  * Importación de clave de Facebook


## 3.7

  * Soporte para Android 6 mejorado (permisos, integración en la selección de texto)
  * API: Versión 10


## 3.6

  * Copias de seguridad cifradas
  * Reparación de fallos de seguridad basadas en auditorias de seguridad externas
  * Asistente de creación de clave YubiKey NEO
  * Soporte MIME interno básico
  * Sincronización automática de clave
  * Característica experimental: Vincular claves a cuentas de Github y Twitter
  * Característica experimental: Confirmación de claves mediante frases
  * Característica experimental: Tema decorativo oscuro
  * API: Versión 9


## 3.5

  * Revocación de clave al borrar clave
  * Comprobaciones mejoradas de criptografía no segura
  * Reparacion: No cierra OpenKeychain tras completar el asistente de primera ejecución
  * API: Versión 8


## 3.4

  * Descarga de clave anónima sobre Tor
  * Soporte para proxy
  * Manejo de errores de YubiKey mejorado


## 3.3

  * Nueva pantalla de descifrado
  * Descifrado de múltiples ficheros a la vez
  * Mejor manejo de errores de YubiKey


## 3.2

  * Primera versión con soporte para YubiKey completo disponible desde la interfaz de usuario: Editar claves, ligar YubiKey a claves...
  * Material design (estilo)
  * Integración de Escaneado de Código QR (se requieren nuevos permisos)
  * Asistente de creación de clave mejorado
  * Repara contactos perdidos después de la sincronización
  * Requiere Android 4
  * Pantalla de clave rediseñada
  * Simplifica las preferencias de criptografía, mejor selección de algoritmos de cifrado seguro
  * API: Firmas desacopladas, selección libre de clave de firmado,...
  * Reparación: Algunas claves válidas se mostraron revocadas o caducadas
  * No acepte algo firmado por subclaves caducadas o revocadas
  * Soporte para Keybase.io en la vista avanzada
  * Método para actualizar todas las claves a la vez


## 3.1.2

  * Repara la exportación de claves a ficheros (ahora de verdad)


## 3.1.1

  * Repara la exportación de claves a ficheros (se escribían parcialmente)
  * Repara una caída en Android 2.3


## 3.1

  * Repara una caída en Android 5
  * Nueva pantalla de certificación
  * Intercambio seguro directamente desde la lista de claves (librería SafeSlinger)
  * Nuevo control de flujo del programa para código QR
  * Pantalla de descifrado rediseñada
  * Nuevo uso y colores del icono
  * Repara la importación de claves secretas (privadas) desde Symantec Encryption Desktop
  * Soporte experimental para YubiKey: Las identificaciones (IDs) de subclaves ahora se comprueban correctamente.


## 3.0.1

  * Mejor manejo de importaciones de claves largas
  * Selección de subclaves mejorada


## 3.0

  * Propone aplicaciones instalables compatibles en la lista de aplicaciones
  * Nuevo diseño para pantallas de descifrado
  * Muchas reparaciones para la importación de claves, también repara claves desnudas
  * Respeta y muestra los distintivos de autentificación de claves
  * Interfaz de usuario para generar claves personalizadas
  * Corregido de certificados de revocación de identificación de usuario
  * Nueva búsqueda en la nube (busca sobre servidores de claves tradicionales y keybase.io)
  * Soporte para desnudar claves dentro de OpenKeychain
  * Soporte experimental para YubiKey: Soporte para generación de firma y descifrado


## 2.9.2

  * Repara claves rotas en la versión 2.9.1
  * Soporte experimental para YubiKey: El descifrado ahora funciona vía API


## 2.9.1

  * Divide en dos la pantalla de cifrado 
  * Repara el manejo de los indicativos de claves (ahora soporta claves de Mailvelope 0.7)
  * Manejo de frase-contraseña mejorado
  * Compartición de claves vía SafeSlinger
  * Soporte experimental para YubiKey: Preferencia para permitir otros PINs, actualmente sólo funciona firmando vía API OpenPGP, no desde dentro de OpenKeychain
  * Repara el uso de claves desnudas
  * SHA256 por defecto para compatibilidad
  * La API de Intent ha cambiado, vea https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * La API de OpenPGP ahora maneja claves revocadas/caducadas y devuelve todas las identificaciones de usuario


## 2.9

  * Repara caídas introducidas en la versión 2.8
  * Soporte para ECC (criptografía de curva elíptica) experimental
  * Soporte experimental para YubiKey: Firmando sólo con claves importadas


## 2.8

  * Han sido reparados tantos fallos en esta versión que nos centramos en las nuevas características principales
  * Edición de clave: Un estupendo diseño nuevo, revocación de clave
  * Importación de clave: Un estupendo diseño nuevo, conexiones seguras a servidor de claves vía hkps (protocolo seguro HTTP de servidor de claves), servidor de claves resolviendo vía registros DNS SRV
  * Nueva pantalla de primera vez
  * Nueva pantalla de creación de clave: Autocompletado del nombre y correo electrónico basados en sus cuentas de Android personales
  * Cifrado de ficheros: Un estupendo diseño nuevo, soporte para cifrar múltiples ficheros
  * Nuevos iconos para mostrar el estado de la clave (por Brennan Novak)
  * Reparación de un fallo importante: La importación de colecciones de claves largas desde un fichero ahora es posible
  * Notificación mostrando frases-contraseña en caché
  * Las claves están conectadas a los contactos de Android

Esta versión no sería posible sin el trabajo de Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar

## 2.7

  * ¡Púrpura! (Dominik, Vincent)
  * Nuevo diseño de vista de clave (Dominik, Vincent)
  * Nuevos botones planos de Android (Dominik, Vincent)
  * Reparaciones de la API (Dominik)
  * Importación desde Keybase.io (Tim Bray)


## 2.6.1

  * Algunas reparaciones para fallos regresivos


## 2.6

  * Certificaciones de clave (gracias a Vincent Breitmoser)
  * Soporte para claves secretas (privadas) parciales de GnuPG (gracias a Vincent Breitmoser)
  * Nuevo diseño para verificación de firma
  * Tamaño de clave personalizado (gracias a Greg Witczak)
  * Repara la funcionalidad-compartida desde otras aplicaciones


## 2.5

  * Repara el descifrado de mensajes/ficheros de OpenPGP simétricos
  * Pantalla de edición de clave refactorizada (gracias a Ash Hughes)
  * Nuevo diseño moderno para pantallas de cifrado/descifrado
  * API de OpenPGP versión 3 (múltiples cuentas API, reparaciones internas, búsqueda de claves)


## 2.4
¡Gracias a todos los interesados del Google Summer of Code 2014 que hicieron posible esta versión rica en características y libre de fallos!
Además de varios parches de seguridad pequeños, un número notable de parches fueron elaborados por las siguientes personas (en orden alfabético):
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser.

  * Nueva lista de claves unificada
  * Huella de validación de clave (fingerprint) coloreada
  * Soporte para puertos de servidor de claves
  * Desactivar la posibilidad de generar claves débiles
  * Mucho más trabajo interno en la API
  * Certificar identificaciones de usuario
  * Petición al servidor de claves basada en salida legible-por-máquina
  * Bloquea la bandeja deslizante de navegación en tabletas
  * Sugerencias sobre creación de claves para correos electrónicos
  * Buscar en listas de claves públicas
  * Y muchas más mejoras y reparaciones...


## 2.3.1

  * Reparación urgente para caída al actualizar desde versiones antiguas


## 2.3

  * Elimina la exportación innecesaria de claves públicas al exportar la clave secreta (privada) (gracias a Ash Hughes)
  * Repara la configuración de fechas de caducidad sobre claves (gracias a Ash Hughes)
  * Más reparaciones internas al editar claves (gracias a Ash Hughes)
  * Realiza peticiones directamente a los servidores de claves desde la pantalla de importación
  * Repara la disposición y estilo del cuadro de diálogo en Android 2.2-3.0
  * Repara caída en claves con identificaciones de usuario vacías
  * Repara caída y listas vacías al volver de la pantalla de firmado
  * Bouncy Castle (librería de criptografía) actualizada desde la versión 1.47 a la 1.50 y compilada desde el código fuente
  * Repara la subida de clave desde la pantalla de firmado


## 2.2

  * Nuevo diseño con bandeja deslizante de navegación
  * Nuevo diseño de lista de claves públicas
  * Nueva vista de clave pública
  * Reparaciones de fallos para la importación de claves
  * Certificación-cruzada de claves (gracias a Ash Hughes)
  * Maneja contraseñas UTF-8 de forma adecuada (gracias a Ash Hughes)
  * Primera versión con nuevos idiomas (gracias a los contribuidores en Transifex)
  * Compartición de claves mediante códigos QR reparada y mejorada
  * Verificación de firmas de paquetes para la API


## 2.1.1

  * Actualizaciones de la API, preparación para la integración de K-9 Mail


## 2.1

  * Muchas reparaciones de fallos
  * Nueva API para desarrolladores
  * Reparación de fallo de PRNG (generador de números pseudoaleatorios) por Google


## 2.0

  * Rediseño completo
  * Comparte claves públicas mediante códigos QR, y NFC
  * Firma de claves
  * Subida de claves al servidor
  * Repara problemas de importación
  * Nueva API de AIDL (lenguaje de definición de interfaz Android)


## 1.0.8

  * Soporte para servidor de claves básico
  * App2sd
  * Más opciones para la caché de frase-contraseña: 1,2,4,8 horas
  * Traducciones: Noruego Bokmål (gracias a Sander Danielsen), Chino (gracias a Zhang Fredrick)
  * Reparaciones de fallos
  * Optimizaciones


## 1.0.7

  * Reparado problema con la verificación de firma de textos con un salto de línea al final
  * Más opciones para el tiempo de vida de la caché de frase-contraseña (20, 40, 60 minutos)


## 1.0.6

  * Reparada caída al añadir cuenta en Froyo
  * Borrado seguro de fichero
  * Opción para borrar el fichero de clave después de importar
  * Cifrado/Descifrado de stream (flujo de datos) (galería, etc.)
  * Nuevas opciones (idioma, forzado de firmas v3)
  * Cambios en la interfaz
  * Reparaciones de fallos


## 1.0.5

  * Traducción al alemán y al italiano
  * Paquete mucho más pequeño, debido al código fuente reducido de Bouncy Castle (BC)
  * Interfaz gráfica (GUI) de nuevas preferencias
  * Ajuste de la distribución para localización
  * Reparación de fallo de firma


## 1.0.4

  * Reparada otra caída causada por algún fallo del SDK con el constructor de peticiones


## 1.0.3

  * Reparadas caídas durante el cifrado/firmado y posible exportación de clave


## 1.0.2

  * Listas de claves filtrables
  * Pre-selección más inteligente de claves de cifrado
  * Nuevo manejo de Intent para VIEW y SEND (ver y enviar), permite que los ficheros sean cifrados/descifrados fuera de los administradores de ficheros
  * Correcciones y características adicionales (preselección de clave) para K-9 Mail, nueva versión beta disponible


## 1.0.1

  * El listado de cuenta de GMail se estropeó en la versión 1.0.0, reparado de nuevo


## 1.0.0

  * Integración de K-9 Mail, APG (Android Privacy Guard) soportando la versión beta de K-9 Mail
  * Soporte para más administradores de ficheros (incluyendo ASTRO)
  * Traducción al esloveno
  * Nueva base de datos, mucho más rápida, menos uso de memoria
  * Definidos Intents y el proveedor de contenido para otras aplicaciones
  * Reparaciones de fallos