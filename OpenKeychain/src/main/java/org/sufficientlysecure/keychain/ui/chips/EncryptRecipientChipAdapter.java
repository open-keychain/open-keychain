package org.sufficientlysecure.keychain.ui.chips;


import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.materialchips.ChipView;
import org.sufficientlysecure.materialchips.ChipsInput;
import org.sufficientlysecure.materialchips.adapter.ChipsAdapter;
import org.sufficientlysecure.materialchips.simple.SimpleChip;
import org.sufficientlysecure.materialchips.util.ViewUtil;
import org.sufficientlysecure.materialchips.views.DetailedChipView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.ui.chips.EncryptRecipientChipAdapter.ItemViewHolder;
import org.sufficientlysecure.keychain.ui.chips.EncryptRecipientChipsInput.EncryptRecipientChip;


public class EncryptRecipientChipAdapter extends ChipsAdapter<EncryptRecipientChip, ItemViewHolder> {
    EncryptRecipientChipAdapter(Context context, ChipsInput<EncryptRecipientChip> chipsInput) {
        super(context, chipsInput);
    }

    @Override
    public ItemViewHolder onCreateChipViewHolder(ViewGroup parent, int viewType) {
        int padding = ViewUtil.dpToPx(4);
        ChipView chipView = new ChipView.Builder(context)
                // .labelColor(mChipLabelColor)
                // .deletable(mChipDeletable)
                // .deleteIcon(mChipDeleteIcon)
                // .deleteIconColor(mChipDeleteIconColor)
                .build();
        chipView.setPadding(padding, padding, padding, padding);

        return new ItemViewHolder(chipView);
    }

    @Override
    public void onBindChipViewHolder(ItemViewHolder holder, int position) {
        EncryptRecipientChip chip = getItem(position);
        holder.chipView.inflate(simpleChipFromKeyInfo(chip.keyInfo));
        handleClickOnEditText(holder.chipView, position);
    }

    @Override
    public DetailedChipView getDetailedChipView(EncryptRecipientChip chip) {
        return new DetailedChipView.Builder(context)
                .chip(simpleChipFromKeyInfo(chip.keyInfo))
                .backgroundColor(ContextCompat.getColorStateList(context, R.color.cardview_light_background))
                .build();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ChipView chipView;

        ItemViewHolder(View view) {
            super(view);
            chipView = (ChipView) view;
        }
    }

    private SimpleChip simpleChipFromKeyInfo(SubKey.UnifiedKeyInfo keyInfo) {
        String name;
        String email;
        if (keyInfo.name() == null) {
            if (keyInfo.email() != null) {
                name = keyInfo.email();
                email = null;
            } else {
                name = context.getString(R.string.user_id_no_name);
                email = null;
            }
        } else {
            name = keyInfo.name();
            if (keyInfo.email() != null) {
                email = keyInfo.email();
            } else {
                email = null;
            }
        }

        return new SimpleChip(name, email);
    }
}
