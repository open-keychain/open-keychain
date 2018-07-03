package org.sufficientlysecure.keychain.ui.adapter;


import android.support.annotation.NonNull;

import com.pchmn.materialchips.model.ChipInterface;

import org.sufficientlysecure.keychain.model.SubKey;

public class KeyChip implements ChipInterface {

    private SubKey.UnifiedKeyInfo keyInfo;
    private String filterString;

    public KeyChip(@NonNull SubKey.UnifiedKeyInfo keyInfo) {
        this.keyInfo = keyInfo;
        this.filterString = keyInfo.user_id_list() != null ? keyInfo.user_id_list().toLowerCase() : keyInfo.name().toLowerCase();
    }

    @Override
    public Object getId() {
        return keyInfo.master_key_id();
    }

    @Override
    public String getLabel() {
        return keyInfo.name();
    }

    @Override
    public String getInfo() {
        return keyInfo.email();
    }

    @Override
    public boolean isKeptForConstraint(CharSequence constraint) {
        return filterString.contains(constraint);
    }

    public SubKey.UnifiedKeyInfo getKeyInfo() {
        return keyInfo;
    }

}