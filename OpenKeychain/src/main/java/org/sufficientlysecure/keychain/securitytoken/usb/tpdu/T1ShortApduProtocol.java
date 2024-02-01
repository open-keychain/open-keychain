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
     * Chain Parameter: Start of multi-command APDU response.
     *
     * "The response APDU begins with this command and is to continue"
     * -- DWG Smart-Card USB Integrated Circuit(s) Card Devices v1.0 § 6.1.1.3
     */
    public static final byte CHAIN_PARAM_APDU_MULTIBLOCK_START = 1;

    /**
     * Chain Parameter: Continued multi-command APDU response with more data.
     *
     * "This abData field continues the response APDU and another block is to follow"
     * -- DWG Smart-Card USB Integrated Circuit(s) Card Devices v1.0 § 6.1.1.3
     */
    public static final byte CHAIN_PARAM_APDU_MULTIBLOCK_MORE = 3;

    private CcidTransceiver ccidTransceiver;

    public void connect(@NonNull CcidTransceiver transceiver) throws UsbTransportException {
        ccidTransceiver = transceiver;
        ccidTransceiver.iccPowerOn();
    }

    @Override
    public byte[] transceive(@NonNull final byte[] apdu) throws UsbTransportException {
        CcidDataBlock initialResponse = ccidTransceiver.sendXfrBlock(apdu);

        if (initialResponse.getChainParameter() != CHAIN_PARAM_APDU_MULTIBLOCK_START) {
            return initialResponse.getData();
        }

        /*
         * Handle multi-block responses in accordance with DWG Smart-Card USB Integrated Circut(s)
         * Card Devices v1.0 § 6.1.1.  If we receive a response with a chain parameter indicating
         * more data is to come, then instruct the device to continue and append the response to our
         * output buffer.
         */

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(initialResponse.getData());

            CcidDataBlock continuedResponse;
            do {
                continuedResponse = ccidTransceiver.receiveContinuedResponse();
                output.write(continuedResponse.getData());
            } while(continuedResponse.getChainParameter() == CHAIN_PARAM_APDU_MULTIBLOCK_MORE);

            return output.toByteArray();
        } catch (UsbTransportException e) {
            // rethrow as-is
            throw e;
        } catch (IOException e) {
            throw new UsbTransportException("Failed to write block to temporary buffer", e);
        }
    }
}
