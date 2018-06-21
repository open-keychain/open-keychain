package org.sufficientlysecure.keychain.ui.adapter;


import java.util.List;

import android.view.View;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.adapter.FlexibleKeyDummyItem.FlexibleKeyDummyViewHolder;
import org.sufficientlysecure.keychain.ui.adapter.FlexibleKeyItem.FlexibleSectionableKeyItem;


public class FlexibleKeyDummyItem extends FlexibleSectionableKeyItem<FlexibleKeyDummyViewHolder> {
    FlexibleKeyDummyItem(FlexibleKeyHeader header) {
        super(header);

        setSelectable(false);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.key_list_dummy;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public FlexibleKeyDummyViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new FlexibleKeyDummyViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, FlexibleKeyDummyViewHolder holder,
            int position, List<Object> payloads) {
    }

    class FlexibleKeyDummyViewHolder extends FlexibleViewHolder {
        private FlexibleKeyDummyViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter, true);
        }
    }
}
