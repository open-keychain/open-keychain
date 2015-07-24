package org.sufficientlysecure.keychain.nfc;


import java.io.IOException;
import java.util.Arrays;

/**
 * Mifare Ultra Light Technology Implementation for Passphrase 128 bit key store.
 * Specification: http://www.nxp.com/documents/data_sheet/MF0ICU1.pdf
 */
public class MifareUltralight implements BaseNfcTagTechnology {
    public static final byte COMMAND_WRITE = (byte) 0xA2;
    public static final byte MEMORY_START_BLOCK = 0x04;
    public static final byte MEMORY_END_BLOCK = 0x15;

    private static final int sTimeout = 100000;
    protected android.nfc.tech.MifareUltralight mMifareUltralight;

    public MifareUltralight(android.nfc.tech.MifareUltralight mifareUltralight) {
        mMifareUltralight = mifareUltralight;

        if (mMifareUltralight == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public void connect() throws NfcDispatcher.CardException {
        //timeout is set to 100 seconds to avoid cancellation during calculation
        mMifareUltralight.setTimeout(sTimeout);
        if (!mMifareUltralight.isConnected()) {
            try {
                mMifareUltralight.connect();
            } catch (IOException e) {
                throw new NfcDispatcher.CardException(e.getMessage(),
                        NfcDispatcher.EXCEPTION_STATUS_GENERIC);
            }
        }
    }

    @Override
    public void upload(byte[] data) throws NfcDispatcher.CardException {
        int totalBytes = data.length;
        int page = MEMORY_START_BLOCK;
        byte[] dataToSend;
        try {
            while (totalBytes > 0) {
                dataToSend = Arrays.copyOfRange(data, (page - MEMORY_START_BLOCK) * 4,
                        (page - MEMORY_START_BLOCK) * 4 + 4);
                mMifareUltralight.writePage(page, dataToSend);
                totalBytes -= 4;
                page += 1;
            }
        } catch (ArrayIndexOutOfBoundsException | IllegalStateException | IOException e) {
            close();
            throw new NfcDispatcher.CardException(e.getMessage(),
                    NfcDispatcher.EXCEPTION_STATUS_GENERIC);
        }
    }

    @Override
    public byte[] read() throws NfcDispatcher.CardException {
        byte[] payload = new byte[16];

        int totalBytes = 16;
        int page = MEMORY_START_BLOCK;

        try {
            while (totalBytes > 0) {
                System.arraycopy(mMifareUltralight.readPages(page), 0, payload,
                        (page - MEMORY_START_BLOCK) * 4, 4);
                totalBytes -= 4;
                page += 1;
            }
        } catch (ArrayIndexOutOfBoundsException | IllegalStateException | IOException e) {
            close();
            throw new NfcDispatcher.CardException(e.getMessage(),
                    NfcDispatcher.EXCEPTION_STATUS_GENERIC);
        }

        return payload;
    }

    @Override
    public boolean verify(byte[] original, byte[] fromNFC) throws NfcDispatcher.CardException {
        return Arrays.equals(original, fromNFC);
    }

    @Override
    public void close() throws NfcDispatcher.CardException {
        try {
            mMifareUltralight.close();
        } catch (IOException e) {
            throw new NfcDispatcher.CardException(e.getMessage(),
                    NfcDispatcher.EXCEPTION_STATUS_GENERIC);
        }
    }

    @Override
    public boolean isConnected() {
        return mMifareUltralight.isConnected();
    }
}
