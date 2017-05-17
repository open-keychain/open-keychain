/*
 * Copyright (C) 2017 Jakob Bode
 * Copyright (C) 2017 Matthias Sekul
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

//Have a look at https://github.com/MrJabo/SecureDataSocket

package org.sufficientlysecure.keychain.network.secureDataSocket;

import android.annotation.TargetApi;
import android.os.Build;

import org.bouncycastle.util.encoders.Base64;

import java.lang.IllegalStateException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import javax.crypto.IllegalBlockSizeException;

import java.security.SecureRandom;

@TargetApi(Build.VERSION_CODES.KITKAT)
class CryptoSocket implements CryptoSocketInterface {
    private Channel mChannel = null;
    private ServerSocket mServer = null;
    private Socket mSocket = null;
    private OutputStream mOut = null;
    private InputStream mIn = null;
    private CryptoObject mCobject = null;
    private boolean mVerified = false;
    private boolean mConnected = false;
    private boolean mRunning = false;

    /**
     * Constructor.
     * channel defines the interface, which will be used for the
     * sharedsecretexchange.
     * If you call listen() afterwards, only connections from the given
     * destination are accepted.
     * If you otherwise call connect() afterwards, you will connect to the
     * given destination.
     */
    CryptoSocket(Channel channel) {
        this.mChannel = channel;
    }

    public SocketAddress listen(int port) throws IOException, CryptoSocketException {
        if (!this.mVerified) {
            throw new CryptoSocketException("call setSharedSecret or createSharedSecret before " +
                    "listen");
        } else {
            this.mServer = new ServerSocket(port);
            this.mRunning = true;
            while (this.mRunning) {
                this.mSocket = this.mServer.accept();

                if (!(this.mChannel.id.equals("") || this.mChannel.id.equals(":") ||
                        this.mChannel.id.equals("::")) &&
                        !this.mChannel.id.equals(mSocket.getRemoteSocketAddress().toString())) {
                    mSocket.close();
                    continue;
                }

                break;
            }

            // begin crypto protocol
            this.mOut = this.mSocket.getOutputStream();
            this.mIn = this.mSocket.getInputStream();
            this.mConnected = true;
            //check if the same sharedSecret was used
            byte[] check = new byte[1];
            new SecureRandom().nextBytes(check);
            try {
                this.write(check);
            } catch (UnverifiedException e) {
                throw new CryptoSocketException("impossible Error while sharedSecret check");
            }
            byte[] ans = new byte[1];
            this.read(true, ans);
            byte[] check2 = new byte[1];
            this.read(true, check2);
            check2[0] += 1;
            try {
                this.write(check2);
            } catch (UnverifiedException e) {
                throw new CryptoSocketException("impossible Error while sharedSecret check");
            }
            if (check[0] + 1 != ans[0]) {
                this.mConnected = false;
                throw new CryptoSocketException("not the same sharedSecret is used. " +
                        "maybe the wrong device connected to you");
            }

            return this.mSocket.getRemoteSocketAddress();
        }
    }

    public boolean connect() throws CryptoSocketException, IOException {
        if (!this.mChannel.id.equals("")) {
            String tmp = new StringBuilder(this.mChannel.id).reverse().toString();

            if (!tmp.contains(":")) {
                throw new CryptoSocketException("for a MANUAL channel the identifier needs to " +
                        "be ip-port-sharedsecret combination e.g. " +
                        "127.0.0.1:1337:mySharedSecretMySharedSecret1234 or " +
                        "::1:4711:mySharedSecretMySharedSecret1234");
            }

            String[] stringParts = tmp.split(":", 3);


            if (3 != stringParts.length) {
                throw new CryptoSocketException("for a MANUAL channel the identifier needs to " +
                        "be ip-port-sharedsecret combination e.g. " +
                        "127.0.0.1:1337:mySharedSecretMySharedSecret1234 or " +
                        "::1:4711:mySharedSecretMySharedSecret1234");
            }

            stringParts[0] = new StringBuilder(stringParts[0]).reverse().toString();
            stringParts[1] = new StringBuilder(stringParts[1]).reverse().toString();
            stringParts[2] = new StringBuilder(stringParts[2]).reverse().toString();
            this.setSharedSecret(stringParts[0]);
            String serverAddress = stringParts[2];
            String port = stringParts[1];
            InetSocketAddress address;

            try {
                address = new InetSocketAddress(serverAddress, Integer.parseInt(port));
            } catch (Exception e) {
                throw new CryptoSocketException("for a MANUAL channel the identifier needs to be " +
                        "ip-port-sharedsecret combination e.g. " +
                        "127.0.0.1:1337:mySharedSecretMySharedSecret1234 or " +
                        "::1:4711:mySharedSecretMySharedSecret1234");
            }

            this.mSocket = new Socket(address.getHostString(), address.getPort());

        } else {
            throw new CryptoSocketException("for a MANUAL channel the identifier needs to be " +
                    "ip-port-sharedsecret combination e.g. " +
                    "127.0.0.1:1337:mySharedSecretMySharedSecret1234 or " +
                    "::1:4711:mySharedSecretMySharedSecret1234");
        }

        // begin crypto protocol
        this.mOut = this.mSocket.getOutputStream();
        this.mIn = this.mSocket.getInputStream();
        this.mConnected = true;
        //check if the same sharedSecret was used
        byte[] check = new byte[1];
        this.read(true, check);
        check[0] += 1;
        try {
            this.write(check);
        } catch (UnverifiedException e) {
            throw new CryptoSocketException("impossible Error while sharedSecret check");
        }
        check = new byte[1];
        new SecureRandom().nextBytes(check);
        try {
            this.write(check);
        } catch (UnverifiedException e) {
            throw new CryptoSocketException("impossible Error while sharedSecret check");
        }
        byte[] ans = new byte[1];
        this.read(true, ans);
        if (ans[0] != check[0] + 1) {
            this.mConnected = false;
            throw new CryptoSocketException("not the same sharedSecret is used. " +
                    "maybe you connected to the wrong device");
        }
        return true;
    }

    private void setSharedSecret(String sharedSecret) throws CryptoSocketException, IOException {
        this.mCobject = new CryptoObject();
        byte[] byteSharedSecret = Base64.decode(sharedSecret);
        this.mCobject.setSharedSecret(byteSharedSecret);
        this.mVerified = true;
    }

    public String createSharedSecret() throws CryptoSocketException, IOException {
        //create sharedsecret
        byte[] byteSharedSecret = new byte[32];
        SecureRandom rand = new SecureRandom();
        rand.nextBytes(byteSharedSecret);
        String sharedSecret = Base64.toBase64String(byteSharedSecret);
        //set sharedsecret
        this.setSharedSecret(sharedSecret);
        return sharedSecret;
    }

    public int write(byte[] array) throws UnverifiedException, IllegalStateException, IOException,
            CryptoSocketException {
        if (!this.mConnected) {
            throw new IllegalStateException("not connected, call listen() or connect() first!");
        }

        if (!this.mVerified) {
            throw new UnverifiedException("you need to verifiy the channel first before " +
                    "you can write data! This channel may not trustworthy!");
        }

        byte[] ciphertext;
        try {
            ciphertext = this.mCobject.encrypt(array);
        } catch (IllegalBlockSizeException ibs) {
            return RETURN.INVALID_CIPHERTEXT.getValue();
        }
        this.mOut.write(ciphertext);
        this.mOut.flush();

        return RETURN.SUCCESS.getValue();
    }

    public int read(boolean blocking, byte[] data) throws IllegalStateException,
            CryptoSocketException, IOException {
        int readBytes;
        int totalBytes = 0;

        if (!this.mConnected) {
            throw new IllegalStateException("cryptosocket is not connected to " +
                    "communication partner!");
        }

        if (null == data) {
            throw new CryptoSocketException("null byte array not allowed!");
        }

        int size = this.mCobject.getOffset() + data.length;
        byte[] tmp = new byte[size];
        byte[] tmp2;

        if (!blocking) {
            int available = this.mIn.available();

            if (available < size) {
                return RETURN.NOT_AVAILABLE.getValue();
            } else {
                totalBytes = size;
            }

            readBytes = this.mIn.read(tmp, 0, totalBytes);

            if (0 > readBytes) {
                return RETURN.READ.getValue();
            }
        } else {
            while (totalBytes < size) {
                readBytes = this.mIn.read(tmp, totalBytes, tmp.length - totalBytes);

                if (0 == readBytes) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ie) {
                        //throw new CryptoSocketException("ERROR Interrupted!");
                    }

                    continue;
                }

                if (0 > readBytes) {
                    return RETURN.READ.getValue();
                }

                totalBytes = totalBytes + readBytes;
            }
        }

        try {
            tmp2 = this.mCobject.decrypt(tmp);
        } catch (IllegalBlockSizeException ibs) {
            return RETURN.INVALID_CIPHERTEXT.getValue();
        }

        if (null == tmp2) {
            return RETURN.WRONG_TAG.getValue();
        }

        int copySize = tmp2.length > data.length ? data.length : tmp2.length;
        System.arraycopy(tmp2, 0, data, 0, copySize);
        totalBytes = copySize;
        return totalBytes;
    }

    public boolean hasNext() throws IOException {
        return this.mCobject.getOffset() < this.mIn.available();
    }

    public void close() {
        //clearing attributes
        this.mOut = null;
        this.mIn = null;
        this.mCobject = null;
        this.mVerified = false;
        this.mConnected = false;
        this.mRunning = false;

        try {
            if (this.mSocket != null) {
                this.mSocket.close();
            }
            if (this.mServer != null) {
                this.mServer.close();
            }
        } catch (IOException ioe) {
        }// for now eat exceptions

        this.mSocket = null;
        this.mServer = null;
    }
}
