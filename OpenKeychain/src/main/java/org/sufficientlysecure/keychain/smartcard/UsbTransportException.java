package org.sufficientlysecure.keychain.smartcard;

import java.io.IOException;

public class UsbTransportException extends IOException {
    public UsbTransportException() {
    }

    public UsbTransportException(final String detailMessage) {
        super(detailMessage);
    }

    public UsbTransportException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public UsbTransportException(final Throwable cause) {
        super(cause);
    }
}
