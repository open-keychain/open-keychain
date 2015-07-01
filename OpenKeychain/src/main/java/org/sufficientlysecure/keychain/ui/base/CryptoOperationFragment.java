/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.ui.base;

import android.content.Intent;
import android.os.Parcelable;
import android.support.v4.app.Fragment;

import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;

/**
 * All fragments executing crypto operations need to extend this class.
 */
public abstract class CryptoOperationFragment<T extends Parcelable, S extends OperationResult>
        extends Fragment implements CryptoOperationHelper.Callback<T, S> {

    private CryptoOperationHelper<T, S> mOperationHelper;

    public CryptoOperationFragment() {

        mOperationHelper = new CryptoOperationHelper<>(this, this);
    }

    public void setProgressMessageResource(int id) {
        mOperationHelper.setProgressMessageResource(id);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mOperationHelper.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public abstract T createOperationInput();

    protected void cryptoOperation() {
        cryptoOperation(new CryptoInputParcel());
    }

    protected void cryptoOperation(CryptoInputParcel cryptoInput) {
        cryptoOperation(cryptoInput, true);
    }

    protected void cryptoOperation(CryptoInputParcel cryptoInput, boolean showProgress) {
        mOperationHelper.cryptoOperation(cryptoInput, showProgress);
    }

    public boolean onCryptoSetProgress(String msg, int progress, int max) {
        return false;
    }

    @Override
    public void onCryptoOperationError(S result) {
        onCryptoOperationResult(result);
        result.createNotify(getActivity()).show();
    }

    @Override
    public void onCryptoOperationCancelled() {
    }

    @Override
    public void onCryptoOperationSuccess(S result) {
        onCryptoOperationResult(result);
    }

    /**
     *
     * To be overriden by subclasses, if desired. Provides a way to access the method by the
     * same name in CryptoOperationHelper, if super.onCryptoOperationSuccess and
     * super.onCryptoOperationError are called at the start of the respective functions in the
     * subclass overriding them
     * @param result
     */
    protected void onCryptoOperationResult(S result) {
    }
}
