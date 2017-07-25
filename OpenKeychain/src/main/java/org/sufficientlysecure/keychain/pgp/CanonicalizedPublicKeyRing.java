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


import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import android.support.annotation.Nullable;

import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.util.IterableIterator;


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
            PGPObjectFactory factory = new PGPObjectFactory(blob, new JcaKeyFingerprintCalculator());
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

    /** Returns a minimized version of this key.
     *
     * The minimized version includes:
     * - the master key
     * - the current best signing key (if any)
     * - one encryption key (if any)
     * - the user id that matches the userIdToKeep parameter, or the primary user id if none matches
     * each with their most recent binding certificates
     */
    public CanonicalizedPublicKeyRing minimize(@Nullable String userIdToKeep) throws IOException, PgpKeyNotFoundException {
        CanonicalizedPublicKey masterKey = getPublicKey();
        PGPPublicKey masterPubKey = masterKey.getPublicKey();
        boolean userIdStrippedOk = false;
        if (userIdToKeep != null) {
            try {
                masterPubKey = PGPPublicKeyUtils.keepOnlyUserId(masterPubKey, userIdToKeep);
                userIdStrippedOk = true;
            } catch (NoSuchElementException e) {
                // will be handled because userIdStrippedOk is false
            }
        }

        if (!userIdStrippedOk) {
            byte[] rawPrimaryUserId = getRawPrimaryUserId();
            masterPubKey = PGPPublicKeyUtils.keepOnlyRawUserId(masterPubKey, rawPrimaryUserId);
        }

        masterPubKey = PGPPublicKeyUtils.keepOnlySelfCertsForUserIds(masterPubKey);
        masterPubKey = PGPPublicKeyUtils.removeAllUserAttributes(masterPubKey);
        masterPubKey = PGPPublicKeyUtils.removeAllDirectKeyCerts(masterPubKey);

        PGPPublicKeyRing resultRing = new PGPPublicKeyRing(masterPubKey.getEncoded(), new JcaKeyFingerprintCalculator());

        Long encryptId;
        try {
            encryptId = getEncryptId();
            // only add if this key doesn't coincide with master key
            if (encryptId != getMasterKeyId()) {
                CanonicalizedPublicKey encryptKey = getPublicKey(encryptId);
                PGPPublicKey encryptPubKey = encryptKey.getPublicKey();
                resultRing = PGPPublicKeyRing.insertPublicKey(resultRing, encryptPubKey);
            }
        } catch (PgpKeyNotFoundException e) {
            // no encryption key: can't be reasonably minimized
            return null;
        }

        try {
            long signingId = getSigningId();
            // only add if this key doesn't coincide with master or encryption key
            if (signingId != encryptId && signingId != getMasterKeyId()) {
                CanonicalizedPublicKey signingKey = getPublicKey(signingId);
                PGPPublicKey signingPubKey = signingKey.getPublicKey();
                resultRing = PGPPublicKeyRing.insertPublicKey(resultRing, signingPubKey);
            }
        } catch (PgpKeyNotFoundException e) {
            // no signing key: can't be reasonably minimized
            return null;
        }

        return new CanonicalizedPublicKeyRing(resultRing, getVerified());
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