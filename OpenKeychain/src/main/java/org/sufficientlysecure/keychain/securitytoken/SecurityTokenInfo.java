package org.sufficientlysecure.keychain.securitytoken;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;


@AutoValue
public abstract class SecurityTokenInfo implements Parcelable {
    private static final byte[] EMPTY_ARRAY = new byte[20];

    public abstract List<byte[]> getFingerprints();
    @Nullable
    public abstract byte[] getAid();
    @Nullable
    public abstract String getUserId();
    @Nullable
    public abstract String getUrl();
    public abstract int getVerifyRetries();
    public abstract int getVerifyAdminRetries();

    public boolean isEmpty() {
        return getFingerprints().isEmpty();
    }

    public static SecurityTokenInfo create(byte[][] fingerprints, byte[] aid, String userId, String url,
            int verifyRetries, int verifyAdminRetries) {
        ArrayList<byte[]> fingerprintList = new ArrayList<>(fingerprints.length);
        for (byte[] fingerprint : fingerprints) {
            if (!Arrays.equals(EMPTY_ARRAY, fingerprint)) {
                fingerprintList.add(fingerprint);
            }
        }
        return new AutoValue_SecurityTokenInfo(fingerprintList, aid, userId, url, verifyRetries, verifyAdminRetries);
    }

    public static SecurityTokenInfo newInstanceDebugKeyserver() {
        if (!BuildConfig.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(
                new byte[][] { KeyFormattingUtils.convertFingerprintHexFingerprint("1efdb4845ca242ca6977fddb1f788094fd3b430a") },
                Hex.decode("010203040506"), "yubinu2@mugenguild.com", null, 3, 3);
    }

    public static SecurityTokenInfo newInstanceDebugUri() {
        if (!BuildConfig.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(
                new byte[][] { KeyFormattingUtils.convertFingerprintHexFingerprint("4700BA1AC417ABEF3CC7765AD686905837779C3E") },
                Hex.decode("010203040506"), "yubinu2@mugenguild.com", "http://valodim.stratum0.net/mryubinu2.asc", 3, 3);
    }

    public static SecurityTokenInfo newInstanceDebugLocked() {
        if (!BuildConfig.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(
                new byte[][] { KeyFormattingUtils.convertFingerprintHexFingerprint("4700BA1AC417ABEF3CC7765AD686905837779C3E") },
                Hex.decode("010203040506"), "yubinu2@mugenguild.com", "http://valodim.stratum0.net/mryubinu2.asc", 0, 3);
    }

    public static SecurityTokenInfo newInstanceDebugLockedHard() {
        if (!BuildConfig.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(
                new byte[][] { KeyFormattingUtils.convertFingerprintHexFingerprint("4700BA1AC417ABEF3CC7765AD686905837779C3E") },
                Hex.decode("010203040506"), "yubinu2@mugenguild.com", "http://valodim.stratum0.net/mryubinu2.asc", 0, 0);
    }

}
