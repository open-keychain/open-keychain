package org.sufficientlysecure.keychain.javacard;

import android.nfc.tech.IsoDep;

import java.io.IOException;

public class NfcTransport implements Transport  {
    // timeout is set to 100 seconds to avoid cancellation during calculation
    private static final int TIMEOUT = 100 * 1000;
    private final IsoDep mIsoDep;

    public NfcTransport(final IsoDep isoDep) throws IOException {
        this.mIsoDep = isoDep;
        mIsoDep.setTimeout(TIMEOUT);
        mIsoDep.connect();
    }

    @Override
    public byte[] sendAndReceive(final byte[] data) throws TransportIoException, IOException {
        return mIsoDep.transceive(data);
    }

    @Override
    public void release() {
    }

    @Override
    public boolean isConnected() {
        return mIsoDep.isConnected();
    }
}
