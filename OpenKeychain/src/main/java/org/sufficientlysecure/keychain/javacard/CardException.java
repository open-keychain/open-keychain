package org.sufficientlysecure.keychain.javacard;

import java.io.IOException;

public class CardException extends IOException {
    private short mResponseCode;

    public CardException(String detailMessage, short responseCode) {
        super(detailMessage);
        mResponseCode = responseCode;
    }

    public short getResponseCode() {
        return mResponseCode;
    }

}