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
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.PositionAwareInputStream;

import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.List;

public class ImportKeysListLoader
        extends AsyncTaskLoader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> {

    public static class FileHasNoContent extends Exception {

    }

    public static class NonPgpPart extends Exception {
        private int mCount;

        public NonPgpPart(int count) {
            this.mCount = count;
        }

        public int getCount() {
            return mCount;
        }
    }

    final Context mContext;
    final InputData mInputData;

    ArrayList<ImportKeysListEntry> mData = new ArrayList<ImportKeysListEntry>();
    LongSparseArray<ParcelableKeyRing> mParcelableRings = new LongSparseArray<ParcelableKeyRing>();
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

        mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(mData, null);

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
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
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

        boolean isEmpty = true;

        PositionAwareInputStream progressIn = new PositionAwareInputStream(
                inputData.getInputStream());

        // need to have access to the bufferedInput, so we can reuse it for the possible
        // PGPObject chunks after the first one, e.g. files with several consecutive ASCII
        // armor blocks
        BufferedInputStream bufferedInput = new BufferedInputStream(progressIn);
        try {

            // read all available blocks... (asc files can contain many blocks with BEGIN END)
            while (bufferedInput.available() > 0) {
                // TODO: deal with non-keyring objects?
                List<UncachedKeyRing> rings = UncachedKeyRing.fromStream(bufferedInput);
                for(UncachedKeyRing key : rings) {
                    ImportKeysListEntry item = new ImportKeysListEntry(getContext(), key);
                    mData.add(item);
                    mParcelableRings.put(item.hashCode(), new ParcelableKeyRing(key.getEncoded()));
                    isEmpty = false;
                }
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception on parsing key file!", e);
            mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(mData, e);
        }

        if (isEmpty) {
            Log.e(Constants.TAG, "File has no content!", new FileHasNoContent());
            mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>
                    (mData, new FileHasNoContent());
        }
    }

}
