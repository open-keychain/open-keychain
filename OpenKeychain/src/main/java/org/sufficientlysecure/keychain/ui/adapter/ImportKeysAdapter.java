/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.annotation.SuppressLint;
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
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;

import java.util.ArrayList;
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
    }

    public ImportKeysAdapter(Activity activity) {
        super(activity, -1);
        mActivity = activity;
        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @SuppressLint("NewApi")
    public void setData(List<ImportKeysListEntry> data) {
        clear();
        if (data != null) {
            this.mData = data;

            // add data to extended ArrayAdapter
            if (Build.VERSION.SDK_INT >= 11) {
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

    public ArrayList<ImportKeysListEntry> getSelectedData() {
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
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.import_keys_list_entry, null);
            holder.mainUserId = (TextView) convertView.findViewById(R.id.mainUserId);
            holder.mainUserIdRest = (TextView) convertView.findViewById(R.id.mainUserIdRest);
            holder.keyId = (TextView) convertView.findViewById(R.id.keyId);
            holder.fingerprint = (TextView) convertView.findViewById(R.id.fingerprint);
            holder.algorithm = (TextView) convertView.findViewById(R.id.algorithm);
            holder.status = (TextView) convertView.findViewById(R.id.status);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        // main user id
        String userId = entry.userIds.get(0);
        String[] userIdSplit = PgpKeyHelper.splitUserId(userId);

        // name
        if (userIdSplit[0] != null) {
            // show red user id if it is a secret key
            if (entry.secretKey) {
                userIdSplit[0] = mActivity.getString(R.string.secret_key) + " " + userIdSplit[0];
                holder.mainUserId.setTextColor(Color.RED);
            }
            holder.mainUserId.setText(userIdSplit[0]);
        } else {
            holder.mainUserId.setText(R.string.user_id_no_name);
        }

        // email
        if (userIdSplit[1] != null) {
            holder.mainUserIdRest.setText(userIdSplit[1]);
            holder.mainUserIdRest.setVisibility(View.VISIBLE);
        } else {
            holder.mainUserIdRest.setVisibility(View.GONE);
        }

        holder.keyId.setText(entry.keyIdHex);

        if (entry.fingerPrintHex != null) {
            holder.fingerprint.setText(PgpKeyHelper.colorizeFingerprint(entry.fingerPrintHex));
            holder.fingerprint.setVisibility(View.VISIBLE);
        } else {
            holder.fingerprint.setVisibility(View.GONE);
        }

        holder.algorithm.setText("" + entry.bitStrength + "/" + entry.algorithm);

        if (entry.revoked) {
            holder.status.setText(R.string.revoked);
        } else {
            holder.status.setVisibility(View.GONE);
        }

        LinearLayout ll = (LinearLayout) convertView.findViewById(R.id.list);
        ll.removeAllViews();
        if (entry.userIds.size() == 1) {
            ll.setVisibility(View.GONE);
        } else {
            boolean first = true;
            boolean second = true;
            for (String uid : entry.userIds) {
                if (first) {
                    first = false;
                    continue;
                }
                if (!second) {
                    View sep = new View(mActivity);
                    sep.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 1));
                    sep.setBackgroundResource(android.R.drawable.divider_horizontal_dark);
                    ll.addView(sep);
                }
                TextView uidView = (TextView) mInflater.inflate(
                        R.layout.import_keys_list_entry_user_id, null);
                uidView.setText(uid);
                ll.addView(uidView);
                second = false;
            }
        }

        CheckBox cBox = (CheckBox) convertView.findViewById(R.id.selected);
        cBox.setChecked(entry.isSelected());

        return convertView;
    }

}
