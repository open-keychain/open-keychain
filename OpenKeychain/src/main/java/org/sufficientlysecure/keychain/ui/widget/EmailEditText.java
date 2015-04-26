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
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.ContactHelper;

import java.util.regex.Matcher;

public class EmailEditText extends AppCompatAutoCompleteTextView {

    public EmailEditText(Context context) {
        super(context);
        init();
    }

    public EmailEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EmailEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        reenableKeyboardSuggestions();

        addTextChangedListener(textWatcher);
        initAdapter();
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String email = editable.toString();
            if (email.length() > 0) {
                Matcher emailMatcher = Patterns.EMAIL_ADDRESS.matcher(email);
                if (emailMatcher.matches()) {
                    EmailEditText.this.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                            R.drawable.uid_mail_ok, 0);
                } else {
                    EmailEditText.this.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                            R.drawable.uid_mail_bad, 0);
                }
            } else {
                // remove drawable if email is empty
                EmailEditText.this.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        }
    };

    private void initAdapter() {
        setThreshold(1); // Start working from first character
        setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item,
                ContactHelper.getPossibleUserEmails(getContext())));
    }

    /**
     * Hack to re-enable keyboard auto correction in AutoCompleteTextView.
     * From http://stackoverflow.com/a/22512858
     */
    private void reenableKeyboardSuggestions() {
        int inputType = getInputType();
        inputType &= ~EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE;
        setRawInputType(inputType);
    }
}
