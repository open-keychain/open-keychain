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

import java.util.List;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;

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

public class ImportKeysAdapter extends ArrayAdapter<ImportKeysListEntry> {
    protected LayoutInflater mInflater;
    protected Activity mActivity;

    protected List<ImportKeysListEntry> data;

    public ImportKeysAdapter(Activity activity) {
        super(activity, -1);
        mActivity = activity;
        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @SuppressLint("NewApi")
    public void setData(List<ImportKeysListEntry> data) {
        clear();
        if (data != null) {
            this.data = data;

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
        return data;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ImportKeysListEntry entry = data.get(position);

        View view = mInflater.inflate(R.layout.import_keys_list_entry, null);

        TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        mainUserId.setText(R.string.unknown_user_id);
        TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        mainUserIdRest.setText("");
        TextView keyId = (TextView) view.findViewById(R.id.keyId);
        keyId.setText(R.string.no_key);
        TextView fingerprint = (TextView) view.findViewById(R.id.fingerprint);
        TextView algorithm = (TextView) view.findViewById(R.id.algorithm);
        algorithm.setText("");
        TextView status = (TextView) view.findViewById(R.id.status);
        status.setText("");

        String userId = entry.userIds.get(0);
        if (userId != null) {
            String[] userIdSplit = PgpKeyHelper.splitUserId(userId);

            if (userIdSplit[0] != null && userIdSplit[0].length() > 0) {
                // show red user id if it is a secret key
                if (entry.secretKey) {
                    userId = mActivity.getString(R.string.secret_key) + " " + userIdSplit[0];
                    mainUserId.setTextColor(Color.RED);
                    mainUserId.setText(userId);
                } else {
                    mainUserId.setText(userIdSplit[0]);
                }
            }

            if (userIdSplit[1] != null && userIdSplit[1].length() > 0) {
                mainUserIdRest.setText(userIdSplit[1]);
                mainUserIdRest.setVisibility(View.VISIBLE);
            } else {
                mainUserIdRest.setVisibility(View.GONE);
            }

        }

        keyId.setText(entry.hexKeyId);
        fingerprint.setText(mActivity.getString(R.string.fingerprint) + " " + entry.fingerPrint);

        algorithm.setText("" + entry.bitStrength + "/" + entry.algorithm);

        if (entry.revoked) {
            status.setText("revoked");
        } else {
            status.setVisibility(View.GONE);
        }

        LinearLayout ll = (LinearLayout) view.findViewById(R.id.list);
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

        CheckBox cBox = (CheckBox) view.findViewById(R.id.selected);
        cBox.setChecked(entry.isSelected());

        return view;
    }

}
