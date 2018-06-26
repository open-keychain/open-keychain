package org.sufficientlysecure.keychain.ui.adapter;


import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.ui.adapter.KeyChoiceAdapter.KeyChoiceViewHolder;


public class KeyChoiceAdapter extends Adapter<KeyChoiceViewHolder> {
    private final LayoutInflater layoutInflater;
    private final Resources resources;
    private List<UnifiedKeyInfo> data;
    private Drawable iconUnselected;
    private Drawable iconSelected;
    private Integer activeItem;

    public KeyChoiceAdapter(LayoutInflater layoutInflater, Resources resources) {
        this.layoutInflater = layoutInflater;
        this.resources = resources;
    }

    @NonNull
    @Override
    public KeyChoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View keyChoiceItemView = layoutInflater.inflate(R.layout.duplicate_key_item, parent, false);
        return new KeyChoiceViewHolder(keyChoiceItemView);
    }

    @Override
    public void onBindViewHolder(@NonNull KeyChoiceViewHolder holder, int position) {
        UnifiedKeyInfo keyInfo = data.get(position);
        Drawable icon = (activeItem != null && position == activeItem) ? iconSelected : iconUnselected;
        holder.bind(keyInfo, icon);
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    public void setData(List<UnifiedKeyInfo> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public void setSelectionDrawable(Drawable drawable) {
        ConstantState constantState = drawable.getConstantState();
        if (constantState == null) {
            return;
        }

        iconSelected = constantState.newDrawable(resources);

        iconUnselected = constantState.newDrawable(resources);
        DrawableCompat.setTint(iconUnselected.mutate(), ResourcesCompat.getColor(resources, R.color.md_grey_300, null));

        notifyDataSetChanged();
    }

    public void setActiveItem(Integer newActiveItem) {
        Integer prevActiveItem = this.activeItem;
        this.activeItem = newActiveItem;

        if (prevActiveItem != null) {
            notifyItemChanged(prevActiveItem);
        }
        if (newActiveItem != null) {
            notifyItemChanged(newActiveItem);
        }
    }

    public static class KeyChoiceViewHolder extends RecyclerView.ViewHolder {
        private final TextView vName;
        private final TextView vCreation;
        private final ImageView vIcon;

        KeyChoiceViewHolder(View itemView) {
            super(itemView);

            vName = itemView.findViewById(R.id.key_list_item_name);
            vCreation = itemView.findViewById(R.id.key_list_item_creation);
            vIcon = itemView.findViewById(R.id.key_list_item_icon);
        }

        void bind(UnifiedKeyInfo keyInfo, Drawable selectionIcon) {
            vName.setText(keyInfo.name());

            Context context = vCreation.getContext();
            String dateTime = DateUtils.formatDateTime(context, keyInfo.creation(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME |
                            DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_MONTH);
            vCreation.setText(context.getString(R.string.label_key_created, dateTime));

            vIcon.setImageDrawable(selectionIcon);
        }
    }
}
