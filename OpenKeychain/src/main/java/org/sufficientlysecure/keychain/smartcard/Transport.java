package org.sufficientlysecure.keychain.smartcard;

import java.io.IOException;

/**
 * Abstraction for transmitting APDU commands
 */
public interface Transport {
    /**
     * Transmit and receive data
     * @param data data to transmit
     * @return received data
     * @throws IOException
     */
    byte[] transceive(byte[] data) throws IOException;

    /**
     * Disconnect and release connection
     */
    void release();

    /**
     * Check if device is was connected to and still is connected
     * @return connection status
     */
    boolean isConnected();

    /**
     * Check if Transport supports persistent connections e.g connections which can
     * handle multiple operations in one session
     * @return true if transport supports persistent connections
     */
    boolean isPersistentConnectionAllowed();


    /**
     * Connect to device
     * @throws IOException
     */
    void connect() throws IOException;
}
