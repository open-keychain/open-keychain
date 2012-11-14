package org.thialfihar.android.apg.demo;

import org.thialfihar.android.apg.integration.ApgData;
import org.thialfihar.android.apg.integration.ApgIntentHelper;
import org.thialfihar.android.apg.service.IApgEncryptDecryptHandler;
import org.thialfihar.android.apg.service.IApgHelperHandler;
import org.thialfihar.android.apg.service.IApgService;

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
import android.widget.Toast;

public class AidlDemoActivity extends Activity {
    Activity mActivity;

    TextView mMessageTextView;
    TextView mCiphertextTextView;
    TextView mDataTextView;

    ApgIntentHelper mApgIntentHelper;
    ApgData mApgData;

    private IApgService service = null;
    private ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = IApgService.Stub.asInterface(binder);
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

        bindService(new Intent("org.thialfihar.android.apg.service.IApgService"), svcConn,
                Context.BIND_AUTO_CREATE);
    }

    public void encryptOnClick(View view) {
        byte[] inputBytes = mMessageTextView.getText().toString().getBytes();

        try {
            service.encryptAsymmetric(inputBytes, null, true, 0, mApgData.getEncryptionKeys(), 7,
                    encryptDecryptHandler);
        } catch (RemoteException e) {
            exceptionImplementation(-1, e.toString());
        }
    }

    public void decryptOnClick(View view) {
        byte[] inputBytes = mCiphertextTextView.getText().toString().getBytes();

        try {
            service.decryptAndVerifyAsymmetric(inputBytes, null, null, encryptDecryptHandler);
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

    private final IApgEncryptDecryptHandler.Stub encryptDecryptHandler = new IApgEncryptDecryptHandler.Stub() {

        @Override
        public void onException(final int exceptionId, final String message) throws RemoteException {
            runOnUiThread(new Runnable() {
                public void run() {
                    exceptionImplementation(exceptionId, message);
                }
            });
        }

        @Override
        public void onSuccessEncrypt(final byte[] outputBytes, String outputUri)
                throws RemoteException {
            runOnUiThread(new Runnable() {
                public void run() {
                    mApgData.setEncryptedData(new String(outputBytes));
                    updateView();
                }
            });
        }

        @Override
        public void onSuccessDecrypt(final byte[] outputBytes, String outputUri, boolean signature,
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

    private final IApgHelperHandler.Stub helperHandler = new IApgHelperHandler.Stub() {

        @Override
        public void onException(final int exceptionId, final String message) throws RemoteException {
            runOnUiThread(new Runnable() {
                public void run() {
                    exceptionImplementation(exceptionId, message);
                }
            });
        }

        @Override
        public void onSuccessGetDecryptionKey(long arg0, boolean arg1) throws RemoteException {
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
        mApgIntentHelper.selectEncryptionKeys("user@example.com");

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
