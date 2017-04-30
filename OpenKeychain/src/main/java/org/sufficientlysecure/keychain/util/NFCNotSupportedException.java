package org.sufficientlysecure.keychain.util;

/**
 * Created by rohan on 20/9/15.
 */
public class NFCNotSupportedException extends Exception{
    public String error_msg;
    public NFCNotSupportedException(String msg)
    {
        super();
        error_msg = msg;
    }
}
