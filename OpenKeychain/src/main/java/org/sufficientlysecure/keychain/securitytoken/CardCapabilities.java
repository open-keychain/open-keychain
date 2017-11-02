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

import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;
import org.sufficientlysecure.keychain.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

@SuppressWarnings("WeakerAccess")
class CardCapabilities {
    private static final int MASK_CHAINING = 1 << 7;
    private static final int MASK_EXTENDED = 1 << 6;

    private static final int STATUS_INDICATOR_NO_INFORMATION = 0x00;
    private static final int STATUS_INDICATOR_INITIALISATION_STATE = 0x03;
    private static final int STATUS_INDICATOR_OPERATIONAL_STATE = 0x05;

    private static final byte[] EXPECTED_PROCESSING_STATUS_BYTES = {(byte) 0x90, (byte) 0x00};

    private byte[] historicalBytes;
    private byte[] capabilityBytes;

    public CardCapabilities(byte[] historicalBytes) throws UsbTransportException {
        if ((historicalBytes == null) || (historicalBytes[0] != 0x00)) {
            throw new UsbTransportException("Invalid historical bytes category indicator byte");
        }
        this.historicalBytes = historicalBytes;
        capabilityBytes = getCapabilitiesBytes(historicalBytes);
    }

    public CardCapabilities() {
        capabilityBytes = null;
    }

    private static byte[] getCapabilitiesBytes(byte[] historicalBytes) {
        // Compact TLV
        ByteBuffer byteBuffer = ByteBuffer.wrap(historicalBytes, 1, historicalBytes.length - 2);
        while (byteBuffer.hasRemaining()) {
            byte tl = byteBuffer.get();
            if (tl == 0x73) { // Capabilities TL
                byte[] val = new byte[3];
                byteBuffer.get(val);
                return val;
            }
            byteBuffer.position(byteBuffer.position() + (tl & 0xF));
        }

        return null;
    }

    public boolean hasChaining() {
        return capabilityBytes != null && (capabilityBytes[2] & MASK_CHAINING) != 0;
    }

    public boolean hasExtended() {
        return capabilityBytes != null && (capabilityBytes[2] & MASK_EXTENDED) != 0;
    }

    public boolean hasLifeCycleManagement() throws UsbTransportException {
        byte[] lastBytes = Arrays.copyOfRange(historicalBytes, historicalBytes.length - 2, historicalBytes.length);
        boolean hasExpectedLastBytes = Arrays.equals(lastBytes, EXPECTED_PROCESSING_STATUS_BYTES);

        // Yk neo simply ends with 0x0000
        if (!hasExpectedLastBytes) {
            return true;
        }

        int statusIndicatorByte = historicalBytes[historicalBytes.length - 3];
        switch (statusIndicatorByte) {
            case STATUS_INDICATOR_NO_INFORMATION: {
                return false;
            }
            case STATUS_INDICATOR_INITIALISATION_STATE:
            case STATUS_INDICATOR_OPERATIONAL_STATE: {
                return true;
            }
            default: {
                throw new UsbTransportException("Status indicator byte not specified in OpenPGP specification");
            }
        }
    }
}
