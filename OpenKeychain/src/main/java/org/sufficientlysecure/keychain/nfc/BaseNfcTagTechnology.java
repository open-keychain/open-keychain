package org.sufficientlysecure.keychain.nfc;


import java.io.IOException;

/**
 * Nfc Tag Technology interface.
 */
public interface BaseNfcTagTechnology {
    void connect() throws IOException;

    void upload(byte[] data) throws NfcDispatcher.CardException;

    byte[] read() throws NfcDispatcher.CardException;

    boolean verify(byte[] original, byte[] fromNFC) throws NfcDispatcher.CardException;

    void close() throws NfcDispatcher.CardException;

    boolean isConnected();
}
