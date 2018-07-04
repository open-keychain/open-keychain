package org.sufficientlysecure.materialchips.simple;


import java.util.List;

import android.content.Context;
import android.util.AttributeSet;

import org.sufficientlysecure.materialchips.ChipsInput;


public class SimpleChipsInput extends ChipsInput<SimpleChip> {
    public SimpleChipsInput(Context context) {
        super(context);
        init();
    }

    public SimpleChipsInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        SimpleChipsAdapter chipsAdapter = new SimpleChipsAdapter(getContext(), this);
        setChipsAdapter(chipsAdapter);
    }

    public void setData(List<SimpleChip> simpleChips) {
        SimpleChipDropdownAdapter chipDropdownAdapter = new SimpleChipDropdownAdapter(getContext(), simpleChips);
        setChipDropdownAdapter(chipDropdownAdapter);
    }
}
