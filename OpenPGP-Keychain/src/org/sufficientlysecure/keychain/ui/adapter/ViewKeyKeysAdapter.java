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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ViewKeyKeysAdapter extends CursorAdapter {
    private LayoutInflater mInflater;

    public ViewKeyKeysAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);

        mInflater = LayoutInflater.from(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String keyIdStr = PgpKeyHelper.convertKeyIdToHex(cursor.getLong(cursor
                .getColumnIndex(Keys.KEY_ID)));
        String algorithmStr = PgpKeyHelper.getAlgorithmInfo(
                cursor.getInt(cursor.getColumnIndex(Keys.ALGORITHM)),
                cursor.getInt(cursor.getColumnIndex(Keys.KEY_SIZE)));

        TextView keyId = (TextView) view.findViewById(R.id.keyId);
        keyId.setText(keyIdStr);

        TextView keyDetails = (TextView) view.findViewById(R.id.keyDetails);
        keyDetails.setText("(" + algorithmStr + ")");

        ImageView masterKeyIcon = (ImageView) view.findViewById(R.id.ic_masterKey);
        if (cursor.getInt(cursor.getColumnIndex(Keys.IS_MASTER_KEY)) != 1) {
            masterKeyIcon.setVisibility(View.INVISIBLE);
        } else {
            masterKeyIcon.setVisibility(View.VISIBLE);
        }

        ImageView certifyIcon = (ImageView) view.findViewById(R.id.ic_certifyKey);
        if (cursor.getInt(cursor.getColumnIndex(Keys.CAN_CERTIFY)) != 1) {
            certifyIcon.setVisibility(View.GONE);
        } else {
            certifyIcon.setVisibility(View.VISIBLE);
        }

        ImageView encryptIcon = (ImageView) view.findViewById(R.id.ic_encryptKey);
        if (cursor.getInt(cursor.getColumnIndex(Keys.CAN_ENCRYPT)) != 1) {
            encryptIcon.setVisibility(View.GONE);
        } else {
            encryptIcon.setVisibility(View.VISIBLE);
        }

        ImageView signIcon = (ImageView) view.findViewById(R.id.ic_signKey);
        if (cursor.getInt(cursor.getColumnIndex(Keys.CAN_SIGN)) != 1) {
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
