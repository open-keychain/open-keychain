package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

import java.util.Date;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractSectionableItem;
import eu.davidea.flexibleadapter.items.IFilterable;
import eu.davidea.flexibleadapter.utils.Utils;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * KeyItem for FlexibleAdapter
 * Created by daquexian on 17-2-2.
 */

public class KeyItem extends AbstractSectionableItem<KeyItem.ViewHolder, KeyHeaderItem>
        implements IFilterable {

    private boolean mHasEncrypt;
    private byte[] mRawFingerPrint;
    private String mFingerPrint;
    private boolean mIsSecret;
    private boolean mIsVerified;
    private long mKeyId;
    private String mName;
    private String mEmail;
    private String mComment;
    private boolean mHasDuplicate;
    private boolean mIsRevoked;
    private boolean mIsExpired;
    private long mCreationTime;
    private Date mCreationDate;

    private KeyListListener mListener;

    public KeyItem(KeyHeaderItem headerItem, Cursor cursor, KeyListListener listener) {
        super(headerItem);

        mHasEncrypt = cursor.getInt(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.HAS_ENCRYPT)) != 0;
        mRawFingerPrint = cursor.getBlob(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.FINGERPRINT));
        mFingerPrint = KeyFormattingUtils.convertFingerprintToHex(mRawFingerPrint);
        mIsSecret = cursor.getInt(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.HAS_ANY_SECRET)) != 0;
        mIsVerified = cursor.getInt(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.VERIFIED)) > 0;

        mKeyId = cursor.getLong(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.MASTER_KEY_ID));
        mName = cursor.getString(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.NAME));
        mEmail = cursor.getString(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.EMAIL));
        mComment = cursor.getString(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.COMMENT));
        mHasDuplicate = cursor.getLong(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.HAS_DUPLICATE_USER_ID)) > 0L;
        mIsRevoked = cursor.getInt(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.IS_REVOKED)) > 0;
        mIsExpired = cursor.getInt(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.IS_EXPIRED)) > 0;
        mCreationTime = cursor.getLong(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.CREATION)) * 1000;
        mCreationDate = new Date(mCreationTime);

        mListener = listener;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.key_list_item;
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, ViewHolder holder, int position, List payloads) {
        holder.bindKey(adapter, this);
    }

    @Override
    public ViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater, ViewGroup parent) {
        return new ViewHolder(inflater.inflate(getLayoutRes(), parent, false), adapter, mListener);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof KeyItem && getKeyId() == ((KeyItem) o).getKeyId();
    }

    @Override
    public int hashCode() {
        return (int) (getKeyId() % (long) Integer.MAX_VALUE);
    }

    public static void setHeaders(Context context, List<KeyItem> keyItems) {
        String section = null;
        int secretKeyNum = 0;
        for (KeyItem keyItem : keyItems) {
            if (keyItem.isSecret()) {
                secretKeyNum++;
            }
        }
        KeyHeaderItem headerItem = KeyHeaderItem.getInstance(context,
                context.getResources().getQuantityString(R.plurals.n_keys, secretKeyNum, secretKeyNum), true);
        for (KeyItem keyItem : keyItems) {
            if (!keyItem.isSecret() && (section == null || !keyItem.getSection().equals(section))) {
                section = keyItem.getSection();
                headerItem = KeyHeaderItem.getInstance(context, section, false);
            }
            keyItem.setHeader(headerItem);
        }
    }

    public String getSection() {
        return getName().substring(0, 1).toUpperCase();
    }

    @Override
    public boolean filter(String constraint) {
        return mName.toUpperCase().contains(constraint.toUpperCase()) || mEmail.toUpperCase().contains(constraint.toUpperCase());
    }

    static final class ViewHolder extends FlexibleViewHolder implements View.OnClickListener, View.OnLongClickListener{
        public ViewHolder(View view, FlexibleAdapter adapter, KeyListListener listener) {
            super(view, adapter);
            mMainUserId = (TextView) itemView.findViewById(R.id.key_list_item_name);
            mMainUserIdRest = (TextView) itemView.findViewById(R.id.key_list_item_email);
            mStatus = (ImageView) itemView.findViewById(R.id.key_list_item_status_icon);
            mSlinger = itemView.findViewById(R.id.key_list_item_slinger_view);
            mSlingerButton = (ImageButton) itemView.findViewById(R.id.key_list_item_slinger_button);
            mCreationDate = (TextView) itemView.findViewById(R.id.key_list_item_creation);

            itemView.setClickable(true);
            itemView.setLongClickable(true);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            mSlingerButton.setClickable(true);
            mSlingerButton.setOnClickListener(this);

            mListener = listener;
        }
        private TextView mMainUserId;
        private TextView mMainUserIdRest;
        private TextView mCreationDate;
        private ImageView mStatus;
        private View mSlinger;
        private ImageButton mSlingerButton;

        private KeyListListener mListener;

        void bindKey(FlexibleAdapter adapter, KeyItem keyItem) {
            Context context = itemView.getContext();

            { // set name and stuff, common to both key types
                String name = keyItem.getName();
                String email = keyItem.getEmail();
                if (name != null) {
                    Utils.highlightText(mMainUserId, name, adapter.getSearchText());
                } else {
                    mMainUserId.setText(R.string.user_id_no_name);
                }
                if (email != null) {
                    Utils.highlightText(mMainUserIdRest, email, adapter.getSearchText());
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
        }

        @Override
        public void onClick(View v) {
            super.onClick(v);
            int pos = getFlexibleAdapterPosition();
            switch (v.getId()) {
                case R.id.key_list_item_slinger_button:
                    if (mListener != null) {
                        mListener.onSlingerButtonClicked(pos);
                    }
                    break;

                default:
                    mListener.onKeyItemClicked(pos);
                    break;
            }

        }
    }

    public boolean hasEncrypt() {
        return mHasEncrypt;
    }

    public byte[] getRawFingerPrint() {
        return mRawFingerPrint;
    }

    public String getFingerPrint() {
        return mFingerPrint;
    }

    public boolean isSecret() {
        return mIsSecret;
    }

    public boolean isVerified() {
        return mIsVerified;
    }

    public long getKeyId() {
        return mKeyId;
    }

    public String getName() {
        return mName;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getComment() {
        return mComment;
    }

    public boolean hasDuplicate() {
        return mHasDuplicate;
    }

    public boolean isRevoked() {
        return mIsRevoked;
    }

    public boolean isExpired() {
        return mIsExpired;
    }

    public long getCreationTime() {
        return mCreationTime;
    }

    public Date getCreationDate() {
        return mCreationDate;
    }

    public interface KeyListListener {
        void onKeyDummyItemClicked();
        void onKeyItemClicked(int position);
        void onSlingerButtonClicked(int position);
    }
}
