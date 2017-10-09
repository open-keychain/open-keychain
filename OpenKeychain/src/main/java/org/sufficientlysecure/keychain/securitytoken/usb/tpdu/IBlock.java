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


import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;


class IBlock extends Block {
    static final byte MASK_IBLOCK = (byte) 0b10000000;
    static final byte MASK_VALUE_IBLOCK = (byte) 0b00000000;

    private static final byte BIT_SEQUENCE = 6;
    private static final byte BIT_CHAINING = 5;

    IBlock(BlockChecksumAlgorithm checksumType, byte[] data) throws UsbTransportException {
        super(checksumType, data);

        if ((getPcb() & MASK_IBLOCK) != MASK_VALUE_IBLOCK) {
            throw new IllegalArgumentException("Data contained incorrect block type!");
        }
    }

    IBlock(BlockChecksumAlgorithm checksumType, byte nad, byte sequence, boolean chaining, byte[] apdu, int offset,
            int length)
            throws UsbTransportException {
        super(checksumType, nad,
                (byte) (((sequence & 1) << BIT_SEQUENCE) | (chaining ? 1 << BIT_CHAINING : 0)),
                apdu, offset, length);
    }

    byte getSequence() {
        return (byte) ((getPcb() >> BIT_SEQUENCE) & 1);
    }

    boolean getChaining() {
        return ((getPcb() >> BIT_CHAINING) & 1) != 0;
    }
}
