/*
 * Copyright (C) 2016 Nikita Mikhailov <nikita.s.mikhailov@gmail.com>
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

package org.sufficientlysecure.keychain.securitytoken.usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CcidTransceiver {
    private static final int TIMEOUT = 20 * 1000; // 20s

    private byte mCounter;
    private UsbDeviceConnection mConnection;
    private UsbEndpoint mBulkIn;
    private UsbEndpoint mBulkOut;

    public CcidTransceiver(final UsbDeviceConnection connection, final UsbEndpoint bulkIn,
                           final UsbEndpoint bulkOut) {

        mConnection = connection;
        mBulkIn = bulkIn;
        mBulkOut = bulkOut;
    }

    public byte[] receiveRaw() throws UsbTransportException {
        byte[] bytes;
        do {
            bytes = receive();
        } while (isDataBlockNotReady(bytes));

        checkDataBlockResponse(bytes);

        return Arrays.copyOfRange(bytes, 10, bytes.length);
    }

    /**
     * Power of ICC
     * Spec: 6.1.1 PC_to_RDR_IccPowerOn
     *
     * @throws UsbTransportException
     */
    @NonNull
    public byte[] iccPowerOn() throws UsbTransportException {
        final byte[] iccPowerCommand = {
                0x62,
                0x00, 0x00, 0x00, 0x00,
                0x00,
                mCounter++,
                0x00,
                0x00, 0x00
        };

        sendRaw(iccPowerCommand);

        long startTime = System.currentTimeMillis();
        byte[] atr = null;
        while (true) {
            try {
                atr = receiveRaw();
                break;
            } catch (Exception e) {
                // Try more startTime
                if (System.currentTimeMillis() - startTime > TIMEOUT) {
                    break;
                }
            }
            SystemClock.sleep(100);
        }

        if (atr == null) {
            throw new UsbTransportException("Couldn't power up Security Token");
        }

        return atr;
    }

    /**
     * Transmits XfrBlock
     * 6.1.4 PC_to_RDR_XfrBlock
     * @param payload payload to transmit
     * @throws UsbTransportException
     */
    public void sendXfrBlock(byte[] payload) throws UsbTransportException {
        int l = payload.length;
        byte[] data = Arrays.concatenate(new byte[]{
                        0x6f,
                        (byte) l, (byte) (l >> 8), (byte) (l >> 16), (byte) (l >> 24),
                        0x00,
                        mCounter++,
                        0x00,
                        0x00, 0x00},
                payload);

        int send = 0;
        while (send < data.length) {
            final int len = Math.min(mBulkIn.getMaxPacketSize(), data.length - send);
            sendRaw(Arrays.copyOfRange(data, send, send + len));
            send += len;
        }
    }

    public byte[] receive() throws UsbTransportException {
        byte[] buffer = new byte[mBulkIn.getMaxPacketSize()];
        byte[] result = null;
        int readBytes = 0, totalBytes = 0;

        do {
            int res = mConnection.bulkTransfer(mBulkIn, buffer, buffer.length, TIMEOUT);
            if (res < 0) {
                throw new UsbTransportException("USB error - failed to receive response " + res);
            }
            if (result == null) {
                if (res < 10) {
                    throw new UsbTransportException("USB-CCID error - failed to receive CCID header");
                }
                totalBytes = ByteBuffer.wrap(buffer, 1, 4).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get() + 10;
                result = new byte[totalBytes];
            }
            System.arraycopy(buffer, 0, result, readBytes, res);
            readBytes += res;
        } while (readBytes < totalBytes);

        return result;
    }

    private void sendRaw(final byte[] data) throws UsbTransportException {
        final int tr1 = mConnection.bulkTransfer(mBulkOut, data, data.length, TIMEOUT);
        if (tr1 != data.length) {
            throw new UsbTransportException("USB error - failed to transmit data " + tr1);
        }
    }

    private static byte getStatus(byte[] bytes) {
        return (byte) ((bytes[7] >> 6) & 0x03);
    }

    private void checkDataBlockResponse(byte[] bytes) throws UsbTransportException {
        final byte status = getStatus(bytes);
        if (status != 0) {
            throw new UsbTransportException("USB-CCID error - status " + status + " error code: " + Hex.toHexString(bytes, 8, 1));
        }
    }

    private static boolean isDataBlockNotReady(byte[] bytes) {
        return getStatus(bytes) == 2;
    }
}
