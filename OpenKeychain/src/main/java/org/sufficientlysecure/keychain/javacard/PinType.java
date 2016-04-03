package org.sufficientlysecure.keychain.javacard;

public enum PinType {
    BASIC(0x81),
    ADMIN(0x83),;

    private final int mMode;

    PinType(final int mode) {
        this.mMode = mode;
    }

    public int getmMode() {
        return mMode;
    }
}
