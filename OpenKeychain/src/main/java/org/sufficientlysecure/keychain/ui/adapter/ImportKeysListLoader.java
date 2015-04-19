/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.util.LongSparseArray;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.GetKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing.IteratorWithIOThrow;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.PositionAwareInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ImportKeysListLoader
        extends AsyncTaskLoader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> {

    final Context mContext;
    final InputData mInputData;

    ArrayList<ImportKeysListEntry> mData = new ArrayList<>();
    LongSparseArray<ParcelableKeyRing> mParcelableRings = new LongSparseArray<>();
    AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> mEntryListWrapper;

    public ImportKeysListLoader(Context context, InputData inputData) {
        super(context);
        this.mContext = context;
        this.mInputData = inputData;
    }

    @Override
    public AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> loadInBackground() {
        // This has already been loaded! nvm any further, just return
        if (mEntryListWrapper != null) {
            return mEntryListWrapper;
        }

        GetKeyResult getKeyResult = new GetKeyResult(GetKeyResult.RESULT_OK, null);
        mEntryListWrapper = new AsyncTaskResultWrapper<>(mData, getKeyResult);

        if (mInputData == null) {
            Log.e(Constants.TAG, "Input data is null!");
            return mEntryListWrapper;
        }

        generateListOfKeyrings(mInputData);

        return mEntryListWrapper;
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();
    }

    @Override
    protected void onStartLoading() {
        super.forceLoad();
    }

    @Override
    protected void onStopLoading() {
        super.cancelLoad();
    }

    @Override
    public void deliverResult(AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> data) {
        super.deliverResult(data);
    }

    public LongSparseArray<ParcelableKeyRing> getParcelableRings() {
        return mParcelableRings;
    }

    /**
     * Reads all PGPKeyRing objects from input
     *
     * @param inputData
     * @return
     */
    private void generateListOfKeyrings(InputData inputData) {
        PositionAwareInputStream progressIn = new PositionAwareInputStream(
                inputData.getInputStream());

        // need to have access to the bufferedInput, so we can reuse it for the possible
        // PGPObject chunks after the first one, e.g. files with several consecutive ASCII
        // armor blocks
        BufferedInputStream bufferedInput = new BufferedInputStream(progressIn);
        try {
            // parse all keyrings
            IteratorWithIOThrow<UncachedKeyRing> it = UncachedKeyRing.fromStream(bufferedInput);
            while (it.hasNext()) {
                UncachedKeyRing ring = it.next();
                ImportKeysListEntry item = new ImportKeysListEntry(getContext(), ring);
                mData.add(item);
                mParcelableRings.put(item.hashCode(), new ParcelableKeyRing(ring.getEncoded()));
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, "IOException on parsing key file! Return NoValidKeysException!", e);
            OperationResult.OperationLog log = new OperationResult.OperationLog();
            log.add(OperationResult.LogType.MSG_GET_NO_VALID_KEYS, 0);
            GetKeyResult getKeyResult = new GetKeyResult(GetKeyResult.RESULT_ERROR_NO_VALID_KEYS, log);
            mEntryListWrapper = new AsyncTaskResultWrapper<>
                    (mData, getKeyResult);
        }
    }

}
