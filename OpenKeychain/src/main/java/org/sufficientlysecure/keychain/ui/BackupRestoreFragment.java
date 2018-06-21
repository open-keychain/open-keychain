/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.daos.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.BackupKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;

public class BackupRestoreFragment extends CryptoOperationFragment<BackupKeyringParcel, ExportResult> {

    public static final int REQUEST_SAVE_FILE = 1;
    // masterKeyId & subKeyId for multi-key export
    private Iterator<Pair<Long, Long>> mIdsForRepeatAskPassphrase;

    private static final int REQUEST_REPEAT_PASSPHRASE = 0x00007002;
    private static final int REQUEST_CODE_INPUT = 0x00007003;
    private View backupPublicKeys;
    private Uri cachedBackupUri;
    private boolean shareNotSave;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.backup_restore_fragment, container, false);

        View backupAll = view.findViewById(R.id.backup_all);
        backupPublicKeys = view.findViewById(R.id.backup_public_keys);
        final View restore = view.findViewById(R.id.restore);

        backupAll.setOnClickListener(v -> backupAllKeys());
        backupPublicKeys.setOnClickListener(v -> exportContactKeys());
        restore.setOnClickListener(v -> restore());

        return view;
    }

    private void backupAllKeys() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        KeyRepository keyRepository = KeyRepository.create(requireContext());

        // This can probably be optimized quite a bit.
        // Typically there are only few secret keys though, so it doesn't really matter.

        new AsyncTask<Void, Void, ArrayList<Pair<Long, Long>>>() {
            @Override
            protected ArrayList<Pair<Long,Long>> doInBackground(Void... ignored) {
                KeyRepository keyRepository = KeyRepository.create(requireContext());
                ArrayList<Pair<Long, Long>> askPassphraseIds = new ArrayList<>();
                for (UnifiedKeyInfo keyInfo : keyRepository.getAllUnifiedKeyInfoWithSecret()) {
                    long masterKeyId = keyInfo.master_key_id();
                    SecretKeyType secretKeyType;
                    try {
                        secretKeyType = keyRepository.getSecretKeyType(keyInfo.master_key_id());
                    } catch (NotFoundException e) {
                        throw new IllegalStateException("Error: no secret key type for secret key!");
                    }
                    switch (secretKeyType) {
                        // all of these make no sense to ask
                        case PASSPHRASE_EMPTY:
                        case DIVERT_TO_CARD:
                        case UNAVAILABLE:
                            continue;
                        case GNU_DUMMY: {
                            Long subKeyId = getFirstSubKeyWithPassphrase(masterKeyId);
                            if(subKeyId != null) {
                                askPassphraseIds.add(new Pair<>(masterKeyId, subKeyId));
                            }
                            continue;
                        }
                        default: {
                            askPassphraseIds.add(new Pair<>(masterKeyId, masterKeyId));
                        }
                    }
                }
                return askPassphraseIds;
            }

            private Long getFirstSubKeyWithPassphrase(long masterKeyId) {
                for (SubKey subKey : keyRepository.getSubKeysByMasterKeyId(masterKeyId)) {
                    switch (subKey.has_secret()) {
                        case PASSPHRASE_EMPTY:
                        case DIVERT_TO_CARD:
                        case UNAVAILABLE:
                            return null;
                        case GNU_DUMMY:
                            continue;
                        default: {
                            return subKey.key_id();
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(ArrayList<Pair<Long, Long>> askPassphraseIds) {
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

                startBackup(true);
            }

        }.execute();
    }

    private void exportContactKeys() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        PopupMenu popupMenu = new PopupMenu(getContext(), backupPublicKeys);
        popupMenu.inflate(R.menu.export_public);
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_export_file:
                    shareNotSave = false;
                    exportContactKeysToFileOrShare();
                    break;
                case R.id.menu_export_share:
                    shareNotSave = true;
                    exportContactKeysToFileOrShare();
                    break;
            }
            return false;
        });
        popupMenu.show();
    }

    private void exportContactKeysToFileOrShare() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String filename =
                Constants.FILE_ENCRYPTED_BACKUP_PREFIX + date + Constants.FILE_EXTENSION_ENCRYPTED_BACKUP_PUBLIC;

        if (cachedBackupUri == null) {
            cachedBackupUri = TemporaryFileProvider.createFile(getContext(), filename,
                    Constants.MIME_TYPE_ENCRYPTED_ALTERNATE);

            cryptoOperation(CryptoInputParcel.createCryptoInputParcel());
            return;
        }

        if (shareNotSave) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(Constants.MIME_TYPE_KEYS);
            intent.putExtra(Intent.EXTRA_STREAM, cachedBackupUri);
            startActivity(intent);
        } else {
            saveFile(filename, false);
        }
    }

    private void saveFile(final String filename, boolean overwrite) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        // for kitkat and above, we have the document api
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            FileHelper.saveDocument(this, filename, Constants.MIME_TYPE_ENCRYPTED_ALTERNATE, REQUEST_SAVE_FILE);
            return;
        }

        if (!Constants.Path.APP_DIR.mkdirs()) {
            Notify.create(activity, R.string.snack_backup_error_saving, Style.ERROR).show();
            return;
        }

        File file = new File(Constants.Path.APP_DIR, filename);

        if (!overwrite && file.exists()) {
            Notify.create(activity, R.string.snack_backup_exists, Style.WARN, () -> saveFile(filename, true), R.string.snack_btn_overwrite).show();
            return;
        }

        try {
            FileHelper.copyUriData(activity, cachedBackupUri, Uri.fromFile(file));
            Notify.create(activity, R.string.snack_backup_saved_dir, Style.OK).show();
        } catch (IOException e) {
            Notify.create(activity, R.string.snack_backup_error_saving, Style.ERROR).show();
        }
    }

    private void startPassphraseActivity() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        Intent intent = new Intent(activity, PassphraseDialogActivity.class);
        Pair<Long, Long> keyPair = mIdsForRepeatAskPassphrase.next();
        long masterKeyId = keyPair.first;
        long subKeyId = keyPair.second;
        RequiredInputParcel requiredInput =
                RequiredInputParcel.createRequiredDecryptPassphrase(masterKeyId, subKeyId);
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
                if (mIdsForRepeatAskPassphrase.hasNext()) {
                    startPassphraseActivity();
                    return;
                }

                startBackup(true);

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

            case REQUEST_SAVE_FILE: {
                FragmentActivity activity = getActivity();
                if (resultCode != Activity.RESULT_OK || activity == null || data == null) {
                    return;
                }
                try {
                    Uri outputUri = data.getData();
                    FileHelper.copyUriData(activity, cachedBackupUri, outputUri);
                    Notify.create(activity, R.string.snack_backup_saved, Style.OK).show();
                } catch (IOException e) {
                    Notify.create(activity, R.string.snack_backup_error_saving, Style.ERROR).show();
                }
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Nullable
    @Override
    public BackupKeyringParcel createOperationInput() {
        return BackupKeyringParcel
                .create(null, false, false, true, cachedBackupUri);
    }

    @Override
    public void onCryptoOperationSuccess(ExportResult result) {
        exportContactKeysToFileOrShare();
    }

    @Override
    public void onCryptoOperationError(ExportResult result) {
        result.createNotify(getActivity()).show();
        cachedBackupUri = null;
    }

    private void startBackup(boolean exportSecret) {
        Intent intent = new Intent(getActivity(), BackupActivity.class);
        intent.putExtra(BackupActivity.EXTRA_SECRET, exportSecret);
        startActivity(intent);
    }

    private void restore() {
        FileHelper.openDocument(this, "*/*", false, REQUEST_CODE_INPUT);
    }

}
