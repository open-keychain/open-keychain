package org.sufficientlysecure.keychain.ui.adapter;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.ui.adapter.KeyChoiceAdapter.KeyChoiceItem;


public class KeyChoiceAdapter extends FlexibleAdapter<KeyChoiceItem> {
    private final OnKeyClickListener onKeyClickListener;
    private Integer activeItem;

    public static KeyChoiceAdapter createSingleClickableAdapter(List<UnifiedKeyInfo> items,
            OnKeyClickListener onKeyClickListener) {
        return new KeyChoiceAdapter(items, Objects.requireNonNull(onKeyClickListener), Mode.IDLE);
    }

    public static KeyChoiceAdapter createSingleChoiceAdapter(List<UnifiedKeyInfo> items) {
        return new KeyChoiceAdapter(items, null, Mode.SINGLE);
    }

    public static KeyChoiceAdapter createMultiChoiceAdapter(List<UnifiedKeyInfo> items) {
        return new KeyChoiceAdapter(items, null, Mode.MULTI);
    }

    private KeyChoiceAdapter(List<UnifiedKeyInfo> items, OnKeyClickListener onKeyClickListener, int idle) {
        super(getKeyChoiceItems(items));
        setMode(idle);
        addListener((OnItemClickListener) (view, position) -> onClickItem(position));
        this.onKeyClickListener = onKeyClickListener;
    }

    @Nullable
    private static ArrayList<KeyChoiceItem> getKeyChoiceItems(@Nullable List<UnifiedKeyInfo> items) {
        if (items == null) {
            return null;
        }
        ArrayList<KeyChoiceItem> choiceItems = new ArrayList<>();
        for (UnifiedKeyInfo keyInfo : items) {
            KeyChoiceItem keyChoiceItem = new KeyChoiceItem(keyInfo);
            choiceItems.add(keyChoiceItem);
        }
        return choiceItems;
    }

    private boolean onClickItem(int position) {
        if (getMode() == Mode.MULTI) {
            toggleSelection(position);
            notifyItemChanged(position);
            return true;
        } else if (getMode() == Mode.SINGLE) {
            setActiveItem(position);
            return true;
        }

        KeyChoiceItem item = getItem(position);
        onKeyClickListener.onKeyClick(item.keyInfo);
        return false;
    }

    public void setActiveItem(Integer newActiveItem) {
        if (getMode() != Mode.SINGLE) {
            throw new IllegalStateException("Cannot get active item in single select mode!");
        }

        clearSelection();

        Integer prevActiveItem = this.activeItem;
        this.activeItem = newActiveItem;

        if (prevActiveItem != null) {
            notifyItemChanged(prevActiveItem);
        }
        if (newActiveItem != null) {
            toggleSelection(newActiveItem);
            notifyItemChanged(newActiveItem);
        }
    }

    public UnifiedKeyInfo getActiveItem() {
        if (getMode() != Mode.SINGLE) {
            throw new IllegalStateException("Cannot get active item in single select mode!");
        }
        if (activeItem == null) {
            return null;
        }

        KeyChoiceItem item = getItem(activeItem);
        return item == null ? null : item.keyInfo;
    }

    public void setUnifiedKeyInfoItems(List<UnifiedKeyInfo> keyInfos) {
        List<KeyChoiceItem> keyChoiceItems = getKeyChoiceItems(keyInfos);
        updateDataSet(keyChoiceItems);
    }

    @Override
    public long getItemId(int position) {
        KeyChoiceItem item = getItem(position);
        if (item == null) {
            return RecyclerView.NO_ID;
        }
        return item.getMasterKeyId();
    }

    public void setSelectionByIds(Set<Long> checkedIds) {
        if (getMode() != Mode.MULTI) {
            throw new IllegalStateException("Cannot get active item in single select mode!");
        }

        clearSelection();
        for (int position = 0; position < getItemCount(); position++) {
            long itemId = getItemId(position);
            if (checkedIds.contains(itemId)) {
                addSelection(position);
            }
        }
    }

    public Set<Long> getSelectionIds() {
        if (getMode() != Mode.MULTI) {
            throw new IllegalStateException("Cannot get active item in single select mode!");
        }

        Set<Long> result = new HashSet<>();
        for (int position : getSelectedPositions()) {
            long itemId = getItemId(position);
            result.add(itemId);
        }
        return result;
    }

    public static class KeyChoiceItem extends AbstractFlexibleItem<KeyChoiceViewHolder> {
        private UnifiedKeyInfo keyInfo;

        KeyChoiceItem(UnifiedKeyInfo keyInfo) {
            this.keyInfo = keyInfo;
            setSelectable(true);
        }

        @Override
        public int getLayoutRes() {
            return R.layout.key_choice_item;
        }

        @Override
        public KeyChoiceViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
            return new KeyChoiceViewHolder(view, adapter);
        }

        @Override
        public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, KeyChoiceViewHolder holder, int position,
                List<Object> payloads) {
            boolean isActive = adapter.isSelected(position);
            holder.bind(keyInfo, adapter.getMode(), isActive);
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof KeyChoiceItem) &&
                    ((KeyChoiceItem) o).keyInfo.master_key_id() == keyInfo.master_key_id();
        }

        @Override
        public int hashCode() {
            long masterKeyId = keyInfo.master_key_id();
            return (int) (masterKeyId ^ (masterKeyId >>> 32));
        }

        public long getMasterKeyId() {
            return keyInfo.master_key_id();
        }
    }

    public static class KeyChoiceViewHolder extends FlexibleViewHolder {
        private final TextView vName;
        private final TextView vCreation;
        private final CheckBox vCheckbox;
        private final RadioButton vRadio;

        KeyChoiceViewHolder(View itemView, FlexibleAdapter<IFlexible> adapter) {
            super(itemView, adapter);

            vName = itemView.findViewById(R.id.text_keychoice_name);
            vCreation = itemView.findViewById(R.id.text_keychoice_creation);
            vCheckbox = itemView.findViewById(R.id.checkbox_keychoice);
            vRadio = itemView.findViewById(R.id.radio_keychoice);
        }

        void bind(UnifiedKeyInfo keyInfo, int choiceMode, boolean isActive) {
            vName.setText(keyInfo.name());

            Context context = vCreation.getContext();
            if (keyInfo.has_any_secret() || keyInfo.has_duplicate()) {
                String dateTime = DateUtils.formatDateTime(context, keyInfo.creation() * 1000,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME |
                                DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_MONTH);
                vCreation.setText(context.getString(R.string.label_key_created, dateTime));
                vCreation.setVisibility(View.VISIBLE);
            } else {
                vCreation.setVisibility(View.GONE);
            }

            switch (choiceMode) {
                case Mode.IDLE: {
                    vRadio.setVisibility(View.GONE);
                    vCheckbox.setVisibility(View.GONE);
                    break;
                }
                case Mode.SINGLE: {
                    vRadio.setVisibility(View.VISIBLE);
                    vRadio.setChecked(isActive);
                    vCheckbox.setVisibility(View.GONE);
                    break;
                }
                case Mode.MULTI: {
                    vCheckbox.setVisibility(View.VISIBLE);
                    vCheckbox.setChecked(isActive);
                    vRadio.setVisibility(View.GONE);
                    break;
                }
            }
        }
    }

    public interface OnKeyClickListener {
        void onKeyClick(UnifiedKeyInfo keyInfo);
    }
}
