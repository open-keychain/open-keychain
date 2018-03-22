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


import android.support.annotation.NonNull;

import org.bouncycastle.util.Arrays;
import org.sufficientlysecure.keychain.securitytoken.usb.CcidTransceiver;
import org.sufficientlysecure.keychain.securitytoken.usb.CcidTransceiver.CcidDataBlock;
import org.sufficientlysecure.keychain.securitytoken.usb.CcidTransportProtocol;
import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;
import timber.log.Timber;


/* T=1 Protocol, see http://www.icedev.se/proxmark3/docs/ISO-7816.pdf, Part 11 */
public class T1TpduProtocol implements CcidTransportProtocol {
    private final static int MAX_FRAME_LEN = 254;

    private static final byte PPS_PPPSS = (byte) 0xFF;
    private static final byte PPS_PPS0_T1 = 1;
    @SuppressWarnings("PointlessBitwiseExpression") // constructed per spec
    private static final byte PPS_PCK = (byte) (PPS_PPPSS ^ PPS_PPS0_T1);

    private CcidTransceiver ccidTransceiver;
    private T1TpduBlockFactory blockFactory;

    private byte sequenceCounter = 0;


    public void connect(@NonNull CcidTransceiver ccidTransceiver) throws UsbTransportException {
        if (this.ccidTransceiver != null) {
            throw new IllegalStateException("Protocol already connected!");
        }
        this.ccidTransceiver = ccidTransceiver;

        this.ccidTransceiver.iccPowerOn();

        // TODO: set checksum from atr
        blockFactory = new T1TpduBlockFactory(BlockChecksumAlgorithm.LRC);

        boolean skipPpsExchange = ccidTransceiver.hasAutomaticPps();
        if (!skipPpsExchange) {
            performPpsExchange();
        }
    }

    private void performPpsExchange() throws UsbTransportException {
        // Perform PPS, see ISO-7816, Part 9
        byte[] pps = { PPS_PPPSS, PPS_PPS0_T1, PPS_PCK };

        CcidDataBlock response = ccidTransceiver.sendXfrBlock(pps);

        if (!Arrays.areEqual(pps, response.getData())) {
            throw new UsbTransportException("Protocol and parameters (PPS) negotiation failed!");
        }
    }

    public byte[] transceive(@NonNull byte[] apdu) throws UsbTransportException {
        if (this.ccidTransceiver == null) {
            throw new IllegalStateException("Protocol not connected!");
        }

        if (apdu.length == 0) {
            throw new UsbTransportException("Cant transcive zero-length apdu(tpdu)");
        }

        IBlock responseBlock = sendChainedData(apdu);
        return receiveChainedResponse(responseBlock);
    }

    private IBlock sendChainedData(@NonNull byte[] apdu) throws UsbTransportException {
        int sentLength = 0;
        while (sentLength < apdu.length) {
            boolean hasMore = sentLength + MAX_FRAME_LEN < apdu.length;
            int len = Math.min(MAX_FRAME_LEN, apdu.length - sentLength);

            Block sendBlock = blockFactory.newIBlock(sequenceCounter++, hasMore, apdu, sentLength, len);
            CcidDataBlock response = ccidTransceiver.sendXfrBlock(sendBlock.getRawData());
            Block responseBlock = blockFactory.fromBytes(response.getData());

            sentLength += len;

            if (responseBlock instanceof SBlock) {
                Timber.d("S-Block received " + responseBlock);
                // just ignore
            } else if (responseBlock instanceof RBlock) {
                Timber.d("R-Block received " + responseBlock);
                if (((RBlock) responseBlock).getError() != RBlock.RError.NO_ERROR) {
                    throw new UsbTransportException("R-Block reports error " + ((RBlock) responseBlock).getError());
                }
            } else {  // I block
                if (sentLength != apdu.length) {
                    throw new UsbTransportException("T1 frame response underflow");
                }
                return (IBlock) responseBlock;
            }
        }

        throw new UsbTransportException("Invalid tpdu sequence state");
    }

    private byte[] receiveChainedResponse(IBlock responseIBlock) throws UsbTransportException {
        byte[] responseApdu = responseIBlock.getApdu();

        while (responseIBlock.getChaining()) {
            byte receivedSeqNum = responseIBlock.getSequence();

            Block ackBlock = blockFactory.createAckRBlock(receivedSeqNum);
            CcidDataBlock response = ccidTransceiver.sendXfrBlock(ackBlock.getRawData());
            Block responseBlock = blockFactory.fromBytes(response.getData());

            if (!(responseBlock instanceof IBlock)) {
                Timber.e("Invalid response block received " + responseBlock);
                throw new UsbTransportException("Response: invalid state - invalid block received");
            }

            responseIBlock = (IBlock) responseBlock;
            responseApdu = Arrays.concatenate(responseApdu, responseBlock.getApdu());
        }

        return responseApdu;
    }
}
