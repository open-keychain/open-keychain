/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.openintents.openpgp.IOpenPgpService;
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
    Activity mActivity;

    EditText mMessage;
    EditText mCiphertext;
    EditText mEncryptUserIds;
    Button mSign;
    Button mEncrypt;
    Button mSignAndEncrypt;
    Button mDecryptAndVerify;

    private OpenPgpServiceConnection mCryptoServiceConnection;

    public static final int REQUEST_CODE_SIGN = 9910;
    public static final int REQUEST_CODE_ENCRYPT = 9911;
    public static final int REQUEST_CODE_SIGN_AND_ENCRYPT = 9912;
    public static final int REQUEST_CODE_DECRYPT_AND_VERIFY = 9913;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.openpgp_provider);

        mActivity = this;

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

        selectCryptoProvider();
    }

    private void handleError(final OpenPgpError error) {
        mActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(mActivity,
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


    public void sign(Bundle params) {
        params.putBoolean(OpenPgpConstants.PARAMS_REQUEST_ASCII_ARMOR, true);

        InputStream is = getInputstream(false);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        OpenPgpApi api = new OpenPgpApi(mCryptoServiceConnection.getService());
        api.sign(params, is, os, new OpenPgpApi.IOpenPgpCallback() {
            @Override
            public void onReturn(Bundle result) {
                switch (result.getInt(OpenPgpConstants.RESULT_CODE)) {
                    case OpenPgpConstants.RESULT_CODE_SUCCESS: {
                        try {
                            Log.d(OpenPgpConstants.TAG, "result: " + os.toByteArray().length
                                    + " str=" + os.toString("UTF-8"));

                            mCiphertext.setText(os.toString("UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            Log.e(Constants.TAG, "UnsupportedEncodingException", e);
                        }
                        break;
                    }
                    case OpenPgpConstants.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                        PendingIntent pi = result.getParcelable(OpenPgpConstants.RESULT_INTENT);
                        try {
                            OpenPgpProviderActivity.this.startIntentSenderForResult(pi.getIntentSender(),
                                    REQUEST_CODE_SIGN, null,
                                    0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(Constants.TAG, "SendIntentException", e);
                        }
                        break;
                    }
                }
            }
        });
    }

    public void encrypt(Bundle params) {
        params.putStringArray(OpenPgpConstants.PARAMS_USER_IDS, mEncryptUserIds.getText().toString().split(","));
        params.putBoolean(OpenPgpConstants.PARAMS_REQUEST_ASCII_ARMOR, true);

        InputStream is = getInputstream(false);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        OpenPgpApi api = new OpenPgpApi(mCryptoServiceConnection.getService());
        api.encrypt(params, is, os, new OpenPgpApi.IOpenPgpCallback() {
            @Override
            public void onReturn(Bundle result) {
                switch (result.getInt(OpenPgpConstants.RESULT_CODE)) {
                    case OpenPgpConstants.RESULT_CODE_SUCCESS: {
                        try {
                            Log.d(OpenPgpConstants.TAG, "result: " + os.toByteArray().length
                                    + " str=" + os.toString("UTF-8"));

                            mCiphertext.setText(os.toString("UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            Log.e(Constants.TAG, "UnsupportedEncodingException", e);
                        }
                        break;
                    }
                    case OpenPgpConstants.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                        PendingIntent pi = result.getParcelable(OpenPgpConstants.RESULT_INTENT);
                        try {
                            OpenPgpProviderActivity.this.startIntentSenderForResult(pi.getIntentSender(),
                                    REQUEST_CODE_ENCRYPT, null,
                                    0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(Constants.TAG, "SendIntentException", e);
                        }
                        break;
                    }
                }
            }
        });
    }

    public void signAndEncrypt(Bundle params) {
        params.putStringArray(OpenPgpConstants.PARAMS_USER_IDS, mEncryptUserIds.getText().toString().split(","));
        params.putBoolean(OpenPgpConstants.PARAMS_REQUEST_ASCII_ARMOR, true);

        InputStream is = getInputstream(false);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        OpenPgpApi api = new OpenPgpApi(mCryptoServiceConnection.getService());
        api.signAndEncrypt(params, is, os, new OpenPgpApi.IOpenPgpCallback() {
            @Override
            public void onReturn(Bundle result) {
                switch (result.getInt(OpenPgpConstants.RESULT_CODE)) {
                    case OpenPgpConstants.RESULT_CODE_SUCCESS: {
                        try {
                            Log.d(OpenPgpConstants.TAG, "result: " + os.toByteArray().length
                                    + " str=" + os.toString("UTF-8"));

                            mCiphertext.setText(os.toString("UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            Log.e(Constants.TAG, "UnsupportedEncodingException", e);
                        }
                        break;
                    }
                    case OpenPgpConstants.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                        PendingIntent pi = result.getParcelable(OpenPgpConstants.RESULT_INTENT);
                        try {
                            OpenPgpProviderActivity.this.startIntentSenderForResult(pi.getIntentSender(),
                                    REQUEST_CODE_SIGN_AND_ENCRYPT, null,
                                    0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(Constants.TAG, "SendIntentException", e);
                        }
                        break;
                    }
                }
            }
        });
    }

    public void decryptAndVerify(Bundle params) {
        params.putBoolean(OpenPgpConstants.PARAMS_REQUEST_ASCII_ARMOR, true);

        InputStream is = getInputstream(true);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        OpenPgpApi api = new OpenPgpApi(mCryptoServiceConnection.getService());
        api.decryptAndVerify(params, is, os, new OpenPgpApi.IOpenPgpCallback() {
            @Override
            public void onReturn(Bundle result) {
                switch (result.getInt(OpenPgpConstants.RESULT_CODE)) {
                    case OpenPgpConstants.RESULT_CODE_SUCCESS: {
                        try {
                            Log.d(OpenPgpConstants.TAG, "result: " + os.toByteArray().length
                                    + " str=" + os.toString("UTF-8"));

                            mMessage.setText(os.toString("UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            Log.e(Constants.TAG, "UnsupportedEncodingException", e);
                        }
                        break;
                    }
                    case OpenPgpConstants.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                        PendingIntent pi = result.getParcelable(OpenPgpConstants.RESULT_INTENT);
                        try {
                            OpenPgpProviderActivity.this.startIntentSenderForResult(pi.getIntentSender(),
                                    REQUEST_CODE_DECRYPT_AND_VERIFY, null,
                                    0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(Constants.TAG, "SendIntentException", e);
                        }
                        break;
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(Constants.TAG, "onActivityResult");
        switch (requestCode) {
            case REQUEST_CODE_SIGN: {
                Log.d(Constants.TAG, "resultCode: " + resultCode);

                // try to sign again after password caching
                if (resultCode == RESULT_OK) {
                    sign(data.getExtras());
                }
                break;
            }
            case REQUEST_CODE_ENCRYPT: {
                Log.d(Constants.TAG, "resultCode: " + resultCode);

                // try to sign again after password caching
                if (resultCode == RESULT_OK) {
                    // use data extras now as params for call (they now include key ids!
                    encrypt(data.getExtras());
                }
                break;
            }
            case REQUEST_CODE_SIGN_AND_ENCRYPT: {
                Log.d(Constants.TAG, "resultCode: " + resultCode);

                // try to sign again after password caching
                if (resultCode == RESULT_OK) {
                    signAndEncrypt(data.getExtras());
                }
                break;
            }
            case REQUEST_CODE_DECRYPT_AND_VERIFY: {
                Log.d(Constants.TAG, "resultCode: " + resultCode);

                // try to sign again after password caching
                if (resultCode == RESULT_OK) {
                    decryptAndVerify(new Bundle());
                }
                break;
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

    private void selectCryptoProvider() {
        Intent intent = new Intent(IOpenPgpService.class.getName());

        final ArrayList<OpenPgpProviderElement> providerList = new ArrayList<OpenPgpProviderElement>();

        List<ResolveInfo> resInfo = getPackageManager().queryIntentServices(intent, 0);
        if (!resInfo.isEmpty()) {
            for (ResolveInfo resolveInfo : resInfo) {
                if (resolveInfo.serviceInfo == null)
                    continue;

                String packageName = resolveInfo.serviceInfo.packageName;
                String simpleName = String.valueOf(resolveInfo.serviceInfo
                        .loadLabel(getPackageManager()));
                Drawable icon = resolveInfo.serviceInfo.loadIcon(getPackageManager());
                providerList.add(new OpenPgpProviderElement(packageName, simpleName, icon));
            }
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Select OpenPGP Provider!");
        alert.setCancelable(false);

        if (!providerList.isEmpty()) {
            // add "disable OpenPGP provider"
            providerList.add(0, new OpenPgpProviderElement(null, "Disable OpenPGP Provider",
                    getResources().getDrawable(android.R.drawable.ic_menu_close_clear_cancel)));

            // Init ArrayAdapter with OpenPGP Providers
            ListAdapter adapter = new ArrayAdapter<OpenPgpProviderElement>(this,
                    android.R.layout.select_dialog_item, android.R.id.text1, providerList) {
                public View getView(int position, View convertView, ViewGroup parent) {
                    // User super class to create the View
                    View v = super.getView(position, convertView, parent);
                    TextView tv = (TextView) v.findViewById(android.R.id.text1);

                    // Put the image on the TextView
                    tv.setCompoundDrawablesWithIntrinsicBounds(providerList.get(position).icon,
                            null, null, null);

                    // Add margin between image and text (support various screen densities)
                    int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
                    tv.setCompoundDrawablePadding(dp5);

                    return v;
                }
            };

            alert.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int position) {
                    String packageName = providerList.get(position).packageName;

                    if (packageName == null) {
                        dialog.cancel();
                        finish();
                    }

                    // bind to service
                    mCryptoServiceConnection = new OpenPgpServiceConnection(
                            OpenPgpProviderActivity.this, packageName);
                    mCryptoServiceConnection.bindToService();

                    dialog.dismiss();
                }
            });
        } else {
            alert.setMessage("No OpenPGP Provider installed!");
        }

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                finish();
            }
        });

        AlertDialog ad = alert.create();
        ad.show();
    }
}
