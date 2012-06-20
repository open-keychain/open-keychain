/*
 * Copyright (C) 2010-2011 K-9 Mail Contributors
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.thialfihar.android.apg.integration;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class ApgIntentHelper {
    private Activity activity;

    public ApgIntentHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * Opens APG activity to create new key
     * 
     * @param userIds
     *            value to specify prefilled values for user that should be created
     * @return true when activity was found and executed successfully
     */
    public boolean createNewKey(String userIds, boolean noPassphrase, boolean generateDefaultKeys) {
        Intent intent = new Intent(Constants.Intent.CREATE_KEY);
        if (userIds != null) {
            intent.putExtra(Constants.EXTRA_USER_IDS, userIds);
        }
        intent.putExtra(Constants.EXTRA_NO_PASSPHRASE, noPassphrase);
        intent.putExtra(Constants.EXTRA_GENERATE_DEFAULT_KEYS, generateDefaultKeys);

        intent.putExtra(Constants.EXTRA_INTENT_VERSION, Constants.INTENT_VERSION);
        try {
            activity.startActivityForResult(intent, Constants.CREATE_KEY);
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
        Intent intent = new Intent(Constants.Intent.EDIT_KEY);
        intent.putExtra(Constants.EXTRA_KEY_ID, keyId);
        intent.putExtra(Constants.EXTRA_INTENT_VERSION, Constants.INTENT_VERSION);
        try {
            activity.startActivityForResult(intent, Constants.EDIT_KEY);
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
        Intent intent = new Intent(Constants.Intent.SELECT_SECRET_KEY);
        intent.putExtra(Constants.EXTRA_INTENT_VERSION, Constants.INTENT_VERSION);
        try {
            activity.startActivityForResult(intent, Constants.SELECT_SECRET_KEY);
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
    public boolean encrypt(String data, long[] encryptionKeyIds, long signatureKeyId) {
        Intent intent = new Intent(Constants.Intent.ENCRYPT_AND_RETURN);
        intent.putExtra(Constants.EXTRA_INTENT_VERSION, Constants.INTENT_VERSION);
        intent.setType("text/plain");
        intent.putExtra(Constants.EXTRA_TEXT, data);
        intent.putExtra(Constants.EXTRA_ENCRYPTION_KEY_IDS, encryptionKeyIds);
        intent.putExtra(Constants.EXTRA_SIGNATURE_KEY_ID, signatureKeyId);
        try {
            activity.startActivityForResult(intent, Constants.ENCRYPT_MESSAGE);
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
    public boolean decrypt(String data) {
        Intent intent = new Intent(Constants.Intent.DECRYPT_AND_RETURN);
        intent.putExtra(Constants.EXTRA_INTENT_VERSION, Constants.INTENT_VERSION);
        intent.setType("text/plain");
        if (data == null) {
            return false;
        }
        try {
            intent.putExtra(Constants.EXTRA_TEXT, data);
            activity.startActivityForResult(intent, Constants.DECRYPT_MESSAGE);
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
    public boolean onActivityResult(int requestCode, int resultCode, Intent data, ApgData apgData) {

        switch (requestCode) {
        case Constants.SELECT_SECRET_KEY:
            if (resultCode != Activity.RESULT_OK || data == null) {
                // user canceled!
                break;
            }
            apgData.setSignatureKeyId(data.getLongExtra(Constants.EXTRA_KEY_ID, 0));
            apgData.setSignatureUserId(data.getStringExtra(Constants.EXTRA_USER_ID));
            break;

        case Constants.SELECT_PUBLIC_KEYS:
            if (resultCode != Activity.RESULT_OK || data == null) {
                apgData.setEncryptionKeys(null);
                break;
            }
            apgData.setEncryptionKeys(data.getLongArrayExtra(Constants.EXTRA_SELECTION));
            break;

        case Constants.ENCRYPT_MESSAGE:
            if (resultCode != Activity.RESULT_OK || data == null) {
                apgData.setEncryptionKeys(null);
                break;
            }
            apgData.setEncryptedData(data.getStringExtra(Constants.EXTRA_ENCRYPTED_MESSAGE));
            break;

        case Constants.DECRYPT_MESSAGE:
            if (resultCode != Activity.RESULT_OK || data == null) {
                break;
            }

            apgData.setSignatureUserId(data.getStringExtra(Constants.EXTRA_SIGNATURE_USER_ID));
            apgData.setSignatureKeyId(data.getLongExtra(Constants.EXTRA_SIGNATURE_KEY_ID, 0));
            apgData.setSignatureSuccess(data.getBooleanExtra(Constants.EXTRA_SIGNATURE_SUCCESS,
                    false));
            apgData.setSignatureUnknown(data.getBooleanExtra(Constants.EXTRA_SIGNATURE_UNKNOWN,
                    false));

            apgData.setDecryptedData(data.getStringExtra(Constants.EXTRA_DECRYPTED_MESSAGE));
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
    public boolean selectEncryptionKeys(String emails) {
        return selectEncryptionKeys(emails, null);
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
    public boolean selectEncryptionKeys(String emails, ApgData apgData) {
        Intent intent = new Intent(Constants.Intent.SELECT_PUBLIC_KEYS);
        intent.putExtra(Constants.EXTRA_INTENT_VERSION, Constants.INTENT_VERSION);

        long[] initialKeyIds = null;
        if (apgData == null || !apgData.hasEncryptionKeys()) {
            List<Long> keyIds = new ArrayList<Long>();
            if (apgData != null && apgData.hasSignatureKey()) {
                keyIds.add(apgData.getSignatureKeyId());
            }

            try {
                Uri contentUri = Uri.withAppendedPath(
                        Constants.CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS, emails);
                Cursor c = activity.getContentResolver().query(contentUri,
                        new String[] { "master_key_id" }, null, null, null);
                if (c != null) {
                    while (c.moveToNext()) {
                        keyIds.add(c.getLong(0));
                    }
                }

                if (c != null) {
                    c.close();
                }
            } catch (SecurityException e) {
                insufficientPermissions();
            }
            if (!keyIds.isEmpty()) {
                initialKeyIds = new long[keyIds.size()];
                for (int i = 0, size = keyIds.size(); i < size; ++i) {
                    initialKeyIds[i] = keyIds.get(i);
                }
            }
        } else {
            initialKeyIds = apgData.getEncryptionKeys();
        }
        intent.putExtra(Constants.EXTRA_SELECTION, initialKeyIds);

        try {
            activity.startActivityForResult(intent, Constants.SELECT_PUBLIC_KEYS);
            return true;
        } catch (ActivityNotFoundException e) {
            activityNotFound();
            return false;
        }
    }

    /**
     * Get secret key ids based on a given email.
     * 
     * @param context
     * @param email
     *            The email in question.
     * @return key ids
     */
    public long[] getSecretKeyIdsFromEmail(String email) {
        long ids[] = null;
        try {
            Uri contentUri = Uri.withAppendedPath(Constants.CONTENT_URI_SECRET_KEY_RING_BY_EMAILS,
                    email);
            Cursor c = activity.getContentResolver().query(contentUri,
                    new String[] { "master_key_id" }, null, null, null);
            if (c != null && c.getCount() > 0) {
                ids = new long[c.getCount()];
                while (c.moveToNext()) {
                    ids[c.getPosition()] = c.getLong(0);
                }
            }

            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            insufficientPermissions();
        }

        return ids;
    }

    /**
     * Get public key ids based on a given email.
     * 
     * @param context
     * @param email
     *            The email in question.
     * @return key ids
     */
    public long[] getPublicKeyIdsFromEmail(String email) {
        long ids[] = null;
        try {
            Uri contentUri = Uri.withAppendedPath(Constants.CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS,
                    email);
            Cursor c = activity.getContentResolver().query(contentUri,
                    new String[] { "master_key_id" }, null, null, null);
            if (c != null && c.getCount() > 0) {
                ids = new long[c.getCount()];
                while (c.moveToNext()) {
                    ids[c.getPosition()] = c.getLong(0);
                }
            }

            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            insufficientPermissions();
        }

        return ids;
    }

    /**
     * Find out if a given email has a secret key.
     * 
     * @param context
     * @param email
     *            The email in question.
     * @return true if there is a secret key for this email.
     */
    public boolean hasSecretKeyForEmail(String email) {
        try {
            Uri contentUri = Uri.withAppendedPath(Constants.CONTENT_URI_SECRET_KEY_RING_BY_EMAILS,
                    email);
            Cursor c = activity.getContentResolver().query(contentUri,
                    new String[] { "master_key_id" }, null, null, null);
            if (c != null && c.getCount() > 0) {
                c.close();
                return true;
            }
            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            insufficientPermissions();
        }
        return false;
    }

    /**
     * Find out if a given email has a public key.
     * 
     * @param context
     * @param email
     *            The email in question.
     * @return true if there is a public key for this email.
     */
    public boolean hasPublicKeyForEmail(String email) {
        try {
            Uri contentUri = Uri.withAppendedPath(Constants.CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS,
                    email);
            Cursor c = activity.getContentResolver().query(contentUri,
                    new String[] { "master_key_id" }, null, null, null);
            if (c != null && c.getCount() > 0) {
                c.close();
                return true;
            }
            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            insufficientPermissions();
        }
        return false;
    }

    /**
     * Get the user id based on the key id.
     * 
     * @param context
     * @param keyId
     * @return user id
     */
    public String getUserId(long keyId) {
        String userId = null;
        try {
            Uri contentUri = ContentUris.withAppendedId(
                    Constants.CONTENT_URI_SECRET_KEY_RING_BY_KEY_ID, keyId);
            Cursor c = activity.getContentResolver().query(contentUri, new String[] { "user_id" },
                    null, null, null);
            if (c != null && c.moveToFirst()) {
                userId = c.getString(0);
            }

            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            insufficientPermissions();
        }

        if (userId == null) {
            userId = "unknown";
        }
        return userId;
    }

    private void activityNotFound() {
        Toast.makeText(activity, "APG Activity not found! Is APG installed correctly?",
                Toast.LENGTH_LONG).show();
    }

    private void insufficientPermissions() {
        Toast.makeText(activity, "Permission to access APG Provider is missing!", Toast.LENGTH_LONG)
                .show();
    }
}
