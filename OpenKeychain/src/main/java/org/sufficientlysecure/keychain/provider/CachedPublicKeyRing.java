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

package org.sufficientlysecure.keychain.provider;


import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeyRepository.NotFoundException;


/** This implementation of KeyRing provides a cached view of PublicKeyRing
 * objects based on database queries exclusively.
 *
 * This class should be used where only few points of data but no actual
 * cryptographic operations are required about a PublicKeyRing which is already
 * in the database.  This happens commonly in UI code, where parsing of a PGP
 * key for examination would be a very expensive operation.
 *
 * Each getter method is implemented using a more or less expensive database
 * query, while object construction is (almost) free. A common pattern is
 * mProviderHelper.getCachedKeyRing(uri).getterMethod()
 *
 * TODO Ensure that the values returned here always match the ones returned by
 * the parsed KeyRing!
 *
 */
public class CachedPublicKeyRing extends KeyRing {
    private UnifiedKeyInfo unifiedKeyInfo;

    public CachedPublicKeyRing(UnifiedKeyInfo unifiedKeyInfo) {
        this.unifiedKeyInfo = unifiedKeyInfo;
    }

    @Override
    public long getMasterKeyId() {
        return unifiedKeyInfo.master_key_id();
    }

    public byte[] getFingerprint() {
        return unifiedKeyInfo.fingerprint();
    }

    public long getCreationTime() {
        return unifiedKeyInfo.creation();
    }

    @Override
    public String getPrimaryUserId() {
        return unifiedKeyInfo.user_id();
    }

    public String getPrimaryUserIdWithFallback() {
        return getPrimaryUserId();
    }

    @Override
    public boolean isRevoked() {
        return unifiedKeyInfo.is_revoked();
    }

    @Override
    public boolean canCertify() {
        return unifiedKeyInfo.can_certify();
    }

    @Override
    public long getEncryptId() {
        return unifiedKeyInfo.has_encrypt_key_int();
    }

    @Override
    public boolean hasEncrypt() {
        return unifiedKeyInfo.has_encrypt_key();
    }

    public long getAuthenticationId() {
        return unifiedKeyInfo.has_auth_key_int();
    }

    @Override
    public VerificationStatus getVerified() {
        return unifiedKeyInfo.verified();
    }

    public boolean hasAnySecret() {
        return unifiedKeyInfo.has_any_secret();
    }
}
