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

    @Override
    public byte[] sendAndReceive(final byte[] data) throws TransportIoException, IOException {
        return mIsoCard.transceive(data);
    }

    @Override
    public void release() {
    }

    @Override
    public boolean isConnected() {
        return mIsoCard != null && mIsoCard.isConnected();
    }

    @Override
    public boolean allowPersistentConnection() {
        return false;
    }

    /**
     * Handle NFC communication and return a result.
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
