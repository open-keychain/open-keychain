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

package org.sufficientlysecure.keychain.securitytoken.usb.tpdu.block;

import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;
import org.sufficientlysecure.keychain.securitytoken.usb.tpdu.BlockChecksumType;

public class BlockFactory {
    private BlockChecksumType mChecksumType = BlockChecksumType.LRC;

    public Block getBlockFromResponse(byte[] data) throws UsbTransportException {
        final Block baseBlock = new Block(mChecksumType, data);

        if ((baseBlock.getPcb() & 0b10000000) == 0b00000000) {
            return new IBlock(baseBlock);
        } else if ((baseBlock.getPcb() & 0b11000000) == 0b11000000) {
            return new SBlock(baseBlock);
        } else if ((baseBlock.getPcb() & 0b11000000) == 0b10000000) {
            return new RBlock(baseBlock);
        }

        throw new UsbTransportException("TPDU Unknown block type");
    }

    public IBlock newIBlock(byte sequence, boolean chaining, byte[] apdu) throws UsbTransportException {
        return new IBlock(mChecksumType, (byte) 0, sequence, chaining, apdu);
    }

    public RBlock newRBlock(byte sequence) throws UsbTransportException {
        return new RBlock(mChecksumType, (byte) 0, sequence);
    }

    public void setChecksumType(final BlockChecksumType checksumType) {
        mChecksumType = checksumType;
    }
}
