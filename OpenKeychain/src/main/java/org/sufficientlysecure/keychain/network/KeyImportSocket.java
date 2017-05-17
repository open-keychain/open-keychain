/*
 * Copyright (C) 2017 Tobias Schülke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.network;


import android.os.Handler;
import android.os.Looper;


import org.sufficientlysecure.keychain.network.secureDataSocket.SecureDataSocket;
import org.sufficientlysecure.keychain.network.secureDataSocket.SecureDataSocketException;

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
                    mSocket.setupClient(connectionDetails);
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
