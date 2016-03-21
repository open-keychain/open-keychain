/*
 * Copyright (C) 2016 Alex Fong Jie Wen <alexfongg@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui.util.spinner;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;

/**
 * Custom spinner which uses a hack to
 * always set focus on first item in list
 *
 */
public class FocusFirstItemSpinner extends Spinner {
    /**
     *  Spinner is originally designed to set focus on the currently selected item.
     *  When Spinner is selected to show dropdown, 'performClick()' is called internally.
     *  'getSelectedItemPosition()' is then called to obtain the item to focus on.
     *  We use a toggle to have 'getSelectedItemPosition()' return the 0th index
     *  for this particular case.
     */

    private boolean mToggleFlag = true;

    public FocusFirstItemSpinner(Context context, AttributeSet attrs,
                                 int defStyle, int mode) {
        super(context, attrs, defStyle, mode);
    }

    public FocusFirstItemSpinner(Context context, AttributeSet attrs,
                                 int defStyle) {
        super(context, attrs, defStyle);
    }

    public FocusFirstItemSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FocusFirstItemSpinner(Context context, int mode) {
        super(context, mode);
    }

    public FocusFirstItemSpinner(Context context) {
        super(context);
    }

    @Override
    public int getSelectedItemPosition() {
        if (!mToggleFlag) {
            return 0;
        }
        return super.getSelectedItemPosition();
    }

    @Override
    public boolean performClick() {
        mToggleFlag = false;
        boolean result = super.performClick();
        mToggleFlag = true;
        return result;
    }
}
