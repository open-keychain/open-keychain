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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ContactHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class UserIdsAddedAdapter extends ArrayAdapter<UserIdsAddedAdapter.UserIdModel> {
    private LayoutInflater mInflater;
    private Activity mActivity;

    private ArrayAdapter<String> mAutoCompleteNameAdapter;
    private ArrayAdapter<String> mAutoCompleteEmailAdapter;

    // hold a private reference to the underlying data List
    private List<UserIdModel> mData;

    public static class UserIdModel {
        String name = "";
        String address = "";
        String comment = "";

        @Override
        public String toString() {
            String userId = null;
            if (!TextUtils.isEmpty(name)) {
                userId = name;
                if (!TextUtils.isEmpty(comment)) {
                    userId += " (" + comment + ")";
                }
                if (!TextUtils.isEmpty(address)) {
                    userId += " <" + address + ">";
                }
            }
            return userId;
        }
    }

    public UserIdsAddedAdapter(Activity activity, List<UserIdModel> data) {
        super(activity, -1, data);
        mActivity = activity;
        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mData = data;
        mAutoCompleteNameAdapter = new ArrayAdapter<String>
                (mActivity, android.R.layout.simple_dropdown_item_1line,
                        ContactHelper.getPossibleUserNames(mActivity)
                );
        mAutoCompleteEmailAdapter = new ArrayAdapter<String>
                (mActivity, android.R.layout.simple_dropdown_item_1line,
                        ContactHelper.getPossibleUserEmails(mActivity)
                );
    }

    public List<String> getDataAsStringList() {
        ArrayList<String> out = new ArrayList<String>();
        for (UserIdModel id : mData) {
            out.add(id.toString());
        }

        return out;
    }

    static class ViewHolder {
        public AutoCompleteTextView vAddress;
        public AutoCompleteTextView vName;
        public EditText vComment;
        public ImageButton vDelete;
        // also hold a reference to the model item
        public UserIdModel mModel;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            // Not recycled, inflate a new view
            convertView = mInflater.inflate(R.layout.edit_key_user_id_added_item, null);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.vAddress = (AutoCompleteTextView) convertView.findViewById(R.id.user_id_added_item_address);
            viewHolder.vName = (AutoCompleteTextView) convertView.findViewById(R.id.user_id_added_item_name);
            viewHolder.vComment = (EditText) convertView.findViewById(R.id.user_id_added_item_comment);
            viewHolder.vDelete = (ImageButton) convertView.findViewById(R.id.user_id_added_item_delete);
            convertView.setTag(viewHolder);

            viewHolder.vAddress.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // update referenced item in view holder
                    viewHolder.mModel.address = s.toString();

                    // show icon on valid email addresses
                    if (viewHolder.mModel.address.length() > 0) {
                        Matcher emailMatcher = Patterns.EMAIL_ADDRESS.matcher(viewHolder.mModel.address);
                        if (emailMatcher.matches()) {
                            viewHolder.vAddress.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                    R.drawable.uid_mail_ok, 0);
                        } else {
                            viewHolder.vAddress.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                    R.drawable.uid_mail_bad, 0);
                        }
                    } else {
                        // remove drawable if email is empty
                        viewHolder.vAddress.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    }
                }
            });

            viewHolder.vName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // update referenced item in view holder
                    viewHolder.mModel.name = s.toString();
                }
            });

            viewHolder.vComment.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // update referenced item in view holder
                    viewHolder.mModel.comment = s.toString();
                }
            });

            viewHolder.vDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // remove reference model item from adapter (data and notify about change)
                    UserIdsAddedAdapter.this.remove(viewHolder.mModel);
                }
            });

        }
        final ViewHolder holder = (ViewHolder) convertView.getTag();

        // save reference to model item
        holder.mModel = getItem(position);

        holder.vAddress.setText(holder.mModel.address);
        holder.vAddress.setThreshold(1); // Start working from first character
        holder.vAddress.setAdapter(mAutoCompleteEmailAdapter);

        holder.vName.setText(holder.mModel.name);
        holder.vName.setThreshold(1); // Start working from first character
        holder.vName.setAdapter(mAutoCompleteNameAdapter);

        holder.vComment.setText(holder.mModel.comment);

        return convertView;
    }

}
