package org.sufficientlysecure.keychain.nfc;

import android.content.Context;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Build;

import org.sufficientlysecure.keychain.R;

import java.io.IOException;

/**
 * Implements NdefFormatable technology.
 * http://developer.android.com/reference/android/nfc/tech/NdefFormatable.html
 */
public class NdefFormatable implements BaseNfcTagTechnology {
    public static final String DOMAIN = "org.openkeychain.nfc";
    public static final String TYPE = "externaltype";
    public static final String DOMAIN_TYPE = "org.openkeychain.nfc:externaltype";
    private android.nfc.tech.NdefFormatable mNdeFormatable;
    private Context mContext;

    public NdefFormatable(android.nfc.tech.NdefFormatable ndefFormatable, Context context) {
        mContext = context;
        mNdeFormatable = ndefFormatable;

        if (mNdeFormatable == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public void connect() throws NfcDispatcher.CardException {
        if (!mNdeFormatable.isConnected()) {
            try {
                mNdeFormatable.connect();
            } catch (IOException e) {
                throw new NfcDispatcher.CardException(mContext.
                        getString(R.string.error_nfc_tag_unable_to_connect),
                        NfcDispatcher.EXCEPTION_STATUS_GENERIC);
            }
        }
    }

    @Override
    public void upload(byte[] data) throws NfcDispatcher.CardException {
        try {
            NdefRecord extRecord;
            NdefMessage ndefMessage;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                extRecord = NdefRecord.createExternal(DOMAIN, TYPE, data);
                ndefMessage = new NdefMessage(extRecord);
            } else {
                extRecord = new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE, DOMAIN_TYPE.getBytes(),
                        new byte[0], data);
                ndefMessage = new NdefMessage(extRecord.getPayload());
            }
            mNdeFormatable.format(ndefMessage);
        } catch (IOException | IllegalArgumentException | FormatException e) {
            throw new NfcDispatcher.CardException(mContext.
                    getString(R.string.error_nfc_tag_failed_to_format),
                    NfcDispatcher.EXCEPTION_STATUS_GENERIC);
        }
    }

    /**
     * NdefFormatable does not have read I/O operations.
     *
     * @throws NfcDispatcher.CardException
     */
    @Override
    public byte[] read() throws NfcDispatcher.CardException {
        return new byte[0];
    }

    /**
     * NDefFormatable does not support read operations, assume that the write operation went ok.
     *
     * @param original
     * @param fromNFC
     * @return
     * @throws NfcDispatcher.CardException
     */
    @Override
    public boolean verify(byte[] original, byte[] fromNFC) throws NfcDispatcher.CardException {
        return true;
    }

    @Override
    public void close() throws NfcDispatcher.CardException {
        if (mNdeFormatable.isConnected()) {
            try {
                mNdeFormatable.close();
            } catch (IOException e) {
                throw new NfcDispatcher.CardException(mContext.
                        getString(R.string.error_nfc_tag_disconnect),
                        NfcDispatcher.EXCEPTION_STATUS_GENERIC);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return mNdeFormatable.isConnected();
    }
}
