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
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.OtherHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;

import java.util.Date;

public class SubkeysAdapter extends CursorAdapter {
    private LayoutInflater mInflater;

    private boolean hasAnySecret;

    private ColorStateList mDefaultTextColor;

    public static final String[] SUBKEYS_PROJECTION = new String[]{
            Keys._ID,
            Keys.KEY_ID,
            Keys.RANK,
            Keys.ALGORITHM,
            Keys.KEY_SIZE,
            Keys.HAS_SECRET,
            Keys.CAN_CERTIFY,
            Keys.CAN_ENCRYPT,
            Keys.CAN_SIGN,
            Keys.IS_REVOKED,
            Keys.CREATION,
            Keys.EXPIRY,
            Keys.FINGERPRINT
    };
    private static final int INDEX_ID = 0;
    private static final int INDEX_KEY_ID = 1;
    private static final int INDEX_RANK = 2;
    private static final int INDEX_ALGORITHM = 3;
    private static final int INDEX_KEY_SIZE = 4;
    private static final int INDEX_HAS_SECRET = 5;
    private static final int INDEX_CAN_CERTIFY = 6;
    private static final int INDEX_CAN_ENCRYPT = 7;
    private static final int INDEX_CAN_SIGN = 8;
    private static final int INDEX_IS_REVOKED = 9;
    private static final int INDEX_CREATION = 10;
    private static final int INDEX_EXPIRY = 11;
    private static final int INDEX_FINGERPRINT = 12;

    public SubkeysAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);

        mInflater = LayoutInflater.from(context);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        hasAnySecret = false;
        if (newCursor != null && newCursor.moveToFirst()) {
            do {
                if (newCursor.getInt(INDEX_HAS_SECRET) != 0) {
                    hasAnySecret = true;
                    break;
                }
            } while (newCursor.moveToNext());
        }

        return super.swapCursor(newCursor);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView keyId = (TextView) view.findViewById(R.id.keyId);
        TextView keyDetails = (TextView) view.findViewById(R.id.keyDetails);
        TextView keyExpiry = (TextView) view.findViewById(R.id.keyExpiry);
        ImageView masterKeyIcon = (ImageView) view.findViewById(R.id.ic_masterKey);
        ImageView certifyIcon = (ImageView) view.findViewById(R.id.ic_certifyKey);
        ImageView encryptIcon = (ImageView) view.findViewById(R.id.ic_encryptKey);
        ImageView signIcon = (ImageView) view.findViewById(R.id.ic_signKey);
        ImageView revokedKeyIcon = (ImageView) view.findViewById(R.id.ic_revokedKey);

        String keyIdStr = PgpKeyHelper.convertKeyIdToHex(cursor.getLong(INDEX_KEY_ID));
        String algorithmStr = PgpKeyHelper.getAlgorithmInfo(
                context,
                cursor.getInt(INDEX_ALGORITHM),
                cursor.getInt(INDEX_KEY_SIZE)
        );

        keyId.setText(keyIdStr);
        // may be set with additional "stripped" later on
        if (hasAnySecret && cursor.getInt(INDEX_HAS_SECRET) == 0) {
            keyDetails.setText(algorithmStr + ", " +
                    context.getString(R.string.key_stripped));
        } else {
            keyDetails.setText(algorithmStr);
        }

        // Set icons according to properties
        masterKeyIcon.setVisibility(cursor.getInt(INDEX_RANK) == 0 ? View.VISIBLE : View.INVISIBLE);
        certifyIcon.setVisibility(cursor.getInt(INDEX_CAN_CERTIFY) != 0 ? View.VISIBLE : View.GONE);
        encryptIcon.setVisibility(cursor.getInt(INDEX_CAN_ENCRYPT) != 0 ? View.VISIBLE : View.GONE);
        signIcon.setVisibility(cursor.getInt(INDEX_CAN_SIGN) != 0 ? View.VISIBLE : View.GONE);

        boolean valid = true;
        if (cursor.getInt(INDEX_IS_REVOKED) > 0) {
            revokedKeyIcon.setVisibility(View.VISIBLE);

            valid = false;
        } else {
            keyId.setTextColor(mDefaultTextColor);
            keyDetails.setTextColor(mDefaultTextColor);
            keyExpiry.setTextColor(mDefaultTextColor);

            revokedKeyIcon.setVisibility(View.GONE);
        }

        if (!cursor.isNull(INDEX_EXPIRY)) {
            Date expiryDate = new Date(cursor.getLong(INDEX_EXPIRY) * 1000);

            valid = valid && expiryDate.after(new Date());
            keyExpiry.setText(
                    context.getString(R.string.label_expiry) + ": " +
                            DateFormat.getDateFormat(context).format(expiryDate)
            );
        } else {
            keyExpiry.setText(
                    context.getString(R.string.label_expiry) + ": " +
                            context.getString(R.string.none)
            );
        }

        // if key is expired or revoked, strike through text
        if (!valid) {
            keyId.setText(OtherHelper.strikeOutText(keyId.getText()));
            keyDetails.setText(OtherHelper.strikeOutText(keyDetails.getText()));
            keyExpiry.setText(OtherHelper.strikeOutText(keyExpiry.getText()));
        }
        keyId.setEnabled(valid);
        keyDetails.setEnabled(valid);
        keyExpiry.setEnabled(valid);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.view_key_keys_item, null);
        if (mDefaultTextColor == null) {
            TextView keyId = (TextView) view.findViewById(R.id.keyId);
            mDefaultTextColor = keyId.getTextColors();
        }
        return view;
    }

    // Disable selection of items, http://stackoverflow.com/a/4075045
    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    // Disable selection of items, http://stackoverflow.com/a/4075045
    @Override
    public boolean isEnabled(int position) {
        return false;
    }

}
