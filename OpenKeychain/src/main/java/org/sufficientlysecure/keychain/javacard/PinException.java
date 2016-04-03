package org.sufficientlysecure.keychain.javacard;

public class PinException extends CardException {
    public PinException(final String detailMessage, final short responseCode) {
        super(detailMessage, responseCode);
    }
}
