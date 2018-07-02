package com.pchmn.materialchips.model;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class Chip implements ChipInterface {

    private Object id;
    private String label;
    private String info;

    public Chip(@NonNull Object id, @NonNull String label, @Nullable String info) {
        this.id = id;
        this.label = label;
        this.info = info;
    }


    public Chip(@NonNull String label, @Nullable String info) {
        this.label = label;
        this.info = info;
    }

    @Override
    public Object getId() {
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
}
