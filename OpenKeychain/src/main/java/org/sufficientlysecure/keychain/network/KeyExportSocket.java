package org.sufficientlysecure.keychain.network;


import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.sufficientlysecure.keychain.network.secureDataSocket.SecureDataSocket;
import org.sufficientlysecure.keychain.network.secureDataSocket.SecureDataSocketException;

import org.sufficientlysecure.keychain.util.FileHelper;

import java.io.IOException;
import java.util.Random;

public class KeyExportSocket {
    private static final int SHOW_CONNECTION_DETAILS = 1;
    private static final int LOAD_KEY = 2;
    private static final int KEY_EXPORTED = 3;

    private static KeyExportSocket instance;

    private SecureDataSocket mSocket;
    private ExportKeyListener mListener;
    private Handler mHandler;
    private int mPort;

    public static KeyExportSocket getInstance(ExportKeyListener listener) {
        if (instance == null) {
            instance = new KeyExportSocket(listener);
        } else {
            instance.mListener = listener;
        }
        return instance;
    }

    public static void setListener(ExportKeyListener listener) {
        if (instance != null) {
            instance.mListener = listener;
        }
    }

    private KeyExportSocket(ExportKeyListener listener) {
        mListener = listener;
        mHandler = new Handler(Looper.getMainLooper());

        Random random = new Random();
        mPort = 6000 + random.nextInt(3000);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String connectionDetails = null;
                try {
                    mSocket = new SecureDataSocket(mPort);
                    connectionDetails = mSocket.prepareServer();
                } catch (SecureDataSocketException e) {
                    e.printStackTrace();
                }

                invokeListener(SHOW_CONNECTION_DETAILS, connectionDetails);

                try {
                    mSocket.setupServer();
                    invokeListener(LOAD_KEY, null);
                } catch (SecureDataSocketException e) {
                    // this exception is thrown when socket is closed (user switches from QR code
                    // to manual ip input)
                    mSocket.close();
                }
            }
        }).start();
    }

    public void close(boolean closeNetworkSocketOnly) {
        mSocket.close();

        if (!closeNetworkSocketOnly) {
            mListener = null;
            instance = null;
        }
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
                }
            }
        };

        mHandler.post(runnable);
    }

    public interface ExportKeyListener {
        /**
         *  Show connection details as a qr code so that the other device can connect.
         *
         *  @param connectionDetails Contains the ip address, the port and a shared secret
         */
        void showConnectionDetails(String connectionDetails);

        /**
         * Connection is established. Load the key to export and invoke
         * {@link #writeKey(Context, Uri)} to send the key.
         */
        void loadKey();

        /**
         * Key is transferred to the oder device.
         */
        void keyExported();
    }
}
