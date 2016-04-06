package org.sufficientlysecure.keychain.smartcard;

public class PinException extends CardException {
    public PinException(final String detailMessage, final short responseCode) {
        super(detailMessage, responseCode);
    }
}
