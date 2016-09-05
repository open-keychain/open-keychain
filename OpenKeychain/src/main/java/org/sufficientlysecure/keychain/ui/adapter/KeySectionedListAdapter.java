package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.adapter.*;
import org.sufficientlysecure.keychain.util.Log;

public class KeySectionedListAdapter extends SectionCursorAdapter<Character, SectionCursorAdapter.ViewHolder, KeySectionedListAdapter.KeyHeaderViewHolder> implements org.sufficientlysecure.keychain.ui.util.adapter.KeyAdapter {
    private static final short VIEW_ITEM_TYPE_KEY = 0x0;
    private static final short VIEW_ITEM_TYPE_DUMMY = 0x1;

    private static final short VIEW_SECTION_TYPE_PRIVATE = 0x0;
    private static final short VIEW_SECTION_TYPE_PUBLIC = 0x1;

    private String mQuery;
    private SparseBooleanArray mSelectionMap;
    private boolean mHasDummy = false;

    public KeySectionedListAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);

        mQuery = "";
        mSelectionMap = new SparseBooleanArray();
    }

    @Override
    public void setSearchQuery(String query) {
        mQuery = query;
    }

    @Override
    public boolean isEnabled(Cursor cursor) {
        return true;
    }

    @Override
    public KeyItem getItem(int position) {
        Cursor cursor = getCursor();

        if(cursor != null) {
            if(cursor.getPosition() != position) {
                moveCursor(position);
            }

            return new KeyItem(cursor);
        }

        return null;
    }

    @Override
    public long getMasterKeyId(int position) {
        return 0;
    }

    @Override
    public boolean isSecretAvailable(int position) {
        return false;
    }

    @Override
    public void onContentChanged() {
        mHasDummy = false;
        super.onContentChanged();
    }

    /**
     * Returns the number of database entries displayed.
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
    protected Character getSectionFromCursor(Cursor cursor) throws IllegalStateException {
        if (cursor.getInt(INDEX_HAS_ANY_SECRET) != 0) {
            if (cursor.getLong(INDEX_MASTER_KEY_ID) == 0L) {
                mHasDummy = true;
            }

            return '#';
        } else {
            String userId = cursor.getString(INDEX_USER_ID);
            if(TextUtils.isEmpty(userId)) {
                return '?';
            } else {
                return Character.toUpperCase(userId.charAt(0));
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
        if(moveCursor(position)) {
            boolean hasMaster = getCursor().getLong(INDEX_MASTER_KEY_ID) != 0L;
            boolean isSecret = getCursor().getInt(INDEX_HAS_ANY_SECRET) != 0;

            if (isSecret && !hasMaster) {
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
    protected void onBindItemViewHolder(ViewHolder holder, Cursor cursor) {
        if (holder.getItemViewTypeWithoutSections() == VIEW_ITEM_TYPE_KEY) {
            Highlighter highlighter = new Highlighter(getContext(), mQuery);
            ((KeyItemViewHolder) holder).bindKey(new KeyItem(cursor), highlighter);
        }
    }

    private static class KeyDummyViewHolder extends SectionCursorAdapter.ViewHolder
            implements View.OnClickListener{

        public KeyDummyViewHolder(View itemView) {
            super(itemView);

            itemView.setClickable(true);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

        }
    }

    private static class KeyItemViewHolder extends SectionCursorAdapter.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        private View mLayoutData;
        private Long mMasterKeyId;
        private TextView mMainUserId;
        private TextView mMainUserIdRest;
        private TextView mCreationDate;
        private ImageView mStatus;
        private View mSlinger;
        private ImageButton mSlingerButton;

        public KeyItemViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            mLayoutData = itemView.findViewById(R.id.key_list_item_data);
            mMainUserId = (TextView) itemView.findViewById(R.id.key_list_item_name);
            mMainUserIdRest = (TextView) itemView.findViewById(R.id.key_list_item_email);
            mStatus = (ImageView) itemView.findViewById(R.id.key_list_item_status_icon);
            mSlinger = itemView.findViewById(R.id.key_list_item_slinger_view);
            mSlingerButton = (ImageButton) itemView.findViewById(R.id.key_list_item_slinger_button);
            mCreationDate = (TextView) itemView.findViewById(R.id.key_list_item_creation);

            mSlingerButton.setOnClickListener(this);
        }

        public void bindKey(KeyItem keyItem, Highlighter highlighter) {
            Context ctx = itemView.getContext();

            { // set name and stuff, common to both key types
                OpenPgpUtils.UserId userIdSplit = keyItem.mUserId;
                if (userIdSplit.name != null) {
                    mMainUserId.setText(highlighter.highlight(userIdSplit.name));
                } else {
                    mMainUserId.setText(R.string.user_id_no_name);
                }
                if (userIdSplit.email != null) {
                    mMainUserIdRest.setText(highlighter.highlight(userIdSplit.email));
                    mMainUserIdRest.setVisibility(View.VISIBLE);
                } else {
                    mMainUserIdRest.setVisibility(View.GONE);
                }
            }

            // sort of a hack: if this item isn't enabled, we make it clickable
            // to intercept its click events. either way, no listener!
            itemView.setClickable(false);

            { // set edit button and status, specific by key type

                mMasterKeyId = keyItem.mKeyId;

                int textColor;

                // Note: order is important!
                if (keyItem.mIsRevoked) {
                    KeyFormattingUtils
                            .setStatusImage(ctx, mStatus, null, KeyFormattingUtils.State.REVOKED, R.color.key_flag_gray);
                    mStatus.setVisibility(View.VISIBLE);
                    mSlinger.setVisibility(View.GONE);
                    textColor = ContextCompat.getColor(ctx, R.color.key_flag_gray);
                } else if (keyItem.mIsExpired) {
                    KeyFormattingUtils.setStatusImage(ctx, mStatus, null, KeyFormattingUtils.State.EXPIRED, R.color.key_flag_gray);
                    mStatus.setVisibility(View.VISIBLE);
                    mSlinger.setVisibility(View.GONE);
                    textColor = ContextCompat.getColor(ctx, R.color.key_flag_gray);
                } else if (keyItem.mIsSecret) {
                    mStatus.setVisibility(View.GONE);
                    if (mSlingerButton.hasOnClickListeners()) {
                        mSlingerButton.setColorFilter(
                                FormattingUtils.getColorFromAttr(ctx, R.attr.colorTertiaryText),
                                PorterDuff.Mode.SRC_IN);
                        mSlinger.setVisibility(View.VISIBLE);
                    } else {
                        mSlinger.setVisibility(View.GONE);
                    }
                    textColor = FormattingUtils.getColorFromAttr(ctx, R.attr.colorText);
                } else {
                    // this is a public key - show if it's verified
                    if (keyItem.mIsVerified) {
                        KeyFormattingUtils.setStatusImage(ctx, mStatus, KeyFormattingUtils.State.VERIFIED);
                        mStatus.setVisibility(View.VISIBLE);
                    } else {
                        KeyFormattingUtils.setStatusImage(ctx, mStatus, KeyFormattingUtils.State.UNVERIFIED);
                        mStatus.setVisibility(View.VISIBLE);
                    }
                    mSlinger.setVisibility(View.GONE);
                    textColor = FormattingUtils.getColorFromAttr(ctx, R.attr.colorText);
                }

                mMainUserId.setTextColor(textColor);
                mMainUserIdRest.setTextColor(textColor);

                if (keyItem.mHasDuplicate) {
                    String dateTime = DateUtils.formatDateTime(ctx,
                            keyItem.mCreation.getTime(),
                            DateUtils.FORMAT_SHOW_DATE
                                    | DateUtils.FORMAT_SHOW_TIME
                                    | DateUtils.FORMAT_SHOW_YEAR
                                    | DateUtils.FORMAT_ABBREV_MONTH);
                    mCreationDate.setText(ctx.getString(R.string.label_key_created,
                            dateTime));
                    mCreationDate.setTextColor(textColor);
                    mCreationDate.setVisibility(View.VISIBLE);
                } else {
                    mCreationDate.setVisibility(View.GONE);
                }

            }
        }

        @Override
        public void onClick(View v) {

        }

        @Override
        public boolean onLongClick(View v) {
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
}
