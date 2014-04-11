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

package org.sufficientlysecure.keychain.helper;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.ui.dialog.DeleteKeyDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.FileDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

public class ExportHelper {
    protected FileDialogFragment mFileDialog;
    protected String mExportFilename;

    ActionBarActivity mActivity;

    public ExportHelper(ActionBarActivity activity) {
        super();
        this.mActivity = activity;
    }

    public void deleteKey(Uri dataUri, Handler deleteHandler) {
        try {
            long masterKeyId = new ProviderHelper(mActivity).extractOrGetMasterKeyId(dataUri);

            // Create a new Messenger for the communication back
            Messenger messenger = new Messenger(deleteHandler);
            DeleteKeyDialogFragment deleteKeyDialog = DeleteKeyDialogFragment.newInstance(messenger,
                    new long[]{ masterKeyId });
            deleteKeyDialog.show(mActivity.getSupportFragmentManager(), "deleteKeyDialog");
        } catch (ProviderHelper.NotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
        }
    }

    /**
     * Show dialog where to export keys
     */
    public void showExportKeysDialog(final long[] masterKeyIds, final String exportFilename,
                                     final boolean showSecretCheckbox) {
        mExportFilename = exportFilename;

        // Message is received after file is selected
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();
                    mExportFilename = data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME);

                    exportKeys(masterKeyIds, data.getBoolean(FileDialogFragment.MESSAGE_DATA_CHECKED));
                }
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
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

                mFileDialog = FileDialogFragment.newInstance(messenger, title, message,
                        exportFilename, checkMsg);

                mFileDialog.show(mActivity.getSupportFragmentManager(), "fileDialog");
            }
        });
    }

    /**
     * Export keys
     */
    public void exportKeys(long[] masterKeyIds, boolean exportSecret) {
        Log.d(Constants.TAG, "exportKeys started");

        // Send all information needed to service to export key in other thread
        final Intent intent = new Intent(mActivity, KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_EXPORT_KEYRING);

        // fill values for this action
        Bundle data = new Bundle();

        data.putString(KeychainIntentService.EXPORT_FILENAME, mExportFilename);
        data.putBoolean(KeychainIntentService.EXPORT_SECRET, exportSecret);

        if (masterKeyIds == null) {
            data.putBoolean(KeychainIntentService.EXPORT_ALL, true);
        } else {
            data.putLongArray(KeychainIntentService.EXPORT_KEY_RING_MASTER_KEY_ID, masterKeyIds);
        }

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after exporting is done in KeychainIntentService
        KeychainIntentServiceHandler exportHandler = new KeychainIntentServiceHandler(mActivity,
                mActivity.getString(R.string.progress_exporting),
                ProgressDialog.STYLE_HORIZONTAL,
                true,
                new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialogInterface) {
                                    mActivity.stopService(intent);
                                }
        }) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    int exported = returnData.getInt(KeychainIntentService.RESULT_EXPORT);
                    String toastMessage;
                    if (exported == 1) {
                        toastMessage = mActivity.getString(R.string.key_exported);
                    } else if (exported > 0) {
                        toastMessage = mActivity.getString(R.string.keys_exported, exported);
                    } else {
                        toastMessage = mActivity.getString(R.string.no_keys_exported);
                    }
                    Toast.makeText(mActivity, toastMessage, Toast.LENGTH_SHORT).show();

                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(exportHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        exportHandler.showProgressDialog(mActivity);

        // start service with intent
        mActivity.startService(intent);
    }

}
