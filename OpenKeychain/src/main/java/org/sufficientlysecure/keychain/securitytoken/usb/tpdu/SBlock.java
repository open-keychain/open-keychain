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


class SBlock extends Block {
    static final byte MASK_SBLOCK = (byte) 0b11000000;
    static final byte MASK_VALUE_SBLOCK = (byte) 0b11000000;

    SBlock(BlockChecksumAlgorithm checksumType, byte[] data) throws UsbTransportException {
        super(checksumType, data);

        if ((getPcb() & MASK_SBLOCK) != MASK_VALUE_SBLOCK) {
            throw new IllegalArgumentException("Data contained incorrect block type!");
        }
    }
}
