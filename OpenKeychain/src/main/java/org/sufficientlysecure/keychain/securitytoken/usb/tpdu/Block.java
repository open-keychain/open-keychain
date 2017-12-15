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

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;

class Block {
    private static final int MAX_PAYLOAD_LEN = 254;
    private static final int OFFSET_NAD = 0;
    static final int OFFSET_PCB = 1;
    private static final int OFFSET_LEN = 2;
    private static final int OFFSET_DATA = 3;

    private final byte[] blockData;
    private final BlockChecksumAlgorithm checksumType;

    Block(BlockChecksumAlgorithm checksumType, byte[] data) throws UsbTransportException {
        this.checksumType = checksumType;
        this.blockData = data;

        int checksumOffset = blockData.length - checksumType.getLength();
        byte[] checksum = checksumType.computeChecksum(data, 0, checksumOffset);
        if (!Arrays.areEqual(checksum, getEdc())) {
            throw new UsbTransportException("TPDU CRC doesn't match");
        }
    }

    /*
    protected Block(BlockChecksumType checksumType, byte nad, byte pcb, byte[] apdu, int offset, int length)
            throws UsbTransportException {
        apdu = Arrays.copyOfRange(apdu, offset, offset + length);

        this.checksumType = checksumType;
        if (apdu.length > MAX_PAYLOAD_LEN) {
            throw new UsbTransportException("APDU is too long; should be split");
        }
        blockData = Arrays.concatenate(
                new byte[]{nad, pcb, (byte) apdu.length},
                apdu,
                new byte[checksumType.getLength()]);

        int checksumOffset = blockData.length - checksumType.getLength();
        byte[] checksum = checksumType.computeChecksum(blockData, 0, checksumOffset);

        System.arraycopy(checksum, 0, blockData, checksumOffset, checksumType.getLength());
    }
    */

//    /*
    Block(BlockChecksumAlgorithm checksumType, byte nad, byte pcb, byte[] apdu, int offset, int length)
            throws UsbTransportException {
        this.checksumType = checksumType;
        if (length > MAX_PAYLOAD_LEN) {
            throw new IllegalArgumentException("Payload too long! " + length + " > " + MAX_PAYLOAD_LEN);
        }

        int lengthWithoutChecksum = length + 3;
        int checksumLength = this.checksumType.getLength();

        blockData = new byte[lengthWithoutChecksum + checksumLength];
        blockData[0] = nad;
        blockData[1] = pcb;
        blockData[2] = (byte) length;
        System.arraycopy(apdu, offset, blockData, 3, length);

        byte[] checksum = this.checksumType.computeChecksum(blockData, 0, lengthWithoutChecksum);
        System.arraycopy(checksum, 0, blockData, lengthWithoutChecksum, checksumLength);
    }

    public byte getNad() {
        return blockData[OFFSET_NAD];
    }

    public byte getPcb() {
        return blockData[OFFSET_PCB];
    }

    public byte getLen() {
        return blockData[OFFSET_LEN];
    }

    public byte[] getEdc() {
        return Arrays.copyOfRange(blockData, blockData.length - checksumType.getLength(), blockData.length);
    }

    public BlockChecksumAlgorithm getChecksumType() {
        return checksumType;
    }

    public byte[] getApdu() {
        return Arrays.copyOfRange(blockData, OFFSET_DATA, blockData.length - checksumType.getLength());
    }

    public byte[] getRawData() {
        return blockData;
    }

    @Override
    public String toString() {
        return Hex.toHexString(blockData);
    }

}
