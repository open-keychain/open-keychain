This is the old API. Currently disabled!

# Security Model

## Basic goals

* Intents without permissions should only work based on user interaction (e.g. click a button in a dialog)

Android primitives to exchange data: Intent, Intent with return values, Send (also an Intent), Content Provider, AIDL

## Possible Permissions

* ACCESS_API: Encrypt/Sign/Decrypt/Create keys without user interaction (intents, remote service), Read key information (not the actual keys)(content provider)
* ACCESS_KEYS: get and import actual public and secret keys (remote service)


## Without Permissions

### Intents
All Intents start with org.sufficientlysecure.keychain.action.

* android.intent.action.VIEW connected to .gpg and .asc files: Import Key and Decrypt
* android.intent.action.SEND connected to all mime types (text/plain and every binary data like files and images): Encrypt and Decrypt
* IMPORT
* IMPORT_FROM_FILE
* IMPORT_FROM_QR_CODE
* IMPORT_FROM_NFC
* SHARE_KEYRING
* SHARE_KEYRING_WITH_QR_CODE
* SHARE_KEYRING_WITH_NFC
* EDIT_KEYRING
* SELECT_PUBLIC_KEYRINGS
* SELECT_SECRET_KEYRING
* ENCRYPT
* ENCRYPT_FILE
* DECRYPT
* DECRYPT_FILE

## With permission ACCESS_API

### Intents

* CREATE_KEYRING
* ENCRYPT_AND_RETURN
* ENCRYPT_STREAM_AND_RETURN
* GENERATE_SIGNATURE_AND_RETURN
* DECRYPT_AND_RETURN
* DECRYPT_STREAM_AND_RETURN

### Broadcast Receiver
On change of database the following broadcast is send.
* DATABASE_CHANGE

### Content Provider

* The whole content provider requires a permission (only read)
* Don't give out blobs (keys can be accessed by ACCESS_KEYS via remote service)
* Make an internal and external content provider (or pathes with <path-permission>)
* Look at android:grantUriPermissions especially for ApgServiceBlobProvider
* Only give out android:readPermission

### ApgApiService (Remote Service)
AIDL service

## With permission ACCESS_KEYS

### ApgKeyService (Remote Service)
AIDL service to access actual private keyring objects