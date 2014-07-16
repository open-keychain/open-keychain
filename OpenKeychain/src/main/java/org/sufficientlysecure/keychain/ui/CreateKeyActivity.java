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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ContactHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;

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
                createKeyCheck();
            }
        });

    }

    private void createKeyCheck() {
        if (isEditTextNotEmpty(this, nameEdit)
                && isEditTextNotEmpty(this, emailEdit)
                && isEditTextNotEmpty(this, passphraseEdit)) {
            createKey();
        }
    }

    private void createKey() {
        Intent intent = new Intent(this, KeychainIntentService.class);
        intent.setAction(KeychainIntentService.ACTION_SAVE_KEYRING);

        // Message is received after importing is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(
                this,
                getString(R.string.progress_importing),
                ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    CreateKeyActivity.this.finish();
                }
            }
        };

        // fill values for this action
        Bundle data = new Bundle();

        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Constants.choice.algorithm.rsa, 4096, KeyFlags.CERTIFY_OTHER, null));
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Constants.choice.algorithm.rsa, 4096, KeyFlags.SIGN_DATA, null));
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Constants.choice.algorithm.rsa, 4096, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE, null));
        String userId = nameEdit.getText().toString() + " <" + emailEdit.getText().toString() + ">";
        parcel.mAddUserIds.add(userId);
        parcel.mNewPassphrase = passphraseEdit.getText().toString();

        // get selected key entries
        data.putParcelable(KeychainIntentService.SAVE_KEYRING_PARCEL, parcel);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        saveHandler.showProgressDialog(this);

        startService(intent);
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
