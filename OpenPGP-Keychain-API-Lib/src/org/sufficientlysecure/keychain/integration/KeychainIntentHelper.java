/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2011 K-9 Mail Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.integration;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.Toast;

public class KeychainIntentHelper {

    public static final String APG_INTENT_PREFIX = "org.thialfihar.android.apg.intent.";

    // Intents
    /**
     * Encrypt
     */
    // without permission
    public static final String ACTION_ENCRYPT = APG_INTENT_PREFIX + "ENCRYPT";
    public static final String ACTION_ENCRYPT_FILE = APG_INTENT_PREFIX + "ENCRYPT_FILE";

    // with permission
    public static final String ACTION_ENCRYPT_AND_RETURN = APG_INTENT_PREFIX + "ENCRYPT_AND_RETURN";
    public static final String ACTION_GENERATE_SIGNATURE_AND_RETURN = APG_INTENT_PREFIX
            + "GENERATE_SIGNATURE_AND_RETURN";
    public static final String ACTION_ENCRYPT_STREAM_AND_RETURN = APG_INTENT_PREFIX
            + "ENCRYPT_STREAM_AND_RETURN";

    /**
     * Decrypt
     */
    // without permission
    public static final String ACTION_DECRYPT = APG_INTENT_PREFIX + "DECRYPT";
    public static final String ACTION_DECRYPT_FILE = APG_INTENT_PREFIX + "DECRYPT_FILE";

    // with permission
    public static final String ACTION_DECRYPT_AND_RETURN = APG_INTENT_PREFIX + "DECRYPT_AND_RETURN";
    public static final String ACTION_DECRYPT_STREAM_AND_RETURN = APG_INTENT_PREFIX
            + "DECRYPT_STREAM_AND_RETURN";

    /**
     * Select keys, without permission
     */
    public static final String ACTION_SELECT_PUBLIC_KEYS = APG_INTENT_PREFIX + "SELECT_PUBLIC_KEYS";
    public static final String ACTION_SELECT_SECRET_KEY = APG_INTENT_PREFIX + "SELECT_SECRET_KEY";

    /**
     * Create key/edit key, without permission
     */
    public static final String ACTION_CREATE_KEY = APG_INTENT_PREFIX + "CREATE_KEY";
    public static final String ACTION_EDIT_KEY = APG_INTENT_PREFIX + "EDIT_KEY";

    /**
     * Import actions
     */
    public static final String ACTION_IMPORT = APG_INTENT_PREFIX + "IMPORT";

    // only used by IMPORT
    public static final String EXTRA_IMPORT_TEXT = "text";
    public static final String EXTRA_IMPORT_KEYRING_BYTES = "keyringBytes";

    public static final String ACTION_IMPORT_FROM_FILE = APG_INTENT_PREFIX + "IMPORT_FROM_FILE";
    public static final String ACTION_IMPORT_FROM_QR_CODE = APG_INTENT_PREFIX
            + "IMPORT_FROM_QR_CODE";

    /**
     * Share actions
     */
    public static final String ACTION_SHARE_KEYRING = APG_INTENT_PREFIX + "SHARE_KEYRING";
    public static final String ACTION_SHARE_KEYRING_WITH_QR_CODE = APG_INTENT_PREFIX
            + "SHARE_KEYRING_WITH_QR_CODE";
    public static final String ACTION_SHARE_KEYRING_WITH_NFC = APG_INTENT_PREFIX
            + "SHARE_KEYRING_WITH_NFC";

    // used by SHARE_WITH_QR_CODE and SHARE_WITH_NFC
    public static final String EXTRA_MASTER_KEY_ID = "masterKeyId";

    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_DECRYPTED_MESSAGE = "decryptedMessage";
    public static final String EXTRA_ENCRYPTED_MESSAGE = "encryptedMessage";
    public static final String EXTRA_SIGNATURE = "signature";
    public static final String EXTRA_SIGNATURE_KEY_ID = "signatureKeyId";
    public static final String EXTRA_SIGNATURE_USER_ID = "signatureUserId";
    public static final String EXTRA_SIGNATURE_SUCCESS = "signatureSuccess";
    public static final String EXTRA_SIGNATURE_UNKNOWN = "signatureUnknown";
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_USER_IDS = "userIds";
    public static final String EXTRA_KEY_ID = "masterKeyId";
    public static final String EXTRA_ENCRYPTION_KEY_IDS = "encryptionKeyIds";
    public static final String EXTRA_SELECTION = "selection";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_NO_PASSPHRASE = "noPassphrase";
    public static final String EXTRA_GENERATE_DEFAULT_KEYS = "generateDefaultKeys";
    public static final String EXTRA_INTENT_VERSION = "intentVersion";

    public static final String RESULT_EXTRA_MASTER_KEY_IDS = "masterKeyIds";
    public static final String RESULT_EXTRA_USER_IDS = "userIds";

    // result from EditKey
    public static final String RESULT_EXTRA_MASTER_KEY_ID = "masterKeyId";
    public static final String RESULT_EXTRA_USER_ID = "userId";

    public static final String INTENT_VERSION = "1";

    public static final int DECRYPT_MESSAGE = 0x00007121;
    public static final int ENCRYPT_MESSAGE = 0x00007122;
    public static final int SELECT_PUBLIC_KEYS = 0x00007123;
    public static final int SELECT_SECRET_KEY = 0x00007124;
    public static final int CREATE_KEY = 0x00007125;
    public static final int EDIT_KEY = 0x00007126;

    private Activity activity;

    public KeychainIntentHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * Start an activity.<br>
     * This method is defined to allow different methods of activity starting for newer versions of
     * Android and for compatibility library.
     * 
     * @param intent
     *            Intent to start.
     * @param code
     *            Request code for the activity
     * @see android.app.Activity#startActivityForResult(Intent, int)
     * @see android.app.Fragment#startActivityForResult(Intent, int)
     */
    protected void startActivityForResult(Intent intent, int code) {
        activity.startActivityForResult(intent, code);
    }

    /**
     * Open activity to scan qr code and import key in it
     * 
     * @return true when activity was found and executed successfully
     */
    public boolean importFromQrCode() {
        Intent intent = new Intent(ACTION_IMPORT_FROM_QR_CODE);
        intent.putExtra(EXTRA_INTENT_VERSION, INTENT_VERSION);
        try {
            startActivityForResult(intent, -1);
            return true;
        } catch (ActivityNotFoundException e) {
            activityNotFound();
            return false;
        }
    }

    /**
     * Opens activity with qr code for masterKeyId
     * 
     * @return true when activity was found and executed successfully
     */
    public boolean shareWithQrCode(long masterKeyId) {
        Intent intent = new Intent(ACTION_SHARE_KEYRING_WITH_QR_CODE);
        intent.putExtra(EXTRA_INTENT_VERSION, INTENT_VERSION);
        intent.putExtra(EXTRA_MASTER_KEY_ID, masterKeyId);
        try {
            startActivityForResult(intent, -1);
            return true;
        } catch (ActivityNotFoundException e) {
            activityNotFound();
            return false;
        }
    }

    /**
     * Opens activity to share keyring withnc based on given masterKeyId
     * 
     * @return true when activity was found and executed successfully
     */
    public boolean shareWithNfc(long masterKeyId) {
        Intent intent = new Intent(ACTION_SHARE_KEYRING_WITH_NFC);
        intent.putExtra(EXTRA_INTENT_VERSION, INTENT_VERSION);
        intent.putExtra(EXTRA_MASTER_KEY_ID, masterKeyId);
        try {
            startActivityForResult(intent, -1);
            return true;
        } catch (ActivityNotFoundException e) {
            activityNotFound();
            return false;
        }
    }

    /**
     * Opens APG activity to create new key
     * 
     * @param userIds
     *            value to specify prefilled values for user that should be created
     * @return true when activity was found and executed successfully
     */
    public boolean createNewKey(String userIds, boolean noPassphrase, boolean generateDefaultKeys) {
        Intent intent = new Intent(ACTION_CREATE_KEY);
        if (userIds != null) {
            intent.putExtra(EXTRA_USER_IDS, userIds);
        }
        intent.putExtra(EXTRA_NO_PASSPHRASE, noPassphrase);
        intent.putExtra(EXTRA_GENERATE_DEFAULT_KEYS, generateDefaultKeys);

        intent.putExtra(EXTRA_INTENT_VERSION, INTENT_VERSION);
        try {
            startActivityForResult(intent, CREATE_KEY);
            return true;
        } catch (ActivityNotFoundException e) {
            activityNotFound();
            return false;
        }
    }

    /**
     * Opens APG activity to create new key
     * 
     * @return true when activity was found and executed successfully
     */
    public boolean createNewKey() {
        return createNewKey(null, false, false);
    }

    /**
     * Opens APG activity to edit already existing key based on keyId
     * 
     * @param keyId
     * @return true when activity was found and executed successfully
     */
    public boolean editKey(long keyId) {
        Intent intent = new Intent(ACTION_EDIT_KEY);
        intent.putExtra(EXTRA_KEY_ID, keyId);
        intent.putExtra(EXTRA_INTENT_VERSION, INTENT_VERSION);
        try {
            startActivityForResult(intent, EDIT_KEY);
            return true;
        } catch (ActivityNotFoundException e) {
            activityNotFound();
            return false;
        }
    }

    /**
     * Opens APG activity to select the signature key.
     * 
     * @return true when activity was found and executed successfully
     */
    public boolean selectSecretKey() {
        Intent intent = new Intent(ACTION_SELECT_SECRET_KEY);
        intent.putExtra(EXTRA_INTENT_VERSION, INTENT_VERSION);
        try {
            startActivityForResult(intent, SELECT_SECRET_KEY);
            return true;
        } catch (ActivityNotFoundException e) {
            activityNotFound();
            return false;
        }
    }

    /**
     * Encrypts the given data by opening APGs encrypt activity. If encryptionKeys are given it
     * encrypts immediately and goes back to your program after that
     * 
     * @param data
     *            String that contains the message to be encrypted
     * @param encryptionKeyIds
     *            long[] that holds the ids of the encryption keys
     * @param signatureKeyId
     *            id of the signature key
     * @return true when activity was found and executed successfully
     */
    public boolean encrypt(String data, long[] encryptionKeyIds, long signatureKeyId,
            boolean returnResult) {
        Intent intent = new Intent();
        if (returnResult) {
            intent.setAction(ACTION_ENCRYPT_AND_RETURN);
        } else {
            intent.setAction(ACTION_ENCRYPT);
        }
        intent.putExtra(EXTRA_INTENT_VERSION, INTENT_VERSION);
        intent.setType("text/plain");
        intent.putExtra(EXTRA_TEXT, data);
        intent.putExtra(EXTRA_ENCRYPTION_KEY_IDS, encryptionKeyIds);
        intent.putExtra(EXTRA_SIGNATURE_KEY_ID, signatureKeyId);
        try {
            startActivityForResult(intent, ENCRYPT_MESSAGE);
            return true;
        } catch (ActivityNotFoundException e) {
            activityNotFound();
            return false;
        }
    }

    /**
     * Start the decrypt activity.
     * 
     * @param activity
     * @param data
     * @param pgpData
     * @return true when activity was found and executed successfully
     */
    public boolean decrypt(String data, boolean returnResult) {
        Intent intent = new Intent();
        if (returnResult) {
            intent.setAction(ACTION_DECRYPT_AND_RETURN);
        } else {
            intent.setAction(ACTION_DECRYPT);
        }
        intent.putExtra(EXTRA_INTENT_VERSION, INTENT_VERSION);
        intent.setType("text/plain");
        if (data == null) {
            return false;
        }
        try {
            intent.putExtra(EXTRA_TEXT, data);
            startActivityForResult(intent, DECRYPT_MESSAGE);
            return true;
        } catch (ActivityNotFoundException e) {
            activityNotFound();
            return false;
        }
    }

    /**
     * Handle the activity results that concern us.
     * 
     * @param requestCode
     * @param resultCode
     * @param data
     * @return handled or not
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data, KeychainData apgData) {

        switch (requestCode) {
        case CREATE_KEY:
            if (resultCode != Activity.RESULT_OK || data == null) {
                // user canceled!
                break;
            }
            apgData.setSecretKeyId(data.getLongExtra(RESULT_EXTRA_MASTER_KEY_ID, 0));
            apgData.setSecretKeyUserId(data.getStringExtra(RESULT_EXTRA_USER_ID));

            break;
        case SELECT_SECRET_KEY:
            if (resultCode != Activity.RESULT_OK || data == null) {
                // user canceled!
                break;
            }
            apgData.setSecretKeyId(data.getLongExtra(EXTRA_KEY_ID, 0));
            apgData.setSecretKeyUserId(data.getStringExtra(EXTRA_USER_ID));

            break;
        case SELECT_PUBLIC_KEYS:
            if (resultCode != Activity.RESULT_OK || data == null) {
                apgData.setPublicKeyIds(null);
                break;
            }
            apgData.setPublicKeyIds(data.getLongArrayExtra(RESULT_EXTRA_MASTER_KEY_IDS));
            apgData.setPublicUserIds(data.getStringArrayExtra(RESULT_EXTRA_USER_IDS));

            break;
        case ENCRYPT_MESSAGE:
            if (resultCode != Activity.RESULT_OK || data == null) {
                apgData.setPublicKeyIds(null);
                break;
            }
            apgData.setEncryptedData(data.getStringExtra(EXTRA_ENCRYPTED_MESSAGE));

            break;
        case DECRYPT_MESSAGE:
            if (resultCode != Activity.RESULT_OK || data == null) {
                break;
            }

            apgData.setSecretKeyUserId(data.getStringExtra(EXTRA_SIGNATURE_USER_ID));
            apgData.setSecretKeyId(data.getLongExtra(EXTRA_SIGNATURE_KEY_ID, 0));
            apgData.setSignatureSuccess(data.getBooleanExtra(EXTRA_SIGNATURE_SUCCESS, false));
            apgData.setSignatureUnknown(data.getBooleanExtra(EXTRA_SIGNATURE_UNKNOWN, false));

            apgData.setDecryptedData(data.getStringExtra(EXTRA_DECRYPTED_MESSAGE));

            break;
        default:
            return false;
        }

        return true;
    }

    /**
     * Select encryption keys.
     * 
     * @param emails
     *            The emails that should be used for preselection.
     * @return true when activity was found and executed successfully
     */
    public boolean selectPublicKeys(String emails) {
        return selectPublicKeys(emails, null);
    }

    /**
     * Select encryption keys.
     * 
     * @param emails
     *            The emails that should be used for preselection.
     * @param apgData
     *            ApgData with encryption keys and signature keys preselected
     * @return true when activity was found and executed successfully
     */
    public boolean selectPublicKeys(String emails, KeychainData apgData) {
        Intent intent = new Intent(ACTION_SELECT_PUBLIC_KEYS);
        intent.putExtra(EXTRA_INTENT_VERSION, INTENT_VERSION);

        long[] initialKeyIds = null;
        if (apgData == null || !apgData.hasPublicKeys()) {
            KeychainContentProviderHelper cPHelper = new KeychainContentProviderHelper(activity);

            initialKeyIds = cPHelper.getPublicKeyringIdsByEmail(emails);
        } else {
            initialKeyIds = apgData.getPublicKeys();
        }
        intent.putExtra(EXTRA_SELECTION, initialKeyIds);

        try {
            startActivityForResult(intent, SELECT_PUBLIC_KEYS);
            return true;
        } catch (ActivityNotFoundException e) {
            activityNotFound();
            return false;
        }
    }

    private void activityNotFound() {
        Toast.makeText(activity, "APG Activity not found! Is APG installed correctly?",
                Toast.LENGTH_LONG).show();
    }
}
