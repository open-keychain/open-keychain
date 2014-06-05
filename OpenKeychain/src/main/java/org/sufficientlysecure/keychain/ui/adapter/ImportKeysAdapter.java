/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.util.Highlighter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ImportKeysAdapter extends ArrayAdapter<ImportKeysListEntry> {
    protected LayoutInflater mInflater;
    protected Activity mActivity;

    protected List<ImportKeysListEntry> mData;

    static class ViewHolder {
        public TextView mainUserId;
        public TextView mainUserIdRest;
        public TextView keyId;
        public TextView fingerprint;
        public TextView algorithm;
        public TextView status;
        public LinearLayout userIdsList;
        public CheckBox checkBox;
    }

    public ImportKeysAdapter(Activity activity) {
        super(activity, -1);
        mActivity = activity;
        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setData(List<ImportKeysListEntry> data) {
        clear();
        if (data != null) {
            this.mData = data;

            // add data to extended ArrayAdapter
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                addAll(data);
            } else {
                for (ImportKeysListEntry entry : data) {
                    add(entry);
                }
            }
        }
    }

    public List<ImportKeysListEntry> getData() {
        return mData;
    }

    public ArrayList<ImportKeysListEntry> getSelectedEntries() {
        ArrayList<ImportKeysListEntry> selectedData = new ArrayList<ImportKeysListEntry>();
        for (ImportKeysListEntry entry : mData) {
            if (entry.isSelected()) {
                selectedData.add(entry);
            }
        }
        return selectedData;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ImportKeysListEntry entry = mData.get(position);
        Highlighter highlighter = new Highlighter(mActivity, entry.getQuery());
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.import_keys_list_entry, null);
            holder.mainUserId = (TextView) convertView.findViewById(R.id.mainUserId);
            holder.mainUserIdRest = (TextView) convertView.findViewById(R.id.mainUserIdRest);
            holder.keyId = (TextView) convertView.findViewById(R.id.keyId);
            holder.fingerprint = (TextView) convertView.findViewById(R.id.view_key_fingerprint);
            holder.algorithm = (TextView) convertView.findViewById(R.id.algorithm);
            holder.status = (TextView) convertView.findViewById(R.id.status);
            holder.userIdsList = (LinearLayout) convertView.findViewById(R.id.user_ids_list);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.selected);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // main user id
        String userId = entry.getUserIds().get(0);
        String[] userIdSplit = KeyRing.splitUserId(userId);

        // name
        if (userIdSplit[0] != null) {
            // show red user id if it is a secret key
            if (entry.isSecretKey()) {
                holder.mainUserId.setText(mActivity.getString(R.string.secret_key)
                        + " " + userIdSplit[0]);
                holder.mainUserId.setTextColor(Color.RED);
            } else {
                holder.mainUserId.setText(highlighter.highlight(userIdSplit[0]));
                holder.mainUserId.setTextColor(Color.BLACK);
            }
        } else {
            holder.mainUserId.setTextColor(Color.BLACK);
            holder.mainUserId.setText(R.string.user_id_no_name);
        }

        // email
        if (userIdSplit[1] != null) {
            holder.mainUserIdRest.setVisibility(View.VISIBLE);
            holder.mainUserIdRest.setText(highlighter.highlight(userIdSplit[1]));
        } else {
            holder.mainUserIdRest.setVisibility(View.GONE);
        }

        holder.keyId.setText(entry.getKeyIdHex());

        // don't show full fingerprint on key import
        holder.fingerprint.setVisibility(View.GONE);

        if (entry.getBitStrength() != 0 && entry.getAlgorithm() != null) {
            holder.algorithm.setText("" + entry.getBitStrength() + "/" + entry.getAlgorithm());
            holder.algorithm.setVisibility(View.VISIBLE);
        } else {
            holder.algorithm.setVisibility(View.INVISIBLE);
        }

        if (entry.isRevoked()) {
            holder.status.setVisibility(View.VISIBLE);
            holder.status.setText(R.string.revoked);
        } else {
            holder.status.setVisibility(View.GONE);
        }

        if (entry.getUserIds().size() == 1) {
            holder.userIdsList.setVisibility(View.GONE);
        } else {
            holder.userIdsList.setVisibility(View.VISIBLE);

            // clear view from holder
            holder.userIdsList.removeAllViews();

            Iterator<String> it = entry.getUserIds().iterator();
            // skip primary user id
            it.next();
            while (it.hasNext()) {
                String uid = it.next();
                TextView uidView = (TextView) mInflater.inflate(
                        R.layout.import_keys_list_entry_user_id, null);
                uidView.setText(highlighter.highlight(uid));
                holder.userIdsList.addView(uidView);
            }
        }

        holder.checkBox.setChecked(entry.isSelected());

        return convertView;
    }

}
