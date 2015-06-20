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
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.operation.OperationHelper;


/**
 * All fragments executing crypto operations need to extend this class.
 */
public abstract class NewCryptoOperationFragment <T extends Parcelable, S extends OperationResult>
        extends Fragment {

    private OperationHelper<T, S> mOperationHelper;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("PHILIP", "requestCode "+ requestCode + " resultCode" + resultCode);
        if (!mOperationHelper.handleActivityResult(requestCode, resultCode, data)) {
                super.onActivityResult(requestCode, resultCode, data);
            }
    }

    protected void setOperationHelper(OperationHelper<T, S> helper) {
        mOperationHelper = helper;
    }

    protected void cryptoOperation() {
        mOperationHelper.cryptoOperation();
    }

    protected  void cryptoOperation(CryptoInputParcel inputParcel) {
        mOperationHelper.cryptoOperation(inputParcel);
    }
}
