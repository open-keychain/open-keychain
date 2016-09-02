package org.sufficientlysecure.keychain.ui.util.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

public class KeySectionedListAdapter extends SectionCursorAdapter<String, KeySectionedListAdapter.KeyItemViewHolder, KeySectionedListAdapter.KeyHeaderViewHolder> implements KeyAdapter {
    private String mQuery;
    private SparseBooleanArray mSelectionMap;


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
    protected String getSectionFromCursor(Cursor cursor) throws IllegalStateException {
        if (cursor.getInt(INDEX_HAS_ANY_SECRET) != 0) {
            return getContext().getString(R.string.my_keys);
        }

        String userId = cursor.getString(INDEX_USER_ID);
        String headerText = getContext().getString(R.string.user_id_no_name);

        if (userId != null && userId.length() > 0) {
            headerText = "" + userId.charAt(0);
        }

        return headerText;
    }

    @Override
    protected KeyHeaderViewHolder onCreateSectionViewHolder(ViewGroup parent) {
        return new KeyHeaderViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.key_list_header, parent, false));
    }

    @Override
    protected KeyItemViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
        return new KeyItemViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.key_list_item, parent, false));
    }

    @Override
    protected void onBindSectionViewHolder(KeyHeaderViewHolder holder, int sectionIndex, String section) {
        System.out.println("SIX: " + sectionIndex);
        if(sectionIndex == 0) {
            holder.bind(section, getCursor().getCount());
        } else {
            holder.bind(section);
        }
    }

    @Override
    protected void onBindItemViewHolder(KeyItemViewHolder holder, Cursor cursor) {
        boolean isSecret = cursor.getInt(INDEX_HAS_ANY_SECRET) != 0;
        long masterKeyId = cursor.getLong(INDEX_MASTER_KEY_ID);
        if (isSecret && masterKeyId == 0L) {
            holder.bindDummy();
        } else {
            Highlighter highlighter = new Highlighter(getContext(), mQuery);
            holder.bindKey(new KeyItem(cursor), highlighter);
        }
    }

    static class KeyItemViewHolder extends RecyclerView.ViewHolder {
        private View mLayoutDummy;
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

            mLayoutData = itemView.findViewById(R.id.key_list_item_data);
            mLayoutDummy = itemView.findViewById(R.id.key_list_item_dummy);
            mMainUserId = (TextView) itemView.findViewById(R.id.key_list_item_name);
            mMainUserIdRest = (TextView) itemView.findViewById(R.id.key_list_item_email);
            mStatus = (ImageView) itemView.findViewById(R.id.key_list_item_status_icon);
            mSlinger = itemView.findViewById(R.id.key_list_item_slinger_view);
            mSlingerButton = (ImageButton) itemView.findViewById(R.id.key_list_item_slinger_button);
            mCreationDate = (TextView) itemView.findViewById(R.id.key_list_item_creation);
        }

        public void bindKey(KeyItem keyItem, Highlighter highlighter) {
            Context ctx = itemView.getContext();

            mLayoutData.setVisibility(View.VISIBLE);
            mLayoutDummy.setVisibility(View.GONE);

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
                    textColor = ctx.getResources().getColor(R.color.key_flag_gray);
                } else if (keyItem.mIsExpired) {
                    KeyFormattingUtils.setStatusImage(ctx, mStatus, null, KeyFormattingUtils.State.EXPIRED, R.color.key_flag_gray);
                    mStatus.setVisibility(View.VISIBLE);
                    mSlinger.setVisibility(View.GONE);
                    textColor = ctx.getResources().getColor(R.color.key_flag_gray);
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

        public void bindDummy() {
            // just reset everything to display the dummy layout
            mLayoutDummy.setVisibility(View.VISIBLE);
            mLayoutData.setVisibility(View.GONE);
            mSlinger.setVisibility(View.GONE);
            mStatus.setVisibility(View.GONE);
            itemView.setClickable(false);
        }
    }

    static class KeyHeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView mHeaderText;
        private TextView mHeaderCount;

        public KeyHeaderViewHolder(View itemView) {
            super(itemView);

            mHeaderText = (TextView) itemView.findViewById(R.id.stickylist_header_text);
            mHeaderCount = (TextView) itemView.findViewById(R.id.contacts_num);
        }

        public void bind(String title, int count) {
            mHeaderText.setText(title);

            String contactsTotal = itemView.getResources()
                    .getQuantityString(R.plurals.n_keys, count, count);

            mHeaderCount.setText(contactsTotal);
            mHeaderCount.setVisibility(View.VISIBLE);

        }

        public void bind(String title) {
            mHeaderText.setText(title);
            mHeaderCount.setVisibility(View.GONE);
        }
    }
}
