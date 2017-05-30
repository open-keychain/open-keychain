/*
 * Copyright (C) 2017 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

import android.net.PskKeyManager;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;


@RequiresApi(api = VERSION_CODES.LOLLIPOP)
public class KeyTransferInteractor {
    private static final int CONNECTION_LISTENING = 1;
    private static final int CONNECTION_ESTABLISHED = 2;
    private static final int CONNECTION_SEND_OK = 3;
    private static final int CONNECTION_RECEIVE_OK = 4;
    private static final int CONNECTION_LOST = 5;


    private TransferThread transferThread;


    public void connectToServer(String connectionDetails, KeyTransferCallback callback) {
        Uri uri = Uri.parse(connectionDetails);
        final byte[] presharedKey = Base64.decode(uri.getUserInfo(), Base64.URL_SAFE | Base64.NO_PADDING);
        final String host = uri.getHost();
        final int port = uri.getPort();

        transferThread = TransferThread.createClientTransferThread(callback, presharedKey, host, port);
        transferThread.start();
    }

    public void startServer(KeyTransferCallback callback) {
        byte[] presharedKey = generatePresharedKey();

        transferThread = TransferThread.createServerTransferThread(callback, presharedKey);
        transferThread.start();
    }

    private static class TransferThread extends Thread {
        private final Handler handler;
        private final KeyTransferCallback callback;
        private final byte[] presharedKey;
        private final boolean isServer;
        private final String clientHost;
        private final Integer clientPort;

        private SSLServerSocket serverSocket;
        private byte[] dataToSend;

        static TransferThread createClientTransferThread(KeyTransferCallback callback, byte[] presharedKey,
                String host, int port) {
            return new TransferThread(callback, presharedKey, false, host, port);
        }

        static TransferThread createServerTransferThread(KeyTransferCallback callback, byte[] presharedKey) {
            return new TransferThread(callback, presharedKey, true, null, null);
        }

        private TransferThread(KeyTransferCallback callback, byte[] presharedKey, boolean isServer,
                String clientHost, Integer clientPort) {
            this.callback = callback;
            this.presharedKey = presharedKey;
            this.clientHost = clientHost;
            this.clientPort = clientPort;
            this.isServer = isServer;

            handler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void run() {
            SSLContext sslContext = createTlsPskSslContext(presharedKey);

            Socket socket = null;
            try {
                if (isServer) {
                    int port = 1336;
                    serverSocket = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(port);

                    String presharedKeyEncoded = Base64.encodeToString(presharedKey, Base64.URL_SAFE | Base64.NO_PADDING);
                    String qrCodeData = presharedKeyEncoded + "@" + getIPAddress(true) + ":" + port;
                    invokeListener(CONNECTION_LISTENING, qrCodeData);

                    socket = serverSocket.accept();
                    invokeListener(CONNECTION_ESTABLISHED, socket.getInetAddress().toString());
                } else {
                    socket = sslContext.getSocketFactory().createSocket(InetAddress.getByName(clientHost), clientPort);
                    invokeListener(CONNECTION_ESTABLISHED, socket.getInetAddress().toString());
                }

                handleOpenConnection(socket);
                Log.d(Constants.TAG, "connection closed ok!");
            } catch (IOException e) {
                Log.e(Constants.TAG, "error!", e);
            } finally {
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

        private static SSLContext createTlsPskSslContext(byte[] presharedKey) {
            try {
                PresharedKeyManager pskKeyManager = new PresharedKeyManager(presharedKey);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(new KeyManager[] { pskKeyManager }, new TrustManager[0], null);

                return sslContext;
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        private void handleOpenConnection(Socket socket) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            socket.setSoTimeout(500);
            while (!isInterrupted() && socket.isConnected()) {
                if (sendDataIfAvailable(socket)) {
                    return;
                }

                if (receiveDataIfAvailable(socket, bufferedReader)) {
                    return;
                }
            }
            Log.d(Constants.TAG, "disconnected");
            invokeListener(CONNECTION_LOST, null);
        }

        private boolean receiveDataIfAvailable(Socket socket, BufferedReader bufferedReader) throws IOException {
            String firstLine;
            try {
                firstLine = bufferedReader.readLine();
            } catch (SocketTimeoutException e) {
                return false;
            }

            if (firstLine == null) {
                invokeListener(CONNECTION_LOST, null);
                return true;
            }

            socket.setSoTimeout(2000);
            String receivedData = receiveAfterFirstLineAndClose(bufferedReader, firstLine);
            invokeListener(CONNECTION_RECEIVE_OK, receivedData);
            return true;
        }

        private boolean sendDataIfAvailable(Socket socket) throws IOException {
            if (dataToSend != null) {
                byte[] data = dataToSend;
                dataToSend = null;

                socket.setSoTimeout(2000);
                sendDataAndClose(socket.getOutputStream(), data);
                invokeListener(CONNECTION_SEND_OK, null);
                return true;
            }
            return false;
        }

        private String receiveAfterFirstLineAndClose(BufferedReader bufferedReader, String line) throws IOException {
            StringBuilder builder = new StringBuilder();
            do {
                builder.append(line);
                line = bufferedReader.readLine();
            } while (line != null);

            return builder.toString();
        }

        private void sendDataAndClose(OutputStream outputStream, byte[] data) throws IOException {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            bufferedOutputStream.write(data);
            bufferedOutputStream.close();
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
                        case CONNECTION_LISTENING:
                            callback.onServerStarted(arg);
                            break;
                        case CONNECTION_ESTABLISHED:
                            callback.onConnectionEstablished(arg);
                            break;
                        case CONNECTION_RECEIVE_OK:
                            callback.onDataReceivedOk(arg);
                            break;
                        case CONNECTION_SEND_OK:
                            callback.onDataSentOk(arg);
                            break;
                        case CONNECTION_LOST:
                            callback.onConnectionLost();
                    }
                }
            };

            handler.post(runnable);
        }

        synchronized void sendDataAndClose(byte[] dataToSend) {
            this.dataToSend = dataToSend;
        }

        @Override
        public void interrupt() {
            super.interrupt();

            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private static byte[] generatePresharedKey() {
        byte[] presharedKey = new byte[16];
        new SecureRandom().nextBytes(presharedKey);
        return presharedKey;
    }

    public void closeConnection() {
        if (transferThread != null) {
            transferThread.interrupt();
        }

        transferThread = null;
    }

    public void sendData(byte[] dataToSend) {
        transferThread.sendDataAndClose(dataToSend);
    }

    public interface KeyTransferCallback {
        void onServerStarted(String qrCodeData);
        void onConnectionEstablished(String otherName);
        void onConnectionLost();

        void onDataReceivedOk(String receivedData);
        void onDataSentOk(String arg);
    }

    /**
     * from: http://stackoverflow.com/a/13007325
     * <p>
     * Get IP address from first non-localhost interface
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    private static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.
                    getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).
                                        toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // ignore
        }
        return "";
    }

    private static class PresharedKeyManager extends PskKeyManager implements KeyManager {
        byte[] presharedKey;

        private PresharedKeyManager(byte[] presharedKey) {
            this.presharedKey = presharedKey;
        }

        @Override
        public SecretKey getKey(String identityHint, String identity, Socket socket) {
            return new SecretKeySpec(presharedKey, "AES");
        }

        @Override
        public SecretKey getKey(String identityHint, String identity, SSLEngine engine) {
            return new SecretKeySpec(presharedKey, "AES");
        }
    }
}
