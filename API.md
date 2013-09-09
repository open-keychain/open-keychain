# Security Model

## Basic goals

* Intents invoked by apps that are not registered by Keychain's App API must require user interaction (e.g. click a button in a dialog to actually encrypt!)

## Without Permissions

### Intents
These Intents require user interaction!

All Intents start with ``org.sufficientlysecure.keychain.action.``

* ``android.intent.action.VIEW`` connected to .gpg and .asc files: Import Key and Decrypt
* ``android.intent.action.SEND connected to all mime types (text/plain and every binary data like files and images): Encrypt and Decrypt


* ``KEY_IMPORT`` with extra "keyring_bytes" or Uri in data with file schema
* ``KEY_IMPORT_FROM_QR_CODE`` without extras


* ``ENCRYPT`` TODO: explain extras (see source)
* ``ENCRYPT_FILE``

* ``DECRYPT`` TODO: explain extras (see source)
* ``DECRYPT_FILE``

TODO:
- new intent REGISTER_APP?

## App API
