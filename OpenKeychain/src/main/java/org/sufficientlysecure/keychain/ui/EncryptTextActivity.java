/*
 * Copyright (C) 2012-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.intents.OpenKeychainIntents;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;

public class EncryptTextActivity extends BaseActivity implements
        EncryptModeAsymmetricFragment.IAsymmetric, EncryptModeSymmetricFragment.ISymmetric,
        EncryptTextFragment.IMode {

    /* Intents */
    public static final String ACTION_ENCRYPT_TEXT = OpenKeychainIntents.ENCRYPT_TEXT;

    /* EXTRA keys for input */
    public static final String EXTRA_TEXT = OpenKeychainIntents.ENCRYPT_EXTRA_TEXT;

    // preselect ids, for internal use
    public static final String EXTRA_SIGNATURE_KEY_ID = Constants.EXTRA_PREFIX + "EXTRA_SIGNATURE_KEY_ID";
    public static final String EXTRA_ENCRYPTION_KEY_IDS = Constants.EXTRA_PREFIX + "EXTRA_SIGNATURE_KEY_IDS";

    Fragment mModeFragment;
    EncryptTextFragment mEncryptFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenDialogClose(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        }, false);

        // Handle intent actions
        handleActions(getIntent(), savedInstanceState);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.encrypt_text_activity);
    }


    /**
     * Handles all actions with this intent
     *
     * @param intent
     */
    private void handleActions(Intent intent, Bundle savedInstanceState) {
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
                }

            }
        }

        String textData = extras.getString(EXTRA_TEXT);
        if (textData == null) {
            textData = "";
        }

        // preselect keys given by intent
        long mSigningKeyId = extras.getLong(EXTRA_SIGNATURE_KEY_ID);
        long[] mEncryptionKeyIds = extras.getLongArray(EXTRA_ENCRYPTION_KEY_IDS);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            mModeFragment = EncryptModeAsymmetricFragment.newInstance(mSigningKeyId, mEncryptionKeyIds);
            transaction.replace(R.id.encrypt_mode_container, mModeFragment, "mode");

            mEncryptFragment = EncryptTextFragment.newInstance(textData);
            transaction.replace(R.id.encrypt_text_container, mEncryptFragment, "text");

            transaction.commit();

            getSupportFragmentManager().executePendingTransactions();
        }
    }

    @Override
    public void onModeChanged(boolean symmetric) {
        // switch fragments
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.encrypt_mode_container,
                        symmetric
                                ? EncryptModeSymmetricFragment.newInstance()
                                : EncryptModeAsymmetricFragment.newInstance(0, null)
                )
                .commitAllowingStateLoss();
        getSupportFragmentManager().executePendingTransactions();
    }

    @Override
    public void onSignatureKeyIdChanged(long signatureKeyId) {
        mEncryptFragment.setSigningKeyId(signatureKeyId);
    }

    @Override
    public void onEncryptionKeyIdsChanged(long[] encryptionKeyIds) {
        mEncryptFragment.setEncryptionKeyIds(encryptionKeyIds);
    }

    @Override
    public void onEncryptionUserIdsChanged(String[] encryptionUserIds) {
        mEncryptFragment.setEncryptionUserIds(encryptionUserIds);
    }

    @Override
    public void onPassphraseChanged(Passphrase passphrase) {
        mEncryptFragment.setSymmetricPassphrase(passphrase);
    }
}
