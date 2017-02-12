package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import android.view.View;

import org.sufficientlysecure.keychain.operations.results.OperationResult;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by daquexian on 17-2-11.
 */

public abstract class LogAbstractVH extends FlexibleViewHolder {
    LogAbstractVH(View view, FlexibleAdapter adapter) {
        super(view, adapter);
    }

    LogAbstractVH(View view, FlexibleAdapter adapter, boolean stickyHeader) {
        super(view, adapter, stickyHeader);
    }

    abstract void bind(OperationResult.LogEntryParcel entry);
}
