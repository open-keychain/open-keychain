package org.sufficientlysecure.keychain.javacard;

import java.io.IOException;

public interface Transport {
    byte[] sendAndReceive(byte[] data) throws IOException;

    void release();

    boolean isConnected();
}
