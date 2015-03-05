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
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.linked.LinkedIdentity;
import org.sufficientlysecure.keychain.pgp.linked.LinkedResource;
import org.sufficientlysecure.keychain.pgp.linked.RawLinkedIdentity;
import org.sufficientlysecure.keychain.pgp.linked.resources.DnsResource;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.ui.ViewKeyFragment;
import org.sufficientlysecure.keychain.ui.linked.LinkedIdViewFragment;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

import java.io.IOException;
import java.util.WeakHashMap;


public class LinkedIdsAdapter extends UserAttributesAdapter {
    protected LayoutInflater mInflater;
    WeakHashMap<Integer,RawLinkedIdentity> mLinkedIdentityCache = new WeakHashMap<>();

    public LinkedIdsAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        RawLinkedIdentity id = getItem(cursor.getPosition());
        ViewHolder holder = (ViewHolder) view.getTag();

        int isVerified = cursor.getInt(INDEX_VERIFIED);
        switch (isVerified) {
            case Certs.VERIFIED_SECRET:
                KeyFormattingUtils.setStatusImage(mContext, holder.vVerified,
                        null, KeyFormattingUtils.STATE_VERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                break;
            case Certs.VERIFIED_SELF:
                KeyFormattingUtils.setStatusImage(mContext, holder.vVerified,
                        null, KeyFormattingUtils.STATE_UNVERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                break;
            default:
                KeyFormattingUtils.setStatusImage(mContext, holder.vVerified,
                        null, KeyFormattingUtils.STATE_INVALID, KeyFormattingUtils.DEFAULT_COLOR);
                break;
        }

        if (holder instanceof ViewHolderNonRaw) {
            ((ViewHolderNonRaw) holder).setData(mContext, (LinkedIdentity) id);
        }

    }

    @Override
    public int getItemViewType(int position) {
        RawLinkedIdentity id = getItem(position);

        if (id instanceof LinkedIdentity) {
            LinkedResource res = ((LinkedIdentity) id).mResource;
            if (res instanceof DnsResource) {
                return 1;
            }
        }

        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public RawLinkedIdentity getItem(int position) {
        RawLinkedIdentity ret = mLinkedIdentityCache.get(position);
        if (ret != null) {
            return ret;
        }

        Cursor c = getCursor();
        c.moveToPosition(position);

        byte[] data = c.getBlob(INDEX_ATTRIBUTE_DATA);
        try {
            ret = LinkedIdentity.fromAttributeData(data);
            mLinkedIdentityCache.put(position, ret);
            return ret;
        } catch (IOException e) {
            Log.e(Constants.TAG, "could not read linked identity subpacket data", e);
            return null;
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int type = getItemViewType(cursor.getPosition());
        switch(type) {
            case 0: {
                View v = mInflater.inflate(R.layout.linked_id_item_unknown, null);
                ViewHolder holder = new ViewHolder(v);
                v.setTag(holder);
                return v;
            }
            case 1: {
                View v = mInflater.inflate(R.layout.linked_id_item_dns, null);
                ViewHolder holder = new ViewHolderDns(v);
                v.setTag(holder);
                return v;
            }
            default:
                throw new AssertionError("all cases must be covered in LinkedIdsAdapter.newView!");
        }
    }

    // don't show revoked user ids, irrelevant for average users
    public static final String LINKED_IDS_WHERE = UserPackets.IS_REVOKED + " = 0";

    public static CursorLoader createLoader(Activity activity, Uri dataUri) {
        Uri baseUri = UserPackets.buildLinkedIdsUri(dataUri);
        return new CursorLoader(activity, baseUri,
                UserIdsAdapter.USER_PACKETS_PROJECTION, LINKED_IDS_WHERE, null, null);
    }

    public Fragment getLinkedIdFragment(int position) {
        RawLinkedIdentity id = getItem(position);

        return LinkedIdViewFragment.newInstance(id);
    }

    static class ViewHolder {
        ImageView vVerified;

        ViewHolder(View view) {
            vVerified = (ImageView) view.findViewById(R.id.user_id_item_certified);
        }
    }

    static abstract class ViewHolderNonRaw extends ViewHolder {
        ViewHolderNonRaw(View view) {
            super(view);
        }

        abstract void setData(Context context, LinkedIdentity id);
    }

    static class ViewHolderDns extends ViewHolderNonRaw {
        TextView vFqdn;

        ViewHolderDns(View view) {
            super(view);

            vFqdn = (TextView) view.findViewById(R.id.linked_id_dns_fqdn);
        }

        @Override
        void setData(Context context, LinkedIdentity id) {
            DnsResource res = (DnsResource) id.mResource;
            vFqdn.setText(res.getFqdn());
        }

    }

    @Override
    public void notifyDataSetChanged() {
        mLinkedIdentityCache.clear();
        super.notifyDataSetChanged();
    }
}
