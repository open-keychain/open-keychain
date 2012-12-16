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

import org.thialfihar.android.apg.integration.ApgData;
import org.thialfihar.android.apg.integration.ApgIntentHelper;
import org.thialfihar.android.apg.service.IApgApiService;
import org.thialfihar.android.apg.service.IApgKeyService;
import org.thialfihar.android.apg.service.handler.IApgDecryptHandler;
import org.thialfihar.android.apg.service.handler.IApgEncryptHandler;
import org.thialfihar.android.apg.service.handler.IApgGetDecryptionKeyIdHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.TextView;

public class AidlDemoActivity extends Activity {
    Activity mActivity;

    TextView mMessageTextView;
    TextView mCiphertextTextView;
    TextView mDataTextView;

    ApgIntentHelper mApgIntentHelper;
    ApgData mApgData;

    private IApgApiService service = null;
    private ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = IApgApiService.Stub.asInterface(binder);
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.aidl_demo);

        mActivity = this;

        mMessageTextView = (TextView) findViewById(R.id.aidl_demo_message);
        mCiphertextTextView = (TextView) findViewById(R.id.aidl_demo_ciphertext);
        mDataTextView = (TextView) findViewById(R.id.aidl_demo_data);

        mApgIntentHelper = new ApgIntentHelper(mActivity);
        mApgData = new ApgData();

        bindService(new Intent(IApgApiService.class.getName()), svcConn, Context.BIND_AUTO_CREATE);
    }

    public void encryptOnClick(View view) {
        byte[] inputBytes = mMessageTextView.getText().toString().getBytes();

        try {
            service.encryptAsymmetric(inputBytes, null, true, 0, mApgData.getPublicKeys(), 7,
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
        if (mApgData.getDecryptedData() != null) {
            mMessageTextView.setText(mApgData.getDecryptedData());
        }
        if (mApgData.getEncryptedData() != null) {
            mCiphertextTextView.setText(mApgData.getEncryptedData());
        }
        mDataTextView.setText(mApgData.toString());
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

    private final IApgEncryptHandler.Stub encryptHandler = new IApgEncryptHandler.Stub() {

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
                    mApgData.setEncryptedData(new String(outputBytes));
                    updateView();
                }
            });
        }

    };

    private final IApgDecryptHandler.Stub decryptHandler = new IApgDecryptHandler.Stub() {

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
                    mApgData.setDecryptedData(new String(outputBytes));
                    updateView();
                }
            });

        }

    };

    private final IApgGetDecryptionKeyIdHandler.Stub helperHandler = new IApgGetDecryptionKeyIdHandler.Stub() {

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
        mApgIntentHelper.selectSecretKey();
    }

    public void selectEncryptionKeysOnClick(View view) {
        mApgIntentHelper.selectPublicKeys("user@example.com");

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
}
