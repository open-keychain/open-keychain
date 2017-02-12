package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;

import eu.davidea.flexibleadapter.FlexibleAdapter;

/**
 * Created by daquexian on 17-2-10.
 */

public class SublogHeaderItem extends LogHeaderItem<SublogItem.SublogViewHolder> {
    public SublogHeaderItem(OperationResult.LogEntryParcel entry) {
        super(entry);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.log_display_sublog_item;
    }

    @Override
    public SublogItem.SublogViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater, ViewGroup parent) {
        return new SublogItem.SublogViewHolder(inflater.inflate(getLayoutRes(), parent, false), adapter, true);
    }
}
