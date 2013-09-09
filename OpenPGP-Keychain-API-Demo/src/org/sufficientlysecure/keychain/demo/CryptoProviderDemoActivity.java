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

import java.util.ArrayList;
import java.util.List;

import org.openintents.crypto.CryptoError;
import org.openintents.crypto.CryptoServiceConnection;
import org.openintents.crypto.CryptoSignatureResult;
import org.openintents.crypto.ICryptoCallback;
import org.openintents.crypto.ICryptoService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class CryptoProviderDemoActivity extends Activity {
    Activity mActivity;

    EditText mMessage;
    EditText mCiphertext;
    EditText mEncryptUserIds;

    private CryptoServiceConnection mCryptoServiceConnection;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.crypto_provider_demo);

        mActivity = this;

        mMessage = (EditText) findViewById(R.id.crypto_provider_demo_message);
        mCiphertext = (EditText) findViewById(R.id.crypto_provider_demo_ciphertext);
        mEncryptUserIds = (EditText) findViewById(R.id.crypto_provider_demo_encrypt_user_id);

        selectCryptoProvider();
    }

    /**
     * Callback from remote crypto service
     */
    final ICryptoCallback.Stub encryptCallback = new ICryptoCallback.Stub() {

        @Override
        public void onSuccess(final byte[] outputBytes, CryptoSignatureResult signatureResult)
                throws RemoteException {
            Log.d(Constants.TAG, "encryptCallback");

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mCiphertext.setText(new String(outputBytes));
                }
            });
        }

        @Override
        public void onError(CryptoError error) throws RemoteException {
            handleError(error);
        }

    };

    final ICryptoCallback.Stub decryptAndVerifyCallback = new ICryptoCallback.Stub() {

        @Override
        public void onSuccess(final byte[] outputBytes, final CryptoSignatureResult signatureResult)
                throws RemoteException {
            Log.d(Constants.TAG, "decryptAndVerifyCallback");

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mMessage.setText(new String(outputBytes));
                    if (signatureResult != null) {
                        Toast.makeText(CryptoProviderDemoActivity.this,
                                "signature result:\n" + signatureResult.toString(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            });

        }

        @Override
        public void onError(CryptoError error) throws RemoteException {
            handleError(error);
        }

    };

    private void handleError(final CryptoError error) {
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

    public void encryptOnClick(View view) {
        byte[] inputBytes = mMessage.getText().toString().getBytes();

        try {
            mCryptoServiceConnection.getService().encrypt(inputBytes,
                    mEncryptUserIds.getText().toString().split(","), true, encryptCallback);
        } catch (RemoteException e) {
            Log.e(Constants.TAG, "CryptoProviderDemo", e);
        }
    }

    public void signOnClick(View view) {
        byte[] inputBytes = mMessage.getText().toString().getBytes();

        try {
            mCryptoServiceConnection.getService().sign(inputBytes, true, encryptCallback);
        } catch (RemoteException e) {
            Log.e(Constants.TAG, "CryptoProviderDemo", e);
        }
    }

    public void encryptAndSignOnClick(View view) {
        byte[] inputBytes = mMessage.getText().toString().getBytes();

        try {
            mCryptoServiceConnection.getService().encryptAndSign(inputBytes,
                    mEncryptUserIds.getText().toString().split(","), true, encryptCallback);
        } catch (RemoteException e) {
            Log.e(Constants.TAG, "CryptoProviderDemo", e);
        }
    }

    public void decryptAndVerifyOnClick(View view) {
        byte[] inputBytes = mCiphertext.getText().toString().getBytes();

        try {
            mCryptoServiceConnection.getService().decryptAndVerify(inputBytes,
                    decryptAndVerifyCallback);
        } catch (RemoteException e) {
            Log.e(Constants.TAG, "CryptoProviderDemo", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mCryptoServiceConnection != null) {
            mCryptoServiceConnection.unbindFromService();
        }
    }

    private static class CryptoProviderElement {
        private String packageName;
        private String simpleName;
        private Drawable icon;

        public CryptoProviderElement(String packageName, String simpleName, Drawable icon) {
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
        Intent intent = new Intent(ICryptoService.class.getName());

        final ArrayList<CryptoProviderElement> providerList = new ArrayList<CryptoProviderElement>();

        List<ResolveInfo> resInfo = getPackageManager().queryIntentServices(intent, 0);
        if (!resInfo.isEmpty()) {
            for (ResolveInfo resolveInfo : resInfo) {
                if (resolveInfo.serviceInfo == null)
                    continue;

                String packageName = resolveInfo.serviceInfo.packageName;
                String simpleName = String.valueOf(resolveInfo.serviceInfo
                        .loadLabel(getPackageManager()));
                Drawable icon = resolveInfo.serviceInfo.loadIcon(getPackageManager());
                providerList.add(new CryptoProviderElement(packageName, simpleName, icon));
            }

            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Select Crypto Provider!");
            alert.setCancelable(false);

            if (!providerList.isEmpty()) {

                // Init ArrayAdapter with Crypto Providers
                ListAdapter adapter = new ArrayAdapter<CryptoProviderElement>(this,
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

                        // bind to service
                        mCryptoServiceConnection = new CryptoServiceConnection(
                                CryptoProviderDemoActivity.this, packageName);
                        mCryptoServiceConnection.bindToService();

                        dialog.dismiss();
                    }
                });
            } else {
                alert.setMessage("No Crypto Provider installed!");
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
}
