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

import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;

import java.nio.ByteBuffer;

public class CardCapabilities {
    private static final int MASK_CHAINING = 1 << 7;
    private static final int MASK_EXTENDED = 1 << 6;

    private byte[] mCapabilityBytes;

    public CardCapabilities(byte[] historicalBytes) throws UsbTransportException {
        if (historicalBytes[0] != 0x00) {
            throw new UsbTransportException("Invalid historical bytes category indicator byte");
        }

        mCapabilityBytes = getCapabilitiesBytes(historicalBytes);
    }

    public CardCapabilities() {
        mCapabilityBytes = null;
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
        return mCapabilityBytes != null && (mCapabilityBytes[2] & MASK_CHAINING) != 0;
    }

    public boolean hasExtended() {
        return mCapabilityBytes != null && (mCapabilityBytes[2] & MASK_EXTENDED) != 0;
    }
}
