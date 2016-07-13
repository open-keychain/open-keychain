/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing.SecretKeyRingType;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.ParcelableHashMap;
import org.sufficientlysecure.keychain.util.ParcelableLong;
import org.sufficientlysecure.keychain.util.Passphrase;

public class BackupRestoreFragment extends Fragment {
    private static final int REQUEST_REPEAT_PASSPHRASE = 0x00007002;
    private static final int REQUEST_CODE_INPUT = 0x00007003;

    private Iterator<Long> mIdsForRepeatAskPassphrase;
    private HashMap<Long, Passphrase> mPassphrases;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mPassphrases = new HashMap<>();
        View view = inflater.inflate(R.layout.backup_restore_fragment, container, false);

        View backupAll = view.findViewById(R.id.backup_all);
        View backupPublicKeys = view.findViewById(R.id.backup_public_keys);
        final View restore = view.findViewById(R.id.restore);

        backupAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportToFile(true);
            }
        });

        backupPublicKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportToFile(false);
            }
        });

        restore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restore();
            }
        });

        return view;
    }

    private void exportToFile(boolean includeSecretKeys) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (!includeSecretKeys) {
            startBackup(false, mPassphrases);
            return;
        }

        new AsyncTask<ContentResolver, Void, ArrayList<Long>>() {
            @Override
            protected ArrayList<Long> doInBackground(ContentResolver... resolver) {
                ArrayList<Long> askPassphraseIds = new ArrayList<>();
                Cursor cursor = resolver[0].query(
                        KeyRings.buildUnifiedKeyRingsUri(), new String[]{
                                KeyRings.MASTER_KEY_ID,
                                KeyRings.HAS_SECRET,
                        }, KeyRings.HAS_SECRET + " != 0", null, null);
                try {
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            // TODO: wip, grab value directly when new db col is added
                            long masterKeyId = cursor.getLong(0);
                            SecretKeyRingType secretKeyRingType = SecretKeyRingType.PASSPHRASE;
                            switch (secretKeyRingType) {
                                // all of these make no sense to ask
                                case PASSPHRASE_EMPTY: {
                                    mPassphrases.put(masterKeyId, new Passphrase());
                                    continue;
                                }
                                case PASSPHRASE: {
                                    askPassphraseIds.add(masterKeyId);
                                    continue;
                                }
                                default: {
                                    throw new AssertionError("Unhandled keyring type");
                                }
                            }
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return askPassphraseIds;
            }

            @Override
            protected void onPostExecute(ArrayList<Long> askPassphraseIds) {
                super.onPostExecute(askPassphraseIds);
                FragmentActivity activity = getActivity();
                if (activity == null) {
                    return;
                }

                mIdsForRepeatAskPassphrase = askPassphraseIds.iterator();

                if (mIdsForRepeatAskPassphrase.hasNext()) {
                    startPassphraseActivity();
                    return;
                }

                startBackup(true, mPassphrases);
            }

        }.execute(activity.getContentResolver());
    }

    private void startPassphraseActivity() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        Intent intent = new Intent(activity, PassphraseDialogActivity.class);
        long masterKeyId = mIdsForRepeatAskPassphrase.next();
        RequiredInputParcel requiredInput =
                RequiredInputParcel.createRequiredKeyringPassphrase(masterKeyId);
        requiredInput.mSkipCaching = true;
        intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
        startActivityForResult(intent, REQUEST_REPEAT_PASSPHRASE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_REPEAT_PASSPHRASE: {
                if (resultCode != Activity.RESULT_OK) {
                    return;
                }

                // save the returned passphrase
                RequiredInputParcel requiredInput =
                        data.getParcelableExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT);
                CryptoInputParcel cryptoResult =
                        data.getParcelableExtra(PassphraseDialogActivity.RESULT_CRYPTO_INPUT);
                mPassphrases.put(requiredInput.getMasterKeyId(), cryptoResult.getPassphrase());

                // continue asking for passphrases
                if (mIdsForRepeatAskPassphrase.hasNext()) {
                    startPassphraseActivity();
                    return;
                }

                startBackup(true, mPassphrases);

                break;
            }

            case REQUEST_CODE_INPUT: {
                if (resultCode != Activity.RESULT_OK || data == null) {
                    return;
                }

                Uri uri = data.getData();
                if (uri == null) {
                    Notify.create(getActivity(), R.string.no_file_selected, Notify.Style.ERROR).show();
                    return;
                }

                Intent intent = new Intent(getActivity(), DecryptActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(uri);
                startActivity(intent);
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void startBackup(boolean exportSecret, HashMap<Long, Passphrase> passphrases) {
        ParcelableHashMap<ParcelableLong, Passphrase> parcelablePassphrases =
                ParcelableHashMap.toParcelableHashMap(passphrases);
        Intent intent = new Intent(getActivity(), BackupActivity.class);
        intent.putExtra(BackupActivity.EXTRA_SECRET, exportSecret);
        intent.putExtra(BackupActivity.EXTRA_PASSPHRASES, parcelablePassphrases);
        startActivity(intent);
    }

    private void restore() {
        FileHelper.openDocument(this, null, "*/*", false, REQUEST_CODE_INPUT);
    }

}
