/*
 * Copyright (C) 2016 Tobias Erthal
 * Copyright (C) 2014-2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.remote.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.adapter.KeyCursorAdapter;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.adapter.CursorAdapter;


public class SelectIdentityKeyAdapter extends KeyCursorAdapter<CursorAdapter.KeyCursor, RecyclerView.ViewHolder> {
    private SelectSignKeyListener mListener;

    public SelectIdentityKeyAdapter(Context context, Cursor cursor) {
        super(context, KeyCursor.wrap(cursor));
    }

    public void setListener(SelectSignKeyListener listener) {
        mListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SignKeyItemHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.select_identity_key_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, KeyCursor cursor, String query) {
        ((SignKeyItemHolder) holder).bind(cursor, query);
    }

    private class SignKeyItemHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private TextView userIdText;
        private TextView creationText;
        private ImageView statusIcon;

        SignKeyItemHolder(View itemView) {
            super(itemView);
            itemView.setClickable(true);
            itemView.setOnClickListener(this);

            userIdText = (TextView) itemView.findViewById(R.id.select_key_item_name);
            creationText = (TextView) itemView.findViewById(R.id.select_key_item_creation);
            statusIcon = (ImageView) itemView.findViewById(R.id.select_key_item_status_icon);
        }

        public void bind(KeyCursor cursor, String query) {
            Context context = itemView.getContext();

            { // set name and stuff, common to both key types
                String name = cursor.getName();
                if (name != null) {
                    userIdText.setText(context.getString(R.string.use_key, name));
                } else {
                    String email = cursor.getEmail();
                    userIdText.setText(context.getString(R.string.use_key, email));
                }
            }

            { // set edit button and status, specific by key type. Note: order is important!
                int textColor;
                if (cursor.isRevoked()) {
                    KeyFormattingUtils.setStatusImage(
                            context,
                            statusIcon,
                            null,
                            KeyFormattingUtils.State.REVOKED,
                            R.color.key_flag_gray
                    );

                    itemView.setEnabled(false);
                    statusIcon.setVisibility(View.VISIBLE);
                    textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
                } else if (cursor.isExpired()) {
                    KeyFormattingUtils.setStatusImage(
                            context,
                            statusIcon,
                            null,
                            KeyFormattingUtils.State.EXPIRED,
                            R.color.key_flag_gray
                    );

                    itemView.setEnabled(false);
                    statusIcon.setVisibility(View.VISIBLE);
                    textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
                } else {
                    itemView.setEnabled(true);
                    statusIcon.setImageResource(R.drawable.ic_vpn_key_grey_24dp);
                    textColor = FormattingUtils.getColorFromAttr(context, R.attr.colorText);
                }

                userIdText.setTextColor(textColor);

                String dateTime = DateUtils.formatDateTime(context,
                        cursor.getCreationTime(),
                        DateUtils.FORMAT_SHOW_DATE
                                | DateUtils.FORMAT_SHOW_TIME
                                | DateUtils.FORMAT_SHOW_YEAR
                                | DateUtils.FORMAT_ABBREV_MONTH);
                creationText.setText(context.getString(R.string.label_key_created,
                        dateTime));
                creationText.setTextColor(textColor);
                creationText.setVisibility(View.VISIBLE);
            }

        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onSelectKeyItemClicked(getItemId());
            }
        }
    }

    public interface SelectSignKeyListener {
        void onSelectKeyItemClicked(long masterKeyId);
    }
}
