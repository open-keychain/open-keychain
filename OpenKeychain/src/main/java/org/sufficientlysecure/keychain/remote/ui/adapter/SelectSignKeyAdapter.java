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

public class SelectSignKeyAdapter extends KeyCursorAdapter<CursorAdapter.KeyCursor, RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_KEY = 0;
    private static final int VIEW_TYPE_DUMMY = 1;

    private SelectSignKeyListener mListener;

    public SelectSignKeyAdapter(Context context, Cursor cursor) {
        super(context, KeyCursor.wrap(cursor));
    }

    public void setListener(SelectSignKeyListener listener) {
        mListener = listener;
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + 1; // received items + 1 dummy key
    }

    @Override
    public long getItemId(int pos) {
        if (pos < super.getItemCount()) {
            return super.getItemId(pos);
        } else {
            return RecyclerView.NO_ID;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == super.getItemCount() ?
                VIEW_TYPE_DUMMY : VIEW_TYPE_KEY;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_KEY:
                return new SignKeyItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.select_sign_key_item, parent, false));

            case VIEW_TYPE_DUMMY:
                return new DummyViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.select_dummy_key_item, parent, false));

            default:
                return null;
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_KEY) {
            super.onBindViewHolder(holder, position);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, KeyCursor cursor, String query) {
        ((SignKeyItemHolder) holder).bind(cursor, query);
    }

    private class DummyViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public DummyViewHolder(View itemView) {
            super(itemView);
            itemView.setClickable(true);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onCreateKeyDummyClicked();
            }
        }
    }

    private class SignKeyItemHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private TextView mUserIdText;
        private TextView mUserIdRestText;
        private TextView mCreationText;
        private ImageView mStatusIcon;

        public SignKeyItemHolder(View itemView) {
            super(itemView);
            itemView.setClickable(true);
            itemView.setOnClickListener(this);

            mUserIdText = (TextView) itemView.findViewById(R.id.select_key_item_name);
            mUserIdRestText = (TextView) itemView.findViewById(R.id.select_key_item_email);
            mCreationText = (TextView) itemView.findViewById(R.id.select_key_item_creation);
            mStatusIcon = (ImageView) itemView.findViewById(R.id.select_key_item_status_icon);
        }

        public void bind(KeyCursor cursor, String query) {
            Highlighter highlighter = new Highlighter(itemView.getContext(), query);
            Context context = itemView.getContext();

            { // set name and stuff, common to both key types
                OpenPgpUtils.UserId userIdSplit = cursor.getUserId();
                if (userIdSplit.name != null) {
                    mUserIdText.setText(highlighter.highlight(userIdSplit.name));
                } else {
                    mUserIdText.setText(R.string.user_id_no_name);
                }
                if (userIdSplit.email != null) {
                    mUserIdRestText.setText(highlighter.highlight(userIdSplit.email));
                    mUserIdRestText.setVisibility(View.VISIBLE);
                } else {
                    mUserIdRestText.setVisibility(View.GONE);
                }
            }

            { // set edit button and status, specific by key type. Note: order is important!
                int textColor;
                if (cursor.isRevoked()) {
                    KeyFormattingUtils.setStatusImage(
                            context,
                            mStatusIcon,
                            null,
                            KeyFormattingUtils.State.REVOKED,
                            R.color.key_flag_gray
                    );

                    itemView.setEnabled(false);
                    mStatusIcon.setVisibility(View.VISIBLE);
                    textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
                } else if (cursor.isExpired()) {
                    KeyFormattingUtils.setStatusImage(
                            context,
                            mStatusIcon,
                            null,
                            KeyFormattingUtils.State.EXPIRED,
                            R.color.key_flag_gray
                    );

                    itemView.setEnabled(false);
                    mStatusIcon.setVisibility(View.VISIBLE);
                    textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
                } else {
                    itemView.setEnabled(true);
                    mStatusIcon.setVisibility(View.GONE);
                    textColor = FormattingUtils.getColorFromAttr(context, R.attr.colorText);
                }

                mUserIdText.setTextColor(textColor);
                mUserIdRestText.setTextColor(textColor);

                if (cursor.hasDuplicate()) {
                    String dateTime = DateUtils.formatDateTime(context,
                            cursor.getCreationTime(),
                            DateUtils.FORMAT_SHOW_DATE
                                    | DateUtils.FORMAT_SHOW_TIME
                                    | DateUtils.FORMAT_SHOW_YEAR
                                    | DateUtils.FORMAT_ABBREV_MONTH);
                    mCreationText.setText(context.getString(R.string.label_key_created,
                            dateTime));
                    mCreationText.setTextColor(textColor);
                    mCreationText.setVisibility(View.VISIBLE);
                } else {
                    mCreationText.setVisibility(View.GONE);
                }
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
        void onCreateKeyDummyClicked();

        void onSelectKeyItemClicked(long masterKeyId);
    }
}
