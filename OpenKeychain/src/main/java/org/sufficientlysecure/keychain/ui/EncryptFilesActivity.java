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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.intents.OpenKeychainIntents;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.util.ArrayList;

public class EncryptFilesActivity extends BaseActivity implements
        EncryptModeAsymmetricFragment.IAsymmetric, EncryptModeSymmetricFragment.ISymmetric,
        EncryptFilesFragment.IMode {

    /* Intents */
    public static final String ACTION_ENCRYPT_DATA = OpenKeychainIntents.ENCRYPT_DATA;

    // enables ASCII Armor for file encryption when uri is given
    public static final String EXTRA_ASCII_ARMOR = OpenKeychainIntents.ENCRYPT_EXTRA_ASCII_ARMOR;

    // preselect ids, for internal use
    public static final String EXTRA_SIGNATURE_KEY_ID = Constants.EXTRA_PREFIX + "EXTRA_SIGNATURE_KEY_ID";
    public static final String EXTRA_ENCRYPTION_KEY_IDS = Constants.EXTRA_PREFIX + "EXTRA_ENCRYPTION_IDS";

    Fragment mModeFragment;
    EncryptFilesFragment mEncryptFragment;

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
        setContentView(R.layout.encrypt_files_activity);
    }

    /**
     * Handles all actions with this intent
     */
    private void handleActions(Intent intent, Bundle savedInstanceState) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        String type = intent.getType();
        ArrayList<Uri> uris = new ArrayList<>();

        if (extras == null) {
            extras = new Bundle();
        }

        if (intent.getData() != null) {
            uris.add(intent.getData());
        }

        /*
         * Android's Action
         */

        // When sending to OpenKeychain Encrypt via share menu
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            // Files via content provider, override uri and action
            uris.clear();
            uris.add(intent.<Uri>getParcelableExtra(Intent.EXTRA_STREAM));
        }

        if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }

        long mSigningKeyId = extras.getLong(EXTRA_SIGNATURE_KEY_ID);
        long[] mEncryptionKeyIds = extras.getLongArray(EXTRA_ENCRYPTION_KEY_IDS);
        boolean useArmor = extras.getBoolean(EXTRA_ASCII_ARMOR, false);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            mModeFragment = EncryptModeAsymmetricFragment.newInstance(mSigningKeyId, mEncryptionKeyIds);
            transaction.replace(R.id.encrypt_mode_container, mModeFragment, "mode");

            mEncryptFragment = EncryptFilesFragment.newInstance(uris, useArmor);
            transaction.replace(R.id.encrypt_file_container, mEncryptFragment, "files");

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
        mEncryptFragment.setPassphrase(passphrase);
    }

}
