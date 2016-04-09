package org.sufficientlysecure.keychain.smartcard;

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

public class UsbTransport implements Transport {
    private static final int CLASS_SMARTCARD = 11;
    private static final int TIMEOUT = 20 * 1000; // 2 s

    private final UsbManager mUsbManager;
    private final UsbDevice mUsbDevice;
    private UsbInterface mUsbInterface;
    private UsbEndpoint mBulkIn;
    private UsbEndpoint mBulkOut;
    private UsbDeviceConnection mConnection;
    private byte mCounter = 0;

    public UsbTransport(final UsbDevice usbDevice, final UsbManager usbManager) {
        mUsbDevice = usbDevice;
        mUsbManager = usbManager;
    }

    private void powerOff() throws TransportIoException {
        final byte[] iccPowerOff = {
                0x63,
                0x00, 0x00, 0x00, 0x00,
                0x00,
                mCounter++,
                0x00,
                0x00, 0x00
        };
        sendRaw(iccPowerOff);
        receive();
    }

    void powerOn() throws TransportIoException {
        final byte[] iccPowerOn = {
                0x62,
                0x00, 0x00, 0x00, 0x00,
                0x00,
                mCounter++,
                0x00,
                0x00, 0x00
        };
        sendRaw(iccPowerOn);
        receive();
    }

    /**
     * Get first class 11 (Chip/Smartcard) interface for the device
     *
     * @param device {@link UsbDevice} which will be searched
     * @return {@link UsbInterface} of smartcard or null if it doesn't exist
     */
    @Nullable
    private static UsbInterface getSmartCardInterface(final UsbDevice device) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            final UsbInterface anInterface = device.getInterface(i);
            if (anInterface.getInterfaceClass() == CLASS_SMARTCARD) {
                return anInterface;
            }
        }
        return null;
    }

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

    @Override
    public void release() {
        mConnection.releaseInterface(mUsbInterface);
        mConnection.close();
    }

    @Override
    public boolean isConnected() {
        // TODO: redo
        return mConnection != null && mUsbManager.getDeviceList().containsValue(mUsbDevice);
    }

    @Override
    public boolean allowPersistentConnection() {
        return true;
    }

    @Override
    public void connect() throws IOException {
        mUsbInterface = getSmartCardInterface(mUsbDevice);
        // throw if mUsbInterface == null
        final Pair<UsbEndpoint, UsbEndpoint> ioEndpoints = getIoEndpoints(mUsbInterface);
        mBulkIn = ioEndpoints.first;
        mBulkOut = ioEndpoints.second;
        // throw if any endpoint is null

        mConnection = mUsbManager.openDevice(mUsbDevice);
        // throw if connection is null
        mConnection.claimInterface(mUsbInterface, true);
        // check result

        powerOn();
        Log.d(Constants.TAG, "Usb transport connected");
    }

    @Override
    public byte[] sendAndReceive(byte[] data) throws TransportIoException {
        send(data);
        byte[] bytes;
        do {
            bytes = receive();
        } while (isXfrBlockNotReady(bytes));

        checkXfrBlockResult(bytes);
        return Arrays.copyOfRange(bytes, 10, bytes.length);
    }

    public void send(byte[] d) throws TransportIoException {
        int l = d.length;
        byte[] data = Arrays.concatenate(new byte[]{
                        0x6f,
                        (byte) l, (byte) (l >> 8), (byte) (l >> 16), (byte) (l >> 24),
                        0x00,
                        mCounter++,
                        0x00,
                        0x00, 0x00},
                d);

        int send = 0;
        while (send < data.length) {
            final int len = Math.min(mBulkIn.getMaxPacketSize(), data.length - send);
            sendRaw(Arrays.copyOfRange(data, send, send + len));
            send += len;
        }
    }

    public byte[] receive() throws TransportIoException {
        byte[] buffer = new byte[mBulkIn.getMaxPacketSize()];
        byte[] result = null;
        int readBytes = 0, totalBytes = 0;

        do {
            int res = mConnection.bulkTransfer(mBulkIn, buffer, buffer.length, TIMEOUT);
            if (res < 0) {
                throw new TransportIoException("USB error, failed to receive response " + res);
            }
            if (result == null) {
                if (res < 10) {
                    throw new TransportIoException("USB error, failed to receive ccid header");
                }
                totalBytes = ByteBuffer.wrap(buffer, 1, 4).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get() + 10;
                result = new byte[totalBytes];
            }
            System.arraycopy(buffer, 0, result, readBytes, res);
            readBytes += res;
        } while (readBytes < totalBytes);

        return result;
    }

    private void sendRaw(final byte[] data) throws TransportIoException {
        final int tr1 = mConnection.bulkTransfer(mBulkOut, data, data.length, TIMEOUT);
        if (tr1 != data.length) {
            throw new TransportIoException("USB error, failed to send data " + tr1);
        }
    }

    private byte getStatus(byte[] bytes) {
        return (byte) ((bytes[7] >> 6) & 0x03);
    }

    private void checkXfrBlockResult(byte[] bytes) throws TransportIoException {
        final byte status = getStatus(bytes);
        if (status != 0) {
            throw new TransportIoException("CCID error, status " + status + " error code: " + Hex.toHexString(bytes, 8, 1));
        }
    }

    private boolean isXfrBlockNotReady(byte[] bytes) {
        return getStatus(bytes) == 2;
    }

    public UsbDevice getUsbDevice() {
        return mUsbDevice;
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
}
