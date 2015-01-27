/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.api.OpenKeychainIntents;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.ShareHelper;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.ui.util.Notify;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class EncryptTextActivity extends EncryptActivity implements EncryptActivityInterface {

    /* Intents */
    public static final String ACTION_ENCRYPT_TEXT = OpenKeychainIntents.ENCRYPT_TEXT;

    /* EXTRA keys for input */
    public static final String EXTRA_TEXT = OpenKeychainIntents.ENCRYPT_EXTRA_TEXT;

    // preselect ids, for internal use
    public static final String EXTRA_SIGNATURE_KEY_ID = Constants.EXTRA_PREFIX + "EXTRA_SIGNATURE_KEY_ID";
    public static final String EXTRA_ENCRYPTION_KEY_IDS = Constants.EXTRA_PREFIX + "EXTRA_SIGNATURE_KEY_IDS";

    // view
    private int mCurrentMode = MODE_ASYMMETRIC;

    // tabs
    private static final int MODE_ASYMMETRIC = 0;
    private static final int MODE_SYMMETRIC = 1;

    // model used by fragments
    private long mEncryptionKeyIds[] = null;
    private String mEncryptionUserIds[] = null;
    // TODO Constants.key.none? What's wrong with a null value?
    private long mSigningKeyId = Constants.key.none;
    private String mPassphrase = "";
    private boolean mShareAfterEncrypt = false;
    private ArrayList<Uri> mInputUris;
    private ArrayList<Uri> mOutputUris;
    private String mMessage = "";

    public boolean isModeSymmetric() {
        return MODE_SYMMETRIC == mCurrentMode;
    }

    @Override
    public boolean isUseArmor() {
        return true;
    }

    @Override
    public long getSignatureKey() {
        return mSigningKeyId;
    }

    @Override
    public long[] getEncryptionKeys() {
        return mEncryptionKeyIds;
    }

    @Override
    public String[] getEncryptionUsers() {
        return mEncryptionUserIds;
    }

    @Override
    public void setSignatureKey(long signatureKey) {
        mSigningKeyId = signatureKey;
        notifyUpdate();
    }

    @Override
    public void setEncryptionKeys(long[] encryptionKeys) {
        mEncryptionKeyIds = encryptionKeys;
        notifyUpdate();
    }

    @Override
    public void setEncryptionUsers(String[] encryptionUsers) {
        mEncryptionUserIds = encryptionUsers;
        notifyUpdate();
    }

    @Override
    public void setPassphrase(String passphrase) {
        mPassphrase = passphrase;
    }

    @Override
    public ArrayList<Uri> getInputUris() {
        if (mInputUris == null) mInputUris = new ArrayList<>();
        return mInputUris;
    }

    @Override
    public ArrayList<Uri> getOutputUris() {
        if (mOutputUris == null) mOutputUris = new ArrayList<>();
        return mOutputUris;
    }

    @Override
    public void setInputUris(ArrayList<Uri> uris) {
        mInputUris = uris;
        notifyUpdate();
    }

    @Override
    public void setOutputUris(ArrayList<Uri> uris) {
        mOutputUris = uris;
        notifyUpdate();
    }

    @Override
    public String getMessage() {
        return mMessage;
    }

    @Override
    public void setMessage(String message) {
        mMessage = message;
    }

    @Override
    public void notifyUpdate() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof UpdateListener) {
                ((UpdateListener) fragment).onNotifyUpdate();
            }
        }
    }

    @Override
    public void startEncrypt(boolean share) {
        mShareAfterEncrypt = share;
        startEncrypt();
    }

    @Override
    protected void onEncryptSuccess(Message message, SignEncryptResult pgpResult) {
        if (mShareAfterEncrypt) {
            // Share encrypted message/file
            startActivity(sendWithChooserExcludingEncrypt(message));
        } else {
            // Copy to clipboard
            copyToClipboard(message);
            pgpResult.createNotify(EncryptTextActivity.this).show();
            // Notify.showNotify(EncryptTextActivity.this,
            // R.string.encrypt_sign_clipboard_successful, Notify.Style.INFO);
        }
    }

    @Override
    protected Bundle createEncryptBundle() {
        // fill values for this action
        Bundle data = new Bundle();

        data.putInt(KeychainIntentService.TARGET, KeychainIntentService.IO_BYTES);
        data.putByteArray(KeychainIntentService.ENCRYPT_MESSAGE_BYTES, mMessage.getBytes());

        data.putInt(KeychainIntentService.ENCRYPT_COMPRESSION_ID,
                Preferences.getPreferences(this).getDefaultMessageCompression());

        // Always use armor for messages
        data.putBoolean(KeychainIntentService.ENCRYPT_USE_ASCII_ARMOR, true);

        if (isModeSymmetric()) {
            Log.d(Constants.TAG, "Symmetric encryption enabled!");
            String passphrase = mPassphrase;
            if (passphrase.length() == 0) {
                passphrase = null;
            }
            data.putString(KeychainIntentService.ENCRYPT_SYMMETRIC_PASSPHRASE, passphrase);
        } else {
            data.putLong(KeychainIntentService.ENCRYPT_SIGNATURE_MASTER_ID, mSigningKeyId);
            data.putLongArray(KeychainIntentService.ENCRYPT_ENCRYPTION_KEYS_IDS, mEncryptionKeyIds);
            data.putString(KeychainIntentService.ENCRYPT_SIGNATURE_KEY_PASSPHRASE, mSigningKeyPassphrase);
            data.putSerializable(KeychainIntentService.ENCRYPT_SIGNATURE_NFC_TIMESTAMP, mNfcTimestamp);
            data.putByteArray(KeychainIntentService.ENCRYPT_SIGNATURE_NFC_HASH, mNfcHash);
        }
        return data;
    }

    private void copyToClipboard(Message message) {
        ClipboardReflection.copyToClipboard(this, new String(message.getData().getByteArray(KeychainIntentService.RESULT_BYTES)));
    }

    /**
     * Create Intent Chooser but exclude OK's EncryptActivity.
     */
    private Intent sendWithChooserExcludingEncrypt(Message message) {
        Intent prototype = createSendIntent(message);
        String title = getString(R.string.title_share_message);

        // we don't want to encrypt the encrypted, no inception ;)
        String[] blacklist = new String[]{
                Constants.PACKAGE_NAME + ".ui.EncryptTextActivity",
                "org.thialfihar.android.apg.ui.EncryptActivity"
        };

        return new ShareHelper(this).createChooserExcluding(prototype, title, blacklist);
    }

    private Intent createSendIntent(Message message) {
        Intent sendIntent;
        sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, new String(message.getData().getByteArray(KeychainIntentService.RESULT_BYTES)));

        if (!isModeSymmetric() && mEncryptionUserIds != null) {
            Set<String> users = new HashSet<>();
            for (String user : mEncryptionUserIds) {
                String[] userId = KeyRing.splitUserId(user);
                if (userId[1] != null) {
                    users.add(userId[1]);
                }
            }
            // pass trough email addresses as extra for email applications
            sendIntent.putExtra(Intent.EXTRA_EMAIL, users.toArray(new String[users.size()]));
        }
        return sendIntent;
    }

    protected boolean inputIsValid() {
        if (mMessage == null) {
            Notify.showNotify(this, R.string.error_message, Notify.Style.ERROR);
            return false;
        }

        if (isModeSymmetric()) {
            // symmetric encryption checks

            if (mPassphrase == null) {
                Notify.showNotify(this, R.string.passphrases_do_not_match, Notify.Style.ERROR);
                return false;
            }
            if (mPassphrase.isEmpty()) {
                Notify.showNotify(this, R.string.passphrase_must_not_be_empty, Notify.Style.ERROR);
                return false;
            }

        } else {
            // asymmetric encryption checks

            boolean gotEncryptionKeys = (mEncryptionKeyIds != null
                    && mEncryptionKeyIds.length > 0);

            if (!gotEncryptionKeys && mSigningKeyId == 0) {
                Notify.showNotify(this, R.string.select_encryption_or_signature_key, Notify.Style.ERROR);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if called with an intent action, do not init drawer navigation
        if (ACTION_ENCRYPT_TEXT.equals(getIntent().getAction())) {
            // lock drawer
//            deactivateDrawerNavigation();
            // TODO: back button to key?
        } else {
//            activateDrawerNavigation(savedInstanceState);
        }

        // Handle intent actions
        handleActions(getIntent());
        updateModeFragment();
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.encrypt_text_activity);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.encrypt_text_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void updateModeFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.encrypt_pager_mode,
                        mCurrentMode == MODE_SYMMETRIC
                                ? new EncryptSymmetricFragment()
                                : new EncryptAsymmetricFragment()
                )
                .commitAllowingStateLoss();
        getSupportFragmentManager().executePendingTransactions();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.isCheckable()) {
            item.setChecked(!item.isChecked());
        }
        switch (item.getItemId()) {
            case R.id.check_use_symmetric:
                mCurrentMode = item.isChecked() ? MODE_SYMMETRIC : MODE_ASYMMETRIC;
                updateModeFragment();
                notifyUpdate();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * Handles all actions with this intent
     *
     * @param intent
     */
    private void handleActions(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        String type = intent.getType();

        if (extras == null) {
            extras = new Bundle();
        }

        /*
         * Android's Action
         */

        // When sending to OpenKeychain Encrypt via share menu
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Log.logDebugBundle(extras, "extras");

            // When sending to OpenKeychain Encrypt via share menu
            if ("text/plain".equals(type)) {
                String sharedText = extras.getString(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    // handle like normal text encryption, override action and extras to later
                    // executeServiceMethod ACTION_ENCRYPT_TEXT in main actions
                    extras.putString(EXTRA_TEXT, sharedText);
                    action = ACTION_ENCRYPT_TEXT;
                }

            }
        }

        String textData = extras.getString(EXTRA_TEXT);

        // preselect keys given by intent
        mSigningKeyId = extras.getLong(EXTRA_SIGNATURE_KEY_ID);
        mEncryptionKeyIds = extras.getLongArray(EXTRA_ENCRYPTION_KEY_IDS);

        /**
         * Main Actions
         */
        if (ACTION_ENCRYPT_TEXT.equals(action) && textData != null) {
            mMessage = textData;
        } else if (ACTION_ENCRYPT_TEXT.equals(action)) {
            Log.e(Constants.TAG, "Include the extra 'text' in your Intent!");
        }
    }

}
