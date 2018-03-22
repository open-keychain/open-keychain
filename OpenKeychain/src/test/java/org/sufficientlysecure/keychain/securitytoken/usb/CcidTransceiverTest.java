package org.sufficientlysecure.keychain.securitytoken.usb;


import java.util.LinkedList;

import android.annotation.TargetApi;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.os.Build.VERSION_CODES;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.securitytoken.usb.CcidTransceiver.CcidDataBlock;
import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException.UsbCcidErrorException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("WeakerAccess")
@RunWith(KeychainTestRunner.class)
@TargetApi(VERSION_CODES.JELLY_BEAN_MR2)
public class CcidTransceiverTest {
    static final String ATR = "3bda11ff81b1fe551f0300318473800180009000e4";
    static final int MAX_PACKET_LENGTH_IN = 61;
    static final int MAX_PACKET_LENGTH_OUT = 63;

    UsbDeviceConnection usbConnection;
    UsbEndpoint usbBulkIn;
    UsbEndpoint usbBulkOut;

    LinkedList<byte[]> expectReplies;
    LinkedList<byte[]> expectRepliesVerify;

    @Before
    public void setUp() throws Exception {
        usbConnection = mock(UsbDeviceConnection.class);
        usbBulkIn = mock(UsbEndpoint.class);
        when(usbBulkIn.getMaxPacketSize()).thenReturn(MAX_PACKET_LENGTH_IN);
        usbBulkOut = mock(UsbEndpoint.class);
        when(usbBulkOut.getMaxPacketSize()).thenReturn(MAX_PACKET_LENGTH_OUT);

        expectReplies = new LinkedList<>();
        expectRepliesVerify = new LinkedList<>();
        when(usbConnection.bulkTransfer(same(usbBulkIn), any(byte[].class), any(Integer.class), any(Integer.class)))
                .thenAnswer(
                        new Answer<Integer>() {
                            @Override
                            public Integer answer(InvocationOnMock invocation) throws Throwable {
                                byte[] reply = expectReplies.poll();
                                if (reply == null) {
                                    return -1;
                                }

                                byte[] buf = invocation.getArgumentAt(1, byte[].class);
                                assertEquals(buf.length, MAX_PACKET_LENGTH_IN);

                                int len = Math.min(buf.length, reply.length);
                                System.arraycopy(reply, 0, buf, 0, len);

                                if (len < reply.length) {
                                    byte[] rest = Arrays.copyOfRange(reply, len, reply.length);
                                    expectReplies.addFirst(rest);
                                }

                                return len;
                            }
                        });

    }

    @Test
    public void testAutoVoltageSelection() throws Exception {
        CcidDescription description = CcidDescription.fromValues((byte) 0, (byte) 1, 2, 132218);
        CcidTransceiver ccidTransceiver = new CcidTransceiver(usbConnection, usbBulkIn, usbBulkOut, description);

        byte[] iccPowerOnVoltageAutoCommand = Hex.decode("62000000000000000000");
        byte[] iccPowerOnReply = Hex.decode("80150000000000000000" + ATR);
        expectReadPreamble();
        expect(iccPowerOnVoltageAutoCommand, iccPowerOnReply);


        CcidDataBlock ccidDataBlock = ccidTransceiver.iccPowerOn();


        verifyDialog();
        assertArrayEquals(Hex.decode(ATR), ccidDataBlock.getData());
    }

    @Test
    public void testManualVoltageSelection() throws Exception {
        CcidDescription description = CcidDescription.fromValues((byte) 0, (byte) 1, 2, 132210);
        CcidTransceiver ccidTransceiver = new CcidTransceiver(usbConnection, usbBulkIn, usbBulkOut, description);

        byte[] iccPowerOnVoltage5VCommand = Hex.decode("62000000000000010000");
        byte[] iccPowerOnReply = Hex.decode("80150000000000000000" + ATR);
        expectReadPreamble();
        expect(iccPowerOnVoltage5VCommand, iccPowerOnReply);


        CcidDataBlock ccidDataBlock = ccidTransceiver.iccPowerOn();


        verifyDialog();
        assertArrayEquals(Hex.decode(ATR), ccidDataBlock.getData());
    }

    @Test
    public void testManualVoltageSelection_failFirst() throws Exception {
        CcidDescription description = CcidDescription.fromValues((byte) 0, (byte) 3, 2, 132210);
        CcidTransceiver ccidTransceiver = new CcidTransceiver(usbConnection, usbBulkIn, usbBulkOut, description);

        byte[] iccPowerOnVoltage5VCommand = Hex.decode("62000000000000010000");
        byte[] iccPowerOnFailureReply = Hex.decode("80000000000000010700");
        byte[] iccPowerOffCommand = Hex.decode("6300000000000100");
        byte[] iccPowerOnVoltage3VCommand = Hex.decode("62000000000002020000");
        byte[] iccPowerOnReply = Hex.decode("80150000000002000000" + ATR);
        expectReadPreamble();
        expect(iccPowerOnVoltage5VCommand, iccPowerOnFailureReply);
        expect(iccPowerOffCommand, null);
        expect(iccPowerOnVoltage3VCommand, iccPowerOnReply);


        CcidDataBlock ccidDataBlock = ccidTransceiver.iccPowerOn();


        verifyDialog();
        assertArrayEquals(Hex.decode(ATR), ccidDataBlock.getData());
    }

    @Test
    public void testXfer() throws Exception {
        CcidTransceiver ccidTransceiver = new CcidTransceiver(usbConnection, usbBulkIn, usbBulkOut, null);

        String commandData = "010203";
        byte[] command = Hex.decode("6F030000000000000000" + commandData);
        String responseData = "0304";
        byte[] response = Hex.decode("80020000000000000000" + responseData);
        expect(command, response);

        CcidDataBlock ccidDataBlock = ccidTransceiver.sendXfrBlock(Hex.decode(commandData));

        verifyDialog();
        assertArrayEquals(Hex.decode(responseData), ccidDataBlock.getData());
    }

    @Test
    public void testXfer_IncrementalSeqNums() throws Exception {
        CcidTransceiver ccidTransceiver = new CcidTransceiver(usbConnection, usbBulkIn, usbBulkOut, null);

        String commandData = "010203";
        byte[] commandSeq1 = Hex.decode("6F030000000000000000" + commandData);
        byte[] commandSeq2 = Hex.decode("6F030000000001000000" + commandData);
        String responseData = "0304";
        byte[] responseSeq1 = Hex.decode("80020000000000000000" + responseData);
        byte[] responseSeq2 = Hex.decode("80020000000001000000" + responseData);
        expect(commandSeq1, responseSeq1);
        expect(commandSeq2, responseSeq2);

        ccidTransceiver.sendXfrBlock(Hex.decode(commandData));
        ccidTransceiver.sendXfrBlock(Hex.decode(commandData));

        verifyDialog();
    }

    @Test(expected = UsbTransportException.class)
    public void testXfer_badSeqNumberReply() throws Exception {
        CcidTransceiver ccidTransceiver = new CcidTransceiver(usbConnection, usbBulkIn, usbBulkOut, null);

        String commandData = "010203";
        byte[] command = Hex.decode("6F030000000000000000" + commandData);
        String responseData = "0304";
        byte[] response = Hex.decode("800200000000AA000000" + responseData);
        expect(command, response);


        ccidTransceiver.sendXfrBlock(Hex.decode(commandData));
    }

    @Test
    public void testXfer_errorReply() throws Exception {
        CcidTransceiver ccidTransceiver = new CcidTransceiver(usbConnection, usbBulkIn, usbBulkOut, null);

        String commandData = "010203";
        byte[] command = Hex.decode("6F030000000000000000" + commandData);
        byte[] response = Hex.decode("80000000000000012A00");
        expect(command, response);

        try {
            ccidTransceiver.sendXfrBlock(Hex.decode(commandData));
        } catch (UsbCcidErrorException e) {
            assertEquals(0x01, e.getErrorResponse().getIccStatus());
            assertEquals(0x2A, e.getErrorResponse().getError());
            return;
        }

        fail();
    }

    @Test
    public void testXfer_chainedCommand() throws Exception {
        CcidTransceiver ccidTransceiver = new CcidTransceiver(usbConnection, usbBulkIn, usbBulkOut, null);

        String commandData =
                "0000000000000123456789000000000000000000000000000000000000000000" +
                "0000000000000000000000012345678900000000000000000000000000000000" +
                "00000000000001234567890000000000";
        byte[] command = Hex.decode("6F500000000000000000" + commandData);
        String responseData = "0304";
        byte[] response = Hex.decode("80020000000000000000" + responseData);
        expectChained(command, response);

        CcidDataBlock ccidDataBlock = ccidTransceiver.sendXfrBlock(Hex.decode(commandData));

        verifyDialog();
        assertArrayEquals(Hex.decode(responseData), ccidDataBlock.getData());
    }

    @Test
    public void testXfer_chainedReply() throws Exception {
        CcidTransceiver ccidTransceiver = new CcidTransceiver(usbConnection, usbBulkIn, usbBulkOut, null);

        String commandData = "010203";
        byte[] command = Hex.decode("6F030000000000000000" + commandData);
        String responseData =
                "0000000000000000000000000000000000012345678900000000000000000000" +
                "0000000000000000000000000001234567890000000000000000000000000000" +
                "00000012345678900000000000000000";
        byte[] response = Hex.decode("80500000000000000000" + responseData);
        expect(command, response);

        CcidDataBlock ccidDataBlock = ccidTransceiver.sendXfrBlock(Hex.decode(commandData));

        verifyDialog();
        assertArrayEquals(Hex.decode(responseData), ccidDataBlock.getData());
    }

    @Test
    public void testXfer_timeoutExtensionReply() throws Exception {
        CcidTransceiver ccidTransceiver = new CcidTransceiver(usbConnection, usbBulkIn, usbBulkOut, null);

        String commandData = "010203";
        byte[] command = Hex.decode("6F030000000000000000" + commandData);
        byte[] timeExtensionResponse = Hex.decode("80000000000000800000");
        String responseData = "0304";
        byte[] response = Hex.decode("80020000000000000000" + responseData);
        expect(command, timeExtensionResponse);
        expect(null, response);

        CcidDataBlock ccidDataBlock = ccidTransceiver.sendXfrBlock(Hex.decode(commandData));

        verifyDialog();
        assertArrayEquals(Hex.decode(responseData), ccidDataBlock.getData());
    }

    @Test
    public void testReturnsCorrectAutoPpsFlag() throws Exception {
        CcidDescription description = CcidDescription.fromValues((byte) 0, (byte) 7, 3, 65722);
        CcidTransceiver ccidTransceiver = new CcidTransceiver(usbConnection, usbBulkIn, usbBulkOut, description);

        assertTrue(ccidTransceiver.hasAutomaticPps());
    }

    private void verifyDialog() {
        assertTrue(expectReplies.isEmpty());
        assertFalse(expectRepliesVerify.isEmpty());

        for (byte[] command : expectRepliesVerify) {
            if (command == null) {
                continue;
            }
            verify(usbConnection).bulkTransfer(same(usbBulkIn), aryEq(command), any(Integer.class), any(Integer.class));
        }

        expectRepliesVerify.clear();
    }

    private void expectReadPreamble() {
        expectReplies.add(null);
        expectRepliesVerify.add(null);
    }

    private void expectChained(byte[] command, byte[] reply) {
        for (int i = 0; i < command.length; i+= MAX_PACKET_LENGTH_OUT) {
            int len = Math.min(MAX_PACKET_LENGTH_OUT, command.length - i);
            when(usbConnection.bulkTransfer(same(usbBulkOut), aryEq(command), eq(i), eq(len),
                    any(Integer.class))).thenReturn(len);
        }
        if (reply != null) {
            expectReplies.add(reply);
            expectRepliesVerify.add(null);
        }
    }

    private void expect(byte[] command, byte[] reply) {
        if (command != null) {
            when(usbConnection.bulkTransfer(same(usbBulkOut), aryEq(command), eq(0), eq(command.length),
                    any(Integer.class))).thenReturn(command.length);
        }
        if (reply != null) {
            expectReplies.add(reply);
            expectRepliesVerify.add(null);
        }
    }
}
