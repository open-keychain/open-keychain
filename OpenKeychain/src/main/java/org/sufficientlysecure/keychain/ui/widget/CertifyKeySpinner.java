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
import android.widget.ImageView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;

public class CertifyKeySpinner extends KeySpinner {
    private long mHiddenMasterKeyId = Constants.key.none;

    public CertifyKeySpinner(Context context) {
        super(context);
    }

    public CertifyKeySpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CertifyKeySpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setHiddenMasterKeyId(long hiddenMasterKeyId) {
        this.mHiddenMasterKeyId = hiddenMasterKeyId;
        reload();
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
                KeychainContract.KeyRings.IS_REVOKED,
                KeychainContract.KeyRings.IS_EXPIRED,
                KeychainContract.KeyRings.HAS_CERTIFY,
                KeychainContract.KeyRings.HAS_ANY_SECRET,
                KeychainContract.KeyRings.HAS_DUPLICATE_USER_ID,
                KeychainContract.KeyRings.CREATION
        };

        String where = KeychainContract.KeyRings.HAS_ANY_SECRET + " = 1 AND "
                + KeychainDatabase.Tables.KEYS + "." + KeychainContract.KeyRings.MASTER_KEY_ID
                + " != " + mHiddenMasterKeyId;

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getContext(), baseUri, projection, where, null, null);
    }

    private int mIndexHasCertify, mIndexIsRevoked, mIndexIsExpired;

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);

        if (loader.getId() == LOADER_ID) {
            mIndexHasCertify = data.getColumnIndex(KeychainContract.KeyRings.HAS_CERTIFY);
            mIndexIsRevoked = data.getColumnIndex(KeychainContract.KeyRings.IS_REVOKED);
            mIndexIsExpired = data.getColumnIndex(KeychainContract.KeyRings.IS_EXPIRED);

            // If:
            // - no key has been pre-selected (e.g. by SageSlinger)
            // - there are actually keys (not just "none" entry)
            // Then:
            // - select key that is capable of certifying, but only if there is only one key capable of it
            if (mPreSelectedKeyId == Constants.key.none && mAdapter.getCount() > 1) {
                // preselect if key can certify
                int selection = -1;
                while (data.moveToNext()) {
                    if (!data.isNull(mIndexHasCertify)) {
                        if (selection == -1) {
                            selection = data.getPosition() + 1;
                        } else {
                            // if selection is already set, we have more than one certify key!
                            // get back to "none"!
                            selection = 0;
                        }
                    }
                }
                setSelection(selection);
            }
        }
    }


    @Override
    boolean setStatus(Context context, Cursor cursor, ImageView statusView) {
        if (cursor.getInt(mIndexIsRevoked) != 0) {
            KeyFormattingUtils.setStatusImage(getContext(), statusView, null, State.REVOKED, R.color.bg_gray);
            return false;
        }
        if (cursor.getInt(mIndexIsExpired) != 0) {
            KeyFormattingUtils.setStatusImage(getContext(), statusView, null, State.EXPIRED, R.color.bg_gray);
            return false;
        }
        // don't invalidate the "None" entry, which is also null!
        if (cursor.getPosition() != 0 && cursor.isNull(mIndexHasCertify)) {
            KeyFormattingUtils.setStatusImage(getContext(), statusView, null, State.UNAVAILABLE, R.color.bg_gray);
            return false;
        }

        // valid key
        return true;
    }

}
