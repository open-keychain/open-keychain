package org.sufficientlysecure.keychain.ui.chips;


import java.util.List;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.materialchips.ChipsInput;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.chips.EncryptRecipientChipsInput.EncryptRecipientChip;
import org.sufficientlysecure.keychain.ui.chips.EncryptRecipientDropdownAdapter.ItemViewHolder;
import org.sufficientlysecure.keychain.ui.util.KeyInfoFormatter;


public class EncryptRecipientDropdownAdapter extends ChipsInput.ChipDropdownAdapter<EncryptRecipientChip, ItemViewHolder> {
    private final LayoutInflater layoutInflater;
    private final KeyInfoFormatter keyInfoFormatter;

    EncryptRecipientDropdownAdapter(Context context, List<EncryptRecipientChip> keyInfoChips) {
        super(keyInfoChips);

        layoutInflater = LayoutInflater.from(context);
        keyInfoFormatter = new KeyInfoFormatter(context);
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

        public void bind(EncryptRecipientChip chip) {
            keyInfoFormatter.setKeyInfo(chip.keyInfo);

            keyInfoFormatter.formatUserId(vMainUserId, vMainUserIdRest);
            keyInfoFormatter.formatCreationDate(vCreationDate);
            keyInfoFormatter.formatStatusIcon(vStatusIcon);
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
        EncryptRecipientChip chip = getItem(position);
        holder.bind(chip);
    }
}
