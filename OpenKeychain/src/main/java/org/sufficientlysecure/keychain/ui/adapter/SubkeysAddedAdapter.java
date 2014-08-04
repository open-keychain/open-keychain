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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;

import java.util.Date;
import java.util.List;

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
        public ImageView vRevokedIcon;
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
            holder.vRevokedIcon = (ImageView) convertView.findViewById(R.id.subkey_item_ic_revoked);
            holder.vDelete = (ImageButton) convertView.findViewById(R.id.subkey_item_delete_button);
            holder.vDelete.setVisibility(View.VISIBLE); // always visible

            // not used:
            ImageView editImage = (ImageView) convertView.findViewById(R.id.subkey_item_edit_image);
            editImage.setVisibility(View.GONE);

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
        holder.vKeyId.setText("new");
        holder.vKeyDetails.setText(algorithmStr);

        // Set icons according to properties
//        holder.vMasterIcon.setVisibility(cursor.getInt(INDEX_RANK) == 0 ? View.VISIBLE : View.INVISIBLE);
//        holder.vCertifyIcon.setVisibility(cursor.getInt(INDEX_CAN_CERTIFY) != 0 ? View.VISIBLE : View.GONE);
//        holder.vEncryptIcon.setVisibility(cursor.getInt(INDEX_CAN_ENCRYPT) != 0 ? View.VISIBLE : View.GONE);
//        holder.vSignIcon.setVisibility(cursor.getInt(INDEX_CAN_SIGN) != 0 ? View.VISIBLE : View.GONE);
//        if (!cursor.isNull(INDEX_EXPIRY)) {
//            Date expiryDate = new Date(cursor.getLong(INDEX_EXPIRY) * 1000);
//            isExpired = expiryDate.before(new Date());
//
//            holder.vKeyExpiry.setText(context.getString(R.string.label_expiry) + ": "
//                    + DateFormat.getDateFormat(context).format(expiryDate));
//        } else {
//            isExpired = false;
//
//            holder.vKeyExpiry.setText(context.getString(R.string.label_expiry) + ": " + context.getString(R.string.none));
//        }
//
//        holder.vAddress.setText(holder.mModel.address);
//        holder.vAddress.setThreshold(1); // Start working from first character
//        holder.vAddress.setAdapter(mAutoCompleteEmailAdapter);
//
//        holder.vName.setText(holder.mModel.name);
//        holder.vName.setThreshold(1); // Start working from first character
//        holder.vName.setAdapter(mAutoCompleteNameAdapter);
//
//        holder.vComment.setText(holder.mModel.comment);

        return convertView;
    }

}
