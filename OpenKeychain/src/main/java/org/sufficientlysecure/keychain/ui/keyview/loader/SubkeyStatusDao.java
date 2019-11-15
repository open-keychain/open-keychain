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

package org.sufficientlysecure.keychain.ui.keyview.loader;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.content.Context;
import androidx.annotation.NonNull;

import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.PgpSecurityConstants;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.KeySecurityProblem;
import org.sufficientlysecure.keychain.daos.KeyRepository;


public class SubkeyStatusDao {
    private final KeyRepository keyRepository;


    public static SubkeyStatusDao getInstance(Context context) {
        KeyRepository keyRepository = KeyRepository.create(context);
        return new SubkeyStatusDao(keyRepository);
    }

    private SubkeyStatusDao(KeyRepository keyRepository) {
        this.keyRepository = keyRepository;
    }

    public KeySubkeyStatus getSubkeyStatus(long masterKeyId) {
        SubKeyItem keyCertify = null;
        ArrayList<SubKeyItem> keysSign = new ArrayList<>();
        ArrayList<SubKeyItem> keysEncrypt = new ArrayList<>();
        for (SubKey subKey : keyRepository.getSubKeysByMasterKeyId(masterKeyId)) {
            SubKeyItem ski = new SubKeyItem(masterKeyId, subKey);

            if (ski.mKeyId == masterKeyId) {
                keyCertify = ski;
            }

            if (ski.mCanSign) {
                keysSign.add(ski);
            }
            if (ski.mCanEncrypt) {
                keysEncrypt.add(ski);
            }
        }

        if (keyCertify == null) {
            if (!keysSign.isEmpty() || !keysEncrypt.isEmpty()) {
                throw new IllegalStateException("Certification key can't be missing for a key that hasn't been deleted!");
            }
            return null;
        }

        Collections.sort(keysSign, SUBKEY_COMPARATOR);
        Collections.sort(keysEncrypt, SUBKEY_COMPARATOR);

        KeyHealthStatus keyHealthStatus = determineKeyHealthStatus(keyCertify, keysSign, keysEncrypt);

        return new KeySubkeyStatus(keyCertify, keysSign, keysEncrypt, keyHealthStatus);
    }

    private KeyHealthStatus determineKeyHealthStatus(SubKeyItem keyCertify,
            ArrayList<SubKeyItem> keysSign,
            ArrayList<SubKeyItem> keysEncrypt) {
        if (keyCertify.mIsRevoked) {
            return KeyHealthStatus.REVOKED;
        }

        if (keyCertify.mIsExpired) {
            return KeyHealthStatus.EXPIRED;
        }

        if (keyCertify.mSecurityProblem != null) {
            return KeyHealthStatus.INSECURE;
        }

        if (!keysSign.isEmpty() && keysEncrypt.isEmpty()) {
            SubKeyItem keySign = keysSign.get(0);
            if (!keySign.isValid()) {
                return KeyHealthStatus.BROKEN;
            }

            if (keySign.mSecurityProblem != null) {
                return KeyHealthStatus.INSECURE;
            }

            return KeyHealthStatus.SIGN_ONLY;
        }

        if (keysSign.isEmpty() || keysEncrypt.isEmpty()) {
            return KeyHealthStatus.BROKEN;
        }

        SubKeyItem keySign = keysSign.get(0);
        SubKeyItem keyEncrypt = keysEncrypt.get(0);

        if (keySign.mSecurityProblem != null && keySign.isValid()
                || keyEncrypt.mSecurityProblem != null && keyEncrypt.isValid()) {
            return KeyHealthStatus.INSECURE;
        }

        if (!keySign.isValid() || !keyEncrypt.isValid()) {
            return KeyHealthStatus.BROKEN;
        }

        if (keyCertify.mSecretKeyType == SecretKeyType.GNU_DUMMY
                && keySign.mSecretKeyType == SecretKeyType.GNU_DUMMY
                && keyEncrypt.mSecretKeyType == SecretKeyType.GNU_DUMMY) {
            return KeyHealthStatus.STRIPPED;
        }

        if (keyCertify.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD
                && keySign.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD
                && keyEncrypt.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD) {
            return KeyHealthStatus.DIVERT;
        }

        boolean containsDivertKeys = keyCertify.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD ||
                keySign.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD ||
                keyEncrypt.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD;
        if (containsDivertKeys) {
            return KeyHealthStatus.DIVERT_PARTIAL;
        }

        boolean containsStrippedKeys = keyCertify.mSecretKeyType == SecretKeyType.GNU_DUMMY
                || keySign.mSecretKeyType == SecretKeyType.GNU_DUMMY
                || keyEncrypt.mSecretKeyType == SecretKeyType.GNU_DUMMY;
        if (containsStrippedKeys) {
            return KeyHealthStatus.PARTIAL_STRIPPED;
        }

        return KeyHealthStatus.OK;
    }

    public enum KeyHealthStatus {
        OK, DIVERT, DIVERT_PARTIAL, REVOKED, EXPIRED, INSECURE, SIGN_ONLY, STRIPPED, PARTIAL_STRIPPED, BROKEN
    }

    public static class KeySubkeyStatus {
        @NonNull
        public final SubKeyItem keyCertify;
        public final List<SubKeyItem> keysSign;
        public final List<SubKeyItem> keysEncrypt;
        public final KeyHealthStatus keyHealthStatus;

        KeySubkeyStatus(@NonNull SubKeyItem keyCertify, List<SubKeyItem> keysSign, List<SubKeyItem> keysEncrypt,
                KeyHealthStatus keyHealthStatus) {
            this.keyCertify = keyCertify;
            this.keysSign = keysSign;
            this.keysEncrypt = keysEncrypt;
            this.keyHealthStatus = keyHealthStatus;
        }
    }

    public static class SubKeyItem {
        final long mKeyId;
        final Date mCreation;
        public final SecretKeyType mSecretKeyType;
        public final boolean mIsRevoked, mIsExpired;
        public final Date mExpiry;
        final boolean mCanCertify, mCanSign, mCanEncrypt;
        public final KeySecurityProblem mSecurityProblem;

        SubKeyItem(long masterKeyId, SubKey subKey) {
            mKeyId = subKey.key_id();
            mCreation = new Date(subKey.creation() * 1000);

            mSecretKeyType = subKey.has_secret();

            mIsRevoked = subKey.is_revoked();
            mExpiry = subKey.expiry() == null ? null : new Date(subKey.expiry() * 1000);
            mIsExpired = mExpiry != null && mExpiry.before(new Date());

            mCanCertify = subKey.can_certify();
            mCanSign = subKey.can_sign();
            mCanEncrypt = subKey.can_encrypt();

            int algorithm = subKey.algorithm();
            Integer bitStrength = subKey.key_size();
            String curveOid = subKey.key_curve_oid();

            mSecurityProblem = PgpSecurityConstants.getKeySecurityProblem(
                    masterKeyId, mKeyId, algorithm, bitStrength, curveOid);
        }

        public boolean newerThan(SubKeyItem other) {
            return mCreation.after(other.mCreation);
        }

        public boolean isValid() {
            return !mIsRevoked && !mIsExpired;
        }
    }

    private static final Comparator<SubKeyItem> SUBKEY_COMPARATOR = (one, two) -> {
        if (one == two) {
            return 0;
        }
        // if one is valid and the other isn't, the valid one always comes first
        if (one.isValid() ^ two.isValid()) {
            return one.isValid() ? -1 : 1;
        }
        // compare usability, if one is "more usable" than the other, that one comes first
        int usability = one.mSecretKeyType.compareUsability(two.mSecretKeyType);
        if (usability != 0) {
            return usability;
        }
        if ((one.mSecurityProblem == null) ^ (two.mSecurityProblem == null)) {
            return one.mSecurityProblem == null ? -1 : 1;
        }
        // otherwise, the newer one comes first
        return one.newerThan(two) ? -1 : 1;
    };
}
