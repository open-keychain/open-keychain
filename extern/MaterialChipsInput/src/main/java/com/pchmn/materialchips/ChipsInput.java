package com.pchmn.materialchips;


import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Filter.FilterListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager;
import com.pchmn.materialchips.RecyclerItemClickListener.OnItemClickListener;
import com.pchmn.materialchips.adapter.ChipsAdapter;
import com.pchmn.materialchips.adapter.FilterableAdapter;
import com.pchmn.materialchips.model.ChipInterface;
import com.pchmn.materialchips.model.SimpleChip;
import com.pchmn.materialchips.util.ActivityUtil;
import com.pchmn.materialchips.util.MyWindowCallback;
import com.pchmn.materialchips.util.ViewUtil;
import com.pchmn.materialchips.views.ChipsInputEditText;
import com.pchmn.materialchips.views.DetailedChipView;
import com.pchmn.materialchips.views.DropdownListView;
import com.pchmn.materialchips.views.ScrollViewMaxHeight;

public class ChipsInput extends ScrollViewMaxHeight {
    // context
    private Context mContext;
    private ChipsAdapter mChipsAdapter;
    // attributes
    private static final int NONE = -1;
    private String mHint;
    private ColorStateList mHintColor;
    private ColorStateList mTextColor;
    private int mMaxRows = 2;
    private ColorStateList mChipLabelColor;
    private boolean mChipDeletable = false;
    private Drawable mChipDeleteIcon;
    private ColorStateList mChipDeleteIconColor;
    private ColorStateList mChipBackgroundColor;
    private boolean mShowChipDetailed = true;
    private ColorStateList mChipDetailedTextColor;
    private ColorStateList mChipDetailedDeleteIconColor;
    private ColorStateList mChipDetailedBackgroundColor;
    // chips listener
    private List<ChipsListener> mChipsListenerList = new ArrayList<>();
    // chip list
    private DropdownListView mDropdownListView;
    // chip validator
    private ChipValidator mChipValidator;
    private ViewGroup filterableListLayout;
    private ChipsInputEditText mEditText;
    private ChipDropdownAdapter<? extends ChipInterface, ?> filterableAdapter;

    public ChipsInput(Context context) {
        super(context);
        mContext = context;
        init(null);
    }

    public ChipsInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(attrs);
    }

    /**
     * Inflate the view according to attributes
     *
     * @param attrs the attributes
     */
    private void init(AttributeSet attrs) {
        // inflate filterableListLayout
        View rootView = inflate(getContext(), R.layout.chips_input, this);

        RecyclerView recyclerView = rootView.findViewById(R.id.chips_recycler);

        initEditText();

        // attributes
        if (attrs != null) {
            TypedArray a = mContext.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.ChipsInput,
                    0, 0);

            try {
                // hint
                mHint = a.getString(R.styleable.ChipsInput_hint);
                mHintColor = a.getColorStateList(R.styleable.ChipsInput_hintColor);
                mTextColor = a.getColorStateList(R.styleable.ChipsInput_textColor);
                mMaxRows = a.getInteger(R.styleable.ChipsInput_maxRows, 2);
                setMaxHeight(ViewUtil.dpToPx((40 * mMaxRows) + 8));
                //setVerticalScrollBarEnabled(true);
                // chip label color
                mChipLabelColor = a.getColorStateList(R.styleable.ChipsInput_chip_labelColor);
                // chip delete icon
                mChipDeletable = a.getBoolean(R.styleable.ChipsInput_chip_deletable, false);
                mChipDeleteIconColor = a.getColorStateList(R.styleable.ChipsInput_chip_deleteIconColor);
                int deleteIconId = a.getResourceId(R.styleable.ChipsInput_chip_deleteIcon, NONE);
                if (deleteIconId != NONE)
                    mChipDeleteIcon = ContextCompat.getDrawable(mContext, deleteIconId);
                // chip background color
                mChipBackgroundColor = a.getColorStateList(R.styleable.ChipsInput_chip_backgroundColor);
                // show chip detailed
                mShowChipDetailed = a.getBoolean(R.styleable.ChipsInput_showChipDetailed, true);
                // chip detailed text color
                mChipDetailedTextColor = a.getColorStateList(R.styleable.ChipsInput_chip_detailed_textColor);
                mChipDetailedBackgroundColor = a.getColorStateList(R.styleable.ChipsInput_chip_detailed_backgroundColor);
                mChipDetailedDeleteIconColor = a.getColorStateList(R.styleable.ChipsInput_chip_detailed_deleteIconColor);
            } finally {
                a.recycle();
            }
        }

        // adapter
        mChipsAdapter = new ChipsAdapter(mContext, this, mEditText, recyclerView);
        ChipsLayoutManager chipsLayoutManager = ChipsLayoutManager.newBuilder(mContext)
                .setOrientation(ChipsLayoutManager.HORIZONTAL)
                .build();
        recyclerView.setLayoutManager(chipsLayoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(mChipsAdapter);

        // set window callback
        // will hide DetailedOpenView and hide keyboard on touch outside
        Activity activity = ActivityUtil.scanForActivity(mContext);
        if (activity == null)
            throw new ClassCastException("android.view.Context cannot be cast to android.app.Activity");

        android.view.Window.Callback mCallBack = (activity).getWindow().getCallback();
        activity.getWindow().setCallback(new MyWindowCallback(mCallBack, activity));
    }

    private void initEditText() {
        mEditText = new ChipsInputEditText(mContext);
        if (mHintColor != null)
            mEditText.setHintTextColor(mHintColor);
        if (mTextColor != null)
            mEditText.setTextColor(mTextColor);

        mEditText.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mEditText.setHint(mHint);
        mEditText.setBackgroundResource(android.R.color.transparent);
        // prevent fullscreen on landscape
        mEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_DONE);
        mEditText.setPrivateImeOptions("nm");
        // no suggestion
        mEditText.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // handle back space
        mEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // backspace
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    // remove last chip
                    if (mEditText.getText().toString().length() == 0)
                        mChipsAdapter.removeLastChip();
                }
                return false;
            }
        });

        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId == EditorInfo.IME_ACTION_DONE) || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    ChipsInput.this.onActionDone(mEditText.getText().toString());
                }
                return false;
            }
        });

        // text changed
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ChipsInput.this.onTextChanged(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public void addChips(List<ChipInterface> chipList) {
        mChipsAdapter.addChipsProgrammatically(chipList);
    }

    public void addChip(ChipInterface chip) {
        mChipsAdapter.addChip(chip);
    }

    public void addChip(Object id, String label, String info, String filterString) {
        SimpleChip chip = new SimpleChip(id, label, info, filterString);
        mChipsAdapter.addChip(chip);
    }

    public void addChip(String label, String info) {
        SimpleChip chip = new SimpleChip(label, info);
        mChipsAdapter.addChip(chip);
    }

    public void removeChip(ChipInterface chip) {
        mChipsAdapter.removeChip(chip);
    }

    public void removeChipById(Object id) {
        mChipsAdapter.removeChipById(id);
    }

    public void removeChipByLabel(String label) {
        mChipsAdapter.removeChipByLabel(label);
    }

    public void removeChipByInfo(String info) {
        mChipsAdapter.removeChipByInfo(info);
    }

    public ChipView getChipView() {
        int padding = ViewUtil.dpToPx(4);
        ChipView chipView = new ChipView.Builder(mContext)
                .labelColor(mChipLabelColor)
                .deletable(mChipDeletable)
                .deleteIcon(mChipDeleteIcon)
                .deleteIconColor(mChipDeleteIconColor)
                .backgroundColor(mChipBackgroundColor)
                .build();

        chipView.setPadding(padding, padding, padding, padding);

        return chipView;
    }

    public ChipsInputEditText getEditText() {
        return mChipsAdapter.getmEditText();
    }

    public DetailedChipView getDetailedChipView(ChipInterface chip) {
        return new DetailedChipView.Builder(mContext)
                .chip(chip)
                .textColor(mChipDetailedTextColor)
                .backgroundColor(mChipDetailedBackgroundColor)
                .deleteIconColor(mChipDetailedDeleteIconColor)
                .build();
    }

    public void addChipsListener(ChipsListener chipsListener) {
        mChipsListenerList.add(chipsListener);
    }

    public void onChipAdded(ChipInterface chip, int size) {
        for (ChipsListener chipsListener : mChipsListenerList) {
            chipsListener.onChipAdded(chip, size);
        }
    }

    public void onChipRemoved(ChipInterface chip, int size) {
        for (ChipsListener chipsListener : mChipsListenerList) {
            chipsListener.onChipRemoved(chip, size);
        }
    }

    public void onTextChanged(CharSequence text) {
        for (ChipsListener chipsListener : mChipsListenerList) {
            chipsListener.onTextChanged(text);
        }
        // show filterable list
        if (mDropdownListView != null) {
            if (text.length() > 0) {
                filterDropdownList(text);
            } else {
                mDropdownListView.fadeOut();
            }
        }
    }

    public void filterDropdownList(CharSequence text) {
        filterableAdapter.getFilter().filter(text, new FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                // show if there are results
                if (filterableAdapter.getItemCount() > 0)
                    mDropdownListView.fadeIn();
                else
                    mDropdownListView.fadeOut();
            }
        });
    }

    public void onActionDone(CharSequence text) {
        for (ChipsListener chipsListener : mChipsListenerList) {
            chipsListener.onActionDone(text);
        }
    }

    public List<? extends ChipInterface> getSelectedChipList() {
        return mChipsAdapter.getChipList();
    }

    public String getHint() {
        return mHint;
    }

    public void setHint(String mHint) {
        this.mHint = mHint;
    }

    public void setHintColor(ColorStateList mHintColor) {
        this.mHintColor = mHintColor;
    }

    public void setTextColor(ColorStateList mTextColor) {
        this.mTextColor = mTextColor;
    }

    public ChipsInput setMaxRows(int mMaxRows) {
        this.mMaxRows = mMaxRows;
        return this;
    }

    public void setChipLabelColor(ColorStateList mLabelColor) {
        this.mChipLabelColor = mLabelColor;
    }

    public void setChipDeletable(boolean mDeletable) {
        this.mChipDeletable = mDeletable;
    }

    public void setChipDeleteIcon(Drawable mDeleteIcon) {
        this.mChipDeleteIcon = mDeleteIcon;
    }

    public void setChipDeleteIconColor(ColorStateList mDeleteIconColor) {
        this.mChipDeleteIconColor = mDeleteIconColor;
    }

    public void setChipBackgroundColor(ColorStateList mBackgroundColor) {
        this.mChipBackgroundColor = mBackgroundColor;
    }

    public ChipsInput setShowChipDetailed(boolean mShowChipDetailed) {
        this.mShowChipDetailed = mShowChipDetailed;
        return this;
    }

    public boolean isShowChipDetailed() {
        return mShowChipDetailed;
    }

    public void setChipDetailedTextColor(ColorStateList mChipDetailedTextColor) {
        this.mChipDetailedTextColor = mChipDetailedTextColor;
    }

    public void setChipDetailedDeleteIconColor(ColorStateList mChipDetailedDeleteIconColor) {
        this.mChipDetailedDeleteIconColor = mChipDetailedDeleteIconColor;
    }

    public void setChipDetailedBackgroundColor(ColorStateList mChipDetailedBackgroundColor) {
        this.mChipDetailedBackgroundColor = mChipDetailedBackgroundColor;
    }

    public void setFilterableListLayout(ViewGroup layout) {
        this.filterableListLayout = layout;
    }

    public abstract static class ChipDropdownAdapter<T extends ChipInterface, VH extends ViewHolder>
            extends FilterableAdapter<T, VH> {
        public ChipDropdownAdapter(List<? extends T> itemList) {
            super(itemList);
        }
    }

    public <T extends ChipInterface> void setChipDropdownAdapter(final ChipDropdownAdapter<T, ?> filterableAdapter) {
        this.filterableAdapter = filterableAdapter;
        if (filterableListLayout != null) {
            mDropdownListView = new DropdownListView(mContext, filterableListLayout);
        } else {
            mDropdownListView = new DropdownListView(mContext, this);
        }
        mDropdownListView.build(filterableAdapter);
        mChipsAdapter.setFilterableListView(mDropdownListView);
        mDropdownListView.getRecyclerView().addOnItemTouchListener(new RecyclerItemClickListener(getContext(), new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                ChipInterface item = filterableAdapter.getItem(position);
                addChip(item);
            }
        }));

        addChipsListener(new ChipsInput.ChipsListener() {
            @Override
            public void onChipAdded(ChipInterface chip, int newSize) {
                filterableAdapter.hideItem((T) chip);
            }

            @Override
            public void onChipRemoved(ChipInterface chip, int newSize) {
                filterableAdapter.unhideItem((T) chip);
            }

            @Override
            public void onTextChanged(CharSequence text) {
                mDropdownListView.getRecyclerView().scrollToPosition(0);
            }

            @Override
            public void onActionDone(CharSequence text) {
                mDropdownListView.getRecyclerView().scrollToPosition(0);
            }
        });
    }

    public ChipValidator getChipValidator() {
        return mChipValidator;
    }

    public void setChipValidator(ChipValidator mChipValidator) {
        this.mChipValidator = mChipValidator;
    }

    public interface ChipsListener {
        void onChipAdded(ChipInterface chip, int newSize);

        void onChipRemoved(ChipInterface chip, int newSize);

        void onTextChanged(CharSequence text);

        void onActionDone(CharSequence text);
    }

    public interface ChipValidator {
        boolean areEquals(ChipInterface chip1, ChipInterface chip2);
    }
}
