/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg.helper;

import org.spongycastle.bcpg.ArmoredInputStream;
import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.BCPGOutputStream;
import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ElGamalParameterSpec;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPEncryptedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyPair;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPKeyRingGenerator;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPOnePassSignature;
import org.spongycastle.openpgp.PGPOnePassSignatureList;
import org.spongycastle.openpgp.PGPPBEEncryptedData;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.PGPV3SignatureGenerator;
import org.spongycastle.openpgp.operator.PBEDataDecryptorFactory;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.spongycastle.openpgp.operator.PGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.PGPDigestCalculator;
import org.spongycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.spongycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.spongycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBEKeyEncryptionMethodGenerator;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.thialfihar.android.apg.provider.ProviderHelper;
import org.thialfihar.android.apg.service.ApgIntentService;
import org.thialfihar.android.apg.util.HkpKeyServer;
import org.thialfihar.android.apg.util.InputData;
import org.thialfihar.android.apg.util.PositionAwareInputStream;
import org.thialfihar.android.apg.util.Primes;
import org.thialfihar.android.apg.util.ProgressDialogUpdater;
import org.thialfihar.android.apg.util.KeyServer.AddKeyException;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import org.thialfihar.android.apg.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * TODO:
 * 
 * - Separate this file into different helpers
 * 
 */
public class PGPMain {

    static {
        // register spongy castle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    // Not BC due to the use of Spongy Castle for Android
    public static final String BOUNCY_CASTLE_PROVIDER_NAME = "SC";

    private static final int[] PREFERRED_SYMMETRIC_ALGORITHMS = new int[] {
            SymmetricKeyAlgorithmTags.AES_256, SymmetricKeyAlgorithmTags.AES_192,
            SymmetricKeyAlgorithmTags.AES_128, SymmetricKeyAlgorithmTags.CAST5,
            SymmetricKeyAlgorithmTags.TRIPLE_DES };
    private static final int[] PREFERRED_HASH_ALGORITHMS = new int[] { HashAlgorithmTags.SHA1,
            HashAlgorithmTags.SHA256, HashAlgorithmTags.RIPEMD160 };
    private static final int[] PREFERRED_COMPRESSION_ALGORITHMS = new int[] {
            CompressionAlgorithmTags.ZLIB, CompressionAlgorithmTags.BZIP2,
            CompressionAlgorithmTags.ZIP };

    public static Pattern PGP_MESSAGE = Pattern.compile(
            ".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*", Pattern.DOTALL);

    public static Pattern PGP_SIGNED_MESSAGE = Pattern
            .compile(
                    ".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
                    Pattern.DOTALL);

    public static Pattern PGP_PUBLIC_KEY = Pattern.compile(
            ".*?(-----BEGIN PGP PUBLIC KEY BLOCK-----.*?-----END PGP PUBLIC KEY BLOCK-----).*",
            Pattern.DOTALL);

    private static String mEditPassPhrase = null;

    public static class ApgGeneralException extends Exception {
        static final long serialVersionUID = 0xf812773342L;

        public ApgGeneralException(String message) {
            super(message);
        }
    }

    public static class NoAsymmetricEncryptionException extends Exception {
        static final long serialVersionUID = 0xf812773343L;

        public NoAsymmetricEncryptionException() {
            super();
        }
    }

    public static void setEditPassPhrase(String passPhrase) {
        mEditPassPhrase = passPhrase;
    }

    public static String getEditPassPhrase() {
        return mEditPassPhrase;
    }

    public static void updateProgress(ProgressDialogUpdater progress, int message, int current,
            int total) {
        if (progress != null) {
            progress.setProgress(message, current, total);
        }
    }

    public static void updateProgress(ProgressDialogUpdater progress, int current, int total) {
        if (progress != null) {
            progress.setProgress(current, total);
        }
    }

    /**
     * Creates new secret key. The returned PGPSecretKeyRing contains only one newly generated key
     * when this key is the new masterkey. If a masterkey is supplied in the parameters
     * PGPSecretKeyRing contains the masterkey and the new key as a subkey (certified by the
     * masterkey).
     * 
     * @param context
     * @param algorithmChoice
     * @param keySize
     * @param passPhrase
     * @param masterSecretKey
     * @return
     * @throws NoSuchAlgorithmException
     * @throws PGPException
     * @throws NoSuchProviderException
     * @throws ApgGeneralException
     * @throws InvalidAlgorithmParameterException
     */
    public static PGPSecretKeyRing createKey(Context context, int algorithmChoice, int keySize,
            String passPhrase, PGPSecretKey masterSecretKey) throws NoSuchAlgorithmException,
            PGPException, NoSuchProviderException, ApgGeneralException,
            InvalidAlgorithmParameterException {

        if (keySize < 512) {
            throw new ApgGeneralException(context.getString(R.string.error_keySizeMinimum512bit));
        }

        if (passPhrase == null) {
            passPhrase = "";
        }

        int algorithm = 0;
        KeyPairGenerator keyGen = null;

        switch (algorithmChoice) {
        case Id.choice.algorithm.dsa: {
            keyGen = KeyPairGenerator.getInstance("DSA", BOUNCY_CASTLE_PROVIDER_NAME);
            keyGen.initialize(keySize, new SecureRandom());
            algorithm = PGPPublicKey.DSA;
            break;
        }

        case Id.choice.algorithm.elgamal: {
            if (masterSecretKey == null) {
                throw new ApgGeneralException(
                        context.getString(R.string.error_masterKeyMustNotBeElGamal));
            }
            keyGen = KeyPairGenerator.getInstance("ElGamal", BOUNCY_CASTLE_PROVIDER_NAME);
            BigInteger p = Primes.getBestPrime(keySize);
            BigInteger g = new BigInteger("2");

            ElGamalParameterSpec elParams = new ElGamalParameterSpec(p, g);

            keyGen.initialize(elParams);
            algorithm = PGPPublicKey.ELGAMAL_ENCRYPT;
            break;
        }

        case Id.choice.algorithm.rsa: {
            keyGen = KeyPairGenerator.getInstance("RSA", BOUNCY_CASTLE_PROVIDER_NAME);
            keyGen.initialize(keySize, new SecureRandom());

            algorithm = PGPPublicKey.RSA_GENERAL;
            break;
        }

        default: {
            throw new ApgGeneralException(context.getString(R.string.error_unknownAlgorithmChoice));
        }
        }

        // build new key pair
        PGPKeyPair keyPair = new JcaPGPKeyPair(algorithm, keyGen.generateKeyPair(), new Date());

        // define hashing and signing algos
        PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(
                HashAlgorithmTags.SHA1);

        // Build key encrypter and decrypter based on passphrase
        PBESecretKeyEncryptor keyEncryptor = new JcePBESecretKeyEncryptorBuilder(
                PGPEncryptedData.CAST5, sha1Calc).setProvider(BOUNCY_CASTLE_PROVIDER_NAME).build(
                passPhrase.toCharArray());
        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                BOUNCY_CASTLE_PROVIDER_NAME).build(passPhrase.toCharArray());

        PGPKeyRingGenerator ringGen = null;
        PGPContentSignerBuilder certificationSignerBuilder = null;
        if (masterSecretKey == null) {
            certificationSignerBuilder = new JcaPGPContentSignerBuilder(keyPair.getPublicKey()
                    .getAlgorithm(), HashAlgorithmTags.SHA1);

            // build keyRing with only this one master key in it!
            ringGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, keyPair, "",
                    sha1Calc, null, null, certificationSignerBuilder, keyEncryptor);
        } else {
            PGPPublicKey masterPublicKey = masterSecretKey.getPublicKey();
            PGPPrivateKey masterPrivateKey = masterSecretKey.extractPrivateKey(keyDecryptor);
            PGPKeyPair masterKeyPair = new PGPKeyPair(masterPublicKey, masterPrivateKey);

            certificationSignerBuilder = new JcaPGPContentSignerBuilder(masterKeyPair
                    .getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1);

            // build keyRing with master key and new key as subkey (certified by masterkey)
            ringGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, masterKeyPair,
                    "", sha1Calc, null, null, certificationSignerBuilder, keyEncryptor);

            ringGen.addSubKey(keyPair);
        }

        PGPSecretKeyRing secKeyRing = ringGen.generateSecretKeyRing();

        return secKeyRing;
    }

    public static void buildSecretKey(Context context, ArrayList<String> userIds,
            ArrayList<PGPSecretKey> keys, ArrayList<Integer> keysUsages, long masterKeyId,
            String oldPassPhrase, String newPassPhrase, ProgressDialogUpdater progress)
            throws ApgGeneralException, NoSuchProviderException, PGPException,
            NoSuchAlgorithmException, SignatureException, IOException {

        Log.d(Constants.TAG, "userIds: " + userIds.toString());

        updateProgress(progress, R.string.progress_buildingKey, 0, 100);

        if (oldPassPhrase == null) {
            oldPassPhrase = "";
        }
        if (newPassPhrase == null) {
            newPassPhrase = "";
        }

        updateProgress(progress, R.string.progress_preparingMasterKey, 10, 100);

        int usageId = keysUsages.get(0);
        boolean canSign = (usageId == Id.choice.usage.sign_only || usageId == Id.choice.usage.sign_and_encrypt);
        boolean canEncrypt = (usageId == Id.choice.usage.encrypt_only || usageId == Id.choice.usage.sign_and_encrypt);

        String mainUserId = userIds.get(0);

        PGPSecretKey masterKey = keys.get(0);

        // this removes all userIds and certifications previously attached to the masterPublicKey
        PGPPublicKey tmpKey = masterKey.getPublicKey();
        PGPPublicKey masterPublicKey = new PGPPublicKey(tmpKey.getAlgorithm(),
                tmpKey.getKey(new BouncyCastleProvider()), tmpKey.getCreationTime());

        // already done by code above:
        // PGPPublicKey masterPublicKey = masterKey.getPublicKey();
        // // Somehow, the PGPPublicKey already has an empty certification attached to it when the
        // // keyRing is generated the first time, we remove that when it exists, before adding the
        // new
        // // ones
        // PGPPublicKey masterPublicKeyRmCert = PGPPublicKey.removeCertification(masterPublicKey,
        // "");
        // if (masterPublicKeyRmCert != null) {
        // masterPublicKey = masterPublicKeyRmCert;
        // }

        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                BOUNCY_CASTLE_PROVIDER_NAME).build(oldPassPhrase.toCharArray());
        PGPPrivateKey masterPrivateKey = masterKey.extractPrivateKey(keyDecryptor);

        updateProgress(progress, R.string.progress_certifyingMasterKey, 20, 100);

        for (String userId : userIds) {
            PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                    masterPublicKey.getAlgorithm(), HashAlgorithmTags.SHA1)
                    .setProvider(BOUNCY_CASTLE_PROVIDER_NAME);
            PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);

            sGen.init(PGPSignature.POSITIVE_CERTIFICATION, masterPrivateKey);

            PGPSignature certification = sGen.generateCertification(userId, masterPublicKey);

            masterPublicKey = PGPPublicKey.addCertification(masterPublicKey, userId, certification);
        }

        // TODO: cross-certify the master key with every sub key (APG 1)

        PGPKeyPair masterKeyPair = new PGPKeyPair(masterPublicKey, masterPrivateKey);

        PGPSignatureSubpacketGenerator hashedPacketsGen = new PGPSignatureSubpacketGenerator();
        PGPSignatureSubpacketGenerator unhashedPacketsGen = new PGPSignatureSubpacketGenerator();

        int keyFlags = KeyFlags.CERTIFY_OTHER | KeyFlags.SIGN_DATA;
        if (canEncrypt) {
            keyFlags |= KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE;
        }
        hashedPacketsGen.setKeyFlags(true, keyFlags);

        hashedPacketsGen.setPreferredSymmetricAlgorithms(true, PREFERRED_SYMMETRIC_ALGORITHMS);
        hashedPacketsGen.setPreferredHashAlgorithms(true, PREFERRED_HASH_ALGORITHMS);
        hashedPacketsGen.setPreferredCompressionAlgorithms(true, PREFERRED_COMPRESSION_ALGORITHMS);

        // TODO: this doesn't work quite right yet (APG 1)
        // if (keyEditor.getExpiryDate() != null) {
        // GregorianCalendar creationDate = new GregorianCalendar();
        // creationDate.setTime(getCreationDate(masterKey));
        // GregorianCalendar expiryDate = keyEditor.getExpiryDate();
        // long numDays = Utils.getNumDaysBetween(creationDate, expiryDate);
        // if (numDays <= 0) {
        // throw new GeneralException(
        // context.getString(R.string.error_expiryMustComeAfterCreation));
        // }
        // hashedPacketsGen.setKeyExpirationTime(true, numDays * 86400);
        // }

        updateProgress(progress, R.string.progress_buildingMasterKeyRing, 30, 100);

        // define hashing and signing algos
        PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(
                HashAlgorithmTags.SHA1);
        PGPContentSignerBuilder certificationSignerBuilder = new JcaPGPContentSignerBuilder(
                masterKeyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1);

        // Build key encrypter based on passphrase
        PBESecretKeyEncryptor keyEncryptor = new JcePBESecretKeyEncryptorBuilder(
                PGPEncryptedData.CAST5, sha1Calc).setProvider(BOUNCY_CASTLE_PROVIDER_NAME).build(
                newPassPhrase.toCharArray());

        PGPKeyRingGenerator keyGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION,
                masterKeyPair, mainUserId, sha1Calc, hashedPacketsGen.generate(),
                unhashedPacketsGen.generate(), certificationSignerBuilder, keyEncryptor);

        updateProgress(progress, R.string.progress_addingSubKeys, 40, 100);

        for (int i = 1; i < keys.size(); ++i) {
            updateProgress(progress, 40 + 50 * (i - 1) / (keys.size() - 1), 100);

            PGPSecretKey subKey = keys.get(i);
            PGPPublicKey subPublicKey = subKey.getPublicKey();

            PBESecretKeyDecryptor keyDecryptor2 = new JcePBESecretKeyDecryptorBuilder()
                    .setProvider(BOUNCY_CASTLE_PROVIDER_NAME).build(oldPassPhrase.toCharArray());
            PGPPrivateKey subPrivateKey = subKey.extractPrivateKey(keyDecryptor2);

            // TODO: now used without algorithm and creation time?! (APG 1)
            PGPKeyPair subKeyPair = new PGPKeyPair(subPublicKey, subPrivateKey);

            hashedPacketsGen = new PGPSignatureSubpacketGenerator();
            unhashedPacketsGen = new PGPSignatureSubpacketGenerator();

            keyFlags = 0;

            usageId = keysUsages.get(i);
            canSign = (usageId == Id.choice.usage.sign_only || usageId == Id.choice.usage.sign_and_encrypt);
            canEncrypt = (usageId == Id.choice.usage.encrypt_only || usageId == Id.choice.usage.sign_and_encrypt);
            if (canSign) {
                keyFlags |= KeyFlags.SIGN_DATA;
            }
            if (canEncrypt) {
                keyFlags |= KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE;
            }
            hashedPacketsGen.setKeyFlags(true, keyFlags);

            // TODO: this doesn't work quite right yet (APG 1)
            // if (keyEditor.getExpiryDate() != null) {
            // GregorianCalendar creationDate = new GregorianCalendar();
            // creationDate.setTime(getCreationDate(masterKey));
            // GregorianCalendar expiryDate = keyEditor.getExpiryDate();
            // long numDays = Utils.getNumDaysBetween(creationDate, expiryDate);
            // if (numDays <= 0) {
            // throw new GeneralException(
            // context.getString(R.string.error_expiryMustComeAfterCreation));
            // }
            // hashedPacketsGen.setKeyExpirationTime(true, numDays * 86400);
            // }

            keyGen.addSubKey(subKeyPair, hashedPacketsGen.generate(), unhashedPacketsGen.generate());
        }

        PGPSecretKeyRing secretKeyRing = keyGen.generateSecretKeyRing();
        PGPPublicKeyRing publicKeyRing = keyGen.generatePublicKeyRing();

        updateProgress(progress, R.string.progress_savingKeyRing, 90, 100);

        ProviderHelper.saveKeyRing(context, secretKeyRing);
        ProviderHelper.saveKeyRing(context, publicKeyRing);

        updateProgress(progress, R.string.progress_done, 100, 100);
    }

    public static int storeKeyRingInCache(Context context, PGPKeyRing keyring) {
        int status = Integer.MIN_VALUE; // out of bounds value (Id.retrun_value.*)
        try {
            if (keyring instanceof PGPSecretKeyRing) {
                PGPSecretKeyRing secretKeyRing = (PGPSecretKeyRing) keyring;
                boolean save = true;
                try {
                    PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder()
                            .setProvider(BOUNCY_CASTLE_PROVIDER_NAME).build(new char[] {});
                    PGPPrivateKey testKey = secretKeyRing.getSecretKey().extractPrivateKey(
                            keyDecryptor);
                    if (testKey == null) {
                        // this is bad, something is very wrong... likely a --export-secret-subkeys
                        // export
                        save = false;
                        status = Id.return_value.bad;
                    }
                } catch (PGPException e) {
                    // all good if this fails, we likely didn't use the right password
                }

                if (save) {
                    ProviderHelper.saveKeyRing(context, secretKeyRing);
                    // TODO: remove status returns, use exceptions!
                    status = Id.return_value.ok;
                }
            } else if (keyring instanceof PGPPublicKeyRing) {
                PGPPublicKeyRing publicKeyRing = (PGPPublicKeyRing) keyring;
                ProviderHelper.saveKeyRing(context, publicKeyRing);
                // TODO: remove status returns, use exceptions!
                status = Id.return_value.ok;
            }
        } catch (IOException e) {
            status = Id.return_value.error;
        }

        return status;
    }

    public static boolean uploadKeyRingToServer(HkpKeyServer server, PGPPublicKeyRing keyring) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ArmoredOutputStream aos = new ArmoredOutputStream(bos);
        try {
            aos.write(keyring.getEncoded());
            aos.close();

            String armouredKey = bos.toString("UTF-8");
            server.add(armouredKey);

            return true;
        } catch (IOException e) {
            return false;
        } catch (AddKeyException e) {
            // TODO: tell the user?
            return false;
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
            }
        }
    }

    public static Bundle importKeyRings(Context context, int type, InputData data,
            ProgressDialogUpdater progress) throws ApgGeneralException, FileNotFoundException,
            PGPException, IOException {
        Bundle returnData = new Bundle();

        if (type == Id.type.secret_key) {
            if (progress != null)
                progress.setProgress(R.string.progress_importingSecretKeys, 0, 100);
        } else {
            if (progress != null)
                progress.setProgress(R.string.progress_importingPublicKeys, 0, 100);
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new ApgGeneralException(context.getString(R.string.error_externalStorageNotReady));
        }

        PositionAwareInputStream progressIn = new PositionAwareInputStream(data.getInputStream());
        // need to have access to the bufferedInput, so we can reuse it for the possible
        // PGPObject chunks after the first one, e.g. files with several consecutive ASCII
        // armour blocks
        BufferedInputStream bufferedInput = new BufferedInputStream(progressIn);
        int newKeys = 0;
        int oldKeys = 0;
        int badKeys = 0;
        try {
            PGPKeyRing keyring = PGPHelper.decodeKeyRing(bufferedInput);
            while (keyring != null) {
                int status = Integer.MIN_VALUE; // out of bounds value

                // if this key is what we expect it to be, save it
                if ((type == Id.type.secret_key && keyring instanceof PGPSecretKeyRing)
                        || (type == Id.type.public_key && keyring instanceof PGPPublicKeyRing)) {
                    status = storeKeyRingInCache(context, keyring);
                }

                if (status == Id.return_value.error) {
                    throw new ApgGeneralException(context.getString(R.string.error_savingKeys));
                }

                // update the counts to display to the user at the end
                if (status == Id.return_value.updated) {
                    ++oldKeys;
                } else if (status == Id.return_value.ok) {
                    ++newKeys;
                } else if (status == Id.return_value.bad) {
                    ++badKeys;
                }

                updateProgress(progress, (int) (100 * progressIn.position() / data.getSize()), 100);

                // TODO: needed?
                // obj = objectFactory.nextObject();

                keyring = PGPHelper.decodeKeyRing(bufferedInput);
            }
        } catch (EOFException e) {
            // nothing to do, we are done
        }

        returnData.putInt(ApgIntentService.RESULT_IMPORT_ADDED, newKeys);
        returnData.putInt(ApgIntentService.RESULT_IMPORT_UPDATED, oldKeys);
        returnData.putInt(ApgIntentService.RESULT_IMPORT_BAD, badKeys);

        updateProgress(progress, R.string.progress_done, 100, 100);

        return returnData;
    }

    public static Bundle exportKeyRings(Context context, Vector<Integer> keyRingIds,
            OutputStream outStream, ProgressDialogUpdater progress) throws ApgGeneralException,
            FileNotFoundException, PGPException, IOException {
        Bundle returnData = new Bundle();

        if (keyRingIds.size() == 1) {
            updateProgress(progress, R.string.progress_exportingKey, 0, 100);
        } else {
            updateProgress(progress, R.string.progress_exportingKeys, 0, 100);
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new ApgGeneralException(context.getString(R.string.error_externalStorageNotReady));
        }
        ArmoredOutputStream out = new ArmoredOutputStream(outStream);
        out.setHeader("Version", getFullVersion(context));

        int numKeys = 0;
        for (int i = 0; i < keyRingIds.size(); ++i) {
            updateProgress(progress, i * 100 / keyRingIds.size(), 100);

            // try to get it as a PGPPublicKeyRing, if that fails try to get it as a SecretKeyRing
            PGPPublicKeyRing publicKeyRing = ProviderHelper.getPGPPublicKeyRingByMasterKeyId(
                    context, keyRingIds.get(i));
            if (publicKeyRing != null) {
                publicKeyRing.encode(out);
            } else {
                PGPSecretKeyRing secretKeyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(
                        context, keyRingIds.get(i));
                if (secretKeyRing != null) {
                    secretKeyRing.encode(out);
                } else {
                    continue;
                }
            }
            ++numKeys;
        }
        out.close();
        returnData.putInt(ApgIntentService.RESULT_EXPORT, numKeys);

        updateProgress(progress, R.string.progress_done, 100, 100);

        return returnData;
    }

    /**
     * Encrypt and Sign data
     * 
     * @param context
     * @param progress
     * @param data
     * @param outStream
     * @param useAsciiArmor
     * @param compression
     * @param encryptionKeyIds
     * @param symmetricEncryptionAlgorithm
     * @param encryptionPassphrase
     * @param signatureKeyId
     * @param signatureHashAlgorithm
     * @param signatureForceV3
     * @param signaturePassphrase
     * @throws IOException
     * @throws ApgGeneralException
     * @throws PGPException
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     */
    public static void encryptAndSign(Context context, ProgressDialogUpdater progress,
            InputData data, OutputStream outStream, boolean useAsciiArmor, int compression,
            long encryptionKeyIds[], String encryptionPassphrase, int symmetricEncryptionAlgorithm,
            long signatureKeyId, int signatureHashAlgorithm, boolean signatureForceV3,
            String signaturePassphrase) throws IOException, ApgGeneralException, PGPException,
            NoSuchProviderException, NoSuchAlgorithmException, SignatureException {

        if (encryptionKeyIds == null) {
            encryptionKeyIds = new long[0];
        }

        ArmoredOutputStream armorOut = null;
        OutputStream out = null;
        OutputStream encryptOut = null;
        if (useAsciiArmor) {
            armorOut = new ArmoredOutputStream(outStream);
            armorOut.setHeader("Version", getFullVersion(context));
            out = armorOut;
        } else {
            out = outStream;
        }
        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;

        if (encryptionKeyIds.length == 0 && encryptionPassphrase == null) {
            throw new ApgGeneralException(
                    context.getString(R.string.error_noEncryptionKeysOrPassPhrase));
        }

        if (signatureKeyId != Id.key.none) {
            signingKeyRing = ProviderHelper.getPGPSecretKeyRingByKeyId(context, signatureKeyId);
            signingKey = PGPHelper.getSigningKey(context, signatureKeyId);
            if (signingKey == null) {
                throw new ApgGeneralException(context.getString(R.string.error_signatureFailed));
            }

            if (signaturePassphrase == null) {
                throw new ApgGeneralException(
                        context.getString(R.string.error_noSignaturePassPhrase));
            }

            updateProgress(progress, R.string.progress_extractingSignatureKey, 0, 100);

            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                    BOUNCY_CASTLE_PROVIDER_NAME).build(signaturePassphrase.toCharArray());
            signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
            if (signaturePrivateKey == null) {
                throw new ApgGeneralException(
                        context.getString(R.string.error_couldNotExtractPrivateKey));
            }
        }
        updateProgress(progress, R.string.progress_preparingStreams, 5, 100);

        // encrypt and compress input file content
        JcePGPDataEncryptorBuilder encryptorBuilder = new JcePGPDataEncryptorBuilder(
                symmetricEncryptionAlgorithm).setProvider(BOUNCY_CASTLE_PROVIDER_NAME)
                .setWithIntegrityPacket(true);

        PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(encryptorBuilder);

        if (encryptionKeyIds.length == 0) {
            // Symmetric encryption
            Log.d(Constants.TAG, "encryptionKeyIds length is 0 -> symmetric encryption");

            JcePBEKeyEncryptionMethodGenerator symmetricEncryptionGenerator = new JcePBEKeyEncryptionMethodGenerator(
                    encryptionPassphrase.toCharArray());
            cPk.addMethod(symmetricEncryptionGenerator);
        } else {
            // Asymmetric encryption
            for (int i = 0; i < encryptionKeyIds.length; ++i) {
                PGPPublicKey key = PGPHelper.getEncryptPublicKey(context, encryptionKeyIds[i]);
                if (key != null) {

                    JcePublicKeyKeyEncryptionMethodGenerator pubKeyEncryptionGenerator = new JcePublicKeyKeyEncryptionMethodGenerator(
                            key);
                    cPk.addMethod(pubKeyEncryptionGenerator);
                }
            }
        }
        encryptOut = cPk.open(out, new byte[1 << 16]);

        PGPSignatureGenerator signatureGenerator = null;
        PGPV3SignatureGenerator signatureV3Generator = null;

        if (signatureKeyId != Id.key.none) {
            updateProgress(progress, R.string.progress_preparingSignature, 10, 100);

            // content signer based on signing key algorithm and choosen hash algorithm
            JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(
                    signingKey.getPublicKey().getAlgorithm(), signatureHashAlgorithm)
                    .setProvider(BOUNCY_CASTLE_PROVIDER_NAME);

            if (signatureForceV3) {
                signatureV3Generator = new PGPV3SignatureGenerator(contentSignerBuilder);
                signatureV3Generator.init(PGPSignature.BINARY_DOCUMENT, signaturePrivateKey);
            } else {
                signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
                signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, signaturePrivateKey);

                String userId = PGPHelper.getMainUserId(PGPHelper.getMasterKey(signingKeyRing));
                PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
                spGen.setSignerUserID(false, userId);
                signatureGenerator.setHashedSubpackets(spGen.generate());
            }
        }

        PGPCompressedDataGenerator compressGen = null;
        BCPGOutputStream bcpgOut = null;
        if (compression == Id.choice.compression.none) {
            bcpgOut = new BCPGOutputStream(encryptOut);
        } else {
            compressGen = new PGPCompressedDataGenerator(compression);
            bcpgOut = new BCPGOutputStream(compressGen.open(encryptOut));
        }
        if (signatureKeyId != Id.key.none) {
            if (signatureForceV3) {
                signatureV3Generator.generateOnePassVersion(false).encode(bcpgOut);
            } else {
                signatureGenerator.generateOnePassVersion(false).encode(bcpgOut);
            }
        }

        PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
        // file name not needed, so empty string
        OutputStream pOut = literalGen.open(bcpgOut, PGPLiteralData.BINARY, "", new Date(),
                new byte[1 << 16]);
        updateProgress(progress, R.string.progress_encrypting, 20, 100);

        long done = 0;
        int n = 0;
        byte[] buffer = new byte[1 << 16];
        InputStream in = data.getInputStream();
        while ((n = in.read(buffer)) > 0) {
            pOut.write(buffer, 0, n);
            if (signatureKeyId != Id.key.none) {
                if (signatureForceV3) {
                    signatureV3Generator.update(buffer, 0, n);
                } else {
                    signatureGenerator.update(buffer, 0, n);
                }
            }
            done += n;
            if (data.getSize() != 0) {
                updateProgress(progress, (int) (20 + (95 - 20) * done / data.getSize()), 100);
            }
        }

        literalGen.close();

        if (signatureKeyId != Id.key.none) {
            updateProgress(progress, R.string.progress_generatingSignature, 95, 100);
            if (signatureForceV3) {
                signatureV3Generator.generate().encode(pOut);
            } else {
                signatureGenerator.generate().encode(pOut);
            }
        }
        if (compressGen != null) {
            compressGen.close();
        }
        encryptOut.close();
        if (useAsciiArmor) {
            armorOut.close();
        }

        updateProgress(progress, R.string.progress_done, 100, 100);
    }

    public static void signText(Context context, ProgressDialogUpdater progress, InputData data,
            OutputStream outStream, long signatureKeyId, String signaturePassphrase,
            int signatureHashAlgorithm, boolean forceV3Signature) throws ApgGeneralException,
            PGPException, IOException, NoSuchAlgorithmException, SignatureException {

        ArmoredOutputStream armorOut = new ArmoredOutputStream(outStream);
        armorOut.setHeader("Version", getFullVersion(context));

        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;

        if (signatureKeyId == 0) {
            armorOut.close();
            throw new ApgGeneralException(context.getString(R.string.error_noSignatureKey));
        }

        signingKeyRing = ProviderHelper.getPGPSecretKeyRingByKeyId(context, signatureKeyId);
        signingKey = PGPHelper.getSigningKey(context, signatureKeyId);
        if (signingKey == null) {
            armorOut.close();
            throw new ApgGeneralException(context.getString(R.string.error_signatureFailed));
        }

        if (signaturePassphrase == null) {
            armorOut.close();
            throw new ApgGeneralException(context.getString(R.string.error_noSignaturePassPhrase));
        }
        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                BOUNCY_CASTLE_PROVIDER_NAME).build(signaturePassphrase.toCharArray());
        signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
        if (signaturePrivateKey == null) {
            armorOut.close();
            throw new ApgGeneralException(
                    context.getString(R.string.error_couldNotExtractPrivateKey));
        }
        updateProgress(progress, R.string.progress_preparingStreams, 0, 100);

        updateProgress(progress, R.string.progress_preparingSignature, 30, 100);

        PGPSignatureGenerator signatureGenerator = null;
        PGPV3SignatureGenerator signatureV3Generator = null;

        // content signer based on signing key algorithm and choosen hash algorithm
        JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(signingKey
                .getPublicKey().getAlgorithm(), signatureHashAlgorithm)
                .setProvider(BOUNCY_CASTLE_PROVIDER_NAME);

        if (forceV3Signature) {
            signatureV3Generator = new PGPV3SignatureGenerator(contentSignerBuilder);
            signatureV3Generator.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, signaturePrivateKey);
        } else {
            signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
            signatureGenerator.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, signaturePrivateKey);

            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            String userId = PGPHelper.getMainUserId(PGPHelper.getMasterKey(signingKeyRing));
            spGen.setSignerUserID(false, userId);
            signatureGenerator.setHashedSubpackets(spGen.generate());
        }

        updateProgress(progress, R.string.progress_signing, 40, 100);

        armorOut.beginClearText(signatureHashAlgorithm);

        InputStream inStream = data.getInputStream();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));

        final byte[] newline = "\r\n".getBytes("UTF-8");

        if (forceV3Signature) {
            processLine(reader.readLine(), armorOut, signatureV3Generator);
        } else {
            processLine(reader.readLine(), armorOut, signatureGenerator);
        }

        while (true) {
            final String line = reader.readLine();

            if (line == null) {
                armorOut.write(newline);
                break;
            }

            armorOut.write(newline);
            if (forceV3Signature) {
                signatureV3Generator.update(newline);
                processLine(line, armorOut, signatureV3Generator);
            } else {
                signatureGenerator.update(newline);
                processLine(line, armorOut, signatureGenerator);
            }
        }

        armorOut.endClearText();

        BCPGOutputStream bOut = new BCPGOutputStream(armorOut);
        if (forceV3Signature) {
            signatureV3Generator.generate().encode(bOut);
        } else {
            signatureGenerator.generate().encode(bOut);
        }
        armorOut.close();

        updateProgress(progress, R.string.progress_done, 100, 100);
    }

    public static void generateSignature(Context context, ProgressDialogUpdater progress,
            InputData data, OutputStream outStream, boolean armored, boolean binary,
            long signatureKeyId, String signaturePassPhrase, int hashAlgorithm,
            boolean forceV3Signature) throws ApgGeneralException, PGPException, IOException,
            NoSuchAlgorithmException, SignatureException {

        OutputStream out = null;

        // Ascii Armor (Base64)
        ArmoredOutputStream armorOut = null;
        if (armored) {
            armorOut = new ArmoredOutputStream(outStream);
            armorOut.setHeader("Version", getFullVersion(context));
            out = armorOut;
        } else {
            out = outStream;
        }

        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;

        if (signatureKeyId == 0) {
            throw new ApgGeneralException(context.getString(R.string.error_noSignatureKey));
        }

        signingKeyRing = ProviderHelper.getPGPSecretKeyRingByKeyId(context, signatureKeyId);
        signingKey = PGPHelper.getSigningKey(context, signatureKeyId);
        if (signingKey == null) {
            throw new ApgGeneralException(context.getString(R.string.error_signatureFailed));
        }

        if (signaturePassPhrase == null) {
            throw new ApgGeneralException(context.getString(R.string.error_noSignaturePassPhrase));
        }

        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                BOUNCY_CASTLE_PROVIDER_NAME).build(signaturePassPhrase.toCharArray());
        signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
        if (signaturePrivateKey == null) {
            throw new ApgGeneralException(
                    context.getString(R.string.error_couldNotExtractPrivateKey));
        }
        updateProgress(progress, R.string.progress_preparingStreams, 0, 100);

        updateProgress(progress, R.string.progress_preparingSignature, 30, 100);

        PGPSignatureGenerator signatureGenerator = null;
        PGPV3SignatureGenerator signatureV3Generator = null;

        int type = PGPSignature.CANONICAL_TEXT_DOCUMENT;
        if (binary) {
            type = PGPSignature.BINARY_DOCUMENT;
        }

        // content signer based on signing key algorithm and choosen hash algorithm
        JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(signingKey
                .getPublicKey().getAlgorithm(), hashAlgorithm)
                .setProvider(BOUNCY_CASTLE_PROVIDER_NAME);

        if (forceV3Signature) {
            signatureV3Generator = new PGPV3SignatureGenerator(contentSignerBuilder);
            signatureV3Generator.init(type, signaturePrivateKey);
        } else {
            signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
            signatureGenerator.init(type, signaturePrivateKey);

            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            String userId = PGPHelper.getMainUserId(PGPHelper.getMasterKey(signingKeyRing));
            spGen.setSignerUserID(false, userId);
            signatureGenerator.setHashedSubpackets(spGen.generate());
        }

        updateProgress(progress, R.string.progress_signing, 40, 100);

        InputStream inStream = data.getInputStream();
        if (binary) {
            byte[] buffer = new byte[1 << 16];
            int n = 0;
            while ((n = inStream.read(buffer)) > 0) {
                if (forceV3Signature) {
                    signatureV3Generator.update(buffer, 0, n);
                } else {
                    signatureGenerator.update(buffer, 0, n);
                }
            }
        } else {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
            final byte[] newline = "\r\n".getBytes("UTF-8");

            while (true) {
                final String line = reader.readLine();

                if (line == null) {
                    break;
                }

                if (forceV3Signature) {
                    processLine(line, null, signatureV3Generator);
                    signatureV3Generator.update(newline);
                } else {
                    processLine(line, null, signatureGenerator);
                    signatureGenerator.update(newline);
                }
            }
        }

        BCPGOutputStream bOut = new BCPGOutputStream(out);
        if (forceV3Signature) {
            signatureV3Generator.generate().encode(bOut);
        } else {
            signatureGenerator.generate().encode(bOut);
        }
        out.close();
        outStream.close();

        if (progress != null)
            progress.setProgress(R.string.progress_done, 100, 100);
    }

    public static PGPPublicKeyRing signKey(Context context, long masterKeyId, long pubKeyId,
            String passphrase) throws ApgGeneralException, NoSuchAlgorithmException,
            NoSuchProviderException, PGPException, SignatureException {
        if (passphrase == null || passphrase.length() <= 0) {
            throw new ApgGeneralException("Unable to obtain passphrase");
        } else {
            PGPPublicKeyRing pubring = ProviderHelper.getPGPPublicKeyRingByKeyId(context, pubKeyId);

            PGPSecretKey signingKey = PGPHelper.getSigningKey(context, masterKeyId);
            if (signingKey == null) {
                throw new ApgGeneralException(context.getString(R.string.error_signatureFailed));
            }

            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                    BOUNCY_CASTLE_PROVIDER_NAME).build(passphrase.toCharArray());
            PGPPrivateKey signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
            if (signaturePrivateKey == null) {
                throw new ApgGeneralException(
                        context.getString(R.string.error_couldNotExtractPrivateKey));
            }

            // TODO: SHA256 fixed?
            JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(
                    signingKey.getPublicKey().getAlgorithm(), PGPUtil.SHA256)
                    .setProvider(BOUNCY_CASTLE_PROVIDER_NAME);

            PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(
                    contentSignerBuilder);

            signatureGenerator.init(PGPSignature.DIRECT_KEY, signaturePrivateKey);

            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();

            PGPSignatureSubpacketVector packetVector = spGen.generate();
            signatureGenerator.setHashedSubpackets(packetVector);

            PGPPublicKey signedKey = PGPPublicKey.addCertification(pubring.getPublicKey(pubKeyId),
                    signatureGenerator.generate());
            pubring = PGPPublicKeyRing.insertPublicKey(pubring, signedKey);

            return pubring;
        }
    }

    public static long getDecryptionKeyId(Context context, InputStream inputStream)
            throws ApgGeneralException, NoAsymmetricEncryptionException, IOException {
        InputStream in = PGPUtil.getDecoderStream(inputStream);
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject();

        // the first object might be a PGP marker packet.
        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        if (enc == null) {
            throw new ApgGeneralException(context.getString(R.string.error_invalidData));
        }

        // TODO: currently we always only look at the first known key
        // find the secret key
        PGPSecretKey secretKey = null;
        Iterator<?> it = enc.getEncryptedDataObjects();
        boolean gotAsymmetricEncryption = false;
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof PGPPublicKeyEncryptedData) {
                gotAsymmetricEncryption = true;
                PGPPublicKeyEncryptedData pbe = (PGPPublicKeyEncryptedData) obj;
                secretKey = ProviderHelper.getPGPSecretKeyByKeyId(context, pbe.getKeyID());
                if (secretKey != null) {
                    break;
                }
            }
        }

        if (!gotAsymmetricEncryption) {
            throw new NoAsymmetricEncryptionException();
        }

        if (secretKey == null) {
            return Id.key.none;
        }

        return secretKey.getKeyID();
    }

    public static boolean hasSymmetricEncryption(Context context, InputStream inputStream)
            throws ApgGeneralException, IOException {
        InputStream in = PGPUtil.getDecoderStream(inputStream);
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject();

        // the first object might be a PGP marker packet.
        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        if (enc == null) {
            throw new ApgGeneralException(context.getString(R.string.error_invalidData));
        }

        Iterator<?> it = enc.getEncryptedDataObjects();
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof PGPPBEEncryptedData) {
                return true;
            }
        }

        return false;
    }

    public static Bundle decryptAndVerify(Context context, ProgressDialogUpdater progress,
            InputData data, OutputStream outStream, String passphrase, boolean assumeSymmetric)
            throws IOException, ApgGeneralException, PGPException, SignatureException {
        if (passphrase == null) {
            passphrase = "";
        }

        Bundle returnData = new Bundle();
        InputStream in = PGPUtil.getDecoderStream(data.getInputStream());
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject();
        long signatureKeyId = 0;

        int currentProgress = 0;
        if (progress != null)
            progress.setProgress(R.string.progress_readingData, currentProgress, 100);

        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        if (enc == null) {
            throw new ApgGeneralException(context.getString(R.string.error_invalidData));
        }

        InputStream clear = null;
        PGPEncryptedData encryptedData = null;

        currentProgress += 5;

        // TODO: currently we always only look at the first known key or symmetric encryption,
        // there might be more...
        if (assumeSymmetric) {
            PGPPBEEncryptedData pbe = null;
            Iterator<?> it = enc.getEncryptedDataObjects();
            // find secret key
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof PGPPBEEncryptedData) {
                    pbe = (PGPPBEEncryptedData) obj;
                    break;
                }
            }

            if (pbe == null) {
                throw new ApgGeneralException(
                        context.getString(R.string.error_noSymmetricEncryptionPacket));
            }

            updateProgress(progress, R.string.progress_preparingStreams, currentProgress, 100);

            PGPDigestCalculatorProvider digestCalcProvider = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(BOUNCY_CASTLE_PROVIDER_NAME).build();
            PBEDataDecryptorFactory decryptorFactory = new JcePBEDataDecryptorFactoryBuilder(
                    digestCalcProvider).setProvider(BOUNCY_CASTLE_PROVIDER_NAME).build(
                    passphrase.toCharArray());

            clear = pbe.getDataStream(decryptorFactory);

            encryptedData = pbe;
            currentProgress += 5;
        } else {
            if (progress != null)
                progress.setProgress(R.string.progress_findingKey, currentProgress, 100);
            PGPPublicKeyEncryptedData pbe = null;
            PGPSecretKey secretKey = null;
            Iterator<?> it = enc.getEncryptedDataObjects();
            // find secret key
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof PGPPublicKeyEncryptedData) {
                    PGPPublicKeyEncryptedData encData = (PGPPublicKeyEncryptedData) obj;
                    secretKey = ProviderHelper.getPGPSecretKeyByKeyId(context, encData.getKeyID());
                    if (secretKey != null) {
                        pbe = encData;
                        break;
                    }
                }
            }

            if (secretKey == null) {
                throw new ApgGeneralException(context.getString(R.string.error_noSecretKeyFound));
            }

            currentProgress += 5;
            updateProgress(progress, R.string.progress_extractingKey, currentProgress, 100);
            PGPPrivateKey privateKey = null;
            try {
                PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder()
                        .setProvider(BOUNCY_CASTLE_PROVIDER_NAME).build(passphrase.toCharArray());
                privateKey = secretKey.extractPrivateKey(keyDecryptor);
            } catch (PGPException e) {
                throw new PGPException(context.getString(R.string.error_wrongPassPhrase));
            }
            if (privateKey == null) {
                throw new ApgGeneralException(
                        context.getString(R.string.error_couldNotExtractPrivateKey));
            }
            currentProgress += 5;
            updateProgress(progress, R.string.progress_preparingStreams, currentProgress, 100);

            PublicKeyDataDecryptorFactory decryptorFactory = new JcePublicKeyDataDecryptorFactoryBuilder()
                    .setProvider(BOUNCY_CASTLE_PROVIDER_NAME).build(privateKey);

            clear = pbe.getDataStream(decryptorFactory);

            encryptedData = pbe;
            currentProgress += 5;
        }

        PGPObjectFactory plainFact = new PGPObjectFactory(clear);
        Object dataChunk = plainFact.nextObject();
        PGPOnePassSignature signature = null;
        PGPPublicKey signatureKey = null;
        int signatureIndex = -1;

        if (dataChunk instanceof PGPCompressedData) {
            if (progress != null)
                progress.setProgress(R.string.progress_decompressingData, currentProgress, 100);
            PGPObjectFactory fact = new PGPObjectFactory(
                    ((PGPCompressedData) dataChunk).getDataStream());
            dataChunk = fact.nextObject();
            plainFact = fact;
            currentProgress += 10;
        }

        if (dataChunk instanceof PGPOnePassSignatureList) {
            if (progress != null)
                progress.setProgress(R.string.progress_processingSignature, currentProgress, 100);
            returnData.putBoolean(ApgIntentService.RESULT_SIGNATURE, true);
            PGPOnePassSignatureList sigList = (PGPOnePassSignatureList) dataChunk;
            for (int i = 0; i < sigList.size(); ++i) {
                signature = sigList.get(i);
                signatureKey = ProviderHelper.getPGPPublicKeyByKeyId(context, signature.getKeyID());
                if (signatureKeyId == 0) {
                    signatureKeyId = signature.getKeyID();
                }
                if (signatureKey == null) {
                    signature = null;
                } else {
                    signatureIndex = i;
                    signatureKeyId = signature.getKeyID();
                    String userId = null;
                    PGPPublicKeyRing signKeyRing = ProviderHelper.getPGPPublicKeyRingByKeyId(
                            context, signatureKeyId);
                    if (signKeyRing != null) {
                        userId = PGPHelper.getMainUserId(PGPHelper.getMasterKey(signKeyRing));
                    }
                    returnData.putString(ApgIntentService.RESULT_SIGNATURE_USER_ID, userId);
                    break;
                }
            }

            returnData.putLong(ApgIntentService.RESULT_SIGNATURE_KEY_ID, signatureKeyId);

            if (signature != null) {
                JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider = new JcaPGPContentVerifierBuilderProvider()
                        .setProvider(BOUNCY_CASTLE_PROVIDER_NAME);

                signature.init(contentVerifierBuilderProvider, signatureKey);
            } else {
                returnData.putBoolean(ApgIntentService.RESULT_SIGNATURE_UNKNOWN, true);
            }

            dataChunk = plainFact.nextObject();
            currentProgress += 10;
        }

        if (dataChunk instanceof PGPSignatureList) {
            dataChunk = plainFact.nextObject();
        }

        if (dataChunk instanceof PGPLiteralData) {
            if (progress != null)
                progress.setProgress(R.string.progress_decrypting, currentProgress, 100);
            PGPLiteralData literalData = (PGPLiteralData) dataChunk;
            OutputStream out = outStream;

            byte[] buffer = new byte[1 << 16];
            InputStream dataIn = literalData.getInputStream();

            int startProgress = currentProgress;
            int endProgress = 100;
            if (signature != null) {
                endProgress = 90;
            } else if (encryptedData.isIntegrityProtected()) {
                endProgress = 95;
            }
            int n = 0;
            int done = 0;
            long startPos = data.getStreamPosition();
            while ((n = dataIn.read(buffer)) > 0) {
                out.write(buffer, 0, n);
                done += n;
                if (signature != null) {
                    try {
                        signature.update(buffer, 0, n);
                    } catch (SignatureException e) {
                        returnData.putBoolean(ApgIntentService.RESULT_SIGNATURE_SUCCESS, false);
                        signature = null;
                    }
                }
                // unknown size, but try to at least have a moving, slowing down progress bar
                currentProgress = startProgress + (endProgress - startProgress) * done
                        / (done + 100000);
                if (data.getSize() - startPos == 0) {
                    currentProgress = endProgress;
                } else {
                    currentProgress = (int) (startProgress + (endProgress - startProgress)
                            * (data.getStreamPosition() - startPos) / (data.getSize() - startPos));
                }
                updateProgress(progress, currentProgress, 100);
            }

            if (signature != null) {
                if (progress != null)
                    progress.setProgress(R.string.progress_verifyingSignature, 90, 100);
                PGPSignatureList signatureList = (PGPSignatureList) plainFact.nextObject();
                PGPSignature messageSignature = signatureList.get(signatureIndex);
                if (signature.verify(messageSignature)) {
                    returnData.putBoolean(ApgIntentService.RESULT_SIGNATURE_SUCCESS, true);
                } else {
                    returnData.putBoolean(ApgIntentService.RESULT_SIGNATURE_SUCCESS, false);
                }
            }
        }

        // TODO: add integrity somewhere
        if (encryptedData.isIntegrityProtected()) {
            if (progress != null)
                progress.setProgress(R.string.progress_verifyingIntegrity, 95, 100);
            if (encryptedData.verify()) {
                // passed
            } else {
                // failed
            }
        } else {
            // no integrity check
        }

        updateProgress(progress, R.string.progress_done, 100, 100);
        return returnData;
    }

    public static Bundle verifyText(Context context, ProgressDialogUpdater progress,
            InputData data, OutputStream outStream, boolean lookupUnknownKey) throws IOException,
            ApgGeneralException, PGPException, SignatureException {
        Bundle returnData = new Bundle();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ArmoredInputStream aIn = new ArmoredInputStream(data.getInputStream());

        updateProgress(progress, R.string.progress_done, 0, 100);

        // mostly taken from ClearSignedFileProcessor
        ByteArrayOutputStream lineOut = new ByteArrayOutputStream();
        int lookAhead = readInputLine(lineOut, aIn);
        byte[] lineSep = getLineSeparator();

        byte[] line = lineOut.toByteArray();
        out.write(line, 0, getLengthWithoutSeparator(line));
        out.write(lineSep);

        while (lookAhead != -1 && aIn.isClearText()) {
            lookAhead = readInputLine(lineOut, lookAhead, aIn);
            line = lineOut.toByteArray();
            out.write(line, 0, getLengthWithoutSeparator(line));
            out.write(lineSep);
        }

        out.close();

        byte[] clearText = out.toByteArray();
        outStream.write(clearText);

        returnData.putBoolean(ApgIntentService.RESULT_SIGNATURE, true);

        updateProgress(progress, R.string.progress_processingSignature, 60, 100);
        PGPObjectFactory pgpFact = new PGPObjectFactory(aIn);

        PGPSignatureList sigList = (PGPSignatureList) pgpFact.nextObject();
        if (sigList == null) {
            throw new ApgGeneralException(context.getString(R.string.error_corruptData));
        }
        PGPSignature signature = null;
        long signatureKeyId = 0;
        PGPPublicKey signatureKey = null;
        for (int i = 0; i < sigList.size(); ++i) {
            signature = sigList.get(i);
            signatureKey = ProviderHelper.getPGPPublicKeyByKeyId(context, signature.getKeyID());
            if (signatureKeyId == 0) {
                signatureKeyId = signature.getKeyID();
            }
            // if key is not known and we want to lookup unknown ones...
            if (signatureKey == null && lookupUnknownKey) {

                returnData = new Bundle();
                returnData.putLong(ApgIntentService.RESULT_SIGNATURE_KEY_ID, signatureKeyId);
                returnData.putBoolean(ApgIntentService.RESULT_SIGNATURE_LOOKUP_KEY, true);

                // return directly now, decrypt will be done again after importing unknown key
                return returnData;
            }

            if (signatureKey == null) {
                signature = null;
            } else {
                signatureKeyId = signature.getKeyID();
                String userId = null;
                PGPPublicKeyRing signKeyRing = ProviderHelper.getPGPPublicKeyRingByKeyId(context,
                        signatureKeyId);
                if (signKeyRing != null) {
                    userId = PGPHelper.getMainUserId(PGPHelper.getMasterKey(signKeyRing));
                }
                returnData.putString(ApgIntentService.RESULT_SIGNATURE_USER_ID, userId);
                break;
            }
        }

        returnData.putLong(ApgIntentService.RESULT_SIGNATURE_KEY_ID, signatureKeyId);

        if (signature == null) {
            returnData.putBoolean(ApgIntentService.RESULT_SIGNATURE_UNKNOWN, true);
            if (progress != null)
                progress.setProgress(R.string.progress_done, 100, 100);
            return returnData;
        }

        JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider = new JcaPGPContentVerifierBuilderProvider()
                .setProvider(BOUNCY_CASTLE_PROVIDER_NAME);

        signature.init(contentVerifierBuilderProvider, signatureKey);

        InputStream sigIn = new BufferedInputStream(new ByteArrayInputStream(clearText));

        lookAhead = readInputLine(lineOut, sigIn);

        processLine(signature, lineOut.toByteArray());

        if (lookAhead != -1) {
            do {
                lookAhead = readInputLine(lineOut, lookAhead, sigIn);

                signature.update((byte) '\r');
                signature.update((byte) '\n');

                processLine(signature, lineOut.toByteArray());
            } while (lookAhead != -1);
        }

        returnData.putBoolean(ApgIntentService.RESULT_SIGNATURE_SUCCESS, signature.verify());

        updateProgress(progress, R.string.progress_done, 100, 100);
        return returnData;
    }

    public static int getStreamContent(Context context, InputStream inStream) throws IOException {
        InputStream in = PGPUtil.getDecoderStream(inStream);
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        Object object = pgpF.nextObject();
        while (object != null) {
            if (object instanceof PGPPublicKeyRing || object instanceof PGPSecretKeyRing) {
                return Id.content.keys;
            } else if (object instanceof PGPEncryptedDataList) {
                return Id.content.encrypted_data;
            }
            object = pgpF.nextObject();
        }

        return Id.content.unknown;
    }

    private static void processLine(final String pLine, final ArmoredOutputStream pArmoredOutput,
            final PGPSignatureGenerator pSignatureGenerator) throws IOException, SignatureException {

        if (pLine == null) {
            return;
        }

        final char[] chars = pLine.toCharArray();
        int len = chars.length;

        while (len > 0) {
            if (!Character.isWhitespace(chars[len - 1])) {
                break;
            }
            len--;
        }

        final byte[] data = pLine.substring(0, len).getBytes("UTF-8");

        if (pArmoredOutput != null) {
            pArmoredOutput.write(data);
        }
        pSignatureGenerator.update(data);
    }

    private static void processLine(final String pLine, final ArmoredOutputStream pArmoredOutput,
            final PGPV3SignatureGenerator pSignatureGenerator) throws IOException,
            SignatureException {

        if (pLine == null) {
            return;
        }

        final char[] chars = pLine.toCharArray();
        int len = chars.length;

        while (len > 0) {
            if (!Character.isWhitespace(chars[len - 1])) {
                break;
            }
            len--;
        }

        final byte[] data = pLine.substring(0, len).getBytes("UTF-8");

        if (pArmoredOutput != null) {
            pArmoredOutput.write(data);
        }
        pSignatureGenerator.update(data);
    }

    // taken from ClearSignedFileProcessor in BC
    private static void processLine(PGPSignature sig, byte[] line) throws SignatureException,
            IOException {
        int length = getLengthWithoutWhiteSpace(line);
        if (length > 0) {
            sig.update(line, 0, length);
        }
    }

    private static int readInputLine(ByteArrayOutputStream bOut, InputStream fIn)
            throws IOException {
        bOut.reset();

        int lookAhead = -1;
        int ch;

        while ((ch = fIn.read()) >= 0) {
            bOut.write(ch);
            if (ch == '\r' || ch == '\n') {
                lookAhead = readPassedEOL(bOut, ch, fIn);
                break;
            }
        }

        return lookAhead;
    }

    private static int readInputLine(ByteArrayOutputStream bOut, int lookAhead, InputStream fIn)
            throws IOException {
        bOut.reset();

        int ch = lookAhead;

        do {
            bOut.write(ch);
            if (ch == '\r' || ch == '\n') {
                lookAhead = readPassedEOL(bOut, ch, fIn);
                break;
            }
        } while ((ch = fIn.read()) >= 0);

        if (ch < 0) {
            lookAhead = -1;
        }

        return lookAhead;
    }

    private static int readPassedEOL(ByteArrayOutputStream bOut, int lastCh, InputStream fIn)
            throws IOException {
        int lookAhead = fIn.read();

        if (lastCh == '\r' && lookAhead == '\n') {
            bOut.write(lookAhead);
            lookAhead = fIn.read();
        }

        return lookAhead;
    }

    private static int getLengthWithoutSeparator(byte[] line) {
        int end = line.length - 1;

        while (end >= 0 && isLineEnding(line[end])) {
            end--;
        }

        return end + 1;
    }

    private static boolean isLineEnding(byte b) {
        return b == '\r' || b == '\n';
    }

    private static int getLengthWithoutWhiteSpace(byte[] line) {
        int end = line.length - 1;

        while (end >= 0 && isWhiteSpace(line[end])) {
            end--;
        }

        return end + 1;
    }

    private static boolean isWhiteSpace(byte b) {
        return b == '\r' || b == '\n' || b == '\t' || b == ' ';
    }

    private static byte[] getLineSeparator() {
        String nl = System.getProperty("line.separator");
        byte[] nlBytes = new byte[nl.length()];

        for (int i = 0; i != nlBytes.length; i++) {
            nlBytes[i] = (byte) nl.charAt(i);
        }

        return nlBytes;
    }

    public static boolean isReleaseVersion(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(Constants.PACKAGE_NAME, 0);
            if (pi.versionCode % 100 == 99) {
                return true;
            } else {
                return false;
            }
        } catch (NameNotFoundException e) {
            // impossible!
            return false;
        }
    }

    public static String getVersion(Context context) {
        String version = null;
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(Constants.PACKAGE_NAME, 0);
            version = pi.versionName;
            return version;
        } catch (NameNotFoundException e) {
            Log.e(Constants.TAG, "Version could not be retrieved!", e);
            return "0.0.0";
        }
    }

    public static String getFullVersion(Context context) {
        return "APG v" + getVersion(context);
    }

    /**
     * Generate a random filename
     * 
     * @param length
     * @return
     */
    public static String generateRandomFilename(int length) {
        SecureRandom random = new SecureRandom();

        byte bytes[] = new byte[length];
        random.nextBytes(bytes);
        String result = "";
        for (int i = 0; i < length; ++i) {
            int v = (bytes[i] + 256) % 64;
            if (v < 10) {
                result += (char) ('0' + v);
            } else if (v < 36) {
                result += (char) ('A' + v - 10);
            } else if (v < 62) {
                result += (char) ('a' + v - 36);
            } else if (v == 62) {
                result += '_';
            } else if (v == 63) {
                result += '.';
            }
        }
        return result;
    }

    /**
     * Go once through stream to get length of stream. The length is later used to display progress
     * when encrypting/decrypting
     * 
     * @param in
     * @return
     * @throws IOException
     */
    public static long getLengthOfStream(InputStream in) throws IOException {
        long size = 0;
        long n = 0;
        byte dummy[] = new byte[0x10000];
        while ((n = in.read(dummy)) > 0) {
            size += n;
        }
        return size;
    }

    /**
     * Deletes file securely by overwriting it with random data before deleting it.
     * 
     * TODO: Does this really help on flash storage?
     * 
     * @param context
     * @param progress
     * @param file
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void deleteFileSecurely(Context context, ProgressDialogUpdater progress, File file)
            throws FileNotFoundException, IOException {
        long length = file.length();
        SecureRandom random = new SecureRandom();
        RandomAccessFile raf = new RandomAccessFile(file, "rws");
        raf.seek(0);
        raf.getFilePointer();
        byte[] data = new byte[1 << 16];
        int pos = 0;
        String msg = context.getString(R.string.progress_deletingSecurely, file.getName());
        while (pos < length) {
            if (progress != null)
                progress.setProgress(msg, (int) (100 * pos / length), 100);
            random.nextBytes(data);
            raf.write(data);
            pos += data.length;
        }
        raf.close();
        file.delete();
    }
}
