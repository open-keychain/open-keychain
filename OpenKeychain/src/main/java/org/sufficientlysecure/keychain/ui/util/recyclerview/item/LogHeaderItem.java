package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import org.sufficientlysecure.keychain.operations.results.OperationResult;

import eu.davidea.flexibleadapter.items.IHeader;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by daquexian on 17-2-10.
 */

public abstract class LogHeaderItem<VH extends LogAbstractVH> extends LogAbstractItem<VH> implements IHeader<VH> {
    LogHeaderItem(OperationResult.LogEntryParcel entry) {
        super(entry);
    }
}
