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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import androidx.annotation.NonNull;

import org.sufficientlysecure.keychain.securitytoken.usb.CcidTransceiver;
import org.sufficientlysecure.keychain.securitytoken.usb.CcidTransceiver.CcidDataBlock;
import org.sufficientlysecure.keychain.securitytoken.usb.CcidTransportProtocol;
import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;

public class T1ShortApduProtocol implements CcidTransportProtocol {
    /**
     * The response APDU begins with this command and is to continue
     */
    public static final byte CHAIN_PARAM_APDU_MULTIBLOCK_START = 1;

    /**
     * This abData field continues the response APDU and another block is to follow
     */
    public static final byte CHAIN_PARAM_APDU_MULTIBLOCK_MORE = 3;

    private CcidTransceiver ccidTransceiver;

    public void connect(@NonNull CcidTransceiver transceiver) throws UsbTransportException {
        ccidTransceiver = transceiver;
        ccidTransceiver.iccPowerOn();
    }

    @Override
    public byte[] transceive(@NonNull final byte[] apdu) throws UsbTransportException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CcidDataBlock response = ccidTransceiver.sendXfrBlock(apdu);

        try {
            output.write(response.getData());
        } catch (IOException e) {
            throw new UsbTransportException("Failed to write block to temporary buffer", e);
        }

        while (
                (response.getChainParameter() == CHAIN_PARAM_APDU_MULTIBLOCK_START)
                || (response.getChainParameter() == CHAIN_PARAM_APDU_MULTIBLOCK_MORE)
        ) {
            response = ccidTransceiver.sendXfrBlock(
                new byte[0], (byte)0,
                CcidTransceiver.LEVEL_PARAM_CONTINUE_RESPONSE
            );
            try {
                output.write(response.getData());
            } catch (IOException e) {
                throw new UsbTransportException("Failed to write block to temporary buffer", e);
            }
        }
        return output.toByteArray();
    }
}
