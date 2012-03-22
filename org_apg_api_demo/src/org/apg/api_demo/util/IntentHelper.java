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

package org.apg.api_demo.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.Toast;

public class IntentHelper {

    /**
     * Select the signature key.
     * 
     * @param activity
     * @param pgpData
     * @return success or failure
     */
    public boolean selectSecretKey(Activity activity) {
        android.content.Intent intent = new android.content.Intent(
                Constants.Intent.SELECT_SECRET_KEY);
        intent.putExtra(Constants.EXTRA_INTENT_VERSION, Constants.INTENT_VERSION);
        try {
            activity.startActivityForResult(intent, Constants.SELECT_SECRET_KEY);
            return true;
        } catch (ActivityNotFoundException e) {
            activityNotFound(activity);
            return false;
        }
    }

    /**
     * Start the encrypt activity.
     * 
     * @param activity
     * @param data
     * @param pgpData
     * @return success or failure
     */
    public boolean encrypt(Activity activity, String data, long[] encryptionKeyIds,
            long signatureKeyId) {
        android.content.Intent intent = new android.content.Intent(
                Constants.Intent.ENCRYPT_AND_RETURN);
        intent.putExtra(Constants.EXTRA_INTENT_VERSION, Constants.INTENT_VERSION);
        intent.setType("text/plain");
        intent.putExtra(Constants.EXTRA_TEXT, data);
        intent.putExtra(Constants.EXTRA_ENCRYPTION_KEY_IDS, encryptionKeyIds);
        intent.putExtra(Constants.EXTRA_SIGNATURE_KEY_ID, signatureKeyId);
        try {
            activity.startActivityForResult(intent, Constants.ENCRYPT_MESSAGE);
            return true;
        } catch (ActivityNotFoundException e) {
            activityNotFound(activity);
            return false;
        }
    }

    /**
     * Start the decrypt activity.
     * 
     * @param activity
     * @param data
     * @param pgpData
     * @return success or failure
     */
    public boolean decrypt(Activity activity, String data) {
        android.content.Intent intent = new android.content.Intent(
                Constants.Intent.DECRYPT_AND_RETURN);
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
            activityNotFound(activity);
            return false;
        }
    }

    private void activityNotFound(Activity activity) {
        Toast.makeText(activity, "APG Activity not found! Is APG installed correctly?",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Handle the activity results that concern us.
     * 
     * @param activity
     * @param requestCode
     * @param resultCode
     * @param data
     * @return handled or not
     */
    public boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case Constants.SELECT_SECRET_KEY:
            // if (resultCode != Activity.RESULT_OK || data == null) {
            // break;
            // }
            // pgpData.setSignatureKeyId(data.getLongExtra(Apg.EXTRA_KEY_ID, 0));
            // pgpData.setSignatureUserId(data.getStringExtra(Apg.EXTRA_USER_ID));
            // ((MessageCompose) activity).updateEncryptLayout();
            break;

        case Constants.SELECT_PUBLIC_KEYS:
            // if (resultCode != Activity.RESULT_OK || data == null) {
            // pgpData.setEncryptionKeys(null);
            // ((MessageCompose) activity).onEncryptionKeySelectionDone();
            // break;
            // }
            // pgpData.setEncryptionKeys(data.getLongArrayExtra(Apg.EXTRA_SELECTION));
            // ((MessageCompose) activity).onEncryptionKeySelectionDone();
            break;

        case Constants.ENCRYPT_MESSAGE:
            // if (resultCode != Activity.RESULT_OK || data == null) {
            // pgpData.setEncryptionKeys(null);
            // ((MessageCompose) activity).onEncryptDone();
            // break;
            // }
            // pgpData.setEncryptedData(data.getStringExtra(Apg.EXTRA_ENCRYPTED_MESSAGE));
            // // this was a stupid bug in an earlier version, just gonna leave this in for an APG
            // // version or two
            // if (pgpData.getEncryptedData() == null) {
            // pgpData.setEncryptedData(data.getStringExtra(Apg.EXTRA_DECRYPTED_MESSAGE));
            // }
            // if (pgpData.getEncryptedData() != null) {
            // ((MessageCompose) activity).onEncryptDone();
            // }
            break;

        case Constants.DECRYPT_MESSAGE:
            if (resultCode != Activity.RESULT_OK || data == null) {
                break;
            }

            // pgpData.setSignatureUserId(data.getStringExtra(Apg.EXTRA_SIGNATURE_USER_ID));
            // pgpData.setSignatureKeyId(data.getLongExtra(Apg.EXTRA_SIGNATURE_KEY_ID, 0));
            // pgpData.setSignatureSuccess(data.getBooleanExtra(Apg.EXTRA_SIGNATURE_SUCCESS,
            // false));
            // pgpData.setSignatureUnknown(data.getBooleanExtra(Apg.EXTRA_SIGNATURE_UNKNOWN,
            // false));
            //
            // pgpData.setDecryptedData(data.getStringExtra(Apg.EXTRA_DECRYPTED_MESSAGE));
            // ((MessageView) activity).onDecryptDone(pgpData);

            break;

        default:
            return false;
        }

        return true;
    }

    //
    // /**
    // * Select encryption keys.
    // *
    // * @param activity
    // * @param emails
    // * The emails that should be used for preselection.
    // * @param pgpData
    // * @return success or failure
    // */
    // public boolean selectEncryptionKeys(Activity activity, String emails) {
    // android.content.Intent intent = new android.content.Intent(Apg.Intent.SELECT_PUBLIC_KEYS);
    // intent.putExtra(EXTRA_INTENT_VERSION, INTENT_VERSION);
    // long[] initialKeyIds = null;
    // if (!pgpData.hasEncryptionKeys()) {
    // List<Long> keyIds = new ArrayList<Long>();
    // if (pgpData.hasSignatureKey()) {
    // keyIds.add(pgpData.getSignatureKeyId());
    // }
    //
    // try {
    // Uri contentUri = Uri.withAppendedPath(Apg.CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS,
    // emails);
    // Cursor c = activity.getContentResolver().query(contentUri,
    // new String[] { "master_key_id" }, null, null, null);
    // if (c != null) {
    // while (c.moveToNext()) {
    // keyIds.add(c.getLong(0));
    // }
    // }
    //
    // if (c != null) {
    // c.close();
    // }
    // } catch (SecurityException e) {
    // Toast.makeText(activity,
    // activity.getResources().getString(R.string.insufficient_apg_permissions),
    // Toast.LENGTH_LONG).show();
    // }
    // if (!keyIds.isEmpty()) {
    // initialKeyIds = new long[keyIds.size()];
    // for (int i = 0, size = keyIds.size(); i < size; ++i) {
    // initialKeyIds[i] = keyIds.get(i);
    // }
    // }
    // } else {
    // initialKeyIds = pgpData.getEncryptionKeys();
    // }
    // intent.putExtra(Apg.EXTRA_SELECTION, initialKeyIds);
    // try {
    // activity.startActivityForResult(intent, Apg.SELECT_PUBLIC_KEYS);
    // return true;
    // } catch (ActivityNotFoundException e) {
    // Toast.makeText(activity, R.string.error_activity_not_found, Toast.LENGTH_SHORT).show();
    // return false;
    // }
    // }

}
