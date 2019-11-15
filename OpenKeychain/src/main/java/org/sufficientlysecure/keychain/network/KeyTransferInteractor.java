/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import timber.log.Timber;


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
    private static final int CONNECTION_ERROR_NO_ROUTE_TO_HOST = 6;
    private static final int CONNECTION_ERROR_CONNECT = 7;
    private static final int CONNECTION_ERROR_WHILE_CONNECTED = 8;
    private static final int CONNECTION_ERROR_LISTEN = 0;

    private static final int TIMEOUT_CONNECTING = 1500;
    private static final int TIMEOUT_RECEIVING = 2000;
    private static final int TIMEOUT_WAITING = 500;
    private static final int PSK_BYTE_LENGTH = 16;


    private final String delimiterStart;
    private final String delimiterEnd;

    private TransferThread transferThread;


    public KeyTransferInteractor(String delimiterStart, String delimiterEnd) {
        this.delimiterStart = delimiterStart;
        this.delimiterEnd = delimiterEnd;
    }

    public void connectToServer(String qrCodeContent, KeyTransferCallback callback) throws URISyntaxException {
        SktUri sktUri = SktUri.parse(qrCodeContent);

        transferThread = TransferThread.createClientTransferThread(delimiterStart, delimiterEnd, callback,
                sktUri.getPresharedKey(), sktUri.getHost(), sktUri.getPort(), sktUri.getWifiSsid());
        transferThread.start();
    }

    public void startServer(KeyTransferCallback callback, String wifiSsid) {
        byte[] presharedKey = generatePresharedKey();

        transferThread = TransferThread.createServerTransferThread(delimiterStart, delimiterEnd, callback, presharedKey, wifiSsid);
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
        private final String wifiSsid;

        private KeyTransferCallback callback;
        private SSLServerSocket serverSocket;
        private byte[] dataToSend;
        private String sendPassthrough;

        static TransferThread createClientTransferThread(String delimiterStart, String delimiterEnd,
                KeyTransferCallback callback, byte[] presharedKey, String host, int port, String wifiSsid) {
            return new TransferThread(delimiterStart, delimiterEnd, callback, presharedKey, false, host, port, wifiSsid);
        }

        static TransferThread createServerTransferThread(String delimiterStart, String delimiterEnd,
                KeyTransferCallback callback, byte[] presharedKey, String wifiSsid) {
            return new TransferThread(delimiterStart, delimiterEnd, callback, presharedKey, true, null, null, wifiSsid);
        }

        private TransferThread(String delimiterStart, String delimiterEnd,
                KeyTransferCallback callback, byte[] presharedKey, boolean isServer,
                String clientHost, Integer clientPort, String wifiSsid) {
            super("TLS-PSK Key Transfer Thread");

            this.delimiterStart = delimiterStart;
            this.delimiterEnd = delimiterEnd;

            this.callback = callback;
            this.presharedKey = presharedKey;
            this.clientHost = clientHost;
            this.clientPort = clientPort;
            this.wifiSsid = wifiSsid;
            this.isServer = isServer;

            handler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void run() {
            SSLContext sslContext = TlsPskCompat.createTlsPskSslContext(presharedKey);

            Socket socket = null;
            try {
                socket = getSocketListenOrConnect(sslContext);
                if (socket == null) {
                    return;
                }

                try {
                    handleOpenConnection(socket);
                    Timber.d("connection closed ok!");
                } catch (SSLHandshakeException e) {
                    Timber.d(e, "ssl handshake error!");
                    invokeListener(CONNECTION_ERROR_CONNECT, null);
                } catch (IOException e) {
                    Timber.e(e, "communication error!");
                    invokeListener(CONNECTION_ERROR_WHILE_CONNECTED, e.getLocalizedMessage());
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

                    SktUri sktUri = SktUri.create(getIPAddress(true), serverSocket.getLocalPort(), presharedKey, wifiSsid);
                    invokeListener(CONNECTION_LISTENING, sktUri.toUriString());

                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Timber.e(e, "error while listening!");
                    invokeListener(CONNECTION_ERROR_LISTEN, null);
                    return null;
                }
            } else {
                try {
                    SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket();
                    String[] supportedCipherSuites = sslSocket.getSupportedCipherSuites();
                    String[] enabledCipherSuites = intersectArrays(supportedCipherSuites, ALLOWED_CIPHERSUITES);
                    sslSocket.setEnabledCipherSuites(enabledCipherSuites);

                    socket = sslSocket;
                    socket.connect(new InetSocketAddress(InetAddress.getByName(clientHost), clientPort), TIMEOUT_CONNECTING);
                } catch (IOException e) {
                    Timber.e(e, "error while connecting!");
                    if (e instanceof NoRouteToHostException) {
                        invokeListener(CONNECTION_ERROR_NO_ROUTE_TO_HOST, wifiSsid);
                    } else {
                        invokeListener(CONNECTION_ERROR_CONNECT, null);
                    }
                    return null;
                }
            }
            return socket;
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
            Timber.d("disconnected");
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
                Timber.d("bad beginning of key block?");
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
                        case CONNECTION_ERROR_WHILE_CONNECTED:
                            callback.onConnectionError(arg);
                            break;
                        case CONNECTION_ERROR_NO_ROUTE_TO_HOST:
                            callback.onConnectionErrorNoRouteToHost(wifiSsid);
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
        byte[] presharedKey = new byte[PSK_BYTE_LENGTH];
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
        void onConnectionErrorNoRouteToHost(String wifiSsid);
        void onConnectionErrorListen();
        void onConnectionError(String arg);
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
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (addr.isLoopbackAddress()) {
                        continue;
                    }
                    String sAddr = addr.getHostAddress();
                    boolean isIPv4 = sAddr.indexOf(':') < 0;
                    if (useIPv4) {
                        if (isIPv4) {
                            return sAddr;
                        }
                    } else {
                        int delimIndex = sAddr.indexOf('%'); // drop ip6 zone suffix
                        if (delimIndex >= 0) {
                            sAddr = sAddr.substring(0, delimIndex);
                        }
                        return sAddr.toUpperCase();
                    }
                }
            }
        } catch (Exception ex) {
            // ignore
        }
        return "";
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
