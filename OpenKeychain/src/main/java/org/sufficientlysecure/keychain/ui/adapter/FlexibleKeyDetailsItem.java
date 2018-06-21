package org.sufficientlysecure.keychain.ui.adapter;


import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFilterable;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.Key.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.ui.adapter.FlexibleKeyDetailsItem.FlexibleKeyItemViewHolder;
import org.sufficientlysecure.keychain.ui.adapter.FlexibleKeyItem.FlexibleSectionableKeyItem;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.PackageIconGetter;


public class FlexibleKeyDetailsItem extends FlexibleSectionableKeyItem<FlexibleKeyItemViewHolder>
        implements IFilterable<String> {
    public final UnifiedKeyInfo keyInfo;

    FlexibleKeyDetailsItem(UnifiedKeyInfo keyInfo, FlexibleKeyHeader header) {
        super(header);
        this.keyInfo = keyInfo;

        setSelectable(true);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.key_list_item;
    }

    @Override
    public FlexibleKeyItemViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new FlexibleKeyItemViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(
            FlexibleAdapter<IFlexible> adapter, FlexibleKeyItemViewHolder holder, int position, List<Object> payloads) {
        String highlightString = adapter.getFilter(String.class);
        holder.bind(keyInfo, highlightString);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof org.sufficientlysecure.keychain.ui.adapter.FlexibleKeyDetailsItem) {
            org.sufficientlysecure.keychain.ui.adapter.FlexibleKeyDetailsItem
                    other = (org.sufficientlysecure.keychain.ui.adapter.FlexibleKeyDetailsItem) o;
            return keyInfo.master_key_id() == other.keyInfo.master_key_id();
        }
        return false;
    }

    @Override
    public int hashCode() {
        long masterKeyId = keyInfo.master_key_id();
        return (int) (masterKeyId ^ (masterKeyId >>> 32));
    }

    @Override
    public boolean filter(String constraint) {
        String uidList = keyInfo.user_id_list();
        return constraint == null || (uidList != null && uidList.contains(constraint));
    }

    class FlexibleKeyItemViewHolder extends FlexibleViewHolder {
        private static final long JUST_NOW_THRESHOLD = DateUtils.MINUTE_IN_MILLIS * 5;

        private final TextView vMainUserId;
        private final TextView vMainUserIdRest;
        private final TextView vCreationDate;
        private final ImageView vStatusIcon;
        private final ImageView vTrustIdIcon;

        FlexibleKeyItemViewHolder(View itemView, FlexibleAdapter adapter) {
            super(itemView, adapter);

            vMainUserId = itemView.findViewById(R.id.key_list_item_name);
            vMainUserIdRest = itemView.findViewById(R.id.key_list_item_email);
            vStatusIcon = itemView.findViewById(R.id.key_list_item_status_icon);
            vCreationDate = itemView.findViewById(R.id.key_list_item_creation);
            vTrustIdIcon = itemView.findViewById(R.id.key_list_item_tid_icon);
        }

        public void bind(UnifiedKeyInfo keyInfo, String highlightString) {
            setEnabled(true);

            Context context = itemView.getContext();
            Highlighter highlighter = new Highlighter(context, highlightString);

            { // set name and stuff, common to both key types
                if (keyInfo.name() == null) {
                    if (keyInfo.email() != null) {
                        vMainUserId.setText(highlighter.highlight(keyInfo.email()));
                        vMainUserIdRest.setVisibility(View.GONE);
                    } else {
                        vMainUserId.setText(R.string.user_id_no_name);
                    }
                } else {
                    vMainUserId.setText(highlighter.highlight(keyInfo.name()));
                    // for some reason, this hangs for me
                    // FlexibleUtils.highlightText(vMainUserId, keyInfo.name(), highlightString);
                    if (keyInfo.email() != null) {
                        vMainUserIdRest.setText(highlighter.highlight(keyInfo.email()));
                        vMainUserIdRest.setVisibility(View.VISIBLE);
                    } else {
                        vMainUserIdRest.setVisibility(View.GONE);
                    }
                }
            }

            { // set edit button and status, specific by key type. Note: order is important!
                int textColor;
                if (keyInfo.is_revoked()) {
                    KeyFormattingUtils.setStatusImage(
                            context,
                            vStatusIcon,
                            null,
                            KeyFormattingUtils.State.REVOKED,
                            R.color.key_flag_gray
                    );

                    vStatusIcon.setVisibility(View.VISIBLE);
                    textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
                } else if (keyInfo.is_expired()) {
                    KeyFormattingUtils.setStatusImage(
                            context,
                            vStatusIcon,
                            null,
                            KeyFormattingUtils.State.EXPIRED,
                            R.color.key_flag_gray
                    );

                    vStatusIcon.setVisibility(View.VISIBLE);
                    textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
                } else if (!keyInfo.is_secure()) {
                    KeyFormattingUtils.setStatusImage(
                            context,
                            vStatusIcon,
                            null,
                            KeyFormattingUtils.State.INSECURE,
                            R.color.key_flag_gray
                    );

                    vStatusIcon.setVisibility(View.VISIBLE);
                    textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
                } else if (keyInfo.has_any_secret()) {
                    vStatusIcon.setVisibility(View.GONE);
                    textColor = FormattingUtils.getColorFromAttr(context, R.attr.colorText);
                } else {
                    // this is a public key - show if it's verified
                    if (keyInfo.is_verified()) {
                        KeyFormattingUtils.setStatusImage(
                                context,
                                vStatusIcon,
                                KeyFormattingUtils.State.VERIFIED
                        );

                        vStatusIcon.setVisibility(View.VISIBLE);
                    } else {
                        KeyFormattingUtils.setStatusImage(
                                context,
                                vStatusIcon,
                                KeyFormattingUtils.State.UNVERIFIED
                        );

                        vStatusIcon.setVisibility(View.VISIBLE);
                    }
                    textColor = FormattingUtils.getColorFromAttr(context, R.attr.colorText);
                }

                vMainUserId.setTextColor(textColor);
                vMainUserIdRest.setTextColor(textColor);

                if (keyInfo.has_duplicate() || keyInfo.has_any_secret()) {
                    vCreationDate.setText(getSecretKeyReadableTime(context, keyInfo));
                    vCreationDate.setTextColor(textColor);
                    vCreationDate.setVisibility(View.VISIBLE);
                } else {
                    vCreationDate.setVisibility(View.GONE);
                }
            }

            { // set icons

                if (!keyInfo.has_any_secret() && !keyInfo.autocrypt_package_names().isEmpty()) {
                    String packageName = keyInfo.autocrypt_package_names().get(0);
                    Drawable drawable = PackageIconGetter.getInstance(context).getDrawableForPackageName(packageName);
                    if (drawable != null) {
                        vTrustIdIcon.setImageDrawable(drawable);
                        vTrustIdIcon.setVisibility(View.VISIBLE);
                    } else {
                        vTrustIdIcon.setVisibility(View.GONE);
                    }
                } else {
                    vTrustIdIcon.setVisibility(View.GONE);
                }
            }
        }


        @NonNull
        private String getSecretKeyReadableTime(Context context, UnifiedKeyInfo keyInfo) {
            long creationMillis = keyInfo.creation() * 1000;

            boolean allowRelativeTimestamp = keyInfo.has_duplicate();
            if (allowRelativeTimestamp) {
                long creationAgeMillis = System.currentTimeMillis() - creationMillis;
                if (creationAgeMillis < JUST_NOW_THRESHOLD) {
                    return context.getString(R.string.label_key_created_just_now);
                }
            }

            String dateTime = DateUtils.formatDateTime(context,
                    creationMillis,
                    DateUtils.FORMAT_SHOW_DATE
                            | DateUtils.FORMAT_SHOW_TIME
                            | DateUtils.FORMAT_SHOW_YEAR
                            | DateUtils.FORMAT_ABBREV_MONTH);
            return context.getString(R.string.label_key_created, dateTime);
        }
    }

}
