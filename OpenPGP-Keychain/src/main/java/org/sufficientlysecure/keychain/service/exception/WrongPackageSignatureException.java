package org.sufficientlysecure.keychain.service.exception;

public class WrongPackageSignatureException extends Exception {

    private static final long serialVersionUID = -8294642703122196028L;

    public WrongPackageSignatureException(String message) {
        super(message);
    }
}