package com.android.crypto;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class CryptoServiceConnection {
    private Context mApplicationContext;

    private ICryptoService mService;
    private boolean bound;
    private String cryptoProviderPackageName;

    private static final String TAG = "CryptoConnection";

    public CryptoServiceConnection(Context context, String cryptoProviderPackageName) {
        mApplicationContext = context.getApplicationContext();
        this.cryptoProviderPackageName = cryptoProviderPackageName;
    }

    public ICryptoService getService() {
        return mService;
    }

    private ServiceConnection mCryptoServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ICryptoService.Stub.asInterface(service);
            Log.d(TAG, "connected to service");
            bound = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            Log.d(TAG, "disconnected from service");
            bound = false;
        }
    };

    /**
     * If not already bound, bind!
     * 
     * @return
     */
    public boolean bindToService() {
        if (mService == null && !bound) { // if not already connected
            try {
                Log.d(TAG, "not bound yet");

                Intent serviceIntent = new Intent();
                serviceIntent.setAction("com.android.crypto.ICryptoService");
                serviceIntent.setPackage(cryptoProviderPackageName); // TODO: test
                mApplicationContext.bindService(serviceIntent, mCryptoServiceConnection,
                        Context.BIND_AUTO_CREATE);

                return true;
            } catch (Exception e) {
                Log.d(TAG, "Exception", e);
                return false;
            }
        } else { // already connected
            Log.d(TAG, "already bound... ");
            return true;
        }
    }

    public void unbindFromService() {
        mApplicationContext.unbindService(mCryptoServiceConnection);
    }

}
