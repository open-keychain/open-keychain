/*
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

package org.thialfihar.android.apg;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import org.bouncycastle2.bcpg.ArmoredInputStream;
import org.bouncycastle2.bcpg.ArmoredOutputStream;
import org.bouncycastle2.bcpg.BCPGOutputStream;
import org.bouncycastle2.bcpg.CompressionAlgorithmTags;
import org.bouncycastle2.bcpg.HashAlgorithmTags;
import org.bouncycastle2.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle2.bcpg.sig.KeyFlags;
import org.bouncycastle2.jce.provider.BouncyCastleProvider;
import org.bouncycastle2.jce.spec.ElGamalParameterSpec;
import org.bouncycastle2.openpgp.PGPCompressedData;
import org.bouncycastle2.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle2.openpgp.PGPEncryptedData;
import org.bouncycastle2.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle2.openpgp.PGPEncryptedDataList;
import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.openpgp.PGPKeyPair;
import org.bouncycastle2.openpgp.PGPKeyRingGenerator;
import org.bouncycastle2.openpgp.PGPLiteralData;
import org.bouncycastle2.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle2.openpgp.PGPObjectFactory;
import org.bouncycastle2.openpgp.PGPOnePassSignature;
import org.bouncycastle2.openpgp.PGPOnePassSignatureList;
import org.bouncycastle2.openpgp.PGPPBEEncryptedData;
import org.bouncycastle2.openpgp.PGPPrivateKey;
import org.bouncycastle2.openpgp.PGPPublicKey;
import org.bouncycastle2.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle2.openpgp.PGPPublicKeyRing;
import org.bouncycastle2.openpgp.PGPSecretKey;
import org.bouncycastle2.openpgp.PGPSecretKeyRing;
import org.bouncycastle2.openpgp.PGPSignature;
import org.bouncycastle2.openpgp.PGPSignatureGenerator;
import org.bouncycastle2.openpgp.PGPSignatureList;
import org.bouncycastle2.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle2.openpgp.PGPSignatureSubpacketVector;
import org.bouncycastle2.openpgp.PGPUtil;
import org.thialfihar.android.apg.provider.PublicKeys;
import org.thialfihar.android.apg.provider.SecretKeys;
import org.thialfihar.android.apg.ui.widget.KeyEditor;
import org.thialfihar.android.apg.ui.widget.SectionView;
import org.thialfihar.android.apg.ui.widget.UserIdEditor;
import org.thialfihar.android.apg.utils.IterableIterator;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.ViewGroup;

public class Apg {
    public static class Intent {
        public static final String DECRYPT = "org.thialfihar.android.apg.intent.DECRYPT";
        public static final String ENCRYPT = "org.thialfihar.android.apg.intent.ENCRYPT";
        public static final String DECRYPT_FILE = "org.thialfihar.android.apg.intent.DECRYPT_FILE";
        public static final String ENCRYPT_FILE = "org.thialfihar.android.apg.intent.ENCRYPT_FILE";
    }

    public static String VERSION = "0.9.5";
    public static String FULL_VERSION = "APG v" + VERSION;

    private static final int[] PREFERRED_SYMMETRIC_ALGORITHMS =
            new int[] {
                    SymmetricKeyAlgorithmTags.AES_256,
                    SymmetricKeyAlgorithmTags.AES_192,
                    SymmetricKeyAlgorithmTags.AES_128,
                    SymmetricKeyAlgorithmTags.CAST5,
                    SymmetricKeyAlgorithmTags.TRIPLE_DES };
    private static final int[] PREFERRED_HASH_ALGORITHMS =
            new int[] {
                    HashAlgorithmTags.SHA1,
                    HashAlgorithmTags.SHA256,
                    HashAlgorithmTags.RIPEMD160 };
    private static final int[] PREFERRED_COMPRESSION_ALGORITHMS =
            new int[] {
                    CompressionAlgorithmTags.ZLIB,
                    CompressionAlgorithmTags.BZIP2,
                    CompressionAlgorithmTags.ZIP };

    protected static Vector<PGPPublicKeyRing> mPublicKeyRings = new Vector<PGPPublicKeyRing>();
    protected static Vector<PGPSecretKeyRing> mSecretKeyRings = new Vector<PGPSecretKeyRing>();

    public static Pattern PGP_MESSAGE =
            Pattern.compile(".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*",
                            Pattern.DOTALL);

    public static Pattern PGP_SIGNED_MESSAGE =
        Pattern.compile(".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
                        Pattern.DOTALL);

    protected static boolean mInitialized = false;

    protected static HashMap<Long, Integer> mSecretKeyIdToIdMap;
    protected static HashMap<Long, PGPSecretKeyRing> mSecretKeyIdToKeyRingMap;
    protected static HashMap<Long, Integer> mPublicKeyIdToIdMap;
    protected static HashMap<Long, PGPPublicKeyRing> mPublicKeyIdToKeyRingMap;

    public static final String PUBLIC_KEY_PROJECTION[] =
            new String[] {
                    PublicKeys._ID,
                    PublicKeys.KEY_ID,
                    PublicKeys.KEY_DATA,
                    PublicKeys.WHO_ID, };
    public static final String SECRET_KEY_PROJECTION[] =
            new String[] {
                    PublicKeys._ID,
                    PublicKeys.KEY_ID,
                    PublicKeys.KEY_DATA,
                    PublicKeys.WHO_ID, };

    private static HashMap<Long, CachedPassPhrase> mPassPhraseCache =
            new HashMap<Long, CachedPassPhrase>();
    private static String mEditPassPhrase = null;

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

    static {
        mPublicKeyRings = new Vector<PGPPublicKeyRing>();
        mSecretKeyRings = new Vector<PGPSecretKeyRing>();
        mSecretKeyIdToIdMap = new HashMap<Long, Integer>();
        mSecretKeyIdToKeyRingMap = new HashMap<Long, PGPSecretKeyRing>();
        mPublicKeyIdToIdMap = new HashMap<Long, Integer>();
        mPublicKeyIdToKeyRingMap = new HashMap<Long, PGPPublicKeyRing>();
    }

    public static void initialize(Activity context) {
        if (mInitialized) {
            return;
        }

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File dir = new File(Constants.path.app_dir);
            if (!dir.exists() && !dir.mkdirs()) {
                // ignore this for now, it's not crucial
                // that the directory doesn't exist at this point
            }
        }

        loadKeyRings(context, Id.type.public_key);
        loadKeyRings(context, Id.type.secret_key);

        mInitialized = true;
    }

    public static class PublicKeySorter implements Comparator<PGPPublicKeyRing> {
        @Override
        public int compare(PGPPublicKeyRing object1, PGPPublicKeyRing object2) {
            PGPPublicKey key1 = getMasterKey(object1);
            PGPPublicKey key2 = getMasterKey(object2);
            if (key1 == null && key2 == null) {
                return 0;
            }

            if (key1 == null) {
                return -1;
            }

            if (key2 == null) {
                return 1;
            }

            String uid1 = getMainUserId(key1);
            String uid2 = getMainUserId(key2);
            if (uid1 == null && uid2 == null) {
                return 0;
            }

            if (uid1 == null) {
                return -1;
            }

            if (uid2 == null) {
                return 1;
            }

            return uid1.compareTo(uid2);
        }
    }

    public static class SecretKeySorter implements Comparator<PGPSecretKeyRing> {
        @Override
        public int compare(PGPSecretKeyRing object1, PGPSecretKeyRing object2) {
            PGPSecretKey key1 = getMasterKey(object1);
            PGPSecretKey key2 = getMasterKey(object2);
            if (key1 == null && key2 == null) {
                return 0;
            }

            if (key1 == null) {
                return -1;
            }

            if (key2 == null) {
                return 1;
            }

            String uid1 = getMainUserId(key1);
            String uid2 = getMainUserId(key2);
            if (uid1 == null && uid2 == null) {
                return 0;
            }

            if (uid1 == null) {
                return -1;
            }

            if (uid2 == null) {
                return 1;
            }

            return uid1.compareTo(uid2);
        }
    }

    public static void setEditPassPhrase(String passPhrase) {
        mEditPassPhrase = passPhrase;
    }

    public static String getEditPassPhrase() {
        return mEditPassPhrase;
    }

    public static void setCachedPassPhrase(long keyId, String passPhrase) {
        mPassPhraseCache.put(keyId, new CachedPassPhrase(new Date().getTime(), passPhrase));
    }

    public static String getCachedPassPhrase(long keyId) {
        long realId = keyId;
        if (realId != Id.key.symmetric) {
            PGPSecretKeyRing keyRing = findSecretKeyRing(keyId);
            if (keyRing == null) {
                return null;
            }
            PGPSecretKey masterKey = getMasterKey(keyRing);
            if (masterKey == null) {
                return null;
            }
            realId = masterKey.getKeyID();
        }
        CachedPassPhrase cpp = mPassPhraseCache.get(realId);
        if (cpp == null) {
            return null;
        }
        // set it again to reset the cache life cycle
        setCachedPassPhrase(realId, cpp.passPhrase);
        return cpp.passPhrase;
    }

    public static void cleanUpCache(int ttl) {
        long now = new Date().getTime();

        Vector<Long> oldKeys = new Vector<Long>();
        for (Map.Entry<Long, CachedPassPhrase> pair : mPassPhraseCache.entrySet()) {
            if ((now - pair.getValue().timestamp) >= 1000 * ttl) {
                oldKeys.add(pair.getKey());
            }
        }

        for (long keyId : oldKeys) {
            mPassPhraseCache.remove(keyId);
        }
    }

    public static PGPSecretKey createKey(Context context,
                                         int algorithmChoice, int keySize, String passPhrase,
                                         PGPSecretKey masterKey)
                  throws NoSuchAlgorithmException, PGPException, NoSuchProviderException,
                  GeneralException, InvalidAlgorithmParameterException {

        if (keySize < 512) {
            throw new GeneralException(context.getString(R.string.error_keySizeMinimum512bit));
        }

        Security.addProvider(new BouncyCastleProvider());

        if (passPhrase == null) {
            passPhrase = "";
        }

        int algorithm = 0;
        KeyPairGenerator keyGen = null;

        switch (algorithmChoice) {
            case Id.choice.algorithm.dsa: {
                keyGen = KeyPairGenerator.getInstance("DSA", new BouncyCastleProvider());
                keyGen.initialize(keySize, new SecureRandom());
                algorithm = PGPPublicKey.DSA;
                break;
            }

            case Id.choice.algorithm.elgamal: {
                if (masterKey == null) {
                    throw new GeneralException(context.getString(R.string.error_masterKeyMustNotBeElGamal));
                }
                keyGen = KeyPairGenerator.getInstance("ELGAMAL", new BouncyCastleProvider());
                BigInteger p = Primes.getBestPrime(keySize);
                BigInteger g = new BigInteger("2");

                ElGamalParameterSpec elParams = new ElGamalParameterSpec(p, g);

                keyGen.initialize(elParams);
                algorithm = PGPPublicKey.ELGAMAL_ENCRYPT;
                break;
            }

            case Id.choice.algorithm.rsa: {
                keyGen = KeyPairGenerator.getInstance("RSA", new BouncyCastleProvider());
                keyGen.initialize(keySize, new SecureRandom());

                algorithm = PGPPublicKey.RSA_GENERAL;
                break;
            }

            default: {
                throw new GeneralException(context.getString(R.string.error_unknownAlgorithmChoice));
            }
        }

        PGPKeyPair keyPair = new PGPKeyPair(algorithm, keyGen.generateKeyPair(), new Date());

        PGPSecretKey secretKey = null;
        if (masterKey == null) {
            // enough for now, as we assemble the key again later anyway
            secretKey = new PGPSecretKey(PGPSignature.DEFAULT_CERTIFICATION, keyPair, "",
                                         PGPEncryptedData.CAST5, passPhrase.toCharArray(),
                                         null, null,
                                         new SecureRandom(), new BouncyCastleProvider().getName());

        } else {
            PGPPublicKey tmpKey = masterKey.getPublicKey();
            PGPPublicKey masterPublicKey =
                new PGPPublicKey(tmpKey.getAlgorithm(),
                                 tmpKey.getKey(new BouncyCastleProvider()),
                                 tmpKey.getCreationTime());
            PGPPrivateKey masterPrivateKey =
                masterKey.extractPrivateKey(passPhrase.toCharArray(),
                                            new BouncyCastleProvider());

            PGPKeyPair masterKeyPair = new PGPKeyPair(masterPublicKey, masterPrivateKey);
            PGPKeyRingGenerator ringGen =
                new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION,
                                        masterKeyPair, "",
                                        PGPEncryptedData.CAST5, passPhrase.toCharArray(),
                                        null, null,
                                        new SecureRandom(), new BouncyCastleProvider().getName());
            ringGen.addSubKey(keyPair);
            PGPSecretKeyRing secKeyRing = ringGen.generateSecretKeyRing();
            Iterator it = secKeyRing.getSecretKeys();
            // first one is the master key
            it.next();
            secretKey = (PGPSecretKey) it.next();
        }


        return secretKey;
    }

    private static long getNumDatesBetween(GregorianCalendar first, GregorianCalendar second) {
        GregorianCalendar tmp = new GregorianCalendar();
        tmp.setTime(first.getTime());
        long numDays = (second.getTimeInMillis() - first.getTimeInMillis()) / 1000 / 86400;
        tmp.add(Calendar.DAY_OF_MONTH, (int)numDays);
        while (tmp.before(second)) {
            tmp.add(Calendar.DAY_OF_MONTH, 1);
            ++numDays;
        }
        return numDays;
    }

    public static void buildSecretKey(Activity context,
                                      SectionView userIdsView, SectionView keysView,
                                      String oldPassPhrase, String newPassPhrase,
                                      ProgressDialogUpdater progress)
            throws Apg.GeneralException, NoSuchProviderException, PGPException,
            NoSuchAlgorithmException, SignatureException {

        progress.setProgress(R.string.progress_buildingKey, 0, 100);

        Security.addProvider(new BouncyCastleProvider());

        if (oldPassPhrase == null || oldPassPhrase.equals("")) {
            oldPassPhrase = "";
        }

        if (newPassPhrase == null || newPassPhrase.equals("")) {
            newPassPhrase = "";
        }

        Vector<String> userIds = new Vector<String>();
        Vector<PGPSecretKey> keys = new Vector<PGPSecretKey>();

        ViewGroup userIdEditors = userIdsView.getEditors();
        ViewGroup keyEditors = keysView.getEditors();

        boolean gotMainUserId = false;
        for (int i = 0; i < userIdEditors.getChildCount(); ++i) {
            UserIdEditor editor = (UserIdEditor)userIdEditors.getChildAt(i);
            String userId = null;
            try {
                userId = editor.getValue();
            } catch (UserIdEditor.NoNameException e) {
                throw new Apg.GeneralException(context.getString(R.string.error_userIdNeedsAName));
            } catch (UserIdEditor.NoEmailException e) {
                throw new Apg.GeneralException(context.getString(R.string.error_userIdNeedsAnEmailAddress));
            } catch (UserIdEditor.InvalidEmailException e) {
                throw new Apg.GeneralException(e.getMessage());
            }

            if (userId.equals("")) {
                continue;
            }

            if (editor.isMainUserId()) {
                userIds.insertElementAt(userId, 0);
                gotMainUserId = true;
            } else {
                userIds.add(userId);
            }
        }

        if (userIds.size() == 0) {
            throw new Apg.GeneralException(context.getString(R.string.error_keyNeedsAUserId));
        }

        if (!gotMainUserId) {
            throw new Apg.GeneralException(context.getString(R.string.error_mainUserIdMustNotBeEmpty));
        }

        if (keyEditors.getChildCount() == 0) {
            throw new Apg.GeneralException(context.getString(R.string.error_keyNeedsMasterKey));
        }

        for (int i = 0; i < keyEditors.getChildCount(); ++i) {
            KeyEditor editor = (KeyEditor)keyEditors.getChildAt(i);
            keys.add(editor.getValue());
        }

        progress.setProgress(R.string.progress_preparingMasterKey, 10, 100);
        KeyEditor keyEditor = (KeyEditor) keyEditors.getChildAt(0);
        int usageId = keyEditor.getUsage();
        boolean canSign = (usageId == Id.choice.usage.sign_only ||
                           usageId == Id.choice.usage.sign_and_encrypt);
        boolean canEncrypt = (usageId == Id.choice.usage.encrypt_only ||
                              usageId == Id.choice.usage.sign_and_encrypt);

        String mainUserId = userIds.get(0);

        PGPSecretKey masterKey = keys.get(0);
        PGPPublicKey tmpKey = masterKey.getPublicKey();
        PGPPublicKey masterPublicKey =
            new PGPPublicKey(tmpKey.getAlgorithm(),
                             tmpKey.getKey(new BouncyCastleProvider()),
                             tmpKey.getCreationTime());
        PGPPrivateKey masterPrivateKey =
            masterKey.extractPrivateKey(oldPassPhrase.toCharArray(),
                                        new BouncyCastleProvider());

        progress.setProgress(R.string.progress_certifyingMasterKey, 20, 100);
        for (int i = 0; i < userIds.size(); ++i) {
            String userId = userIds.get(i);

            PGPSignatureGenerator sGen =
                    new PGPSignatureGenerator(masterPublicKey.getAlgorithm(),
                                              HashAlgorithmTags.SHA1, new BouncyCastleProvider());

            sGen.initSign(PGPSignature.POSITIVE_CERTIFICATION, masterPrivateKey);

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

        if (keyEditor.getExpiryDate() != null) {
            GregorianCalendar creationDate = new GregorianCalendar();
            creationDate.setTime(getCreationDate(masterKey));
            GregorianCalendar expiryDate = keyEditor.getExpiryDate();
            long numDays = getNumDatesBetween(creationDate, expiryDate);
            if (numDays <= 0) {
                throw new GeneralException(context.getString(R.string.error_expiryMustComeAfterCreation));
            }
            hashedPacketsGen.setKeyExpirationTime(true, numDays * 86400);
        }

        progress.setProgress(R.string.progress_buildingMasterKeyRing, 30, 100);
        PGPKeyRingGenerator keyGen =
                new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION,
                                        masterKeyPair, mainUserId,
                                        PGPEncryptedData.CAST5, newPassPhrase.toCharArray(),
                                        hashedPacketsGen.generate(), unhashedPacketsGen.generate(),
                                        new SecureRandom(), new BouncyCastleProvider().getName());

        progress.setProgress(R.string.progress_addingSubKeys, 40, 100);
        for (int i = 1; i < keys.size(); ++i) {
            progress.setProgress(40 + 50 * (i - 1)/ (keys.size() - 1), 100);
            PGPSecretKey subKey = keys.get(i);
            keyEditor = (KeyEditor) keyEditors.getChildAt(i);
            PGPPublicKey subPublicKey = subKey.getPublicKey();
            PGPPrivateKey subPrivateKey =
                    subKey.extractPrivateKey(oldPassPhrase.toCharArray(),
                                             new BouncyCastleProvider());
            PGPKeyPair subKeyPair =
                new PGPKeyPair(subPublicKey.getAlgorithm(),
                               subPublicKey.getKey(new BouncyCastleProvider()),
                               subPrivateKey.getKey(),
                               subPublicKey.getCreationTime());

            hashedPacketsGen = new PGPSignatureSubpacketGenerator();
            unhashedPacketsGen = new PGPSignatureSubpacketGenerator();

            keyFlags = 0;
            usageId = keyEditor.getUsage();
            canSign = (usageId == Id.choice.usage.sign_only ||
                       usageId == Id.choice.usage.sign_and_encrypt);
            canEncrypt = (usageId == Id.choice.usage.encrypt_only ||
                          usageId == Id.choice.usage.sign_and_encrypt);
            if (canSign) {
                keyFlags |= KeyFlags.SIGN_DATA;
            }
            if (canEncrypt) {
                keyFlags |= KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE;
            }
            hashedPacketsGen.setKeyFlags(true, keyFlags);

            if (keyEditor.getExpiryDate() != null) {
                GregorianCalendar creationDate = new GregorianCalendar();
                creationDate.setTime(getCreationDate(masterKey));
                GregorianCalendar expiryDate = keyEditor.getExpiryDate();
                long numDays = getNumDatesBetween(creationDate, expiryDate);
                if (numDays <= 0) {
                    throw new GeneralException(context.getString(R.string.error_expiryMustComeAfterCreation));
                }
                hashedPacketsGen.setKeyExpirationTime(true, numDays * 86400);
            }

            keyGen.addSubKey(subKeyPair,
                             hashedPacketsGen.generate(), unhashedPacketsGen.generate());
        }

        PGPSecretKeyRing secretKeyRing = keyGen.generateSecretKeyRing();
        PGPPublicKeyRing publicKeyRing = keyGen.generatePublicKeyRing();

        progress.setProgress(R.string.progress_savingKeyRing, 90, 100);
        saveKeyRing(context, secretKeyRing);
        saveKeyRing(context, publicKeyRing);

        loadKeyRings(context, Id.type.public_key);
        loadKeyRings(context, Id.type.secret_key);
        progress.setProgress(R.string.progress_done, 100, 100);
    }

    private static int saveKeyRing(Activity context, PGPPublicKeyRing keyRing) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ContentValues values = new ContentValues();

        PGPPublicKey masterKey = getMasterKey(keyRing);
        if (masterKey == null) {
            return Id.return_value.no_master_key;
        }

        try {
            keyRing.encode(out);
            out.close();
        } catch (IOException e) {
            return Id.return_value.error;
        }

        values.put(PublicKeys.KEY_ID, masterKey.getKeyID());
        values.put(PublicKeys.KEY_DATA, out.toByteArray());

        Uri uri = Uri.withAppendedPath(PublicKeys.CONTENT_URI_BY_KEY_ID, "" + masterKey.getKeyID());
        Cursor cursor = context.managedQuery(uri, PUBLIC_KEY_PROJECTION, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            context.getContentResolver().update(uri, values, null, null);
            return Id.return_value.updated;
        } else {
            context.getContentResolver().insert(PublicKeys.CONTENT_URI, values);
            return Id.return_value.ok;
        }
    }

    private static int saveKeyRing(Activity context, PGPSecretKeyRing keyRing) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ContentValues values = new ContentValues();

        PGPSecretKey masterKey = getMasterKey(keyRing);
        if (masterKey == null) {
            return Id.return_value.no_master_key;
        }

        try {
            keyRing.encode(out);
            out.close();
        } catch (IOException e) {
            return Id.return_value.error;
        }

        values.put(SecretKeys.KEY_ID, masterKey.getKeyID());
        values.put(SecretKeys.KEY_DATA, out.toByteArray());

        Uri uri = Uri.withAppendedPath(SecretKeys.CONTENT_URI_BY_KEY_ID, "" + masterKey.getKeyID());
        Cursor cursor = context.managedQuery(uri, SECRET_KEY_PROJECTION, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            context.getContentResolver().update(uri, values, null, null);
            return Id.return_value.updated;
        } else {
            context.getContentResolver().insert(SecretKeys.CONTENT_URI, values);
            return Id.return_value.ok;
        }
    }

    public static Bundle importKeyRings(Activity context, int type, String filename,
                                        ProgressDialogUpdater progress)
            throws GeneralException, FileNotFoundException, PGPException, IOException {
        Bundle returnData = new Bundle();
        PGPObjectFactory objectFactory = null;

        if (type == Id.type.secret_key) {
            progress.setProgress(R.string.progress_importingSecretKeys, 0, 100);
        } else {
            progress.setProgress(R.string.progress_importingPublicKeys, 0, 100);
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new GeneralException(context.getString(R.string.error_externalStorageNotReady));
        }

        FileInputStream fileIn = new FileInputStream(filename);
        InputStream in = PGPUtil.getDecoderStream(fileIn);
        objectFactory = new PGPObjectFactory(in);

        Vector<Object> objects = new Vector<Object>();
        Object obj = objectFactory.nextObject();
        while (obj != null) {
            objects.add(obj);
            obj = objectFactory.nextObject();
        }

        int newKeys = 0;
        int oldKeys = 0;
        for (int i = 0; i < objects.size(); ++i) {
            progress.setProgress(i * 100 / objects.size(), 100);
            obj = objects.get(i);
            PGPPublicKeyRing publicKeyRing;
            PGPSecretKeyRing secretKeyRing;
            int retValue;

            if (type == Id.type.secret_key) {
                if (!(obj instanceof PGPSecretKeyRing)) {
                    continue;
                }
                secretKeyRing = (PGPSecretKeyRing) obj;
                retValue = saveKeyRing(context, secretKeyRing);
            } else {
                if (!(obj instanceof PGPPublicKeyRing)) {
                    continue;
                }
                publicKeyRing = (PGPPublicKeyRing) obj;
                retValue = saveKeyRing(context, publicKeyRing);
            }

            if (retValue == Id.return_value.error) {
                throw new GeneralException(context.getString(R.string.error_savingKeys));
            }

            if (retValue == Id.return_value.updated) {
                ++oldKeys;
            } else if (retValue == Id.return_value.ok) {
                ++newKeys;
            }
        }

        progress.setProgress(R.string.progress_reloadingKeys, 100, 100);
        loadKeyRings(context, type);

        returnData.putInt("added", newKeys);
        returnData.putInt("updated", oldKeys);

        progress.setProgress(R.string.progress_done, 100, 100);

        return returnData;
    }

    public static Bundle exportKeyRings(Activity context, Vector<Object> keys, String filename,
                                        ProgressDialogUpdater progress)
            throws GeneralException, FileNotFoundException, PGPException, IOException {
        Bundle returnData = new Bundle();

        if (keys.size() == 1) {
            progress.setProgress(R.string.progress_exportingKey, 0, 100);
        } else {
            progress.setProgress(R.string.progress_exportingKeys, 0, 100);
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new GeneralException(context.getString(R.string.error_externalStorageNotReady));
        }
        FileOutputStream fileOut = new FileOutputStream(new File(filename), false);
        ArmoredOutputStream out = new ArmoredOutputStream(fileOut);

        int numKeys = 0;
        for (int i = 0; i < keys.size(); ++i) {
            progress.setProgress(i * 100 / keys.size(), 100);
            Object obj = keys.get(i);
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
        fileOut.close();
        returnData.putInt("exported", numKeys);

        progress.setProgress(R.string.progress_done, 100, 100);

        return returnData;
    }

    private static void loadKeyRings(Activity context, int type) {
        Cursor cursor;
        if (type == Id.type.secret_key) {
            mSecretKeyRings.clear();
            mSecretKeyIdToIdMap.clear();
            mSecretKeyIdToKeyRingMap.clear();
            cursor = context.managedQuery(SecretKeys.CONTENT_URI, SECRET_KEY_PROJECTION,
                                          null, null, null);
        } else {
            mPublicKeyRings.clear();
            mPublicKeyIdToIdMap.clear();
            mPublicKeyIdToKeyRingMap.clear();
            cursor = context.managedQuery(PublicKeys.CONTENT_URI, PUBLIC_KEY_PROJECTION,
                                          null, null, null);
        }

        for (int i = 0; i < cursor.getCount(); ++i) {
            cursor.moveToPosition(i);
            String sharedIdColumn = PublicKeys._ID; // same in both
            String sharedKeyIdColumn = PublicKeys.KEY_ID; // same in both
            String sharedKeyDataColumn = PublicKeys.KEY_DATA; // same in both
            int idIndex = cursor.getColumnIndex(sharedIdColumn);
            int keyIdIndex = cursor.getColumnIndex(sharedKeyIdColumn);
            int keyDataIndex = cursor.getColumnIndex(sharedKeyDataColumn);

            byte keyData[] = cursor.getBlob(keyDataIndex);
            int id = cursor.getInt(idIndex);
            long keyId = cursor.getLong(keyIdIndex);

            try {
                if (type == Id.type.secret_key) {
                    PGPSecretKeyRing key = new PGPSecretKeyRing(keyData);
                    mSecretKeyRings.add(key);
                    mSecretKeyIdToIdMap.put(keyId, id);
                    mSecretKeyIdToKeyRingMap.put(keyId, key);
                } else {
                    PGPPublicKeyRing key = new PGPPublicKeyRing(keyData);
                    mPublicKeyRings.add(key);
                    mPublicKeyIdToIdMap.put(keyId, id);
                    mPublicKeyIdToKeyRingMap.put(keyId, key);
                }
            } catch (IOException e) {
                // TODO: some error handling
            } catch (PGPException e) {
                // TODO: some error handling
            }
        }

        if (type == Id.type.secret_key) {
            Collections.sort(mSecretKeyRings, new SecretKeySorter());
        } else {
            Collections.sort(mPublicKeyRings, new PublicKeySorter());
        }
    }

    public static Date getCreationDate(PGPPublicKey key) {
        return key.getCreationTime();
    }

    public static Date getCreationDate(PGPSecretKey key) {
        return key.getPublicKey().getCreationTime();
    }

    public static PGPPublicKey getMasterKey(PGPPublicKeyRing keyRing) {
        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
            if (key.isMasterKey()) {
                return key;
            }
        }

        return null;
    }

    public static PGPSecretKey getMasterKey(PGPSecretKeyRing keyRing) {
        for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
            if (key.isMasterKey()) {
                return key;
            }
        }

        return null;
    }

    public static Vector<PGPPublicKey> getEncryptKeys(PGPPublicKeyRing keyRing) {
        Vector<PGPPublicKey> encryptKeys = new Vector<PGPPublicKey>();

        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
            if (isEncryptionKey(key)) {
                encryptKeys.add(key);
            }
        }

        return encryptKeys;
    }

    public static Vector<PGPSecretKey> getSigningKeys(PGPSecretKeyRing keyRing) {
        Vector<PGPSecretKey> signingKeys = new Vector<PGPSecretKey>();

        for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
            if (isSigningKey(key)) {
                signingKeys.add(key);
            }
        }

        return signingKeys;
    }

    public static Vector<PGPPublicKey> getUsableEncryptKeys(PGPPublicKeyRing keyRing) {
        Vector<PGPPublicKey> usableKeys = new Vector<PGPPublicKey>();
        Vector<PGPPublicKey> encryptKeys = getEncryptKeys(keyRing);
        PGPPublicKey masterKey = null;
        for (int i = 0; i < encryptKeys.size(); ++i) {
            PGPPublicKey key = encryptKeys.get(i);
            if (!isExpired(key)) {
                if (key.isMasterKey()) {
                    masterKey = key;
                } else {
                    usableKeys.add(key);
                }
            }
        }
        if (masterKey != null) {
            usableKeys.add(masterKey);
        }
        return usableKeys;
    }

    public static boolean isExpired(PGPPublicKey key) {
        Date creationDate = getCreationDate(key);
        Date expiryDate = getExpiryDate(key);
        Date now = new Date();
        if (now.compareTo(creationDate) >= 0 &&
            (expiryDate == null || now.compareTo(expiryDate) <= 0)) {
            return false;
        }
        return true;
    }

    public static boolean isExpired(PGPSecretKey key) {
        return isExpired(key.getPublicKey());
    }

    public static Vector<PGPSecretKey> getUsableSigningKeys(PGPSecretKeyRing keyRing) {
        Vector<PGPSecretKey> usableKeys = new Vector<PGPSecretKey>();
        Vector<PGPSecretKey> signingKeys = getSigningKeys(keyRing);
        PGPSecretKey masterKey = null;
        for (int i = 0; i < signingKeys.size(); ++i) {
            PGPSecretKey key = signingKeys.get(i);
            if (key.isMasterKey()) {
                masterKey = key;
            } else {
                usableKeys.add(key);
            }
        }
        if (masterKey != null) {
            usableKeys.add(masterKey);
        }
        return usableKeys;
    }

    public static Date getExpiryDate(PGPPublicKey key) {
        Date creationDate = getCreationDate(key);
        if (key.getValidDays() == 0) {
            // no expiry
            return null;
        }
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(creationDate);
        calendar.add(Calendar.DATE, key.getValidDays());
        Date expiryDate = calendar.getTime();

        return expiryDate;
    }

    public static Date getExpiryDate(PGPSecretKey key) {
        return getExpiryDate(key.getPublicKey());
    }

    public static PGPPublicKey getEncryptPublicKey(long masterKeyId) {
        PGPPublicKeyRing keyRing = mPublicKeyIdToKeyRingMap.get(masterKeyId);
        if (keyRing == null) {
            return null;
        }
        Vector<PGPPublicKey> encryptKeys = getUsableEncryptKeys(keyRing);
        if (encryptKeys.size() == 0) {
            return null;
        }
        return encryptKeys.get(0);
    }

    public static PGPSecretKey getSigningKey(long masterKeyId) {
        PGPSecretKeyRing keyRing = mSecretKeyIdToKeyRingMap.get(masterKeyId);
        if (keyRing == null) {
            return null;
        }
        Vector<PGPSecretKey> signingKeys = getUsableSigningKeys(keyRing);
        if (signingKeys.size() == 0) {
            return null;
        }
        return signingKeys.get(0);
    }

    public static String getMainUserId(PGPPublicKey key) {
        for (String userId : new IterableIterator<String>(key.getUserIDs())) {
            return userId;
        }
        return null;
    }

    public static String getMainUserId(PGPSecretKey key) {
        for (String userId : new IterableIterator<String>(key.getUserIDs())) {
            return userId;
        }
        return null;
    }

    public static String getMainUserIdSafe(Context context, PGPPublicKey key) {
        String userId = getMainUserId(key);
        if (userId == null) {
            userId = context.getResources().getString(R.string.unknownUserId);
        }
        return userId;
    }

    public static String getMainUserIdSafe(Context context, PGPSecretKey key) {
        String userId = getMainUserId(key);
        if (userId == null) {
            userId = context.getResources().getString(R.string.unknownUserId);
        }
        return userId;
    }

    public static PGPPublicKeyRing getPublicKeyRing(long keyId) {
        return mPublicKeyIdToKeyRingMap.get(keyId);
    }

    public static PGPSecretKeyRing getSecretKeyRing(long keyId) {
        return mSecretKeyIdToKeyRingMap.get(keyId);
    }

    public static boolean isEncryptionKey(PGPPublicKey key) {
        if (!key.isEncryptionKey()) {
            return false;
        }

        if (key.getVersion() <= 3) {
            // this must be true now
            return key.isEncryptionKey();
        }

        // special cases
        if (key.getAlgorithm() == PGPPublicKey.ELGAMAL_ENCRYPT) {
            return true;
        }

        if (key.getAlgorithm() == PGPPublicKey.RSA_ENCRYPT) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(key.getSignatures())) {
            if (key.isMasterKey() && sig.getKeyID() != key.getKeyID()) {
                continue;
            }
            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();

            if (hashed != null &&(hashed.getKeyFlags() &
                                  (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0) {
                return true;
            }

            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();

            if (unhashed != null &&(unhashed.getKeyFlags() &
                                  (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEncryptionKey(PGPSecretKey key) {
        return isEncryptionKey(key.getPublicKey());
    }

    public static boolean isSigningKey(PGPPublicKey key) {
        if (key.getVersion() <= 3) {
            return true;
        }

        // special case
        if (key.getAlgorithm() == PGPPublicKey.RSA_SIGN) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(key.getSignatures())) {
            if (key.isMasterKey() && sig.getKeyID() != key.getKeyID()) {
                continue;
            }
            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();

            if (hashed != null && (hashed.getKeyFlags() & KeyFlags.SIGN_DATA) != 0) {
                return true;
            }

            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();

            if (unhashed != null && (unhashed.getKeyFlags() & KeyFlags.SIGN_DATA) != 0) {
                return true;
            }
        }

        return false;
    }

    public static boolean isSigningKey(PGPSecretKey key) {
        return isSigningKey(key.getPublicKey());
    }

    public static String getAlgorithmInfo(PGPPublicKey key) {
        String algorithmStr = null;

        switch (key.getAlgorithm()) {
            case PGPPublicKey.RSA_ENCRYPT:
            case PGPPublicKey.RSA_GENERAL:
            case PGPPublicKey.RSA_SIGN: {
                algorithmStr = "RSA";
                break;
            }

            case PGPPublicKey.DSA: {
                algorithmStr = "DSA";
                break;
            }

            case PGPPublicKey.ELGAMAL_ENCRYPT:
            case PGPPublicKey.ELGAMAL_GENERAL: {
                algorithmStr = "ElGamal";
                break;
            }

            default: {
                algorithmStr = "???";
                break;
            }
        }
        return algorithmStr + ", " + key.getBitStrength() + "bit";
    }

    public static String getAlgorithmInfo(PGPSecretKey key) {
        return getAlgorithmInfo(key.getPublicKey());
    }

    public static void deleteKey(Activity context, PGPPublicKeyRing keyRing) {
        PGPPublicKey masterKey = getMasterKey(keyRing);
        Uri uri = Uri.withAppendedPath(PublicKeys.CONTENT_URI_BY_KEY_ID, "" + masterKey.getKeyID());
        context.getContentResolver().delete(uri, null, null);
        loadKeyRings(context, Id.type.public_key);
    }

    public static void deleteKey(Activity context, PGPSecretKeyRing keyRing) {
        PGPSecretKey masterKey = getMasterKey(keyRing);
        Uri uri = Uri.withAppendedPath(SecretKeys.CONTENT_URI_BY_KEY_ID, "" + masterKey.getKeyID());
        context.getContentResolver().delete(uri, null, null);
        loadKeyRings(context, Id.type.secret_key);
    }

    public static PGPPublicKey findPublicKey(long keyId) {
        PGPPublicKey key = null;
        for (int i = 0; i < mPublicKeyRings.size(); ++i) {
            PGPPublicKeyRing keyRing = mPublicKeyRings.get(i);
            try {
                key = keyRing.getPublicKey(keyId);
                if (key != null) {
                    return key;
                }
            } catch (PGPException e) {
                // just not found, can ignore this
            }
        }
        return null;
    }

    public static PGPSecretKey findSecretKey(long keyId) {
        PGPSecretKey key = null;
        for (int i = 0; i < mSecretKeyRings.size(); ++i) {
            PGPSecretKeyRing keyRing = mSecretKeyRings.get(i);
            key = keyRing.getSecretKey(keyId);
            if (key != null) {
                return key;
            }
        }
        return null;
    }

    public static PGPSecretKeyRing findSecretKeyRing(long keyId) {
        for (int i = 0; i < mSecretKeyRings.size(); ++i) {
            PGPSecretKeyRing keyRing = mSecretKeyRings.get(i);
            PGPSecretKey key = null;
            key = keyRing.getSecretKey(keyId);
            if (key != null) {
                return keyRing;
            }
        }
        return null;
    }

    public static PGPPublicKeyRing findPublicKeyRing(long keyId) {
        for (int i = 0; i < mPublicKeyRings.size(); ++i) {
            PGPPublicKeyRing keyRing = mPublicKeyRings.get(i);
            PGPPublicKey key = null;
            try {
                key = keyRing.getPublicKey(keyId);
                if (key != null) {
                    return keyRing;
                }
            } catch (PGPException e) {
                // key not found
            }
        }
        return null;
    }

    public static PGPPublicKey getPublicMasterKey(long keyId) {
        PGPPublicKey key = null;
        for (int i = 0; i < mPublicKeyRings.size(); ++i) {
            PGPPublicKeyRing keyRing = mPublicKeyRings.get(i);
            try {
                key = keyRing.getPublicKey(keyId);
                if (key != null) {
                    return getMasterKey(keyRing);
                }
            } catch (PGPException e) {
                // just not found, can ignore this
            }
        }
        return null;
    }

    public static void encrypt(Context context,
                               InputStream inStream, OutputStream outStream,
                               long dataLength,
                               boolean armored,
                               long encryptionKeyIds[], long signatureKeyId,
                               String signaturePassPhrase,
                               ProgressDialogUpdater progress,
                               int symmetricAlgorithm, int hashAlgorithm, int compression,
                               String passPhrase)
            throws IOException, GeneralException, PGPException, NoSuchProviderException,
            NoSuchAlgorithmException, SignatureException {
        Security.addProvider(new BouncyCastleProvider());

        if (encryptionKeyIds == null) {
            encryptionKeyIds = new long[0];
        }

        ArmoredOutputStream armorOut = null;
        OutputStream out = null;
        OutputStream encryptOut = null;
        if (armored) {
            armorOut = new ArmoredOutputStream(outStream);
            armorOut.setHeader("Version", FULL_VERSION);
            out = armorOut;
        } else {
            out = outStream;
        }
        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;

        if (encryptionKeyIds.length == 0 && passPhrase == null) {
            throw new GeneralException(context.getString(R.string.error_noEncryptionKeysOrPassPhrase));
        }

        if (signatureKeyId != 0) {
            signingKeyRing = findSecretKeyRing(signatureKeyId);
            signingKey = getSigningKey(signatureKeyId);
            if (signingKey == null) {
                throw new GeneralException(context.getString(R.string.error_signatureFailed));
            }

            if (signaturePassPhrase == null) {
                throw new GeneralException(context.getString(R.string.error_noSignaturePassPhrase));
            }
            progress.setProgress(R.string.progress_extractingSignatureKey, 0, 100);
            signaturePrivateKey = signingKey.extractPrivateKey(signaturePassPhrase.toCharArray(),
                                                               new BouncyCastleProvider());
        }

        PGPSignatureGenerator signatureGenerator = null;
        progress.setProgress(R.string.progress_preparingStreams, 5, 100);
        // encrypt and compress input file content
        PGPEncryptedDataGenerator cPk =
                new PGPEncryptedDataGenerator(symmetricAlgorithm, true, new SecureRandom(),
                                              new BouncyCastleProvider());

        if (encryptionKeyIds.length == 0) {
            // symmetric encryption
            cPk.addMethod(passPhrase.toCharArray());
        }
        for (int i = 0; i < encryptionKeyIds.length; ++i) {
            PGPPublicKey key = getEncryptPublicKey(encryptionKeyIds[i]);
            if (key != null) {
                cPk.addMethod(key);
            }
        }
        encryptOut = cPk.open(out, new byte[1 << 16]);

        if (signatureKeyId != 0) {
            progress.setProgress(R.string.progress_preparingSignature, 10, 100);
            signatureGenerator =
                    new PGPSignatureGenerator(signingKey.getPublicKey().getAlgorithm(),
                                              hashAlgorithm,
                                              new BouncyCastleProvider());
            signatureGenerator.initSign(PGPSignature.BINARY_DOCUMENT, signaturePrivateKey);
            String userId = getMainUserId(getMasterKey(signingKeyRing));

            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            spGen.setSignerUserID(false, userId);
            signatureGenerator.setHashedSubpackets(spGen.generate());
        }

        PGPCompressedDataGenerator compressGen = null;
        BCPGOutputStream bcpgOut = null;
        if (compression == Id.choice.compression.none) {
            bcpgOut = new BCPGOutputStream(encryptOut);
        } else {
            compressGen = new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZLIB);
            bcpgOut = new BCPGOutputStream(compressGen.open(encryptOut));
        }
        if (signatureKeyId != 0) {
            signatureGenerator.generateOnePassVersion(false).encode(bcpgOut);
        }

        PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
        // file name not needed, so empty string
        OutputStream pOut = literalGen.open(bcpgOut, PGPLiteralData.BINARY, "",
                                            new Date(), new byte[1 << 16]);

        progress.setProgress(R.string.progress_encrypting, 20, 100);
        long done = 0;
        int n = 0;
        byte[] buffer = new byte[1 << 16];
        while ((n = inStream.read(buffer)) > 0) {
            pOut.write(buffer, 0, n);
            if (signatureKeyId != 0) {
                signatureGenerator.update(buffer, 0, n);
            }
            done += n;
            if (dataLength != 0) {
                progress.setProgress((int) (20 + (95 - 20) * done / dataLength), 100);
            }
        }

        literalGen.close();

        if (signatureKeyId != 0) {
            progress.setProgress(R.string.progress_generatingSignature, 95, 100);
            signatureGenerator.generate().encode(pOut);
        }
        if (compressGen != null) {
            compressGen.close();
        }
        encryptOut.close();
        if (armored) {
            armorOut.close();
        }

        progress.setProgress(R.string.progress_done, 100, 100);
    }

    public static void signText(Context context,
                                InputStream inStream, OutputStream outStream,
                                long signatureKeyId, String signaturePassPhrase,
                                int hashAlgorithm,
                                ProgressDialogUpdater progress)
            throws GeneralException, PGPException, IOException, NoSuchAlgorithmException,
            SignatureException {
        Security.addProvider(new BouncyCastleProvider());

        ArmoredOutputStream armorOut = new ArmoredOutputStream(outStream);
        armorOut.setHeader("Version", FULL_VERSION);

        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;

        if (signatureKeyId == 0) {
            throw new GeneralException(context.getString(R.string.error_noSignatureKey));
        }

        signingKeyRing = findSecretKeyRing(signatureKeyId);
        signingKey = getSigningKey(signatureKeyId);
        if (signingKey == null) {
            throw new GeneralException(context.getString(R.string.error_signatureFailed));
        }

        if (signaturePassPhrase == null) {
            throw new GeneralException(context.getString(R.string.error_noSignaturePassPhrase));
        }
        signaturePrivateKey =
                signingKey.extractPrivateKey(signaturePassPhrase.toCharArray(),
                                             new BouncyCastleProvider());

        PGPSignatureGenerator signatureGenerator = null;
        progress.setProgress(R.string.progress_preparingStreams, 0, 100);

        progress.setProgress(R.string.progress_preparingSignature, 30, 100);
        signatureGenerator =
                new PGPSignatureGenerator(signingKey.getPublicKey().getAlgorithm(),
                                          hashAlgorithm,
                                          new BouncyCastleProvider());
        signatureGenerator.initSign(PGPSignature.CANONICAL_TEXT_DOCUMENT, signaturePrivateKey);
        String userId = getMainUserId(getMasterKey(signingKeyRing));

        PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
        spGen.setSignerUserID(false, userId);
        signatureGenerator.setHashedSubpackets(spGen.generate());

        progress.setProgress(R.string.progress_signing, 40, 100);

        armorOut.beginClearText(hashAlgorithm);

        ByteArrayOutputStream lineOut = new ByteArrayOutputStream();
        int lookAhead = readInputLine(lineOut, inStream);

        processLine(armorOut, signatureGenerator, lineOut.toByteArray());

        if (lookAhead != -1) {
            do {
                lookAhead = readInputLine(lineOut, lookAhead, inStream);

                signatureGenerator.update((byte)'\r');
                signatureGenerator.update((byte)'\n');

                processLine(armorOut, signatureGenerator, lineOut.toByteArray());
            }
            while (lookAhead != -1);
        }

        armorOut.endClearText();

        BCPGOutputStream bOut = new BCPGOutputStream(armorOut);
        signatureGenerator.generate().encode(bOut);
        armorOut.close();

        progress.setProgress(R.string.progress_done, 100, 100);
    }

    public static long getDecryptionKeyId(Context context, InputStream inStream)
            throws GeneralException, NoAsymmetricEncryptionException, IOException {
        InputStream in = PGPUtil.getDecoderStream(inStream);
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
        Iterator it = enc.getEncryptedDataObjects();
        boolean gotAsymmetricEncryption = false;
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof PGPPublicKeyEncryptedData) {
                gotAsymmetricEncryption = true;
                PGPPublicKeyEncryptedData pbe = (PGPPublicKeyEncryptedData) obj;
                secretKey = findSecretKey(pbe.getKeyID());
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

    public static boolean hasSymmetricEncryption(Context context, InputStream inStream)
            throws GeneralException, IOException {
        InputStream in = PGPUtil.getDecoderStream(inStream);
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

        Iterator it = enc.getEncryptedDataObjects();
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof PGPPBEEncryptedData) {
                return true;
            }
        }

        return false;
    }

    public static Bundle decrypt(Context context,
                                 PositionAwareInputStream inStream, OutputStream outStream,
                                 long dataLength,
                                 String passPhrase, ProgressDialogUpdater progress,
                                 boolean assumeSymmetric)
            throws IOException, GeneralException, PGPException, SignatureException {
        Bundle returnData = new Bundle();
        InputStream in = PGPUtil.getDecoderStream(inStream);
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject();
        long signatureKeyId = 0;

        int currentProgress = 0;
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
            Iterator it = enc.getEncryptedDataObjects();
            // find secret key
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof PGPPBEEncryptedData) {
                    pbe = (PGPPBEEncryptedData) obj;
                    break;
                }
            }

            if (pbe == null) {
                throw new GeneralException(context.getString(R.string.error_noSymmetricEncryptionPacket));
            }

            progress.setProgress(R.string.progress_preparingStreams, currentProgress, 100);
            clear = pbe.getDataStream(passPhrase.toCharArray(), new BouncyCastleProvider());
            encryptedData = pbe;
            currentProgress += 5;
        } else {
            progress.setProgress(R.string.progress_findingKey, currentProgress, 100);
            PGPPublicKeyEncryptedData pbe = null;
            PGPSecretKey secretKey = null;
            Iterator it = enc.getEncryptedDataObjects();
            // find secret key
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof PGPPublicKeyEncryptedData) {
                    PGPPublicKeyEncryptedData encData = (PGPPublicKeyEncryptedData) obj;
                    secretKey = findSecretKey(encData.getKeyID());
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
            progress.setProgress(R.string.progress_extractingKey, currentProgress, 100);
            PGPPrivateKey privateKey = null;
            try {
                privateKey = secretKey.extractPrivateKey(passPhrase.toCharArray(),
                                                         new BouncyCastleProvider());
            } catch (PGPException e) {
                throw new PGPException(context.getString(R.string.error_wrongPassPhrase));
            }
            currentProgress += 5;
            progress.setProgress(R.string.progress_preparingStreams, currentProgress, 100);
            clear = pbe.getDataStream(privateKey, new BouncyCastleProvider());
            encryptedData = pbe;
            currentProgress += 5;
        }

        PGPObjectFactory plainFact = new PGPObjectFactory(clear);
        Object dataChunk = plainFact.nextObject();
        PGPOnePassSignature signature = null;
        PGPPublicKey signatureKey = null;
        int signatureIndex = -1;

        if (dataChunk instanceof PGPCompressedData) {
            progress.setProgress(R.string.progress_decompressingData, currentProgress, 100);
            PGPObjectFactory fact =
                    new PGPObjectFactory(((PGPCompressedData) dataChunk).getDataStream());
            dataChunk = fact.nextObject();
            plainFact = fact;
            currentProgress += 10;
        }

        if (dataChunk instanceof PGPOnePassSignatureList) {
            progress.setProgress(R.string.progress_processingSignature, currentProgress, 100);
            returnData.putBoolean("signature", true);
            PGPOnePassSignatureList sigList = (PGPOnePassSignatureList) dataChunk;
            for (int i = 0; i < sigList.size(); ++i) {
                signature = sigList.get(i);
                signatureKey = findPublicKey(signature.getKeyID());
                if (signatureKeyId == 0) {
                    signatureKeyId = signature.getKeyID();
                }
                if (signatureKey == null) {
                    signature = null;
                } else {
                    signatureIndex = i;
                    signatureKeyId = signature.getKeyID();
                    String userId = null;
                    PGPPublicKeyRing sigKeyRing = findPublicKeyRing(signatureKeyId);
                    if (sigKeyRing != null) {
                        userId = getMainUserId(getMasterKey(sigKeyRing));
                    }
                    returnData.putString("signatureUserId", userId);
                    break;
                }
            }

            returnData.putLong("signatureKeyId", signatureKeyId);

            if (signature != null) {
                signature.initVerify(signatureKey, new BouncyCastleProvider());
            } else {
                returnData.putBoolean("signatureUnknown", true);
            }

            dataChunk = plainFact.nextObject();
            currentProgress += 10;
        }

        if (dataChunk instanceof PGPLiteralData) {
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
            long startPos = inStream.position();
            while ((n = dataIn.read(buffer)) > 0) {
                out.write(buffer, 0, n);
                done += n;
                if (signature != null) {
                    try {
                        signature.update(buffer, 0, n);
                    } catch (SignatureException e) {
                        returnData.putBoolean("signatureSuccess", false);
                        signature = null;
                    }
                }
                // unknown size, but try to at least have a moving, slowing down progress bar
                currentProgress = startProgress + (endProgress - startProgress) * done / (done + 100000);
                if (dataLength - startPos == 0) {
                    currentProgress = endProgress;
                } else {
                    currentProgress = (int)(startProgress + (endProgress - startProgress) *
                                        (inStream.position() - startPos) / (dataLength - startPos));
                }
                progress.setProgress(currentProgress, 100);
            }

            if (signature != null) {
                progress.setProgress(R.string.progress_verifyingSignature, 90, 100);
                PGPSignatureList signatureList = (PGPSignatureList) plainFact.nextObject();
                PGPSignature messageSignature = (PGPSignature) signatureList.get(signatureIndex);
                if (signature.verify(messageSignature)) {
                    returnData.putBoolean("signatureSuccess", true);
                } else {
                    returnData.putBoolean("signatureSuccess", false);
                }
            }
        }

        // TODO: add integrity somewhere
        if (encryptedData.isIntegrityProtected()) {
            progress.setProgress(R.string.progress_verifyingIntegrity, 95, 100);
            if (encryptedData.verify()) {
                // passed
            } else {
                // failed
            }
        } else {
            // no integrity check
        }

        progress.setProgress(R.string.progress_done, 100, 100);
        return returnData;
    }

    public static Bundle verifyText(Context context,
                                    InputStream inStream, OutputStream outStream,
                                    ProgressDialogUpdater progress)
            throws IOException, GeneralException, PGPException, SignatureException {
        Bundle returnData = new Bundle();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ArmoredInputStream aIn = new ArmoredInputStream(inStream);

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

        returnData.putBoolean("signature", true);

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
            signatureKey = findPublicKey(signature.getKeyID());
            if (signatureKeyId == 0) {
                signatureKeyId = signature.getKeyID();
            }
            if (signatureKey == null) {
                signature = null;
            } else {
                signatureKeyId = signature.getKeyID();
                String userId = null;
                PGPPublicKeyRing sigKeyRing = findPublicKeyRing(signatureKeyId);
                if (sigKeyRing != null) {
                    userId = getMainUserId(getMasterKey(sigKeyRing));
                }
                returnData.putString("signatureUserId", userId);
                break;
            }
        }

        returnData.putLong("signatureKeyId", signatureKeyId);

        if (signature == null) {
            returnData.putBoolean("signatureUnknown", true);
            progress.setProgress(R.string.progress_done, 100, 100);
            return returnData;
        }

        signature.initVerify(signatureKey, new BouncyCastleProvider());

        InputStream sigIn = new BufferedInputStream(new ByteArrayInputStream(clearText));

        lookAhead = readInputLine(lineOut, sigIn);

        processLine(signature, lineOut.toByteArray());

        if (lookAhead != -1) {
            do {
                lookAhead = readInputLine(lineOut, lookAhead, sigIn);

                signature.update((byte)'\r');
                signature.update((byte)'\n');

                processLine(signature, lineOut.toByteArray());
            }
            while (lookAhead != -1);
        }

        returnData.putBoolean("signatureSuccess", signature.verify());

        progress.setProgress(R.string.progress_done, 100, 100);
        return returnData;
    }

    public static Vector<PGPPublicKeyRing> getPublicKeyRings() {
        return mPublicKeyRings;
    }

    public static Vector<PGPSecretKeyRing> getSecretKeyRings() {
        return mSecretKeyRings;
    }


    // taken from ClearSignedFileProcessor in BC
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
        }
        while ((ch = fIn.read()) >= 0);

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

    private static void processLine(PGPSignature sig, byte[] line)
        throws SignatureException, IOException {
        int length = getLengthWithoutWhiteSpace(line);
        if (length > 0) {
            sig.update(line, 0, length);
        }
    }

    private static void processLine(OutputStream aOut, PGPSignatureGenerator sGen, byte[] line)
        throws SignatureException, IOException {
        int length = getLengthWithoutWhiteSpace(line);
        if (length > 0) {
            sGen.update(line, 0, length);
        }

        aOut.write(line, 0, line.length);
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
            nlBytes[i] = (byte)nl.charAt(i);
        }

        return nlBytes;
    }
}
