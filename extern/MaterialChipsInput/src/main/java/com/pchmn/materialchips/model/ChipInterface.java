package com.pchmn.materialchips.model;


import com.pchmn.materialchips.adapter.FilterableAdapter.FilterableItem;


public interface ChipInterface extends FilterableItem {
    String getLabel();
    String getInfo();
}
