package com.pchmn.materialchips.simple;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.pchmn.materialchips.ChipView;
import com.pchmn.materialchips.ChipsInput;
import com.pchmn.materialchips.adapter.ChipsAdapter;
import com.pchmn.materialchips.simple.SimpleChipsAdapter.ItemViewHolder;
import com.pchmn.materialchips.util.ViewUtil;
import com.pchmn.materialchips.views.DetailedChipView;


public class SimpleChipsAdapter extends ChipsAdapter<SimpleChip, ItemViewHolder> {
    public SimpleChipsAdapter(Context context, ChipsInput<SimpleChip> chipsInput) {
        super(context, chipsInput);
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ChipView chipView;

        ItemViewHolder(View view) {
            super(view);
            chipView = (ChipView) view;
        }
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
        holder.chipView.inflate(getItem(position));
        handleClickOnEditText(holder.chipView, position);
    }

    @Override
    public DetailedChipView getDetailedChipView(SimpleChip chip) {
        return new DetailedChipView.Builder(context)
                .chip(chip)
                .build();
    }

}
