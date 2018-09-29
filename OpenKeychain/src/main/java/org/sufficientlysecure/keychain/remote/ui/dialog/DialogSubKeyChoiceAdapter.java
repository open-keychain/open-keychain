package org.sufficientlysecure.keychain.remote.ui.dialog;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.remote.ui.dialog.DialogSubKeyChoiceAdapter.SubKeyChoiceViewHolder;
import org.sufficientlysecure.keychain.ui.util.KeyInfoFormatter;

import java.util.List;

import static org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.*;


class DialogSubKeyChoiceAdapter extends Adapter<SubKeyChoiceViewHolder> {
    private final LayoutInflater layoutInflater;
    private List<SubKey> data;
    private Drawable iconUnselected;
    private Drawable iconSelected;
    private Integer activeItem;

    DialogSubKeyChoiceAdapter(Context context, LayoutInflater layoutInflater) {
        this.layoutInflater = layoutInflater;
    }

    @NonNull
    @Override
    public SubKeyChoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View keyChoiceItemView = layoutInflater.inflate(R.layout.api_select_auth_subkey_item, parent, false);
        return new SubKeyChoiceViewHolder(keyChoiceItemView);
    }

    void setActiveItem(Integer activeItem) {
        this.activeItem = activeItem;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull SubKeyChoiceViewHolder holder, int position) {
        SubKey keyInfo = data.get(position);
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

    public void setData(List<SubKey> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    void setSelectionDrawables(Drawable iconSelected, Drawable iconUnselected) {
        this.iconSelected = iconSelected;
        this.iconUnselected = iconUnselected;

        notifyDataSetChanged();
    }

    class SubKeyChoiceViewHolder extends RecyclerView.ViewHolder {
        private final TextView vId;
        private final TextView vType;
        private final TextView vCreation = (TextView) itemView.findViewById(R.id.key_list_item_creation);
        private final ImageView vIcon;

        SubKeyChoiceViewHolder(View itemView) {
            super(itemView);

            vId = itemView.findViewById(R.id.key_list_item_id);
            vIcon = itemView.findViewById(R.id.key_list_item_icon);
            vType = itemView.findViewById(R.id.key_list_item_type);
        }

        void bind(SubKey keyInfo, Drawable selectionIcon) {
            Context context = vCreation.getContext();

            // TODO: prettify UI

            vId.setText(context.getString(R.string.use_key, beautifyKeyId(keyInfo.key_id())));
            vType.setText(getAlgorithmInfo(keyInfo.algorithm(), keyInfo.key_size(), keyInfo.key_curve_oid()));

            // TODO: taken from KeyFormattingUtils, try to resuse
            long creationMillis = keyInfo.creation() * 1000;
            String dateTime = DateUtils.formatDateTime(context,
                creationMillis,
                DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_SHOW_YEAR
                        | DateUtils.FORMAT_ABBREV_MONTH);
            String creationDate = context.getString(R.string.label_key_created, dateTime);
            vCreation.setText(creationDate);

            vIcon.setImageDrawable(selectionIcon);
        }
    }
}
