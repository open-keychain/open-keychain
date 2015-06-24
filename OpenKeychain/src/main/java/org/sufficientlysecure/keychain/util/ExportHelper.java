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

import android.support.v4.app.FragmentActivity;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.service.ExportKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;

import java.io.File;

public class ExportHelper
        implements CryptoOperationHelper.Callback <ExportKeyringParcel, ExportResult> {
    protected File mExportFile;

    FragmentActivity mActivity;

    private CryptoOperationHelper<ExportKeyringParcel, ExportResult> mExportOpHelper;
    private boolean mExportSecret;
    private long[] mMasterKeyIds;

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

    // TODO: If ExportHelper requires pending data (see CryptoOPerationHelper), activities using
    // TODO: this class should be able to call mExportOpHelper.handleActivity

    /**
     * Export keys
     */
    public void exportKeys(long[] masterKeyIds, boolean exportSecret) {
        Log.d(Constants.TAG, "exportKeys started");
        mExportSecret = exportSecret;
        mMasterKeyIds = masterKeyIds; // if masterKeyIds is null it means export all

        mExportOpHelper = new CryptoOperationHelper(mActivity, this, R.string.progress_exporting);
        mExportOpHelper.cryptoOperation();
    }

    @Override
    public ExportKeyringParcel createOperationInput() {
        return new ExportKeyringParcel(mMasterKeyIds, mExportSecret, mExportFile.getAbsolutePath());
    }

    @Override
    public void onCryptoOperationSuccess(ExportResult result) {
        result.createNotify(mActivity).show();
    }

    @Override
    public void onCryptoOperationCancelled() {

    }

    @Override
    public void onCryptoOperationError(ExportResult result) {
        result.createNotify(mActivity).show();
    }
}
