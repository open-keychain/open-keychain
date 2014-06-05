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
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.KeybaseKeyserver;
import org.sufficientlysecure.keychain.keyimport.Keyserver;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;

public class ImportKeysListKeybaseLoader
        extends AsyncTaskLoader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> {
    Context mContext;

    String mKeybaseQuery;

    private ArrayList<ImportKeysListEntry> mEntryList = new ArrayList<ImportKeysListEntry>();
    private AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> mEntryListWrapper;

    public ImportKeysListKeybaseLoader(Context context, String keybaseQuery) {
        super(context);
        mContext = context;
        mKeybaseQuery = keybaseQuery;
    }

    @Override
    public AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> loadInBackground() {

        mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(mEntryList, null);

        if (mKeybaseQuery == null) {
            Log.e(Constants.TAG, "mKeybaseQery is null!");
            return mEntryListWrapper;
        }

        queryServer(mKeybaseQuery);

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
     * Query keybase
     */
    private void queryServer(String query) {

        KeybaseKeyserver server = new KeybaseKeyserver();
        try {
            ArrayList<ImportKeysListEntry> searchResult = server.search(query);

            mEntryList.clear();

            mEntryList.addAll(searchResult);
            mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(mEntryList, null);
        } catch (Keyserver.QueryFailedException e) {
            mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(mEntryList, e);
        } catch (Keyserver.QueryNeedsRepairException e) {
            mEntryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(mEntryList, e);
        }
    }
}
