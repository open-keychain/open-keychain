package org.sufficientlysecure.keychain.javacard;

import java.io.IOException;

import nordpol.IsoCard;

public class NfcTransport implements Transport {
    // timeout is set to 100 seconds to avoid cancellation during calculation
    private static final int TIMEOUT = 100 * 1000;
    private final IsoCard mIsoCard;

    public NfcTransport(final IsoCard isoDep) throws IOException {
        this.mIsoCard = isoDep;
        mIsoCard.setTimeout(TIMEOUT);
        mIsoCard.connect();
    }

    @Override
    public byte[] sendAndReceive(final byte[] data) throws TransportIoException, IOException {
        return mIsoCard.transceive(data);
    }

    @Override
    public void release() {
    }

    @Override
    public boolean isConnected() {
        return mIsoCard.isConnected();
    }
}
