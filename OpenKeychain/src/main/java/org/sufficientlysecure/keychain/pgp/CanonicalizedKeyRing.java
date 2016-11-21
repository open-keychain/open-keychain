/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.pgp;

import org.bouncycastle.openpgp.PGPKeyRing;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.IterableIterator;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


/**
 * A generic wrapped PGPKeyRing object.
 * <p>
 * This class provides implementations for all basic getters which both
 * PublicKeyRing and SecretKeyRing have in common. To make the wrapped keyring
 * class typesafe in implementing subclasses, the field is stored in the
 * implementing class, providing properly typed access through the getRing
 * getter method.
 */
public abstract class CanonicalizedKeyRing extends KeyRing {

    private final int mVerified;

    CanonicalizedKeyRing(int verified) {
        mVerified = verified;
    }

    public long getMasterKeyId() {
        return getRing().getPublicKey().getKeyID();
    }

    public int getVerified() {
        return mVerified;
    }

    public byte[] getFingerprint() {
        return getRing().getPublicKey().getFingerprint();
    }

    public byte[] getRawPrimaryUserId() throws PgpKeyNotFoundException {
        return getPublicKey().getRawPrimaryUserId();
    }

    public String getPrimaryUserId() throws PgpKeyNotFoundException {
        return getPublicKey().getPrimaryUserId();
    }

    public String getPrimaryUserIdWithFallback() throws PgpKeyNotFoundException {
        return getPublicKey().getPrimaryUserIdWithFallback();
    }

    public ArrayList<byte[]> getUnorderedRawUserIds() {
        return getPublicKey().getUnorderedRawUserIds();
    }

    public ArrayList<String> getUnorderedUserIds() {
        return getPublicKey().getUnorderedUserIds();
    }

    public boolean isRevoked() {
        // Is the master key revoked?
        return getRing().getPublicKey().hasRevocation();
    }

    public boolean isSecure() {
        return getPublicKey().isSecure();
    }

    public Date getCreationDate() {
        return getPublicKey().getCreationTime();
    }

    public Date getExpirationDate() {
        return getPublicKey().getExpiryTime();
    }

    public boolean isExpired() {
        // Is the master key expired?
        Date creationDate = getCreationDate();
        Date expirationDate = getExpirationDate();

        Date now = new Date();
        return creationDate.after(now) || (expirationDate != null && expirationDate.before(now));
    }

    public boolean canCertify() throws PgpKeyNotFoundException {
        return getRing().getPublicKey().isEncryptionKey();
    }

    public Set<Long> getEncryptIds() {
        HashSet<Long> result = new HashSet<>();
        for (CanonicalizedPublicKey key : publicKeyIterator()) {
            if (key.canEncrypt() && key.isValid()) {
                result.add(key.getKeyId());
            }
        }
        return result;
    }

    public long getEncryptId() throws PgpKeyNotFoundException {
        for (CanonicalizedPublicKey key : publicKeyIterator()) {
            if (key.canEncrypt() && key.isValid()) {
                return key.getKeyId();
            }
        }
        throw new PgpKeyNotFoundException("No valid encryption key found!");
    }

    public boolean hasEncrypt() throws PgpKeyNotFoundException {
        try {
            getEncryptId();
            return true;
        } catch (PgpKeyNotFoundException e) {
            return false;
        }
    }

    public long getSigningId() throws PgpKeyNotFoundException {
        for(CanonicalizedPublicKey key : publicKeyIterator()) {
            if (key.canSign() && key.isValid()) {
                return key.getKeyId();
            }
        }
        throw new PgpKeyNotFoundException("No valid signing key found!");
    }

    public void encode(OutputStream stream) throws IOException {
        getRing().encode(stream);
    }

    /**
     * Returns an UncachedKeyRing which wraps the same data as this ring. This method should
     * only be used
     */
    public UncachedKeyRing getUncachedKeyRing() {
        return new UncachedKeyRing(getRing());
    }

    abstract PGPKeyRing getRing();

    abstract public IterableIterator<CanonicalizedPublicKey> publicKeyIterator();

    public CanonicalizedPublicKey getPublicKey() {
        return new CanonicalizedPublicKey(this, getRing().getPublicKey());
    }

    public CanonicalizedPublicKey getPublicKey(long id) {
        PGPPublicKey pubKey = getRing().getPublicKey(id);
        if (pubKey == null) {
            return null;
        }
        return new CanonicalizedPublicKey(this, pubKey);
    }

    public byte[] getEncoded() throws IOException {
        return getRing().getEncoded();
    }

    /// Returns true iff the keyring contains a primary key or mutually bound subkey with the expected fingerprint
    public boolean containsBoundSubkey(String expectedFingerprint) {
        for (CanonicalizedPublicKey key : publicKeyIterator()) {
            boolean isMasterOrMutuallyBound = key.isMasterKey() || key.canSign();
            if (!isMasterOrMutuallyBound) {
                continue;
            }
            if (KeyFormattingUtils.convertFingerprintToHex(
                    key.getFingerprint()).equalsIgnoreCase(expectedFingerprint)) {
                return true;
            }
        }
        return false;
    }

}
