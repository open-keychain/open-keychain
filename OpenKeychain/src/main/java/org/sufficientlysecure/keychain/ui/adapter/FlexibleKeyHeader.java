package org.sufficientlysecure.keychain.ui.adapter;


import java.util.List;

import android.view.View;
import android.widget.TextView;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.flexibleadapter.items.IHeader;
import eu.davidea.viewholders.FlexibleViewHolder;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.adapter.FlexibleKeyHeader.FlexibleHeaderViewHolder;


public class FlexibleKeyHeader extends FlexibleKeyItem<FlexibleHeaderViewHolder>
        implements IHeader<FlexibleHeaderViewHolder> {
    private final String sectionTitle;

    FlexibleKeyHeader(String sectionTitle) {
        super();
        this.sectionTitle = sectionTitle;
        setEnabled(false);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FlexibleKeyHeader) {
            FlexibleKeyHeader other = (FlexibleKeyHeader) o;
            return sectionTitle.equals(other.sectionTitle);
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.key_list_header_public;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    @Override
    public FlexibleHeaderViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new FlexibleHeaderViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, FlexibleHeaderViewHolder holder, int position,
            List<Object> payloads) {
        holder.text1.setText(sectionTitle);
    }

    static class FlexibleHeaderViewHolder extends FlexibleViewHolder {
        final TextView text1;

        FlexibleHeaderViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter, true);
            text1 = itemView.findViewById(android.R.id.text1);
        }


    }
}
