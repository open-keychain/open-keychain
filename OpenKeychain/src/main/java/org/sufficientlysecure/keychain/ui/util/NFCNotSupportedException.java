package org.sufficientlysecure.keychain.ui.util;

/**
 * Created by rohan on 21/9/15.
 */
public class NFCNotSupportedException extends Exception {
    public String error_msg;
        public NFCNotSupportedException(String msg)
        {
            super();
            error_msg = msg;
        }
}
