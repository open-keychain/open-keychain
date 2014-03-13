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
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;

public class ViewKeyKeysAdapter extends CursorAdapter {
    private LayoutInflater mInflater;

    private int mIndexKeyId;
    private int mIndexAlgorithm;
    private int mIndexKeySize;
    private int mIndexIsMasterKey;
    private int mIndexCanCertify;
    private int mIndexCanEncrypt;
    private int mIndexCanSign;

    public ViewKeyKeysAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);

        mInflater = LayoutInflater.from(context);

        initIndex(c);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        initIndex(newCursor);

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
            mIndexIsMasterKey = cursor.getColumnIndexOrThrow(Keys.IS_MASTER_KEY);
            mIndexCanCertify = cursor.getColumnIndexOrThrow(Keys.CAN_CERTIFY);
            mIndexCanEncrypt = cursor.getColumnIndexOrThrow(Keys.CAN_ENCRYPT);
            mIndexCanSign = cursor.getColumnIndexOrThrow(Keys.CAN_SIGN);
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView keyId = (TextView) view.findViewById(R.id.keyId);
        TextView keyDetails = (TextView) view.findViewById(R.id.keyDetails);
        ImageView masterKeyIcon = (ImageView) view.findViewById(R.id.ic_masterKey);
        ImageView certifyIcon = (ImageView) view.findViewById(R.id.ic_certifyKey);
        ImageView encryptIcon = (ImageView) view.findViewById(R.id.ic_encryptKey);
        ImageView signIcon = (ImageView) view.findViewById(R.id.ic_signKey);

        String keyIdStr = PgpKeyHelper.convertKeyIdToHex(cursor.getLong(mIndexKeyId));
        String algorithmStr = PgpKeyHelper.getAlgorithmInfo(cursor.getInt(mIndexAlgorithm),
                cursor.getInt(mIndexKeySize));

        keyId.setText(keyIdStr);

        keyDetails.setText("(" + algorithmStr + ")");

        if (cursor.getInt(mIndexIsMasterKey) != 1) {
            masterKeyIcon.setVisibility(View.INVISIBLE);
        } else {
            masterKeyIcon.setVisibility(View.VISIBLE);
        }

        if (cursor.getInt(mIndexCanCertify) != 1) {
            certifyIcon.setVisibility(View.GONE);
        } else {
            certifyIcon.setVisibility(View.VISIBLE);
        }

        if (cursor.getInt(mIndexCanEncrypt) != 1) {
            encryptIcon.setVisibility(View.GONE);
        } else {
            encryptIcon.setVisibility(View.VISIBLE);
        }

        if (cursor.getInt(mIndexCanSign) != 1) {
            signIcon.setVisibility(View.GONE);
        } else {
            signIcon.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.view_key_keys_item, null);
    }

}
