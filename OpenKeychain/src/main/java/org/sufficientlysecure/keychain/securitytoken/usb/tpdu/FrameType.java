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

public enum FrameType {
    I_BLOCK(0b00000000, 0b10000000, 6, true), // Information
    R_BLOCK(0b10000000, 0b11000000, 4, false), // Receipt ack
    S_BLOCK(0b11000000, 0b11000000, -1, false); // System

    private byte mValue;
    private byte mMask;
    private int mSequenceBit;
    private boolean mChainingSupported;

    FrameType(int value, int mask, int sequenceBit, boolean chaining) {
        // Accept ints just to avoid cast in creation
        this.mValue = (byte) value;
        this.mMask = (byte) mask;
        this.mSequenceBit = sequenceBit;
        this.mChainingSupported = chaining;
    }

    public static FrameType fromPCB(byte pcb) throws UsbTransportException {
        for (final FrameType frameType : values()) {
            if ((frameType.mMask & pcb) == frameType.mValue) {
                return frameType;
            }
        }
        throw new UsbTransportException("Invalid PCB byte");
    }

    public int getSequenceBit() {
        return mSequenceBit;
    }

    public boolean isChainingSupported() {
        return mChainingSupported;
    }
}
