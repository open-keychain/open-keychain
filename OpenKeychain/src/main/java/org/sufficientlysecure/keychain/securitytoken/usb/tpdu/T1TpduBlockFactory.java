package org.sufficientlysecure.keychain.securitytoken.usb.tpdu;


import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;


class T1TpduBlockFactory {
    private BlockChecksumAlgorithm checksumType;

    T1TpduBlockFactory(BlockChecksumAlgorithm checksumType) {
        this.checksumType = checksumType;
    }

    Block fromBytes(byte[] data) throws UsbTransportException {
        byte pcbByte = data[Block.OFFSET_PCB];

        if ((pcbByte & IBlock.MASK_IBLOCK) == IBlock.MASK_VALUE_IBLOCK) {
            return new IBlock(checksumType, data);
        } else if ((pcbByte & SBlock.MASK_SBLOCK) == SBlock.MASK_VALUE_SBLOCK) {
            return new SBlock(checksumType, data);
        } else if ((pcbByte & RBlock.MASK_RBLOCK) == RBlock.MASK_VALUE_RBLOCK) {
            return new RBlock(checksumType, data);
        }

        throw new UsbTransportException("TPDU Unknown block type");
    }

    IBlock newIBlock(byte sequence, boolean chaining, byte[] apdu, int offset, int length)
            throws UsbTransportException {
        return new IBlock(checksumType, (byte) 0, sequence, chaining, apdu, offset, length);
    }

    RBlock createAckRBlock(byte receivedSeqNum) throws UsbTransportException {
        return new RBlock(checksumType, (byte) 0, (byte) (receivedSeqNum + 1));
    }
}
