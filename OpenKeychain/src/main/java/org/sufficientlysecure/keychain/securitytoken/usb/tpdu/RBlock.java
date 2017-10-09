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

package org.sufficientlysecure.keychain.securitytoken.usb.tpdu;

import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;

class RBlock extends Block {
    static final byte MASK_RBLOCK = (byte) 0b11000000;
    static final byte MASK_VALUE_RBLOCK = (byte) 0b10000000;

    private static final byte BIT_SEQUENCE = 4;

    RBlock(BlockChecksumAlgorithm checksumType, byte[] data) throws UsbTransportException {
        super(checksumType, data);

        if ((getPcb() & MASK_RBLOCK) != MASK_VALUE_RBLOCK) {
            throw new IllegalArgumentException("Data contained incorrect block type!");
        }

        if (getApdu().length != 0) {
            throw new UsbTransportException("Data in R-block");
        }
    }

    RBlock(BlockChecksumAlgorithm checksumType, byte nad, byte sequence)
            throws UsbTransportException {
        super(checksumType, nad, (byte) (MASK_VALUE_RBLOCK | ((sequence & 1) << BIT_SEQUENCE)), new byte[0], 0, 0);
    }

    public RError getError() throws UsbTransportException {
        return RError.from(getPcb());
    }

    enum RError {
        NO_ERROR(0), EDC_ERROR(1), OTHER_ERROR(2);

        private byte mLowBits;

        RError(int lowBits) {
            mLowBits = (byte) lowBits;
        }

        @NonNull
        public static RError from(byte i) throws UsbTransportException {
            for (final RError error : values()) {
                if (error.mLowBits == (i & 0x3)) {
                    return error;
                }
            }
            throw new UsbTransportException("Invalid R block error bits");
        }
    }
}
