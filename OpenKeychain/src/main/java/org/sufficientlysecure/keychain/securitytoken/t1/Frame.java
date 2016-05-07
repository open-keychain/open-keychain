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

package org.sufficientlysecure.keychain.securitytoken.t1;

import android.support.annotation.NonNull;

import org.bouncycastle.util.Arrays;
import org.sufficientlysecure.keychain.securitytoken.UsbTransportException;

public class Frame {
    private static final byte IBLOCK_PCB_M_BIT_MASK = 1 << 5;
    private ChecksumType mChecksumType;
    private FrameType mFrameType;

    // Prologue
    private byte mNAD;
    private byte mPCB;
    private byte mLEN;
    // Information field
    private byte[] mAPDU; // 0..254
    // Epilogue
    private byte[] mEDC; // 1..2

    @NonNull
    public static Frame newFrame(@NonNull ChecksumType checksumType, @NonNull FrameType frameType,
                                 boolean hasMore, byte sequenceCounter, @NonNull byte[] apdu) throws UsbTransportException {
        final Frame res = new Frame();

        res.mNAD = 0;
        res.mPCB = (byte) (frameType.getSequenceBit() == -1 ? 0 : ((1 & sequenceCounter) << frameType.getSequenceBit()));
        if (hasMore && !frameType.isChainingSupported()) {
            throw new UsbTransportException("Invalid arguments");
        }
        if (hasMore) {
            res.mPCB |= 1 << 5;
        }

        if (apdu.length == 0) {
            throw new UsbTransportException("APDU is too short");
        }
        if (apdu.length > 254) {
            throw new UsbTransportException("APDU is too long; should be split");
        }
        res.mLEN = (byte) apdu.length;
        res.mAPDU = apdu;
        res.mChecksumType = checksumType;
        res.mFrameType = frameType;

        return res;
    }

    @NonNull
    public static Frame fromData(@NonNull ChecksumType checksumProtocol, @NonNull byte[] data)
            throws UsbTransportException {

        final Frame res = new Frame();
        res.mChecksumType = checksumProtocol;

        res.mNAD = data[0];
        res.mPCB = data[1];
        res.mLEN = data[2];

        int epilogueOffset = 3 + res.mLEN;
        res.mAPDU = Arrays.copyOfRange(data, 3, epilogueOffset);
        res.mEDC = Arrays.copyOfRange(data, epilogueOffset, data.length);

        if (!Arrays.areEqual(res.mEDC, res.mChecksumType.computeChecksum(Arrays.concatenate(
                new byte[]{res.mNAD, res.mPCB, res.mLEN},
                res.mAPDU)))) {
            throw new UsbTransportException("T=1 CRC doesnt match");
        }

        return res;
    }

    @NonNull
    public byte[] getBytes() {
        byte[] res = Arrays.concatenate(new byte[]{
                mNAD, mPCB, mLEN
        }, mAPDU);

        return Arrays.concatenate(res, mChecksumType.computeChecksum(res));
    }

    @NonNull
    public RError getRError() throws UsbTransportException {
        if (mFrameType != FrameType.R_BLOCK) {
            throw new UsbTransportException("getRerror called for non R block");
        }
        return RError.from(mPCB & 0x3);
    }

    public byte[] getAPDU() {
        return mAPDU;
    }

    @NonNull
    public FrameType getBlockType() throws UsbTransportException {
        return FrameType.fromPCB(mPCB);
    }

    public boolean hasMore() {
        return mFrameType.isChainingSupported() && (mPCB & IBLOCK_PCB_M_BIT_MASK) != 0;
    }

    public enum RError {
        NO_ERROR(0), EDC_ERROR(1), OTHER_ERROR(2);

        private int mLowBits;

        RError(int lowBits) {
            mLowBits = lowBits;
        }

        public int getLowBits() {
            return mLowBits;
        }

        @NonNull
        public static RError from(int i) throws UsbTransportException {
            for (final RError error : values()) {
                if (error.mLowBits == (i & 0x3)) {
                    return error;
                }
            }
            throw new UsbTransportException("Invalid R block error bits");
        }
    }

}
