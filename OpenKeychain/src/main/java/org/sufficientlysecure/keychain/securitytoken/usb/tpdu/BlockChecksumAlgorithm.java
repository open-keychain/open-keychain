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

package org.sufficientlysecure.keychain.securitytoken.usb.tpdu;

import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;

enum BlockChecksumAlgorithm {
    LRC(1), CRC(2);

    private int mLength;

    BlockChecksumAlgorithm(int length) {
        mLength = length;
    }

    public byte[] computeChecksum(byte[] data, int offset, int len) throws UsbTransportException {
        if (this == LRC) {
            byte res = 0;
            for (int i = offset; i < len; i++) {
                res ^= data[i];
            }
            return new byte[]{res};
        } else {
            throw new UsbTransportException("CRC checksum is not implemented");
        }
    }

    public int getLength() {
        return mLength;
    }
}
