package org.sufficientlysecure.keychain.smartcard;

import android.nfc.Tag;

import org.sufficientlysecure.keychain.ui.base.BaseSecurityTokenNfcActivity;

import java.io.IOException;

import nordpol.IsoCard;
import nordpol.android.AndroidCard;

public class NfcTransport implements Transport {
    // timeout is set to 100 seconds to avoid cancellation during calculation
    private static final int TIMEOUT = 100 * 1000;
    private final Tag mTag;
    private IsoCard mIsoCard;

    public NfcTransport(Tag tag) {
        this.mTag = tag;
    }

    /**
     * Transmit and receive data
     * @param data data to transmit
     * @return received data
     * @throws IOException
     */
    @Override
    public byte[] transceive(final byte[] data) throws IOException {
        return mIsoCard.transceive(data);
    }

    /**
     * Disconnect and release connection
     */
    @Override
    public void release() {
        // Not supported
    }

    @Override
    public boolean isConnected() {
        return mIsoCard != null && mIsoCard.isConnected();
    }

    /**
     * Check if Transport supports persistent connections e.g connections which can
     * handle multiple operations in one session
     * @return true if transport supports persistent connections
     */
    @Override
    public boolean isPersistentConnectionAllowed() {
        return false;
    }

    /**
     * Connect to NFC device.
     * <p/>
     * On general communication, see also
     * http://www.cardwerk.com/smartcards/smartcard_standard_ISO7816-4_annex-a.aspx
     * <p/>
     * References to pages are generally related to the OpenPGP Application
     * on ISO SmartCard Systems specification.
     */
    @Override
    public void connect() throws IOException {
        mIsoCard = AndroidCard.get(mTag);
        if (mIsoCard == null) {
            throw new BaseSecurityTokenNfcActivity.IsoDepNotSupportedException("Tag does not support ISO-DEP (ISO 14443-4)");
        }

        mIsoCard.setTimeout(TIMEOUT);
        mIsoCard.connect();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final NfcTransport that = (NfcTransport) o;

        if (mTag != null ? !mTag.equals(that.mTag) : that.mTag != null) return false;
        if (mIsoCard != null ? !mIsoCard.equals(that.mIsoCard) : that.mIsoCard != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mTag != null ? mTag.hashCode() : 0;
        result = 31 * result + (mIsoCard != null ? mIsoCard.hashCode() : 0);
        return result;
    }
}
