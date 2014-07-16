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

package org.sufficientlysecure.keychain.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ContactHelper;

import java.util.regex.Matcher;

public class CreateKeyActivity extends ActionBarActivity {

    AutoCompleteTextView nameEdit;
    AutoCompleteTextView emailEdit;
    EditText passphraseEdit;
    Button createButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.create_key_activity);

        nameEdit = (AutoCompleteTextView) findViewById(R.id.name);
        emailEdit = (AutoCompleteTextView) findViewById(R.id.email);
        passphraseEdit = (EditText) findViewById(R.id.passphrase);
        createButton = (Button) findViewById(R.id.create_key_button);

        emailEdit.setThreshold(1); // Start working from first character
        emailEdit.setAdapter(
                new ArrayAdapter<String>
                        (this, android.R.layout.simple_spinner_dropdown_item,
                                ContactHelper.getPossibleUserEmails(this)
                        )
        );
        emailEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String email = editable.toString();
                if (email.length() > 0) {
                    Matcher emailMatcher = Patterns.EMAIL_ADDRESS.matcher(email);
                    if (emailMatcher.matches()) {
                        emailEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.uid_mail_ok, 0);
                    } else {
                        emailEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.uid_mail_bad, 0);
                    }
                } else {
                    // remove drawable if email is empty
                    emailEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }
            }
        });
        nameEdit.setThreshold(1); // Start working from first character
        nameEdit.setAdapter(
                new ArrayAdapter<String>
                        (this, android.R.layout.simple_spinner_dropdown_item,
                                ContactHelper.getPossibleUserNames(this)
                        )
        );

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createKey();
            }
        });

    }

    private void createKey() {
        if (isEditTextNotEmpty(this, nameEdit)
                && isEditTextNotEmpty(this, emailEdit)
                && isEditTextNotEmpty(this, passphraseEdit)) {

        }
    }

    /**
     * Checks if text of given EditText is not empty. If it is empty an error is
     * set and the EditText gets the focus.
     *
     * @param context
     * @param editText
     * @return true if EditText is not empty
     */
    private static boolean isEditTextNotEmpty(Context context, EditText editText) {
        boolean output = true;
        if (editText.getText().toString().length() == 0) {
            editText.setError("empty!");
            editText.requestFocus();
            output = false;
        } else {
            editText.setError(null);
        }

        return output;
    }
}
