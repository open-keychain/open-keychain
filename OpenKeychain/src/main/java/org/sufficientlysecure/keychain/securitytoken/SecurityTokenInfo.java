package org.sufficientlysecure.keychain.securitytoken;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;


@AutoValue
public abstract class SecurityTokenInfo implements Parcelable {
    private static final byte[] EMPTY_ARRAY = new byte[20];
    private static final Pattern GNUK_VERSION_PATTERN = Pattern.compile("FSIJ-(\\d\\.\\d\\.\\d)-.+");

    public abstract TransportType getTransportType();
    public abstract TokenType getTokenType();

    public abstract List<byte[]> getFingerprints();
    @Nullable
    public abstract byte[] getAid();
    @Nullable
    public abstract String getUserId();
    @Nullable
    public abstract String getUrl();
    public abstract int getVerifyRetries();
    public abstract int getVerifyAdminRetries();
    public abstract boolean hasLifeCycleManagement();

    public boolean isEmpty() {
        return getFingerprints().isEmpty();
    }

    public static SecurityTokenInfo create(TransportType transportType, TokenType tokenType, byte[][] fingerprints,
            byte[] aid, String userId, String url,
            int verifyRetries, int verifyAdminRetries,
            boolean hasLifeCycleSupport) {
        ArrayList<byte[]> fingerprintList = new ArrayList<>(fingerprints.length);
        for (byte[] fingerprint : fingerprints) {
            if (!Arrays.equals(EMPTY_ARRAY, fingerprint)) {
                fingerprintList.add(fingerprint);
            }
        }
        return new AutoValue_SecurityTokenInfo(
                transportType, tokenType, fingerprintList, aid, userId, url, verifyRetries, verifyAdminRetries, hasLifeCycleSupport);
    }

    public static SecurityTokenInfo newInstanceDebugKeyserver() {
        if (!BuildConfig.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(TransportType.NFC, TokenType.UNKNOWN,
                new byte[][] { KeyFormattingUtils.convertFingerprintHexFingerprint("1efdb4845ca242ca6977fddb1f788094fd3b430a") },
                Hex.decode("010203040506"), "yubinu2@mugenguild.com", null, 3, 3, true);
    }

    public static SecurityTokenInfo newInstanceDebugUri() {
        if (!BuildConfig.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(TransportType.NFC, TokenType.UNKNOWN,
                new byte[][] { KeyFormattingUtils.convertFingerprintHexFingerprint("4700BA1AC417ABEF3CC7765AD686905837779C3E") },
                Hex.decode("010203040506"), "yubinu2@mugenguild.com", "http://valodim.stratum0.net/mryubinu2.asc", 3, 3, true);
    }

    public static SecurityTokenInfo newInstanceDebugLocked() {
        if (!BuildConfig.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(TransportType.NFC, TokenType.UNKNOWN,
                new byte[][] { KeyFormattingUtils.convertFingerprintHexFingerprint("4700BA1AC417ABEF3CC7765AD686905837779C3E") },
                Hex.decode("010203040506"), "yubinu2@mugenguild.com", "http://valodim.stratum0.net/mryubinu2.asc", 0, 3, true);
    }

    public static SecurityTokenInfo newInstanceDebugLockedHard() {
        if (!BuildConfig.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(TransportType.NFC, TokenType.UNKNOWN,
                new byte[][] { KeyFormattingUtils.convertFingerprintHexFingerprint("4700BA1AC417ABEF3CC7765AD686905837779C3E") },
                Hex.decode("010203040506"), "yubinu2@mugenguild.com", "http://valodim.stratum0.net/mryubinu2.asc", 0, 0, true);
    }

    public enum TransportType {
        NFC, USB
    }

    public enum TokenType {
        YUBIKEY_NEO, YUBIKEY_4, FIDESMO, NITROKEY_PRO, NITROKEY_STORAGE, NITROKEY_START,
        GNUK_OLD, GNUK_UNKNOWN, GNUK_1_25_AND_NEWER, LEDGER_NANO_S, UNKNOWN
    }

    private static final HashSet<TokenType> SUPPORTED_USB_TOKENS = new HashSet<>(Arrays.asList(
            TokenType.YUBIKEY_NEO,
            TokenType.YUBIKEY_4,
            TokenType.NITROKEY_PRO,
            TokenType.NITROKEY_STORAGE,
            TokenType.GNUK_OLD,
            TokenType.GNUK_UNKNOWN,
            TokenType.GNUK_1_25_AND_NEWER
    ));

    private static final HashSet<TokenType> SUPPORTED_USB_RESET = new HashSet<>(Arrays.asList(
            TokenType.YUBIKEY_NEO,
            TokenType.YUBIKEY_4,
            TokenType.NITROKEY_PRO,
            TokenType.GNUK_1_25_AND_NEWER
    ));

    private static final HashSet<TokenType> SUPPORTED_USB_PUT_KEY = new HashSet<>(Arrays.asList(
            TokenType.YUBIKEY_NEO,
            TokenType.YUBIKEY_4, // Not clear, will be tested: https://github.com/open-keychain/open-keychain/issues/2069
            TokenType.NITROKEY_PRO
    ));

    public boolean isSecurityTokenSupported() {
        boolean isKnownSupported = SUPPORTED_USB_TOKENS.contains(getTokenType());
        boolean isNfcTransport = getTransportType() == TransportType.NFC;

        return isKnownSupported || isNfcTransport;
    }

    public boolean isPutKeySupported() {
        boolean isKnownSupported = SUPPORTED_USB_PUT_KEY.contains(getTokenType());
        boolean isNfcTransport = getTransportType() == TransportType.NFC;

        return isKnownSupported || isNfcTransport;
    }

    public boolean isResetSupported() {
        boolean isKnownSupported = SUPPORTED_USB_RESET.contains(getTokenType());
        boolean isNfcTransport = getTransportType() == TransportType.NFC;
        boolean hasLifeCycleManagement = hasLifeCycleManagement();

        return (isKnownSupported || isNfcTransport) && hasLifeCycleManagement;
    }

    public static Version parseGnukVersionString(String serialNo) {
        if (serialNo == null) {
            return null;
        }

        Matcher matcher = GNUK_VERSION_PATTERN.matcher(serialNo);
        if (!matcher.matches()) {
            return null;
        }
        return new Version(matcher.group(1));
    }

    public static class Version implements Comparable<Version> {

        private String version;

        public final String get() {
            return this.version;
        }

        public Version(String version) {
            if (version == null) {
                throw new IllegalArgumentException("Version can not be null");
            }
            if (!version.matches("[0-9]+(\\.[0-9]+)*")) {
                throw new IllegalArgumentException("Invalid version format");
            }
            this.version = version;
        }

        @Override
        public int compareTo(@NonNull Version that) {
            String[] thisParts = this.get().split("\\.");
            String[] thatParts = that.get().split("\\.");
            int length = Math.max(thisParts.length, thatParts.length);
            for (int i = 0; i < length; i++) {
                int thisPart = i < thisParts.length ?
                        Integer.parseInt(thisParts[i]) : 0;
                int thatPart = i < thatParts.length ?
                        Integer.parseInt(thatParts[i]) : 0;
                if (thisPart < thatPart) {
                    return -1;
                }
                if (thisPart > thatPart) {
                    return 1;
                }
            }
            return 0;
        }

        @Override
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (that == null) {
                return false;
            }
            if (this.getClass() != that.getClass()) {
                return false;
            }
            return this.compareTo((Version) that) == 0;
        }

    }
}
