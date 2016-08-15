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

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.securitytoken.usb.CcidTransceiver;
import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;
import org.sufficientlysecure.keychain.securitytoken.usb.CcidTransportProtocol;
import org.sufficientlysecure.keychain.util.Log;

public class T1TpduProtocol implements CcidTransportProtocol {
    private final static int MAX_FRAME_LEN = 254;

    private byte mCounter = 0;
    private CcidTransceiver mTransceiver;
    private BlockChecksumType mChecksumType;

    public T1TpduProtocol(final CcidTransceiver transceiver) throws UsbTransportException {
        mTransceiver = transceiver;

        // Connect
        byte[] atr = mTransceiver.iccPowerOn();
        Log.d(Constants.TAG, "Usb transport connected  T1/TPDU, ATR=" + Hex.toHexString(atr));
        // TODO: set checksum from atr
        mChecksumType = BlockChecksumType.LRC;

        // PPS all auto
        pps();
    }

    protected void pps() throws UsbTransportException {
        byte[] pps = new byte[]{(byte) 0xFF, 1, (byte) (0xFF ^ 1)};

        mTransceiver.sendXfrBlock(pps);

        byte[] ppsResponse = mTransceiver.receiveRaw();

        Log.d(Constants.TAG, "PPS response " + Hex.toHexString(ppsResponse));
    }

    public byte[] transceive(@NonNull byte[] apdu) throws UsbTransportException {
        int start = 0;

        if (apdu.length == 0) {
            throw new UsbTransportException("Cant transcive zero-length apdu(tpdu)");
        }

        Block responseBlock = null;
        while (apdu.length - start > 0) {
            boolean hasMore = start + MAX_FRAME_LEN < apdu.length;
            int len = Math.min(MAX_FRAME_LEN, apdu.length - start);

            // Send next frame
            Block block = newIBlock(mCounter++, hasMore, Arrays.copyOfRange(apdu, start, start + len));

            mTransceiver.sendXfrBlock(block.getRawData());

            // Receive I or R block
            responseBlock = getBlockFromResponse(mTransceiver.receiveRaw());

            start += len;

            if (responseBlock instanceof SBlock) {
                Log.d(Constants.TAG, "S-Block received " + responseBlock.toString());
                // just ignore
            } else if (responseBlock instanceof RBlock) {
                Log.d(Constants.TAG, "R-Block received " + responseBlock.toString());
                if (((RBlock) responseBlock).getError() != RBlock.RError.NO_ERROR) {
                    throw new UsbTransportException("R-Block reports error "
                            + ((RBlock) responseBlock).getError());
                }
            } else {  // I block
                if (start != apdu.length) {
                    throw new UsbTransportException("T1 frame response underflow");
                }
                break;
            }
        }

        // Receive
        if (responseBlock == null || !(responseBlock instanceof IBlock))
            throw new UsbTransportException("Invalid tpdu sequence state");

        byte[] responseApdu = responseBlock.getApdu();

        while (((IBlock) responseBlock).getChaining()) {
            Block ackBlock = newRBlock((byte) (((IBlock) responseBlock).getSequence() + 1));
            mTransceiver.sendXfrBlock(ackBlock.getRawData());

            responseBlock = getBlockFromResponse(mTransceiver.receiveRaw());

            if (responseBlock instanceof IBlock) {
                responseApdu = Arrays.concatenate(responseApdu, responseBlock.getApdu());
            } else {
                Log.d(Constants.TAG, "Response block received " + responseBlock.toString());
                throw new UsbTransportException("Response: invalid state - invalid block received");
            }
        }

        return responseApdu;
    }

    // Factory methods
    public Block getBlockFromResponse(byte[] data) throws UsbTransportException {
        final Block baseBlock = new Block(mChecksumType, data);

        if ((baseBlock.getPcb() & IBlock.MASK_RBLOCK) == IBlock.MASK_VALUE_RBLOCK) {
            return new IBlock(baseBlock);
        } else if ((baseBlock.getPcb() & SBlock.MASK_SBLOCK) == SBlock.MASK_VALUE_SBLOCK) {
            return new SBlock(baseBlock);
        } else if ((baseBlock.getPcb() & RBlock.MASK_RBLOCK) == RBlock.MASK_VALUE_RBLOCK) {
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
}
