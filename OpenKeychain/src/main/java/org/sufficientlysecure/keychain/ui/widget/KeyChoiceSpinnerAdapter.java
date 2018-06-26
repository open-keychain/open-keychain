package org.sufficientlysecure.keychain.ui.widget;


import java.util.List;

import android.content.Context;
import android.support.annotation.StringRes;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;


class KeyChoiceSpinnerAdapter extends BaseAdapter {
    private Integer noneItemString;
    private List<UnifiedKeyInfo> data;
    private final LayoutInflater layoutInflater;

    KeyChoiceSpinnerAdapter(Context context) {
        super();

        layoutInflater = LayoutInflater.from(context);
    }

    public void setData(List<UnifiedKeyInfo> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public void setNoneItemString(@StringRes Integer noneItemString) {
        this.noneItemString = noneItemString;
        notifyDataSetChanged();
    }

    public boolean hasNoneItem() {
        return noneItemString != null;
    }

    @Override
    public int getCount() {
        return (data != null ? data.size() : 0) + (noneItemString != null ? 1 : 0);
    }

    public boolean isSingleEntry() {
        return data != null && data.size() == 1;
    }

    @Override
    public UnifiedKeyInfo getItem(int position) {
        if (noneItemString != null) {
            if (position == 0) {
                return null;
            }
            position -= 1;
        }
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (noneItemString != null) {
            if (position == 0) {
                return 0;
            }
            position -= 1;
        }
        return data != null ? data.get(position).master_key_id() : 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getItemViewType(int position) {
        if (noneItemString != null && position == 0) {
            return 1;
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (noneItemString != null) {
            if (position == 0) {
                if (convertView != null && convertView.getTag() == null) {
                    return convertView;
                } else {
                    View view = layoutInflater.inflate(R.layout.keyspinner_item_none, parent, false);
                    view.<TextView>findViewById(R.id.keyspinner_key_name).setText(noneItemString);
                    return view;
                }
            }
        }

        View view;
        KeyChoiceViewHolder viewHolder;
        if (convertView == null || !(convertView.getTag() instanceof KeyChoiceViewHolder)) {
            view = layoutInflater.inflate(R.layout.key_list_item, parent, false);
            viewHolder = new KeyChoiceViewHolder(view);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = ((KeyChoiceViewHolder) view.getTag());
        }

        UnifiedKeyInfo keyInfo = getItem(position);
        viewHolder.bind(view.getContext(), keyInfo, isEnabled(position));

        return view;
    }

    public static class KeyChoiceViewHolder {
        private View mView;
        private TextView mMainUserId;
        private TextView mMainUserIdRest;
        private TextView mCreationDate;
        private ImageView mStatus;

        KeyChoiceViewHolder(View view) {
            mView = view;
            mMainUserId = view.findViewById(R.id.key_list_item_name);
            mMainUserIdRest = view.findViewById(R.id.key_list_item_email);
            mStatus = view.findViewById(R.id.key_list_item_status_icon);
            mCreationDate = view.findViewById(R.id.key_list_item_creation);
        }

        public void bind(Context context, UnifiedKeyInfo keyInfo, boolean enabled) {
            { // set name and stuff, common to both key types
                if (keyInfo.name() != null) {
                    mMainUserId.setText(keyInfo.name());
                } else {
                    mMainUserId.setText(R.string.user_id_no_name);
                }
                if (keyInfo.email() != null) {
                    mMainUserIdRest.setText(keyInfo.email());
                    mMainUserIdRest.setVisibility(View.VISIBLE);
                } else {
                    mMainUserIdRest.setVisibility(View.GONE);
                }
            }

            // sort of a hack: if this item isn't enabled, we make it clickable
            // to intercept its click events. either way, no listener!
            mView.setClickable(!enabled);

            { // set edit button and status, specific by key type

                int textColor;

                // Note: order is important!
                if (keyInfo.is_revoked()) {
                    KeyFormattingUtils
                            .setStatusImage(context, mStatus, null, State.REVOKED, R.color.key_flag_gray);
                    mStatus.setVisibility(View.VISIBLE);
                    textColor = context.getResources().getColor(R.color.key_flag_gray);
                } else if (keyInfo.is_expired()) {
                    KeyFormattingUtils.setStatusImage(context, mStatus, null, State.EXPIRED, R.color.key_flag_gray);
                    mStatus.setVisibility(View.VISIBLE);
                    textColor = context.getResources().getColor(R.color.key_flag_gray);
                } else if (!keyInfo.is_secure()) {
                    KeyFormattingUtils.setStatusImage(context, mStatus, null, State.INSECURE, R.color.key_flag_gray);
                    mStatus.setVisibility(View.VISIBLE);
                    textColor = context.getResources().getColor(R.color.key_flag_gray);
                } else if (keyInfo.has_any_secret()) {
                    mStatus.setVisibility(View.GONE);
                    textColor = FormattingUtils.getColorFromAttr(context, R.attr.colorText);
                } else {
                    // this is a public key - show if it's verified
                    if (keyInfo.is_verified()) {
                        KeyFormattingUtils.setStatusImage(context, mStatus, State.VERIFIED);
                        mStatus.setVisibility(View.VISIBLE);
                    } else {
                        KeyFormattingUtils.setStatusImage(context, mStatus, State.UNVERIFIED);
                        mStatus.setVisibility(View.VISIBLE);
                    }
                    textColor = FormattingUtils.getColorFromAttr(context, R.attr.colorText);
                }

                mMainUserId.setTextColor(textColor);
                mMainUserIdRest.setTextColor(textColor);

                if (keyInfo.has_duplicate()) {
                    String dateTime = DateUtils.formatDateTime(context,
                            keyInfo.creation() * 1000,
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
    }
}
