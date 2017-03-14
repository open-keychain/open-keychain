package org.sufficientlysecure.keychain.network;


import android.os.Handler;
import android.os.Looper;

import com.cryptolib.SecureDataSocket;
import com.cryptolib.SecureDataSocketException;

public class KeyImportSocket {
    private static final int SHOW_PHRASE = 1;
    private static final int IMPORT_KEY = 2;

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

    public void startImport(final String ipAddress, final int port) {
        if (ipAddress == null || port <= 0) {
            close();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                mSocket = new SecureDataSocket(port);

                String connectionDetails = ipAddress + ":" + port;
                String comparePhrase = null;
                try {
                    comparePhrase = mSocket.setupClientNoCamera(connectionDetails);
                } catch (SecureDataSocketException e) {
                    e.printStackTrace();
                }

                invokeListener(SHOW_PHRASE, comparePhrase, null);

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

    public void importPhrasesMatched(final boolean phrasesMatched) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mSocket.comparedPhrases(phrasesMatched);
                } catch (SecureDataSocketException e) {
                    e.printStackTrace();
                }

                if (phrasesMatched) {
                    importKey();
                } else {
                    close();
                }
            }
        }).start();
    }

    private void importKey() {
        byte[] keyRing = null;
        try {
            keyRing = mSocket.read();
        } catch (SecureDataSocketException e) {
            e.printStackTrace();
        }

        invokeListener(IMPORT_KEY, null, keyRing);
    }

    /**
     * Execute method of listener on main thread.
     *
     * @param method    Number of method to call.
     * @param arg1      String argument for method.
     * @param arg2      Key for method.
     */
    private void invokeListener(final int method, final String arg1, final byte[] arg2) {
        if (mListener == null) {
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                switch (method) {
                    case SHOW_PHRASE:
                        mListener.showPhrase(arg1);
                        break;
                    case IMPORT_KEY:
                        mListener.importKey(arg2);
                        close();
                        break;
                }
            }
        };

        mHandler.post(runnable);
    }

    public interface KeyImportListener {
        void showPhrase(String phrase);
        void importKey(byte[] keyRing);
    }
}
