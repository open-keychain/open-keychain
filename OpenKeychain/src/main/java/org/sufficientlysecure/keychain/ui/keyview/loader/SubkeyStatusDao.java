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
import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.PgpSecurityConstants;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.KeySecurityProblem;
import org.sufficientlysecure.keychain.provider.KeyRepository;


public class SubkeyStatusDao {
    private final KeyRepository keyRepository;


    public static SubkeyStatusDao getInstance(Context context) {
        KeyRepository keyRepository = KeyRepository.create(context);
        return new SubkeyStatusDao(keyRepository);
    }

    private SubkeyStatusDao(KeyRepository keyRepository) {
        this.keyRepository = keyRepository;
    }

    KeySubkeyStatus getSubkeyStatus(long masterKeyId, Comparator<SubKeyItem> comparator) {
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

        Collections.sort(keysSign, comparator);
        Collections.sort(keysEncrypt, comparator);

        return new KeySubkeyStatus(keyCertify, keysSign, keysEncrypt);
    }

    public static class KeySubkeyStatus {
        @NonNull
        public final SubKeyItem keyCertify;
        public final List<SubKeyItem> keysSign;
        public final List<SubKeyItem> keysEncrypt;

        KeySubkeyStatus(@NonNull SubKeyItem keyCertify, List<SubKeyItem> keysSign, List<SubKeyItem> keysEncrypt) {
            this.keyCertify = keyCertify;
            this.keysSign = keysSign;
            this.keysEncrypt = keysEncrypt;
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
}
