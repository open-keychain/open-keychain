package org.sufficientlysecure.keychain.network;


import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.cryptolib.SecureDataSocket;
import com.cryptolib.SecureDataSocketException;

import org.sufficientlysecure.keychain.util.FileHelper;

import java.io.IOException;

public class KeyExportSocket {
    public static final int PORT = 5891;

    private static final int SHOW_CONNECTION_DETAILS = 1;
    private static final int LOAD_KEY = 2;
    private static final int KEY_EXPORTED = 3;
    private static final int SHOW_PHRASE = 4;

    private static KeyExportSocket mInstance;

    private SecureDataSocket mSocket;
    private ExportKeyListener mListener;
    private Handler mHandler;

    public static KeyExportSocket getInstance(ExportKeyListener listener) {
        if (mInstance == null) {
            mInstance = new KeyExportSocket(listener);
        } else {
            mInstance.mListener = listener;
        }
        return mInstance;
    }

    private KeyExportSocket(ExportKeyListener listener) {
        mListener = listener;
        mHandler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                String connectionDetails = null;
                try {
                    mSocket = new SecureDataSocket(PORT);
                    connectionDetails = mSocket.prepareServerWithClientCamera();
                } catch (SecureDataSocketException e) {
                    e.printStackTrace();
                }

                invokeListener(SHOW_CONNECTION_DETAILS, connectionDetails);

                try {
                    mSocket.setupServerWithClientCamera();
                    invokeListener(LOAD_KEY, null);
                } catch (SecureDataSocketException e) {
                    // this exception is thrown when socket is closed (user switches from QR code to manual ip input)
                    mSocket.close();

                    String phrase = null;
                    try {
                        phrase = mSocket.setupServerNoClientCamera();
                    } catch (SecureDataSocketException e1) {
                        e1.printStackTrace();
                    }

                    invokeListener(SHOW_PHRASE, phrase);
                }
            }
        }).start();
    }

    public void close() {
        mSocket.close();
        mListener = null;
        mInstance = null;
    }

    public void manualMode() {
        mSocket.close();
    }

    public void writeKey(final Context context, final Uri keyUri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] exportData = FileHelper.readBytesFromUri(context, keyUri);
                    mSocket.write(exportData);
                } catch (SecureDataSocketException | IOException e) {
                    e.printStackTrace();
                }

                invokeListener(KEY_EXPORTED, null);
            }
        }).start();
    }

    public void exportPhrasesMatched(final boolean phrasesMatched) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mSocket.comparedPhrases(phrasesMatched);
                } catch (SecureDataSocketException e) {
                    e.printStackTrace();
                }

                if (phrasesMatched) {
                    invokeListener(LOAD_KEY, null);
                } else {
                    invokeListener(KEY_EXPORTED, null);
                }
            }
        }).start();
    }

    /**
     * Execute method of listener on main thread.
     *
     * @param method    Number of method to call.
     * @param arg       Argument for method.
     */
    private void invokeListener(final int method, final String arg) {
        if (mListener == null) {
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                switch (method) {
                    case SHOW_CONNECTION_DETAILS:
                        mListener.showConnectionDetails(arg);
                        break;
                    case LOAD_KEY:
                        mListener.loadKey();
                        break;
                    case KEY_EXPORTED:
                        mListener.keyExported();
                        break;
                    case SHOW_PHRASE:
                        mListener.showPhrase(arg);
                        break;
                }
            }
        };

        mHandler.post(runnable);
    }

    public interface ExportKeyListener {
        /**
         * Can be null!
         *
         * @param connectionDetails
         */
        void showConnectionDetails(String connectionDetails);

        void loadKey();

        void keyExported();

        void showPhrase(String phrase);
    }
}
