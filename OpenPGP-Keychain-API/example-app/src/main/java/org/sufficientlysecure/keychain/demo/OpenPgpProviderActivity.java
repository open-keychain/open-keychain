/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpConstants;
import org.openintents.openpgp.util.OpenPgpServiceConnection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class OpenPgpProviderActivity extends Activity {
    EditText mMessage;
    EditText mCiphertext;
    EditText mEncryptUserIds;
    Button mSign;
    Button mEncrypt;
    Button mSignAndEncrypt;
    Button mDecryptAndVerify;

    OpenPgpServiceConnection mCryptoServiceConnection;

    public static final int REQUEST_CODE_SIGN = 9910;
    public static final int REQUEST_CODE_ENCRYPT = 9911;
    public static final int REQUEST_CODE_SIGN_AND_ENCRYPT = 9912;
    public static final int REQUEST_CODE_DECRYPT_AND_VERIFY = 9913;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.openpgp_provider);

        mMessage = (EditText) findViewById(R.id.crypto_provider_demo_message);
        mCiphertext = (EditText) findViewById(R.id.crypto_provider_demo_ciphertext);
        mEncryptUserIds = (EditText) findViewById(R.id.crypto_provider_demo_encrypt_user_id);
        mSign = (Button) findViewById(R.id.crypto_provider_demo_sign);
        mEncrypt = (Button) findViewById(R.id.crypto_provider_demo_encrypt);
        mSignAndEncrypt = (Button) findViewById(R.id.crypto_provider_demo_sign_and_encrypt);
        mDecryptAndVerify = (Button) findViewById(R.id.crypto_provider_demo_decrypt_and_verify);

        mSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sign(new Bundle());
            }
        });
        mEncrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encrypt(new Bundle());
            }
        });
        mSignAndEncrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signAndEncrypt(new Bundle());
            }
        });
        mDecryptAndVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptAndVerify(new Bundle());
            }
        });

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String providerPackageName = settings.getString("openpgp_provider_list", "");
        if (TextUtils.isEmpty(providerPackageName)) {
            Toast.makeText(this, "No OpenPGP Provider selected!", Toast.LENGTH_LONG).show();
            finish();
        } else {
            // bind to service
            mCryptoServiceConnection = new OpenPgpServiceConnection(
                    OpenPgpProviderActivity.this, providerPackageName);
            mCryptoServiceConnection.bindToService();
        }
    }

    private void handleError(final OpenPgpError error) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(OpenPgpProviderActivity.this,
                        "onError id:" + error.getErrorId() + "\n\n" + error.getMessage(),
                        Toast.LENGTH_LONG).show();
                Log.e(Constants.TAG, "onError getErrorId:" + error.getErrorId());
                Log.e(Constants.TAG, "onError getMessage:" + error.getMessage());
            }
        });
    }

    private InputStream getInputstream(boolean ciphertext) {
        InputStream is = null;
        try {
            String inputStr = null;
            if (ciphertext) {
                inputStr = mCiphertext.getText().toString();
            } else {
                inputStr = mMessage.getText().toString();
            }
            is = new ByteArrayInputStream(inputStr.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return is;
    }

    private class MyCallback implements OpenPgpApi.IOpenPgpCallback {
        boolean returnToCiphertextField;
        ByteArrayOutputStream os;
        int requestCode;

        private MyCallback(boolean returnToCiphertextField, ByteArrayOutputStream os, int requestCode) {
            this.returnToCiphertextField = returnToCiphertextField;
            this.os = os;
            this.requestCode = requestCode;
        }

        @Override
        public void onReturn(Bundle result) {
            switch (result.getInt(OpenPgpConstants.RESULT_CODE)) {
                case OpenPgpConstants.RESULT_CODE_SUCCESS: {
                    try {
                        Log.d(OpenPgpConstants.TAG, "result: " + os.toByteArray().length
                                + " str=" + os.toString("UTF-8"));

                        if (returnToCiphertextField) {
                            mCiphertext.setText(os.toString("UTF-8"));
                        } else {
                            mMessage.setText(os.toString("UTF-8"));
                        }
                    } catch (UnsupportedEncodingException e) {
                        Log.e(Constants.TAG, "UnsupportedEncodingException", e);
                    }
                    break;
                }
                case OpenPgpConstants.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                    PendingIntent pi = result.getParcelable(OpenPgpConstants.RESULT_INTENT);
                    try {
                        OpenPgpProviderActivity.this.startIntentSenderForResult(pi.getIntentSender(),
                                requestCode, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(Constants.TAG, "SendIntentException", e);
                    }
                    break;
                }
                case OpenPgpConstants.RESULT_CODE_ERROR: {
                    OpenPgpError error = result.getParcelable(OpenPgpConstants.RESULT_ERRORS);
                    handleError(error);
                    break;
                }
            }
        }
    }


    public void sign(Bundle params) {
        params.putBoolean(OpenPgpConstants.PARAMS_REQUEST_ASCII_ARMOR, true);

        InputStream is = getInputstream(false);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        OpenPgpApi api = new OpenPgpApi(this, mCryptoServiceConnection.getService());
        api.sign(params, is, os, new MyCallback(true, os, REQUEST_CODE_SIGN));
    }

    public void encrypt(Bundle params) {
        params.putStringArray(OpenPgpConstants.PARAMS_USER_IDS, mEncryptUserIds.getText().toString().split(","));
        params.putBoolean(OpenPgpConstants.PARAMS_REQUEST_ASCII_ARMOR, true);

        InputStream is = getInputstream(false);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        OpenPgpApi api = new OpenPgpApi(this, mCryptoServiceConnection.getService());
        api.encrypt(params, is, os, new MyCallback(true, os, REQUEST_CODE_ENCRYPT));
    }

    public void signAndEncrypt(Bundle params) {
        params.putStringArray(OpenPgpConstants.PARAMS_USER_IDS, mEncryptUserIds.getText().toString().split(","));
        params.putBoolean(OpenPgpConstants.PARAMS_REQUEST_ASCII_ARMOR, true);

        InputStream is = getInputstream(false);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        OpenPgpApi api = new OpenPgpApi(this, mCryptoServiceConnection.getService());
        api.signAndEncrypt(params, is, os, new MyCallback(true, os, REQUEST_CODE_SIGN_AND_ENCRYPT));
    }

    public void decryptAndVerify(Bundle params) {
        params.putBoolean(OpenPgpConstants.PARAMS_REQUEST_ASCII_ARMOR, true);

        InputStream is = getInputstream(true);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        OpenPgpApi api = new OpenPgpApi(this, mCryptoServiceConnection.getService());
        api.decryptAndVerify(params, is, os, new MyCallback(true, os, REQUEST_CODE_DECRYPT_AND_VERIFY));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(Constants.TAG, "onActivityResult resultCode: " + resultCode);

        // try again after user interaction
        if (resultCode == RESULT_OK) {
            Bundle params = data.getBundleExtra(OpenPgpConstants.PI_RESULT_PARAMS);

            switch (requestCode) {
                case REQUEST_CODE_SIGN: {
                    sign(params);
                    break;
                }
                case REQUEST_CODE_ENCRYPT: {
                    encrypt(params);
                    break;
                }
                case REQUEST_CODE_SIGN_AND_ENCRYPT: {
                    signAndEncrypt(params);
                    break;
                }
                case REQUEST_CODE_DECRYPT_AND_VERIFY: {
                    decryptAndVerify(params);
                    break;
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mCryptoServiceConnection != null) {
            mCryptoServiceConnection.unbindFromService();
        }
    }

    private static class OpenPgpProviderElement {
        private String packageName;
        private String simpleName;
        private Drawable icon;

        public OpenPgpProviderElement(String packageName, String simpleName, Drawable icon) {
            this.packageName = packageName;
            this.simpleName = simpleName;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return simpleName;
        }
    }

}
