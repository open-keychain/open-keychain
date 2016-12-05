/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.keyimport.processing;

import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;

public class ImportKeysOperationCallback implements
        CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> {

    private ImportKeysResultListener mResultListener;
    private ImportKeyringParcel mKeyringParcel;

    public ImportKeysOperationCallback(
            ImportKeysResultListener resultListener,
            ImportKeyringParcel inputParcel
    ) {
        this.mResultListener = resultListener;
        this.mKeyringParcel = inputParcel;
    }

    @Override
    public ImportKeyringParcel createOperationInput() {
        return mKeyringParcel;
    }

    @Override
    public void onCryptoOperationSuccess(ImportKeyResult result) {
        mResultListener.handleResult(result);
    }

    @Override
    public void onCryptoOperationCancelled() {
        // do nothing
    }

    @Override
    public void onCryptoOperationError(ImportKeyResult result) {
        mResultListener.handleResult(result);
    }

    @Override
    public boolean onCryptoSetProgress(String msg, int progress, int max) {
        return false;
    }

}
