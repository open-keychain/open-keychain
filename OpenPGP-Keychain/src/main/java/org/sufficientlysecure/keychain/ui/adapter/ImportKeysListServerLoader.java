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
import java.util.List;

public class ImportKeysListServerLoader extends AsyncTaskLoader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> {
    Context mContext;

    String mServerQuery;
    String mKeyServer;

    //ArrayList<ImportKeysListEntry> data = new ArrayList<ImportKeysListEntry>();
    ArrayList<ImportKeysListEntry> entryList = new ArrayList<ImportKeysListEntry>();
    AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> entryListWrapper;

    public ImportKeysListServerLoader(Context context, String serverQuery, String keyServer) {
        super(context);
        mContext = context;
        mServerQuery = serverQuery;
        mKeyServer = keyServer;
    }

    @Override
    public AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> loadInBackground() {
        if (mServerQuery == null) {
            Log.e(Constants.TAG, "mServerQuery is null!");
            entryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(entryList, null);
            return entryListWrapper;
        }

        queryServer(mServerQuery, mKeyServer);

        return entryListWrapper;
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

            // add result to data
            entryList.addAll(searchResult);
            entryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(entryList, null);
        } catch (KeyServer.InsufficientQuery e) {
            Log.e(Constants.TAG, "InsufficientQuery", e);
            entryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(entryList, e);
        } catch (KeyServer.QueryException e) {
            Log.e(Constants.TAG, "QueryException", e);
            entryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(entryList, e);
        } catch (KeyServer.TooManyResponses e) {
            Log.e(Constants.TAG, "TooManyResponses", e);
            entryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(entryList, e);
        }
    }

}
