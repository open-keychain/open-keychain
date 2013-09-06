# Security Model

## Basic goals

* Intents without permissions should only work based on user interaction (e.g. click a button in a dialog)

Android primitives to exchange data: Intent, Intent with return values, Send (also an Intent), Content Provider, AIDL

## Without Permissions

### Intents
All Intents start with ``org.sufficientlysecure.keychain.action.``

* ``android.intent.action.VIEW`` connected to .gpg and .asc files: Import Key and Decrypt
* ``android.intent.action.SEND connected to all mime types (text/plain and every binary data like files and images): Encrypt and Decrypt
* ``IMPORT``
* ``IMPORT_FROM_FILE``
* ``IMPORT_FROM_QR_CODE``
* ``IMPORT_FROM_NFC``
* ``SHARE_KEYRING``
* ``SHARE_KEYRING_WITH_QR_CODE``
* ``SHARE_KEYRING_WITH_NFC``
* ``EDIT_KEYRING``
* ``SELECT_PUBLIC_KEYRINGS``
* ``SELECT_SECRET_KEYRING``
* ``ENCRYPT``
* ``ENCRYPT_FILE``
* ``DECRYPT``
* ``DECRYPT_FILE``

TODO:
- remove IMPORT, SHARE intents, simplify ENCRYPT and DECRYPT intents (include _FILE derivates like done in SEND based on file type)
- EDIT_KEYRING and CREATE_KEYRING, should be available via for registered apps
- new intent REGISTER_APP?