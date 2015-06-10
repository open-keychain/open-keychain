/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.util;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.FragmentActivity;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.service.KeychainService;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;

import java.io.File;

public class ExportHelper {
    protected File mExportFile;

    FragmentActivity mActivity;

    public ExportHelper(FragmentActivity activity) {
        super();
        this.mActivity = activity;
    }

    /**
     * Show dialog where to export keys
     */
    public void showExportKeysDialog(final long[] masterKeyIds, final File exportFile,
                                     final boolean showSecretCheckbox) {
        mExportFile = exportFile;

        String title = null;
        if (masterKeyIds == null) {
            // export all keys
            title = mActivity.getString(R.string.title_export_keys);
        } else {
            // export only key specified at data uri
            title = mActivity.getString(R.string.title_export_key);
        }

        String message = mActivity.getString(R.string.specify_file_to_export_to);
        String checkMsg = showSecretCheckbox ?
                mActivity.getString(R.string.also_export_secret_keys) : null;

        FileHelper.saveFile(new FileHelper.FileDialogCallback() {
            @Override
            public void onFileSelected(File file, boolean checked) {
                mExportFile = file;
                exportKeys(masterKeyIds, checked);
            }
        }, mActivity.getSupportFragmentManager() ,title, message, exportFile, checkMsg);
    }

    /**
     * Export keys
     */
    public void exportKeys(long[] masterKeyIds, boolean exportSecret) {
        Log.d(Constants.TAG, "exportKeys started");

        // Send all information needed to service to export key in other thread
        final Intent intent = new Intent(mActivity, KeychainService.class);

        intent.setAction(KeychainService.ACTION_EXPORT_KEYRING);

        // fill values for this action
        Bundle data = new Bundle();

        data.putString(KeychainService.EXPORT_FILENAME, mExportFile.getAbsolutePath());
        data.putBoolean(KeychainService.EXPORT_SECRET, exportSecret);

        if (masterKeyIds == null) {
            data.putBoolean(KeychainService.EXPORT_ALL, true);
        } else {
            data.putLongArray(KeychainService.EXPORT_KEY_RING_MASTER_KEY_ID, masterKeyIds);
        }

        intent.putExtra(KeychainService.EXTRA_DATA, data);

        // Message is received after exporting is done in KeychainService
        ServiceProgressHandler exportHandler = new ServiceProgressHandler(mActivity) {
            @Override
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    // get returned data bundle
                    Bundle data = message.getData();

                    ExportResult result = data.getParcelable(ExportResult.EXTRA_RESULT);
                    result.createNotify(mActivity).show();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(exportHandler);
        intent.putExtra(KeychainService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        exportHandler.showProgressDialog(
                mActivity.getString(R.string.progress_exporting),
                ProgressDialog.STYLE_HORIZONTAL, false
        );

        // start service with intent
        mActivity.startService(intent);
    }

}
