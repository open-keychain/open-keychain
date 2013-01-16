/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.demo;

import org.sufficientlysecure.keychain.demo.R;
import org.sufficientlysecure.keychain.integration.KeychainData;
import org.sufficientlysecure.keychain.integration.KeychainIntentHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class IntentDemoActivity extends Activity {
    Activity mActivity;

    TextView mMessageTextView;
    TextView mCiphertextTextView;
    TextView mDataTextView;

    KeychainIntentHelper mKeychainIntentHelper;
    KeychainData mKeychainData;

    /**
     * Instantiate View for this Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.intent_demo);

        mActivity = this;

        mMessageTextView = (TextView) findViewById(R.id.intent_demo_message);
        mCiphertextTextView = (TextView) findViewById(R.id.intent_demo_ciphertext);
        mDataTextView = (TextView) findViewById(R.id.intent_demo_data);

        mKeychainIntentHelper = new KeychainIntentHelper(mActivity);
        mKeychainData = new KeychainData();
    }

    public void createNewKeyOnClick(View view) {
        // mKeychainIntentHelper.createNewKey();
        mKeychainIntentHelper.createNewKey("test <user@example.com>", true, true);
    }

    public void selectSecretKeyOnClick(View view) {
        mKeychainIntentHelper.selectSecretKey();
    }

    public void selectEncryptionKeysOnClick(View view) {
        mKeychainIntentHelper.selectPublicKeys("user@example.com");
    }

    public void encryptOnClick(View view) {
        mKeychainIntentHelper.encrypt(mMessageTextView.getText().toString(),
                mKeychainData.getPublicKeys(), mKeychainData.getSecretKeyId(), false);
    }

    public void encryptAndReturnOnClick(View view) {
        mKeychainIntentHelper.encrypt(mMessageTextView.getText().toString(),
                mKeychainData.getPublicKeys(), mKeychainData.getSecretKeyId(), true);
    }

    public void decryptOnClick(View view) {
        mKeychainIntentHelper.decrypt(mCiphertextTextView.getText().toString(), false);
    }

    public void decryptAndReturnOnClick(View view) {
        mKeychainIntentHelper.decrypt(mCiphertextTextView.getText().toString(), true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // this updates the mKeychainData object to the result of the methods
        boolean result = mKeychainIntentHelper.onActivityResult(requestCode, resultCode, data,
                mKeychainData);
        if (result) {
            updateView();
        }

        // continue with other activity results
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateView() {
        if (mKeychainData.getDecryptedData() != null) {
            mMessageTextView.setText(mKeychainData.getDecryptedData());
        }
        if (mKeychainData.getEncryptedData() != null) {
            mCiphertextTextView.setText(mKeychainData.getEncryptedData());
        }
        mDataTextView.setText(mKeychainData.toString());
    }
}
