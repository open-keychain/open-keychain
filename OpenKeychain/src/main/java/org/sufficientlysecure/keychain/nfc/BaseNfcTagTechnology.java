package org.sufficientlysecure.keychain.nfc;


/**
 * Nfc Tag Technology interface.
 */
public interface BaseNfcTagTechnology {
    void connect() throws NfcDispatcher.CardException;

    void upload(byte[] data) throws NfcDispatcher.CardException;

    byte[] read() throws NfcDispatcher.CardException;

    boolean verify(byte[] original, byte[] fromNFC) throws NfcDispatcher.CardException;

    void close() throws NfcDispatcher.CardException;

    boolean isConnected();
}
