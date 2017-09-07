package org.sufficientlysecure.keychain.securitytoken;


import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;


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

    public static SecurityTokenInfo newInstanceDebugKeyserver() {
        if (!BuildConfig.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(
                KeyFormattingUtils.convertFingerprintHexFingerprint("1efdb4845ca242ca6977fddb1f788094fd3b430a"),
                new byte[20], new byte[20], Hex.decode("010203040506"), "yubinu2@mugenguild.com", null, 3, 3);
    }

    public static SecurityTokenInfo newInstanceDebugUri() {
        if (!BuildConfig.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(
                KeyFormattingUtils.convertFingerprintHexFingerprint("4700BA1AC417ABEF3CC7765AD686905837779C3E"),
                new byte[20], new byte[20], Hex.decode("010203040506"),
                "yubinu2@mugenguild.com", "http://valodim.stratum0.net/mryubinu2.asc", 3, 3);
    }

    public static SecurityTokenInfo newInstanceDebugLocked() {
        if (!BuildConfig.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(
                KeyFormattingUtils.convertFingerprintHexFingerprint("4700BA1AC417ABEF3CC7765AD686905837779C3E"),
                new byte[20], new byte[20], Hex.decode("010203040506"),
                "yubinu2@mugenguild.com", "http://valodim.stratum0.net/mryubinu2.asc", 0, 3);
    }

    public static SecurityTokenInfo newInstanceDebugLockedHard() {
        if (!BuildConfig.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(
                KeyFormattingUtils.convertFingerprintHexFingerprint("4700BA1AC417ABEF3CC7765AD686905837779C3E"),
                new byte[20], new byte[20], Hex.decode("010203040506"),
                "yubinu2@mugenguild.com", "http://valodim.stratum0.net/mryubinu2.asc", 0, 0);
    }

}
