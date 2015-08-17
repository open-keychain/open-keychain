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

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

public class CanonicalizedSecretKeyRing extends CanonicalizedKeyRing {

    private PGPSecretKeyRing mRing;

    CanonicalizedSecretKeyRing(PGPSecretKeyRing ring, int verified) {
        super(verified);
        mRing = ring;
    }

    public CanonicalizedSecretKeyRing(byte[] blob, int verified)
    {
        super(verified);
        JcaPGPObjectFactory factory = new JcaPGPObjectFactory(blob);
        PGPKeyRing keyRing = null;
        try {
            if ((keyRing = (PGPKeyRing) factory.nextObject()) == null) {
                Log.e(Constants.TAG, "No keys given!");
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, "Error while converting to PGPKeyRing!", e);
        }

        mRing = (PGPSecretKeyRing) keyRing;
    }

    PGPSecretKeyRing getRing() {
        return mRing;
    }

    public CanonicalizedSecretKey getSecretKey() {
        return new CanonicalizedSecretKey(this, mRing.getSecretKey());
    }

    public CanonicalizedSecretKey getSecretKey(long id) {
        return new CanonicalizedSecretKey(this, mRing.getSecretKey(id));
    }

    /** Returns the key id which should be used for signing.
     *
     * This method returns keys which are actually available (ie. secret available, and not stripped,
     * revoked, or expired), hence only works on keyrings where a secret key is available!
     */
    public long getSecretSignId() throws PgpGeneralException {
        for(CanonicalizedSecretKey key : secretKeyIterator()) {
            if (key.canSign() && key.isValid() && key.getSecretKeyType().isUsable()) {
                return key.getKeyId();
            }
        }
        throw new PgpGeneralException("no valid signing key available");
    }

    public IterableIterator<CanonicalizedSecretKey> secretKeyIterator() {
        final Iterator<PGPSecretKey> it = mRing.getSecretKeys();
        return new IterableIterator<>(new Iterator<CanonicalizedSecretKey>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public CanonicalizedSecretKey next() {
                return new CanonicalizedSecretKey(CanonicalizedSecretKeyRing.this, it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        });
    }

    public IterableIterator<CanonicalizedPublicKey> publicKeyIterator() {
        final Iterator<PGPPublicKey> it = getRing().getPublicKeys();
        return new IterableIterator<>(new Iterator<CanonicalizedPublicKey>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public CanonicalizedPublicKey next() {
                return new CanonicalizedPublicKey(CanonicalizedSecretKeyRing.this, it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        });
    }

    public HashMap<String,String> getLocalNotationData() {
        HashMap<String,String> result = new HashMap<>();
        Iterator<PGPSignature> it = getRing().getPublicKey().getKeySignatures();
        while (it.hasNext()) {
            WrappedSignature sig = new WrappedSignature(it.next());
            if (sig.isLocal()) {
                result.putAll(sig.getNotation());
            }
        }
        return result;
    }

}
