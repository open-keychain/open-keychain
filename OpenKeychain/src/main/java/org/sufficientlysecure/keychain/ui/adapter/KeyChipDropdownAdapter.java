package org.sufficientlysecure.keychain.ui.adapter;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pchmn.materialchips.ChipsInput;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.KeyInfoFormatter;

import java.util.List;


public class KeyChipDropdownAdapter extends ChipsInput.ChipDropdownAdapter<KeyChip, KeyChipDropdownAdapter.ItemViewHolder> {
    private final LayoutInflater layoutInflater;

    public KeyChipDropdownAdapter(Context context, List<KeyChip> keyInfoChips) {
        super(keyInfoChips);

        layoutInflater = LayoutInflater.from(context);
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView vMainUserId;
        private final TextView vMainUserIdRest;
        private final TextView vCreationDate;
        private final ImageView vStatusIcon;

        ItemViewHolder(View view) {
            super(view);

            vMainUserId = itemView.findViewById(R.id.key_list_item_name);
            vMainUserIdRest = itemView.findViewById(R.id.key_list_item_email);
            vStatusIcon = itemView.findViewById(R.id.key_list_item_status_icon);
            vCreationDate = itemView.findViewById(R.id.key_list_item_creation);
        }
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.key_list_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        KeyChip chip = getItem(position);

        KeyInfoFormatter keyInfoFormatter = new KeyInfoFormatter(layoutInflater.getContext(), chip.getKeyInfo(), null);
        keyInfoFormatter.formatUserId(holder.vMainUserId, holder.vMainUserIdRest);
        keyInfoFormatter.formatCreationDate(holder.vCreationDate);
        keyInfoFormatter.formatStatusIcon(holder.vStatusIcon);
    }

}
