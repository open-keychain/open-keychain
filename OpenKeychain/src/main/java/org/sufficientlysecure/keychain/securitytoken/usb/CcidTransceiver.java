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

package org.sufficientlysecure.keychain.securitytoken.usb;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.auto.value.AutoValue;
import org.bouncycastle.util.Arrays;
import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException.UsbCcidErrorException;
import timber.log.Timber;

import static org.bouncycastle.util.encoders.Hex.toHexString;


public class CcidTransceiver {
    private static final int CCID_HEADER_LENGTH = 10;

    private static final int MESSAGE_TYPE_RDR_TO_PC_DATA_BLOCK = 0x80;
    private static final int MESSAGE_TYPE_PC_TO_RDR_ICC_POWER_ON = 0x62;
    private static final int MESSAGE_TYPE_PC_TO_RDR_ICC_POWER_OFF = 0x63;
    private static final int MESSAGE_TYPE_PC_TO_RDR_XFR_BLOCK = 0x6f;

    private static final int COMMAND_STATUS_SUCCESS = 0;
    private static final int COMMAND_STATUS_TIME_EXTENSION_RQUESTED = 2;

    private static final int SLOT_NUMBER = 0x00;

    private static final int ICC_STATUS_SUCCESS = 0;

    private static final int DEVICE_COMMUNICATE_TIMEOUT_MILLIS = 5000;
    private static final int DEVICE_SKIP_TIMEOUT_MILLIS = 100;


    private final UsbDeviceConnection usbConnection;
    private final UsbEndpoint usbBulkIn;
    private final UsbEndpoint usbBulkOut;
    private final CcidDescription usbCcidDescription;
    private final byte[] inputBuffer;

    private byte currentSequenceNumber;


    CcidTransceiver(UsbDeviceConnection connection, UsbEndpoint bulkIn, UsbEndpoint bulkOut,
            CcidDescription ccidDescription) {
        usbConnection = connection;
        usbBulkIn = bulkIn;
        usbBulkOut = bulkOut;
        usbCcidDescription = ccidDescription;

        inputBuffer = new byte[usbBulkIn.getMaxPacketSize()];
    }

    /**
     * Power of ICC
     * Spec: 6.1.1 PC_to_RDR_IccPowerOn
     */
    @NonNull
    @WorkerThread
    public synchronized CcidDataBlock iccPowerOn() throws UsbTransportException {
        long startTime = SystemClock.elapsedRealtime();

        skipAvailableInput();

        CcidDataBlock response = null;
        for (CcidDescription.Voltage v : usbCcidDescription.getVoltages()) {
            Timber.v("CCID: attempting to power on with voltage " + v.toString());
            try {
                response = iccPowerOnVoltage(v.powerOnValue);
            } catch (UsbCcidErrorException e) {
                if (e.getErrorResponse().getError() == 7) { // Power select error
                    Timber.v("CCID: failed to power on with voltage " + v.toString());
                    iccPowerOff();
                    Timber.v("CCID: powered off");
                    continue;
                }

                throw e;
            }

            break;
        }
        if (response == null) {
            throw new UsbTransportException("Couldn't power up ICC2");
        }

        long elapsedTime = SystemClock.elapsedRealtime() - startTime;

        Timber.d("Usb transport connected, took " + elapsedTime + "ms, ATR=" +
                toHexString(response.getData()));

        return response;
    }

    private CcidDataBlock iccPowerOnVoltage(byte voltage) throws UsbTransportException {
        byte sequenceNumber = currentSequenceNumber++;
        final byte[] iccPowerCommand = {
                MESSAGE_TYPE_PC_TO_RDR_ICC_POWER_ON,
                0x00, 0x00, 0x00, 0x00,
                SLOT_NUMBER,
                sequenceNumber,
                voltage,
                0x00, 0x00 // reserved for future use
        };

        sendRaw(iccPowerCommand, 0, iccPowerCommand.length);

        return receiveDataBlock(sequenceNumber);
    }

    private void iccPowerOff() throws UsbTransportException {
        byte sequenceNumber = currentSequenceNumber++;
        final byte[] iccPowerCommand = {
                MESSAGE_TYPE_PC_TO_RDR_ICC_POWER_OFF,
                0x00, 0x00, 0x00, 0x00,
                0x00,
                sequenceNumber,
                0x00
        };

        sendRaw(iccPowerCommand, 0, iccPowerCommand.length);
    }

    /**
     * Transmits XfrBlock
     * 6.1.4 PC_to_RDR_XfrBlock
     *
     * @param payload payload to transmit
     */
    @WorkerThread
    public synchronized CcidDataBlock sendXfrBlock(byte[] payload) throws UsbTransportException {
        long startTime = SystemClock.elapsedRealtime();

        int l = payload.length;
        byte sequenceNumber = currentSequenceNumber++;
        byte[] headerData = {
                MESSAGE_TYPE_PC_TO_RDR_XFR_BLOCK,
                (byte) l, (byte) (l >> 8), (byte) (l >> 16), (byte) (l >> 24),
                SLOT_NUMBER,
                sequenceNumber,
                0x00, // block waiting time
                0x00, 0x00 // level parameters
        };
        byte[] data = Arrays.concatenate(headerData, payload);

        int sentBytes = 0;
        while (sentBytes < data.length) {
            int bytesToSend = Math.min(usbBulkOut.getMaxPacketSize(), data.length - sentBytes);
            sendRaw(data, sentBytes, bytesToSend);
            sentBytes += bytesToSend;
        }

        CcidDataBlock ccidDataBlock = receiveDataBlock(sequenceNumber);

        long elapsedTime = SystemClock.elapsedRealtime() - startTime;
        Timber.d("USB XferBlock call took " + elapsedTime + "ms");

        return ccidDataBlock;
    }

    private void skipAvailableInput() {
        int ignoredBytes;
        do {
            ignoredBytes = usbConnection.bulkTransfer(
                    usbBulkIn, inputBuffer, inputBuffer.length, DEVICE_SKIP_TIMEOUT_MILLIS);
            if (ignoredBytes > 0) {
                Timber.e("Skipped " + ignoredBytes + " bytes: " + toHexString(inputBuffer, 0, ignoredBytes));
            }
        } while (ignoredBytes > 0);
    }

    private CcidDataBlock receiveDataBlock(byte expectedSequenceNumber) throws UsbTransportException {
        CcidDataBlock response;
        do {
            response = receiveDataBlockImmediate(expectedSequenceNumber);
        } while (response.isStatusTimeoutExtensionRequest());

        if (!response.isStatusSuccess()) {
            throw new UsbCcidErrorException("USB-CCID error!", response);
        }

        return response;
    }

    private CcidDataBlock receiveDataBlockImmediate(byte expectedSequenceNumber) throws UsbTransportException {
        int readBytes = usbConnection.bulkTransfer(usbBulkIn, inputBuffer, inputBuffer.length, DEVICE_COMMUNICATE_TIMEOUT_MILLIS);
        if (readBytes < CCID_HEADER_LENGTH) {
            throw new UsbTransportException("USB-CCID error - failed to receive CCID header");
        }
        if (inputBuffer[0] != (byte) MESSAGE_TYPE_RDR_TO_PC_DATA_BLOCK) {
            if (expectedSequenceNumber != inputBuffer[6]) {
                throw new UsbTransportException("USB-CCID error - bad CCID header, type " + inputBuffer[0] + " (expected " +
                        MESSAGE_TYPE_RDR_TO_PC_DATA_BLOCK + "), sequence number " + inputBuffer[6] + " (expected " +
                        expectedSequenceNumber + ")");
            }

            throw new UsbTransportException("USB-CCID error - bad CCID header type " + inputBuffer[0]);
        }
        CcidDataBlock result = CcidDataBlock.parseHeaderFromBytes(inputBuffer);

        if (expectedSequenceNumber != result.getSeq()) {
            throw new UsbTransportException("USB-CCID error - expected sequence number " +
                    expectedSequenceNumber + ", got " + result);
        }

        byte[] dataBuffer = new byte[result.getDataLength()];
        int bufferedBytes = readBytes - CCID_HEADER_LENGTH;
        System.arraycopy(inputBuffer, CCID_HEADER_LENGTH, dataBuffer, 0, bufferedBytes);

        while (bufferedBytes < dataBuffer.length) {
            readBytes = usbConnection.bulkTransfer(usbBulkIn, inputBuffer, inputBuffer.length, DEVICE_COMMUNICATE_TIMEOUT_MILLIS);
            if (readBytes < 0) {
                throw new UsbTransportException("USB error - failed reading response data! Header: " + result);
            }
            System.arraycopy(inputBuffer, 0, dataBuffer, bufferedBytes, readBytes);
            bufferedBytes += readBytes;
        }

        result = result.withData(dataBuffer);

        return result;
    }

    private void sendRaw(byte[] data, int offset, int length) throws UsbTransportException {
        int tr1;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            tr1 = usbConnection.bulkTransfer(usbBulkOut, data, offset, length, DEVICE_COMMUNICATE_TIMEOUT_MILLIS);
        } else {
            byte[] dataToSend = Arrays.copyOfRange(data, offset, offset+length);
            tr1 = usbConnection.bulkTransfer(usbBulkOut, dataToSend, dataToSend.length, DEVICE_COMMUNICATE_TIMEOUT_MILLIS);
        }

        if (tr1 != length) {
            throw new UsbTransportException("USB error - failed to transmit data (" + tr1 + "/" + length + ")");
        }
    }

    public boolean hasAutomaticPps() {
        return usbCcidDescription.hasAutomaticPps();
    }

    /** Corresponds to 6.2.1 RDR_to_PC_DataBlock. */
    @AutoValue
    public abstract static class CcidDataBlock {
        public abstract int getDataLength();
        public abstract byte getSlot();
        public abstract byte getSeq();
        public abstract byte getStatus();
        public abstract byte getError();
        public abstract byte getChainParameter();
        @Nullable
        @SuppressWarnings("mutable")
        public abstract byte[] getData();

        static CcidDataBlock parseHeaderFromBytes(byte[] headerBytes) {
            ByteBuffer buf = ByteBuffer.wrap(headerBytes);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            byte type = buf.get();
            if (type != (byte) MESSAGE_TYPE_RDR_TO_PC_DATA_BLOCK) {
                throw new IllegalArgumentException("Header has incorrect type value!");
            }
            int dwLength = buf.getInt();
            byte bSlot = buf.get();
            byte bSeq = buf.get();
            byte bStatus = buf.get();
            byte bError = buf.get();
            byte bChainParameter = buf.get();

            return new AutoValue_CcidTransceiver_CcidDataBlock(
                    dwLength, bSlot, bSeq, bStatus, bError, bChainParameter, null);
        }

        CcidDataBlock withData(byte[] data) {
            if (getData() != null) {
                throw new IllegalStateException("Cannot add data to this class twice!");
            }

            return new AutoValue_CcidTransceiver_CcidDataBlock(
                    getDataLength(), getSlot(), getSeq(), getStatus(), getError(), getChainParameter(), data);
        }

        byte getIccStatus() {
            return (byte) (getStatus() & 0x03);
        }

        byte getCommandStatus() {
            return (byte) ((getStatus() >> 6) & 0x03);
        }

        boolean isStatusTimeoutExtensionRequest() {
            return getCommandStatus() == COMMAND_STATUS_TIME_EXTENSION_RQUESTED;
        }

        boolean isStatusSuccess() {
            return getIccStatus() == ICC_STATUS_SUCCESS && getCommandStatus() == COMMAND_STATUS_SUCCESS;
        }
    }
}
