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

package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.HkpKeyServer;
import org.sufficientlysecure.keychain.util.KeyServer;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;

public class ImportKeysListServerLoader
        extends AsyncTaskLoader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> {
    Context mContext;

    String mServerQuery;
    String mKeyServer;

    private ArrayList<ImportKeysListEntry> mEntryList = new ArrayList<ImportKeysListEntry>();
    private AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> mEntryListWrapper;

    public ImportKeysListServerLoader(Context context, String serverQuery, String keyServer) {
        super(context);
        mContext = context;
        mServerQuery = serverQuery;
        mKeyServer = keyServer;
    }

    @Override
    public AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> loadInBackground() {

        mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(mEntryList, null);

        if (mServerQuery == null) {
            Log.e(Constants.TAG, "mServerQuery is null!");
            return mEntryListWrapper;
        }

        queryServer(mServerQuery, mKeyServer);

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
    private void queryServer(String query, String keyServer) {
        HkpKeyServer server = new HkpKeyServer(keyServer);
        try {
            ArrayList<ImportKeysListEntry> searchResult = server.search(query);

            mEntryList.clear();
            // add result to data
            mEntryList.addAll(searchResult);
            mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(mEntryList, null);
        } catch (KeyServer.InsufficientQuery e) {
            Log.e(Constants.TAG, "InsufficientQuery", e);
            mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(mEntryList, e);
        } catch (KeyServer.QueryException e) {
            Log.e(Constants.TAG, "QueryException", e);
            mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(mEntryList, e);
        } catch (KeyServer.TooManyResponses e) {
            Log.e(Constants.TAG, "TooManyResponses", e);
            mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(mEntryList, e);
        }
    }

}
