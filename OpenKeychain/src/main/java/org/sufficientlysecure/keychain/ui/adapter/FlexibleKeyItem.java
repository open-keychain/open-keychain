package org.sufficientlysecure.keychain.ui.adapter;


import android.support.v7.widget.RecyclerView;

import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.ISectionable;


public abstract class FlexibleKeyItem<VH extends RecyclerView.ViewHolder> extends AbstractFlexibleItem<VH> {

    public static abstract class FlexibleSectionableKeyItem<VH extends RecyclerView.ViewHolder>
        extends FlexibleKeyItem<VH> implements ISectionable<VH, FlexibleKeyHeader> {
        FlexibleKeyHeader header;

        FlexibleSectionableKeyItem(FlexibleKeyHeader header) {
            this.header = header;
        }

        @Override
        public FlexibleKeyHeader getHeader() {
            return header;
        }

        @Override
        public void setHeader(FlexibleKeyHeader header) {
            this.header = header;
        }
    }
}
