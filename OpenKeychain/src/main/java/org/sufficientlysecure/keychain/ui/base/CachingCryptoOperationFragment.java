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


public abstract class CachingCryptoOperationFragment <T extends Parcelable, S extends OperationResult>
        extends QueueingCryptoOperationFragment<T, S> {

    public static final String ARG_CACHED_ACTIONS = "cached_actions";

    private T mCachedActionsParcel;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ARG_CACHED_ACTIONS, mCachedActionsParcel);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCachedActionsParcel = savedInstanceState.getParcelable(ARG_CACHED_ACTIONS);
        }
    }

    @Override
    public void onQueuedOperationSuccess(S result) {
        mCachedActionsParcel = null;
    }

    @Override
    public void onQueuedOperationError(S result) {
        super.onQueuedOperationError(result);
        mCachedActionsParcel = null;
    }

    @Override
    public abstract T createOperationInput();

    protected T getCachedActionsParcel() {
        return mCachedActionsParcel;
    }

    protected void cacheActionsParcel(T cachedActionsParcel) {
        mCachedActionsParcel = cachedActionsParcel;
    }

    public void onCryptoOperationCancelled() {
        mCachedActionsParcel = null;
    }

}
