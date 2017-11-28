package org.sufficientlysecure.keychain.securitytoken.usb;


public class UnsupportedUsbTokenException extends UsbTransportException {
    UnsupportedUsbTokenException() {
        super("This USB token is not supported!");
    }
}
