package com.pchmn.materialchips.simple;


import java.util.List;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pchmn.materialchips.ChipsInput.ChipDropdownAdapter;
import com.pchmn.materialchips.R;
import com.pchmn.materialchips.simple.SimpleChipDropdownAdapter.ItemViewHolder;


public class SimpleChipDropdownAdapter extends ChipDropdownAdapter<SimpleChip, ItemViewHolder> {
    private final LayoutInflater layoutInflater;

    public SimpleChipDropdownAdapter(Context context, List<SimpleChip> keyInfoChips) {
        super(keyInfoChips);

        layoutInflater = LayoutInflater.from(context);
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView mLabel;
        private TextView mInfo;

        ItemViewHolder(View view) {
            super(view);
            mLabel = view.findViewById(com.pchmn.materialchips.R.id.label);
            mInfo = view.findViewById(com.pchmn.materialchips.R.id.info);
        }
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.item_list_filterable, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        SimpleChip chip = getItem(position);

        holder.mLabel.setText(chip.getLabel());
        if (chip.getInfo() != null) {
            holder.mInfo.setVisibility(View.VISIBLE);
            holder.mInfo.setText(chip.getInfo());
        } else {
            holder.mInfo.setVisibility(View.GONE);
        }
    }

}
