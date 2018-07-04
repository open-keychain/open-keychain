package org.sufficientlysecure.keychain.ui.adapter;


import java.util.Arrays;
import java.util.List;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFilterable;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.ui.adapter.FlexibleKeyDetailsItem.FlexibleKeyItemViewHolder;
import org.sufficientlysecure.keychain.ui.adapter.FlexibleKeyItem.FlexibleSectionableKeyItem;
import org.sufficientlysecure.keychain.ui.util.KeyInfoFormatter;


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
        return constraint == null || keyInfo.uidSearchString().contains(constraint);
    }

    class FlexibleKeyItemViewHolder extends FlexibleViewHolder {
        private final TextView vMainUserId;
        private final TextView vMainUserIdRest;
        private final TextView vCreationDate;
        private final ImageView vStatusIcon;
        private final ImageView vTrustIdIcon;
        private final KeyInfoFormatter keyInfoFormatter;

        FlexibleKeyItemViewHolder(View itemView, FlexibleAdapter adapter) {
            super(itemView, adapter);

            vMainUserId = itemView.findViewById(R.id.key_list_item_name);
            vMainUserIdRest = itemView.findViewById(R.id.key_list_item_email);
            vStatusIcon = itemView.findViewById(R.id.key_list_item_status_icon);
            vCreationDate = itemView.findViewById(R.id.key_list_item_creation);
            vTrustIdIcon = itemView.findViewById(R.id.key_list_item_tid_icon);

            keyInfoFormatter = new KeyInfoFormatter(itemView.getContext());
        }

        public void bind(UnifiedKeyInfo keyInfo, String highlightString) {
            setEnabled(true);

            keyInfoFormatter.setKeyInfo(keyInfo);
            keyInfoFormatter.setHighlightString(highlightString);
            keyInfoFormatter.formatUserId(vMainUserId, vMainUserIdRest);
            keyInfoFormatter.formatCreationDate(vCreationDate);
            keyInfoFormatter.greyInvalidKeys(Arrays.asList(vMainUserId, vMainUserIdRest, vCreationDate));
            keyInfoFormatter.formatStatusIcon(vStatusIcon);
            keyInfoFormatter.formatTrustIcon(vTrustIdIcon);
        }

    }

}
