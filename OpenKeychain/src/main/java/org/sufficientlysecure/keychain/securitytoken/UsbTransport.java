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

package org.sufficientlysecure.keychain.securitytoken;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Based on USB CCID Specification rev. 1.1
 * http://www.usb.org/developers/docs/devclass_docs/DWG_Smart-Card_CCID_Rev110.pdf
 * Implements small subset of these features
 */
public class UsbTransport implements Transport {
    private static final int USB_CLASS_SMARTCARD = 11;
    private static final int TIMEOUT = 20 * 1000; // 20s

    private final UsbManager mUsbManager;
    private final UsbDevice mUsbDevice;
    private UsbInterface mUsbInterface;
    private UsbEndpoint mBulkIn;
    private UsbEndpoint mBulkOut;
    private UsbDeviceConnection mConnection;
    private byte mCounter;

    public UsbTransport(UsbDevice usbDevice, UsbManager usbManager) {
        mUsbDevice = usbDevice;
        mUsbManager = usbManager;
    }


    /**
     * Manage ICC power, Yubikey requires to power on ICC
     * Spec: 6.1.1 PC_to_RDR_IccPowerOn; 6.1.2 PC_to_RDR_IccPowerOff
     *
     * @param on true to turn ICC on, false to turn it off
     * @throws UsbTransportException
     */
    private void setIccPower(boolean on) throws UsbTransportException {
        final byte[] iccPowerCommand = {
                (byte) (on ? 0x62 : 0x63),
                0x00, 0x00, 0x00, 0x00,
                0x00,
                mCounter++,
                0x00,
                0x00, 0x00
        };

        sendRaw(iccPowerCommand);
        byte[] bytes;
        do {
            bytes = receive();
        } while (isDataBlockNotReady(bytes));
        checkDataBlockResponse(bytes);
    }

    /**
     * Get first class 11 (Chip/Smartcard) interface of the device
     *
     * @param device {@link UsbDevice} which will be searched
     * @return {@link UsbInterface} of smartcard or null if it doesn't exist
     */
    @Nullable
    private static UsbInterface getSmartCardInterface(UsbDevice device) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface anInterface = device.getInterface(i);
            if (anInterface.getInterfaceClass() == USB_CLASS_SMARTCARD) {
                return anInterface;
            }
        }
        return null;
    }

    /**
     * Get device's bulk-in and bulk-out endpoints
     *
     * @param usbInterface usb device interface
     * @return pair of builk-in and bulk-out endpoints respectively
     */
    @NonNull
    private static Pair<UsbEndpoint, UsbEndpoint> getIoEndpoints(final UsbInterface usbInterface) {
        UsbEndpoint bulkIn = null, bulkOut = null;
        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
            final UsbEndpoint endpoint = usbInterface.getEndpoint(i);
            if (endpoint.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) {
                continue;
            }

            if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                bulkIn = endpoint;
            } else if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                bulkOut = endpoint;
            }
        }
        return new Pair<>(bulkIn, bulkOut);
    }

    /**
     * Release interface and disconnect
     */
    @Override
    public void release() {
        if (mConnection != null) {
            mConnection.releaseInterface(mUsbInterface);
            mConnection.close();
            mConnection = null;
        }

        Log.d(Constants.TAG, "Usb transport disconnected");
    }

    /**
     * Check if device is was connected to and still is connected
     * @return true if device is connected
     */
    @Override
    public boolean isConnected() {
        return mConnection != null && mUsbManager.getDeviceList().containsValue(mUsbDevice) &&
                mConnection.getSerial() != null;
    }

    /**
     * Check if Transport supports persistent connections e.g connections which can
     * handle multiple operations in one session
     * @return true if transport supports persistent connections
     */
    @Override
    public boolean isPersistentConnectionAllowed() {
        return true;
    }

    /**
     * Connect to OTG device
     * @throws IOException
     */
    @Override
    public void connect() throws IOException {
        mCounter = 0;
        mUsbInterface = getSmartCardInterface(mUsbDevice);
        if (mUsbInterface == null) {
            // Shouldn't happen as we whitelist only class 11 devices
            throw new UsbTransportException("USB error - device doesn't have class 11 interface");
        }

        final Pair<UsbEndpoint, UsbEndpoint> ioEndpoints = getIoEndpoints(mUsbInterface);
        mBulkIn = ioEndpoints.first;
        mBulkOut = ioEndpoints.second;

        if (mBulkIn == null || mBulkOut == null) {
            throw new UsbTransportException("USB error - invalid class 11 interface");
        }

        mConnection = mUsbManager.openDevice(mUsbDevice);
        if (mConnection == null) {
            throw new UsbTransportException("USB error - failed to connect to device");
        }

        if (!mConnection.claimInterface(mUsbInterface, true)) {
            throw new UsbTransportException("USB error - failed to claim interface");
        }

        setIccPower(true);
        Log.d(Constants.TAG, "Usb transport connected");
    }

    /**
     * Transmit and receive data
     * @param data data to transmit
     * @return received data
     * @throws UsbTransportException
     */
    @Override
    public byte[] transceive(byte[] data) throws UsbTransportException {
        sendXfrBlock(data);
        byte[] bytes;
        do {
            bytes = receive();
        } while (isDataBlockNotReady(bytes));

        checkDataBlockResponse(bytes);
        // Discard header
        return Arrays.copyOfRange(bytes, 10, bytes.length);
    }

    /**
     * Transmits XfrBlock
     * 6.1.4 PC_to_RDR_XfrBlock
     * @param payload payload to transmit
     * @throws UsbTransportException
     */
    private void sendXfrBlock(byte[] payload) throws UsbTransportException {
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

    private byte[] receive() throws UsbTransportException {
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

    private byte getStatus(byte[] bytes) {
        return (byte) ((bytes[7] >> 6) & 0x03);
    }

    private void checkDataBlockResponse(byte[] bytes) throws UsbTransportException {
        final byte status = getStatus(bytes);
        if (status != 0) {
            throw new UsbTransportException("USB-CCID error - status " + status + " error code: " + Hex.toHexString(bytes, 8, 1));
        }
    }

    private boolean isDataBlockNotReady(byte[] bytes) {
        return getStatus(bytes) == 2;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final UsbTransport that = (UsbTransport) o;

        return mUsbDevice != null ? mUsbDevice.equals(that.mUsbDevice) : that.mUsbDevice == null;
    }

    @Override
    public int hashCode() {
        return mUsbDevice != null ? mUsbDevice.hashCode() : 0;
    }

    public UsbDevice getUsbDevice() {
        return mUsbDevice;
    }
}
