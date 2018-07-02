package org.sufficientlysecure.keychain.ui.adapter;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.ui.adapter.KeyChoiceAdapter.KeyChoiceItem;


public class KeyChoiceAdapter extends FlexibleAdapter<KeyChoiceItem> {
    private final OnKeyClickListener onKeyClickListener;
    private final KeyDisabledPredicate keyDisabledPredicate;
    private Integer activeItem;

    public static KeyChoiceAdapter createSingleClickableAdapter(List<UnifiedKeyInfo> items,
            OnKeyClickListener onKeyClickListener) {
        return new KeyChoiceAdapter(items, Objects.requireNonNull(onKeyClickListener), Mode.IDLE, null);
    }

    public static KeyChoiceAdapter createSingleChoiceAdapter(List<UnifiedKeyInfo> items) {
        return new KeyChoiceAdapter(items, null, Mode.SINGLE, null);
    }

    public static KeyChoiceAdapter createMultiChoiceAdapter(List<UnifiedKeyInfo> items, KeyDisabledPredicate keyDisabledPredicate) {
        return new KeyChoiceAdapter(items, null, Mode.MULTI, keyDisabledPredicate);
    }

    private KeyChoiceAdapter(List<UnifiedKeyInfo> items, OnKeyClickListener onKeyClickListener, int idle,
            KeyDisabledPredicate keyDisabledPredicate) {
        super(getKeyChoiceItems(items, keyDisabledPredicate));
        setMode(idle);
        addListener((OnItemClickListener) (view, position) -> onClickItem(position));
        this.onKeyClickListener = onKeyClickListener;
        this.keyDisabledPredicate = keyDisabledPredicate;
    }

    @Nullable
    private static ArrayList<KeyChoiceItem> getKeyChoiceItems(@Nullable List<UnifiedKeyInfo> items,
            KeyDisabledPredicate keyDisabledPredicate) {
        if (items == null) {
            return null;
        }
        ArrayList<KeyChoiceItem> choiceItems = new ArrayList<>();
        for (UnifiedKeyInfo keyInfo : items) {
            Integer disabledString = keyDisabledPredicate.getDisabledString(keyInfo);
            KeyChoiceItem keyChoiceItem = new KeyChoiceItem(keyInfo, disabledString);
            choiceItems.add(keyChoiceItem);
        }
        return choiceItems;
    }

    private boolean onClickItem(int position) {
        KeyChoiceItem item = getItem(position);
        if (item != null && item.disabledStringRes != null) {
            Toast.makeText(getRecyclerView().getContext(), item.disabledStringRes, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (getMode() == Mode.MULTI) {
            toggleSelection(position);
            notifyItemChanged(position);
            return true;
        } else if (getMode() == Mode.SINGLE) {
            setActiveItem(position);
            return true;
        }

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
        List<KeyChoiceItem> keyChoiceItems = getKeyChoiceItems(keyInfos, keyDisabledPredicate);
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
        @StringRes
        private Integer disabledStringRes;

        KeyChoiceItem(UnifiedKeyInfo keyInfo, @StringRes Integer disabledStringRes) {
            this.keyInfo = keyInfo;
            this.disabledStringRes = disabledStringRes;
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
            boolean isEnabled = disabledStringRes == null;
            holder.bind(keyInfo, adapter.getMode(), isActive, isEnabled);
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

        void bind(UnifiedKeyInfo keyInfo, int choiceMode, boolean isActive, boolean isEnabled) {
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

            vCheckbox.setEnabled(isEnabled);
            vRadio.setEnabled(isEnabled);
            vName.setEnabled(isEnabled);
            vCreation.setEnabled(isEnabled);
        }
    }

    public interface OnKeyClickListener {
        void onKeyClick(UnifiedKeyInfo keyInfo);
    }

    public interface KeyDisabledPredicate {
        @StringRes
        Integer getDisabledString(UnifiedKeyInfo keyInfo);
    }
}
