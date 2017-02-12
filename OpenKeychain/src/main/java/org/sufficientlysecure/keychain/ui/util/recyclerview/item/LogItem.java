package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import org.sufficientlysecure.keychain.operations.results.OperationResult;

import eu.davidea.flexibleadapter.items.AbstractSectionableItem;
import eu.davidea.flexibleadapter.items.ISectionable;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by daquexian on 17-2-10.
 */

public abstract class LogItem<T extends LogAbstractVH> extends LogAbstractItem<T> implements ISectionable<T, LogHeaderItem> {
    LogHeaderItem mHeader;

    public LogItem(LogHeaderItem headerItem, OperationResult.LogEntryParcel entry) {
        super(entry);

        setHeader(headerItem);
    }

    @Override
    public void setHeader(LogHeaderItem header) {
        mHeader = header;
    }

    @Override
    public LogHeaderItem getHeader() {
        return mHeader;
    }

}
