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

import java.util.ArrayList;
import java.util.List;

import org.thialfihar.android.apg.integration.ApgData;
import org.thialfihar.android.apg.integration.ApgIntentHelper;
import org.thialfihar.android.apg.service.IApgKeyService;
import org.thialfihar.android.apg.service.handler.IApgGetKeyringsHandler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;

public class AidlDemoActivity2 extends Activity {
    Activity mActivity;

    TextView mKeyringsTextView;

    ApgIntentHelper mApgIntentHelper;
    ApgData mApgData;

    byte[] keysBytes;
    ArrayList<String> keysStrings;

    private IApgKeyService service = null;
    private ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = IApgKeyService.Stub.asInterface(binder);
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.aidl_demo2);

        mActivity = this;

        mKeyringsTextView = (TextView) findViewById(R.id.aidl_demo_keyrings);

        mApgIntentHelper = new ApgIntentHelper(mActivity);
        mApgData = new ApgData();

        bindService(new Intent("org.thialfihar.android.apg.service.IApgKeyService"), svcConn,
                Context.BIND_AUTO_CREATE);
    }

    public void getKeyringsStringsOnClick(View view) {
        try {
            service.getPublicKeyRings(mApgData.getPublicKeys(), true, getKeyringsHandler);
        } catch (RemoteException e) {
            exceptionImplementation(-1, e.toString());
        }
    }

    public void getKeyringsBytesOnClick(View view) {
        try {
            service.getPublicKeyRings(mApgData.getPublicKeys(), false, getKeyringsHandler);
        } catch (RemoteException e) {
            exceptionImplementation(-1, e.toString());
        }
    }

    @SuppressLint("NewApi")
    private void updateView() {
        if (keysBytes != null) {
            mKeyringsTextView.setText(Base64.encodeToString(keysBytes, Base64.DEFAULT));
        } else if (keysStrings != null) {
            mKeyringsTextView.setText("");
            for (String output : keysStrings) {
                mKeyringsTextView.append(output);
            }
        }
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

    private final IApgGetKeyringsHandler.Stub getKeyringsHandler = new IApgGetKeyringsHandler.Stub() {

        @Override
        public void onException(final int exceptionId, final String message) throws RemoteException {
            runOnUiThread(new Runnable() {
                public void run() {
                    exceptionImplementation(exceptionId, message);
                }
            });
        }

        @Override
        public void onSuccess(final byte[] outputBytes, final List<String> outputStrings)
                throws RemoteException {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (outputBytes != null) {
                        keysBytes = outputBytes;
                        keysStrings = null;
                    } else if (outputStrings != null) {
                        keysBytes = null;
                        keysStrings = (ArrayList<String>) outputStrings;
                    }
                    updateView();
                }
            });

        }

    };

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
