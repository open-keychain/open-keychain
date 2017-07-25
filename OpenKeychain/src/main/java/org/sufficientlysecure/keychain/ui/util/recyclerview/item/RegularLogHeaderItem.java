package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;

import eu.davidea.flexibleadapter.FlexibleAdapter;

/**
 * Created by daquexian on 17-2-10.
 */

public class RegularLogHeaderItem extends LogHeaderItem<RegularLogItem.RegularLogViewHolder> {
    public RegularLogHeaderItem(OperationResult.LogEntryParcel entry) {
        super(entry);
    }

    @Override
    public RegularLogItem.RegularLogViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater, ViewGroup parent) {
        return new RegularLogItem.RegularLogViewHolder(
                inflater.inflate(R.layout.log_display_regular_item, parent, false), adapter, true);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.log_display_regular_item;
    }
}
