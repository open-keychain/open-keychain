package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.operations.results.OperationResult;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by daquexian on 17-2-10.
 */

abstract class LogAbstractItem<VH extends LogAbstractVH> extends AbstractFlexibleItem<VH> {
    OperationResult.LogEntryParcel mEntry;

    LogAbstractItem(OperationResult.LogEntryParcel entry) {
        mEntry = entry;
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, VH holder, int position, List payloads) {
        holder.bind(mEntry);
    }

    @Override
    public boolean equals(Object obj) {
        if (this instanceof LogDummyItem || obj instanceof LogDummyItem) {
            return false;
        }
        return obj instanceof LogAbstractItem && ((LogAbstractItem) obj).mEntry.equals(mEntry);
    }

    @Override
    public int hashCode() {
        return mEntry.hashCode();
    }

    public OperationResult.LogEntryParcel getEntry() {
        return mEntry;
    }
}
