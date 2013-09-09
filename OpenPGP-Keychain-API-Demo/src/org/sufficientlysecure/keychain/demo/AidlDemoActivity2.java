///*
// * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.sufficientlysecure.keychain.demo;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.sufficientlysecure.keychain.demo.R;
//import org.sufficientlysecure.keychain.integration.KeychainData;
//import org.sufficientlysecure.keychain.integration.KeychainIntentHelper;
//import org.sufficientlysecure.keychain.service.IKeychainKeyService;
//import org.sufficientlysecure.keychain.service.handler.IKeychainGetKeyringsHandler;
//
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.Intent;
//import android.content.ServiceConnection;
//import android.os.Bundle;
//import android.os.IBinder;
//import android.os.RemoteException;
//import android.util.Base64;
//import android.view.View;
//import android.widget.TextView;
//
//public class AidlDemoActivity2 extends Activity {
//    Activity mActivity;
//
//    TextView mKeyringsTextView;
//
//    KeychainIntentHelper mKeychainIntentHelper;
//    KeychainData mKeychainData;
//
//    byte[] keysBytes;
//    ArrayList<String> keysStrings;
//
//    private IKeychainKeyService service = null;
//    private ServiceConnection svcConn = new ServiceConnection() {
//        public void onServiceConnected(ComponentName className, IBinder binder) {
//            service = IKeychainKeyService.Stub.asInterface(binder);
//        }
//
//        public void onServiceDisconnected(ComponentName className) {
//            service = null;
//        }
//    };
//
//    @Override
//    public void onCreate(Bundle icicle) {
//        super.onCreate(icicle);
//        setContentView(R.layout.aidl_demo2);
//
//        mActivity = this;
//
//        mKeyringsTextView = (TextView) findViewById(R.id.aidl_demo_keyrings);
//
//        mKeychainIntentHelper = new KeychainIntentHelper(mActivity);
//        mKeychainData = new KeychainData();
//
//        bindService(new Intent(IKeychainKeyService.class.getName()), svcConn,
//                Context.BIND_AUTO_CREATE);
//    }
//
//    public void getKeyringsStringsOnClick(View view) {
//        try {
//            service.getPublicKeyRings(mKeychainData.getPublicKeys(), true, getKeyringsHandler);
//        } catch (RemoteException e) {
//            exceptionImplementation(-1, e.toString());
//        }
//    }
//
//    public void getKeyringsBytesOnClick(View view) {
//        try {
//            service.getPublicKeyRings(mKeychainData.getPublicKeys(), false, getKeyringsHandler);
//        } catch (RemoteException e) {
//            exceptionImplementation(-1, e.toString());
//        }
//    }
//
//    @SuppressLint("NewApi")
//    private void updateView() {
//        if (keysBytes != null) {
//            mKeyringsTextView.setText(Base64.encodeToString(keysBytes, Base64.DEFAULT));
//        } else if (keysStrings != null) {
//            mKeyringsTextView.setText("");
//            for (String output : keysStrings) {
//                mKeyringsTextView.append(output);
//            }
//        }
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//
//        unbindService(svcConn);
//    }
//
//    private void exceptionImplementation(int exceptionId, String error) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Exception!").setMessage(error).setPositiveButton("OK", null).show();
//    }
//
//    private final IKeychainGetKeyringsHandler.Stub getKeyringsHandler = new IKeychainGetKeyringsHandler.Stub() {
//
//        @Override
//        public void onException(final int exceptionId, final String message) throws RemoteException {
//            runOnUiThread(new Runnable() {
//                public void run() {
//                    exceptionImplementation(exceptionId, message);
//                }
//            });
//        }
//
//        @Override
//        public void onSuccess(final byte[] outputBytes, final List<String> outputStrings)
//                throws RemoteException {
//            runOnUiThread(new Runnable() {
//                public void run() {
//                    if (outputBytes != null) {
//                        keysBytes = outputBytes;
//                        keysStrings = null;
//                    } else if (outputStrings != null) {
//                        keysBytes = null;
//                        keysStrings = (ArrayList<String>) outputStrings;
//                    }
//                    updateView();
//                }
//            });
//
//        }
//
//    };
//
//    public void selectEncryptionKeysOnClick(View view) {
//        mKeychainIntentHelper.selectPublicKeys("user@example.com");
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        // this updates the mKeychainData object to the result of the methods
//        boolean result = mKeychainIntentHelper.onActivityResult(requestCode, resultCode, data,
//                mKeychainData);
//        if (result) {
//            updateView();
//        }
//
//        // continue with other activity results
//        super.onActivityResult(requestCode, resultCode, data);
//    }
//
//}
