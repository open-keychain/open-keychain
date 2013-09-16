package org.sufficientlysecure.keychain.service.exception;

public class UserInteractionRequiredException extends Exception {

    private static final long serialVersionUID = -60128148603511936L;

    public UserInteractionRequiredException(String message) {
        super(message);
    }
}