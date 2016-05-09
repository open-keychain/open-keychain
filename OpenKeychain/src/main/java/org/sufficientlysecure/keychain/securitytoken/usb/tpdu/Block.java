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

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;

public class Block {
    protected static final int MAX_PAYLOAD_LEN = 254;
    protected static final int OFFSET_NAD = 0;
    protected static final int OFFSET_PCB = 1;
    protected static final int OFFSET_LEN = 2;
    protected static final int OFFSET_DATA = 3;

    protected byte[] mData;
    protected BlockChecksumType mChecksumType;

    public Block(BlockChecksumType checksumType, byte[] data) throws UsbTransportException {
        this.mChecksumType = checksumType;
        this.mData = data;

        int checksumOffset = this.mData.length - mChecksumType.getLength();
        byte[] checksum = mChecksumType.computeChecksum(data, 0, checksumOffset);
        if (!Arrays.areEqual(checksum, getEdc())) {
            throw new UsbTransportException("TPDU CRC doesn't match");
        }
    }

    protected Block(BlockChecksumType checksumType, byte nad, byte pcb, byte[] apdu)
            throws UsbTransportException {
        this.mChecksumType = checksumType;
        if (apdu.length > MAX_PAYLOAD_LEN) {
            throw new UsbTransportException("APDU is too long; should be split");
        }
        this.mData = Arrays.concatenate(
                new byte[]{nad, pcb, (byte) apdu.length},
                apdu,
                new byte[mChecksumType.getLength()]);

        int checksumOffset = this.mData.length - mChecksumType.getLength();
        byte[] checksum = mChecksumType.computeChecksum(this.mData, 0, checksumOffset);

        System.arraycopy(checksum, 0, this.mData, checksumOffset, mChecksumType.getLength());
    }

    protected Block(Block baseBlock) {
        this.mChecksumType = baseBlock.getChecksumType();
        this.mData = baseBlock.getRawData();
    }

    public byte getNad() {
        return mData[OFFSET_NAD];
    }

    public byte getPcb() {
        return mData[OFFSET_PCB];
    }

    public byte getLen() {
        return mData[OFFSET_LEN];
    }

    public byte[] getEdc() {
        return Arrays.copyOfRange(mData, mData.length - mChecksumType.getLength(), mData.length);
    }

    public BlockChecksumType getChecksumType() {
        return mChecksumType;
    }

    public byte[] getApdu() {
        return Arrays.copyOfRange(mData, OFFSET_DATA, mData.length - mChecksumType.getLength());
    }

    public byte[] getRawData() {
        return mData;
    }

    @Override
    public String toString() {
        return Hex.toHexString(mData);
    }
}
