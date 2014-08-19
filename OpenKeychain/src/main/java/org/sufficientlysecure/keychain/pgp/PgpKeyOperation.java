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
import org.spongycastle.bcpg.PublicKeyAlgorithmTags;
import org.spongycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.spongycastle.bcpg.sig.Features;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.jce.spec.ElGamalParameterSpec;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyPair;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
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
import org.sufficientlysecure.keychain.service.OperationResultParcel;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogLevel;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogType;
import org.sufficientlysecure.keychain.service.OperationResultParcel.OperationLog;
import org.sufficientlysecure.keychain.service.OperationResults.EditKeyResult;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Primes;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Stack;

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
    private Stack<Progressable> mProgress;

    // most preferred is first
    private static final int[] PREFERRED_SYMMETRIC_ALGORITHMS = new int[]{
            SymmetricKeyAlgorithmTags.AES_256,
            SymmetricKeyAlgorithmTags.AES_192,
            SymmetricKeyAlgorithmTags.AES_128,
            SymmetricKeyAlgorithmTags.CAST5
    };
    private static final int[] PREFERRED_HASH_ALGORITHMS = new int[]{
            HashAlgorithmTags.SHA512,
            HashAlgorithmTags.SHA384,
            HashAlgorithmTags.SHA224,
            HashAlgorithmTags.SHA256,
            HashAlgorithmTags.RIPEMD160
    };
    private static final int[] PREFERRED_COMPRESSION_ALGORITHMS = new int[]{
            CompressionAlgorithmTags.ZLIB,
            CompressionAlgorithmTags.BZIP2,
            CompressionAlgorithmTags.ZIP
    };

    /*
     * Note: s2kcount is a number between 0 and 0xff that controls the
     * number of times to iterate the password hash before use. More
     * iterations are useful against offline attacks, as it takes more
     * time to check each password. The actual number of iterations is
     * rather complex, and also depends on the hash function in use.
     * Refer to Section 3.7.1.3 in rfc4880.txt. Bigger numbers give
     * you more iterations.  As a rough rule of thumb, when using
     * SHA256 as the hashing function, 0x10 gives you about 64
     * iterations, 0x20 about 128, 0x30 about 256 and so on till 0xf0,
     * or about 1 million iterations. The maximum you can go to is
     * 0xff, or about 2 million iterations.  I'll use 0xc0 as a
     * default -- about 130,000 iterations.
     *
     * http://kbsriram.com/2013/01/generating-rsa-keys-with-bouncycastle.html
     */
    private static final int SECRET_KEY_ENCRYPTOR_HASH_ALGO = HashAlgorithmTags.SHA512;
    private static final int SECRET_KEY_ENCRYPTOR_SYMMETRIC_ALGO = SymmetricKeyAlgorithmTags.AES_256;
    private static final int SECRET_KEY_ENCRYPTOR_S2K_COUNT = 0xc0;

    public PgpKeyOperation(Progressable progress) {
        super();
        if (progress != null) {
            mProgress = new Stack<Progressable>();
            mProgress.push(progress);
        }
    }

    private void subProgressPush(int from, int to) {
        if (mProgress == null) {
            return;
        }
        mProgress.push(new ProgressScaler(mProgress.peek(), from, to, 100));
    }
    private void subProgressPop() {
        if (mProgress == null) {
            return;
        }
        if (mProgress.size() == 1) {
            throw new RuntimeException("Tried to pop progressable without prior push! "
                    + "This is a programming error, please file a bug report.");
        }
        mProgress.pop();
    }

    private void progress(int message, int current) {
        if (mProgress == null) {
            return;
        }
        mProgress.peek().setProgress(message, current, 100);
    }

    /** Creates new secret key. */
    private PGPKeyPair createKey(int algorithmChoice, int keySize, OperationLog log, int indent) {

        try {
            if (keySize < 512) {
                log.add(LogLevel.ERROR, LogType.MSG_CR_ERROR_KEYSIZE_512, indent);
                return null;
            }

            int algorithm;
            KeyPairGenerator keyGen;

            switch (algorithmChoice) {
                case PublicKeyAlgorithmTags.DSA: {
                    progress(R.string.progress_generating_dsa, 30);
                    keyGen = KeyPairGenerator.getInstance("DSA", Constants.BOUNCY_CASTLE_PROVIDER_NAME);
                    keyGen.initialize(keySize, new SecureRandom());
                    algorithm = PGPPublicKey.DSA;
                    break;
                }

                case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT: {
                    progress(R.string.progress_generating_elgamal, 30);
                    keyGen = KeyPairGenerator.getInstance("ElGamal", Constants.BOUNCY_CASTLE_PROVIDER_NAME);
                    BigInteger p = Primes.getBestPrime(keySize);
                    BigInteger g = new BigInteger("2");

                    ElGamalParameterSpec elParams = new ElGamalParameterSpec(p, g);

                    keyGen.initialize(elParams);
                    algorithm = PGPPublicKey.ELGAMAL_ENCRYPT;
                    break;
                }

                case PublicKeyAlgorithmTags.RSA_GENERAL: {
                    progress(R.string.progress_generating_rsa, 30);
                    keyGen = KeyPairGenerator.getInstance("RSA", Constants.BOUNCY_CASTLE_PROVIDER_NAME);
                    keyGen.initialize(keySize, new SecureRandom());

                    algorithm = PGPPublicKey.RSA_GENERAL;
                    break;
                }

                default: {
                    log.add(LogLevel.ERROR, LogType.MSG_CR_ERROR_UNKNOWN_ALGO, indent);
                    return null;
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
            Log.e(Constants.TAG, "internal pgp error", e);
            log.add(LogLevel.ERROR, LogType.MSG_CR_ERROR_INTERNAL_PGP, indent);
            return null;
        }
    }

    public EditKeyResult createSecretKeyRing(SaveKeyringParcel saveParcel) {

        OperationLog log = new OperationLog();
        int indent = 0;

        try {

            log.add(LogLevel.START, LogType.MSG_CR, indent);
            progress(R.string.progress_building_key, 0);
            indent += 1;

            if (saveParcel.mAddSubKeys.isEmpty()) {
                log.add(LogLevel.ERROR, LogType.MSG_CR_ERROR_NO_MASTER, indent);
                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
            }

            if (saveParcel.mAddUserIds.isEmpty()) {
                log.add(LogLevel.ERROR, LogType.MSG_CR_ERROR_NO_USER_ID, indent);
                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
            }

            SubkeyAdd add = saveParcel.mAddSubKeys.remove(0);
            if ((add.mFlags & KeyFlags.CERTIFY_OTHER) != KeyFlags.CERTIFY_OTHER) {
                log.add(LogLevel.ERROR, LogType.MSG_CR_ERROR_NO_CERTIFY, indent);
                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
            }

            if (add.mExpiry == null) {
                log.add(LogLevel.ERROR, LogType.MSG_CR_ERROR_NULL_EXPIRY, indent);
                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
            }

            if (add.mAlgorithm == PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT) {
                log.add(LogLevel.ERROR, LogType.MSG_CR_ERROR_MASTER_ELGAMAL, indent);
                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
            }

            subProgressPush(10, 30);
            PGPKeyPair keyPair = createKey(add.mAlgorithm, add.mKeysize, log, indent);
            subProgressPop();

            // return null if this failed (an error will already have been logged by createKey)
            if (keyPair == null) {
                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
            }

            progress(R.string.progress_building_master_key, 40);

            // Build key encrypter and decrypter based on passphrase
            PGPDigestCalculator encryptorHashCalc = new JcaPGPDigestCalculatorProviderBuilder()
                    .build().get(SECRET_KEY_ENCRYPTOR_HASH_ALGO);
            PBESecretKeyEncryptor keyEncryptor = new JcePBESecretKeyEncryptorBuilder(
                    SECRET_KEY_ENCRYPTOR_SYMMETRIC_ALGO, encryptorHashCalc, SECRET_KEY_ENCRYPTOR_S2K_COUNT)
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build("".toCharArray());

            // NOTE: only SHA1 is supported for key checksum calculations.
            PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder()
                    .build().get(HashAlgorithmTags.SHA1);
            PGPSecretKey masterSecretKey = new PGPSecretKey(keyPair.getPrivateKey(), keyPair.getPublicKey(),
                    sha1Calc, true, keyEncryptor);

            PGPSecretKeyRing sKR = new PGPSecretKeyRing(
                    masterSecretKey.getEncoded(), new JcaKeyFingerprintCalculator());

            subProgressPush(50, 100);
            return internal(sKR, masterSecretKey, add.mFlags, add.mExpiry, saveParcel, "", log);

        } catch (PGPException e) {
            log.add(LogLevel.ERROR, LogType.MSG_CR_ERROR_INTERNAL_PGP, indent);
            Log.e(Constants.TAG, "pgp error encoding key", e);
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        } catch (IOException e) {
            Log.e(Constants.TAG, "io error encoding key", e);
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
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
    public EditKeyResult modifySecretKeyRing(CanonicalizedSecretKeyRing wsKR, SaveKeyringParcel saveParcel,
                                               String passphrase) {

        OperationLog log = new OperationLog();
        int indent = 0;

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

        log.add(LogLevel.START, LogType.MSG_MF, indent,
                PgpKeyHelper.convertKeyIdToHex(wsKR.getMasterKeyId()));
        indent += 1;
        progress(R.string.progress_building_key, 0);

        // Make sure this is called with a proper SaveKeyringParcel
        if (saveParcel.mMasterKeyId == null || saveParcel.mMasterKeyId != wsKR.getMasterKeyId()) {
            log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_KEYID, indent);
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        }

        // We work on bouncycastle object level here
        PGPSecretKeyRing sKR = wsKR.getRing();
        PGPSecretKey masterSecretKey = sKR.getSecretKey();

        // Make sure the fingerprint matches
        if (saveParcel.mFingerprint == null
                || !Arrays.equals(saveParcel.mFingerprint,
                                    masterSecretKey.getPublicKey().getFingerprint())) {
            log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_FINGERPRINT, indent);
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        }

        // read masterKeyFlags, and use the same as before.
        // since this is the master key, this contains at least CERTIFY_OTHER
        PGPPublicKey masterPublicKey = masterSecretKey.getPublicKey();
        int masterKeyFlags = readKeyFlags(masterPublicKey) | KeyFlags.CERTIFY_OTHER;
        long masterKeyExpiry = masterPublicKey.getValidSeconds() == 0L ? 0L :
                masterPublicKey.getCreationTime().getTime() / 1000 + masterPublicKey.getValidSeconds();

        return internal(sKR, masterSecretKey, masterKeyFlags, masterKeyExpiry, saveParcel, passphrase, log);

    }

    private EditKeyResult internal(PGPSecretKeyRing sKR, PGPSecretKey masterSecretKey,
                                     int masterKeyFlags, long masterKeyExpiry,
                                     SaveKeyringParcel saveParcel, String passphrase,
                                     OperationLog log) {

        int indent = 1;

        progress(R.string.progress_modify, 0);

        PGPPublicKey masterPublicKey = masterSecretKey.getPublicKey();

        // 1. Unlock private key
        progress(R.string.progress_modify_unlock, 10);
        log.add(LogLevel.DEBUG, LogType.MSG_MF_UNLOCK, indent);
        PGPPrivateKey masterPrivateKey;
        {
            try {
                PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                        Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(passphrase.toCharArray());
                masterPrivateKey = masterSecretKey.extractPrivateKey(keyDecryptor);
            } catch (PGPException e) {
                log.add(LogLevel.ERROR, LogType.MSG_MF_UNLOCK_ERROR, indent + 1);
                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
            }
        }

        try {

            { // work on master secret key

                PGPPublicKey modifiedPublicKey = masterPublicKey;

                // 2a. Add certificates for new user ids
                subProgressPush(15, 25);
                for (int i = 0; i < saveParcel.mAddUserIds.size(); i++) {

                    progress(R.string.progress_modify_adduid, (i - 1) * (100 / saveParcel.mAddUserIds.size()));
                    String userId = saveParcel.mAddUserIds.get(i);
                    log.add(LogLevel.INFO, LogType.MSG_MF_UID_ADD, indent, userId);

                    if (userId.equals("")) {
                        log.add(LogLevel.ERROR, LogType.MSG_MF_UID_ERROR_EMPTY, indent + 1);
                        return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                    }

                    // this operation supersedes all previous binding and revocation certificates,
                    // so remove those to retain assertions from canonicalization for later operations
                    @SuppressWarnings("unchecked")
                    Iterator<PGPSignature> it = modifiedPublicKey.getSignaturesForID(userId);
                    if (it != null) {
                        for (PGPSignature cert : new IterableIterator<PGPSignature>(it)) {
                            if (cert.getKeyID() != masterPublicKey.getKeyID()) {
                                // foreign certificate?! error error error
                                log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_INTEGRITY, indent);
                                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                            }
                            if (cert.getSignatureType() == PGPSignature.CERTIFICATION_REVOCATION
                                    || cert.getSignatureType() == PGPSignature.NO_CERTIFICATION
                                    || cert.getSignatureType() == PGPSignature.CASUAL_CERTIFICATION
                                    || cert.getSignatureType() == PGPSignature.POSITIVE_CERTIFICATION
                                    || cert.getSignatureType() == PGPSignature.DEFAULT_CERTIFICATION) {
                                modifiedPublicKey = PGPPublicKey.removeCertification(
                                        modifiedPublicKey, userId, cert);
                            }
                        }
                    }

                    // if it's supposed to be primary, we can do that here as well
                    boolean isPrimary = saveParcel.mChangePrimaryUserId != null
                            && userId.equals(saveParcel.mChangePrimaryUserId);
                    // generate and add new certificate
                    PGPSignature cert = generateUserIdSignature(masterPrivateKey,
                            masterPublicKey, userId, isPrimary, masterKeyFlags, masterKeyExpiry);
                    modifiedPublicKey = PGPPublicKey.addCertification(modifiedPublicKey, userId, cert);
                }
                subProgressPop();

                // 2b. Add revocations for revoked user ids
                subProgressPush(25, 40);
                for (int i = 0; i < saveParcel.mRevokeUserIds.size(); i++) {

                    progress(R.string.progress_modify_revokeuid, (i - 1) * (100 / saveParcel.mRevokeUserIds.size()));
                    String userId = saveParcel.mRevokeUserIds.get(i);
                    log.add(LogLevel.INFO, LogType.MSG_MF_UID_REVOKE, indent, userId);

                    // Make sure the user id exists (yes these are 10 LoC in Java!)
                    boolean exists = false;
                    //noinspection unchecked
                    for (String uid : new IterableIterator<String>(modifiedPublicKey.getUserIDs())) {
                        if (userId.equals(uid)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_NOEXIST_REVOKE, indent);
                        return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                    }

                    // a duplicate revocation will be removed during canonicalization, so no need to
                    // take care of that here.
                    PGPSignature cert = generateRevocationSignature(masterPrivateKey,
                            masterPublicKey, userId);
                    modifiedPublicKey = PGPPublicKey.addCertification(modifiedPublicKey, userId, cert);
                }
                subProgressPop();

                // 3. If primary user id changed, generate new certificates for both old and new
                if (saveParcel.mChangePrimaryUserId != null) {
                    progress(R.string.progress_modify_primaryuid, 40);

                    // keep track if we actually changed one
                    boolean ok = false;
                    log.add(LogLevel.INFO, LogType.MSG_MF_UID_PRIMARY, indent);
                    indent += 1;

                    // we work on the modifiedPublicKey here, to respect new or newly revoked uids
                    // noinspection unchecked
                    for (String userId : new IterableIterator<String>(modifiedPublicKey.getUserIDs())) {
                        boolean isRevoked = false;
                        PGPSignature currentCert = null;
                        // noinspection unchecked
                        for (PGPSignature cert : new IterableIterator<PGPSignature>(
                                modifiedPublicKey.getSignaturesForID(userId))) {
                            if (cert.getKeyID() != masterPublicKey.getKeyID()) {
                                // foreign certificate?! error error error
                                log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_INTEGRITY, indent);
                                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                            }
                            // we know from canonicalization that if there is any revocation here, it
                            // is valid and not superseded by a newer certification.
                            if (cert.getSignatureType() == PGPSignature.CERTIFICATION_REVOCATION) {
                                isRevoked = true;
                                continue;
                            }
                            // we know from canonicalization that there is only one binding
                            // certification here, so we can just work with the first one.
                            if (cert.getSignatureType() == PGPSignature.NO_CERTIFICATION ||
                                    cert.getSignatureType() == PGPSignature.CASUAL_CERTIFICATION ||
                                    cert.getSignatureType() == PGPSignature.POSITIVE_CERTIFICATION ||
                                    cert.getSignatureType() == PGPSignature.DEFAULT_CERTIFICATION) {
                                currentCert = cert;
                            }
                        }

                        if (currentCert == null) {
                            // no certificate found?! error error error
                            log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_INTEGRITY, indent);
                            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                        }

                        // we definitely should not update certifications of revoked keys, so just leave it.
                        if (isRevoked) {
                            // revoked user ids cannot be primary!
                            if (userId.equals(saveParcel.mChangePrimaryUserId)) {
                                log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_REVOKED_PRIMARY, indent);
                                return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                            }
                            continue;
                        }

                        // if this is~ the/a primary user id
                        if (currentCert.getHashedSubPackets() != null
                                && currentCert.getHashedSubPackets().isPrimaryUserID()) {
                            // if it's the one we want, just leave it as is
                            if (userId.equals(saveParcel.mChangePrimaryUserId)) {
                                ok = true;
                                continue;
                            }
                            // otherwise, generate new non-primary certification
                            log.add(LogLevel.DEBUG, LogType.MSG_MF_PRIMARY_REPLACE_OLD, indent);
                            modifiedPublicKey = PGPPublicKey.removeCertification(
                                    modifiedPublicKey, userId, currentCert);
                            PGPSignature newCert = generateUserIdSignature(
                                    masterPrivateKey, masterPublicKey, userId, false,
                                    masterKeyFlags, masterKeyExpiry);
                            modifiedPublicKey = PGPPublicKey.addCertification(
                                    modifiedPublicKey, userId, newCert);
                            continue;
                        }

                        // if we are here, this is not currently a primary user id

                        // if it should be
                        if (userId.equals(saveParcel.mChangePrimaryUserId)) {
                            // add shiny new primary user id certificate
                            log.add(LogLevel.DEBUG, LogType.MSG_MF_PRIMARY_NEW, indent);
                            modifiedPublicKey = PGPPublicKey.removeCertification(
                                    modifiedPublicKey, userId, currentCert);
                            PGPSignature newCert = generateUserIdSignature(
                                    masterPrivateKey, masterPublicKey, userId, true,
                                    masterKeyFlags, masterKeyExpiry);
                            modifiedPublicKey = PGPPublicKey.addCertification(
                                    modifiedPublicKey, userId, newCert);
                            ok = true;
                        }

                        // user id is not primary and is not supposed to be - nothing to do here.

                    }

                    indent -= 1;

                    if (!ok) {
                        log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_NOEXIST_PRIMARY, indent);
                        return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                    }
                }

                // Update the secret key ring
                if (modifiedPublicKey != masterPublicKey) {
                    masterSecretKey = PGPSecretKey.replacePublicKey(masterSecretKey, modifiedPublicKey);
                    masterPublicKey = modifiedPublicKey;
                    sKR = PGPSecretKeyRing.insertSecretKey(sKR, masterSecretKey);
                }

            }

            // 4a. For each subkey change, generate new subkey binding certificate
            subProgressPush(50, 60);
            for (int i = 0; i < saveParcel.mChangeSubKeys.size(); i++) {

                progress(R.string.progress_modify_subkeychange, (i-1) * (100 / saveParcel.mChangeSubKeys.size()));
                SaveKeyringParcel.SubkeyChange change = saveParcel.mChangeSubKeys.get(i);
                log.add(LogLevel.INFO, LogType.MSG_MF_SUBKEY_CHANGE,
                        indent, PgpKeyHelper.convertKeyIdToHex(change.mKeyId));

                PGPSecretKey sKey = sKR.getSecretKey(change.mKeyId);
                if (sKey == null) {
                    log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_SUBKEY_MISSING,
                            indent + 1, PgpKeyHelper.convertKeyIdToHex(change.mKeyId));
                    return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                }

                // expiry must not be in the past
                if (change.mExpiry != null && change.mExpiry != 0 &&
                        new Date(change.mExpiry*1000).before(new Date())) {
                    log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_PAST_EXPIRY,
                            indent + 1, PgpKeyHelper.convertKeyIdToHex(change.mKeyId));
                    return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                }

                // if this is the master key, update uid certificates instead
                if (change.mKeyId == masterPublicKey.getKeyID()) {
                    int flags = change.mFlags == null ? masterKeyFlags : change.mFlags;
                    long expiry = change.mExpiry == null ? masterKeyExpiry : change.mExpiry;

                    if ((flags & KeyFlags.CERTIFY_OTHER) != KeyFlags.CERTIFY_OTHER) {
                        log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_NO_CERTIFY, indent + 1);
                        return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                    }

                    PGPPublicKey pKey =
                            updateMasterCertificates(masterPrivateKey, masterPublicKey,
                                    flags, expiry, indent, log);
                    if (pKey == null) {
                        // error log entry has already been added by updateMasterCertificates itself
                        return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                    }
                    masterSecretKey = PGPSecretKey.replacePublicKey(masterSecretKey, pKey);
                    masterPublicKey = pKey;
                    sKR = PGPSecretKeyRing.insertSecretKey(sKR, masterSecretKey);
                    continue;
                }

                // otherwise, continue working on the public key
                PGPPublicKey pKey = sKey.getPublicKey();

                // keep old flags, or replace with new ones
                int flags = change.mFlags == null ? readKeyFlags(pKey) : change.mFlags;
                long expiry;
                if (change.mExpiry == null) {
                    long valid = pKey.getValidSeconds();
                    expiry = valid == 0
                            ? 0
                            : pKey.getCreationTime().getTime() / 1000 + pKey.getValidSeconds();
                } else {
                    expiry = change.mExpiry;
                }

                // drop all old signatures, they will be superseded by the new one
                //noinspection unchecked
                for (PGPSignature sig : new IterableIterator<PGPSignature>(pKey.getSignatures())) {
                    // special case: if there is a revocation, don't use expiry from before
                    if ( (change.mExpiry == null || change.mExpiry == 0L)
                            && sig.getSignatureType() == PGPSignature.SUBKEY_REVOCATION) {
                        expiry = 0;
                    }
                    pKey = PGPPublicKey.removeCertification(pKey, sig);
                }

                // generate and add new signature
                PGPSignature sig = generateSubkeyBindingSignature(masterPublicKey, masterPrivateKey,
                        sKey, pKey, flags, expiry, passphrase);
                pKey = PGPPublicKey.addCertification(pKey, sig);
                sKR = PGPSecretKeyRing.insertSecretKey(sKR, PGPSecretKey.replacePublicKey(sKey, pKey));
            }
            subProgressPop();

            // 4b. For each subkey revocation, generate new subkey revocation certificate
            subProgressPush(60, 70);
            for (int i = 0; i < saveParcel.mRevokeSubKeys.size(); i++) {

                progress(R.string.progress_modify_subkeyrevoke, (i-1) * (100 / saveParcel.mRevokeSubKeys.size()));
                long revocation = saveParcel.mRevokeSubKeys.get(i);
                log.add(LogLevel.INFO, LogType.MSG_MF_SUBKEY_REVOKE,
                        indent, PgpKeyHelper.convertKeyIdToHex(revocation));

                PGPSecretKey sKey = sKR.getSecretKey(revocation);
                if (sKey == null) {
                    log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_SUBKEY_MISSING,
                            indent+1, PgpKeyHelper.convertKeyIdToHex(revocation));
                    return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                }
                PGPPublicKey pKey = sKey.getPublicKey();

                // generate and add new signature
                PGPSignature sig = generateRevocationSignature(masterPublicKey, masterPrivateKey, pKey);

                pKey = PGPPublicKey.addCertification(pKey, sig);
                sKR = PGPSecretKeyRing.insertSecretKey(sKR, PGPSecretKey.replacePublicKey(sKey, pKey));
            }
            subProgressPop();

            // 5. Generate and add new subkeys
            subProgressPush(70, 90);
            for (int i = 0; i < saveParcel.mAddSubKeys.size(); i++) {

                progress(R.string.progress_modify_subkeyadd, (i-1) * (100 / saveParcel.mAddSubKeys.size()));
                SaveKeyringParcel.SubkeyAdd add = saveParcel.mAddSubKeys.get(i);
                log.add(LogLevel.INFO, LogType.MSG_MF_SUBKEY_NEW, indent, add.mKeysize,
                        PgpKeyHelper.getAlgorithmInfo(add.mAlgorithm) );

                if (add.mExpiry == null) {
                    log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_NULL_EXPIRY, indent +1);
                    return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                }

                if (add.mExpiry > 0L && new Date(add.mExpiry*1000).before(new Date())) {
                    log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_PAST_EXPIRY, indent +1);
                    return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                }

                // generate a new secret key (privkey only for now)
                subProgressPush(
                    (i-1) * (100 / saveParcel.mAddSubKeys.size()),
                    i * (100 / saveParcel.mAddSubKeys.size())
                );
                PGPKeyPair keyPair = createKey(add.mAlgorithm, add.mKeysize, log, indent);
                subProgressPop();
                if (keyPair == null) {
                    log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_PGP, indent +1);
                    return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                }

                // add subkey binding signature (making this a sub rather than master key)
                PGPPublicKey pKey = keyPair.getPublicKey();
                PGPSignature cert = generateSubkeyBindingSignature(
                        masterPublicKey, masterPrivateKey, keyPair.getPrivateKey(), pKey,
                        add.mFlags, add.mExpiry);
                pKey = PGPPublicKey.addSubkeyBindingCertification(pKey, cert);

                PGPSecretKey sKey; {
                    // Build key encrypter and decrypter based on passphrase
                    PGPDigestCalculator encryptorHashCalc = new JcaPGPDigestCalculatorProviderBuilder()
                            .build().get(SECRET_KEY_ENCRYPTOR_HASH_ALGO);
                    PBESecretKeyEncryptor keyEncryptor = new JcePBESecretKeyEncryptorBuilder(
                            SECRET_KEY_ENCRYPTOR_SYMMETRIC_ALGO, encryptorHashCalc, SECRET_KEY_ENCRYPTOR_S2K_COUNT)
                            .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(passphrase.toCharArray());

                    // NOTE: only SHA1 is supported for key checksum calculations.
                    PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder()
                            .build().get(HashAlgorithmTags.SHA1);
                    sKey = new PGPSecretKey(keyPair.getPrivateKey(), pKey, sha1Calc, false, keyEncryptor);
                }

                log.add(LogLevel.DEBUG, LogType.MSG_MF_SUBKEY_NEW_ID,
                        indent+1, PgpKeyHelper.convertKeyIdToHex(sKey.getKeyID()));

                sKR = PGPSecretKeyRing.insertSecretKey(sKR, sKey);

            }
            subProgressPop();

            // 6. If requested, change passphrase
            if (saveParcel.mNewPassphrase != null) {
                progress(R.string.progress_modify_passphrase, 90);
                log.add(LogLevel.INFO, LogType.MSG_MF_PASSPHRASE, indent);
                indent += 1;

                PGPDigestCalculator encryptorHashCalc = new JcaPGPDigestCalculatorProviderBuilder().build()
                        .get(SECRET_KEY_ENCRYPTOR_HASH_ALGO);
                PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                        Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(passphrase.toCharArray());
                // Build key encryptor based on new passphrase
                PBESecretKeyEncryptor keyEncryptorNew = new JcePBESecretKeyEncryptorBuilder(
                        SECRET_KEY_ENCRYPTOR_SYMMETRIC_ALGO, encryptorHashCalc, SECRET_KEY_ENCRYPTOR_S2K_COUNT)
                        .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(
                                saveParcel.mNewPassphrase.toCharArray());

                // noinspection unchecked
                for (PGPSecretKey sKey : new IterableIterator<PGPSecretKey>(sKR.getSecretKeys())) {
                    log.add(LogLevel.DEBUG, LogType.MSG_MF_PASSPHRASE_KEY, indent,
                            PgpKeyHelper.convertKeyIdToHex(sKey.getKeyID()));

                    boolean ok = false;

                    try {
                        // try to set new passphrase
                        sKey = PGPSecretKey.copyWithNewPassword(sKey, keyDecryptor, keyEncryptorNew);
                        ok = true;
                    } catch (PGPException e) {

                        // if this is the master key, error!
                        if (sKey.getKeyID() == masterPublicKey.getKeyID()) {
                            log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_PASSPHRASE_MASTER, indent+1);
                            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
                        }

                        // being in here means decrypt failed, likely due to a bad passphrase try
                        // again with an empty passphrase, maybe we can salvage this
                        try {
                            log.add(LogLevel.DEBUG, LogType.MSG_MF_PASSPHRASE_EMPTY_RETRY, indent+1);
                            PBESecretKeyDecryptor emptyDecryptor =
                                new JcePBESecretKeyDecryptorBuilder().setProvider(
                                    Constants.BOUNCY_CASTLE_PROVIDER_NAME).build("".toCharArray());
                            sKey = PGPSecretKey.copyWithNewPassword(sKey, emptyDecryptor, keyEncryptorNew);
                            ok = true;
                        } catch (PGPException e2) {
                            // non-fatal but not ok, handled below
                        }
                    }

                    if (!ok) {
                        // for a subkey, it's merely a warning
                        log.add(LogLevel.WARN, LogType.MSG_MF_PASSPHRASE_FAIL, indent+1,
                                PgpKeyHelper.convertKeyIdToHex(sKey.getKeyID()));
                        continue;
                    }

                    sKR = PGPSecretKeyRing.insertSecretKey(sKR, sKey);

                }

                indent -= 1;
            }

        } catch (IOException e) {
            Log.e(Constants.TAG, "encountered IOException while modifying key", e);
            log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_ENCODE, indent+1);
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        } catch (PGPException e) {
            Log.e(Constants.TAG, "encountered pgp error while modifying key", e);
            log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_PGP, indent+1);
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        } catch (SignatureException e) {
            Log.e(Constants.TAG, "encountered SignatureException while modifying key", e);
            log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_SIG, indent+1);
            return new EditKeyResult(EditKeyResult.RESULT_ERROR, log, null);
        }

        progress(R.string.progress_done, 100);
        log.add(LogLevel.OK, LogType.MSG_MF_SUCCESS, indent);
        return new EditKeyResult(OperationResultParcel.RESULT_OK, log, new UncachedKeyRing(sKR));

    }

    /** Update all (non-revoked) uid signatures with new flags and expiry time. */
    private static PGPPublicKey updateMasterCertificates(
            PGPPrivateKey masterPrivateKey, PGPPublicKey masterPublicKey,
            int flags, long expiry, int indent, OperationLog log)
            throws PGPException, IOException, SignatureException {

        // keep track if we actually changed one
        boolean ok = false;
        log.add(LogLevel.DEBUG, LogType.MSG_MF_MASTER, indent);
        indent += 1;

        PGPPublicKey modifiedPublicKey = masterPublicKey;

        // we work on the modifiedPublicKey here, to respect new or newly revoked uids
        // noinspection unchecked
        for (String userId : new IterableIterator<String>(modifiedPublicKey.getUserIDs())) {
            boolean isRevoked = false;
            PGPSignature currentCert = null;
            // noinspection unchecked
            for (PGPSignature cert : new IterableIterator<PGPSignature>(
                    modifiedPublicKey.getSignaturesForID(userId))) {
                if (cert.getKeyID() != masterPublicKey.getKeyID()) {
                    // foreign certificate?! error error error
                    log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_INTEGRITY, indent);
                    return null;
                }
                // we know from canonicalization that if there is any revocation here, it
                // is valid and not superseded by a newer certification.
                if (cert.getSignatureType() == PGPSignature.CERTIFICATION_REVOCATION) {
                    isRevoked = true;
                    continue;
                }
                // we know from canonicalization that there is only one binding
                // certification here, so we can just work with the first one.
                if (cert.getSignatureType() == PGPSignature.NO_CERTIFICATION ||
                        cert.getSignatureType() == PGPSignature.CASUAL_CERTIFICATION ||
                        cert.getSignatureType() == PGPSignature.POSITIVE_CERTIFICATION ||
                        cert.getSignatureType() == PGPSignature.DEFAULT_CERTIFICATION) {
                    currentCert = cert;
                }
            }

            if (currentCert == null) {
                // no certificate found?! error error error
                log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_INTEGRITY, indent);
                return null;
            }

            // we definitely should not update certifications of revoked keys, so just leave it.
            if (isRevoked) {
                continue;
            }

            // add shiny new user id certificate
            modifiedPublicKey = PGPPublicKey.removeCertification(
                    modifiedPublicKey, userId, currentCert);
            PGPSignature newCert = generateUserIdSignature(
                    masterPrivateKey, masterPublicKey, userId, true, flags, expiry);
            modifiedPublicKey = PGPPublicKey.addCertification(
                    modifiedPublicKey, userId, newCert);
            ok = true;

        }

        if (!ok) {
            // might happen, theoretically, if there is a key with no uid..
            log.add(LogLevel.ERROR, LogType.MSG_MF_ERROR_MASTER_NONE, indent);
            return null;
        }

        return modifiedPublicKey;

    }

    private static PGPSignature generateUserIdSignature(
            PGPPrivateKey masterPrivateKey, PGPPublicKey pKey, String userId, boolean primary,
            int flags, long expiry)
            throws IOException, PGPException, SignatureException {
        PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                masterPrivateKey.getPublicKeyPacket().getAlgorithm(), HashAlgorithmTags.SHA512)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);

        PGPSignatureSubpacketGenerator hashedPacketsGen = new PGPSignatureSubpacketGenerator();
        {
            hashedPacketsGen.setSignatureCreationTime(true, new Date());
            hashedPacketsGen.setPreferredSymmetricAlgorithms(false, PREFERRED_SYMMETRIC_ALGORITHMS);
            hashedPacketsGen.setPreferredHashAlgorithms(false, PREFERRED_HASH_ALGORITHMS);
            hashedPacketsGen.setPreferredCompressionAlgorithms(false, PREFERRED_COMPRESSION_ALGORITHMS);
            // Request that senders add the MDC to the message (authenticate unsigned messages)
            hashedPacketsGen.setFeature(true, Features.FEATURE_MODIFICATION_DETECTION);
            hashedPacketsGen.setPrimaryUserID(false, primary);
            hashedPacketsGen.setKeyFlags(true, flags);
            if (expiry > 0) {
                hashedPacketsGen.setKeyExpirationTime(
                        true, expiry - pKey.getCreationTime().getTime() / 1000);
            }
        }

        sGen.setHashedSubpackets(hashedPacketsGen.generate());
        sGen.init(PGPSignature.POSITIVE_CERTIFICATION, masterPrivateKey);
        return sGen.generateCertification(userId, pKey);
    }

    private static PGPSignature generateRevocationSignature(
            PGPPrivateKey masterPrivateKey, PGPPublicKey pKey, String userId)
        throws IOException, PGPException, SignatureException {
        PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                masterPrivateKey.getPublicKeyPacket().getAlgorithm(), HashAlgorithmTags.SHA512)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);
        PGPSignatureSubpacketGenerator subHashedPacketsGen = new PGPSignatureSubpacketGenerator();
        subHashedPacketsGen.setSignatureCreationTime(true, new Date());
        sGen.setHashedSubpackets(subHashedPacketsGen.generate());
        sGen.init(PGPSignature.CERTIFICATION_REVOCATION, masterPrivateKey);
        return sGen.generateCertification(userId, pKey);
    }

    private static PGPSignature generateRevocationSignature(
            PGPPublicKey masterPublicKey, PGPPrivateKey masterPrivateKey, PGPPublicKey pKey)
            throws IOException, PGPException, SignatureException {
        PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                masterPublicKey.getAlgorithm(), HashAlgorithmTags.SHA512)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);
        PGPSignatureSubpacketGenerator subHashedPacketsGen = new PGPSignatureSubpacketGenerator();
        subHashedPacketsGen.setSignatureCreationTime(true, new Date());
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
            PGPSecretKey sKey, PGPPublicKey pKey, int flags, long expiry, String passphrase)
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
            PGPPrivateKey subPrivateKey, PGPPublicKey pKey, int flags, long expiry)
            throws IOException, PGPException, SignatureException {

        // date for signing
        Date creationTime = new Date();

        PGPSignatureSubpacketGenerator unhashedPacketsGen = new PGPSignatureSubpacketGenerator();

        // If this key can sign, we need a primary key binding signature
        if ((flags & KeyFlags.SIGN_DATA) > 0) {
            // cross-certify signing keys
            PGPSignatureSubpacketGenerator subHashedPacketsGen = new PGPSignatureSubpacketGenerator();
            subHashedPacketsGen.setSignatureCreationTime(false, creationTime);
            PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                    pKey.getAlgorithm(), HashAlgorithmTags.SHA512)
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
            PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);
            sGen.init(PGPSignature.PRIMARYKEY_BINDING, subPrivateKey);
            sGen.setHashedSubpackets(subHashedPacketsGen.generate());
            PGPSignature certification = sGen.generateCertification(masterPublicKey, pKey);
            unhashedPacketsGen.setEmbeddedSignature(true, certification);
        }

        PGPSignatureSubpacketGenerator hashedPacketsGen;
        {
            hashedPacketsGen = new PGPSignatureSubpacketGenerator();
            hashedPacketsGen.setSignatureCreationTime(true, creationTime);
            hashedPacketsGen.setKeyFlags(true, flags);
            if (expiry > 0) {
                hashedPacketsGen.setKeyExpirationTime(true,
                        expiry - pKey.getCreationTime().getTime() / 1000);
            }
        }

        PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                masterPublicKey.getAlgorithm(), HashAlgorithmTags.SHA512)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);
        sGen.init(PGPSignature.SUBKEY_BINDING, masterPrivateKey);
        sGen.setHashedSubpackets(hashedPacketsGen.generate());
        sGen.setUnhashedSubpackets(unhashedPacketsGen.generate());

        return sGen.generateCertification(masterPublicKey, pKey);

    }

    /** Returns all flags valid for this key.
     *
     * This method does not do any validity checks on the signature, so it should not be used on
     * a non-canonicalized key!
     *
     */
    private static int readKeyFlags(PGPPublicKey key) {
        int flags = 0;
        //noinspection unchecked
        for(PGPSignature sig : new IterableIterator<PGPSignature>(key.getSignatures())) {
            if (sig.getHashedSubPackets() == null) {
                continue;
            }
            flags |= sig.getHashedSubPackets().getKeyFlags();
        }
        return flags;
    }

}
