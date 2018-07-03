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

package org.sufficientlysecure.keychain.keyimport.processing;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;

import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.operations.results.GetKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing.IteratorWithIOThrow;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.PositionAwareInputStream;
import timber.log.Timber;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ImportKeysListLoader
        extends AsyncTaskLoader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> {

    private Context mContext;
    private BytesLoaderState mState;

    private ArrayList<ImportKeysListEntry> mData = new ArrayList<>();
    private AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> mEntryListWrapper;

    public ImportKeysListLoader(Context context, BytesLoaderState loaderState) {
        super(context);
        mContext = context;
        mState = loaderState;
    }

    @Override
    public AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> loadInBackground() {
        // This has already been loaded! nvm any further, just return
        if (mEntryListWrapper != null) {
            return mEntryListWrapper;
        }

        {
            GetKeyResult getKeyResult = new GetKeyResult(GetKeyResult.RESULT_OK, null);
            mEntryListWrapper = new AsyncTaskResultWrapper<>(mData, getKeyResult);
        }

        if (mState == null) {
            Timber.e("Input data is null!");
            return mEntryListWrapper;
        }

        try {
            InputData inputData = getInputData(mState);
            generateListOfKeyrings(inputData);
        } catch (FileNotFoundException e) {
            OperationLog log = new OperationLog();
            log.add(LogType.MSG_GET_FILE_NOT_FOUND, 0);
            GetKeyResult getKeyResult = new GetKeyResult(GetKeyResult.RESULT_ERROR_FILE_NOT_FOUND, log);
            mEntryListWrapper = new AsyncTaskResultWrapper<>(mData, getKeyResult);
        }

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

    /**
     * Reads all PGPKeyRing objects from the bytes of an InputData object.
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
                mData.add(new ImportKeysListEntry(mContext, it.next()));
            }
        } catch (IOException e) {
            Timber.e(e, "IOException on parsing key file! Return NoValidKeysException!");
            if (mData.isEmpty()) {
                OperationResult.OperationLog log = new OperationResult.OperationLog();
                log.add(OperationResult.LogType.MSG_GET_NO_VALID_KEYS, 0);
                GetKeyResult getKeyResult = new GetKeyResult(GetKeyResult.RESULT_ERROR_NO_VALID_KEYS, log);
                mData.clear();
                mEntryListWrapper = new AsyncTaskResultWrapper<>(mData, getKeyResult);
            }
        }
    }

    @NonNull
    private InputData getInputData(BytesLoaderState ls)
            throws FileNotFoundException {

        InputData inputData;
        if (ls.mKeyBytes != null) {
            inputData = new InputData(new ByteArrayInputStream(ls.mKeyBytes), ls.mKeyBytes.length);
        } else if (ls.mDataUri != null) {
            InputStream inputStream = mContext.getContentResolver().openInputStream(ls.mDataUri);
            long length = FileHelper.getFileSize(mContext, ls.mDataUri, -1);

            inputData = new InputData(inputStream, length);
        } else {
            throw new AssertionError("Loader state must contain bytes or a data URI. This is a bug!");
        }

        return inputData;
    }

}
