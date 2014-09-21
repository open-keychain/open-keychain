/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;

/**
 * Hack to re-enable keyboard auto correction in AutoCompleteTextView.
 * From http://stackoverflow.com/a/22512858
 */
public class AutoCorrectAutoCompleteTextView extends AutoCompleteTextView {

    public AutoCorrectAutoCompleteTextView(Context context) {
        super(context);
        removeFlag();
    }

    public AutoCorrectAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        removeFlag();
    }

    public AutoCorrectAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        removeFlag();
    }

    private void removeFlag() {
        int inputType = getInputType();
        inputType &= ~EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE;
        setRawInputType(inputType);
    }
}
