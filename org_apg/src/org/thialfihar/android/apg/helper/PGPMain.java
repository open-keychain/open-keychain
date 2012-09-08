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
import org.thialfihar.android.apg.provider.DataProvider;
import org.thialfihar.android.apg.provider.Database;
import org.thialfihar.android.apg.provider.KeyRings;
import org.thialfihar.android.apg.provider.Keys;
import org.thialfihar.android.apg.provider.UserIds;
import org.thialfihar.android.apg.service.ApgService;
import org.thialfihar.android.apg.service.CachedPassphrase;
import org.thialfihar.android.apg.util.HkpKeyServer;
import org.thialfihar.android.apg.util.InputData;
import org.thialfihar.android.apg.util.PositionAwareInputStream;
import org.thialfihar.android.apg.util.Primes;
import org.thialfihar.android.apg.util.ProgressDialogUpdater;
import org.thialfihar.android.apg.util.KeyServer.AddKeyException;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * TODO:
 * 
 * - Externalize the authority and content uri constants
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

    public static final String AUTHORITY = DataProvider.AUTHORITY;

    public static final Uri CONTENT_URI_SECRET_KEY_RINGS = Uri.parse("content://" + AUTHORITY
            + "/key_rings/secret/");
    public static final Uri CONTENT_URI_SECRET_KEY_RING_BY_KEY_ID = Uri.parse("content://"
            + AUTHORITY + "/key_rings/secret/key_id/");
    public static final Uri CONTENT_URI_SECRET_KEY_RING_BY_EMAILS = Uri.parse("content://"
            + AUTHORITY + "/key_rings/secret/emails/");

    public static final Uri CONTENT_URI_PUBLIC_KEY_RINGS = Uri.parse("content://" + AUTHORITY
            + "/key_rings/public/");
    public static final Uri CONTENT_URI_PUBLIC_KEY_RING_BY_KEY_ID = Uri.parse("content://"
            + AUTHORITY + "/key_rings/public/key_id/");
    public static final Uri CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS = Uri.parse("content://"
            + AUTHORITY + "/key_rings/public/emails/");

    private static String VERSION = null;

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

    private static HashMap<Long, CachedPassphrase> mPassPhraseCache = new HashMap<Long, CachedPassphrase>();
    private static String mEditPassPhrase = null;

    private static Database mDatabase = null;

    public static class GeneralException extends Exception {
        static final long serialVersionUID = 0xf812773342L;

        public GeneralException(String message) {
            super(message);
        }
    }

    public static class NoAsymmetricEncryptionException extends Exception {
        static final long serialVersionUID = 0xf812773343L;

        public NoAsymmetricEncryptionException() {
            super();
        }
    }

    public static void initialize(Context context) {
        if (mDatabase == null) {
            mDatabase = new Database(context);
        }
    }

    public static Database getDatabase() {
        return mDatabase;
    }

    public static void setEditPassPhrase(String passPhrase) {
        mEditPassPhrase = passPhrase;
    }

    public static String getEditPassPhrase() {
        return mEditPassPhrase;
    }

    public static void setCachedPassPhrase(long keyId, String passPhrase) {
        mPassPhraseCache.put(keyId, new CachedPassphrase(new Date().getTime(), passPhrase));
    }

    public static String getCachedPassPhrase(long keyId) {
        long realId = keyId;
        if (realId != Id.key.symmetric) {
            PGPSecretKeyRing keyRing = getSecretKeyRing(keyId);
            if (keyRing == null) {
                return null;
            }
            PGPSecretKey masterKey = PGPHelper.getMasterKey(keyRing);
            if (masterKey == null) {
                return null;
            }
            realId = masterKey.getKeyID();
        }
        CachedPassphrase cpp = mPassPhraseCache.get(realId);
        if (cpp == null) {
            return null;
        }
        // set it again to reset the cache life cycle
        setCachedPassPhrase(realId, cpp.passPhrase);
        return cpp.passPhrase;
    }

    public static int cleanUpCache(int ttl, int initialDelay) {
        int delay = initialDelay;
        long realTtl = ttl * 1000;
        long now = new Date().getTime();
        Vector<Long> oldKeys = new Vector<Long>();
        for (Map.Entry<Long, CachedPassphrase> pair : mPassPhraseCache.entrySet()) {
            long lived = now - pair.getValue().timestamp;
            if (lived >= realTtl) {
                oldKeys.add(pair.getKey());
            } else {
                // see, whether the remaining time for this cache entry improves our
                // check delay
                long nextCheck = realTtl - lived + 1000;
                if (nextCheck < delay) {
                    delay = (int) nextCheck;
                }
            }
        }

        for (long keyId : oldKeys) {
            mPassPhraseCache.remove(keyId);
        }

        return delay;
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
     * @throws GeneralException
     * @throws InvalidAlgorithmParameterException
     */
    public static PGPSecretKeyRing createKey(Context context, int algorithmChoice, int keySize,
            String passPhrase, PGPSecretKey masterSecretKey) throws NoSuchAlgorithmException,
            PGPException, NoSuchProviderException, GeneralException,
            InvalidAlgorithmParameterException {

        if (keySize < 512) {
            throw new GeneralException(context.getString(R.string.error_keySizeMinimum512bit));
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
                throw new GeneralException(
                        context.getString(R.string.error_masterKeyMustNotBeElGamal));
            }
            keyGen = KeyPairGenerator.getInstance("ELGAMAL", BOUNCY_CASTLE_PROVIDER_NAME);
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
            throw new GeneralException(context.getString(R.string.error_unknownAlgorithmChoice));
        }
        }

        // build new key pair
        PGPKeyPair keyPair = new JcaPGPKeyPair(algorithm, keyGen.generateKeyPair(), new Date());

        // define hashing and signing algos
        PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(
                HashAlgorithmTags.SHA1);
        PGPContentSignerBuilder certificationSignerBuilder = new JcaPGPContentSignerBuilder(keyPair
                .getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1);

        // Build key encrypter and decrypter based on passphrase
        PBESecretKeyEncryptor keyEncryptor = new JcePBESecretKeyEncryptorBuilder(
                PGPEncryptedData.CAST5, sha1Calc).setProvider(BOUNCY_CASTLE_PROVIDER_NAME).build(
                passPhrase.toCharArray());
        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                BOUNCY_CASTLE_PROVIDER_NAME).build(passPhrase.toCharArray());

        PGPKeyRingGenerator ringGen = null;
        if (masterSecretKey == null) {

            // build keyRing with only this one master key in it!
            ringGen = new PGPKeyRingGenerator(PGPSignature.DEFAULT_CERTIFICATION, keyPair, "",
                    sha1Calc, null, null, certificationSignerBuilder, keyEncryptor);
        } else {
            PGPPublicKey masterPublicKey = masterSecretKey.getPublicKey();
            PGPPrivateKey masterPrivateKey = masterSecretKey.extractPrivateKey(keyDecryptor);
            PGPKeyPair masterKeyPair = new PGPKeyPair(masterPublicKey, masterPrivateKey);

            // build keyRing with master key and new key as subkey (certified by masterkey)
            ringGen = new PGPKeyRingGenerator(PGPSignature.DEFAULT_CERTIFICATION, masterKeyPair,
                    "", sha1Calc, null, null, certificationSignerBuilder, keyEncryptor);

            ringGen.addSubKey(keyPair);
        }

        PGPSecretKeyRing secKeyRing = ringGen.generateSecretKeyRing();

        return secKeyRing;
    }

    public static void buildSecretKey(Context context, ArrayList<String> userIds,
            ArrayList<PGPSecretKey> keys, ArrayList<Integer> keysUsages, long masterKeyId,
            String oldPassPhrase, String newPassPhrase, ProgressDialogUpdater progress)
            throws PGPMain.GeneralException, NoSuchProviderException, PGPException,
            NoSuchAlgorithmException, SignatureException, IOException, Database.GeneralException {

        if (progress != null)
            progress.setProgress(R.string.progress_buildingKey, 0, 100);

        if (oldPassPhrase == null || oldPassPhrase.equals("")) {
            oldPassPhrase = "";
        }

        if (newPassPhrase == null || newPassPhrase.equals("")) {
            newPassPhrase = "";
        }

        // TODO: What is with this code?
        // Vector<String> userIds = new Vector<String>();
        // Vector<PGPSecretKey> keys = new Vector<PGPSecretKey>();

        // ViewGroup userIdEditors = userIdsView.getEditors();
        // ViewGroup keyEditors = keysView.getEditors();
        //
        // boolean gotMainUserId = false;
        // for (int i = 0; i < userIdEditors.getChildCount(); ++i) {
        // UserIdEditor editor = (UserIdEditor) userIdEditors.getChildAt(i);
        // String userId = null;
        // try {
        // userId = editor.getValue();
        // } catch (UserIdEditor.NoNameException e) {
        // throw new Apg.GeneralException(context.getString(R.string.error_userIdNeedsAName));
        // } catch (UserIdEditor.NoEmailException e) {
        // throw new Apg.GeneralException(
        // context.getString(R.string.error_userIdNeedsAnEmailAddress));
        // } catch (UserIdEditor.InvalidEmailException e) {
        // throw new Apg.GeneralException("" + e);
        // }
        //
        // if (userId.equals("")) {
        // continue;
        // }
        //
        // if (editor.isMainUserId()) {
        // userIds.insertElementAt(userId, 0);
        // gotMainUserId = true;
        // } else {
        // userIds.add(userId);
        // }
        // }

        // if (userIds.size() == 0) {
        // throw new Apg.GeneralException(context.getString(R.string.error_keyNeedsAUserId));
        // }
        //
        // if (!gotMainUserId) {
        // throw new Apg.GeneralException(
        // context.getString(R.string.error_mainUserIdMustNotBeEmpty));
        // }

        // if (keyEditors.getChildCount() == 0) {
        // throw new Apg.GeneralException(context.getString(R.string.error_keyNeedsMasterKey));
        // }
        //
        // for (int i = 0; i < keyEditors.getChildCount(); ++i) {
        // KeyEditor editor = (KeyEditor) keyEditors.getChildAt(i);
        // keys.add(editor.getValue());
        // }

        if (progress != null)
            progress.setProgress(R.string.progress_preparingMasterKey, 10, 100);

        int usageId = keysUsages.get(0);
        boolean canSign = (usageId == Id.choice.usage.sign_only || usageId == Id.choice.usage.sign_and_encrypt);
        boolean canEncrypt = (usageId == Id.choice.usage.encrypt_only || usageId == Id.choice.usage.sign_and_encrypt);

        String mainUserId = userIds.get(0);

        PGPSecretKey masterKey = keys.get(0);
        PGPPublicKey masterPublicKey = masterKey.getPublicKey();

        // TODO: why was this done?:
        // PGPPublicKey tmpKey = masterKey.getPublicKey();
        // PGPPublicKey masterPublicKey = new PGPPublicKey(tmpKey.getAlgorithm(),
        // tmpKey.getKey(new BouncyCastleProvider()), tmpKey.getCreationTime());

        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                BOUNCY_CASTLE_PROVIDER_NAME).build(oldPassPhrase.toCharArray());
        PGPPrivateKey masterPrivateKey = masterKey.extractPrivateKey(keyDecryptor);

        if (progress != null)
            progress.setProgress(R.string.progress_certifyingMasterKey, 20, 100);
        for (int i = 0; i < userIds.size(); ++i) {
            String userId = userIds.get(i);

            PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                    masterPublicKey.getAlgorithm(), HashAlgorithmTags.SHA1)
                    .setProvider(BOUNCY_CASTLE_PROVIDER_NAME);
            PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);

            sGen.init(PGPSignature.POSITIVE_CERTIFICATION, masterPrivateKey);

            PGPSignature certification = sGen.generateCertification(userId, masterPublicKey);

            masterPublicKey = PGPPublicKey.addCertification(masterPublicKey, userId, certification);
        }

        // TODO: cross-certify the master key with every sub key

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

        // TODO: this doesn't work quite right yet
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

        if (progress != null) {
            progress.setProgress(R.string.progress_buildingMasterKeyRing, 30, 100);
        }

        // deprecated method:
        // PGPKeyRingGenerator keyGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION,
        // masterKeyPair, mainUserId, PGPEncryptedData.CAST5, newPassPhrase.toCharArray(),
        // hashedPacketsGen.generate(), unhashedPacketsGen.generate(), new SecureRandom(),
        // new BouncyCastleProvider().getName());

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

        if (progress != null)
            progress.setProgress(R.string.progress_addingSubKeys, 40, 100);
        for (int i = 1; i < keys.size(); ++i) {
            if (progress != null)
                progress.setProgress(40 + 50 * (i - 1) / (keys.size() - 1), 100);
            PGPSecretKey subKey = keys.get(i);
            PGPPublicKey subPublicKey = subKey.getPublicKey();

            PBESecretKeyDecryptor keyDecryptor2 = new JcePBESecretKeyDecryptorBuilder()
                    .setProvider(BOUNCY_CASTLE_PROVIDER_NAME).build(oldPassPhrase.toCharArray());
            PGPPrivateKey subPrivateKey = subKey.extractPrivateKey(keyDecryptor2);

            // deprecated method:
            // PGPKeyPair subKeyPair = new PGPKeyPair(subPublicKey.getAlgorithm(),
            // subPublicKey.getKey(new BouncyCastleProvider()), subPrivateKey.getKey(),
            // subPublicKey.getCreationTime());

            // TODO: now used without algorithm and creation time?!
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

            // TODO: this doesn't work quite right yet
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

        if (progress != null)
            progress.setProgress(R.string.progress_savingKeyRing, 90, 100);
        mDatabase.saveKeyRing(secretKeyRing);
        mDatabase.saveKeyRing(publicKeyRing);

        if (progress != null)
            progress.setProgress(R.string.progress_done, 100, 100);
    }

    public static int storeKeyRingInCache(PGPKeyRing keyring) {
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
                    status = mDatabase.saveKeyRing(secretKeyRing);
                }
            } else if (keyring instanceof PGPPublicKeyRing) {
                PGPPublicKeyRing publicKeyRing = (PGPPublicKeyRing) keyring;
                status = mDatabase.saveKeyRing(publicKeyRing);
            }
        } catch (IOException e) {
            status = Id.return_value.error;
        } catch (Database.GeneralException e) {
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

    public static Bundle importKeyRings(Activity context, int type, InputData data,
            ProgressDialogUpdater progress) throws GeneralException, FileNotFoundException,
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
            throw new GeneralException(context.getString(R.string.error_externalStorageNotReady));
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
                    status = storeKeyRingInCache(keyring);
                }

                if (status == Id.return_value.error) {
                    throw new GeneralException(context.getString(R.string.error_savingKeys));
                }

                // update the counts to display to the user at the end
                if (status == Id.return_value.updated) {
                    ++oldKeys;
                } else if (status == Id.return_value.ok) {
                    ++newKeys;
                } else if (status == Id.return_value.bad) {
                    ++badKeys;
                }

                if (progress != null) {
                    progress.setProgress((int) (100 * progressIn.position() / data.getSize()), 100);
                }
                // TODO: needed?
                // obj = objectFactory.nextObject();

                keyring = PGPHelper.decodeKeyRing(bufferedInput);
            }
        } catch (EOFException e) {
            // nothing to do, we are done
        }

        returnData.putInt("added", newKeys);
        returnData.putInt("updated", oldKeys);
        returnData.putInt("bad", badKeys);

        if (progress != null)
            progress.setProgress(R.string.progress_done, 100, 100);

        return returnData;
    }

    public static Bundle exportKeyRings(Activity context, Vector<Integer> keyRingIds,
            OutputStream outStream, ProgressDialogUpdater progress) throws GeneralException,
            FileNotFoundException, PGPException, IOException {
        Bundle returnData = new Bundle();

        if (keyRingIds.size() == 1) {
            if (progress != null)
                progress.setProgress(R.string.progress_exportingKey, 0, 100);
        } else {
            if (progress != null)
                progress.setProgress(R.string.progress_exportingKeys, 0, 100);
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new GeneralException(context.getString(R.string.error_externalStorageNotReady));
        }
        ArmoredOutputStream out = new ArmoredOutputStream(outStream);

        int numKeys = 0;
        for (int i = 0; i < keyRingIds.size(); ++i) {
            if (progress != null)
                progress.setProgress(i * 100 / keyRingIds.size(), 100);
            Object obj = mDatabase.getKeyRing(keyRingIds.get(i));
            PGPPublicKeyRing publicKeyRing;
            PGPSecretKeyRing secretKeyRing;

            if (obj instanceof PGPSecretKeyRing) {
                secretKeyRing = (PGPSecretKeyRing) obj;
                secretKeyRing.encode(out);
            } else if (obj instanceof PGPPublicKeyRing) {
                publicKeyRing = (PGPPublicKeyRing) obj;
                publicKeyRing.encode(out);
            } else {
                continue;
            }
            ++numKeys;
        }
        out.close();
        returnData.putInt("exported", numKeys);

        if (progress != null)
            progress.setProgress(R.string.progress_done, 100, 100);

        return returnData;
    }

    public static void deleteKey(int keyRingId) {
        mDatabase.deleteKeyRing(keyRingId);
    }

    public static PGPKeyRing getKeyRing(int keyRingId) {
        return (PGPKeyRing) mDatabase.getKeyRing(keyRingId);
    }

    public static PGPSecretKeyRing getSecretKeyRing(long keyId) {
        byte[] data = mDatabase.getKeyRingDataFromKeyId(Id.database.type_secret, keyId);
        if (data == null) {
            return null;
        }
        return PGPConversionHelper.BytesToPGPSecretKeyRing(data);

        // deprecated method:
        // try {
        // return new PGPSecretKeyRing(data);
        // } catch (IOException e) {
        // // no good way to handle this, return null
        // // TODO: some info?
        // } catch (PGPException e) {
        // // no good way to handle this, return null
        // // TODO: some info?
        // }
        // return null;
    }

    public static PGPPublicKeyRing getPublicKeyRing(long keyId) {
        byte[] data = mDatabase.getKeyRingDataFromKeyId(Id.database.type_public, keyId);
        if (data == null) {
            return null;
        }
        return PGPConversionHelper.BytesToPGPPublicKeyRing(data);

        // deprecated method:
        // try {
        // return new PGPPublicKeyRing(data);
        // } catch (IOException e) {
        // // no good way to handle this, return null
        // // TODO: some info?
        // }
        // return null;
    }

    public static PGPSecretKey getSecretKey(long keyId) {
        PGPSecretKeyRing keyRing = getSecretKeyRing(keyId);
        if (keyRing == null) {
            return null;
        }
        return keyRing.getSecretKey(keyId);
    }

    public static PGPPublicKey getPublicKey(long keyId) {
        PGPPublicKeyRing keyRing = getPublicKeyRing(keyId);
        if (keyRing == null) {
            return null;
        }

        return keyRing.getPublicKey(keyId);
    }

    public static Vector<Integer> getKeyRingIds(int type) {
        SQLiteDatabase db = mDatabase.db();
        Vector<Integer> keyIds = new Vector<Integer>();
        Cursor c = db.query(KeyRings.TABLE_NAME, new String[] { KeyRings._ID }, KeyRings.TYPE
                + " = ?", new String[] { "" + type }, null, null, null);
        if (c != null && c.moveToFirst()) {
            do {
                keyIds.add(c.getInt(0));
            } while (c.moveToNext());
        }

        if (c != null) {
            c.close();
        }

        return keyIds;
    }

    public static String getMainUserId(long keyId, int type) {
        SQLiteDatabase db = mDatabase.db();
        Cursor c = db.query(Keys.TABLE_NAME + " INNER JOIN " + KeyRings.TABLE_NAME + " ON ("
                + KeyRings.TABLE_NAME + "." + KeyRings._ID + " = " + Keys.TABLE_NAME + "."
                + Keys.KEY_RING_ID + ") " + " INNER JOIN " + Keys.TABLE_NAME + " AS masterKey ON ("
                + KeyRings.TABLE_NAME + "." + KeyRings._ID + " = " + "masterKey."
                + Keys.KEY_RING_ID + " AND " + "masterKey." + Keys.IS_MASTER_KEY + " = '1') "
                + " INNER JOIN " + UserIds.TABLE_NAME + " ON (" + UserIds.TABLE_NAME + "."
                + UserIds.KEY_ID + " = " + "masterKey." + Keys._ID + " AND " + UserIds.TABLE_NAME
                + "." + UserIds.RANK + " = '0')", new String[] { UserIds.USER_ID }, Keys.TABLE_NAME
                + "." + Keys.KEY_ID + " = ? AND " + KeyRings.TABLE_NAME + "." + KeyRings.TYPE
                + " = ?", new String[] { "" + keyId, "" + type, }, null, null, null);
        String userId = "";
        if (c != null && c.moveToFirst()) {
            do {
                userId = c.getString(0);
            } while (c.moveToNext());
        }

        if (c != null) {
            c.close();
        }

        return userId;
    }

    public static void encrypt(Context context, InputData data, OutputStream outStream,
            boolean armored, long encryptionKeyIds[], long signatureKeyId,
            String signaturePassPhrase, ProgressDialogUpdater progress, int symmetricAlgorithm,
            int hashAlgorithm, int compression, boolean forceV3Signature, String passPhrase)
            throws IOException, GeneralException, PGPException, NoSuchProviderException,
            NoSuchAlgorithmException, SignatureException {

        if (encryptionKeyIds == null) {
            encryptionKeyIds = new long[0];
        }

        ArmoredOutputStream armorOut = null;
        OutputStream out = null;
        OutputStream encryptOut = null;
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

        if (encryptionKeyIds.length == 0 && passPhrase == null) {
            throw new GeneralException(
                    context.getString(R.string.error_noEncryptionKeysOrPassPhrase));
        }

        if (signatureKeyId != Id.key.none) {
            signingKeyRing = getSecretKeyRing(signatureKeyId);
            signingKey = PGPHelper.getSigningKey(signatureKeyId);
            if (signingKey == null) {
                throw new GeneralException(context.getString(R.string.error_signatureFailed));
            }

            if (signaturePassPhrase == null) {
                throw new GeneralException(context.getString(R.string.error_noSignaturePassPhrase));
            }
            if (progress != null)
                progress.setProgress(R.string.progress_extractingSignatureKey, 0, 100);
            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                    BOUNCY_CASTLE_PROVIDER_NAME).build(signaturePassPhrase.toCharArray());
            signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
            if (signaturePrivateKey == null) {
                throw new GeneralException(
                        context.getString(R.string.error_couldNotExtractPrivateKey));
            }
        }
        if (progress != null)
            progress.setProgress(R.string.progress_preparingStreams, 5, 100);

        // encrypt and compress input file content
        JcePGPDataEncryptorBuilder encryptorBuilder = new JcePGPDataEncryptorBuilder(
                symmetricAlgorithm).setProvider(BOUNCY_CASTLE_PROVIDER_NAME)
                .setWithIntegrityPacket(true);

        PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(encryptorBuilder);

        // deprecated method:
        // PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(symmetricAlgorithm, true,
        // new SecureRandom(), new BouncyCastleProvider());

        if (encryptionKeyIds.length == 0) {
            // symmetric encryption
            Log.d(Constants.TAG, "encryptionKeyIds length is 0 -> symmetric encryption");

            JcePBEKeyEncryptionMethodGenerator symmetricEncryptionGenerator = new JcePBEKeyEncryptionMethodGenerator(
                    passPhrase.toCharArray());
            cPk.addMethod(symmetricEncryptionGenerator);

            // deprecated method:
            // cPk.addMethod(passPhrase.toCharArray());
        }
        for (int i = 0; i < encryptionKeyIds.length; ++i) {
            PGPPublicKey key = PGPHelper.getEncryptPublicKey(encryptionKeyIds[i]);
            if (key != null) {

                JcePublicKeyKeyEncryptionMethodGenerator pubKeyEncryptionGenerator = new JcePublicKeyKeyEncryptionMethodGenerator(
                        key);
                cPk.addMethod(pubKeyEncryptionGenerator);

                // deprecated method:
                // cPk.addMethod(key);
            }
        }
        encryptOut = cPk.open(out, new byte[1 << 16]);

        PGPSignatureGenerator signatureGenerator = null;
        PGPV3SignatureGenerator signatureV3Generator = null;

        if (signatureKeyId != Id.key.none) {
            if (progress != null)
                progress.setProgress(R.string.progress_preparingSignature, 10, 100);

            // content signer based on signing key algorithm and choosen hash algorithm
            JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(
                    signingKey.getPublicKey().getAlgorithm(), hashAlgorithm)
                    .setProvider(BOUNCY_CASTLE_PROVIDER_NAME);

            if (forceV3Signature) {
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
            if (forceV3Signature) {
                signatureV3Generator.generateOnePassVersion(false).encode(bcpgOut);
            } else {
                signatureGenerator.generateOnePassVersion(false).encode(bcpgOut);
            }
        }

        PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
        // file name not needed, so empty string
        OutputStream pOut = literalGen.open(bcpgOut, PGPLiteralData.BINARY, "", new Date(),
                new byte[1 << 16]);
        if (progress != null)
            progress.setProgress(R.string.progress_encrypting, 20, 100);

        long done = 0;
        int n = 0;
        byte[] buffer = new byte[1 << 16];
        InputStream in = data.getInputStream();
        while ((n = in.read(buffer)) > 0) {
            pOut.write(buffer, 0, n);
            if (signatureKeyId != Id.key.none) {
                if (forceV3Signature) {
                    signatureV3Generator.update(buffer, 0, n);
                } else {
                    signatureGenerator.update(buffer, 0, n);
                }
            }
            done += n;
            if (data.getSize() != 0) {
                if (progress != null)
                    progress.setProgress((int) (20 + (95 - 20) * done / data.getSize()), 100);
            }
        }

        literalGen.close();

        if (signatureKeyId != Id.key.none) {
            if (progress != null)
                progress.setProgress(R.string.progress_generatingSignature, 95, 100);
            if (forceV3Signature) {
                signatureV3Generator.generate().encode(pOut);
            } else {
                signatureGenerator.generate().encode(pOut);
            }
        }
        if (compressGen != null) {
            compressGen.close();
        }
        encryptOut.close();
        if (armored) {
            armorOut.close();
        }

        if (progress != null)
            progress.setProgress(R.string.progress_done, 100, 100);
    }

    public static void signText(Context context, InputData data, OutputStream outStream,
            long signatureKeyId, String signaturePassPhrase, int hashAlgorithm,
            boolean forceV3Signature, ProgressDialogUpdater progress) throws GeneralException,
            PGPException, IOException, NoSuchAlgorithmException, SignatureException {

        ArmoredOutputStream armorOut = new ArmoredOutputStream(outStream);
        armorOut.setHeader("Version", getFullVersion(context));

        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;

        if (signatureKeyId == 0) {
            throw new GeneralException(context.getString(R.string.error_noSignatureKey));
        }

        signingKeyRing = getSecretKeyRing(signatureKeyId);
        signingKey = PGPHelper.getSigningKey(signatureKeyId);
        if (signingKey == null) {
            throw new GeneralException(context.getString(R.string.error_signatureFailed));
        }

        if (signaturePassPhrase == null) {
            throw new GeneralException(context.getString(R.string.error_noSignaturePassPhrase));
        }
        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                BOUNCY_CASTLE_PROVIDER_NAME).build(signaturePassPhrase.toCharArray());
        signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
        if (signaturePrivateKey == null) {
            throw new GeneralException(context.getString(R.string.error_couldNotExtractPrivateKey));
        }
        if (progress != null)
            progress.setProgress(R.string.progress_preparingStreams, 0, 100);

        if (progress != null)
            progress.setProgress(R.string.progress_preparingSignature, 30, 100);

        PGPSignatureGenerator signatureGenerator = null;
        PGPV3SignatureGenerator signatureV3Generator = null;

        // content signer based on signing key algorithm and choosen hash algorithm
        JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(signingKey
                .getPublicKey().getAlgorithm(), hashAlgorithm)
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

        if (progress != null)
            progress.setProgress(R.string.progress_signing, 40, 100);

        armorOut.beginClearText(hashAlgorithm);

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

        if (progress != null)
            progress.setProgress(R.string.progress_done, 100, 100);
    }

    public static void generateSignature(Context context, InputData data, OutputStream outStream,
            boolean armored, boolean binary, long signatureKeyId, String signaturePassPhrase,
            int hashAlgorithm, boolean forceV3Signature, ProgressDialogUpdater progress)
            throws GeneralException, PGPException, IOException, NoSuchAlgorithmException,
            SignatureException {

        ArmoredOutputStream armorOut = null;
        OutputStream out = null;
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
            throw new GeneralException(context.getString(R.string.error_noSignatureKey));
        }

        signingKeyRing = getSecretKeyRing(signatureKeyId);
        signingKey = PGPHelper.getSigningKey(signatureKeyId);
        if (signingKey == null) {
            throw new GeneralException(context.getString(R.string.error_signatureFailed));
        }

        if (signaturePassPhrase == null) {
            throw new GeneralException(context.getString(R.string.error_noSignaturePassPhrase));
        }
        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                BOUNCY_CASTLE_PROVIDER_NAME).build(signaturePassPhrase.toCharArray());
        signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
        if (signaturePrivateKey == null) {
            throw new GeneralException(context.getString(R.string.error_couldNotExtractPrivateKey));
        }
        if (progress != null)
            progress.setProgress(R.string.progress_preparingStreams, 0, 100);

        if (progress != null)
            progress.setProgress(R.string.progress_preparingSignature, 30, 100);

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

        if (progress != null)
            progress.setProgress(R.string.progress_signing, 40, 100);

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

    public static long getDecryptionKeyId(Context context, InputStream inputStream)
            throws GeneralException, NoAsymmetricEncryptionException, IOException {
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
            throw new GeneralException(context.getString(R.string.error_invalidData));
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
                secretKey = getSecretKey(pbe.getKeyID());
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
            throws GeneralException, IOException {
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
            throw new GeneralException(context.getString(R.string.error_invalidData));
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

    public static Bundle decrypt(Context context, InputData data, OutputStream outStream,
            String passPhrase, ProgressDialogUpdater progress, boolean assumeSymmetric)
            throws IOException, GeneralException, PGPException, SignatureException {
        if (passPhrase == null) {
            passPhrase = "";
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
            throw new GeneralException(context.getString(R.string.error_invalidData));
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
                throw new GeneralException(
                        context.getString(R.string.error_noSymmetricEncryptionPacket));
            }

            if (progress != null)
                progress.setProgress(R.string.progress_preparingStreams, currentProgress, 100);

            PGPDigestCalculatorProvider digestCalcProvider = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(BOUNCY_CASTLE_PROVIDER_NAME).build();
            PBEDataDecryptorFactory decryptorFactory = new JcePBEDataDecryptorFactoryBuilder(
                    digestCalcProvider).setProvider(BOUNCY_CASTLE_PROVIDER_NAME).build(
                    passPhrase.toCharArray());

            clear = pbe.getDataStream(decryptorFactory);

            // deprecated method:
            // clear = pbe.getDataStream(passPhrase.toCharArray(), new BouncyCastleProvider());
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
                    secretKey = getSecretKey(encData.getKeyID());
                    if (secretKey != null) {
                        pbe = encData;
                        break;
                    }
                }
            }

            if (secretKey == null) {
                throw new GeneralException(context.getString(R.string.error_noSecretKeyFound));
            }

            currentProgress += 5;
            if (progress != null)
                progress.setProgress(R.string.progress_extractingKey, currentProgress, 100);
            PGPPrivateKey privateKey = null;
            try {
                PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder()
                        .setProvider(BOUNCY_CASTLE_PROVIDER_NAME).build(passPhrase.toCharArray());
                privateKey = secretKey.extractPrivateKey(keyDecryptor);
            } catch (PGPException e) {
                throw new PGPException(context.getString(R.string.error_wrongPassPhrase));
            }
            if (privateKey == null) {
                throw new GeneralException(
                        context.getString(R.string.error_couldNotExtractPrivateKey));
            }
            currentProgress += 5;
            if (progress != null)
                progress.setProgress(R.string.progress_preparingStreams, currentProgress, 100);

            PublicKeyDataDecryptorFactory decryptorFactory = new JcePublicKeyDataDecryptorFactoryBuilder()
                    .setProvider(BOUNCY_CASTLE_PROVIDER_NAME).build(privateKey);

            clear = pbe.getDataStream(decryptorFactory);

            // deprecated method:
            // clear = pbe.getDataStream(privateKey, new BouncyCastleProvider());
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
            returnData.putBoolean(ApgService.RESULT_SIGNATURE, true);
            PGPOnePassSignatureList sigList = (PGPOnePassSignatureList) dataChunk;
            for (int i = 0; i < sigList.size(); ++i) {
                signature = sigList.get(i);
                signatureKey = getPublicKey(signature.getKeyID());
                if (signatureKeyId == 0) {
                    signatureKeyId = signature.getKeyID();
                }
                if (signatureKey == null) {
                    signature = null;
                } else {
                    signatureIndex = i;
                    signatureKeyId = signature.getKeyID();
                    String userId = null;
                    PGPPublicKeyRing sigKeyRing = getPublicKeyRing(signatureKeyId);
                    if (sigKeyRing != null) {
                        userId = PGPHelper.getMainUserId(PGPHelper.getMasterKey(sigKeyRing));
                    }
                    returnData.putString(ApgService.RESULT_SIGNATURE_USER_ID, userId);
                    break;
                }
            }

            returnData.putLong(ApgService.RESULT_SIGNATURE_KEY_ID, signatureKeyId);

            if (signature != null) {
                JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider = new JcaPGPContentVerifierBuilderProvider()
                        .setProvider(BOUNCY_CASTLE_PROVIDER_NAME);

                signature.init(contentVerifierBuilderProvider, signatureKey);

                // deprecated method:
                // signature.initVerify(signatureKey, new BouncyCastleProvider());
            } else {
                returnData.putBoolean(ApgService.RESULT_SIGNATURE_UNKNOWN, true);
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
                        returnData.putBoolean(ApgService.RESULT_SIGNATURE_SUCCESS, false);
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
                if (progress != null)
                    progress.setProgress(currentProgress, 100);
            }

            if (signature != null) {
                if (progress != null)
                    progress.setProgress(R.string.progress_verifyingSignature, 90, 100);
                PGPSignatureList signatureList = (PGPSignatureList) plainFact.nextObject();
                PGPSignature messageSignature = signatureList.get(signatureIndex);
                if (signature.verify(messageSignature)) {
                    returnData.putBoolean(ApgService.RESULT_SIGNATURE_SUCCESS, true);
                } else {
                    returnData.putBoolean(ApgService.RESULT_SIGNATURE_SUCCESS, false);
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

        if (progress != null)
            progress.setProgress(R.string.progress_done, 100, 100);
        return returnData;
    }

    public static Bundle verifyText(Context context, InputData data, OutputStream outStream,
            boolean lookupUnknownKey, ProgressDialogUpdater progress) throws IOException,
            GeneralException, PGPException, SignatureException {
        Bundle returnData = new Bundle();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ArmoredInputStream aIn = new ArmoredInputStream(data.getInputStream());

        if (progress != null)
            progress.setProgress(R.string.progress_done, 0, 100);

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

        returnData.putBoolean(ApgService.RESULT_SIGNATURE, true);

        if (progress != null)
            progress.setProgress(R.string.progress_processingSignature, 60, 100);
        PGPObjectFactory pgpFact = new PGPObjectFactory(aIn);

        PGPSignatureList sigList = (PGPSignatureList) pgpFact.nextObject();
        if (sigList == null) {
            throw new GeneralException(context.getString(R.string.error_corruptData));
        }
        PGPSignature signature = null;
        long signatureKeyId = 0;
        PGPPublicKey signatureKey = null;
        for (int i = 0; i < sigList.size(); ++i) {
            signature = sigList.get(i);
            signatureKey = getPublicKey(signature.getKeyID());
            if (signatureKeyId == 0) {
                signatureKeyId = signature.getKeyID();
            }
            // if key is not known and we want to lookup unknown ones...
            if (signatureKey == null && lookupUnknownKey) {
                
                returnData = new Bundle();
                returnData.putLong(ApgService.RESULT_SIGNATURE_KEY_ID, signatureKeyId);
                returnData.putBoolean(ApgService.RESULT_SIGNATURE_LOOKUP_KEY, true);
                
                return returnData;
                
                // TODO: reimplement!
                // Bundle pauseData = new Bundle();
                // pauseData.putInt(Constants.extras.STATUS, Id.message.unknown_signature_key);
                // pauseData.putLong(Constants.extras.KEY_ID, signatureKeyId);
                // Message msg = new Message();
                // msg.setData(pauseData);
                // context.sendMessage(msg);
                // // pause here
                // context.getRunningThread().pause();
                // // see whether the key was found in the meantime
                // signatureKey = getPublicKey(signature.getKeyID());
            }

            if (signatureKey == null) {
                signature = null;
            } else {
                signatureKeyId = signature.getKeyID();
                String userId = null;
                PGPPublicKeyRing sigKeyRing = getPublicKeyRing(signatureKeyId);
                if (sigKeyRing != null) {
                    userId = PGPHelper.getMainUserId(PGPHelper.getMasterKey(sigKeyRing));
                }
                returnData.putString(ApgService.RESULT_SIGNATURE_USER_ID, userId);
                break;
            }
        }

        returnData.putLong(ApgService.RESULT_SIGNATURE_KEY_ID, signatureKeyId);

        if (signature == null) {
            returnData.putBoolean(ApgService.RESULT_SIGNATURE_UNKNOWN, true);
            if (progress != null)
                progress.setProgress(R.string.progress_done, 100, 100);
            return returnData;
        }

        JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider = new JcaPGPContentVerifierBuilderProvider()
                .setProvider(BOUNCY_CASTLE_PROVIDER_NAME);

        signature.init(contentVerifierBuilderProvider, signatureKey);

        // deprecated method:
        // signature.initVerify(signatureKey, new BouncyCastleProvider());

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

        returnData.putBoolean(ApgService.RESULT_SIGNATURE_SUCCESS, signature.verify());

        if (progress != null)
            progress.setProgress(R.string.progress_done, 100, 100);
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
        if (VERSION != null) {
            return VERSION;
        }
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(Constants.PACKAGE_NAME, 0);
            VERSION = pi.versionName;
            return VERSION;
        } catch (NameNotFoundException e) {
            // impossible!
            return "0.0.0";
        }
    }

    public static String getFullVersion(Context context) {
        return "APG v" + getVersion(context);
    }

    public static String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        /*
         * try { random = SecureRandom.getInstance("SHA1PRNG", new BouncyCastleProvider()); } catch
         * (NoSuchAlgorithmException e) { // TODO: need to handle this case somehow return null; }
         */
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

    public static long getLengthOfStream(InputStream in) throws IOException {
        long size = 0;
        long n = 0;
        byte dummy[] = new byte[0x10000];
        while ((n = in.read(dummy)) > 0) {
            size += n;
        }
        return size;
    }

    public static void deleteFileSecurely(Context context, File file, ProgressDialogUpdater progress)
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
