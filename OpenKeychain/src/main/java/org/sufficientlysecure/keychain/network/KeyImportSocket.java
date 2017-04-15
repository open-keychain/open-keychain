package org.sufficientlysecure.keychain.network;


import android.os.Handler;
import android.os.Looper;

import com.cryptolib.SecureDataSocket;
import com.cryptolib.SecureDataSocketException;

public class KeyImportSocket {
    private static KeyImportSocket mInstance;

    private SecureDataSocket mSocket;
    private KeyImportListener mListener;
    private Handler mHandler;

    public static KeyImportSocket getInstance(KeyImportListener listener) {
        if (mInstance == null) {
            mInstance = new KeyImportSocket();
        }
        mInstance.mListener = listener;

        return mInstance;
    }

    private KeyImportSocket() {
        mHandler = new Handler(Looper.getMainLooper());
    }

    public void startImport(final String connectionDetails) {
        if (connectionDetails == null) {
            close();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                mSocket = new SecureDataSocket(0);
                try {
                    mSocket.setupClientWithCamera(connectionDetails);
                } catch (SecureDataSocketException e) {
                    e.printStackTrace();
                }
                importKey();
            }
        }).start();
    }

    public void close() {
        if (mSocket != null) {
            mSocket.close();
        }
        mListener = null;
        mInstance = null;
    }

    private void importKey() {
        byte[] keyRing = null;
        try {
            keyRing = mSocket.read();
        } catch (SecureDataSocketException e) {
            e.printStackTrace();
        }

        invokeListener(keyRing);
    }

    /**
     * Execute method of listener on main thread.
     *
     * @param key      Imported key.
     */
    private void invokeListener(final byte[] key) {
        if (mListener == null) {
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mListener.importKey(key);
                close();
            }
        };

        mHandler.post(runnable);
    }

    public interface KeyImportListener {

        /**
         * Key is received and can be imported.
         *
         * @param key Key to import from the other device
         */
        void importKey(byte[] key);
    }
}
