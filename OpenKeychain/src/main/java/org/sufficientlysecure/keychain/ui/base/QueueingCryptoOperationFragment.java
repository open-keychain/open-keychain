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

package org.sufficientlysecure.keychain.ui.base;


import android.os.Bundle;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.operations.results.OperationResult;


/** CryptoOperationFragment which calls crypto operation results only while
 * attached to Activity.
 *
 * This subclass of CryptoOperationFragment substitutes the onCryptoOperation*
 * methods for onQueuedOperation* ones, which are ensured to be called while
 * the fragment is attached to an Activity, possibly delaying the call until
 * the Fragment is re-attached.
 *
 * TODO merge this functionality into CryptoOperationFragment?
 *
 * @see CryptoOperationFragment
 */
public abstract class QueueingCryptoOperationFragment<T extends Parcelable, S extends OperationResult>
        extends CryptoOperationFragment<T,S> {

    public static final String ARG_QUEUED_RESULT = "queued_result";
    private S mQueuedResult;

    public QueueingCryptoOperationFragment() {
        super();
    }

    public QueueingCryptoOperationFragment(Integer initialProgressMsg) {
        super(initialProgressMsg);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (mQueuedResult != null) {
            try {
                if (mQueuedResult.success()) {
                    onQueuedOperationSuccess(mQueuedResult);
                } else {
                    onQueuedOperationError(mQueuedResult);
                }
            } finally {
                mQueuedResult = null;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ARG_QUEUED_RESULT, mQueuedResult);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mQueuedResult = savedInstanceState.getParcelable(ARG_QUEUED_RESULT);
        }
    }

    public abstract void onQueuedOperationSuccess(S result);

    public void onQueuedOperationError(S result) {
        hideKeyboard();
        result.createNotify(getActivity()).show();
    }

    @Override
    final public void onCryptoOperationSuccess(S result) {
        if (getActivity() == null) {
            mQueuedResult = result;
            return;
        }
        onQueuedOperationSuccess(result);
    }

    @Override
    final public void onCryptoOperationError(S result) {
        if (getActivity() == null) {
            mQueuedResult = result;
            return;
        }
        onQueuedOperationError(result);
    }
}
