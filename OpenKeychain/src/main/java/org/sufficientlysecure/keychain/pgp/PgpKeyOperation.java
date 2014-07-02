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
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.spongycastle.openpgp.operator.PGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.PGPDigestCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
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
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;
import org.sufficientlysecure.keychain.util.Log;
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
    private PGPKeyPair createKey(int algorithmChoice, int keySize) throws PgpGeneralMsgIdException {

        try {
            if (keySize < 512) {
                throw new PgpGeneralMsgIdException(R.string.error_key_size_minimum512bit);
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
            return new JcaPGPKeyPair(algorithm, keyGen.generateKeyPair(), new Date());

        } catch(NoSuchProviderException e) {
            throw new RuntimeException(e);
        } catch(NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch(InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch(PGPException e) {
            throw new PgpGeneralMsgIdException(R.string.msg_mf_error_pgp, e);
        }
    }

    public UncachedKeyRing createSecretKeyRing(SaveKeyringParcel saveParcel, OperationLog log,
                                               int indent) {

        try {

            log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_KEYID, indent);
            indent += 1;
            updateProgress(R.string.progress_building_key, 0, 100);

            if (saveParcel.addSubKeys == null || saveParcel.addSubKeys.isEmpty()) {
                log.add(LogLevel.ERROR, LogType.MSG_CR_ERROR_NO_MASTER, indent);
                return null;
            }

            SubkeyAdd add = saveParcel.addSubKeys.remove(0);
            PGPKeyPair keyPair = createKey(add.mAlgorithm, add.mKeysize);

            if (add.mAlgorithm == Constants.choice.algorithm.elgamal) {
                throw new PgpGeneralMsgIdException(R.string.error_master_key_must_not_be_el_gamal);
            }

            // define hashing and signing algos
            PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder()
                    .build().get(HashAlgorithmTags.SHA1);
            // Build key encrypter and decrypter based on passphrase
            PBESecretKeyEncryptor keyEncryptor = new JcePBESecretKeyEncryptorBuilder(
                    PGPEncryptedData.CAST5, sha1Calc)
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build("".toCharArray());
            PGPSecretKey masterSecretKey = new PGPSecretKey(keyPair.getPrivateKey(), keyPair.getPublicKey(),
                    sha1Calc, true, keyEncryptor);

            PGPSecretKeyRing sKR = new PGPSecretKeyRing(
                    masterSecretKey.getEncoded(), new JcaKeyFingerprintCalculator());

            return internal(sKR, masterSecretKey, saveParcel, "", log, indent);

        } catch (PGPException e) {
            Log.e(Constants.TAG, "pgp error encoding key", e);
            return null;
        } catch (IOException e) {
            Log.e(Constants.TAG, "io error encoding key", e);
            return null;
        } catch (PgpGeneralMsgIdException e) {
            Log.e(Constants.TAG, "pgp msg id error", e);
            return null;
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

        log.add(LogLevel.START, LogType.MSG_MF, indent);
        indent += 1;
        updateProgress(R.string.progress_building_key, 0, 100);

        // Make sure this is called with a proper SaveKeyringParcel
        if (saveParcel.mMasterKeyId == null || saveParcel.mMasterKeyId != wsKR.getMasterKeyId()) {
            log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_KEYID, indent);
            return null;
        }

        // We work on bouncycastle object level here
        PGPSecretKeyRing sKR = wsKR.getRing();
        PGPSecretKey masterSecretKey = sKR.getSecretKey();

        // Make sure the fingerprint matches
        if (saveParcel.mFingerprint == null
                || !Arrays.equals(saveParcel.mFingerprint,
                                    masterSecretKey.getPublicKey().getFingerprint())) {
            log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_FINGERPRINT, indent);
            return null;
        }

        return internal(sKR, masterSecretKey, saveParcel, passphrase, log, indent);

    }

    private UncachedKeyRing internal(PGPSecretKeyRing sKR, PGPSecretKey masterSecretKey,
                                     SaveKeyringParcel saveParcel, String passphrase,
                                     OperationLog log, int indent) {

        updateProgress(R.string.progress_certifying_master_key, 20, 100);

        PGPPublicKey masterPublicKey = masterSecretKey.getPublicKey();

        // 1. Unlock private key
        log.add(LogLevel.DEBUG, LogType.MSG_MF_UNLOCK, indent);
        PGPPrivateKey masterPrivateKey;
        {
            try {
                PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                        Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(passphrase.toCharArray());
                masterPrivateKey = masterSecretKey.extractPrivateKey(keyDecryptor);
            } catch (PGPException e) {
                log.add(LogLevel.ERROR, LogType.MSG_MF_UNLOCK_ERROR, indent + 1);
                return null;
            }
        }

        // work on master secret key
        try {

            PGPPublicKey modifiedPublicKey = masterPublicKey;

            // 2a. Add certificates for new user ids
            for (String userId : saveParcel.addUserIds) {
                log.add(LogLevel.INFO, LogType.MSG_MF_UID_ADD, indent);
                PGPSignature cert = generateUserIdSignature(masterPrivateKey,
                        masterPublicKey, userId, false);
                modifiedPublicKey = PGPPublicKey.addCertification(masterPublicKey, userId, cert);
            }

            // 2b. Add revocations for revoked user ids
            for (String userId : saveParcel.revokeUserIds) {
                log.add(LogLevel.INFO, LogType.MSG_MF_UID_REVOKE, indent);
                PGPSignature cert = generateRevocationSignature(masterPrivateKey,
                        masterPublicKey, userId);
                modifiedPublicKey = PGPPublicKey.addCertification(masterPublicKey, userId, cert);
            }

            // 3. If primary user id changed, generate new certificates for both old and new
            if (saveParcel.changePrimaryUserId != null) {
                log.add(LogLevel.INFO, LogType.MSG_MF_UID_PRIMARY, indent);
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
                log.add(LogLevel.INFO, LogType.MSG_MF_SUBKEY_CHANGE,
                        indent, PgpKeyHelper.convertKeyIdToHex(change.mKeyId));
                PGPSecretKey sKey = sKR.getSecretKey(change.mKeyId);
                if (sKey == null) {
                    log.add(LogLevel.ERROR, LogType.MSG_MF_SUBKEY_MISSING,
                            indent + 1, PgpKeyHelper.convertKeyIdToHex(change.mKeyId));
                    return null;
                }
                PGPPublicKey pKey = sKey.getPublicKey();

                if (change.mExpiry != null && new Date(change.mExpiry).before(new Date())) {
                    log.add(LogLevel.ERROR, LogType.MSG_MF_SUBKEY_PAST_EXPIRY,
                            indent + 1, PgpKeyHelper.convertKeyIdToHex(change.mKeyId));
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
                log.add(LogLevel.INFO, LogType.MSG_MF_SUBKEY_REVOKE,
                        indent, PgpKeyHelper.convertKeyIdToHex(revocation));
                PGPSecretKey sKey = sKR.getSecretKey(revocation);
                if (sKey == null) {
                    log.add(LogLevel.ERROR, LogType.MSG_MF_SUBKEY_MISSING,
                            indent+1, PgpKeyHelper.convertKeyIdToHex(revocation));
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
                        log.add(LogLevel.ERROR, LogType.MSG_MF_SUBKEY_PAST_EXPIRY, indent +1);
                        return null;
                    }

                    log.add(LogLevel.INFO, LogType.MSG_MF_SUBKEY_NEW, indent);

                    // generate a new secret key (privkey only for now)
                    PGPKeyPair keyPair = createKey(add.mAlgorithm, add.mKeysize);

                    // add subkey binding signature (making this a sub rather than master key)
                    PGPPublicKey pKey = keyPair.getPublicKey();
                    PGPSignature cert = generateSubkeyBindingSignature(
                            masterPublicKey, masterPrivateKey, keyPair.getPrivateKey(), pKey,
                            add.mFlags, add.mExpiry);
                    pKey = PGPPublicKey.addSubkeyBindingCertification(pKey, cert);

                    PGPSecretKey sKey; {
                        // define hashing and signing algos
                        PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder()
                                .build().get(HashAlgorithmTags.SHA1);

                        // Build key encrypter and decrypter based on passphrase
                        PBESecretKeyEncryptor keyEncryptor = new JcePBESecretKeyEncryptorBuilder(
                                PGPEncryptedData.CAST5, sha1Calc)
                                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build("".toCharArray());

                        sKey = new PGPSecretKey(keyPair.getPrivateKey(), pKey,
                                sha1Calc, false, keyEncryptor);
                    }

                    log.add(LogLevel.DEBUG, LogType.MSG_MF_SUBKEY_NEW_ID,
                            indent+1, PgpKeyHelper.convertKeyIdToHex(sKey.getKeyID()));

                    sKR = PGPSecretKeyRing.insertSecretKey(sKR, sKey);

                } catch (PgpGeneralMsgIdException e) {
                    return null;
                }
            }

            // 6. If requested, change passphrase
            if (saveParcel.newPassphrase != null) {
                log.add(LogLevel.INFO, LogType.MSG_MF_PASSPHRASE, indent);
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
            log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_ENCODE, indent+1);
            return null;
        } catch (PGPException e) {
            log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_PGP, indent+1);
            return null;
        } catch (SignatureException e) {
            log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_SIG, indent+1);
            return null;
        }

        log.add(LogLevel.OK, LogType.MSG_MF_SUCCESS, indent);
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
        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder()
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(
                        passphrase.toCharArray());
        PGPPrivateKey subPrivateKey = sKey.extractPrivateKey(keyDecryptor);
        return generateSubkeyBindingSignature(masterPublicKey, masterPrivateKey, subPrivateKey,
                pKey, flags, expiry);
    }

    private static PGPSignature generateSubkeyBindingSignature(
            PGPPublicKey masterPublicKey, PGPPrivateKey masterPrivateKey,
            PGPPrivateKey subPrivateKey, PGPPublicKey pKey, int flags, Long expiry)
            throws IOException, PGPException, SignatureException {

        // date for signing
        Date todayDate = new Date();
        PGPSignatureSubpacketGenerator unhashedPacketsGen = new PGPSignatureSubpacketGenerator();

        // If this key can sign, we need a primary key binding signature
        if ((flags & KeyFlags.SIGN_DATA) != 0) {
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

}
