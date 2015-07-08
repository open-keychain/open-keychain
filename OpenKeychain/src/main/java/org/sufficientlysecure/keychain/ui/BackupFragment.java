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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.util.ExportHelper;

import java.util.ArrayList;

public class BackupFragment extends Fragment {

    // This ids for multiple key export.
    private ArrayList<Long> mIdsForRepeatAskPassphrase;
    private ArrayList<Long> mIdsForExport;
    // This index for remembering the number of master key.
    private int mIndex;

    static final int REQUEST_REPEAT_PASSPHRASE = 1;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.backup_fragment, container, false);

        View mBackupAll = view.findViewById(R.id.backup_all);
        View mBackupPublicKeys = view.findViewById(R.id.backup_public_keys);

        mBackupAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportToFile(true);
            }
        });

        mBackupPublicKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportToFile(false);
            }
        });

        return view;
    }

    private void exportToFile(boolean includeSecretKeys) {
        if (includeSecretKeys) {
            mIdsForRepeatAskPassphrase = new ArrayList<>();

            Cursor cursor = getActivity().getContentResolver().query(
                    KeychainContract.KeyRingData.buildSecretKeyRingUri(), null, null, null, null);
            try {
                if (cursor != null) {
                    int keyIdColumn = cursor.getColumnIndex(KeychainContract.KeyRingData.MASTER_KEY_ID);
                    while (cursor.moveToNext()) {
                        long keyId = cursor.getLong(keyIdColumn);
                        try {
                            if (PassphraseCacheService.getCachedPassphrase(
                                    getActivity(), keyId, keyId) == null) {
                                mIdsForRepeatAskPassphrase.add(keyId);
                            }
                        } catch (PassphraseCacheService.KeyNotFoundException e) {
                            // This happens when the master key is stripped
                            // and ignore this key.
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            mIndex = 0;
            if (mIdsForRepeatAskPassphrase.size() != 0) {
                startPassphraseActivity();
                return;
            }

            ExportHelper exportHelper = new ExportHelper(getActivity());
            exportHelper.showExportKeysDialog(null, Constants.Path.APP_DIR_FILE, true);
        } else {
            ExportHelper exportHelper = new ExportHelper(getActivity());
            exportHelper.showExportKeysDialog(null, Constants.Path.APP_DIR_FILE, false);
        }
    }

    private void startPassphraseActivity() {
        Intent intent = new Intent(getActivity(), PassphraseDialogActivity.class);
        long masterKeyId = mIdsForRepeatAskPassphrase.get(mIndex++);
        intent.putExtra(PassphraseDialogActivity.EXTRA_SUBKEY_ID, masterKeyId);
        startActivityForResult(intent, REQUEST_REPEAT_PASSPHRASE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_REPEAT_PASSPHRASE) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }
            if (mIndex < mIdsForRepeatAskPassphrase.size()) {
                startPassphraseActivity();
                return;
            }

            ExportHelper exportHelper = new ExportHelper(getActivity());
            exportHelper.showExportKeysDialog(null, Constants.Path.APP_DIR_FILE, true);
        }
    }
}
