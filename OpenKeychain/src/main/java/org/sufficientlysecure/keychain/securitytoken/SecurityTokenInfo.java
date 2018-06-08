/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.securitytoken;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;


@AutoValue
public abstract class SecurityTokenInfo implements Parcelable {
    private static final byte[] EMPTY_ARRAY = new byte[20];
    private static final Pattern GNUK_VERSION_PATTERN = Pattern.compile("FSIJ-(\\d\\.\\d\\.\\d)-.+");

    public abstract TransportType getTransportType();
    public abstract TokenType getTokenType();

    public abstract List<byte[]> getFingerprints();
    @Nullable
    @SuppressWarnings("mutable")
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
        if (!Constants.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(TransportType.NFC, TokenType.UNKNOWN,
                new byte[][] { KeyFormattingUtils.convertFingerprintHexFingerprint("1efdb4845ca242ca6977fddb1f788094fd3b430a") },
                Hex.decode("010203040506"), "yubinu2@mugenguild.com", null, 3, 3, true);
    }

    public static SecurityTokenInfo newInstanceDebugUri() {
        if (!Constants.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(TransportType.NFC, TokenType.UNKNOWN,
                new byte[][] { KeyFormattingUtils.convertFingerprintHexFingerprint("4700BA1AC417ABEF3CC7765AD686905837779C3E") },
                Hex.decode("010203040506"), "yubinu2@mugenguild.com", "http://valodim.stratum0.net/mryubinu2.asc", 3, 3, true);
    }

    public static SecurityTokenInfo newInstanceDebugLocked() {
        if (!Constants.DEBUG) {
            throw new UnsupportedOperationException("This operation is only available in debug builds!");
        }
        return SecurityTokenInfo.create(TransportType.NFC, TokenType.UNKNOWN,
                new byte[][] { KeyFormattingUtils.convertFingerprintHexFingerprint("4700BA1AC417ABEF3CC7765AD686905837779C3E") },
                Hex.decode("010203040506"), "yubinu2@mugenguild.com", "http://valodim.stratum0.net/mryubinu2.asc", 0, 3, true);
    }

    public static SecurityTokenInfo newInstanceDebugLockedHard() {
        if (!Constants.DEBUG) {
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
        YUBIKEY_NEO, YUBIKEY_4, FIDESMO, NITROKEY_PRO, NITROKEY_STORAGE, NITROKEY_START_OLD,
        NITROKEY_START_1_25_AND_NEWER, GNUK_OLD, GNUK_1_25_AND_NEWER, LEDGER_NANO_S, UNKNOWN
    }

    public static final Set<TokenType> SUPPORTED_USB_TOKENS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            TokenType.YUBIKEY_NEO,
            TokenType.YUBIKEY_4,
            TokenType.NITROKEY_PRO,
            TokenType.NITROKEY_STORAGE,
            TokenType.NITROKEY_START_OLD,
            TokenType.NITROKEY_START_1_25_AND_NEWER,
            TokenType.GNUK_OLD,
            TokenType.GNUK_1_25_AND_NEWER,
            TokenType.LEDGER_NANO_S
    )));

    private static final Set<TokenType> SUPPORTED_USB_SETUP = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            TokenType.YUBIKEY_NEO,
            TokenType.YUBIKEY_4,
            TokenType.NITROKEY_PRO,
            TokenType.NITROKEY_STORAGE,
            TokenType.NITROKEY_START_1_25_AND_NEWER,
            TokenType.GNUK_1_25_AND_NEWER
    )));

    public boolean isPutKeySupported() {
        boolean isKnownSupported = SUPPORTED_USB_SETUP.contains(getTokenType());
        boolean isNfcTransport = getTransportType() == TransportType.NFC;

        return isKnownSupported || isNfcTransport;
    }

    public boolean isResetSupported() {
        boolean isKnownSupported = SUPPORTED_USB_SETUP.contains(getTokenType());
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
        return Version.create(matcher.group(1));
    }

    public double getOpenPgpVersion() {
        byte[] aid = getAid();
        float minv = aid[7];
        while (minv > 0) minv /= 10.0;
        return aid[6] + minv;
    }

    @AutoValue
    public static abstract class Version implements Comparable<Version> {

        abstract String getVersion();

        public static Version create(@NonNull String version) {
            if (!version.matches("[0-9]+(\\.[0-9]+)*")) {
                throw new IllegalArgumentException("Invalid version format");
            }
            return new AutoValue_SecurityTokenInfo_Version(version);
        }

        @Override
        public int compareTo(@NonNull Version that) {
            String[] thisParts = this.getVersion().split("\\.");
            String[] thatParts = that.getVersion().split("\\.");
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

    }
}
