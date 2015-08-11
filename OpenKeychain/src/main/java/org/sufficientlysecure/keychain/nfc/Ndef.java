package org.sufficientlysecure.keychain.nfc;

import java.util.Arrays;

/**
 * Nfc NDef technology
 */
public class Ndef implements BaseNfcTagTechnology {
    private static final int sTimeout = 100000;
    private android.nfc.tech.Ndef mNdef;

    public Ndef(android.nfc.tech.Ndef ndef) {
        mNdef = ndef;
    }

    @Override
    public void connect() throws NfcDispatcher.CardException {

    }

    @Override
    public void upload(byte[] data) throws NfcDispatcher.CardException {

    }

    @Override
    public byte[] read() throws NfcDispatcher.CardException {
        return new byte[0];
    }

    @Override
    public boolean verify(byte[] original, byte[] fromNFC) throws NfcDispatcher.CardException {
        return Arrays.equals(original, fromNFC);
    }

    @Override
    public void close() throws NfcDispatcher.CardException {

    }

    @Override
    public boolean isConnected() {
        return false;
    }
}
