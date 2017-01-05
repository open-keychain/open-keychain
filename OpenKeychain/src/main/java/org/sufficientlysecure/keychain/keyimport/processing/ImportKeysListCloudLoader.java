/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.keyimport.CloudSearch;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.Keyserver;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.GetKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.network.orbot.OrbotHelper;

import java.util.ArrayList;

public class ImportKeysListCloudLoader
        extends AsyncTaskLoader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> {

    private Context mContext;
    private CloudLoaderState mState;
    private ParcelableProxy mParcelableProxy;

    private ArrayList<ImportKeysListEntry> mEntryList = new ArrayList<>();
    private AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> mEntryListWrapper;

    /**
     * Searches a keyserver as specified in cloudPrefs, using an explicit proxy if passed
     *
     * @param loaderState     state containing the string to search on servers for (if it is a
     *                        fingerprint, will enforce fingerprint check) and the keyserver to
     *                        search on (whether to search on the keyserver, and whether to search
     *                        keybase.io)
     * @param parcelableProxy explicit proxy to use. If null, will retrieve from preferences
     */
    public ImportKeysListCloudLoader(Context context, CloudLoaderState loaderState,
                                     @Nullable ParcelableProxy parcelableProxy) {

        super(context);
        mContext = context;
        mState = loaderState;
        mParcelableProxy = parcelableProxy;
    }

    @Override
    public AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> loadInBackground() {
        mEntryListWrapper = new AsyncTaskResultWrapper<>(mEntryList, null);

        if (mState.mServerQuery == null) {
            Log.e(Constants.TAG, "mServerQuery is null!");
            return mEntryListWrapper;
        }

        if (mState.mServerQuery.startsWith("0x") && mState.mServerQuery.length() == 42) {
            Log.d(Constants.TAG, "This search is based on a unique fingerprint. Enforce a fingerprint check!");
            queryServer(true);
        } else {
            queryServer(false);
        }

        // Now we have all the data needed to build the parcelable key ring for this key
        for (ImportKeysListEntry e : mEntryList) {
            e.setParcelableKeyRing(new ParcelableKeyRing(e.getFingerprintHex(), e.getKeyIdHex(),
                    e.getKeybaseName(), e.getFbUsername()));
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
     * Query keyserver
     */
    private void queryServer(boolean enforceFingerprint) {
        ParcelableProxy proxy;

        if (mParcelableProxy == null) {
            // no explicit proxy specified, fetch from preferences
            if (OrbotHelper.isOrbotInRequiredState(mContext)) {
                proxy = Preferences.getPreferences(mContext).getParcelableProxy();
            } else {
                // user needs to enable/install orbot
                mEntryList.clear();
                GetKeyResult pendingResult = new GetKeyResult(null,
                        RequiredInputParcel.createOrbotRequiredOperation(),
                        new CryptoInputParcel());
                mEntryListWrapper = new AsyncTaskResultWrapper<>(mEntryList, pendingResult);
                return;
            }
        } else {
            proxy = mParcelableProxy;
        }

        try {
            ArrayList<ImportKeysListEntry> searchResult = CloudSearch.search(
                    mState.mServerQuery,
                    mState.mCloudPrefs,
                    proxy
            );

            mEntryList.clear();
            // add result to data
            if (enforceFingerprint) {
                String fingerprint = mState.mServerQuery.substring(2);
                Log.d(Constants.TAG, "fingerprint: " + fingerprint);
                // query must return only one result!
                if (searchResult.size() == 1) {
                    ImportKeysListEntry uniqueEntry = searchResult.get(0);
                    /*
                     * set fingerprint explicitly after query
                     * to enforce a check when the key is imported by KeychainService
                     */
                    uniqueEntry.setFingerprintHex(fingerprint);
                    mEntryList.add(uniqueEntry);
                }
            } else {
                mEntryList.addAll(searchResult);
            }
            GetKeyResult getKeyResult = new GetKeyResult(GetKeyResult.RESULT_OK, null);
            mEntryListWrapper = new AsyncTaskResultWrapper<>(mEntryList, getKeyResult);
        } catch (Keyserver.CloudSearchFailureException e) {
            // convert exception to result parcel
            int error = GetKeyResult.RESULT_ERROR;
            OperationResult.LogType logType = null;
            if (e instanceof Keyserver.QueryFailedException) {
                error = GetKeyResult.RESULT_ERROR_QUERY_FAILED;
                logType = OperationResult.LogType.MSG_GET_QUERY_FAILED;
            } else if (e instanceof Keyserver.TooManyResponsesException) {
                error = GetKeyResult.RESULT_ERROR_TOO_MANY_RESPONSES;
                logType = OperationResult.LogType.MSG_GET_TOO_MANY_RESPONSES;
            } else if (e instanceof Keyserver.QueryTooShortException) {
                error = GetKeyResult.RESULT_ERROR_QUERY_TOO_SHORT;
                logType = OperationResult.LogType.MSG_GET_QUERY_TOO_SHORT;
            } else if (e instanceof Keyserver.QueryTooShortOrTooManyResponsesException) {
                error = GetKeyResult.RESULT_ERROR_TOO_SHORT_OR_TOO_MANY_RESPONSES;
                logType = OperationResult.LogType.MSG_GET_QUERY_TOO_SHORT_OR_TOO_MANY_RESPONSES;
            } else if (e instanceof Keyserver.QueryNoEnabledSourceException) {
                error = GetKeyResult.RESULT_ERROR_NO_ENABLED_SOURCE;
                logType = OperationResult.LogType.MSG_GET_NO_ENABLED_SOURCE;
            }
            OperationResult.OperationLog log = new OperationResult.OperationLog();
            log.add(logType, 0);
            GetKeyResult getKeyResult = new GetKeyResult(error, log);
            mEntryListWrapper = new AsyncTaskResultWrapper<>(mEntryList, getKeyResult);
        }
    }
}
