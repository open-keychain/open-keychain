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

package org.sufficientlysecure.keychain.ui.widget;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.AttributeSet;
import org.sufficientlysecure.keychain.provider.KeychainContract;

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

        // These are the rows that we will retrieve.
        String[] projection = new String[]{
                KeychainContract.KeyRings._ID,
                KeychainContract.KeyRings.MASTER_KEY_ID,
                KeychainContract.KeyRings.KEY_ID,
                KeychainContract.KeyRings.USER_ID,
                KeychainContract.KeyRings.IS_EXPIRED,
                KeychainContract.KeyRings.HAS_SIGN,
                KeychainContract.KeyRings.HAS_ANY_SECRET
        };

        String where = KeychainContract.KeyRings.HAS_ANY_SECRET + " = 1 AND " + KeychainContract.KeyRings.HAS_SIGN + " NOT NULL AND "
                + KeychainContract.KeyRings.IS_REVOKED + " = 0 AND " + KeychainContract.KeyRings.IS_EXPIRED + " = 0";

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getContext(), baseUri, projection, where, null, null);
    }
}
