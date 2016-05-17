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

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;

import java.util.List;

public class UserIdsAddedAdapter extends ArrayAdapter<String> {
    private LayoutInflater mInflater;
    private boolean mNewKeyring;

    // hold a private reference to the underlying data List
    private List<String> mData;

    public UserIdsAddedAdapter(Activity activity, List<String> data, boolean newKeyring) {
        super(activity, -1, data);
        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mData = data;
        mNewKeyring = newKeyring;
    }

    public List<String> getData() {
        return mData;
    }

    static class ViewHolder {
        public TextView vAddress;
        public TextView vName;
        public TextView vComment;
        public ImageButton vDelete;
        // also hold a reference to the model item
        public String mModel;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            // Not recycled, inflate a new view
            convertView = mInflater.inflate(R.layout.view_key_adv_user_id_item, null);
            final ViewHolder holder = new ViewHolder();
            holder.vAddress = (TextView) convertView.findViewById(R.id.user_id_item_address);
            holder.vName = (TextView) convertView.findViewById(R.id.user_id_item_name);
            holder.vComment = (TextView) convertView.findViewById(R.id.user_id_item_comment);
            holder.vDelete = (ImageButton) convertView.findViewById(R.id.user_id_item_delete_button);
            holder.vDelete.setVisibility(View.VISIBLE); // always visible

            // not used:
            View certifiedLayout = convertView.findViewById(R.id.user_id_icon_animator);
            ImageView editImage = (ImageView) convertView.findViewById(R.id.user_id_item_edit_image);
            certifiedLayout.setVisibility(View.GONE);
            editImage.setVisibility(View.GONE);

            convertView.setTag(holder);

            holder.vDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // remove reference model item from adapter (data and notify about change)
                    UserIdsAddedAdapter.this.remove(holder.mModel);
                }
            });
        }
        final ViewHolder holder = (ViewHolder) convertView.getTag();

        // save reference to model item
        holder.mModel = getItem(position);

        OpenPgpUtils.UserId splitUserId = KeyRing.splitUserId(holder.mModel);
        if (splitUserId.name != null) {
            holder.vName.setText(splitUserId.name);
        } else {
            holder.vName.setText(R.string.user_id_no_name);
        }
        if (splitUserId.email != null) {
            holder.vAddress.setText(splitUserId.email);
            holder.vAddress.setVisibility(View.VISIBLE);
        } else {
            holder.vAddress.setVisibility(View.GONE);
        }
        if (splitUserId.comment != null) {
            holder.vComment.setText(splitUserId.comment);
            holder.vComment.setVisibility(View.VISIBLE);
        } else {
            holder.vComment.setVisibility(View.GONE);
        }

        boolean isPrimary = mNewKeyring && position == 0;
        if (isPrimary) {
            holder.vName.setTypeface(null, Typeface.BOLD);
            holder.vAddress.setTypeface(null, Typeface.BOLD);
        } else {
            holder.vName.setTypeface(null, Typeface.NORMAL);
            holder.vAddress.setTypeface(null, Typeface.NORMAL);
        }

        return convertView;
    }

}
