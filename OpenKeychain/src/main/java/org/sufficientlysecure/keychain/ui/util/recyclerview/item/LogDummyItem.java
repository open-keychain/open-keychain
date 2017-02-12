package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;

import eu.davidea.flexibleadapter.FlexibleAdapter;

/**
 * Created by daquexian on 17-2-11.
 * Force all headers to be shown
 */

public class LogDummyItem extends LogItem<LogDummyItem.LogDummyViewHolder> {
    public LogDummyItem(LogHeaderItem headerItem) {
        super(headerItem, null);
    }

    @Override
    public LogDummyViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater,
                                               ViewGroup parent) {
        return new LogDummyViewHolder(inflater.inflate(
                R.layout.log_display_dummy_item, parent, false), adapter);
    }

    static class LogDummyViewHolder extends LogAbstractVH {
        LogDummyViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
        }

        @Override
        void bind(OperationResult.LogEntryParcel entry) {

        }
    }
}
