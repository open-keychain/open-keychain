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

package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.futuremind.recyclerviewfastscroll.SectionTitleProvider;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.adapter.CursorAdapter;
import org.sufficientlysecure.keychain.ui.util.adapter.SectionCursorAdapter;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class KeySectionedListAdapter extends SectionCursorAdapter<KeySectionedListAdapter.KeyListCursor, Character,
        SectionCursorAdapter.ViewHolder, KeySectionedListAdapter.KeyHeaderViewHolder> implements SectionTitleProvider {

    private static final short VIEW_ITEM_TYPE_KEY = 0x0;
    private static final short VIEW_ITEM_TYPE_DUMMY = 0x1;

    private static final short VIEW_SECTION_TYPE_PRIVATE = 0x0;
    private static final short VIEW_SECTION_TYPE_PUBLIC = 0x1;

    private String mQuery;
    private List<Integer> mSelected;
    private KeyListListener mListener;

    private boolean mHasDummy = false;

    public KeySectionedListAdapter(Context context, Cursor cursor) {
        super(context, KeyListCursor.wrap(cursor, KeyListCursor.class), 0);

        mQuery = "";
        mSelected = new ArrayList<>();
    }

    public void setSearchQuery(String query) {
        mQuery = query;
    }


    @Override
    public void onContentChanged() {
        mHasDummy = false;
        mSelected.clear();

        if (mListener != null) {
            mListener.onSelectionStateChanged(0);
        }

        super.onContentChanged();
    }

    @Override
    public KeyListCursor swapCursor(KeyListCursor cursor) {
        if (cursor != null && (mQuery == null || TextUtils.isEmpty(mQuery))) {
            boolean isSecret = cursor.moveToFirst() && cursor.isSecret();

            if (!isSecret) {
                MatrixCursor headerCursor = new MatrixCursor(KeyListCursor.PROJECTION);
                Long[] row = new Long[KeyListCursor.PROJECTION.length];
                row[cursor.getColumnIndex(KeychainContract.KeyRings.HAS_ANY_SECRET)] = 1L;
                row[cursor.getColumnIndex(KeychainContract.KeyRings.MASTER_KEY_ID)] = 0L;
                headerCursor.addRow(row);

                Cursor[] toMerge = {
                        headerCursor,
                        cursor.getWrappedCursor()
                };

                cursor = KeyListCursor.wrap(new MergeCursor(toMerge));
            }
        }

        return super.swapCursor(cursor);
    }

    public void setKeyListener(KeyListListener listener) {
        mListener = listener;
    }

    private int getSelectedCount() {
        return mSelected.size();
    }

    private void selectPosition(int position) {
        mSelected.add(position);
        notifyItemChanged(position);
    }

    private void deselectPosition(int position) {
        mSelected.remove(Integer.valueOf(position));
        notifyItemChanged(position);
    }

    private boolean isSelected(int position) {
        return mSelected.contains(position);
    }

    public long[] getSelectedMasterKeyIds() {
        long[] keys = new long[mSelected.size()];
        for (int i = 0; i < keys.length; i++) {
            int index = getCursorPositionWithoutSections(mSelected.get(i));
            if (!moveCursor(index)) {
                return keys;
            }

            keys[i] = getIdFromCursor(getCursor());
        }

        return keys;
    }

    public boolean isAnySecretKeySelected() {
        for (int i = 0; i < mSelected.size(); i++) {
            int index = getCursorPositionWithoutSections(mSelected.get(i));
            if (!moveCursor(index)) {
                return false;
            }

            if (getCursor().isSecret()) {
                return true;
            }
        }

        return false;
    }


    /**
     * Returns the number of database entries displayed.
     *
     * @return The item count
     */
    public int getCount() {
        if (getCursor() != null) {
            return getCursor().getCount() - (mHasDummy ? 1 : 0);
        } else {
            return 0;
        }
    }

    @Override
    public long getIdFromCursor(KeyListCursor cursor) {
        return cursor.getKeyId();
    }

    @Override
    protected Character getSectionFromCursor(KeyListCursor cursor) throws IllegalStateException {
        if (cursor.isSecret()) {
            if (cursor.getKeyId() == 0L) {
                mHasDummy = true;
            }

            return '#';
        } else {
            String name = cursor.getName();
            if (name != null) {
                return Character.toUpperCase(name.charAt(0));
            } else {
                return '?';
            }
        }
    }

    @Override
    protected short getSectionHeaderViewType(int sectionIndex) {
        return (sectionIndex < 1) ?
                VIEW_SECTION_TYPE_PRIVATE :
                VIEW_SECTION_TYPE_PUBLIC;
    }

    @Override
    protected short getSectionItemViewType(int position) {
        if (moveCursor(position)) {
            KeyListCursor c = getCursor();

            if (c.isSecret() && c.getKeyId() == 0L) {
                return VIEW_ITEM_TYPE_DUMMY;
            }
        } else {
            Log.w(Constants.TAG, "Unable to determine key view type. "
                    + "Reason: Could not move cursor over dataset.");
        }

        return VIEW_ITEM_TYPE_KEY;
    }

    @Override
    protected KeyHeaderViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_SECTION_TYPE_PUBLIC:
                return new KeyHeaderViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.key_list_header_public, parent, false));

            case VIEW_SECTION_TYPE_PRIVATE:
                return new KeyHeaderViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.key_list_header_private, parent, false));

            default:
                return null;
        }
    }

    @Override
    protected ViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_ITEM_TYPE_KEY:
                return new KeyItemViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.key_list_item, parent, false));

            case VIEW_ITEM_TYPE_DUMMY:
                return new KeyDummyViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.key_list_dummy, parent, false));

            default:
                return null;
        }

    }

    @Override
    protected void onBindSectionViewHolder(KeyHeaderViewHolder holder, Character section) {
        switch (holder.getItemViewTypeWithoutSections()) {
            case VIEW_SECTION_TYPE_PUBLIC: {
                String title = section.equals('?') ?
                        getContext().getString(R.string.user_id_no_name) :
                        String.valueOf(section);

                holder.bind(title);
                break;
            }

            case VIEW_SECTION_TYPE_PRIVATE: {
                int count = getCount();
                String title = getContext().getResources()
                        .getQuantityString(R.plurals.n_keys, count, count);
                holder.bind(title);
                break;
            }

        }
    }

    @Override
    protected void onBindItemViewHolder(ViewHolder holder, KeyListCursor cursor) {
        if (holder.getItemViewTypeWithoutSections() == VIEW_ITEM_TYPE_KEY) {
            Highlighter highlighter = new Highlighter(getContext(), mQuery);
            ((KeyItemViewHolder) holder).bindKey(cursor, highlighter);
        }
    }

    public void finishSelection() {
        Integer[] selected = mSelected.toArray(
                new Integer[mSelected.size()]
        );

        mSelected.clear();

        for (Integer aSelected : selected) {
            notifyItemChanged(aSelected);
        }
    }

    @Override
    public String getSectionTitle(int position) {
        // this String will be shown in a bubble for specified position
        if (moveCursor(getCursorPositionWithoutSections(position))) {
            KeyListCursor cursor = getCursor();

            if (cursor.isSecret()) {
                if (cursor.getKeyId() == 0L) {
                    mHasDummy = true;
                }

                return "My";
            } else {
                String name = cursor.getName();
                if (name != null) {
                    return name.substring(0, 1).toUpperCase();
                } else {
                    return null;
                }
            }
        } else {
            Log.w(Constants.TAG, "Unable to determine section title. "
                    + "Reason: Could not move cursor over dataset.");
            return null;
        }
    }

    private class KeyDummyViewHolder extends SectionCursorAdapter.ViewHolder
            implements View.OnClickListener {

        KeyDummyViewHolder(View itemView) {
            super(itemView);

            itemView.setClickable(true);
            itemView.setOnClickListener(this);
            itemView.setEnabled(getSelectedCount() == 0);
        }

        @Override
        public void onClick(View view) {
            if (mListener != null) {
                mListener.onKeyDummyItemClicked();
            }
        }
    }

    public class KeyItemViewHolder extends SectionCursorAdapter.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        private final ViewGroup mTrustIdIcons;
        private final TextView mMainUserId;
        private final TextView mMainUserIdRest;
        private final TextView mCreationDate;
        private final ImageView mStatus;
        private final View mSlinger;
        private final ImageButton mSlingerButton;

        KeyItemViewHolder(View itemView) {
            super(itemView);

            mMainUserId = (TextView) itemView.findViewById(R.id.key_list_item_name);
            mMainUserIdRest = (TextView) itemView.findViewById(R.id.key_list_item_email);
            mStatus = (ImageView) itemView.findViewById(R.id.key_list_item_status_icon);
            mSlinger = itemView.findViewById(R.id.key_list_item_slinger_view);
            mSlingerButton = (ImageButton) itemView.findViewById(R.id.key_list_item_slinger_button);
            mCreationDate = (TextView) itemView.findViewById(R.id.key_list_item_creation);
            mTrustIdIcons = (ViewGroup) itemView.findViewById(R.id.key_list_item_tid_icon);

            itemView.setClickable(true);
            itemView.setLongClickable(true);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            mSlingerButton.setClickable(true);
            mSlingerButton.setOnClickListener(this);
        }

        void bindKey(KeyListCursor keyItem, Highlighter highlighter) {
            itemView.setSelected(isSelected(getAdapterPosition()));
            Context context = itemView.getContext();

            { // set name and stuff, common to both key types
                String name = keyItem.getName();
                String email = keyItem.getEmail();
                if (name != null) {
                    mMainUserId.setText(highlighter.highlight(name));
                } else {
                    mMainUserId.setText(R.string.user_id_no_name);
                }
                if (email != null) {
                    mMainUserIdRest.setText(highlighter.highlight(email));
                    mMainUserIdRest.setVisibility(View.VISIBLE);
                } else {
                    mMainUserIdRest.setVisibility(View.GONE);
                }
            }

            { // set edit button and status, specific by key type. Note: order is important!
                int textColor;
                if (keyItem.isRevoked()) {
                    KeyFormattingUtils.setStatusImage(
                            context,
                            mStatus,
                            null,
                            KeyFormattingUtils.State.REVOKED,
                            R.color.key_flag_gray
                    );

                    mStatus.setVisibility(View.VISIBLE);
                    mSlinger.setVisibility(View.GONE);
                    textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
                } else if (keyItem.isExpired()) {
                    KeyFormattingUtils.setStatusImage(
                            context,
                            mStatus,
                            null,
                            KeyFormattingUtils.State.EXPIRED,
                            R.color.key_flag_gray
                    );

                    mStatus.setVisibility(View.VISIBLE);
                    mSlinger.setVisibility(View.GONE);
                    textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
                } else if (!keyItem.isSecure()) {
                    KeyFormattingUtils.setStatusImage(
                            context,
                            mStatus,
                            null,
                            KeyFormattingUtils.State.INSECURE,
                            R.color.key_flag_gray
                    );

                    mStatus.setVisibility(View.VISIBLE);
                    mSlinger.setVisibility(View.GONE);
                    textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
                } else if (keyItem.isSecret()) {
                    mStatus.setVisibility(View.GONE);
                    if (mSlingerButton.hasOnClickListeners()) {
                        mSlingerButton.setColorFilter(
                                FormattingUtils.getColorFromAttr(context, R.attr.colorTertiaryText),
                                PorterDuff.Mode.SRC_IN
                        );

                        mSlinger.setVisibility(View.VISIBLE);
                    } else {
                        mSlinger.setVisibility(View.GONE);
                    }
                    textColor = FormattingUtils.getColorFromAttr(context, R.attr.colorText);
                } else {
                    // this is a public key - show if it's verified
                    if (keyItem.isVerified()) {
                        KeyFormattingUtils.setStatusImage(
                                context,
                                mStatus,
                                KeyFormattingUtils.State.VERIFIED
                        );

                        mStatus.setVisibility(View.VISIBLE);
                    } else {
                        KeyFormattingUtils.setStatusImage(
                                context,
                                mStatus,
                                KeyFormattingUtils.State.UNVERIFIED
                        );

                        mStatus.setVisibility(View.VISIBLE);
                    }
                    mSlinger.setVisibility(View.GONE);
                    textColor = FormattingUtils.getColorFromAttr(context, R.attr.colorText);
                }

                mMainUserId.setTextColor(textColor);
                mMainUserIdRest.setTextColor(textColor);

                if (keyItem.hasDuplicate()) {
                    String dateTime = DateUtils.formatDateTime(context,
                            keyItem.getCreationTime(),
                            DateUtils.FORMAT_SHOW_DATE
                                    | DateUtils.FORMAT_SHOW_TIME
                                    | DateUtils.FORMAT_SHOW_YEAR
                                    | DateUtils.FORMAT_ABBREV_MONTH);
                    mCreationDate.setText(context.getString(R.string.label_key_created,
                            dateTime));
                    mCreationDate.setTextColor(textColor);
                    mCreationDate.setVisibility(View.VISIBLE);
                } else {
                    mCreationDate.setVisibility(View.GONE);
                }
            }

            { // set icons
                List<String> packageNames = keyItem.getTrustIdPackages();

                LayoutInflater layoutInflater = LayoutInflater.from(getContext());
                while (mTrustIdIcons.getChildCount() < packageNames.size()) {
                    layoutInflater.inflate(R.layout.trust_id_icon, mTrustIdIcons, true);
                }

                int visibleIcons = 0;
                for (int i = 0; i < packageNames.size(); i++) {
                    ImageView imageView = (ImageView) mTrustIdIcons.getChildAt(i);
                    Drawable drawable = getDrawableForPackageName(packageNames.get(i));
                    if (drawable == null) {
                        continue;
                    }

                    imageView.setImageDrawable(drawable);
                    imageView.setVisibility(View.VISIBLE);
                    visibleIcons += 1;
                }
                for (int i = visibleIcons; i < mTrustIdIcons.getChildCount(); i++) {
                    mTrustIdIcons.getChildAt(i).setVisibility(View.GONE);
                }

            }
        }

        @Override
        public void onClick(View v) {
            int pos = getAdapterPosition();
            switch (v.getId()) {
                case R.id.key_list_item_slinger_button:
                    if (mListener != null) {
                        mListener.onSlingerButtonClicked(getItemId());
                    }
                    break;

                default:
                    if (getSelectedCount() == 0) {
                        if (mListener != null) {
                            mListener.onKeyItemClicked(getItemId());
                        }
                    } else {
                        if (isSelected(pos)) {
                            deselectPosition(pos);
                        } else {
                            selectPosition(pos);
                        }

                        if (mListener != null) {
                            mListener.onSelectionStateChanged(getSelectedCount());
                        }
                    }
                    break;
            }

        }

        @Override
        public boolean onLongClick(View v) {
            System.out.println("Long Click!");
            if (getSelectedCount() == 0) {
                selectPosition(getAdapterPosition());

                if (mListener != null) {
                    mListener.onSelectionStateChanged(getSelectedCount());
                }
                return true;
            }

            return false;
        }
    }

    static class KeyHeaderViewHolder extends SectionCursorAdapter.ViewHolder {
        private TextView mText1;

        public KeyHeaderViewHolder(View itemView) {
            super(itemView);
            mText1 = (TextView) itemView.findViewById(android.R.id.text1);
        }

        public void bind(String title) {
            mText1.setText(title);
        }
    }

    public static class KeyListCursor extends CursorAdapter.KeyCursor {
        public static final String ORDER = KeychainContract.KeyRings.HAS_ANY_SECRET
                + " DESC, " + KeychainContract.KeyRings.USER_ID + " COLLATE NOCASE ASC";

        public static final String[] PROJECTION;

        static {
            ArrayList<String> arr = new ArrayList<>();
            arr.addAll(Arrays.asList(KeyCursor.PROJECTION));
            arr.addAll(Arrays.asList(
                    KeychainContract.KeyRings.VERIFIED,
                    KeychainContract.KeyRings.HAS_ANY_SECRET,
                    KeychainContract.KeyRings.FINGERPRINT,
                    KeychainContract.KeyRings.HAS_ENCRYPT,
                    KeychainContract.KeyRings.API_KNOWN_TO_PACKAGE_NAMES
            ));

            PROJECTION = arr.toArray(new String[arr.size()]);
        }

        public static KeyListCursor wrap(Cursor cursor) {
            if (cursor != null) {
                return new KeyListCursor(cursor);
            } else {
                return null;
            }
        }

        private KeyListCursor(Cursor cursor) {
            super(cursor);
        }

        public boolean hasEncrypt() {
            int index = getColumnIndexOrThrow(KeychainContract.KeyRings.HAS_ENCRYPT);
            return getInt(index) != 0;
        }

        public byte[] getRawFingerprint() {
            int index = getColumnIndexOrThrow(KeychainContract.KeyRings.FINGERPRINT);
            return getBlob(index);
        }

        public String getFingerprint() {
            return KeyFormattingUtils.convertFingerprintToHex(getRawFingerprint());
        }

        public boolean isSecret() {
            int index = getColumnIndexOrThrow(KeychainContract.KeyRings.HAS_ANY_SECRET);
            return getInt(index) != 0;
        }

        public boolean isVerified() {
            int index = getColumnIndexOrThrow(KeychainContract.KeyRings.VERIFIED);
            return getInt(index) > 0;
        }

        public List<String> getTrustIdPackages() {
            int index = getColumnIndexOrThrow(KeyRings.API_KNOWN_TO_PACKAGE_NAMES);
            String packageNames = getString(index);
            if (packageNames == null) {
                return Collections.EMPTY_LIST;
            }
            return Arrays.asList(packageNames.split(","));
        }
    }

    public interface KeyListListener {
        void onKeyDummyItemClicked();

        void onKeyItemClicked(long masterKeyId);

        void onSlingerButtonClicked(long masterKeyId);

        void onSelectionStateChanged(int selectedCount);
    }

    private HashMap<String, Drawable> appIconCache = new HashMap<>();

    private Drawable getDrawableForPackageName(String packageName) {
        if (appIconCache.containsKey(packageName)) {
            return appIconCache.get(packageName);
        }

        PackageManager pm = getContext().getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);

            Drawable appIcon = pm.getApplicationIcon(ai);
            appIconCache.put(packageName, appIcon);

            return appIcon;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
