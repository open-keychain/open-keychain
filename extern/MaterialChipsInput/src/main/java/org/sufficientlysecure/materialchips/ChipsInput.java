package org.sufficientlysecure.materialchips;


import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
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
import org.sufficientlysecure.materialchips.RecyclerItemClickListener.OnItemClickListener;
import org.sufficientlysecure.materialchips.adapter.ChipsAdapter;
import org.sufficientlysecure.materialchips.adapter.FilterableAdapter;
import org.sufficientlysecure.materialchips.adapter.FilterableAdapter.FilterableItem;
import org.sufficientlysecure.materialchips.util.ActivityUtil;
import org.sufficientlysecure.materialchips.util.ClickOutsideCallback;
import org.sufficientlysecure.materialchips.util.ViewUtil;
import org.sufficientlysecure.materialchips.views.ChipsInputEditText;
import org.sufficientlysecure.materialchips.views.DropdownListView;
import org.sufficientlysecure.materialchips.views.ScrollViewMaxHeight;

public abstract class ChipsInput<T extends FilterableItem> extends ScrollViewMaxHeight {
    private Context mContext;

    // attributes
    private String mHint;
    private ColorStateList mHintColor;
    private ColorStateList mTextColor;
    private int mMaxRows = 2;
    private boolean mShowChipDetailed = true;

    private List<ChipsListener<T>> mChipsListenerList = new ArrayList<>();

    private ChipsAdapter<T, ?> chipsAdapter;
    private RecyclerView chipsRecyclerView;
    private ChipsInputEditText chipsInputEditText;

    private ChipDropdownAdapter<T, ?> filterableAdapter;
    private ViewGroup filterableListLayout;
    private DropdownListView mDropdownListView;

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

        chipsRecyclerView = rootView.findViewById(R.id.chips_recycler);

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
                mShowChipDetailed = a.getBoolean(R.styleable.ChipsInput_showChipDetailed, true);
                // chip detailed text color
            } finally {
                a.recycle();
            }
        }

        ChipsLayoutManager chipsLayoutManager = ChipsLayoutManager.newBuilder(mContext)
                .setOrientation(ChipsLayoutManager.HORIZONTAL)
                .build();
        chipsRecyclerView.setLayoutManager(chipsLayoutManager);
        chipsRecyclerView.setNestedScrollingEnabled(false);

        setupClickOutsideCallback();
    }

    public void setChipsAdapter(ChipsAdapter<T, ?> chipsAdapter) {
        this.chipsAdapter = chipsAdapter;
        chipsRecyclerView.setAdapter(chipsAdapter);
    }

    private void setupClickOutsideCallback() {
        Activity activity = ActivityUtil.scanForActivity(mContext);
        if (activity == null) {
            throw new ClassCastException("android.view.Context cannot be cast to android.app.Activity");
        }

        android.view.Window.Callback originalWindowCallback = (activity).getWindow().getCallback();
        activity.getWindow().setCallback(new ClickOutsideCallback(originalWindowCallback, activity));
    }

    private void initEditText() {
        chipsInputEditText = new ChipsInputEditText(mContext);
        if (mHintColor != null)
            chipsInputEditText.setHintTextColor(mHintColor);
        if (mTextColor != null)
            chipsInputEditText.setTextColor(mTextColor);

        chipsInputEditText.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        chipsInputEditText.setHint(mHint);
        chipsInputEditText.setBackgroundResource(android.R.color.transparent);
        // prevent fullscreen on landscape
        chipsInputEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_DONE);
        chipsInputEditText.setPrivateImeOptions("nm");
        // no suggestion
        chipsInputEditText.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // handle back space
        chipsInputEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // backspace
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    // remove last chip
                    if (chipsInputEditText.getText().toString().length() == 0)
                        chipsAdapter.removeLastChip();
                }
                return false;
            }
        });

        chipsInputEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId == EditorInfo.IME_ACTION_DONE) || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    ChipsInput.this.onActionDone(chipsInputEditText.getText().toString());
                }
                return false;
            }
        });

        // text changed
        chipsInputEditText.addTextChangedListener(new TextWatcher() {
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

    public void addChips(List<T> chips) {
        chipsAdapter.addChipsProgrammatically(chips);
    }

    public ChipsInputEditText getEditText() {
        return chipsInputEditText;
    }

    public void addChipsListener(ChipsListener<T> chipsListener) {
        mChipsListenerList.add(chipsListener);
    }

    public void onChipAdded(T chip, int size) {
        filterableAdapter.hideItem(chip);
        for (ChipsListener<T> chipsListener : mChipsListenerList) {
            chipsListener.onChipAdded(chip, size);
        }
    }

    public void onChipRemoved(T chip, int size) {
        filterableAdapter.unhideItem(chip);
        for (ChipsListener<T> chipsListener : mChipsListenerList) {
            chipsListener.onChipRemoved(chip, size);
        }
    }

    public void onTextChanged(CharSequence text) {
        for (ChipsListener chipsListener : mChipsListenerList) {
            chipsListener.onTextChanged(text);
        }

        mDropdownListView.getRecyclerView().scrollToPosition(0);

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
        mDropdownListView.getRecyclerView().scrollToPosition(0);
    }

    public List<T> getSelectedChipList() {
        return chipsAdapter.getChipList();
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

    public ChipsInput setShowChipDetailed(boolean mShowChipDetailed) {
        this.mShowChipDetailed = mShowChipDetailed;
        return this;
    }

    public boolean isShowChipDetailed() {
        return mShowChipDetailed;
    }

    public void setFilterableListLayout(ViewGroup layout) {
        this.filterableListLayout = layout;
    }

    public RecyclerView getChipsRecyclerView() {
        return chipsRecyclerView;
    }

    public abstract static class ChipDropdownAdapter<T extends FilterableItem, VH extends ViewHolder>
            extends FilterableAdapter<T, VH> {
        public ChipDropdownAdapter(List<? extends T> itemList) {
            super(itemList);
        }
    }

    public void setChipDropdownAdapter(final ChipDropdownAdapter<T, ?> filterableAdapter) {
        this.filterableAdapter = filterableAdapter;
        if (filterableListLayout != null) {
            mDropdownListView = new DropdownListView(mContext, filterableListLayout);
        } else {
            mDropdownListView = new DropdownListView(mContext, this);
        }
        mDropdownListView.build(filterableAdapter);
        chipsInputEditText.setFilterableListView(mDropdownListView);
        mDropdownListView.getRecyclerView().addOnItemTouchListener(new RecyclerItemClickListener(getContext(), new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                T item = filterableAdapter.getItem(position);
                chipsAdapter.addChip(item);
            }
        }));
    }

    public interface ChipsListener<T extends FilterableItem> {
        void onChipAdded(T chip, int newSize);
        void onChipRemoved(T chip, int newSize);
        void onTextChanged(CharSequence text);
        void onActionDone(CharSequence text);
    }

    public static abstract class SimpleChipsListener<T extends FilterableItem> implements ChipsListener<T> {
        public void onChipAdded(T chip, int newSize) { }
        public void onChipRemoved(T chip, int newSize) { }
        public void onTextChanged(CharSequence text) { }
        public void onActionDone(CharSequence text) { }
    }
}
