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

package org.sufficientlysecure.keychain.pgp;


import java.io.IOException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.bcpg.ECDHPublicBCPGKey;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.RFC6637Utils;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyConverter;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.IterableIterator;
import timber.log.Timber;

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
    final CanonicalizedKeyRing mRing;

    CanonicalizedPublicKey(CanonicalizedKeyRing ring, PGPPublicKey key) {
        super(key);
        mRing = ring;
    }

    public CanonicalizedKeyRing getKeyRing() {
        return mRing;
    }

    public IterableIterator<String> getUserIds() {
        return new IterableIterator<String>(mPublicKey.getUserIDs());
    }

    JcePublicKeyKeyEncryptionMethodGenerator getPubKeyEncryptionGenerator(boolean hiddenRecipients) {
        return new JcePublicKeyKeyEncryptionMethodGenerator(mPublicKey, hiddenRecipients);
    }

    public boolean canSign() {
        // if key flags subpacket is available, honor it!
        if (getKeyUsage() != 0) {
            return (getKeyUsage() & KeyFlags.SIGN_DATA) != 0;
        }

        if (UncachedKeyRing.isSigningAlgo(mPublicKey.getAlgorithm())) {
            return true;
        }

        return false;
    }

    public boolean canCertify() {
        if (!isMasterKey()) {
            return false;
        }

        // if key flags subpacket is available, honor it!
        if (getKeyUsage() != 0) {
            return (getKeyUsage() & KeyFlags.CERTIFY_OTHER) != 0;
        }

        if (UncachedKeyRing.isSigningAlgo(mPublicKey.getAlgorithm())) {
            return true;
        }

        return false;
    }

    public boolean canEncrypt() {
        // if key flags subpacket is available, honor it!
        if (getKeyUsage() != 0) {
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
        if (getKeyUsage() != 0) {
            return (getKeyUsage() & KeyFlags.AUTHENTICATION) != 0;
        }

        return false;
    }

    public boolean isRevoked() {
        return mPublicKey.getSignaturesOfType(isMasterKey()
                ? PGPSignature.KEY_REVOCATION
                : PGPSignature.SUBKEY_REVOCATION).hasNext();
    }

    public boolean isExpired() {
        Date expiry = getExpiryTime();
        return expiry != null && expiry.before(new Date());
    }

    private boolean hasFutureSigningDate() {
        if (isMasterKey()) {
            return false;
        }

        WrappedSignature subkeyBindingSignature = getSubkeyBindingSignature();
        return subkeyBindingSignature.getCreationTime().after(new Date());
    }

    private WrappedSignature getSubkeyBindingSignature() {
        Iterator subkeyBindingSignatures = mPublicKey.getSignaturesOfType(PGPSignature.SUBKEY_BINDING);
        PGPSignature singleSubkeyBindingsignature = (PGPSignature) subkeyBindingSignatures.next();
        if (subkeyBindingSignatures.hasNext()) {
            throw new IllegalStateException();
        }
        return new WrappedSignature(singleSubkeyBindingsignature);
    }

    public Date getBindingSignatureTime() {
        return isMasterKey() ? getCreationTime() : getSubkeyBindingSignature().getCreationTime();
    }

    public boolean isSecure() {
        return PgpSecurityConstants.checkForSecurityProblems(this) == null;
    }

    public long getValidSeconds() {

        long seconds;

        // the getValidSeconds method is unreliable for master keys. we need to iterate all
        // user ids, then use the most recent certification from a non-revoked user id
        if (isMasterKey()) {
            seconds = 0;

            long masterKeyId = getKeyId();

            Date latestCreation = null;
            for (byte[] rawUserId : getUnorderedRawUserIds()) {
                Iterator<WrappedSignature> sigs = getSignaturesForRawId(rawUserId);
                while (sigs.hasNext()) {
                    WrappedSignature sig = sigs.next();
                    if (sig.getKeyId() != masterKeyId) {
                        continue;
                    }
                    if (sig.isRevocation()) {
                        continue;
                    }

                    if (latestCreation == null || latestCreation.before(sig.getCreationTime())) {
                        latestCreation = sig.getCreationTime();
                        seconds = sig.getKeyExpirySeconds();
                    }

                }
            }
        } else {
            seconds = mPublicKey.getValidSeconds();
        }

        return seconds;
    }

    public Date getExpiryTime() {
        long seconds = getValidSeconds();

        if (seconds > Integer.MAX_VALUE) {
            Timber.e("error, expiry time too large");
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

    /** Same method as superclass, but we make it public. */
    public Integer getKeyUsage() {
        return super.getKeyUsage();
    }

    /** Returns whether this key is valid, ie not expired or revoked. */
    public boolean isValid() {
        return !isRevoked() && !isExpired() && !hasFutureSigningDate();
    }

    // For use in key export only; returns the public key in a JCA compatible format.
    public PublicKey getJcaPublicKey() throws PgpGeneralException {
        JcaPGPKeyConverter keyConverter = new JcaPGPKeyConverter();
        PublicKey publicKey;
        try {
            publicKey = keyConverter.getPublicKey(mPublicKey);
        } catch (PGPException e) {
            throw new PgpGeneralException("Error converting public key: "+ e.getMessage(), e);
        }
        return publicKey;
    }

    // For use in card export only; returns the public key in a JCA compatible format.
    public ECPublicKey getSecurityTokenECPublicKey()
            throws PgpGeneralException {
        return (ECPublicKey) getJcaPublicKey();
    }

    public ASN1ObjectIdentifier getSecurityTokenHashAlgorithm()
            throws PGPException {
        if (!isEC()) {
            throw new PGPException("Key encryption OID is valid only for EC key!");
        }

        final ECDHPublicBCPGKey eck = (ECDHPublicBCPGKey)mPublicKey.getPublicKeyPacket().getKey();

        switch (eck.getHashAlgorithm()) {
            case HashAlgorithmTags.SHA256:
                return NISTObjectIdentifiers.id_sha256;
            case HashAlgorithmTags.SHA384:
                return NISTObjectIdentifiers.id_sha384;
            case HashAlgorithmTags.SHA512:
                return NISTObjectIdentifiers.id_sha512;
            default:
                throw new PGPException("Invalid hash algorithm for EC key : " + eck.getHashAlgorithm());
        }
    }

    public int getSecurityTokenSymmetricKeySize()
            throws PGPException {
        if (!isEC()) {
            throw new PGPException("Key encryption OID is valid only for EC key!");
        }

        final ECDHPublicBCPGKey eck = (ECDHPublicBCPGKey)mPublicKey.getPublicKeyPacket().getKey();

        switch (eck.getSymmetricKeyAlgorithm()) {
            case SymmetricKeyAlgorithmTags.AES_128:
                return 128;
            case SymmetricKeyAlgorithmTags.AES_192:
                return 192;
            case SymmetricKeyAlgorithmTags.AES_256:
                return 256;
            default:
                throw new PGPException("Invalid symmetric encryption algorithm for EC key : " + eck.getSymmetricKeyAlgorithm());
        }
    }

    public byte[] createUserKeyingMaterial(KeyFingerPrintCalculator fingerPrintCalculator)
            throws IOException, PGPException {
        return RFC6637Utils.createUserKeyingMaterial(mPublicKey.getPublicKeyPacket(), fingerPrintCalculator);
    }
}
