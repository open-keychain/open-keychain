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


import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.service.KeychainService;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;

/** This is a base class for fragments which implement a cryptoOperation.
 *
 * Subclasses of this class can call the cryptoOperation method to execute an
 * operation in KeychainService which takes a parcelable of type T as its input
 * and returns an OperationResult of type S as a result.
 *
 * The input (of type T) is not given directly to the cryptoOperation method,
 * but must be provided by the overriden createOperationInput method to be
 * available upon request during execution of the cryptoOperation.
 *
 * After running cryptoOperation, one of the onCryptoOperation*() methods will
 * be called, depending on the success status of the operation. The subclass
 * must override at least onCryptoOperationSuccess to proceed after a
 * successful operation.
 *
 * @see KeychainService
 *
 */
public abstract class CryptoOperationFragment<T extends Parcelable, S extends OperationResult>
        extends Fragment implements CryptoOperationHelper.Callback<T, S> {

    final private CryptoOperationHelper<T, S> mOperationHelper;

    public CryptoOperationFragment() {
        mOperationHelper = new CryptoOperationHelper<>(1, this, this, R.string.progress_processing);
    }

    public CryptoOperationFragment(Integer initialProgressMsg) {
        mOperationHelper = new CryptoOperationHelper<>(1, this, this, initialProgressMsg);
    }

    public CryptoOperationFragment(int id, Integer initialProgressMsg) {
        mOperationHelper = new CryptoOperationHelper<>(id, this, this, initialProgressMsg);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mOperationHelper.handleActivityResult(requestCode, resultCode, data);
    }

    /** Starts execution of the cryptographic operation.
     *
     * During this process, the createOperationInput() method will be called,
     * this input will be handed to KeychainService, where it is executed in
     * the appropriate *Operation class. If the result is a PendingInputResult,
     * it is handled accordingly. Otherwise, it is returned in one of the
     * onCryptoOperation* callbacks.
     */
    protected void cryptoOperation() {
        mOperationHelper.cryptoOperation();
    }

    protected void cryptoOperation(CryptoInputParcel cryptoInput) {
        mOperationHelper.cryptoOperation(cryptoInput);
    }

    @Override @Nullable
    /** Creates input for the crypto operation. Called internally after the
     * crypto operation is started by a call to cryptoOperation(). Silently
     * cancels operation if this method returns null. */
    public abstract T createOperationInput();

    /** Returns false, indicating that we did not handle progress ourselves. */
    public boolean onCryptoSetProgress(String msg, int progress, int max) {
        return false;
    }

    public void setProgressMessageResource(int id) {
        mOperationHelper.setProgressMessageResource(id);
    }

    @Override
    /** Called when the cryptoOperation() was successful. No default behavior
     * here, this should always be implemented by a subclass! */
    abstract public void onCryptoOperationSuccess(S result);

    @Override
    abstract public void onCryptoOperationError(S result);

    @Override
    public void onCryptoOperationCancelled() {
    }

    public void hideKeyboard() {
        if (getActivity() == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        // check if no view has focus
        View v = getActivity().getCurrentFocus();
        if (v == null)
            return;

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

}
