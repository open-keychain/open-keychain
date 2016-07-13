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
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
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

    public enum SecretKeyRingType {
        UNAVAILABLE(0), PASSPHRASE(1), PASSPHRASE_EMPTY(2), PIN(3), PATTERN(4);

        final int mNum;

        SecretKeyRingType(int num) {
            mNum = num;
        }

        public static SecretKeyRingType fromNum(int num) {
            switch (num) {
                case 1:
                    return PASSPHRASE;
                case 2:
                    return PASSPHRASE_EMPTY;
                case 3:
                    return PIN;
                case 4:
                    return PATTERN;
                default:
                    // no secret keyring
                    return UNAVAILABLE;
            }
        }

        public int getNum() {
            return mNum;
        }
    }

    public CanonicalizedSecretKeyRing(byte[] blob, boolean isRevoked, int verified)
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
