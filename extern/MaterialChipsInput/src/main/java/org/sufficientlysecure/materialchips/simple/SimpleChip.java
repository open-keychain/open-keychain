package org.sufficientlysecure.materialchips.simple;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.sufficientlysecure.materialchips.model.ChipInterface;


public class SimpleChip implements ChipInterface {
    private long id;
    private String label;
    private String info;
    private String filterString;

    public SimpleChip(@NonNull long id, @NonNull String label, @Nullable String info, @Nullable String filterString) {
        this.id = id;
        this.label = label;
        this.info = info;
        this.filterString = filterString != null ? filterString.toLowerCase() : label.toLowerCase();
    }

    public SimpleChip(@NonNull String label, @Nullable String info) {
        this.label = label;
        this.info = info;
        this.id = (label + info).hashCode();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getInfo() {
        return info;
    }

    @Override
    public boolean isKeptForConstraint(CharSequence constraint) {
        return filterString.contains(constraint);
    }
}
