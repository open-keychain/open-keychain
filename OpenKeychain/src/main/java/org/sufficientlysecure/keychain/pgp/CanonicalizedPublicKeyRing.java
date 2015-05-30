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

import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.util.IterableIterator;

import java.io.IOException;
import java.util.Iterator;

public class CanonicalizedPublicKeyRing extends CanonicalizedKeyRing {

    private PGPPublicKeyRing mRing;

    CanonicalizedPublicKeyRing(PGPPublicKeyRing ring, int verified) {
        super(verified);
        mRing = ring;
    }

    public CanonicalizedPublicKeyRing(byte[] blob, int verified) {
        super(verified);
        if(mRing == null) {
            // get first object in block
            PGPObjectFactory factory = new PGPObjectFactory(blob);
            try {
                Object obj = factory.nextObject();
                if (! (obj instanceof PGPPublicKeyRing)) {
                    throw new RuntimeException("Error constructing CanonicalizedPublicKeyRing, should never happen!");
                }
                mRing = (PGPPublicKeyRing) obj;
                if (factory.nextObject() != null) {
                    throw new RuntimeException("Encountered trailing data after keyring, should never happen!");
                }
            } catch (IOException e) {
                throw new RuntimeException("IO Error constructing CanonicalizedPublicKeyRing, should never happen!");
            }
        }
    }

    PGPPublicKeyRing getRing() {
        return mRing;
    }

    /** Getter that returns the subkey that should be used for signing. */
    CanonicalizedPublicKey getEncryptionSubKey() throws PgpKeyNotFoundException {
        PGPPublicKey key = getRing().getPublicKey(getEncryptId());
        if(key != null) {
            CanonicalizedPublicKey cKey = new CanonicalizedPublicKey(this, key);
            if(!cKey.canEncrypt()) {
                throw new PgpKeyNotFoundException("key error");
            }
            return cKey;
        }
        throw new PgpKeyNotFoundException("no encryption key available");
    }

    public IterableIterator<CanonicalizedPublicKey> publicKeyIterator() {
        @SuppressWarnings("unchecked")
        final Iterator<PGPPublicKey> it = getRing().getPublicKeys();
        return new IterableIterator<>(new Iterator<CanonicalizedPublicKey>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public CanonicalizedPublicKey next() {
                return new CanonicalizedPublicKey(CanonicalizedPublicKeyRing.this, it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        });
    }

    /** Create a dummy secret ring from this key */
    public UncachedKeyRing createDivertSecretRing (byte[] cardAid, long[] subKeyIds) {
        PGPSecretKeyRing secRing = PGPSecretKeyRing.constructDummyFromPublic(getRing(), cardAid);

        if (subKeyIds == null) {
            return new UncachedKeyRing(secRing);
        }

        // if only specific subkeys should be promoted, construct a
        // stripped dummy, then move divert-to-card keys over
        PGPSecretKeyRing newRing = PGPSecretKeyRing.constructDummyFromPublic(getRing());
        for (long subKeyId : subKeyIds) {
            PGPSecretKey key = secRing.getSecretKey(subKeyId);
            if (key != null) {
                newRing = PGPSecretKeyRing.insertSecretKey(newRing, key);
            }
        }

        return new UncachedKeyRing(newRing);

    }

}