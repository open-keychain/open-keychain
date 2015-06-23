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
        extends Fragment {

    private CryptoOperationHelper<T, S> mOperationHelper;

    public CryptoOperationFragment() {
        // this is implemented here instead of by the fragment so that the corresponding methods in
        // CryptoOperationFragment may continue using the "protected" modifier.
        CryptoOperationHelper.Callback callback = new CryptoOperationHelper.Callback<T, S>() {

            @Override
            public T createOperationInput() {
                return CryptoOperationFragment.this.createOperationInput();
            }

            @Override
            public void onCryptoOperationSuccess(S result) {
                CryptoOperationFragment.this.onCryptoOperationSuccess(result);
            }

            @Override
            public void onCryptoOperationCancelled() {
                CryptoOperationFragment.this.onCryptoOperationCancelled();
            }

            @Override
            public void onCryptoOperationError(S result) {
                CryptoOperationFragment.this.onCryptoOperationError(result);
            }
        };

        mOperationHelper = new CryptoOperationHelper<>(this, callback);
    }

    public void setProgressMessageResource(int id) {
        mOperationHelper.setProgressMessageResource(id);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mOperationHelper.handleActivityResult(requestCode, resultCode, data);
    }

    protected abstract T createOperationInput();

    protected void cryptoOperation() {
        cryptoOperation(new CryptoInputParcel());
    }

    protected void cryptoOperation(CryptoInputParcel cryptoInput) {
        mOperationHelper.cryptoOperation(cryptoInput);
    }

    protected void onCryptoOperationError(S result) {
        result.createNotify(getActivity()).show();
    }

    protected void onCryptoOperationCancelled() {
    }

    abstract protected void onCryptoOperationSuccess(S result);

    protected void onCryptoOperationResult(S result) {
        mOperationHelper.onCryptoOperationResult(result);
    }
}
