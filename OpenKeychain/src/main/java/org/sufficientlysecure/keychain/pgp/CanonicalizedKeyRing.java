/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.spongycastle.openpgp.PGPKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.IterableIterator;

import java.io.IOException;
import java.io.OutputStream;

/** A generic wrapped PGPKeyRing object.
 *
 * This class provides implementations for all basic getters which both
 * PublicKeyRing and SecretKeyRing have in common. To make the wrapped keyring
 * class typesafe in implementing subclasses, the field is stored in the
 * implementing class, providing properly typed access through the getRing
 * getter method.
 *
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

    public String getPrimaryUserId() throws PgpGeneralException {
        return getPublicKey().getPrimaryUserId();
    }

    public String getPrimaryUserIdWithFallback() throws PgpGeneralException {
        return getPublicKey().getPrimaryUserIdWithFallback();
    }

    public boolean isRevoked() throws PgpGeneralException {
        // Is the master key revoked?
        return getRing().getPublicKey().isRevoked();
    }

    public boolean canCertify() throws PgpGeneralException {
        return getRing().getPublicKey().isEncryptionKey();
    }

    public long getEncryptId() throws PgpGeneralException {
        for(CanonicalizedPublicKey key : publicKeyIterator()) {
            if(key.canEncrypt()) {
                return key.getKeyId();
            }
        }
        throw new PgpGeneralException("No valid encryption key found!");
    }

    public boolean hasEncrypt() throws PgpGeneralException {
        try {
            getEncryptId();
            return true;
        } catch(PgpGeneralException e) {
            return false;
        }
    }

    public long getSignId() throws PgpGeneralException {
        for(CanonicalizedPublicKey key : publicKeyIterator()) {
            if(key.canSign()) {
                return key.getKeyId();
            }
        }
        throw new PgpGeneralException("No valid signing key found!");
    }

    public boolean hasSign() throws PgpGeneralException {
        try {
            getSignId();
            return true;
        } catch (PgpGeneralException e) {
            return false;
        }
    }

    public void encode(OutputStream stream) throws IOException {
        getRing().encode(stream);
    }

    /** Returns an UncachedKeyRing which wraps the same data as this ring. This method should
     * only be used */
    public UncachedKeyRing getUncachedKeyRing() {
        return new UncachedKeyRing(getRing());
    }

    abstract PGPKeyRing getRing();

    abstract public IterableIterator<CanonicalizedPublicKey> publicKeyIterator();

    public CanonicalizedPublicKey getPublicKey() {
        return new CanonicalizedPublicKey(this, getRing().getPublicKey());
    }

    public CanonicalizedPublicKey getPublicKey(long id) {
        return new CanonicalizedPublicKey(this, getRing().getPublicKey(id));
    }

    public byte[] getEncoded() throws IOException {
        return getRing().getEncoded();
    }

}
