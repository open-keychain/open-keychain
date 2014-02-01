/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

public class ImportKeysListServerLoader extends AsyncTaskLoader<List<ImportKeysListEntry>> {
    Context mContext;

    String mServerQuery;
    String mKeyServer;

    ArrayList<ImportKeysListEntry> data = new ArrayList<ImportKeysListEntry>();

    public ImportKeysListServerLoader(Context context, String serverQuery, String keyServer) {
        super(context);
        mContext = context;
        mServerQuery = serverQuery;
        mKeyServer = keyServer;
    }

    @Override
    public List<ImportKeysListEntry> loadInBackground() {
        if (mServerQuery == null) {
            Log.e(Constants.TAG, "mServerQuery is null!");
            return data;
        }

        queryServer(mServerQuery, mKeyServer);

        return data;
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
    public void deliverResult(List<ImportKeysListEntry> data) {
        super.deliverResult(data);
    }

    /**
     * Query key server
     */
    private void queryServer(String query, String keyServer) {
        HkpKeyServer server = new HkpKeyServer(keyServer);
        try {
            ArrayList<ImportKeysListEntry> searchResult = server.search(query);

            // add result to data
            data.addAll(searchResult);
        } catch (KeyServer.InsufficientQuery e) {
            Log.e(Constants.TAG, "InsufficientQuery", e);
        } catch (KeyServer.QueryException e) {
            Log.e(Constants.TAG, "QueryException", e);
        } catch (KeyServer.TooManyResponses e) {
            Log.e(Constants.TAG, "TooManyResponses", e);
        }
    }

}
