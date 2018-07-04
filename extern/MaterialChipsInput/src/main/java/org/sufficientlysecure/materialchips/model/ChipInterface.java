package org.sufficientlysecure.materialchips.model;


import org.sufficientlysecure.materialchips.adapter.FilterableAdapter.FilterableItem;


public interface ChipInterface extends FilterableItem {
    String getLabel();
    String getInfo();
}
