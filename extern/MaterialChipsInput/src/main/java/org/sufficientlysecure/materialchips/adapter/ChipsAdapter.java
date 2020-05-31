package org.sufficientlysecure.materialchips.adapter;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.RelativeLayout;

import org.sufficientlysecure.materialchips.ChipView;
import org.sufficientlysecure.materialchips.ChipsInput;
import org.sufficientlysecure.materialchips.adapter.FilterableAdapter.FilterableItem;
import org.sufficientlysecure.materialchips.util.ViewUtil;
import org.sufficientlysecure.materialchips.views.ChipsInputEditText;
import org.sufficientlysecure.materialchips.views.DetailedChipView;


public abstract class ChipsAdapter<T extends FilterableItem, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_EDIT_TEXT = 0;
    private static final int TYPE_ITEM = 1;

    protected Context context;

    private ChipsInput<T> chipsInput;
    private List<T> chipList = new ArrayList<>();

    private ChipsInputEditText editText;
    private String hintLabel;

    private RecyclerView chipsRecycler;

    public ChipsAdapter(Context context, ChipsInput<T> chipsInput) {
        this.chipsInput = chipsInput;
        this.chipsRecycler = chipsInput.getChipsRecyclerView();
        this.editText = chipsInput.getEditText();
        this.context = context;

        this.hintLabel = chipsInput.getHint();
        setHasStableIds(true);
    }

    @Override
    public int getItemCount() {
        return chipList.size() + 1;
    }

    protected T getItem(int position) {
        return chipList.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == chipList.size()) {
            return TYPE_EDIT_TEXT;
        }

        return TYPE_ITEM;
    }

    @Override
    public long getItemId(int position) {
        if (position == chipList.size()) {
            return 0;
        }

        FilterableItem item = getItem(position);
        return item != null ? item.getId() : RecyclerView.NO_ID;
    }

    private void autofitEditText() {
        // min width of edit text = 50 dp
        ViewGroup.LayoutParams params = editText.getLayoutParams();
        params.width = ViewUtil.dpToPx(50);
        editText.setLayoutParams(params);

        // listen to change in the tree
        editText.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                // get right of recycler and left of edit text
                int right = chipsRecycler.getRight();
                int left = editText.getLeft();

                // edit text will fill the space
                ViewGroup.LayoutParams params = editText.getLayoutParams();
                params.width = right - left - ViewUtil.dpToPx(8);
                editText.setLayoutParams(params);

                // request focus
                editText.requestFocus();

                // remove the listener:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    editText.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    editText.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }

        });
    }

    public void handleClickOnEditText(ChipView chipView, final int position) {
        // delete chip
        chipView.setOnDeleteClicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeChip(position);
            }
        });

        // show detailed chip
        if (chipsInput.isShowChipDetailed()) {
            chipView.setOnChipClicked(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // get chip position
                    int[] coord = new int[2];
                    v.getLocationInWindow(coord);

                    final DetailedChipView detailedChipView = getDetailedChipView(getItem(position));
                    setDetailedChipViewPosition(detailedChipView, coord);

                    // delete button
                    detailedChipView.setOnDeleteClicked(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            removeChip(position);
                            detailedChipView.fadeOut();
                        }
                    });
                }
            });
        }
    }

    public abstract DetailedChipView getDetailedChipView(T chip);

    private void setDetailedChipViewPosition(DetailedChipView detailedChipView, int[] coord) {
        // window width
        ViewGroup rootView = (ViewGroup) chipsRecycler.getRootView();
        int windowWidth = ViewUtil.getWindowWidth(context);

        // chip size
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                ViewUtil.dpToPx(300),
                ViewUtil.dpToPx(100));

        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

        // align left window
        if (coord[0] <= 0) {
            layoutParams.leftMargin = 0;
            layoutParams.topMargin = coord[1] - ViewUtil.dpToPx(13);
            detailedChipView.alignLeft();
        }
        // align right
        else if (coord[0] + ViewUtil.dpToPx(300) > windowWidth + ViewUtil.dpToPx(13)) {
            layoutParams.leftMargin = windowWidth - ViewUtil.dpToPx(300);
            layoutParams.topMargin = coord[1] - ViewUtil.dpToPx(13);
            detailedChipView.alignRight();
        }
        // same position as chip
        else {
            layoutParams.leftMargin = coord[0] - ViewUtil.dpToPx(13);
            layoutParams.topMargin = coord[1] - ViewUtil.dpToPx(13);
        }

        // show view
        rootView.addView(detailedChipView, layoutParams);
        detailedChipView.fadeIn();
    }

    public void addChipsProgrammatically(List<T> chipList) {
        if (chipList != null) {
            if (chipList.size() > 0) {
                int chipsBeforeAdding = getItemCount();
                for (T chip : chipList) {
                    this.chipList.add(chip);
                    chipsInput.onChipAdded(chip, getItemCount());
                }

                // hide hint
                editText.setHint(null);
                // reset text
                editText.setText(null);

                notifyItemRangeChanged(chipsBeforeAdding, chipList.size());
            }
        }
    }

    public void addChip(T chip) {
        if (chipList.contains(chip)) {
            return;
        }

        chipList.add(chip);
        // notify listener
        chipsInput.onChipAdded(chip, chipList.size());
        // hide hint
        editText.setHint(null);
        // reset text
        editText.setText(null);
        // refresh data
        notifyItemInserted(chipList.size() -1);
    }

    public void removeChip(T chip) {
        int position = chipList.indexOf(chip);
        chipList.remove(position);
        // notify listener
        notifyItemRangeChanged(position, getItemCount());
        chipsInput.onChipRemoved(chip, chipList.size());
        // if 0 chip
        if (chipList.size() == 0) {
            editText.setHint(hintLabel);
        }
        // refresh data
        notifyDataSetChanged();
    }

    public void removeChip(int position) {
        T chip = chipList.get(position);
        // remove contact
        chipList.remove(position);
        // notify listener
        chipsInput.onChipRemoved(chip, chipList.size());
        // if 0 chip
        if (chipList.size() == 0) {
            editText.setHint(hintLabel);
        }
        // refresh data
        notifyDataSetChanged();
    }

    public void removeLastChip() {
        if (chipList.size() > 0) {
            removeChip(chipList.get(chipList.size() - 1));
        }
    }

    public List<T> getChipList() {
        return chipList;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_EDIT_TEXT) {
            return new EditTextViewHolder(editText);
        } else {
            return onCreateChipViewHolder(parent, viewType);
        }
    }

    public abstract ViewHolder onCreateChipViewHolder(ViewGroup parent, int viewType);

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position == chipList.size()) {
            if (chipList.size() == 0) {
                editText.setHint(hintLabel);
            }

            autofitEditText();
        } else if (getItemCount() > 1) {
            onBindChipViewHolder((VH) holder, position);
        }
    }

    public abstract void onBindChipViewHolder(VH holder, int position);

    protected class EditTextViewHolder extends RecyclerView.ViewHolder {
        private final EditText editText;

        EditTextViewHolder(View view) {
            super(view);
            editText = (EditText) view;
        }
    }
}
