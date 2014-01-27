package org.sufficientlysecure.keychain.service.exception;

public class WrongPassphraseException extends Exception {

    private static final long serialVersionUID = -5309689232853485740L;

    public WrongPassphraseException(String message) {
        super(message);
    }
}