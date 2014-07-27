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
import android.net.Uri;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.devspark.appmsg.AppMsg;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ContactHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;

import java.util.regex.Matcher;

public class CreateKeyActivity extends ActionBarActivity {

    AutoCompleteTextView mNameEdit;
    AutoCompleteTextView mEmailEdit;
    EditText mPassphraseEdit;
    View mCreateButton;
    CheckBox mUploadCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.create_key_activity);

        mNameEdit = (AutoCompleteTextView) findViewById(R.id.name);
        mEmailEdit = (AutoCompleteTextView) findViewById(R.id.email);
        mPassphraseEdit = (EditText) findViewById(R.id.passphrase);
        mCreateButton = findViewById(R.id.create_key_button);
        mUploadCheckbox = (CheckBox) findViewById(R.id.create_key_upload);

        mEmailEdit.setThreshold(1); // Start working from first character
        mEmailEdit.setAdapter(
                new ArrayAdapter<String>
                        (this, android.R.layout.simple_spinner_dropdown_item,
                                ContactHelper.getPossibleUserEmails(this)
                        )
        );
        mEmailEdit.addTextChangedListener(new TextWatcher() {
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
                        mEmailEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.uid_mail_ok, 0);
                    } else {
                        mEmailEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.uid_mail_bad, 0);
                    }
                } else {
                    // remove drawable if email is empty
                    mEmailEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }
            }
        });

        mNameEdit.setThreshold(1); // Start working from first character
        mNameEdit.setAdapter(
                new ArrayAdapter<String>
                        (this, android.R.layout.simple_spinner_dropdown_item,
                                ContactHelper.getPossibleUserNames(this)
                        )
        );

        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createKeyCheck();
            }
        });

    }

    private void createKeyCheck() {
        if (isEditTextNotEmpty(this, mNameEdit)
                && isEditTextNotEmpty(this, mEmailEdit)
                && isEditTextNotEmpty(this, mPassphraseEdit)) {
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

                // TODO
//                if (mUploadCheckbox.isChecked()) {
//                    uploadKey();
//                } else {
                    if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                        CreateKeyActivity.this.setResult(RESULT_OK);
                        CreateKeyActivity.this.finish();
                    }
//                }
            }
        };

        // fill values for this action
        Bundle data = new Bundle();

        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Constants.choice.algorithm.rsa, 4096, KeyFlags.CERTIFY_OTHER, null));
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Constants.choice.algorithm.rsa, 4096, KeyFlags.SIGN_DATA, null));
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(Constants.choice.algorithm.rsa, 4096, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE, null));
        String userId = mNameEdit.getText().toString() + " <" + mEmailEdit.getText().toString() + ">";
        parcel.mAddUserIds.add(userId);
        parcel.mNewPassphrase = mPassphraseEdit.getText().toString();

        // get selected key entries
        data.putParcelable(KeychainIntentService.SAVE_KEYRING_PARCEL, parcel);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        saveHandler.showProgressDialog(this);

        startService(intent);
    }

    private void uploadKey() {
        // Send all information needed to service to upload key in other thread
        Intent intent = new Intent(this, KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_UPLOAD_KEYRING);

        // set data uri as path to keyring
        // TODO
//        Uri blobUri = KeychainContract.KeyRingData.buildPublicKeyRingUri(mDataUri);
//        intent.setData(blobUri);

        // fill values for this action
        Bundle data = new Bundle();

        Spinner keyServer = (Spinner) findViewById(R.id.upload_key_keyserver);
        String server = (String) keyServer.getSelectedItem();
        data.putString(KeychainIntentService.UPLOAD_KEY_SERVER, server);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after uploading is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(this,
                getString(R.string.progress_exporting), ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    AppMsg.makeText(CreateKeyActivity.this, R.string.key_send_success,
                            AppMsg.STYLE_INFO).show();

                    CreateKeyActivity.this.setResult(RESULT_OK);
                    CreateKeyActivity.this.finish();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(this);

        // start service with intent
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
            editText.setError(context.getString(R.string.create_key_empty));
            editText.requestFocus();
            output = false;
        } else {
            editText.setError(null);
        }

        return output;
    }
}
