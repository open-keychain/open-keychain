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

package org.sufficientlysecure.keychain.securitytoken.usb;


import java.io.IOException;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.securitytoken.CommandApdu;
import org.sufficientlysecure.keychain.securitytoken.ResponseApdu;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TokenType;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TransportType;
import org.sufficientlysecure.keychain.securitytoken.Transport;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;

import static org.bouncycastle.util.encoders.Hex.toHexString;


/**
 * Based on USB CCID Specification rev. 1.1
 * http://www.usb.org/developers/docs/devclass_docs/DWG_Smart-Card_CCID_Rev110.pdf
 * Implements small subset of these features
 */
public class UsbTransport implements Transport {
    // https://github.com/Yubico/yubikey-personalization/blob/master/ykcore/ykdef.h
    private static final int VENDOR_YUBICO = 4176;
    private static final int PRODUCT_YUBIKEY_NEO_OTP_CCID = 273;
    private static final int PRODUCT_YUBIKEY_NEO_CCID = 274;
    private static final int PRODUCT_YUBIKEY_NEO_U2F_CCID = 277;
    private static final int PRODUCT_YUBIKEY_NEO_OTP_U2F_CCID = 278;
    private static final int PRODUCT_YUBIKEY_4_CCID = 1028;
    private static final int PRODUCT_YUBIKEY_4_OTP_CCID = 1029;
    private static final int PRODUCT_YUBIKEY_4_U2F_CCID = 1030;
    private static final int PRODUCT_YUBIKEY_4_OTP_U2F_CCID = 1031;

    // https://www.nitrokey.com/de/documentation/installation#p:nitrokey-pro&os:linux
    private static final int VENDOR_NITROKEY = 8352;
    private static final int PRODUCT_NITROKEY_PRO = 16648;
    private static final int PRODUCT_NITROKEY_START = 16913;
    private static final int PRODUCT_NITROKEY_STORAGE = 16649;

    private static final int VENDOR_FSIJ = 9035;
    private static final int VENDOR_LEDGER = 11415;

    private final UsbDevice usbDevice;
    private final UsbManager usbManager;

    private UsbDeviceConnection usbConnection;
    private UsbInterface usbInterface;
    private CcidTransportProtocol ccidTransportProtocol;
    private boolean allowUntestedUsbTokens;

    public static UsbTransport createUsbTransport(Context context, UsbDevice usbDevice) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        boolean allowUntestedUsbTokens = Preferences.getPreferences(context).getExperimentalUsbAllowUntested();

        return new UsbTransport(usbDevice, usbManager, allowUntestedUsbTokens);
    }

    private UsbTransport(UsbDevice usbDevice, UsbManager usbManager, boolean allowUntestedUsbTokens) {
        this.usbDevice = usbDevice;
        this.usbManager = usbManager;
        this.allowUntestedUsbTokens = allowUntestedUsbTokens;
    }

    @Override
    public void release() {
        if (usbConnection != null) {
            usbConnection.releaseInterface(usbInterface);
            usbConnection.close();
            usbConnection = null;
        }

        Timber.d("Usb transport disconnected");
    }

    /**
     * Check if device is was connected to and still is connected
     *
     * @return true if device is connected
     */
    @Override
    public boolean isConnected() {
        return usbConnection != null && usbManager.getDeviceList().containsValue(usbDevice) &&
                usbConnection.getSerial() != null;
    }

    /**
     * Check if Transport supports persistent connections e.g connections which can
     * handle multiple operations in one session
     *
     * @return true if transport supports persistent connections
     */
    @Override
    public boolean isPersistentConnectionAllowed() {
        return true;
    }

    /**
     * Connect to OTG device
     */
    @Override
    public void connect() throws IOException {
        usbInterface = getSmartCardInterface(usbDevice);
        if (usbInterface == null) {
            throw new UsbTransportException("USB error: CCID mode must be enabled (no class 11 interface)");
        }

        final Pair<UsbEndpoint, UsbEndpoint> ioEndpoints = getIoEndpoints(usbInterface);
        UsbEndpoint usbBulkIn = ioEndpoints.first;
        UsbEndpoint usbBulkOut = ioEndpoints.second;

        if (usbBulkIn == null || usbBulkOut == null) {
            throw new UsbTransportException("USB error: invalid class 11 interface");
        }

        usbConnection = usbManager.openDevice(usbDevice);
        if (usbConnection == null) {
            throw new UsbTransportException("USB error: failed to connect to device");
        }

        boolean tokenTypeSupported = SecurityTokenInfo.SUPPORTED_USB_TOKENS.contains(getTokenTypeIfAvailable());
        if (!allowUntestedUsbTokens && !tokenTypeSupported) {
            usbConnection.close();
            usbConnection = null;
            throw new UnsupportedUsbTokenException();
        }

        if (!usbConnection.claimInterface(usbInterface, true)) {
            throw new UsbTransportException("USB error: failed to claim interface");
        }

        CcidDescription ccidDescription = CcidDescription.fromRawDescriptors(usbConnection.getRawDescriptors());
        Timber.d("CCID Description: " + ccidDescription);
        CcidTransceiver transceiver = new CcidTransceiver(usbConnection, usbBulkIn, usbBulkOut, ccidDescription);

        ccidTransportProtocol = ccidDescription.getSuitableTransportProtocol();
        ccidTransportProtocol.connect(transceiver);
    }

    /**
     * Transmit and receive data
     *
     * @param data data to transmit
     * @return received data
     */
    @Override
    public ResponseApdu transceive(CommandApdu data) throws UsbTransportException {
        byte[] rawCommand = data.toBytes();
        if (Constants.DEBUG) {
            Timber.d("USB >> " + toHexString(rawCommand));
        }

        byte[] rawResponse = ccidTransportProtocol.transceive(rawCommand);
        if (Constants.DEBUG) {
            Timber.d("USB << " + toHexString(rawResponse));
        }

        return ResponseApdu.fromBytes(rawResponse);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final UsbTransport that = (UsbTransport) o;

        return usbDevice != null ? usbDevice.equals(that.usbDevice) : that.usbDevice == null;
    }

    @Override
    public int hashCode() {
        return usbDevice != null ? usbDevice.hashCode() : 0;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.USB;
    }

    @Nullable
    @Override
    public TokenType getTokenTypeIfAvailable() {
        return getTokenTypeFromUsbDeviceInfo(usbDevice.getVendorId(), usbDevice.getProductId(), usbConnection.getSerial());
    }

    @Nullable
    public static TokenType getTokenTypeFromUsbDeviceInfo(int vendorId, int productId, String serialNo) {
        switch (vendorId) {
            case VENDOR_YUBICO: {
                switch (productId) {
                    case PRODUCT_YUBIKEY_NEO_OTP_CCID:
                    case PRODUCT_YUBIKEY_NEO_CCID:
                    case PRODUCT_YUBIKEY_NEO_U2F_CCID:
                    case PRODUCT_YUBIKEY_NEO_OTP_U2F_CCID:
                        return TokenType.YUBIKEY_NEO;
                    case PRODUCT_YUBIKEY_4_CCID:
                    case PRODUCT_YUBIKEY_4_OTP_CCID:
                    case PRODUCT_YUBIKEY_4_U2F_CCID:
                    case PRODUCT_YUBIKEY_4_OTP_U2F_CCID:
                        return TokenType.YUBIKEY_4;
                }
                break;
            }
            case VENDOR_NITROKEY: {
                switch (productId) {
                    case PRODUCT_NITROKEY_PRO:
                        return TokenType.NITROKEY_PRO;
                    case PRODUCT_NITROKEY_START:
                        SecurityTokenInfo.Version gnukVersion = SecurityTokenInfo.parseGnukVersionString(serialNo);
                        boolean versionGreaterEquals125 = gnukVersion != null
                                && SecurityTokenInfo.Version.create("1.2.5").compareTo(gnukVersion) <= 0;
                        return versionGreaterEquals125 ? TokenType.NITROKEY_START_1_25_AND_NEWER : TokenType.NITROKEY_START_OLD;
                    case PRODUCT_NITROKEY_STORAGE:
                        return TokenType.NITROKEY_STORAGE;
                }
                break;
            }
            case VENDOR_FSIJ: {
                SecurityTokenInfo.Version gnukVersion = SecurityTokenInfo.parseGnukVersionString(serialNo);
                boolean versionGreaterEquals125 = gnukVersion != null
                        && SecurityTokenInfo.Version.create("1.2.5").compareTo(gnukVersion) <= 0;
                return versionGreaterEquals125 ? TokenType.GNUK_1_25_AND_NEWER : TokenType.GNUK_OLD;
            }
            case VENDOR_LEDGER: {
                return TokenType.LEDGER_NANO_S;
            }
        }

        Timber.d("Unknown USB token. Vendor ID: %s, Product ID: %s", vendorId, productId);
        return null;
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
            if (anInterface.getInterfaceClass() == UsbConstants.USB_CLASS_CSCID) {
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
}
