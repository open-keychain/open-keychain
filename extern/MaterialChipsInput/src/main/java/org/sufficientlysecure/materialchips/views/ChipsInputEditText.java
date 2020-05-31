package org.sufficientlysecure.materialchips.views;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;


public class ChipsInputEditText extends androidx.appcompat.widget.AppCompatEditText {

    private View filterableListView;

    public ChipsInputEditText(Context context) {
        super(context);
    }

    public ChipsInputEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean isFilterableListVisible() {
        return filterableListView != null && filterableListView.getVisibility() == VISIBLE;
    }

    public void setFilterableListView(View filterableListView) {
        this.filterableListView = filterableListView;
    }
}
