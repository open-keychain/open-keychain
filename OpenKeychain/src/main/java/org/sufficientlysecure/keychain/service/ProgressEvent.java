package org.sufficientlysecure.keychain.service;


public class ProgressEvent {

    public final String mMessage;
    public final int mProgress;
    public final int mMax;

    public ProgressEvent(String message, int progress, int max) {
        mMessage = message;
        mProgress = progress;
        mMax = max;
    }

}
