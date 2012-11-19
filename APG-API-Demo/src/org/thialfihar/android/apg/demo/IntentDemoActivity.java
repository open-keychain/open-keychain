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

package org.thialfihar.android.apg.demo;

import org.thialfihar.android.apg.demo.R;
import org.thialfihar.android.apg.integration.ApgData;
import org.thialfihar.android.apg.integration.ApgIntentHelper;

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

    ApgIntentHelper mApgIntentHelper;
    ApgData mApgData;

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

        mApgIntentHelper = new ApgIntentHelper(mActivity);
        mApgData = new ApgData();
    }

    public void createNewKeyOnClick(View view) {
        // mApgIntentHelper.createNewKey();
        mApgIntentHelper.createNewKey("test <user@example.com>", true, true);
    }

    public void selectSecretKeyOnClick(View view) {
        mApgIntentHelper.selectSecretKey();
    }

    public void selectEncryptionKeysOnClick(View view) {
        mApgIntentHelper.selectEncryptionKeys("user@example.com");
    }

    public void encryptOnClick(View view) {
        mApgIntentHelper.encrypt(mMessageTextView.getText().toString(),
                mApgData.getEncryptionKeys(), mApgData.getSignatureKeyId(), false);
    }

    public void encryptAndReturnOnClick(View view) {
        mApgIntentHelper.encrypt(mMessageTextView.getText().toString(),
                mApgData.getEncryptionKeys(), mApgData.getSignatureKeyId(), true);
    }

    public void decryptOnClick(View view) {
        mApgIntentHelper.decrypt(mCiphertextTextView.getText().toString(), false);
    }

    public void decryptAndReturnOnClick(View view) {
        mApgIntentHelper.decrypt(mCiphertextTextView.getText().toString(), true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // this updates the mApgData object to the result of the methods
        boolean result = mApgIntentHelper.onActivityResult(requestCode, resultCode, data, mApgData);
        if (result) {
            updateView();
        }

        // continue with other activity results
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateView() {
        if (mApgData.getDecryptedData() != null) {
            mMessageTextView.setText(mApgData.getDecryptedData());
        }
        if (mApgData.getEncryptedData() != null) {
            mCiphertextTextView.setText(mApgData.getEncryptedData());
        }
        mDataTextView.setText(mApgData.toString());
    }
}
