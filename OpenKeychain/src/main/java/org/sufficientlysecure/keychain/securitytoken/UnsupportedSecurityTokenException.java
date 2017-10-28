package org.sufficientlysecure.keychain.securitytoken;

import java.io.IOException;

public class UnsupportedSecurityTokenException extends IOException {

    UnsupportedSecurityTokenException() {
        super();
    }

    UnsupportedSecurityTokenException(String detailMessage) {
        super(detailMessage);
    }

}