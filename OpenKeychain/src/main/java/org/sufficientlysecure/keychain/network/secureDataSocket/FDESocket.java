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

import org.sufficientlysecure.keychain.network.secureDataSocket.CryptoSocketInterface.RETURN;

import java.security.SecureRandom;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.net.SocketAddress;

class FDESocket {
    //cannot be larger than 32767
    private final int PAYLOAD_SIZE;
    private final int PACKET_SIZE;
    private CryptoSocket mCryptoSocket;

    private FDESocket(CryptoSocketInterface.Channel c, int payloadSize) throws CryptoSocketException {
        this.PAYLOAD_SIZE = payloadSize;
        this.PACKET_SIZE = PAYLOAD_SIZE + 2;
        if (PAYLOAD_SIZE > 32767)
            throw new CryptoSocketException("too large PAYLOAD_SIZE");
        if (PAYLOAD_SIZE < 1)
            throw new CryptoSocketException("too small PAYLOAD_SIZE");
        mCryptoSocket = new CryptoSocket(c);
    }

    FDESocket(CryptoSocketInterface.Channel c) throws CryptoSocketException {
        this(c, 1024);
    }

    public SocketAddress listen(int port) throws CryptoSocketException, IOException {
        return mCryptoSocket.listen(port);
    }

    public boolean connect() throws CryptoSocketException, IOException {
        return mCryptoSocket.connect();
    }

    String createSharedSecret() throws CryptoSocketException, IOException {
        return mCryptoSocket.createSharedSecret();
    }

    public byte[] read() throws CryptoSocketException, IllegalStateException, IOException {
        boolean done = false;
        byte[] data = new byte[0];
        //stop reading after specific size?
        while (!done) {
            byte[] newData = new byte[PACKET_SIZE];
            //maybe read more times here?
            mCryptoSocket.read(true, newData);
            done = isLast(newData);
            newData = getContent(newData);
            data = merge(data, newData);
        }
        return data;
    }

    public int write(byte[] array) throws UnverifiedException, IllegalStateException,
            CryptoSocketException, IOException {
        byte[][] packets = splitUp(array);
        for (byte[] packet : packets) {
            if (mCryptoSocket.write(packet) != RETURN.SUCCESS.getValue()) {
                byte[] end = {(byte) 0, (byte) 0x80};
                mCryptoSocket.write(end);
                return RETURN.INVALID_CIPHERTEXT.getValue();
            }
        }
        return RETURN.SUCCESS.getValue();
    }

    public void close() {
        this.mCryptoSocket.close();
    }

    private byte[][] splitUp(byte[] array) {
        int parts = array.length / PAYLOAD_SIZE + 1;
        byte[][] out = new byte[parts][PACKET_SIZE];
        for (int i = 0; i < parts; i++) {
            byte header1 = (byte) (PAYLOAD_SIZE % 256);
            byte header2 = (byte) (PAYLOAD_SIZE / 256);
            int partSize = PAYLOAD_SIZE;

            if (i == parts - 1) {
                int rest = array.length % PAYLOAD_SIZE;
                header1 = (byte) (rest % 256);
                header2 = (byte) (rest / 256);
                /*has no following packets*/
                header2 = (byte) (header2 | 0x80);
                partSize = rest;
                SecureRandom sr = new SecureRandom();
                sr.nextBytes(out[i]);
            }
            out[i][0] = header1;
            out[i][1] = header2;
            System.arraycopy(array, i * PAYLOAD_SIZE, out[i], 2, partSize);
        }
        return out;
    }

    private boolean isLast(byte[] packet) throws CryptoSocketException {
        if (packet.length != PACKET_SIZE) {
            throw new CryptoSocketException("invalid packetsize!");
        }
        return packet[1] < 0;
    }

    private byte[] getContent(byte[] packet) throws CryptoSocketException {
        int size = getContentSize(packet);
        byte[] out = new byte[size];
        System.arraycopy(packet, 2, out, 0, size);
        return out;
    }

    private int getContentSize(byte[] packet) throws CryptoSocketException {
        if (packet.length != PACKET_SIZE) {
            throw new CryptoSocketException("invalid packetsize!");
        }
        int size = (packet[0] & 0xFF) + (packet[1] & 0x7F) * 256;
        if (size > PAYLOAD_SIZE) {
            throw new CryptoSocketException("invalid lengthfield!");
        }
        return size;
    }

    private byte[] merge(byte[] arr1, byte[] arr2) {
        byte[] out = new byte[arr1.length + arr2.length];
        System.arraycopy(arr1, 0, out, 0, arr1.length);
        System.arraycopy(arr2, 0, out, arr1.length, arr2.length);
        return out;
    }
}
