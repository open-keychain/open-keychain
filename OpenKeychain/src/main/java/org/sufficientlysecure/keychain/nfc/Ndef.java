package org.sufficientlysecure.keychain.nfc;

import android.content.Context;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Build;

import org.sufficientlysecure.keychain.R;

import java.io.IOException;
import java.util.Arrays;

/**
 * Nfc NDef technology
 */
public class Ndef implements BaseNfcTagTechnology {
    public static final String DOMAIN = "org.openkeychain.nfc";
    public static final String TYPE = "externaltype";
    public static final String DOMAIN_TYPE = "org.openkeychain.nfc:externaltype";
    protected android.nfc.tech.Ndef mNdef;
    protected Context mContext;

    public Ndef(android.nfc.tech.Ndef ndef, Context context) {
        mContext = context;
        mNdef = ndef;

        if (mNdef == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public void connect() throws NfcDispatcher.CardException {
        if (!mNdef.isConnected()) {
            try {
                mNdef.connect();
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
            if (!mNdef.isWritable()) {
                throw new NfcDispatcher.CardException(mContext.
                        getString(R.string.error_nfc_tag_not_writable),
                        NfcDispatcher.EXCEPTION_STATUS_GENERIC);
            }

            NdefRecord extRecord;
            NdefMessage ndefMessage = mNdef.getNdefMessage();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                extRecord = NdefRecord.createExternal(DOMAIN, TYPE, data);
                if (ndefMessage == null) {
                    ndefMessage = new NdefMessage(extRecord);
                } else {
                    int index = getRecordIndex(ndefMessage);
                    if (index != -1) {
                        NdefRecord[] ndefRecords = ndefMessage.getRecords();
                        ndefRecords[index] = extRecord;
                        ndefMessage = new NdefMessage(ndefRecords);
                    } else {
                        ndefMessage = new NdefMessage(extRecord, ndefMessage.getRecords());
                    }
                }
            } else {
                extRecord = new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE, DOMAIN_TYPE.getBytes(),
                        new byte[0], data);
                if (ndefMessage == null) {
                    ndefMessage = new NdefMessage(extRecord.getPayload());
                } else {
                    //join the records together
                    NdefRecord[] ndefRecords = ndefMessage.getRecords();
                    int index = getRecordIndex(ndefMessage);
                    if (index != -1) {
                        ndefRecords[index] = extRecord;
                        ndefMessage = new NdefMessage(ndefRecords);
                    } else {
                        NdefRecord[] ndefRecordsDst = new NdefRecord[ndefRecords.length + 1];
                        System.arraycopy(ndefRecords, 0, ndefRecordsDst, 0, ndefRecords.length);
                        ndefRecordsDst[ndefRecords.length] = extRecord;

                        ndefMessage = new NdefMessage(ndefRecordsDst);
                    }
                }
            }
            mNdef.writeNdefMessage(ndefMessage);

        } catch (IOException | IllegalArgumentException | FormatException e) {
            throw new NfcDispatcher.CardException(mContext.
                    getString(R.string.error_nfc_tag_data_write),
                    NfcDispatcher.EXCEPTION_STATUS_GENERIC);
        }
    }

    /**
     * Returns previous ndef record index
     *
     * @param ndefMessage
     * @return
     */
    int getRecordIndex(NdefMessage ndefMessage) {
        int recordIndex = 0;
        byte[] domainType = DOMAIN_TYPE.getBytes();
        for (NdefRecord ndefRecord : ndefMessage.getRecords()) {
            if (ndefRecord.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE &&
                    Arrays.equals(ndefRecord.getType(), domainType)) {
                return recordIndex;
            }
            recordIndex++;
        }
        return -1;
    }

    @Override
    public byte[] read() throws NfcDispatcher.CardException {
        try {
            NdefMessage ndefMessage = mNdef.getNdefMessage();
            if (ndefMessage != null) {
                byte[] domainType = DOMAIN_TYPE.getBytes();
                for (NdefRecord ndefRecord : ndefMessage.getRecords()) {
                    if (ndefRecord.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE &&
                            Arrays.equals(ndefRecord.getType(), domainType)) {
                        return ndefRecord.getPayload();
                    }
                }
            }
        } catch (IOException | FormatException e) {
            throw new NfcDispatcher.CardException(mContext.
                    getString(R.string.error_nfc_tag_data_read),
                    NfcDispatcher.EXCEPTION_STATUS_GENERIC);
        }
        //no pay load
        return null;
    }

    @Override
    public boolean verify(byte[] original, byte[] fromNFC) throws NfcDispatcher.CardException {
        return Arrays.equals(original, fromNFC);
    }

    @Override
    public void close() throws NfcDispatcher.CardException {
        if (mNdef.isConnected()) {
            try {
                mNdef.close();
            } catch (IOException e) {
                throw new NfcDispatcher.CardException(mContext.
                        getString(R.string.error_nfc_tag_disconnect),
                        NfcDispatcher.EXCEPTION_STATUS_GENERIC);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return mNdef.isConnected();
    }
}
