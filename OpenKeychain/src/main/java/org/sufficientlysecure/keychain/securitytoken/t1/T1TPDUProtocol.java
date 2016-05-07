package org.sufficientlysecure.keychain.securitytoken.t1;

import android.os.Debug;
import android.support.annotation.NonNull;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.securitytoken.UsbTransport;
import org.sufficientlysecure.keychain.securitytoken.UsbTransportException;
import org.sufficientlysecure.keychain.util.Log;

public class T1TPDUProtocol {
    private final static int MAX_FRAME_LEN = 254;
    private byte mCounter = 0;
    private UsbTransport mTransport;

    public T1TPDUProtocol(final UsbTransport transport) {
        mTransport = transport;
    }


    public void pps() throws UsbTransportException {
        byte[] pps = new byte[]{
                (byte) 0xFF,
                1,
                (byte) (0xFF ^ 1)
        };

        mTransport.sendXfrBlock(pps);

        byte[] ppsResponse = mTransport.receiveRaw();

        Log.d(Constants.TAG, "PPS response " + Hex.toHexString(ppsResponse));
    }

    public byte[] transceive(@NonNull byte[] apdu) throws UsbTransportException {
        int start = 0;

        Frame responseFrame = null;
        while (apdu.length - start > 0) {
            boolean hasMore = start + MAX_FRAME_LEN < apdu.length;
            int len = Math.min(MAX_FRAME_LEN, apdu.length - start);

            final Frame frame = Frame.newFrame(ChecksumType.LRC, FrameType.I_BLOCK, hasMore, mCounter ^= 1,
                    Arrays.copyOfRange(apdu, start, start + len));

            mTransport.sendXfrBlock(frame.getBytes());

            // Receive R
            byte[] response = mTransport.receiveRaw();
            responseFrame = Frame.fromData(ChecksumType.LRC, response);

            start += len;

            if (responseFrame.getBlockType() == FrameType.S_BLOCK) {
                Log.d(Constants.TAG, "S block received " + Hex.toHexString(response));
            } else if (responseFrame.getBlockType() == FrameType.R_BLOCK) {
                Log.d(Constants.TAG, "R block received " + Hex.toHexString(response));
                if (responseFrame.getRError() != Frame.RError.NO_ERROR) {
                    throw new UsbTransportException("R block reports error");
                }
            } else {
                // I block
                if (start == apdu.length) {
                    throw new UsbTransportException("T1 frame response underflow");
                }
                break;
            }
        }

        if (responseFrame == null || responseFrame.getBlockType() != FrameType.I_BLOCK)
            throw new UsbTransportException("Invalid frame state");

        byte[] responseApdu = responseFrame.getAPDU();

        while (responseFrame.hasMore()) {
            responseFrame = Frame.fromData(ChecksumType.LRC, mTransport.receive());
            responseApdu = Arrays.concatenate(responseApdu, responseFrame.getAPDU());
        }

        return responseApdu;
    }

}
