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
import org.sufficientlysecure.keychain.demo.R;
import org.sufficientlysecure.keychain.integration.Constants;

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
import android.widget.ListAdapter;
import android.widget.TextView;

public class CryptoProviderDemoActivity extends Activity {
    Activity mActivity;

    TextView mMessageTextView;
    TextView mCiphertextTextView;
    TextView mDataTextView;

    private CryptoServiceConnection mCryptoServiceConnection;

    private static final String CRYPTO_SERVICE_INTENT = "org.openintents.crypto.ICryptoService";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.crypto_provider_demo);

        mActivity = this;

        mMessageTextView = (TextView) findViewById(R.id.crypto_provider_demo_message);
        mCiphertextTextView = (TextView) findViewById(R.id.crypto_provider_demo_ciphertext);
        mDataTextView = (TextView) findViewById(R.id.aidl_demo_data);

        selectCryptoProvider();
    }

    /**
     * Callback from remote crypto service
     */
    final ICryptoCallback.Stub callback = new ICryptoCallback.Stub() {

        @Override
        public void onEncryptSignSuccess(byte[] outputBytes) throws RemoteException {
            // not needed here
        }

        @Override
        public void onDecryptVerifySuccess(byte[] outputBytes, CryptoSignatureResult signatureResult)
                throws RemoteException {
            Log.d(Constants.TAG, "onDecryptVerifySuccess");

            // PgpData data = new PgpData();
            // data.setDecryptedData(new String(outputBytes));
            // mFragment.setMessageWithPgpData(data);
        }

        @Override
        public void onError(CryptoError error) throws RemoteException {
            Log.e(Constants.TAG, "onError getErrorId:" + error.getErrorId());
            Log.e(Constants.TAG, "onError getErrorId:" + error.getMessage());
        }

    };

    public void encryptOnClick(View view) {
        byte[] inputBytes = mMessageTextView.getText().toString().getBytes();

        try {
            mCryptoServiceConnection.getService().encrypt(inputBytes,
                    new String[] { "dominik@dominikschuermann.de" }, callback);
        } catch (RemoteException e) {
            Log.e(Constants.TAG, "CryptoProviderDemo", e);
        }
    }

    public void decryptOnClick(View view) {
        byte[] inputBytes = mCiphertextTextView.getText().toString().getBytes();

        try {
            mCryptoServiceConnection.getService().decryptAndVerify(inputBytes, callback);
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
        Intent intent = new Intent(CRYPTO_SERVICE_INTENT);

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
