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
import java.util.Date;
import java.util.Locale;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.BackupKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Numeric9x4PassphraseUtil;
import org.sufficientlysecure.keychain.util.Passphrase;

public class BackupCodeFragment extends CryptoOperationFragment<BackupKeyringParcel, ExportResult> {

    public static final String ARG_BACKUP_CODE = "backup_code";
    public static final String ARG_EXPORT_SECRET = "export_secret";
    public static final String ARG_EXECUTE_BACKUP_OPERATION = "execute_backup_operation";
    public static final String ARG_MASTER_KEY_IDS = "master_key_ids";

    public static final int REQUEST_SAVE = 1;

    private boolean mExportSecret;
    private long[] mMasterKeyIds;
    Passphrase mBackupCode;
    private boolean mExecuteBackupOperation;

    private Uri mCachedBackupUri;
    private boolean mShareNotSave;
    private View buttonSave;
    private View buttonShare;
    private View buttonExport;

    public static BackupCodeFragment newInstance(long[] masterKeyIds, boolean exportSecret,
                                                 boolean executeBackupOperation) {
        BackupCodeFragment frag = new BackupCodeFragment();

        Passphrase backupCode = Numeric9x4PassphraseUtil.generateNumeric9x4Passphrase();

        Bundle args = new Bundle();
        args.putParcelable(ARG_BACKUP_CODE, backupCode);
        args.putLongArray(ARG_MASTER_KEY_IDS, masterKeyIds);
        args.putBoolean(ARG_EXPORT_SECRET, exportSecret);
        args.putBoolean(ARG_EXECUTE_BACKUP_OPERATION, executeBackupOperation);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.backup_code_fragment, container, false);

        Bundle args = getArguments();
        mBackupCode = args.getParcelable(ARG_BACKUP_CODE);
        mMasterKeyIds = args.getLongArray(ARG_MASTER_KEY_IDS);
        mExportSecret = args.getBoolean(ARG_EXPORT_SECRET);
        mExecuteBackupOperation = args.getBoolean(ARG_EXECUTE_BACKUP_OPERATION, true);

        {
            TextView[] codeDisplayText = getTransferCodeTextViews(view, R.id.transfer_code_display);

            // set backup code in code TextViews
            char[] backupCode = mBackupCode.getCharArray();
            for (int i = 0; i < codeDisplayText.length; i++) {
                codeDisplayText[i].setText(backupCode, i * 5, 4);
            }

            // set background to null in TextViews - this will retain padding from EditText style!
            for (TextView textView : codeDisplayText) {
                // noinspection deprecation, setBackground(Drawable) is API level >=16
                textView.setBackgroundDrawable(null);
            }
        }

        buttonSave = view.findViewById(R.id.button_backup_save);
        buttonShare = view.findViewById(R.id.button_backup_share);
        buttonExport = view.findViewById(R.id.button_backup_export);

        if (mExecuteBackupOperation) {
            buttonSave.setOnClickListener(v -> {
                mShareNotSave = false;
                startBackup();
            });

            buttonShare.setOnClickListener(v -> {
                mShareNotSave = true;
                startBackup();
            });
        } else {
            view.findViewById(R.id.button_bar_backup).setVisibility(View.GONE);
            buttonExport.setVisibility(View.VISIBLE);
            buttonExport.setOnClickListener(v -> startBackup());
        }

        ((CheckBox) view.findViewById(R.id.check_backup_code_written)).setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    buttonSave.setEnabled(isChecked);
                    buttonShare.setEnabled(isChecked);
                    buttonExport.setEnabled(isChecked);
                });

        view.findViewById(R.id.button_faq).setOnClickListener(v -> showFaq());
        return view;
    }

    @NonNull
    private TextView[] getTransferCodeTextViews(View view, int transferCodeViewGroupId) {
        ViewGroup transferCodeGroup = view.findViewById(transferCodeViewGroupId);
        TextView[] codeDisplayText = new TextView[9];
        codeDisplayText[0] = transferCodeGroup.findViewById(R.id.transfer_code_block_1);
        codeDisplayText[1] = transferCodeGroup.findViewById(R.id.transfer_code_block_2);
        codeDisplayText[2] = transferCodeGroup.findViewById(R.id.transfer_code_block_3);
        codeDisplayText[3] = transferCodeGroup.findViewById(R.id.transfer_code_block_4);
        codeDisplayText[4] = transferCodeGroup.findViewById(R.id.transfer_code_block_5);
        codeDisplayText[5] = transferCodeGroup.findViewById(R.id.transfer_code_block_6);
        codeDisplayText[6] = transferCodeGroup.findViewById(R.id.transfer_code_block_7);
        codeDisplayText[7] = transferCodeGroup.findViewById(R.id.transfer_code_block_8);
        codeDisplayText[8] = transferCodeGroup.findViewById(R.id.transfer_code_block_9);
        return codeDisplayText;
    }

    private void showFaq() {
        HelpActivity.startHelpActivity(getActivity(), HelpActivity.TAB_FAQ);
    }

    private void startBackup() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String filename = Constants.FILE_ENCRYPTED_BACKUP_PREFIX + date
                + (mExportSecret ? Constants.FILE_EXTENSION_ENCRYPTED_BACKUP_SECRET
                : Constants.FILE_EXTENSION_ENCRYPTED_BACKUP_PUBLIC);

        // if we don't want to execute the actual operation outside of this activity, drop out here
        if (!mExecuteBackupOperation) {
            ((BackupActivity) getActivity()).handleBackupOperation(
                    CryptoInputParcel.createCryptoInputParcel(mBackupCode));
            return;
        }

        if (mCachedBackupUri == null) {
            mCachedBackupUri = TemporaryFileProvider.createFile(activity, filename,
                    Constants.MIME_TYPE_ENCRYPTED_ALTERNATE);

            cryptoOperation(CryptoInputParcel.createCryptoInputParcel(mBackupCode));
            return;
        }

        if (mShareNotSave) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(Constants.MIME_TYPE_ENCRYPTED_ALTERNATE);
            intent.putExtra(Intent.EXTRA_STREAM, mCachedBackupUri);
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
            FileHelper.saveDocument(this, filename, Constants.MIME_TYPE_ENCRYPTED_ALTERNATE, REQUEST_SAVE);
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
            FileHelper.copyUriData(activity, mCachedBackupUri, Uri.fromFile(file));
            Notify.create(activity, R.string.snack_backup_saved_dir, Style.OK).show();
        } catch (IOException e) {
            Notify.create(activity, R.string.snack_backup_error_saving, Style.ERROR).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_SAVE) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if (resultCode != FragmentActivity.RESULT_OK) {
            return;
        }

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        try {
            Uri outputUri = data.getData();
            FileHelper.copyUriData(activity, mCachedBackupUri, outputUri);
            Notify.create(activity, R.string.snack_backup_saved, Style.OK).show();
        } catch (IOException e) {
            Notify.create(activity, R.string.snack_backup_error_saving, Style.ERROR).show();
        }
    }

    @Nullable
    @Override
    public BackupKeyringParcel createOperationInput() {
        return BackupKeyringParcel
                .create(mMasterKeyIds, mExportSecret, true, true, mCachedBackupUri);
    }

    @Override
    public void onCryptoOperationSuccess(ExportResult result) {
        startBackup();
    }

    @Override
    public void onCryptoOperationError(ExportResult result) {
        result.createNotify(getActivity()).show();
        mCachedBackupUri = null;
    }

    @Override
    public void onCryptoOperationCancelled() {
        mCachedBackupUri = null;
    }

}
