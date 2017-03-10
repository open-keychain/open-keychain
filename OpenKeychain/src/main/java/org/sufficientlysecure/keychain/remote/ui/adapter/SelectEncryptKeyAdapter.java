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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.adapter.KeyCursorAdapter;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.adapter.CursorAdapter;

import java.util.ArrayList;
import java.util.Arrays;

public class SelectEncryptKeyAdapter extends KeyCursorAdapter<SelectEncryptKeyAdapter.PublicKeyCursor,
        SelectEncryptKeyAdapter.EncryptKeyItemHolder> {

    private ArrayList<Integer> mSelected;

    public SelectEncryptKeyAdapter(Context context, PublicKeyCursor cursor) {
        super(context, cursor);

        mSelected = new ArrayList<>();
    }

    private boolean isSelected(int position) {
        return mSelected.contains(position);
    }

    private void select(int position) {
        if (!isSelected(position)) {
            mSelected.add(position);
            notifyItemChanged(position);
        }
    }

    private void switchSelected(int position) {
        int index = mSelected.indexOf(position);
        if (index < 0) {
            mSelected.add(position);
        } else {
            mSelected.remove(index);
        }

        notifyItemChanged(position);
    }

    public long[] getMasterKeyIds() {
        long[] selected = new long[mSelected.size()];
        for (int i = 0; i < selected.length; i++) {
            int position = mSelected.get(i);
            if (!moveCursor(position)) {
                return selected;
            }

            selected[i] = getCursor().getKeyId();
        }

        return selected;
    }

    public void preselectMasterKeyIds(long[] keyIds) {
        if (keyIds != null) {
            int count = 0;
            for (int i = 0; i < getItemCount(); i++) {
                if (!moveCursor(i)) {
                    continue;
                }

                long id = getCursor().getKeyId();
                for (long keyId : keyIds) {
                    if (id == keyId) {
                        select(i);
                        count++;
                        break;
                    }
                }

                if (count >= keyIds.length) {
                    return;
                }
            }
        }
    }

    @Override
    public void onContentChanged() {
        mSelected.clear();
        super.onContentChanged();
    }

    @Override
    public void onBindViewHolder(EncryptKeyItemHolder holder, PublicKeyCursor cursor, String query) {
        holder.bind(cursor, query, isSelected(holder.getAdapterPosition()));
    }

    @Override
    public EncryptKeyItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new EncryptKeyItemHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.select_encrypt_key_item, parent, false));
    }

    class EncryptKeyItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mUserIdText;
        private TextView mUserIdRestText;
        private TextView mCreationText;
        private ImageView mStatusIcon;
        private CheckBox mChecked;

        public EncryptKeyItemHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            mUserIdText = (TextView) itemView.findViewById(R.id.select_key_item_name);
            mUserIdRestText = (TextView) itemView.findViewById(R.id.select_key_item_email);
            mCreationText = (TextView) itemView.findViewById(R.id.select_key_item_creation);
            mStatusIcon = (ImageView) itemView.findViewById(R.id.select_key_item_status_icon);
            mChecked = (CheckBox) itemView.findViewById(R.id.selected);
        }

        public void bind(PublicKeyCursor cursor, String query, boolean selected) {
            Highlighter highlighter = new Highlighter(itemView.getContext(), query);
            Context context = itemView.getContext();

            { // set name and stuff, common to both key types
                String name = cursor.getName();
                String email = cursor.getEmail();
                if (name != null) {
                    mUserIdText.setText(highlighter.highlight(name));
                } else {
                    mUserIdText.setText(R.string.user_id_no_name);
                }
                if (email != null) {
                    mUserIdRestText.setText(highlighter.highlight(email));
                    mUserIdRestText.setVisibility(View.VISIBLE);
                } else {
                    mUserIdRestText.setVisibility(View.GONE);
                }
            }

            boolean enabled;
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

                    mStatusIcon.setVisibility(View.VISIBLE);

                    enabled = false;
                    textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
                } else if (cursor.isExpired()) {
                    KeyFormattingUtils.setStatusImage(
                            context,
                            mStatusIcon,
                            null,
                            KeyFormattingUtils.State.EXPIRED,
                            R.color.key_flag_gray
                    );

                    mStatusIcon.setVisibility(View.VISIBLE);

                    enabled = false;
                    textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
                } else if (!cursor.hasEncrypt()) {
                    KeyFormattingUtils.setStatusImage(
                            context,
                            mStatusIcon,
                            KeyFormattingUtils.State.UNAVAILABLE
                    );

                    mStatusIcon.setVisibility(View.VISIBLE);

                    enabled = false;
                    textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
                } else if (cursor.isVerified()) {
                    KeyFormattingUtils.setStatusImage(
                            context,
                            mStatusIcon,
                            KeyFormattingUtils.State.VERIFIED
                    );

                    mStatusIcon.setVisibility(View.VISIBLE);

                    enabled = true;
                    textColor = FormattingUtils.getColorFromAttr(context, R.attr.colorText);
                } else {
                    KeyFormattingUtils.setStatusImage(
                            context,
                            mStatusIcon,
                            KeyFormattingUtils.State.UNVERIFIED
                    );

                    mStatusIcon.setVisibility(View.VISIBLE);

                    enabled = true;
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

            itemView.setEnabled(enabled);
            itemView.setClickable(enabled);
            mChecked.setChecked(enabled && selected);

        }

        @Override
        public void onClick(View v) {
            switchSelected(getAdapterPosition());
        }
    }

    public static class PublicKeyCursor extends CursorAdapter.KeyCursor {
        public static final String[] PROJECTION;

        static {
            ArrayList<String> arr = new ArrayList<>();
            arr.addAll(Arrays.asList(KeyCursor.PROJECTION));
            arr.addAll(Arrays.asList(
                    KeychainContract.KeyRings.HAS_ENCRYPT,
                    KeychainContract.KeyRings.VERIFIED
            ));

            PROJECTION = arr.toArray(new String[arr.size()]);
        }

        public static PublicKeyCursor wrap(Cursor cursor) {
            if (cursor != null) {
                return new PublicKeyCursor(cursor);
            } else {
                return null;
            }
        }

        private PublicKeyCursor(Cursor cursor) {
            super(cursor);
        }

        public boolean hasEncrypt() {
            int index = getColumnIndexOrThrow(KeychainContract.KeyRings.HAS_ENCRYPT);
            return getInt(index) != 0;
        }

        public boolean isVerified() {
            int index = getColumnIndexOrThrow(KeychainContract.KeyRings.VERIFIED);
            return getInt(index) != 0;
        }
    }
}
