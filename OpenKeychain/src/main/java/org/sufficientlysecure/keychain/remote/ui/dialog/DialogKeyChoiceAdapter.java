package org.sufficientlysecure.keychain.remote.ui.dialog;


import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.remote.ui.dialog.DialogKeyChoiceAdapter.KeyChoiceViewHolder;
import org.sufficientlysecure.keychain.ui.util.KeyInfoFormatter;


class DialogKeyChoiceAdapter extends Adapter<KeyChoiceViewHolder> {
    private final LayoutInflater layoutInflater;
    private List<UnifiedKeyInfo> data;
    private Drawable iconUnselected;
    private Drawable iconSelected;
    private Integer activeItem;
    private KeyInfoFormatter keyInfoFormatter;

    DialogKeyChoiceAdapter(Context context, LayoutInflater layoutInflater) {
        this.layoutInflater = layoutInflater;
        this.keyInfoFormatter = new KeyInfoFormatter(context);
    }

    @NonNull
    @Override
    public KeyChoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View keyChoiceItemView = layoutInflater.inflate(R.layout.api_select_identity_item, parent, false);
        return new KeyChoiceViewHolder(keyChoiceItemView);
    }

    void setActiveItem(Integer activeItem) {
        this.activeItem = activeItem;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull KeyChoiceViewHolder holder, int position) {
        UnifiedKeyInfo keyInfo = data.get(position);
        boolean hasActiveItem = activeItem != null;
        boolean isActiveItem = hasActiveItem && position == activeItem;

        Drawable icon = isActiveItem ? iconSelected : iconUnselected;
        holder.bind(keyInfo, icon);

        holder.itemView.setVisibility(!hasActiveItem || isActiveItem ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    public void setData(List<UnifiedKeyInfo> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    void setSelectionDrawables(Drawable iconSelected, Drawable iconUnselected) {
        this.iconSelected = iconSelected;
        this.iconUnselected = iconUnselected;

        notifyDataSetChanged();
    }

    class KeyChoiceViewHolder extends RecyclerView.ViewHolder {
        private final TextView vName;
        private final TextView vCreation = (TextView) itemView.findViewById(R.id.key_list_item_creation);
        private final ImageView vIcon;

        KeyChoiceViewHolder(View itemView) {
            super(itemView);

            vName = itemView.findViewById(R.id.key_list_item_name);
            vIcon = itemView.findViewById(R.id.key_list_item_icon);
        }

        void bind(UnifiedKeyInfo keyInfo, Drawable selectionIcon) {
            Context context = vCreation.getContext();

            keyInfoFormatter.setKeyInfo(keyInfo);

            String email = keyInfo.email();
            String name = keyInfo.name();
            if (email != null) {
                vName.setText(context.getString(R.string.use_key, email));
            } else if (name != null) {
                vName.setText(context.getString(R.string.use_key, name));
            } else {
                vName.setText(context.getString(R.string.use_key_no_name));
            }

            keyInfoFormatter.formatCreationDate(vCreation);

            vIcon.setImageDrawable(selectionIcon);
        }
    }
}
