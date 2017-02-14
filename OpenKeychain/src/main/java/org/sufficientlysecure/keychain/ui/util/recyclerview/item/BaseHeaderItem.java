package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import java.util.ArrayList;
import java.util.List;

import eu.davidea.flexibleadapter.items.AbstractHeaderItem;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Superclass of CertHeaderItem and KeyHeaderItem
 * LogHeaderItem doesn't inherit this
 * Created by daquexian on 17-2-7.
 */

public abstract class BaseHeaderItem<T extends FlexibleViewHolder> extends AbstractHeaderItem<T> {
    private static List<BaseHeaderItem> headerItemList = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static <K extends BaseHeaderItem> K getInstance(Object object, String title, Class<K> myClass) {
        BaseHeaderItem headerItem;
        if (CertHeaderItem.class.equals(myClass)) {
            headerItem = new CertHeaderItem(object, title);
        } else if (KeyHeaderItem.class.equals(myClass)) {
            headerItem = new KeyHeaderItem(object, title);
        } else {
            throw new IllegalArgumentException("wrong");
        }

        for (BaseHeaderItem thisHeaderItem : headerItemList) {
            if (myClass.equals(thisHeaderItem.getClass()) && thisHeaderItem.equals(headerItem)) {
                return (K) thisHeaderItem;
            }
        }
        headerItemList.add(headerItem);

        return (K) headerItem;
    }

    int mContextHash;
    String mTitle;

    int getContextHash() {
        return mContextHash;
    }

    public String getTitle() {
        return mTitle;
    }

    void setTitle(String title) {
        this.mTitle = title;
    }

}
