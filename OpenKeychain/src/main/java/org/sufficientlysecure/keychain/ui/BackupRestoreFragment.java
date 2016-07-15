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
    private static final int REQUEST_CODE_INPUT = 0x00007003;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

        startBackup(includeSecretKeys);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
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

    private void startBackup(boolean exportSecret) {
        Intent intent = new Intent(getActivity(), BackupActivity.class);
        intent.putExtra(BackupActivity.EXTRA_SECRET, exportSecret);
        startActivity(intent);
    }

    private void restore() {
        FileHelper.openDocument(this, null, "*/*", false, REQUEST_CODE_INPUT);
    }

}
