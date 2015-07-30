package org.sufficientlysecure.keychain.nfc;


/**
 * Nfc Tag Technology interface.
 *
 * All NFC Card Exceptions should be of type CardException.
 */
public interface BaseNfcTagTechnology {

    /**
     * Connects the device to the NFC tag
     *
     * @throws NfcDispatcher.CardException
     */
    void connect() throws NfcDispatcher.CardException;

    /**
     * Writes the data into the NFC Tag memory
     *
     * @param data
     * @throws NfcDispatcher.CardException
     */
    void upload(byte[] data) throws NfcDispatcher.CardException;

    /**
     * Reads the data from the NFC tag
     *
     * @return
     * @throws NfcDispatcher.CardException
     */
    byte[] read() throws NfcDispatcher.CardException;

    /**
     * Compares the computed passphrase with the one that was read from the NFC tag
     *
     * @param original
     * @param fromNFC
     * @return
     * @throws NfcDispatcher.CardException
     */
    boolean verify(byte[] original, byte[] fromNFC) throws NfcDispatcher.CardException;

    /**
     * Disconnects the NFC tag from the device
     *
     * @throws NfcDispatcher.CardException
     */
    void close() throws NfcDispatcher.CardException;

    /**
     * Checks if the NFC tag is connected to the device
     *
     * @return
     */
    boolean isConnected();
}
