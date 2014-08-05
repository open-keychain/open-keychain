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

import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

public class UncachedPublicKey {
    protected final PGPPublicKey mPublicKey;
    private Integer mCacheUsage = null;

    public UncachedPublicKey(PGPPublicKey key) {
        mPublicKey = key;
    }

    public long getKeyId() {
        return mPublicKey.getKeyID();
    }

    /** The revocation signature is NOT checked here, so this may be false! */
    public boolean isRevoked() {
        for (PGPSignature sig : new IterableIterator<PGPSignature>(
                mPublicKey.getSignaturesOfType(isMasterKey() ? PGPSignature.KEY_REVOCATION
                                                             : PGPSignature.SUBKEY_REVOCATION))) {
            return true;
        }
        return false;
    }

    public Date getCreationTime() {
        return mPublicKey.getCreationTime();
    }

    public Date getExpiryTime() {
        long seconds = mPublicKey.getValidSeconds();
        if (seconds > Integer.MAX_VALUE) {
            Log.e(Constants.TAG, "error, expiry time too large");
            return null;
        }
        if (seconds == 0) {
            // no expiry
            return null;
        }
        Date creationDate = getCreationTime();
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(creationDate);
        calendar.add(Calendar.SECOND, (int) seconds);

        return calendar.getTime();
    }

    public boolean isExpired() {
        Date creationDate = mPublicKey.getCreationTime();
        Date expiryDate = mPublicKey.getValidSeconds() > 0
                ? new Date(creationDate.getTime() + mPublicKey.getValidSeconds() * 1000) : null;

        Date now = new Date();
        return creationDate.after(now) || (expiryDate != null && expiryDate.before(now));
    }

    public boolean isMasterKey() {
        return mPublicKey.isMasterKey();
    }

    public int getAlgorithm() {
        return mPublicKey.getAlgorithm();
    }

    public int getBitStrength() {
        return mPublicKey.getBitStrength();
    }

    /** Returns the primary user id, as indicated by the public key's self certificates.
     *
     * This is an expensive operation, since potentially a lot of certificates (and revocations)
     * have to be checked, and even then the result is NOT guaranteed to be constant through a
     * canonicalization operation.
     *
     * Returns null if there is no primary user id (as indicated by certificates)
     *
     */
    public String getPrimaryUserId() {
        String found = null;
        PGPSignature foundSig = null;
        for (String userId : new IterableIterator<String>(mPublicKey.getUserIDs())) {
            PGPSignature revocation = null;

            for (PGPSignature sig : new IterableIterator<PGPSignature>(mPublicKey.getSignaturesForID(userId))) {
                try {

                    // if this is a revocation, this is not the user id
                    if (sig.getSignatureType() == PGPSignature.CERTIFICATION_REVOCATION) {
                        // make sure it's actually valid
                        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider(
                                Constants.BOUNCY_CASTLE_PROVIDER_NAME), mPublicKey);
                        if (!sig.verifyCertification(userId, mPublicKey)) {
                            continue;
                        }
                        if (found != null && found.equals(userId)) {
                            found = null;
                        }
                        revocation = sig;
                        // this revocation may still be overridden by a newer cert
                        continue;
                    }

                    if (sig.getHashedSubPackets() != null && sig.getHashedSubPackets().isPrimaryUserID()) {
                        if (foundSig != null && sig.getCreationTime().before(foundSig.getCreationTime())) {
                            continue;
                        }
                        // ignore if there is a newer revocation for this user id
                        if (revocation != null && sig.getCreationTime().before(revocation.getCreationTime())) {
                            continue;
                        }
                        // make sure it's actually valid
                        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider(
                                Constants.BOUNCY_CASTLE_PROVIDER_NAME), mPublicKey);
                        if (sig.verifyCertification(userId, mPublicKey)) {
                            found = userId;
                            foundSig = sig;
                            // this one can't be relevant anymore at this point
                            revocation = null;
                        }
                    }

                } catch (Exception e) {
                    // nothing bad happens, the key is just not considered the primary key id
                }
            }
        }
        return found;
    }

    /**
     * Returns primary user id if existing. If not, return first encountered user id.
     */
    public String getPrimaryUserIdWithFallback()  {
        String userId = getPrimaryUserId();
        if (userId == null) {
            userId = (String) mPublicKey.getUserIDs().next();
        }
        return userId;
    }

    public ArrayList<String> getUnorderedUserIds() {
        ArrayList<String> userIds = new ArrayList<String>();
        for (String userId : new IterableIterator<String>(mPublicKey.getUserIDs())) {
            userIds.add(userId);
        }
        return userIds;
    }

    public boolean isElGamalEncrypt() {
        return getAlgorithm() == PGPPublicKey.ELGAMAL_ENCRYPT;
    }

    public boolean isDSA() {
        return getAlgorithm() == PGPPublicKey.DSA;
    }

    @SuppressWarnings("unchecked")
    // TODO make this safe
    public int getKeyUsage() {
        if(mCacheUsage == null) {
            mCacheUsage = 0;
            if (mPublicKey.getVersion() >= 4) {
                for (PGPSignature sig : new IterableIterator<PGPSignature>(mPublicKey.getSignatures())) {
                    if (mPublicKey.isMasterKey() && sig.getKeyID() != mPublicKey.getKeyID()) {
                        continue;
                    }

                    PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();
                    if (hashed != null) {
                        mCacheUsage |= hashed.getKeyFlags();
                    }
                }
            }
        }
        return mCacheUsage;
    }

    public boolean canAuthenticate() {
        return mPublicKey.getVersion() <= 3 || (getKeyUsage() & KeyFlags.AUTHENTICATION) != 0;
    }

    public boolean canCertify() {
        return mPublicKey.getVersion() <= 3 || (getKeyUsage() & KeyFlags.CERTIFY_OTHER) != 0;
    }

    public boolean canEncrypt() {
        if (!mPublicKey.isEncryptionKey()) {
            return false;
        }

        // special cases
        if (mPublicKey.getAlgorithm() == PGPPublicKey.ELGAMAL_ENCRYPT) {
            return true;
        }

        if (mPublicKey.getAlgorithm() == PGPPublicKey.RSA_ENCRYPT) {
            return true;
        }

        return mPublicKey.getVersion() <= 3 ||
                (getKeyUsage() & (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0;

    }

    public boolean canSign() {
        // special case
        if (mPublicKey.getAlgorithm() == PGPPublicKey.RSA_SIGN) {
            return true;
        }

        return mPublicKey.getVersion() <= 3 || (getKeyUsage() & KeyFlags.SIGN_DATA) != 0;
    }

    public byte[] getFingerprint() {
        return mPublicKey.getFingerprint();
    }

    // TODO This method should have package visibility - no access outside the pgp package!
    // (It's still used in ProviderHelper at this point)
    public PGPPublicKey getPublicKey() {
        return mPublicKey;
    }

    public Iterator<WrappedSignature> getSignatures() {
        final Iterator<PGPSignature> it = mPublicKey.getSignatures();
        return new Iterator<WrappedSignature>() {
            public void remove() {
                it.remove();
            }
            public WrappedSignature next() {
                return new WrappedSignature(it.next());
            }
            public boolean hasNext() {
                return it.hasNext();
            }
        };
    }

    public Iterator<WrappedSignature> getSignaturesForId(String userId) {
        final Iterator<PGPSignature> it = mPublicKey.getSignaturesForID(userId);
        return new Iterator<WrappedSignature>() {
            public void remove() {
                it.remove();
            }
            public WrappedSignature next() {
                return new WrappedSignature(it.next());
            }
            public boolean hasNext() {
                return it.hasNext();
            }
        };
    }

}
