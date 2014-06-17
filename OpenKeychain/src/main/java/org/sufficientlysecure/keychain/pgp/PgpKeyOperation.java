/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.jce.spec.ElGamalParameterSpec;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyPair;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.spongycastle.openpgp.operator.PGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.PGPDigestCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralMsgIdException;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogLevel;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogType;
import org.sufficientlysecure.keychain.service.OperationResultParcel.OperationLog;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Primes;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * This class is the single place where ALL operations that actually modify a PGP public or secret
 * key take place.
 * <p/>
 * Note that no android specific stuff should be done here, ie no imports from com.android.
 * <p/>
 * All operations support progress reporting to a Progressable passed on initialization.
 * This indicator may be null.
 */
public class PgpKeyOperation {
    private Progressable mProgress;

    private static final int[] PREFERRED_SYMMETRIC_ALGORITHMS = new int[]{
            SymmetricKeyAlgorithmTags.AES_256, SymmetricKeyAlgorithmTags.AES_192,
            SymmetricKeyAlgorithmTags.AES_128, SymmetricKeyAlgorithmTags.CAST5,
            SymmetricKeyAlgorithmTags.TRIPLE_DES};
    private static final int[] PREFERRED_HASH_ALGORITHMS = new int[]{HashAlgorithmTags.SHA1,
            HashAlgorithmTags.SHA256, HashAlgorithmTags.RIPEMD160};
    private static final int[] PREFERRED_COMPRESSION_ALGORITHMS = new int[]{
            CompressionAlgorithmTags.ZLIB, CompressionAlgorithmTags.BZIP2,
            CompressionAlgorithmTags.ZIP};

    public PgpKeyOperation(Progressable progress) {
        super();
        this.mProgress = progress;
    }

    void updateProgress(int message, int current, int total) {
        if (mProgress != null) {
            mProgress.setProgress(message, current, total);
        }
    }

    /** Creates new secret key. */
    private PGPSecretKey createKey(int algorithmChoice, int keySize, String passphrase,
                                  boolean isMasterKey) throws PgpGeneralMsgIdException {

        try {
            if (keySize < 512) {
                throw new PgpGeneralMsgIdException(R.string.error_key_size_minimum512bit);
            }

            if (passphrase == null) {
                passphrase = "";
            }

            int algorithm;
            KeyPairGenerator keyGen;

            switch (algorithmChoice) {
                case Constants.choice.algorithm.dsa: {
                    keyGen = KeyPairGenerator.getInstance("DSA", Constants.BOUNCY_CASTLE_PROVIDER_NAME);
                    keyGen.initialize(keySize, new SecureRandom());
                    algorithm = PGPPublicKey.DSA;
                    break;
                }

                case Constants.choice.algorithm.elgamal: {
                    if (isMasterKey) {
                        throw new PgpGeneralMsgIdException(R.string.error_master_key_must_not_be_el_gamal);
                    }
                    keyGen = KeyPairGenerator.getInstance("ElGamal", Constants.BOUNCY_CASTLE_PROVIDER_NAME);
                    BigInteger p = Primes.getBestPrime(keySize);
                    BigInteger g = new BigInteger("2");

                    ElGamalParameterSpec elParams = new ElGamalParameterSpec(p, g);

                    keyGen.initialize(elParams);
                    algorithm = PGPPublicKey.ELGAMAL_ENCRYPT;
                    break;
                }

                case Constants.choice.algorithm.rsa: {
                    keyGen = KeyPairGenerator.getInstance("RSA", Constants.BOUNCY_CASTLE_PROVIDER_NAME);
                    keyGen.initialize(keySize, new SecureRandom());

                    algorithm = PGPPublicKey.RSA_GENERAL;
                    break;
                }

                default: {
                    throw new PgpGeneralMsgIdException(R.string.error_unknown_algorithm_choice);
                }
            }

            // build new key pair
            PGPKeyPair keyPair = new JcaPGPKeyPair(algorithm, keyGen.generateKeyPair(), new Date());

            // define hashing and signing algos
            PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(
                    HashAlgorithmTags.SHA1);

            // Build key encrypter and decrypter based on passphrase
            PBESecretKeyEncryptor keyEncryptor = new JcePBESecretKeyEncryptorBuilder(
                    PGPEncryptedData.CAST5, sha1Calc)
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(passphrase.toCharArray());

            return new PGPSecretKey(keyPair.getPrivateKey(), keyPair.getPublicKey(),
                    sha1Calc, isMasterKey, keyEncryptor);
        } catch(NoSuchProviderException e) {
            throw new RuntimeException(e);
        } catch(NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch(InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch(PGPException e) {
            throw new PgpGeneralMsgIdException(R.string.msg_mr_error_pgp, e);
        }
    }

    /** This method introduces a list of modifications specified by a SaveKeyringParcel to a
     * WrappedSecretKeyRing.
     *
     * This method relies on WrappedSecretKeyRing's canonicalization property!
     *
     * Note that PGPPublicKeyRings can not be directly modified. Instead, the corresponding
     * PGPSecretKeyRing must be modified and consequently consolidated with its public counterpart.
     * This is a natural workflow since pgp keyrings are immutable data structures: Old semantics
     * are changed by adding new certificates, which implicitly override older certificates.
     *
     */
    public UncachedKeyRing modifySecretKeyRing(WrappedSecretKeyRing wsKR, SaveKeyringParcel saveParcel,
                                               String passphrase, OperationLog log, int indent) {

        /*
         * 1. Unlock private key
         * 2a. Add certificates for new user ids
         * 2b. Add revocations for revoked user ids
         * 3. If primary user id changed, generate new certificates for both old and new
         * 4a. For each subkey change, generate new subkey binding certificate
         * 4b. For each subkey revocation, generate new subkey revocation certificate
         * 5. Generate and add new subkeys
         * 6. If requested, change passphrase
         */

        log.add(LogLevel.START, LogType.MSG_MR, indent);
        indent += 1;
        updateProgress(R.string.progress_building_key, 0, 100);

        // We work on bouncycastle object level here
        PGPSecretKeyRing sKR = wsKR.getRing();
        PGPPublicKey masterPublicKey = sKR.getPublicKey();
        PGPSecretKey masterSecretKey = sKR.getSecretKey();

        // 1. Unlock private key
        log.add(LogLevel.DEBUG, LogType.MSG_MR_UNLOCK, indent);
        PGPPrivateKey masterPrivateKey; {
            try {
                PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                        Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(passphrase.toCharArray());
                masterPrivateKey = masterSecretKey.extractPrivateKey(keyDecryptor);
            } catch (PGPException e) {
                log.add(LogLevel.ERROR, LogType.MSG_MR_UNLOCK_ERROR, indent+1);
                return null;
            }
        }
        if (!Arrays.equals(saveParcel.mFingerprint, sKR.getPublicKey().getFingerprint())) {
            return null;
        }

        updateProgress(R.string.progress_certifying_master_key, 20, 100);

        // work on master secret key
        try {

            PGPPublicKey modifiedPublicKey = masterPublicKey;

            // 2a. Add certificates for new user ids
            for (String userId : saveParcel.addUserIds) {
                log.add(LogLevel.INFO, LogType.MSG_MR_UID_ADD, indent);
                PGPSignature cert = generateUserIdSignature(masterPrivateKey,
                        masterPublicKey, userId, false);
                modifiedPublicKey = PGPPublicKey.addCertification(masterPublicKey, userId, cert);
            }

            // 2b. Add revocations for revoked user ids
            for (String userId : saveParcel.revokeUserIds) {
                log.add(LogLevel.INFO, LogType.MSG_MR_UID_REVOKE, indent);
                PGPSignature cert = generateRevocationSignature(masterPrivateKey,
                        masterPublicKey, userId);
                modifiedPublicKey = PGPPublicKey.addCertification(masterPublicKey, userId, cert);
            }

            // 3. If primary user id changed, generate new certificates for both old and new
            if (saveParcel.changePrimaryUserId != null) {
                log.add(LogLevel.INFO, LogType.MSG_MR_UID_PRIMARY, indent);
                // todo
            }

            // Update the secret key ring
            if (modifiedPublicKey != masterPublicKey) {
                masterSecretKey = PGPSecretKey.replacePublicKey(masterSecretKey, modifiedPublicKey);
                masterPublicKey = modifiedPublicKey;
                sKR = PGPSecretKeyRing.insertSecretKey(sKR, masterSecretKey);
            }


        // 4a. For each subkey change, generate new subkey binding certificate
            for (SaveKeyringParcel.SubkeyChange change : saveParcel.changeSubKeys) {
                log.add(LogLevel.INFO, LogType.MSG_MR_SUBKEY_CHANGE,
                        new String[]{PgpKeyHelper.convertKeyIdToHex(change.mKeyId)}, indent);
                PGPSecretKey sKey = sKR.getSecretKey(change.mKeyId);
                if (sKey == null) {
                    log.add(LogLevel.ERROR, LogType.MSG_MR_SUBKEY_MISSING,
                            new String[]{PgpKeyHelper.convertKeyIdToHex(change.mKeyId)}, indent + 1);
                    return null;
                }
                PGPPublicKey pKey = sKey.getPublicKey();

                if (change.mExpiry != null && new Date(change.mExpiry).before(new Date())) {
                    log.add(LogLevel.ERROR, LogType.MSG_MR_SUBKEY_PAST_EXPIRY,
                            new String[]{PgpKeyHelper.convertKeyIdToHex(change.mKeyId)}, indent + 1);
                    return null;
                }

                // generate and add new signature
                PGPSignature sig = generateSubkeyBindingSignature(masterPublicKey, masterPrivateKey,
                        sKey, pKey, change.mFlags, change.mExpiry, passphrase);
                pKey = PGPPublicKey.addCertification(pKey, sig);
                sKR = PGPSecretKeyRing.insertSecretKey(sKR, PGPSecretKey.replacePublicKey(sKey, pKey));
            }

            // 4b. For each subkey revocation, generate new subkey revocation certificate
            for (long revocation : saveParcel.revokeSubKeys) {
                log.add(LogLevel.INFO, LogType.MSG_MR_SUBKEY_REVOKE,
                        new String[] { PgpKeyHelper.convertKeyIdToHex(revocation) }, indent);
                PGPSecretKey sKey = sKR.getSecretKey(revocation);
                if (sKey == null) {
                    log.add(LogLevel.ERROR, LogType.MSG_MR_SUBKEY_MISSING,
                            new String[] { PgpKeyHelper.convertKeyIdToHex(revocation) }, indent+1);
                    return null;
                }
                PGPPublicKey pKey = sKey.getPublicKey();

                // generate and add new signature
                PGPSignature sig = generateRevocationSignature(masterPublicKey, masterPrivateKey, pKey);

                pKey = PGPPublicKey.addCertification(pKey, sig);
                sKR = PGPSecretKeyRing.insertSecretKey(sKR, PGPSecretKey.replacePublicKey(sKey, pKey));
            }

            // 5. Generate and add new subkeys
            for (SaveKeyringParcel.SubkeyAdd add : saveParcel.addSubKeys) {
                try {

                    if (add.mExpiry != null && new Date(add.mExpiry).before(new Date())) {
                        log.add(LogLevel.ERROR, LogType.MSG_MR_SUBKEY_PAST_EXPIRY, indent +1);
                        return null;
                    }

                    log.add(LogLevel.INFO, LogType.MSG_MR_SUBKEY_NEW, indent);
                    PGPSecretKey sKey = createKey(add.mAlgorithm, add.mKeysize, passphrase, false);
                    log.add(LogLevel.DEBUG, LogType.MSG_MR_SUBKEY_NEW_ID,
                            new String[] { PgpKeyHelper.convertKeyIdToHex(sKey.getKeyID()) }, indent+1);

                    PGPPublicKey pKey = sKey.getPublicKey();
                    PGPSignature cert = generateSubkeyBindingSignature(masterPublicKey, masterPrivateKey,
                            sKey, pKey, add.mFlags, add.mExpiry, passphrase);
                    pKey = PGPPublicKey.addCertification(pKey, cert);
                    sKey = PGPSecretKey.replacePublicKey(sKey, pKey);
                    sKR = PGPSecretKeyRing.insertSecretKey(sKR, PGPSecretKey.replacePublicKey(sKey, pKey));
                } catch (PgpGeneralMsgIdException e) {
                    return null;
                }
            }

            // 6. If requested, change passphrase
            if (saveParcel.newPassphrase != null) {
                log.add(LogLevel.INFO, LogType.MSG_MR_PASSPHRASE, indent);
                PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build()
                        .get(HashAlgorithmTags.SHA1);
                PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                        Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(passphrase.toCharArray());
                // Build key encryptor based on new passphrase
                PBESecretKeyEncryptor keyEncryptorNew = new JcePBESecretKeyEncryptorBuilder(
                        PGPEncryptedData.CAST5, sha1Calc)
                        .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(
                                saveParcel.newPassphrase.toCharArray());

                sKR = PGPSecretKeyRing.copyWithNewPassword(sKR, keyDecryptor, keyEncryptorNew);
            }

            // This one must only be thrown by
        } catch (IOException e) {
            log.add(LogLevel.ERROR, LogType.MSG_MR_ERROR_ENCODE, indent+1);
            return null;
        } catch (PGPException e) {
            log.add(LogLevel.ERROR, LogType.MSG_MR_ERROR_PGP, indent+1);
            return null;
        } catch (SignatureException e) {
            log.add(LogLevel.ERROR, LogType.MSG_MR_ERROR_SIG, indent+1);
            return null;
        }

        log.add(LogLevel.OK, LogType.MSG_MR_SUCCESS, indent);
        return new UncachedKeyRing(sKR);

    }

    private static PGPSignature generateUserIdSignature(
            PGPPrivateKey masterPrivateKey, PGPPublicKey pKey, String userId, boolean primary)
            throws IOException, PGPException, SignatureException {
        PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                pKey.getAlgorithm(), PGPUtil.SHA1)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);
        PGPSignatureSubpacketGenerator subHashedPacketsGen = new PGPSignatureSubpacketGenerator();
        subHashedPacketsGen.setSignatureCreationTime(false, new Date());
        subHashedPacketsGen.setPreferredSymmetricAlgorithms(true, PREFERRED_SYMMETRIC_ALGORITHMS);
        subHashedPacketsGen.setPreferredHashAlgorithms(true, PREFERRED_HASH_ALGORITHMS);
        subHashedPacketsGen.setPreferredCompressionAlgorithms(true, PREFERRED_COMPRESSION_ALGORITHMS);
        subHashedPacketsGen.setPrimaryUserID(false, primary);
        sGen.setHashedSubpackets(subHashedPacketsGen.generate());
        sGen.init(PGPSignature.POSITIVE_CERTIFICATION, masterPrivateKey);
        return sGen.generateCertification(userId, pKey);
    }

    private static PGPSignature generateRevocationSignature(
            PGPPrivateKey masterPrivateKey, PGPPublicKey pKey, String userId)
        throws IOException, PGPException, SignatureException {
        PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                pKey.getAlgorithm(), PGPUtil.SHA1)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);
        PGPSignatureSubpacketGenerator subHashedPacketsGen = new PGPSignatureSubpacketGenerator();
        subHashedPacketsGen.setSignatureCreationTime(false, new Date());
        sGen.setHashedSubpackets(subHashedPacketsGen.generate());
        sGen.init(PGPSignature.CERTIFICATION_REVOCATION, masterPrivateKey);
        return sGen.generateCertification(userId, pKey);
    }

    private static PGPSignature generateRevocationSignature(
            PGPPublicKey masterPublicKey, PGPPrivateKey masterPrivateKey, PGPPublicKey pKey)
            throws IOException, PGPException, SignatureException {
        PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                pKey.getAlgorithm(), PGPUtil.SHA1)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);
        PGPSignatureSubpacketGenerator subHashedPacketsGen = new PGPSignatureSubpacketGenerator();
        subHashedPacketsGen.setSignatureCreationTime(false, new Date());
        sGen.setHashedSubpackets(subHashedPacketsGen.generate());
        // Generate key revocation or subkey revocation, depending on master/subkey-ness
        if (masterPublicKey.getKeyID() == pKey.getKeyID()) {
            sGen.init(PGPSignature.KEY_REVOCATION, masterPrivateKey);
            return sGen.generateCertification(masterPublicKey);
        } else {
            sGen.init(PGPSignature.SUBKEY_REVOCATION, masterPrivateKey);
            return sGen.generateCertification(masterPublicKey, pKey);
        }
    }

    private static PGPSignature generateSubkeyBindingSignature(
            PGPPublicKey masterPublicKey, PGPPrivateKey masterPrivateKey,
            PGPSecretKey sKey, PGPPublicKey pKey, int flags, Long expiry, String passphrase)
            throws IOException, PGPException, SignatureException {

        // date for signing
        Date todayDate = new Date();
        PGPSignatureSubpacketGenerator unhashedPacketsGen = new PGPSignatureSubpacketGenerator();

        // If this key can sign, we need a primary key binding signature
        if ((flags & KeyFlags.SIGN_DATA) != 0) {

            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder()
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(
                            passphrase.toCharArray());
            PGPPrivateKey subPrivateKey = sKey.extractPrivateKey(keyDecryptor);

            // cross-certify signing keys
            PGPSignatureSubpacketGenerator subHashedPacketsGen = new PGPSignatureSubpacketGenerator();
            subHashedPacketsGen.setSignatureCreationTime(false, todayDate);
            PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                    pKey.getAlgorithm(), PGPUtil.SHA1)
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
            PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);
            sGen.init(PGPSignature.PRIMARYKEY_BINDING, subPrivateKey);
            sGen.setHashedSubpackets(subHashedPacketsGen.generate());
            PGPSignature certification = sGen.generateCertification(masterPublicKey, pKey);
            unhashedPacketsGen.setEmbeddedSignature(false, certification);
        }

        PGPSignatureSubpacketGenerator hashedPacketsGen;
        {
            hashedPacketsGen = new PGPSignatureSubpacketGenerator();
            hashedPacketsGen.setSignatureCreationTime(false, todayDate);
            hashedPacketsGen.setKeyFlags(false, flags);
        }

        if (expiry != null) {
            Calendar creationDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            creationDate.setTime(pKey.getCreationTime());

            // (Just making sure there's no programming error here, this MUST have been checked above!)
            if (new Date(expiry).before(todayDate)) {
                throw new RuntimeException("Bad subkey creation date, this is a bug!");
            }
            hashedPacketsGen.setKeyExpirationTime(false, expiry - creationDate.getTimeInMillis());
        } else {
            hashedPacketsGen.setKeyExpirationTime(false, 0);
        }

        PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                pKey.getAlgorithm(), PGPUtil.SHA1)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);
        sGen.init(PGPSignature.SUBKEY_BINDING, masterPrivateKey);
        sGen.setHashedSubpackets(hashedPacketsGen.generate());
        sGen.setUnhashedSubpackets(unhashedPacketsGen.generate());

        return sGen.generateCertification(masterPublicKey, pKey);

    }


    /**
     * Certify the given pubkeyid with the given masterkeyid.
     *
     * @param certificationKey Certifying key
     * @param publicKey        public key to certify
     * @param userIds          User IDs to certify, must not be null or empty
     * @param passphrase       Passphrase of the secret key
     * @return A keyring with added certifications
     */
    public PGPPublicKey certifyKey(PGPSecretKey certificationKey, PGPPublicKey publicKey,
                                   List<String> userIds, String passphrase)
            throws PgpGeneralMsgIdException, NoSuchAlgorithmException, NoSuchProviderException,
            PGPException, SignatureException {

        // create a signatureGenerator from the supplied masterKeyId and passphrase
        PGPSignatureGenerator signatureGenerator;
        {

            if (certificationKey == null) {
                throw new PgpGeneralMsgIdException(R.string.error_no_signature_key);
            }

            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                    Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(passphrase.toCharArray());
            PGPPrivateKey signaturePrivateKey = certificationKey.extractPrivateKey(keyDecryptor);
            if (signaturePrivateKey == null) {
                throw new PgpGeneralMsgIdException(R.string.error_could_not_extract_private_key);
            }

            // TODO: SHA256 fixed?
            JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(
                    certificationKey.getPublicKey().getAlgorithm(), PGPUtil.SHA256)
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

            signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
            signatureGenerator.init(PGPSignature.DEFAULT_CERTIFICATION, signaturePrivateKey);
        }

        { // supply signatureGenerator with a SubpacketVector
            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            PGPSignatureSubpacketVector packetVector = spGen.generate();
            signatureGenerator.setHashedSubpackets(packetVector);
        }

        // fetch public key ring, add the certification and return it
        for (String userId : new IterableIterator<String>(userIds.iterator())) {
            PGPSignature sig = signatureGenerator.generateCertification(userId, publicKey);
            publicKey = PGPPublicKey.addCertification(publicKey, userId, sig);
        }

        return publicKey;
    }

}
