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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import android.net.PskKeyManager;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;


@RequiresApi(api = VERSION_CODES.LOLLIPOP)
public class KeyTransferClientInteractor {
    private static final int SHOW_CONNECTION_DETAILS = 1;
    private static final int CONNECTION_ESTABLISHED = 2;
    public static final int CONNECTION_LOST = 3;


    private Thread socketThread;
    private KeyTransferClientCallback callback;
    private Handler handler;
    private SSLServerSocket serverSocket;


    public void connectToServer(final String connectionDetails, KeyTransferClientCallback callback) {
        this.callback = callback;

        handler = new Handler(Looper.getMainLooper());
        socketThread = new Thread() {
            @Override
            public void run() {
                serverSocket = null;
                Socket socket = null;
                BufferedReader bufferedReader = null;
                try {
                    int port = 1336;

                    PKM pskKeyManager = new PKM();
                    SSLContext sslContext = null;
                    sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(new KeyManager[] { pskKeyManager }, new TrustManager[0], null);
                    socket = sslContext.getSocketFactory().createSocket(InetAddress.getByName(connectionDetails), port);

                    invokeListener(CONNECTION_ESTABLISHED, socket.getInetAddress().toString());

                    socket.setSoTimeout(500);
                    bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    while (!isInterrupted() && socket.isConnected()) {
                        try {
                            String line = bufferedReader.readLine();
                            if (line == null) {
                                break;
                            }
                            Log.d(Constants.TAG, "got line: " + line);
                        } catch (SocketTimeoutException e) {
                            // ignore
                        }
                    }
                    Log.d(Constants.TAG, "disconnected");
                    invokeListener(CONNECTION_LOST, null);
                } catch (NoSuchAlgorithmException | KeyManagementException | IOException e) {
                    Log.e(Constants.TAG, "error!", e);
                } finally {
                    try {
                        if (bufferedReader != null) {
                            bufferedReader.close();
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                    try {
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                    try {
                        if (serverSocket != null) {
                            serverSocket.close();
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        };

        socketThread.start();
    }

    public void closeConnection() {
        if (socketThread != null) {
            socketThread.interrupt();
        }

        socketThread = null;
        callback = null;
    }

    private void invokeListener(final int method, final String arg) {
        if (handler == null) {
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (callback == null) {
                    return;
                }

                switch (method) {
                    case CONNECTION_ESTABLISHED:
                        callback.onConnectionEstablished(arg);
                        break;
                    case CONNECTION_LOST:
                        callback.onConnectionLost();
                }
            }
        };

        handler.post(runnable);
    }

    public interface KeyTransferClientCallback {
        void onConnectionEstablished(String otherName);
        void onConnectionLost();
    }

    private static class PKM extends PskKeyManager implements KeyManager {
        @Override
        public SecretKey getKey(String identityHint, String identity, Socket socket) {
            return new SecretKeySpec("swag".getBytes(), "AES");
        }

        @Override
        public SecretKey getKey(String identityHint, String identity, SSLEngine engine) {
            return new SecretKeySpec("swag".getBytes(), "AES");
        }
    }
}
