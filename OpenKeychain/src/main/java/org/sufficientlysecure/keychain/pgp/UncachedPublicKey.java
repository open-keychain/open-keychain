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

import org.bouncycastle.bcpg.ECPublicBCPGKey;
import org.bouncycastle.bcpg.SignatureSubpacketTags;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureSubpacketVector;
import org.bouncycastle.openpgp.PGPUserAttributeSubpacketVector;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Utf8Util;

import java.util.ArrayList;
import java.util.Arrays;
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
    public boolean isMaybeRevoked() {
        return mPublicKey.getSignaturesOfType(isMasterKey()
                ? PGPSignature.KEY_REVOCATION
                : PGPSignature.SUBKEY_REVOCATION).hasNext();
    }

    public Date getCreationTime() {
        return mPublicKey.getCreationTime();
    }

    /** The revocation signature is NOT checked here, so this may be false! */
    public boolean isMaybeExpired() {
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

    public Integer getBitStrength() {
        if (isEC()) {
            return null;
        }
        return mPublicKey.getBitStrength();
    }

    public String getCurveOid() {
        if ( ! isEC()) {
            return null;
        }
        if ( ! (mPublicKey.getPublicKeyPacket().getKey() instanceof ECPublicBCPGKey)) {
            return null;
        }
        return ((ECPublicBCPGKey) mPublicKey.getPublicKeyPacket().getKey()).getCurveOID().getId();
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
        byte[] found = null;
        PGPSignature foundSig = null;
        // noinspection unchecked
        for (byte[] rawUserId : new IterableIterator<byte[]>(mPublicKey.getRawUserIDs())) {
            PGPSignature revocation = null;

            @SuppressWarnings("unchecked")
            Iterator<PGPSignature> signaturesIt = mPublicKey.getSignaturesForID(rawUserId);
            // no signatures for this User ID
            if (signaturesIt == null) {
                continue;
            }

            for (PGPSignature sig : new IterableIterator<>(signaturesIt)) {
                try {

                    // if this is a revocation, this is not the user id
                    if (sig.getSignatureType() == PGPSignature.CERTIFICATION_REVOCATION) {
                        // make sure it's actually valid
                        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider(
                                Constants.BOUNCY_CASTLE_PROVIDER_NAME), mPublicKey);
                        if (!sig.verifyCertification(rawUserId, mPublicKey)) {
                            continue;
                        }
                        if (found != null && Arrays.equals(found, rawUserId)) {
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
                        if (sig.verifyCertification(rawUserId, mPublicKey)) {
                            found = rawUserId;
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
        if (found != null) {
            return Utf8Util.fromUTF8ByteArrayReplaceBadEncoding(found);
        } else {
            return null;
        }
    }

    /**
     * Returns primary user id if existing. If not, return first encountered user id. If there
     * is no user id, return null (this can only happen for not yet canonicalized keys during import)
     */
    public String getPrimaryUserIdWithFallback()  {
        String userId = getPrimaryUserId();
        if (userId == null) {
            Iterator<String> it = mPublicKey.getUserIDs();
            userId = it.hasNext() ? it.next() : null;
        }
        return userId;
    }

    public ArrayList<String> getUnorderedUserIds() {
        ArrayList<String> userIds = new ArrayList<>();
        for (byte[] rawUserId : new IterableIterator<byte[]>(mPublicKey.getRawUserIDs())) {
            // use our decoding method
            userIds.add(Utf8Util.fromUTF8ByteArrayReplaceBadEncoding(rawUserId));
        }
        return userIds;
    }

    public ArrayList<byte[]> getUnorderedRawUserIds() {
        ArrayList<byte[]> userIds = new ArrayList<>();
        for (byte[] userId : new IterableIterator<byte[]>(mPublicKey.getRawUserIDs())) {
            userIds.add(userId);
        }
        return userIds;
    }

    public ArrayList<WrappedUserAttribute> getUnorderedUserAttributes() {
        ArrayList<WrappedUserAttribute> userAttributes = new ArrayList<>();
        for (PGPUserAttributeSubpacketVector userAttribute :
                new IterableIterator<PGPUserAttributeSubpacketVector>(mPublicKey.getUserAttributes())) {
            userAttributes.add(new WrappedUserAttribute(userAttribute));
        }
        return userAttributes;
    }

    public boolean isElGamalEncrypt() {
        return getAlgorithm() == PGPPublicKey.ELGAMAL_ENCRYPT;
    }

    public boolean isRSA() {
        return getAlgorithm() == PGPPublicKey.RSA_GENERAL
                || getAlgorithm() == PGPPublicKey.RSA_ENCRYPT
                || getAlgorithm() == PGPPublicKey.RSA_SIGN;
    }

    public boolean isDSA() {
        return getAlgorithm() == PGPPublicKey.DSA;
    }

    public boolean isEC() {
        return getAlgorithm() == PGPPublicKey.ECDH
                || getAlgorithm() == PGPPublicKey.ECDSA
                || getAlgorithm() == PGPPublicKey.EDDSA;
    }

    public byte[] getFingerprint() {
        return mPublicKey.getFingerprint();
    }

    // (It's still used in ProviderHelper at this point)
    PGPPublicKey getPublicKey() {
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

    public Iterator<WrappedSignature> getSignaturesForRawId(byte[] rawUserId) {
        final Iterator<PGPSignature> it = mPublicKey.getSignaturesForID(rawUserId);
        if (it != null) {
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
        } else {
            return null;
        }
    }

    public Iterator<WrappedSignature> getSignaturesForUserAttribute(WrappedUserAttribute attribute) {
        final Iterator<PGPSignature> it = mPublicKey.getSignaturesForUserAttribute(attribute.getVector());
        if (it != null) {
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
        } else {
            return null;
        }
    }

    /** Get all key usage flags.
     * If at least one key flag subpacket is present return these. If no
     * subpacket is present it returns null.
     *
     * Note that this method has package visiblity because it is used in test
     * cases. Certificates of UncachedPublicKey instances can NOT be assumed to
     * be verified or even by the correct key, so the result of this method
     * should never be used in other places!
     */
    @SuppressWarnings("unchecked")
    Integer getKeyUsage() {
        if (mCacheUsage == null) {
            PGPSignature mostRecentSig = null;
            for (PGPSignature sig : new IterableIterator<PGPSignature>(mPublicKey.getSignatures())) {
                if (mPublicKey.isMasterKey() && sig.getKeyID() != mPublicKey.getKeyID()) {
                    continue;
                }

                switch (sig.getSignatureType()) {
                    case PGPSignature.DEFAULT_CERTIFICATION:
                    case PGPSignature.POSITIVE_CERTIFICATION:
                    case PGPSignature.CASUAL_CERTIFICATION:
                    case PGPSignature.NO_CERTIFICATION:
                    case PGPSignature.SUBKEY_BINDING:
                        break;
                    // if this is not one of the above types, don't care
                    default:
                        continue;
                }

                // If we have no sig yet, take the first we can get
                if (mostRecentSig == null) {
                    mostRecentSig = sig;
                    continue;
                }

                // If the new sig is less recent, skip it
                if (mostRecentSig.getCreationTime().after(sig.getCreationTime())) {
                    continue;
                }

                // Otherwise, note it down as the new "most recent" one
                mostRecentSig = sig;
            }

            // Initialize to 0 as cached but empty value, if there is no sig (can't happen
            // for canonicalized keyring), or there is no KEY_FLAGS packet in the sig
            mCacheUsage = 0;
            if (mostRecentSig != null) {
                // If a mostRecentSig has been found, (cache and) return its flags
                PGPSignatureSubpacketVector hashed = mostRecentSig.getHashedSubPackets();
                if (hashed != null && hashed.getSubpacket(SignatureSubpacketTags.KEY_FLAGS) != null) {
                    mCacheUsage = hashed.getKeyFlags();
                }
            }

        }
        return mCacheUsage;
    }

    // this method relies on UNSAFE assumptions about the keyring, and should ONLY be used for
    // TEST CASES!!
    Date getUnsafeExpiryTimeForTesting () {
        long valid = mPublicKey.getValidSeconds();

        if (valid > Integer.MAX_VALUE) {
            Log.e(Constants.TAG, "error, expiry time too large");
            return null;
        }
        if (valid == 0) {
            // no expiry
            return null;
        }
        Date creationDate = getCreationTime();
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(creationDate);
        calendar.add(Calendar.SECOND, (int) valid);

        return calendar.getTime();
    }

}
