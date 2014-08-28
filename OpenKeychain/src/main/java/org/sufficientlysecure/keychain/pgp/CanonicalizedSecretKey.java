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

import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.bcpg.S2K;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.PGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.spongycastle.openpgp.operator.jcajce.NfcSyncPGPContentSignerBuilder;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralMsgIdException;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Wrapper for a PGPSecretKey.
 * <p/>
 * This object can only be obtained from a WrappedSecretKeyRing, and stores a
 * back reference to its parent.
 * <p/>
 * This class represents known secret keys which are stored in the database.
 * All "crypto operations using a known secret key" should be implemented in
 * this class, to ensure on type level that these operations are performed on
 * properly imported secret keys only.
 */
public class CanonicalizedSecretKey extends CanonicalizedPublicKey {

    private final PGPSecretKey mSecretKey;
    private PGPPrivateKey mPrivateKey = null;

    private int mPrivateKeyState = PRIVATE_KEY_STATE_LOCKED;
    private static int PRIVATE_KEY_STATE_LOCKED = 0;
    private static int PRIVATE_KEY_STATE_UNLOCKED = 1;
    private static int PRIVATE_KEY_STATE_DIVERT_TO_CARD = 2;

    CanonicalizedSecretKey(CanonicalizedSecretKeyRing ring, PGPSecretKey key) {
        super(ring, key.getPublicKey());
        mSecretKey = key;
    }

    public CanonicalizedSecretKeyRing getRing() {
        return (CanonicalizedSecretKeyRing) mRing;
    }

    /**
     * Returns true on right passphrase
     */
    public boolean unlock(String passphrase) throws PgpGeneralException {
        // handle keys on OpenPGP cards like they were unlocked
        if (mSecretKey.getS2K().getType() == S2K.GNU_DUMMY_S2K
                && mSecretKey.getS2K().getProtectionMode() == S2K.GNU_PROTECTION_MODE_DIVERT_TO_CARD) {
            mPrivateKeyState = PRIVATE_KEY_STATE_DIVERT_TO_CARD;
            return true;
        }

        // try to extract keys using the passphrase
        try {
            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                    Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(passphrase.toCharArray());
            mPrivateKey = mSecretKey.extractPrivateKey(keyDecryptor);
            mPrivateKeyState = PRIVATE_KEY_STATE_UNLOCKED;
        } catch (PGPException e) {
            return false;
        }
        if (mPrivateKey == null) {
            throw new PgpGeneralException("error extracting key");
        }
        return true;
    }

    /**
     * Returns a list of all supported hash algorithms. This list is currently hardcoded to return
     * a limited set of algorithms supported by Yubikeys.
     *
     * @return
     */
    public LinkedList<Integer> getSupportedHashAlgorithms() {
        LinkedList<Integer> supported = new LinkedList<Integer>();

        if (mPrivateKeyState == PRIVATE_KEY_STATE_DIVERT_TO_CARD) {
            // TODO: no support for MD5
            supported.add(HashAlgorithmTags.RIPEMD160);
            supported.add(HashAlgorithmTags.SHA1);
            supported.add(HashAlgorithmTags.SHA224);
            supported.add(HashAlgorithmTags.SHA256);
            supported.add(HashAlgorithmTags.SHA384);
            supported.add(HashAlgorithmTags.SHA512); // preferred is latest
        } else {
            supported.add(HashAlgorithmTags.MD5);
            supported.add(HashAlgorithmTags.RIPEMD160);
            supported.add(HashAlgorithmTags.SHA1);
            supported.add(HashAlgorithmTags.SHA224);
            supported.add(HashAlgorithmTags.SHA256);
            supported.add(HashAlgorithmTags.SHA384);
            supported.add(HashAlgorithmTags.SHA512); // preferred is latest
        }

        return supported;
    }

    public PGPSignatureGenerator getSignatureGenerator(int hashAlgo, boolean cleartext,
                                                       byte[] nfcSignedHash, Date nfcCreationTimestamp)
            throws PgpGeneralException {
        if (mPrivateKeyState == PRIVATE_KEY_STATE_LOCKED) {
            throw new PrivateKeyNotUnlockedException();
        }

        PGPContentSignerBuilder contentSignerBuilder;
        if (mPrivateKeyState == PRIVATE_KEY_STATE_DIVERT_TO_CARD) {
            // to sign using nfc PgpSignEncrypt is executed two times.
            // the first time it stops to return the PendingIntent for nfc connection and signing the hash
            // the second time the signed hash is used.
            // to get the same hash we cache the timestamp for the second round!
            if (nfcCreationTimestamp == null) {
                nfcCreationTimestamp = new Date();
            }

            // use synchronous "NFC based" SignerBuilder
            contentSignerBuilder = new NfcSyncPGPContentSignerBuilder(
                    mSecretKey.getPublicKey().getAlgorithm(), hashAlgo,
                    mSecretKey.getKeyID(), nfcSignedHash, nfcCreationTimestamp)
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

            Log.d(Constants.TAG, "mSecretKey.getKeyID() " + PgpKeyHelper.convertKeyIdToHex(mSecretKey.getKeyID()));
        } else {
            // content signer based on signing key algorithm and chosen hash algorithm
            contentSignerBuilder = new JcaPGPContentSignerBuilder(
                    mSecretKey.getPublicKey().getAlgorithm(), hashAlgo)
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        }

        int signatureType;
        if (cleartext) {
            // for sign-only ascii text
            signatureType = PGPSignature.CANONICAL_TEXT_DOCUMENT;
        } else {
            signatureType = PGPSignature.BINARY_DOCUMENT;
        }

        try {
            PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
            signatureGenerator.init(signatureType, mPrivateKey);

            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            spGen.setSignerUserID(false, mRing.getPrimaryUserIdWithFallback());
            if (nfcCreationTimestamp != null) {
                spGen.setSignatureCreationTime(false, nfcCreationTimestamp);
                Log.d(Constants.TAG, "For NFC: set sig creation time to " + nfcCreationTimestamp);
            }
            signatureGenerator.setHashedSubpackets(spGen.generate());
            return signatureGenerator;
        } catch (PGPException e) {
            // TODO: simply throw PGPException!
            throw new PgpGeneralException("Error initializing signature!", e);
        }
    }

    public PublicKeyDataDecryptorFactory getDecryptorFactory() {
        // TODO: divert to card missing
        if (mPrivateKeyState != PRIVATE_KEY_STATE_UNLOCKED) {
            throw new PrivateKeyNotUnlockedException();
        }
        return new JcePublicKeyDataDecryptorFactoryBuilder()
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(mPrivateKey);
    }

    /**
     * Certify the given pubkeyid with the given masterkeyid.
     *
     * @param publicKeyRing Keyring to add certification to.
     * @param userIds       User IDs to certify, must not be null or empty
     * @return A keyring with added certifications
     */
    public UncachedKeyRing certifyUserIds(CanonicalizedPublicKeyRing publicKeyRing, List<String> userIds)
            throws PgpGeneralMsgIdException, NoSuchAlgorithmException, NoSuchProviderException,
            PGPException, SignatureException {

        // TODO: divert to card missing
        if (mPrivateKeyState != PRIVATE_KEY_STATE_UNLOCKED) {
            throw new PrivateKeyNotUnlockedException();
        }

        // create a signatureGenerator from the supplied masterKeyId and passphrase
        PGPSignatureGenerator signatureGenerator;
        {
            // TODO: SHA256 fixed?
            JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(
                    mSecretKey.getPublicKey().getAlgorithm(), PGPUtil.SHA256)
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

            signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
            signatureGenerator.init(PGPSignature.DEFAULT_CERTIFICATION, mPrivateKey);
        }

        { // supply signatureGenerator with a SubpacketVector
            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            PGPSignatureSubpacketVector packetVector = spGen.generate();
            signatureGenerator.setHashedSubpackets(packetVector);
        }

        // get the master subkey (which we certify for)
        PGPPublicKey publicKey = publicKeyRing.getPublicKey().getPublicKey();

        // fetch public key ring, add the certification and return it
        for (String userId : new IterableIterator<String>(userIds.iterator())) {
            PGPSignature sig = signatureGenerator.generateCertification(userId, publicKey);
            publicKey = PGPPublicKey.addCertification(publicKey, userId, sig);
        }

        PGPPublicKeyRing ring = PGPPublicKeyRing.insertPublicKey(publicKeyRing.getRing(), publicKey);

        return new UncachedKeyRing(ring);
    }

    static class PrivateKeyNotUnlockedException extends RuntimeException {
        // this exception is a programming error which happens when an operation which requires
        // the private key is called without a previous call to unlock()
    }

    public UncachedSecretKey getUncached() {
        return new UncachedSecretKey(mSecretKey);
    }

    // HACK
    public PGPSecretKey getSecretKey() {
        return mSecretKey;
    }

}
