package org.sufficientlysecure.keychain.javacard;

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

public class UsbTransport implements Transport {
    private static final int CLASS_SMARTCARD = 11;
    private static final int TIMEOUT = 1000; // 1 s

    private final UsbManager mUsbManager;
    private final UsbDevice mUsbDevice;
    private final UsbInterface mUsbInterface;
    private final UsbEndpoint mBulkIn;
    private final UsbEndpoint mBulkOut;
    private final UsbDeviceConnection mConnection;
    private byte counter = 0;

    public UsbTransport(final UsbDevice usbDevice, final UsbManager usbManager) throws TransportIoException {
        mUsbDevice = usbDevice;
        mUsbManager = usbManager;

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

        final byte[] iccPowerOn = {
                0x62,
                0x00, 0x00, 0x00, 0x00,
                0x00,
                counter++,
                0x03,
                0x00, 0x00
        };
        sendRaw(iccPowerOn);
        receiveRaw();
        // Check result
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
        return mUsbManager.getDeviceList().containsValue(mUsbDevice);
    }

    @Override
    public byte[] sendAndReceive(final byte[] data) throws TransportIoException {
        send(data);
        return receive();
    }

    public void send(final byte[] d) throws TransportIoException {
        int l = d.length;
        byte[] data = Arrays.concatenate(new byte[]{
                        0x6f,
                        (byte) l, (byte) (l >> 8), (byte) (l >> 16), (byte) (l >> 24),
                        0x00,
                        counter++,
                        0x01,
                        0x00, 0x00},
                d);
        sendRaw(data);
    }

    public byte[] receive() throws TransportIoException {
        final byte[] bytes = receiveRaw();
        return Arrays.copyOfRange(bytes, 10, bytes.length);
    }

    private void sendRaw(final byte[] data) throws TransportIoException {
        final int tr1 = mConnection.bulkTransfer(mBulkOut, data, data.length, TIMEOUT);
        if (tr1 != data.length) {
            throw new TransportIoException("USB error, failed to send data " + tr1);
        }
    }

    private byte[] receiveRaw() throws TransportIoException {
        byte[] buffer = new byte[1024];

        int res = mConnection.bulkTransfer(mBulkIn, buffer, buffer.length, TIMEOUT);
        if (res < 0) {
            throw new TransportIoException("USB error, failed to receive response " + res);
        }

        return Arrays.copyOfRange(buffer, 0, res);
    }
}
