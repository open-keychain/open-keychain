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

import org.bouncycastle.util.encoders.Base64;

import org.sufficientlysecure.keychain.network.secureDataSocket.CryptoSocketInterface.RETURN;
import java.lang.IllegalStateException;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.IllegalBlockSizeException;
import java.security.SecureRandom;

public class CryptoSocket implements CryptoSocketInterface {
        private Channel channel = null;
        private ServerSocket server = null;
        private Socket socket = null;
        private OutputStream out = null;
        private InputStream in = null;
        private CryptoObject cobject = null;
        private boolean verified = false;
        private boolean connected = false;
        private boolean running = false;

        /**
        * Constructor.
        * channel defines the interface, which will be used for the
        * sharedsecretexchange.
        * If you call listen() afterwards, connections from the given
        * destination are accepted.
        * If you otherwise call connect() afterwards, the other device will be
        * searched in the localnetwork via broadcast and when found, a
        * connection will be set up.
        */
        //public CryptoSocket(Channel channel){
        //	this.channel = channel;
        //}

        /**
        * Constructor.
        * channel defines the interface, which will be used for the
        * sharedsecretexchange.
        * If you call listen() afterwards, only connections from the given
        * destination are accepted.
        * If you otherwise call connect() afterwards, you will connect to the
        * given destination.
        */
        public CryptoSocket(Channel channel){
            this.channel = channel;
        }

        public SocketAddress listen(int port) throws IOException, SocketTimeoutException, CryptoSocketException {
            if (!this.verified) {
                throw new CryptoSocketException("call setSharedSecret or createSharedSecret before listen");
            }
            else {
                this.server = new ServerSocket(port);
                this.running = true;
                while(this.running){
                    while (this.running){
                        this.socket = this.server.accept();

                        if (!(this.channel.id.equals("") || this.channel.id.equals(":") || this.channel.id.equals("::")) && !this.channel.id.equals(socket.getRemoteSocketAddress().toString())){
                            socket.close();
                            continue;
                        }

                        break;
                    }

                    //System.out.println("Begin crypto protocol..");
                    // begin crypto protocol
                    this.out = this.socket.getOutputStream();
                    this.in = this.socket.getInputStream();
                    this.connected = true;
                    //check if the same sharedSecret was used
                    byte[] check = new byte[1];
                    new SecureRandom().nextBytes(check);
                    try{
                        this.write(check);
                    }
                    catch(UnverifiedException e){
                        throw new CryptoSocketException("impossible Error while sharedSecret check");
                    }
                    byte[] ans = new byte[1];
                    this.read(true, ans);
                    byte[] check2 = new byte[1];
                    this.read(true, check2);
                    check2[0] += 1;
                    try{
                        this.write(check2);
                    }
                    catch(UnverifiedException e){
                        throw new CryptoSocketException("impossible Error while sharedSecret check");
                    }
                    if (check[0]+1 != ans[0]) {
                        this.connected = false;
                        throw new CryptoSocketException("not the same sharedSecret is used. maybe the wrong device connected to you");
                    }
                    break;
                }
                return this.socket.getRemoteSocketAddress();
            }
        }

        public boolean connect() throws CryptoSocketException, IOException, SocketTimeoutException {
            if (!this.channel.id.equals("")){
                String tmp = new StringBuilder(this.channel.id).reverse().toString();

                if (!tmp.contains(":")){
                    throw new CryptoSocketException("for a MANUAL channel the identifier needs to be ip-port-sharedsecret combination e.g. 127.0.0.1:1337:mySharedSecretMySharedSecret1234 or ::1:4711:mySharedSecretMySharedSecret1234");
                }

                String[] stringParts = tmp.split(":", 3);


                if (3 != stringParts.length){
                    throw new CryptoSocketException("for a MANUAL channel the identifier needs to be ip-port-sharedsecret combination e.g. 127.0.0.1:1337:mySharedSecretMySharedSecret1234 or ::1:4711:mySharedSecretMySharedSecret1234");
                }

                stringParts[0] = new StringBuilder(stringParts[0]).reverse().toString();
                stringParts[1] = new StringBuilder(stringParts[1]).reverse().toString();
                stringParts[2] = new StringBuilder(stringParts[2]).reverse().toString();
                this.setSharedSecret(stringParts[0]);
                String serveraddress = stringParts[2];
                String port = stringParts[1];
                InetSocketAddress address = null;

                try {
                    address = new InetSocketAddress(serveraddress, Integer.parseInt(port));
                } catch (Exception e) {
                    throw new CryptoSocketException("for a MANUAL channel the identifier needs to be ip-port-sharedsecret combination e.g. 127.0.0.1:1337:mySharedSecretMySharedSecret1234 or ::1:4711:mySharedSecretMySharedSecret1234");
                }

                this.socket = new Socket(address.getHostString(), address.getPort());

            } else {
                throw new CryptoSocketException("for a MANUAL channel the identifier needs to be ip-port-sharedsecret combination e.g. 127.0.0.1:1337:mySharedSecretMySharedSecret1234 or ::1:4711:mySharedSecretMySharedSecret1234");
            }

            // begin crypto protocol
            //System.out.println("Begin crypto protocol..");
            this.out = this.socket.getOutputStream();
            this.in = this.socket.getInputStream();
            this.connected = true;
            //check if the same sharedSecret was used
            byte[] check = new byte[1];
            this.read(true, check);
            check[0] += 1;
            try{
                this.write(check);
            }
            catch(UnverifiedException e){
                throw new CryptoSocketException("impossible Error while sharedSecret check");
            }
            check = new byte[1];
            new SecureRandom().nextBytes(check);
            try{
                this.write(check);
            }
            catch(UnverifiedException e){
                throw new CryptoSocketException("impossible Error while sharedSecret check");
            }
            byte[] ans = new byte[1];
            this.read(true, ans);
            if (ans[0] != check[0]+1){
                this.connected = false;
                throw new CryptoSocketException("not the same sharedSecret is used. maybe you connected to the wrong device");
            }
            return true;
        }

        private void setSharedSecret(String sharedSecret) throws CryptoSocketException, IOException {
            this.cobject = new CryptoObject();
            byte[] byteSharedSecret = Base64.decode(sharedSecret);
            this.cobject.setSharedSecret(byteSharedSecret);
            this.verified = true;
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

        public int write(byte[] array) throws UnverifiedException, IllegalStateException, IOException, CryptoSocketException {
            if (!this.connected){
                throw new IllegalStateException("not connected, call listen() or connect() first!");
            }

            if (!this.verified){
                throw new UnverifiedException("you need to verifiy the channel first before you can write data! This channel may not trustworthy!");
            }

            byte[] ciphertext = null;
            try {
                ciphertext = this.cobject.encrypt(array);
            } catch(IllegalBlockSizeException ibs){
                return RETURN.INVALID_CIPHERTEXT.getValue();
            }
            this.out.write(ciphertext);
            this.out.flush();

            return RETURN.SUCCESS.getValue();
        }

        public int read(boolean blocking, byte [] data) throws IllegalStateException, CryptoSocketException, IOException {
            int readBytes = 0;
            int totalBytes = 0;

            if (!this.connected){
                throw new IllegalStateException("cryptosocket is not connected to communication partner!");
            }

            if (null == data){
                throw new CryptoSocketException("null byte array not allowed!");
            }

            int size = this.cobject.getOffset() + data.length;
            byte[] tmp = new byte[size];
            byte[] tmp2 = null;

            if (!blocking){
                int available = this.in.available();

                if (available < size){
                    return RETURN.NOT_AVAILABLE.getValue();
                } else {
                    totalBytes = size;
                }

                readBytes = this.in.read(tmp, 0, totalBytes);
            } else {
                while(totalBytes < size){
                    readBytes = this.in.read(tmp, totalBytes, tmp.length - totalBytes);

                    if (0 == readBytes){
                        try{
                            TimeUnit.SECONDS.sleep(1);
                        } catch(InterruptedException ie){
                            //throw new CryptoSocketException("ERROR Interrupted!");
                        }

                        continue;
                    }

                    if (0 > readBytes){
                        return RETURN.READ.getValue();
                    }

                    totalBytes = totalBytes + readBytes;
                }
            }

            try {
                tmp2 = this.cobject.decrypt(tmp);
            } catch (IllegalBlockSizeException ibs){
                return RETURN.INVALID_CIPHERTEXT.getValue();
            }

            if (null == tmp2){
                return RETURN.WRONG_TAG.getValue();
            }

            int copySize = tmp2.length > data.length ? data.length: tmp2.length;
            System.arraycopy(tmp2, 0, data, 0, copySize);
            totalBytes = copySize;
            return totalBytes;
        }

        public boolean hasNext() throws IOException{
            return this.cobject.getOffset() < this.in.available();
        }

        public void close(){
            //clearing attributes
            this.out = null;
            this.in = null;
            this.cobject = null;
            this.verified = false;
            this.connected = false;
            this.running = false;

            try{
                if (this.socket != null){
                    this.socket.close();
                }
                if (this.server != null){
                    this.server.close();
                }
            } catch(IOException ioe){}

            this.socket = null;
            this.server = null;
        }
}
