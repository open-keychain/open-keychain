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
import java.io.Closeable;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.net.PskKeyManager;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;


@RequiresApi(api = VERSION_CODES.LOLLIPOP)
public class KeyTransferInteractor {
    private static final String[] ALLOWED_CIPHERSUITES = new String[] {
            // only allow ephemeral diffie-hellman based PSK ciphers!
            "TLS_DHE_PSK_WITH_AES_128_CBC_SHA",
            "TLS_DHE_PSK_WITH_AES_256_CBC_SHA",
            "TLS_DHE_PSK_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256"
    };

    private static final int CONNECTION_LISTENING = 1;
    private static final int CONNECTION_ESTABLISHED = 2;
    private static final int CONNECTION_SEND_OK = 3;
    private static final int CONNECTION_RECEIVE_OK = 4;
    private static final int CONNECTION_LOST = 5;
    private static final int CONNECTION_ERROR_CONNECT = 6;
    private static final int CONNECTION_ERROR_LISTEN = 7;

    private static final int TIMEOUT_RECEIVING = 2000;
    private static final int TIMEOUT_WAITING = 500;


    private final String delimiterStart;
    private final String delimiterEnd;

    private TransferThread transferThread;


    public KeyTransferInteractor(String delimiterStart, String delimiterEnd) {
        this.delimiterStart = delimiterStart;
        this.delimiterEnd = delimiterEnd;
    }

    public void connectToServer(String connectionDetails, KeyTransferCallback callback) {
        Uri uri = Uri.parse(connectionDetails);
        final byte[] presharedKey = Hex.decode(uri.getUserInfo());
        final String host = uri.getHost();
        final int port = uri.getPort();

        transferThread = TransferThread.createClientTransferThread(delimiterStart, delimiterEnd, callback, presharedKey, host, port);
        transferThread.start();
    }

    public void startServer(KeyTransferCallback callback) {
        byte[] presharedKey = generatePresharedKey();

        transferThread = TransferThread.createServerTransferThread(delimiterStart, delimiterEnd, callback, presharedKey);
        transferThread.start();
    }

    private static class TransferThread extends Thread {
        private final String delimiterStart;
        private final String delimiterEnd;

        private final Handler handler;
        private final byte[] presharedKey;
        private final boolean isServer;
        private final String clientHost;
        private final Integer clientPort;

        private KeyTransferCallback callback;
        private SSLServerSocket serverSocket;
        private byte[] dataToSend;
        private String sendPassthrough;

        static TransferThread createClientTransferThread(String delimiterStart, String delimiterEnd,
                KeyTransferCallback callback, byte[] presharedKey, String host, int port) {
            return new TransferThread(delimiterStart, delimiterEnd, callback, presharedKey, false, host, port);
        }

        static TransferThread createServerTransferThread(String delimiterStart, String delimiterEnd,
                KeyTransferCallback callback, byte[] presharedKey) {
            return new TransferThread(delimiterStart, delimiterEnd, callback, presharedKey, true, null, null);
        }

        private TransferThread(String delimiterStart, String delimiterEnd,
                KeyTransferCallback callback, byte[] presharedKey, boolean isServer,
                String clientHost, Integer clientPort) {
            super("TLS-PSK Key Transfer Thread");

            this.delimiterStart = delimiterStart;
            this.delimiterEnd = delimiterEnd;

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
                socket = getSocketListenOrConnect(sslContext);
                if (socket == null) {
                    return;
                }

                try {
                    handleOpenConnection(socket);
                    Log.d(Constants.TAG, "connection closed ok!");
                } catch (SSLHandshakeException e) {
                    Log.d(Constants.TAG, "ssl handshake error!", e);
                    invokeListener(CONNECTION_ERROR_CONNECT, null);
                } catch (IOException e) {
                    Log.e(Constants.TAG, "error!", e);
                }
            } finally {
                closeQuietly(socket);
                closeQuietly(serverSocket);
            }
        }

        @Nullable
        private Socket getSocketListenOrConnect(SSLContext sslContext) {
            Socket socket;
            if (isServer) {
                try {
                    serverSocket = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(0);
                    String[] supportedCipherSuites = serverSocket.getSupportedCipherSuites();
                    String[] enabledCipherSuites = intersectArrays(supportedCipherSuites, ALLOWED_CIPHERSUITES);
                    serverSocket.setEnabledCipherSuites(enabledCipherSuites);

                    String presharedKeyEncoded = Hex.toHexString(presharedKey);
                    String qrCodeData =
                            "pgp+transfer://" + presharedKeyEncoded + "@" + getIPAddress(true) + ":" + serverSocket.getLocalPort();
                    qrCodeData = qrCodeData.toUpperCase(Locale.getDefault());
                    invokeListener(CONNECTION_LISTENING, qrCodeData);

                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "error while listening!", e);
                    invokeListener(CONNECTION_ERROR_LISTEN, null);
                    return null;
                }
            } else {
                try {
                    SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory()
                            .createSocket(InetAddress.getByName(clientHost), clientPort);
                    String[] supportedCipherSuites = sslSocket.getSupportedCipherSuites();
                    String[] enabledCipherSuites = intersectArrays(supportedCipherSuites, ALLOWED_CIPHERSUITES);
                    sslSocket.setEnabledCipherSuites(enabledCipherSuites);

                    socket = sslSocket;
                } catch (IOException e) {
                    Log.e(Constants.TAG, "error while connecting!", e);
                    invokeListener(CONNECTION_ERROR_CONNECT, null);
                    return null;
                }
            }
            return socket;
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
            OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());

            invokeListener(CONNECTION_ESTABLISHED, socket.getInetAddress().toString());

            socket.setSoTimeout(TIMEOUT_WAITING);
            while (!isInterrupted() && socket.isConnected() && !socket.isClosed()) {
                sendDataIfAvailable(socket, outputStream);
                boolean connectionTerminated = receiveDataIfAvailable(socket, bufferedReader);
                if (connectionTerminated) {
                    break;
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
                return true;
            }

            boolean lineIsDelimiter = delimiterStart.equals(firstLine);
            if (!lineIsDelimiter) {
                Log.d(Constants.TAG, "bad beginning of key block?");
                return false;
            }

            socket.setSoTimeout(TIMEOUT_RECEIVING);
            String receivedData = receiveLinesUntilEndDelimiter(bufferedReader, firstLine);
            socket.setSoTimeout(TIMEOUT_WAITING);

            invokeListener(CONNECTION_RECEIVE_OK, receivedData);
            return false;
        }

        private boolean sendDataIfAvailable(Socket socket, OutputStream outputStream) throws IOException {
            if (dataToSend != null) {
                byte[] data = dataToSend;
                dataToSend = null;

                socket.setSoTimeout(TIMEOUT_RECEIVING);
                outputStream.write(data);
                outputStream.flush();
                socket.setSoTimeout(TIMEOUT_WAITING);

                invokeListener(CONNECTION_SEND_OK, sendPassthrough);
                sendPassthrough = null;
                return true;
            }
            return false;
        }

        private String receiveLinesUntilEndDelimiter(BufferedReader bufferedReader, String line) throws IOException {
            StringBuilder builder = new StringBuilder();
            do {
                boolean lineIsDelimiter = delimiterEnd.equals(line);
                if (lineIsDelimiter) {
                    break;
                }

                builder.append(line).append('\n');

                line = bufferedReader.readLine();
            } while (line != null);

            return builder.toString();
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
                            break;
                        case CONNECTION_ERROR_CONNECT:
                            callback.onConnectionErrorConnect();
                            break;
                        case CONNECTION_ERROR_LISTEN:
                            callback.onConnectionErrorListen();
                            break;
                    }
                }
            };

            handler.post(runnable);
        }

        synchronized void sendData(byte[] dataToSend, String passthrough) {
            this.dataToSend = dataToSend;
            this.sendPassthrough = passthrough;
        }

        @Override
        public void interrupt() {
            callback = null;
            super.interrupt();
            closeQuietly(serverSocket);
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

    public void sendData(byte[] dataToSend, String passthrough) {
        transferThread.sendData(dataToSend, passthrough);
    }

    public interface KeyTransferCallback {
        void onServerStarted(String qrCodeData);
        void onConnectionEstablished(String otherName);
        void onConnectionLost();

        void onDataReceivedOk(String receivedData);
        void onDataSentOk(String passthrough);

        void onConnectionErrorConnect();
        void onConnectionErrorListen();
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
        public String chooseClientKeyIdentity(String identityHint, Socket socket) {
            return identityHint;
        }

        @Override
        public String chooseClientKeyIdentity(String identityHint, SSLEngine engine) {
            return identityHint;
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

    private static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private static String[] intersectArrays(String[] array1, String[] array2) {
        Set<String> s1 = new HashSet<>(Arrays.asList(array1));
        Set<String> s2 = new HashSet<>(Arrays.asList(array2));
        s1.retainAll(s2);

        return s1.toArray(new String[0]);
    }
}
