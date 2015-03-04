/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.linked.RawLinkedIdentity;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

public class LinkedIdsAdapter extends UserAttributesAdapter {
    protected LayoutInflater mInflater;

    public LinkedIdsAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemViewType(int position) {
        RawLinkedIdentity id = (RawLinkedIdentity) getItem(position);

        // TODO return different ids by type

        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public Object getItem(int position) {
        Cursor c = getCursor();
        c.moveToPosition(position);

        byte[] data = c.getBlob(INDEX_ATTRIBUTE_DATA);
        RawLinkedIdentity identity = RawLinkedIdentity.fromSubpacketData(data);

        return identity;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView vName = (TextView) view.findViewById(R.id.user_id_item_name);
        TextView vAddress = (TextView) view.findViewById(R.id.user_id_item_address);
        TextView vComment = (TextView) view.findViewById(R.id.user_id_item_comment);
        ImageView vVerified = (ImageView) view.findViewById(R.id.user_id_item_certified);
        View vVerifiedLayout = view.findViewById(R.id.user_id_item_certified_layout);
        ImageView vEditImage = (ImageView) view.findViewById(R.id.user_id_item_edit_image);
        ImageView vDeleteButton = (ImageView) view.findViewById(R.id.user_id_item_delete_button);
        vDeleteButton.setVisibility(View.GONE); // not used

        String userId = cursor.getString(INDEX_USER_ID);
        String[] splitUserId = KeyRing.splitUserId(userId);
        if (splitUserId[0] != null) {
            vName.setText(splitUserId[0]);
        } else {
            vName.setText(R.string.user_id_no_name);
        }
        if (splitUserId[1] != null) {
            vAddress.setText(splitUserId[1]);
            vAddress.setVisibility(View.VISIBLE);
        } else {
            vAddress.setVisibility(View.GONE);
        }
        if (splitUserId[2] != null) {
            vComment.setText(splitUserId[2]);
            vComment.setVisibility(View.VISIBLE);
        } else {
            vComment.setVisibility(View.GONE);
        }

        boolean isPrimary = cursor.getInt(INDEX_IS_PRIMARY) != 0;
        boolean isRevoked = cursor.getInt(INDEX_IS_REVOKED) > 0;
        vVerifiedLayout.setVisibility(View.VISIBLE);

        if (isRevoked) {
            // set revocation icon (can this even be primary?)
            KeyFormattingUtils.setStatusImage(mContext, vVerified, null, KeyFormattingUtils.STATE_REVOKED, R.color.bg_gray);

            // disable revoked user ids
            vName.setEnabled(false);
            vAddress.setEnabled(false);
            vComment.setEnabled(false);
        } else {
            vName.setEnabled(true);
            vAddress.setEnabled(true);
            vComment.setEnabled(true);

            if (isPrimary) {
                vName.setTypeface(null, Typeface.BOLD);
                vAddress.setTypeface(null, Typeface.BOLD);
            } else {
                vName.setTypeface(null, Typeface.NORMAL);
                vAddress.setTypeface(null, Typeface.NORMAL);
            }

            int isVerified = cursor.getInt(INDEX_VERIFIED);
            switch (isVerified) {
                case Certs.VERIFIED_SECRET:
                    KeyFormattingUtils.setStatusImage(mContext, vVerified, null, KeyFormattingUtils.STATE_VERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
                case Certs.VERIFIED_SELF:
                    KeyFormattingUtils.setStatusImage(mContext, vVerified, null, KeyFormattingUtils.STATE_UNVERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
                default:
                    KeyFormattingUtils.setStatusImage(mContext, vVerified, null, KeyFormattingUtils.STATE_INVALID, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
            }
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.view_key_adv_user_id_item, null);
    }

    // don't show revoked user ids, irrelevant for average users
    public static final String LINKED_IDS_WHERE = UserPackets.IS_REVOKED + " = 0";

    public static CursorLoader createLoader(Activity activity, Uri dataUri) {
        Uri baseUri = UserPackets.buildLinkedIdsUri(dataUri);
        return new CursorLoader(activity, baseUri,
                UserIdsAdapter.USER_PACKETS_PROJECTION, LINKED_IDS_WHERE, null, null);
    }

}
