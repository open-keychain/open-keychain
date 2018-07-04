package org.sufficientlysecure.keychain.ui.chips;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;

import org.sufficientlysecure.materialchips.ChipsInput;
import org.sufficientlysecure.materialchips.adapter.FilterableAdapter.FilterableItem;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.ui.chips.EncryptRecipientChipsInput.EncryptRecipientChip;


public class EncryptRecipientChipsInput extends ChipsInput<EncryptRecipientChip> {
    private long[] preselectedKeyIds;

    public EncryptRecipientChipsInput(Context context) {
        super(context);
        init();
    }

    public EncryptRecipientChipsInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        EncryptRecipientChipAdapter chipsAdapter = new EncryptRecipientChipAdapter(getContext(), this);
        setChipsAdapter(chipsAdapter);
    }

    public void setData(List<EncryptRecipientChip> keyInfoChips) {
        EncryptRecipientDropdownAdapter chipDropdownAdapter = new EncryptRecipientDropdownAdapter(getContext(), keyInfoChips);
        setChipDropdownAdapter(chipDropdownAdapter);

        if (preselectedKeyIds != null) {
            Arrays.sort(preselectedKeyIds);
            ArrayList<EncryptRecipientChip> preselectedChips = new ArrayList<>();
            for (EncryptRecipientChip keyInfoChip : keyInfoChips) {
                if (Arrays.binarySearch(preselectedKeyIds, keyInfoChip.keyInfo.master_key_id()) >= 0) {
                    preselectedChips.add(keyInfoChip);
                }
            }
            addChips(preselectedChips);
            preselectedKeyIds = null;
        }
    }

    public void setPreSelectedKeyIds(long[] preselectedEncryptionKeyIds) {
        this.preselectedKeyIds = preselectedEncryptionKeyIds;
    }

    public static class EncryptRecipientChip implements FilterableItem {
        public final UnifiedKeyInfo keyInfo;

        EncryptRecipientChip(UnifiedKeyInfo keyInfo) {
            this.keyInfo = keyInfo;
        }

        @Override
        public long getId() {
            return keyInfo.master_key_id();
        }

        @Override
        public boolean isKeptForConstraint(CharSequence constraint) {
            return keyInfo.uidSearchString().contains(constraint);
        }
    }

    public static EncryptRecipientChip chipFromUnifiedKeyInfo(UnifiedKeyInfo keyInfo) {
        return new EncryptRecipientChip(keyInfo);
    }
}
