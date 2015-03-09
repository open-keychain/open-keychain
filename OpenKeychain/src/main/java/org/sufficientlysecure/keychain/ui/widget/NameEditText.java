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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import org.sufficientlysecure.keychain.util.ContactHelper;

public class NameEditText extends AutoCompleteTextView {
    public NameEditText(Context context) {
        super(context);
        init();
    }

    public NameEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NameEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NameEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        removeFlag();
        initAdapter();
    }

    private void initAdapter() {
        setThreshold(1); // Start working from first character
        setAdapter(new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_dropdown_item,
                ContactHelper.getPossibleUserNames(getContext())));
    }

    /**
     * Hack to re-enable keyboard auto correction in AutoCompleteTextView.
     * From http://stackoverflow.com/a/22512858
     */
    private void removeFlag() {
        int inputType = getInputType();
        inputType &= ~EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE;
        setRawInputType(inputType);
    }
}
