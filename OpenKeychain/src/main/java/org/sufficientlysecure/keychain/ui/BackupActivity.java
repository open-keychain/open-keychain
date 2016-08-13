/*
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.ui;


import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;

import android.widget.Toast;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing.SecretKeyRingType;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.passphrasedialog.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class BackupActivity extends BaseActivity {

    public static final String EXTRA_MASTER_KEY_IDS = "master_key_ids";
    public static final String EXTRA_SECRET = "export_secret";

    private static final int REQUEST_REPEAT_ASK_PASSPHRASE = 1;

    private HashMap<Long, Passphrase> mPassphrases;
    private Iterator<Long> mIdsForRepeatAskPassphrase;
    private boolean mFinishedCollectingPassphrases;

    @Override
    protected void initLayout() {
        setContentView(R.layout.backup_activity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // noinspection ConstantConditions, we know this activity has an action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            final boolean exportSecret = intent.getBooleanExtra(EXTRA_SECRET, false);
            long[] masterKeyIds = intent.getLongArrayExtra(EXTRA_MASTER_KEY_IDS);
            mPassphrases = new HashMap<>();

            if (exportSecret) {
                // get passphrases for secret keyrings owned by user, then show backup code
                boolean importAllKeys = (masterKeyIds == null);
                if (importAllKeys) {
                    masterKeyIds = getOwnMasterKeyIds();
                }

                List<Long> keysNeedingPassphrases = getKeysRequiringPassphrases(masterKeyIds);

                mIdsForRepeatAskPassphrase = keysNeedingPassphrases.iterator();
                if (mIdsForRepeatAskPassphrase.hasNext()) {
                    askForPassphrase(mIdsForRepeatAskPassphrase.next());
                    return;
                }
            }

            // no need to get passphrases, just show backup code
            showBackupCodeFragment(masterKeyIds, exportSecret, mPassphrases);
        }
    }

    private long[] getOwnMasterKeyIds() {
        ArrayList<Long> idList = new ArrayList<>();
        Cursor cursor = getContentResolver().query(KeyRings.buildUnifiedKeyRingsUri(),
                new String[]{ KeyRings.MASTER_KEY_ID, KeyRings.HAS_SECRET },
                KeyRings.HAS_SECRET + " != 0", null, null);

        while (cursor != null && cursor.moveToNext()) {
            idList.add(cursor.getLong(0));
        }
        if (cursor != null) {
            cursor.close();
        }

        long[] idArray = new long[idList.size()];
        for (int i = 0; i < idList.size(); i++) {
            idArray[i] = idList.get(i);
        }
        return idArray;
    }

    private List<Long> getKeysRequiringPassphrases(long[] masterKeyIds) {
        ProviderHelper providerHelper = new ProviderHelper(this);

        ArrayList<Long> keysNeedingPassphrases = new ArrayList<>();

        for (long masterKeyId : masterKeyIds) {
            try {
                SecretKeyRingType secretKeyRingType =
                        providerHelper.getCachedPublicKeyRing(masterKeyId).getSecretKeyringType();
                switch (secretKeyRingType) {
                    case PASSPHRASE_EMPTY: {
                        mPassphrases.put(masterKeyId, new Passphrase());
                        continue;
                    }
                    case PASSPHRASE: {
                        keysNeedingPassphrases.add(masterKeyId);
                        continue;
                    }
                    case UNAVAILABLE: {
                        continue;
                    }
                    default: {
                        throw new AssertionError("Unhandled keyring type");
                    }
                }
            } catch (ProviderHelper.NotFoundException e) {
                Toast.makeText(getApplicationContext(), R.string.msg_backup_error_db, Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
        return keysNeedingPassphrases;
    }

    private void askForPassphrase(long masterKeyId) {
        Intent intent = new Intent(this, PassphraseDialogActivity.class);
        RequiredInputParcel requiredInput =
                RequiredInputParcel.createRequiredKeyringPassphrase(masterKeyId);
        requiredInput.mSkipCaching = true;
        intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
        startActivityForResult(intent, REQUEST_REPEAT_ASK_PASSPHRASE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                FragmentManager fragMan = getSupportFragmentManager();
                // pop from back stack, or if nothing was on there finish activity
                if (!fragMan.popBackStackImmediate()) {
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_REPEAT_ASK_PASSPHRASE: {
                if (resultCode != RESULT_OK) {
                    this.finish();
                    return;
                }

                // save the returned passphrase
                RequiredInputParcel requiredInput =
                        data.getParcelableExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT);
                CryptoInputParcel cryptoResult =
                        data.getParcelableExtra(PassphraseDialogActivity.RESULT_CRYPTO_INPUT);
                mPassphrases.put(requiredInput.getMasterKeyId(), cryptoResult.getPassphrase());

                if (mIdsForRepeatAskPassphrase.hasNext()) {
                    askForPassphrase(mIdsForRepeatAskPassphrase.next());
                } else {
                    // backup-code fragment is shown at onPostResume() to preserve state, refer to
                    // http://stackoverflow.com/questions/16265733/failure-delivering-result-onactivityforresult
                    mFinishedCollectingPassphrases = true;
                }
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (mFinishedCollectingPassphrases) {
            Intent intent = getIntent();
            final boolean exportSecret = intent.getBooleanExtra(EXTRA_SECRET, false);
            final long[] masterKeyIds = intent.getLongArrayExtra(EXTRA_MASTER_KEY_IDS);

            showBackupCodeFragment(masterKeyIds, exportSecret, mPassphrases);
        }

        mFinishedCollectingPassphrases = false;
    }

    /**
     * Overridden in RemoteBackupActivity
     */
    public void handleBackupOperation(Passphrase symmetricPassphrase, HashMap<Long, Passphrase> passphrases) {
        // only used for RemoteBackupActivity
    }

    /**
     * Overridden in RemoteBackupActivity
     */
    public void showBackupCodeFragment(long[] masterKeyIds, boolean exportSecret, HashMap<Long, Passphrase> passphrases) {
        Fragment frag = BackupCodeFragment.newInstance(masterKeyIds, exportSecret, passphrases, true);
        FragmentManager fragMan = getSupportFragmentManager();
        fragMan.beginTransaction()
                .setCustomAnimations(0, 0)
                .add(R.id.content_frame, frag)
                .commit();
    }
}
