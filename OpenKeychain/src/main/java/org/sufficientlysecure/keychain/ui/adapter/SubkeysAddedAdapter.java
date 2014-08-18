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
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class SubkeysAddedAdapter extends ArrayAdapter<SaveKeyringParcel.SubkeyAdd> {
    private LayoutInflater mInflater;
    private Activity mActivity;

    // hold a private reference to the underlying data List
    private List<SaveKeyringParcel.SubkeyAdd> mData;

    public SubkeysAddedAdapter(Activity activity, List<SaveKeyringParcel.SubkeyAdd> data) {
        super(activity, -1, data);
        mActivity = activity;
        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mData = data;
    }

    static class ViewHolder {
        public TextView vKeyId;
        public TextView vKeyDetails;
        public TextView vKeyExpiry;
        public ImageView vCertifyIcon;
        public ImageView vEncryptIcon;
        public ImageView vSignIcon;
        public ImageButton vDelete;
        // also hold a reference to the model item
        public SaveKeyringParcel.SubkeyAdd mModel;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            // Not recycled, inflate a new view
            convertView = mInflater.inflate(R.layout.view_key_subkey_item, null);
            final ViewHolder holder = new ViewHolder();
            holder.vKeyId = (TextView) convertView.findViewById(R.id.subkey_item_key_id);
            holder.vKeyDetails = (TextView) convertView.findViewById(R.id.subkey_item_details);
            holder.vKeyExpiry = (TextView) convertView.findViewById(R.id.subkey_item_expiry);
            holder.vCertifyIcon = (ImageView) convertView.findViewById(R.id.subkey_item_ic_certify);
            holder.vEncryptIcon = (ImageView) convertView.findViewById(R.id.subkey_item_ic_encrypt);
            holder.vSignIcon = (ImageView) convertView.findViewById(R.id.subkey_item_ic_sign);
            holder.vDelete = (ImageButton) convertView.findViewById(R.id.subkey_item_delete_button);
            holder.vDelete.setVisibility(View.VISIBLE); // always visible

            // not used:
            ImageView editImage = (ImageView) convertView.findViewById(R.id.subkey_item_edit_image);
            editImage.setVisibility(View.GONE);
            ImageView revokedIcon = (ImageView) convertView.findViewById(R.id.subkey_item_ic_revoked);
            revokedIcon.setVisibility(View.GONE);

            convertView.setTag(holder);

            holder.vDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // remove reference model item from adapter (data and notify about change)
                    SubkeysAddedAdapter.this.remove(holder.mModel);
                }
            });

        }
        final ViewHolder holder = (ViewHolder) convertView.getTag();

        // save reference to model item
        holder.mModel = getItem(position);

        String algorithmStr = PgpKeyHelper.getAlgorithmInfo(
                mActivity,
                holder.mModel.mAlgorithm,
                holder.mModel.mKeysize
        );
        holder.vKeyId.setText(R.string.edit_key_new_subkey);
        holder.vKeyDetails.setText(algorithmStr);

        if (holder.mModel.mExpiry != 0L) {
            Date expiryDate = new Date(holder.mModel.mExpiry * 1000);
            Calendar expiryCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            expiryCal.setTime(expiryDate);
            // convert from UTC to time zone of device
            expiryCal.setTimeZone(TimeZone.getDefault());

            holder.vKeyExpiry.setText(getContext().getString(R.string.label_expiry) + ": "
                    + DateFormat.getDateFormat(getContext()).format(expiryCal.getTime()));
        } else {
            holder.vKeyExpiry.setText(getContext().getString(R.string.label_expiry) + ": "
                    + getContext().getString(R.string.none));
        }

        int flags = holder.mModel.mFlags;
        if ((flags & KeyFlags.CERTIFY_OTHER) > 0) {
            holder.vCertifyIcon.setVisibility(View.VISIBLE);
        } else {
            holder.vCertifyIcon.setVisibility(View.GONE);
        }
        if ((flags & KeyFlags.SIGN_DATA) > 0) {
            holder.vSignIcon.setVisibility(View.VISIBLE);
        } else {
            holder.vSignIcon.setVisibility(View.GONE);
        }
        if (((flags & KeyFlags.ENCRYPT_COMMS) > 0)
                || ((flags & KeyFlags.ENCRYPT_STORAGE) > 0)) {
            holder.vEncryptIcon.setVisibility(View.VISIBLE);
        } else {
            holder.vEncryptIcon.setVisibility(View.GONE);
        }
        // TODO: missing icon for authenticate

        return convertView;
    }

}
