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

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPUtil;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.PositionAwareInputStream;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;

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

    Context mContext;

    InputData mInputData;

    ArrayList<ImportKeysListEntry> mData = new ArrayList<ImportKeysListEntry>();
    AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> mEntryListWrapper;

    public ImportKeysListLoader(Context context, InputData inputData) {
        super(context);
        this.mContext = context;
        this.mInputData = inputData;
    }

    @Override
    public AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> loadInBackground() {

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

    /**
     * Reads all PGPKeyRing objects from input
     *
     * @param inputData
     * @return
     */
    private void generateListOfKeyrings(InputData inputData) {

        boolean isEmpty = true;
        int nonPgpCounter = 0;

        PositionAwareInputStream progressIn = new PositionAwareInputStream(
                inputData.getInputStream());

        // need to have access to the bufferedInput, so we can reuse it for the possible
        // PGPObject chunks after the first one, e.g. files with several consecutive ASCII
        // armor blocks
        BufferedInputStream bufferedInput = new BufferedInputStream(progressIn);
        try {

            // read all available blocks... (asc files can contain many blocks with BEGIN END)
            while (bufferedInput.available() > 0) {
                isEmpty = false;
                InputStream in = PGPUtil.getDecoderStream(bufferedInput);
                PGPObjectFactory objectFactory = new PGPObjectFactory(in);

                // go through all objects in this block
                Object obj;
                while ((obj = objectFactory.nextObject()) != null) {
                    Log.d(Constants.TAG, "Found class: " + obj.getClass());

                    if (obj instanceof PGPKeyRing) {
                        PGPKeyRing newKeyring = (PGPKeyRing) obj;
                        addToData(newKeyring);
                    } else {
                        Log.e(Constants.TAG, "Object not recognized as PGPKeyRing!");
                        nonPgpCounter++;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception on parsing key file!", e);
            mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(mData, e);
            nonPgpCounter = 0;
        }

        if (isEmpty) {
            Log.e(Constants.TAG, "File has no content!", new FileHasNoContent());
            mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>
                    (mData, new FileHasNoContent());
        }

        if (nonPgpCounter > 0) {
            mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>
                    (mData, new NonPgpPart(nonPgpCounter));
        }
    }

    private void addToData(PGPKeyRing keyring) {
        ImportKeysListEntry item = new ImportKeysListEntry(keyring);
        mData.add(item);
    }

}
