package org.sufficientlysecure.keychain.ui.chips;


import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.pchmn.materialchips.ChipView;
import com.pchmn.materialchips.ChipsInput;
import com.pchmn.materialchips.adapter.ChipsAdapter;
import com.pchmn.materialchips.simple.SimpleChip;
import com.pchmn.materialchips.util.ViewUtil;
import com.pchmn.materialchips.views.DetailedChipView;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.chips.EncryptRecipientChipAdapter.ItemViewHolder;
import org.sufficientlysecure.keychain.ui.chips.EncryptRecipientChipsInput.EncryptRecipientChip;


public class EncryptRecipientChipAdapter extends ChipsAdapter<EncryptRecipientChip, ItemViewHolder> {
    public EncryptRecipientChipAdapter(Context context, ChipsInput<EncryptRecipientChip> chipsInput) {
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
        holder.chipView.inflate(new SimpleChip(chip.keyInfo.name(), chip.keyInfo.email()));
        handleClickOnEditText(holder.chipView, position);
    }

    @Override
    public DetailedChipView getDetailedChipView(EncryptRecipientChip chip) {
        return new DetailedChipView.Builder(context)
                .chip(new SimpleChip(chip.keyInfo.name(), chip.keyInfo.email()))
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
}
