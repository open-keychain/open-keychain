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

package org.sufficientlysecure.keychain.ui.widget;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.AttributeSet;

import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.adapter.KeyAdapter;

public class SignKeySpinner extends KeySpinner {
    public SignKeySpinner(Context context) {
        super(context);
    }

    public SignKeySpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SignKeySpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle data) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        Uri baseUri = KeychainContract.KeyRings.buildUnifiedKeyRingsUri();

        String[] projection = KeyAdapter.getProjectionWith(new String[] {
                KeychainContract.KeyRings.HAS_SIGN,
        });

        String where = KeychainContract.KeyRings.HAS_ANY_SECRET + " = 1";

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getContext(), baseUri, projection, where, null, null);
    }

    private int mIndexHasSign;

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);

        if (loader.getId() == LOADER_ID) {
            mIndexHasSign = data.getColumnIndex(KeychainContract.KeyRings.HAS_SIGN);
        }
    }

    @Override
    boolean isItemEnabled(Cursor cursor) {
        if (cursor.getInt(KeyAdapter.INDEX_IS_REVOKED) != 0) {
            return false;
        }
        if (cursor.getInt(KeyAdapter.INDEX_IS_EXPIRED) != 0) {
            return false;
        }
        if (cursor.getInt(KeyAdapter.INDEX_IS_SECURE) == 0) {
            return false;
        }
        if (cursor.isNull(mIndexHasSign)) {
            return false;
        }

        // valid key
        return true;
    }

}
