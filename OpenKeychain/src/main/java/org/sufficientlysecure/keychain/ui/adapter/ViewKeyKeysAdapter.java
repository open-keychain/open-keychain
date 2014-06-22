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

public class ViewKeyKeysAdapter extends CursorAdapter {
    private LayoutInflater mInflater;

    private int mIndexKeyId;
    private int mIndexAlgorithm;
    private int mIndexKeySize;
    private int mIndexRank;
    private int mIndexCanCertify;
    private int mIndexCanEncrypt;
    private int mIndexCanSign;
    private int mIndexHasSecret;
    private int mIndexRevokedKey;
    private int mIndexExpiry;

    private boolean hasAnySecret;

    private ColorStateList mDefaultTextColor;

    public static final String[] KEYS_PROJECTION = new String[] {
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

    public ViewKeyKeysAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);

        mInflater = LayoutInflater.from(context);

        initIndex(c);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        initIndex(newCursor);

        hasAnySecret = false;
        if (newCursor != null) {
            newCursor.moveToFirst();
            do {
                if (newCursor.getInt(mIndexHasSecret) != 0) {
                    hasAnySecret = true;
                    break;
                }
            } while (newCursor.moveToNext());
        }

        return super.swapCursor(newCursor);
    }

    /**
     * Get column indexes for performance reasons just once in constructor and swapCursor. For a
     * performance comparison see http://stackoverflow.com/a/17999582
     *
     * @param cursor
     */
    private void initIndex(Cursor cursor) {
        if (cursor != null) {
            mIndexKeyId = cursor.getColumnIndexOrThrow(Keys.KEY_ID);
            mIndexAlgorithm = cursor.getColumnIndexOrThrow(Keys.ALGORITHM);
            mIndexKeySize = cursor.getColumnIndexOrThrow(Keys.KEY_SIZE);
            mIndexRank = cursor.getColumnIndexOrThrow(Keys.RANK);
            mIndexCanCertify = cursor.getColumnIndexOrThrow(Keys.CAN_CERTIFY);
            mIndexCanEncrypt = cursor.getColumnIndexOrThrow(Keys.CAN_ENCRYPT);
            mIndexCanSign = cursor.getColumnIndexOrThrow(Keys.CAN_SIGN);
            mIndexHasSecret = cursor.getColumnIndexOrThrow(Keys.HAS_SECRET);
            mIndexRevokedKey = cursor.getColumnIndexOrThrow(Keys.IS_REVOKED);
            mIndexExpiry = cursor.getColumnIndexOrThrow(Keys.EXPIRY);
        }
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

        String keyIdStr = PgpKeyHelper.convertKeyIdToHex(cursor.getLong(mIndexKeyId));
        String algorithmStr = PgpKeyHelper.getAlgorithmInfo(
                context,
                cursor.getInt(mIndexAlgorithm),
                cursor.getInt(mIndexKeySize)
        );

        keyId.setText(keyIdStr);
        // may be set with additional "stripped" later on
        if (hasAnySecret && cursor.getInt(mIndexHasSecret) == 0) {
            keyDetails.setText(algorithmStr + ", " +
                    context.getString(R.string.key_stripped));
        } else {
            keyDetails.setText(algorithmStr);
        }

        // Set icons according to properties
        masterKeyIcon.setVisibility(cursor.getInt(mIndexRank) == 0 ? View.VISIBLE : View.INVISIBLE);
        certifyIcon.setVisibility(cursor.getInt(mIndexCanCertify) != 0 ? View.VISIBLE : View.GONE);
        encryptIcon.setVisibility(cursor.getInt(mIndexCanEncrypt) != 0 ? View.VISIBLE : View.GONE);
        signIcon.setVisibility(cursor.getInt(mIndexCanSign) != 0 ? View.VISIBLE : View.GONE);

        boolean valid = true;
        if (cursor.getInt(mIndexRevokedKey) > 0) {
            revokedKeyIcon.setVisibility(View.VISIBLE);

            valid = false;
        } else {
            keyId.setTextColor(mDefaultTextColor);
            keyDetails.setTextColor(mDefaultTextColor);
            keyExpiry.setTextColor(mDefaultTextColor);

            revokedKeyIcon.setVisibility(View.GONE);
        }

        if (!cursor.isNull(mIndexExpiry)) {
            Date expiryDate = new Date(cursor.getLong(mIndexExpiry) * 1000);

            valid = valid && expiryDate.after(new Date());
            keyExpiry.setText(
                    context.getString(R.string.label_expiry) + ": " +
                    DateFormat.getDateFormat(context).format(expiryDate));
        } else {
            keyExpiry.setText(
                    context.getString(R.string.label_expiry) + ": " +
                    context.getString(R.string.none));
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
