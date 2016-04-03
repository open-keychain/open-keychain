package org.sufficientlysecure.keychain.javacard;

import java.io.IOException;

public class TransportIoException extends IOException {
    public TransportIoException() {
    }

    public TransportIoException(final String detailMessage) {
        super(detailMessage);
    }

    public TransportIoException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TransportIoException(final Throwable cause) {
        super(cause);
    }
}
