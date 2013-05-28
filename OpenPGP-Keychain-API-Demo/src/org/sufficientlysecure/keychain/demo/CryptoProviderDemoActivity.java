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
import org.sufficientlysecure.keychain.integration.Constants;
import org.sufficientlysecure.keychain.integration.KeychainData;
import org.sufficientlysecure.keychain.integration.KeychainIntentHelper;
import org.sufficientlysecure.keychain.service.IKeychainApiService;
import org.sufficientlysecure.keychain.service.IKeychainKeyService;
import org.sufficientlysecure.keychain.service.handler.IKeychainDecryptHandler;
import org.sufficientlysecure.keychain.service.handler.IKeychainEncryptHandler;
import org.sufficientlysecure.keychain.service.handler.IKeychainGetDecryptionKeyIdHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class CryptoProviderDemoActivity extends Activity {
    Activity mActivity;

    TextView mMessageTextView;
    TextView mCiphertextTextView;
    TextView mDataTextView;

    KeychainIntentHelper mKeychainIntentHelper;
    KeychainData mKeychainData;

    private IKeychainApiService service = null;
    private ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = IKeychainApiService.Stub.asInterface(binder);
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.crypto_provider_demo);

        mActivity = this;

        mMessageTextView = (TextView) findViewById(R.id.aidl_demo_message);
        mCiphertextTextView = (TextView) findViewById(R.id.aidl_demo_ciphertext);
        mDataTextView = (TextView) findViewById(R.id.aidl_demo_data);

        mKeychainIntentHelper = new KeychainIntentHelper(mActivity);
        mKeychainData = new KeychainData();

        bindService(new Intent(IKeychainApiService.class.getName()), svcConn,
                Context.BIND_AUTO_CREATE);
    }

    public void registerCryptoProvider(View view) {
        try {
            startActivityForResult(Intent.createChooser(new Intent("com.android.crypto.REGISTER"),
                    "select crypto provider"), 123);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mActivity, "No app that handles com.android.crypto.REGISTER!",
                    Toast.LENGTH_LONG).show();
            Log.e(Constants.TAG, "No app that handles com.android.crypto.REGISTER!");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 123) {
            if (resultCode == RESULT_OK) {
                String packageName = data.getStringExtra("packageName");
                Log.d(Constants.TAG, "packageName: " + packageName);
            }
        }

        // boolean result = mKeychainIntentHelper.onActivityResult(requestCode, resultCode, data,
        // mKeychainData);
        // if (result) {
        // updateView();
        // }

        // continue with other activity results
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void encryptOnClick(View view) {
        byte[] inputBytes = mMessageTextView.getText().toString().getBytes();

        try {
            service.encryptAsymmetric(inputBytes, null, true, 0, mKeychainData.getPublicKeys(), 7,
                    encryptHandler);
        } catch (RemoteException e) {
            exceptionImplementation(-1, e.toString());
        }
    }

    public void decryptOnClick(View view) {
        byte[] inputBytes = mCiphertextTextView.getText().toString().getBytes();

        try {
            service.decryptAndVerifyAsymmetric(inputBytes, null, null, decryptHandler);
        } catch (RemoteException e) {
            exceptionImplementation(-1, e.toString());
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        unbindService(svcConn);
    }

    private void exceptionImplementation(int exceptionId, String error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exception!").setMessage(error).setPositiveButton("OK", null).show();
    }

    private final IKeychainEncryptHandler.Stub encryptHandler = new IKeychainEncryptHandler.Stub() {

        @Override
        public void onException(final int exceptionId, final String message) throws RemoteException {
            runOnUiThread(new Runnable() {
                public void run() {
                    exceptionImplementation(exceptionId, message);
                }
            });
        }

        @Override
        public void onSuccess(final byte[] outputBytes, String outputUri) throws RemoteException {
            runOnUiThread(new Runnable() {
                public void run() {
                    mKeychainData.setEncryptedData(new String(outputBytes));
                    updateView();
                }
            });
        }

    };

    private final IKeychainDecryptHandler.Stub decryptHandler = new IKeychainDecryptHandler.Stub() {

        @Override
        public void onException(final int exceptionId, final String message) throws RemoteException {
            runOnUiThread(new Runnable() {
                public void run() {
                    exceptionImplementation(exceptionId, message);
                }
            });
        }

        @Override
        public void onSuccess(final byte[] outputBytes, String outputUri, boolean signature,
                long signatureKeyId, String signatureUserId, boolean signatureSuccess,
                boolean signatureUnknown) throws RemoteException {
            runOnUiThread(new Runnable() {
                public void run() {
                    mKeychainData.setDecryptedData(new String(outputBytes));
                    updateView();
                }
            });

        }

    };

    private final IKeychainGetDecryptionKeyIdHandler.Stub helperHandler = new IKeychainGetDecryptionKeyIdHandler.Stub() {

        @Override
        public void onException(final int exceptionId, final String message) throws RemoteException {
            runOnUiThread(new Runnable() {
                public void run() {
                    exceptionImplementation(exceptionId, message);
                }
            });
        }

        @Override
        public void onSuccess(long arg0, boolean arg1) throws RemoteException {
            // TODO Auto-generated method stub

        }

    };

    /**
     * Selection is done with Intents, not AIDL!
     * 
     * @param view
     */
    public void selectSecretKeyOnClick(View view) {
        mKeychainIntentHelper.selectSecretKey();
    }

    public void selectEncryptionKeysOnClick(View view) {
        mKeychainIntentHelper.selectPublicKeys("user@example.com");

    }

}
