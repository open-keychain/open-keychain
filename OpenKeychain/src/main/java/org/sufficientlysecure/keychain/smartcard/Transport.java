package org.sufficientlysecure.keychain.smartcard;

import java.io.IOException;

public interface Transport {
    byte[] sendAndReceive(byte[] data) throws IOException;

    void release();

    boolean isConnected();

    boolean allowPersistentConnection();
}
