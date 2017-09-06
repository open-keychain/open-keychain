package org.sufficientlysecure.keychain.securitytoken;


import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;


@AutoValue
public abstract class SecurityTokenInfo implements Parcelable {
    @Nullable
    public abstract byte[] getFingerprintSign();
    @Nullable
    public abstract byte[] getFingerprintDecrypt();
    @Nullable
    public abstract byte[] getFingerprintAuth();
    @Nullable
    public abstract byte[] getAid();
    @Nullable
    public abstract String getUserId();
    @Nullable
    public abstract String getUrl();
    public abstract int getVerifyRetries();
    public abstract int getVerifyAdminRetries();

    public byte[][] getAllFingerprints() {
        byte[][] fingerprints = new byte[3][];
        fingerprints[0] = getFingerprintSign();
        fingerprints[1] = getFingerprintDecrypt();
        fingerprints[2] = getFingerprintAuth();
        return fingerprints;
    }

    public boolean isEmpty() {
        return getFingerprintSign() == null && getFingerprintDecrypt() == null && getFingerprintAuth() == null;
    }

    public static SecurityTokenInfo create(byte[] fpSign, byte[] fpDecrypt, byte[] fpAuth,
            byte[] aid, String userId, String url, int verifyRetries, int verifyAdminRetries) {
        return new AutoValue_SecurityTokenInfo(fpSign, fpDecrypt, fpAuth, aid,
                userId, url, verifyRetries, verifyAdminRetries);
    }

    public static SecurityTokenInfo createBlank(byte[] aid) {
        return new AutoValue_SecurityTokenInfo(null, null, null, aid, null, null, 0, 0);
    }
}
