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

import org.spongycastle.bcpg.SignatureSubpacketTags;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.sufficientlysecure.keychain.util.IterableIterator;

/** Wrapper for a PGPPublicKey.
 *
 * The methods implemented in this class are a thin layer over
 * UncachedPublicKey. The difference between the two classes is that objects of
 * this class can only be obtained from a WrappedKeyRing, and that it stores a
 * back reference to its parent as well. A method which works with
 * WrappedPublicKey is therefore guaranteed to work on a KeyRing which is
 * stored in the database.
 *
 */
public class CanonicalizedPublicKey extends UncachedPublicKey {

    // this is the parent key ring
    final KeyRing mRing;
    private Integer mCacheUsage = null;

    CanonicalizedPublicKey(KeyRing ring, PGPPublicKey key) {
        super(key);
        mRing = ring;
    }

    public IterableIterator<String> getUserIds() {
        return new IterableIterator<String>(mPublicKey.getUserIDs());
    }

    JcePublicKeyKeyEncryptionMethodGenerator getPubKeyEncryptionGenerator() {
        return  new JcePublicKeyKeyEncryptionMethodGenerator(mPublicKey);
    }

    public boolean canSign() {
        // if key flags subpacket is available, honor it!
        if (getKeyUsage() != null) {
            return (getKeyUsage() & KeyFlags.SIGN_DATA) != 0;
        }

        if (UncachedKeyRing.isSigningAlgo(mPublicKey.getAlgorithm())) {
            return true;
        }

        return false;
    }

    /**
     * Get all key usage flags.
     * If at least one key flag subpacket is present return these.
     * If no subpacket is present it returns null.
     */
    @SuppressWarnings("unchecked")
    public Integer getKeyUsage() {
        if (mCacheUsage == null) {
            for (PGPSignature sig : new IterableIterator<PGPSignature>(mPublicKey.getSignatures())) {
                if (mPublicKey.isMasterKey() && sig.getKeyID() != mPublicKey.getKeyID()) {
                    continue;
                }

                PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();
                if (hashed != null && hashed.getSubpacket(SignatureSubpacketTags.KEY_FLAGS) != null) {
                    // init if at least one key flag subpacket has been found
                    if (mCacheUsage == null) {
                        mCacheUsage = 0;
                    }
                    mCacheUsage |= hashed.getKeyFlags();
                }
            }
        }
        return mCacheUsage;
    }

    public boolean canCertify() {
        // if key flags subpacket is available, honor it!
        if (getKeyUsage() != null) {
            return (getKeyUsage() & KeyFlags.CERTIFY_OTHER) != 0;
        }

        if (UncachedKeyRing.isSigningAlgo(mPublicKey.getAlgorithm())) {
            return true;
        }

        return false;
    }

    public boolean canEncrypt() {
        // if key flags subpacket is available, honor it!
        if (getKeyUsage() != null) {
            return (getKeyUsage() & (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0;
        }

        // RSA_GENERAL, RSA_ENCRYPT, ELGAMAL_ENCRYPT, ELGAMAL_GENERAL, ECDH
        if (UncachedKeyRing.isEncryptionAlgo(mPublicKey.getAlgorithm())) {
            return true;
        }

        return false;
    }

    public boolean canAuthenticate() {
        // if key flags subpacket is available, honor it!
        if (getKeyUsage() != null) {
            return (getKeyUsage() & KeyFlags.AUTHENTICATION) != 0;
        }

        return false;
    }
}
