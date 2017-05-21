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


import java.io.IOException;
import java.util.WeakHashMap;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.support.v4.content.CursorLoader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.linked.LinkedAttribute;
import org.sufficientlysecure.keychain.linked.UriAttribute;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.ui.keyview.LinkedIdViewFragment;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;
import org.sufficientlysecure.keychain.ui.util.SubtleAttentionSeeker;
import org.sufficientlysecure.keychain.util.FilterCursorWrapper;
import org.sufficientlysecure.keychain.util.Log;

public class LinkedIdsAdapter extends UserAttributesAdapter {
    private final boolean mIsSecret;
    protected LayoutInflater mInflater;
    WeakHashMap<Integer,UriAttribute> mLinkedIdentityCache = new WeakHashMap<>();

    public LinkedIdsAdapter(Context context, Cursor c, int flags, boolean isSecret) {
        super(context, c, flags);
        mInflater = LayoutInflater.from(context);
        mIsSecret = isSecret;
    }

    @Override
    public Cursor swapCursor(Cursor cursor) {
        if (cursor == null) {
            return super.swapCursor(null);
        }
        FilterCursorWrapper filteredCursor = new FilterCursorWrapper(cursor) {
            @Override
            public boolean isVisible(Cursor cursor) {
                UriAttribute id = getItemAtPosition(cursor);
                return id instanceof LinkedAttribute;
            }
        };

        return super.swapCursor(filteredCursor);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder holder = (ViewHolder) view.getTag();

        if (!mIsSecret) {
            int isVerified = cursor.getInt(INDEX_VERIFIED);
            switch (isVerified) {
                case Certs.VERIFIED_SECRET:
                    KeyFormattingUtils.setStatusImage(mContext, holder.vVerified,
                            null, State.VERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
                case Certs.VERIFIED_SELF:
                    KeyFormattingUtils.setStatusImage(mContext, holder.vVerified,
                            null, State.UNVERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
                default:
                    KeyFormattingUtils.setStatusImage(mContext, holder.vVerified,
                            null, State.INVALID, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
            }
        }

        UriAttribute id = getItemAtPosition(cursor);
        holder.setData(mContext, id);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setTransitionName(id.mUri.toString());
        }

    }

    public UriAttribute getItemAtPosition(Cursor cursor) {
        int rank = cursor.getInt(INDEX_RANK);
        Log.d(Constants.TAG, "requested rank: " + rank);

        UriAttribute ret = mLinkedIdentityCache.get(rank);
        if (ret != null) {
            Log.d(Constants.TAG, "cached!");
            return ret;
        }
        Log.d(Constants.TAG, "not cached!");

        try {
            byte[] data = cursor.getBlob(INDEX_ATTRIBUTE_DATA);
            ret = LinkedAttribute.fromAttributeData(data);
            mLinkedIdentityCache.put(rank, ret);
            return ret;
        } catch (IOException e) {
            Log.e(Constants.TAG, "could not read linked identity subpacket data", e);
            return null;
        }
    }

    @Override
    public UriAttribute getItem(int position) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);
        return getItemAtPosition(cursor);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = mInflater.inflate(R.layout.linked_id_item, null);
        ViewHolder holder = new ViewHolder(v);
        v.setTag(holder);
        return v;
    }

    // don't show revoked user ids, irrelevant for average users
    public static final String LINKED_IDS_WHERE = UserPackets.IS_REVOKED + " = 0";

    public static CursorLoader createLoader(Context context, Uri dataUri) {
        Uri baseUri = UserPackets.buildLinkedIdsUri(dataUri);
        return new CursorLoader(context, baseUri,
                UserIdsAdapter.USER_PACKETS_PROJECTION, LINKED_IDS_WHERE, null, null);
    }

    public LinkedIdViewFragment getLinkedIdFragment(int position, long masterKeyId) throws IOException {
        Cursor c = getCursor();
        c.moveToPosition(position);
        int rank = c.getInt(UserIdsAdapter.INDEX_RANK);

        Uri dataUri = UserPackets.buildLinkedIdsUri(KeyRings.buildGenericKeyRingUri(masterKeyId));
        return LinkedIdViewFragment.newInstance(dataUri, rank, mIsSecret, masterKeyId);
    }

    public static class ViewHolder {
        final public ImageView vVerified;
        final public ImageView vIcon;
        final public TextView vTitle;
        final public TextView vComment;

        public ViewHolder(View view) {
            vVerified = (ImageView) view.findViewById(R.id.linked_id_certified_icon);
            vIcon = (ImageView) view.findViewById(R.id.linked_id_type_icon);
            vTitle = (TextView) view.findViewById(R.id.linked_id_title);
            vComment = (TextView) view.findViewById(R.id.linked_id_comment);
        }

        public void setData(Context context, UriAttribute id) {

            vTitle.setText(id.getDisplayTitle(context));

            String comment = id.getDisplayComment(context);
            if (comment != null) {
                vComment.setVisibility(View.VISIBLE);
                vComment.setText(comment);
            } else {
                vComment.setVisibility(View.GONE);
            }

            vIcon.setImageResource(id.getDisplayIcon());

        }

        public void seekAttention() {
            if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                ObjectAnimator anim = SubtleAttentionSeeker.tintText(vComment, 1000);
                anim.setStartDelay(200);
                anim.start();
            }
        }

    }

}
