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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.operations.ImportOperation;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
        public ImageView status;
        public View userIdsDivider;
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

    /** This method returns a list of all selected entries, with public keys sorted
     * before secret keys, see ImportOperation for specifics.
     * @see ImportOperation
     */
    public ArrayList<ImportKeysListEntry> getSelectedEntries() {
        ArrayList<ImportKeysListEntry> result = new ArrayList<>();
        ArrayList<ImportKeysListEntry> secrets = new ArrayList<>();
        if (mData == null) {
            return result;
        }
        for (ImportKeysListEntry entry : mData) {
            if (entry.isSelected()) {
                // add this entry to either the secret or the public list
                (entry.isSecretKey() ? secrets : result).add(entry);
            }
        }
        // add secret keys at the end of the list
        result.addAll(secrets);
        return result;
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
            convertView = mInflater.inflate(R.layout.import_keys_list_item, null);
            holder.mainUserId = (TextView) convertView.findViewById(R.id.import_item_user_id);
            holder.mainUserIdRest = (TextView) convertView.findViewById(R.id.import_item_user_id_email);
            holder.keyId = (TextView) convertView.findViewById(R.id.import_item_key_id);
            holder.fingerprint = (TextView) convertView.findViewById(R.id.import_item_fingerprint);
            holder.algorithm = (TextView) convertView.findViewById(R.id.import_item_algorithm);
            holder.status = (ImageView) convertView.findViewById(R.id.import_item_status);
            holder.userIdsDivider = convertView.findViewById(R.id.import_item_status_divider);
            holder.userIdsList = (LinearLayout) convertView.findViewById(R.id.import_item_user_ids_list);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.import_item_selected);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // main user id
        String userId = entry.getUserIds().get(0);
        KeyRing.UserId userIdSplit = KeyRing.splitUserId(userId);

        // name
        if (userIdSplit.name != null) {
            // show red user id if it is a secret key
            if (entry.isSecretKey()) {
                holder.mainUserId.setText(mActivity.getString(R.string.secret_key)
                        + " " + userIdSplit.name);
            } else {
                holder.mainUserId.setText(highlighter.highlight(userIdSplit.name));
            }
        } else {
            holder.mainUserId.setText(R.string.user_id_no_name);
        }

        // email
        if (userIdSplit.email != null) {
            holder.mainUserIdRest.setVisibility(View.VISIBLE);
            holder.mainUserIdRest.setText(highlighter.highlight(userIdSplit.email));
        } else {
            holder.mainUserIdRest.setVisibility(View.GONE);
        }

        holder.keyId.setText(KeyFormattingUtils.beautifyKeyIdWithPrefix(getContext(), entry.getKeyIdHex()));

        // don't show full fingerprint on key import
        holder.fingerprint.setVisibility(View.GONE);

        if (entry.getAlgorithm() != null) {
            holder.algorithm.setText(entry.getAlgorithm());
            holder.algorithm.setVisibility(View.VISIBLE);
        } else {
            holder.algorithm.setVisibility(View.GONE);
        }

        if (entry.isRevoked()) {
            KeyFormattingUtils.setStatusImage(getContext(), holder.status, null, State.REVOKED, R.color.bg_gray);
        } else if (entry.isExpired()) {
            KeyFormattingUtils.setStatusImage(getContext(), holder.status, null, State.EXPIRED, R.color.bg_gray);
        }

        if (entry.isRevoked() || entry.isExpired()) {
            holder.status.setVisibility(View.VISIBLE);

            // no more space for algorithm display
            holder.algorithm.setVisibility(View.GONE);

            holder.mainUserId.setTextColor(getContext().getResources().getColor(R.color.bg_gray));
            holder.mainUserIdRest.setTextColor(getContext().getResources().getColor(R.color.bg_gray));
            holder.keyId.setTextColor(getContext().getResources().getColor(R.color.bg_gray));
        } else {
            holder.status.setVisibility(View.GONE);
            holder.algorithm.setVisibility(View.VISIBLE);

            if (entry.isSecretKey()) {
                holder.mainUserId.setTextColor(Color.RED);
            } else {
                holder.mainUserId.setTextColor(Color.BLACK);
            }

            holder.mainUserIdRest.setTextColor(Color.BLACK);
            holder.keyId.setTextColor(Color.BLACK);
        }

        if (entry.getUserIds().size() == 1) {
            holder.userIdsList.setVisibility(View.GONE);
            holder.userIdsDivider.setVisibility(View.GONE);
        } else {
            holder.userIdsList.setVisibility(View.VISIBLE);
            holder.userIdsDivider.setVisibility(View.VISIBLE);

            // destroyLoader view from holder
            holder.userIdsList.removeAllViews();

            // we want conventional gpg UserIDs first, then Keybase ”proofs”
            HashMap<String, HashSet<String>> mergedUserIds = entry.getMergedUserIds();
            ArrayList<Map.Entry<String, HashSet<String>>> sortedIds = new ArrayList<Map.Entry<String, HashSet<String>>>(mergedUserIds.entrySet());
            java.util.Collections.sort(sortedIds, new java.util.Comparator<Map.Entry<String, HashSet<String>>>() {
                @Override
                public int compare(Map.Entry<String, HashSet<String>> entry1, Map.Entry<String, HashSet<String>> entry2) {

                    // sort keybase UserIds after non-Keybase
                    boolean e1IsKeybase = entry1.getKey().contains(":");
                    boolean e2IsKeybase = entry2.getKey().contains(":");
                    if (e1IsKeybase != e2IsKeybase) {
                        return (e1IsKeybase) ? 1 : -1;
                    }
                    return entry1.getKey().compareTo(entry2.getKey());
                }
            });

            for (Map.Entry<String, HashSet<String>> pair : sortedIds) {
                String cUserId = pair.getKey();
                HashSet<String> cEmails = pair.getValue();

                TextView uidView = (TextView) mInflater.inflate(
                        R.layout.import_keys_list_entry_user_id, null);
                uidView.setText(highlighter.highlight(cUserId));
                uidView.setPadding(0, 0, FormattingUtils.dpToPx(getContext(), 8), 0);

                if (entry.isRevoked() || entry.isExpired()) {
                    uidView.setTextColor(getContext().getResources().getColor(R.color.bg_gray));
                } else {
                    uidView.setTextColor(getContext().getResources().getColor(R.color.black));
                }

                holder.userIdsList.addView(uidView);

                for (String email : cEmails) {
                    TextView emailView = (TextView) mInflater.inflate(
                            R.layout.import_keys_list_entry_user_id, null);
                    emailView.setPadding(
                            FormattingUtils.dpToPx(getContext(), 16), 0,
                            FormattingUtils.dpToPx(getContext(), 8), 0);
                    emailView.setText(highlighter.highlight(email));

                    if (entry.isRevoked() || entry.isExpired()) {
                        emailView.setTextColor(getContext().getResources().getColor(R.color.bg_gray));
                    } else {
                        emailView.setTextColor(getContext().getResources().getColor(R.color.black));
                    }

                    holder.userIdsList.addView(emailView);
                }
            }
        }

        holder.checkBox.setChecked(entry.isSelected());

        return convertView;
    }

}
