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
import java.io.BufferedOutputStream;
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
    }

    public static String VERSION = "0.9.0";
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

    protected static Vector<PGPPublicKeyRing> mPublicKeyRings;
    protected static Vector<PGPSecretKeyRing> mSecretKeyRings;

    public static Pattern PGP_MESSAGE =
            Pattern.compile(".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*",
                            Pattern.DOTALL);

    public static Pattern PGP_SIGNED_MESSAGE =
        Pattern.compile(".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
                        Pattern.DOTALL);

    protected static boolean mInitialized = false;

    protected static final int RETURN_NO_MASTER_KEY = -2;
    protected static final int RETURN_ERROR = -1;
    protected static final int RETURN_OK = 0;
    protected static final int RETURN_UPDATED = 1;

    protected static final int TYPE_PUBLIC = 0;
    protected static final int TYPE_SECRET = 1;

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

    private static String mPassPhrase = null;

    public static class GeneralException extends Exception {
        static final long serialVersionUID = 0xf812773342L;

        public GeneralException(String message) {
            super(message);
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
        setPassPhrase(null);
        if (mInitialized) {
            return;
        }

        loadKeyRings(context, TYPE_PUBLIC);
        loadKeyRings(context, TYPE_SECRET);

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

    public static void setPassPhrase(String passPhrase) {
        mPassPhrase = passPhrase;
    }

    public static String getPassPhrase() {
        return mPassPhrase;
    }

    public static PGPSecretKey createKey(KeyEditor.AlgorithmChoice algorithmChoice, int keySize,
                                         String passPhrase)
                  throws NoSuchAlgorithmException, PGPException, NoSuchProviderException,
                  GeneralException, InvalidAlgorithmParameterException {

        if (algorithmChoice == null) {
            throw new GeneralException("unknown algorithm choice");
        }
        if (keySize < 512) {
            throw new GeneralException("key size must be at least 512bit");
        }

        Security.addProvider(new BouncyCastleProvider());

        if (passPhrase == null) {
            passPhrase = "";
        }

        int algorithm = 0;
        KeyPairGenerator keyGen = null;

        switch (algorithmChoice.getId()) {
            case KeyEditor.AlgorithmChoice.DSA: {
                keyGen = KeyPairGenerator.getInstance("DSA", new BouncyCastleProvider());
                keyGen.initialize(keySize, new SecureRandom());
                algorithm = PGPPublicKey.DSA;
                break;
            }

            case KeyEditor.AlgorithmChoice.ELGAMAL: {
                if (keySize != 2048) {
                    throw new GeneralException("ElGamal currently requires 2048bit");
                }
                keyGen = KeyPairGenerator.getInstance("ELGAMAL", new BouncyCastleProvider());
                BigInteger p = new BigInteger(
                "36F0255DDE973DCB3B399D747F23E32ED6FDB1F77598338BFDF44159C4EC64DDAEB5F78671CBFB22" +
                "106AE64C32C5BCE4CFD4F5920DA0EBC8B01ECA9292AE3DBA1B7A4A899DA181390BB3BD1659C81294" +
                "F400A3490BF9481211C79404A576605A5160DBEE83B4E019B6D799AE131BA4C23DFF83475E9C40FA" +
                "6725B7C9E3AA2C6596E9C05702DB30A07C9AA2DC235C5269E39D0CA9DF7AAD44612AD6F88F696992" +
                "98F3CAB1B54367FB0E8B93F735E7DE83CD6FA1B9D1C931C41C6188D3E7F179FC64D87C5D13F85D70" +
                "4A3AA20F90B3AD3621D434096AA7E8E7C66AB683156A951AEA2DD9E76705FAEFEA8D71A575535597" +
                "0000000000000001", 16);
                ElGamalParameterSpec elParams = new ElGamalParameterSpec(p, new BigInteger("2"));
                keyGen.initialize(elParams);
                algorithm = PGPPublicKey.ELGAMAL_GENERAL;
                break;
            }

            case KeyEditor.AlgorithmChoice.RSA: {
                keyGen = KeyPairGenerator.getInstance("RSA", new BouncyCastleProvider());
                keyGen.initialize(keySize, new SecureRandom());

                algorithm = PGPPublicKey.RSA_GENERAL;
                break;
            }

            default: {
                throw new GeneralException("unknown algorithm choice");
            }
        }

        PGPKeyPair keyPair = new PGPKeyPair(algorithm, keyGen.generateKeyPair(), new Date());

        // enough for now, as we assemble the key again later anyway
        PGPSecretKey secretKey =
                new PGPSecretKey(PGPSignature.DEFAULT_CERTIFICATION, keyPair, "",
                                 PGPEncryptedData.CAST5, passPhrase.toCharArray(), null, null,
                                 new SecureRandom(), new BouncyCastleProvider().getName());

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

        progress.setProgress("building key...", 0, 100);

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
                throw new Apg.GeneralException("you need to specify a name");
            } catch (UserIdEditor.NoEmailException e) {
                throw new Apg.GeneralException("you need to specify an email");
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
            throw new Apg.GeneralException("need at least one user id");
        }

        if (!gotMainUserId) {
            throw new Apg.GeneralException("main user id can't be empty");
        }

        if (keyEditors.getChildCount() == 0) {
            throw new Apg.GeneralException("need at least a main key");
        }

        for (int i = 0; i < keyEditors.getChildCount(); ++i) {
            KeyEditor editor = (KeyEditor)keyEditors.getChildAt(i);
            keys.add(editor.getValue());
        }

        progress.setProgress("preparing master key...", 10, 100);
        KeyEditor keyEditor = (KeyEditor) keyEditors.getChildAt(0);
        int usageId = keyEditor.getUsage().getId();
        boolean canSign = (usageId == KeyEditor.UsageChoice.SIGN_ONLY ||
                           usageId == KeyEditor.UsageChoice.SIGN_AND_ENCRYPT);
        boolean canEncrypt = (usageId == KeyEditor.UsageChoice.ENCRYPT_ONLY ||
                              usageId == KeyEditor.UsageChoice.SIGN_AND_ENCRYPT);

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

        progress.setProgress("certifying master key...", 20, 100);
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
                throw new GeneralException("expiry date must be later than creation date");
            }
            hashedPacketsGen.setKeyExpirationTime(true, numDays * 86400);
        }

        progress.setProgress("building master key ring...", 30, 100);
        PGPKeyRingGenerator keyGen =
                new PGPKeyRingGenerator(PGPSignature.DEFAULT_CERTIFICATION,
                                        masterKeyPair, mainUserId,
                                        PGPEncryptedData.CAST5, newPassPhrase.toCharArray(),
                                        hashedPacketsGen.generate(), unhashedPacketsGen.generate(),
                                        new SecureRandom(), new BouncyCastleProvider().getName());

        progress.setProgress("adding sub keys...", 40, 100);
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
            usageId = keyEditor.getUsage().getId();
            canSign = (usageId == KeyEditor.UsageChoice.SIGN_ONLY ||
                       usageId == KeyEditor.UsageChoice.SIGN_AND_ENCRYPT);
            canEncrypt = (usageId == KeyEditor.UsageChoice.ENCRYPT_ONLY ||
                          usageId == KeyEditor.UsageChoice.SIGN_AND_ENCRYPT);
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
                    throw new GeneralException("expiry date must be later than creation date");
                }
                hashedPacketsGen.setKeyExpirationTime(true, numDays * 86400);
            }

            keyGen.addSubKey(subKeyPair,
                             hashedPacketsGen.generate(), unhashedPacketsGen.generate());
        }

        PGPSecretKeyRing secretKeyRing = keyGen.generateSecretKeyRing();
        PGPPublicKeyRing publicKeyRing = keyGen.generatePublicKeyRing();

        progress.setProgress("saving key ring...", 90, 100);
        saveKeyRing(context, secretKeyRing);
        saveKeyRing(context, publicKeyRing);

        loadKeyRings(context, TYPE_PUBLIC);
        loadKeyRings(context, TYPE_SECRET);
        progress.setProgress("done.", 100, 100);
    }

    private static int saveKeyRing(Activity context, PGPPublicKeyRing keyRing) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ContentValues values = new ContentValues();

        PGPPublicKey masterKey = getMasterKey(keyRing);
        if (masterKey == null) {
            return RETURN_NO_MASTER_KEY;
        }

        try {
            keyRing.encode(out);
            out.close();
        } catch (IOException e) {
            return RETURN_ERROR;
        }

        values.put(PublicKeys.KEY_ID, masterKey.getKeyID());
        values.put(PublicKeys.KEY_DATA, out.toByteArray());

        Uri uri = Uri.withAppendedPath(PublicKeys.CONTENT_URI_BY_KEY_ID, "" + masterKey.getKeyID());
        Cursor cursor = context.managedQuery(uri, PUBLIC_KEY_PROJECTION, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            context.getContentResolver().update(uri, values, null, null);
            return RETURN_UPDATED;
        } else {
            context.getContentResolver().insert(PublicKeys.CONTENT_URI, values);
            return RETURN_OK;
        }
    }

    private static int saveKeyRing(Activity context, PGPSecretKeyRing keyRing) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ContentValues values = new ContentValues();

        PGPSecretKey masterKey = getMasterKey(keyRing);
        if (masterKey == null) {
            return RETURN_NO_MASTER_KEY;
        }

        try {
            keyRing.encode(out);
            out.close();
        } catch (IOException e) {
            return RETURN_ERROR;
        }

        values.put(SecretKeys.KEY_ID, masterKey.getKeyID());
        values.put(SecretKeys.KEY_DATA, out.toByteArray());

        Uri uri = Uri.withAppendedPath(SecretKeys.CONTENT_URI_BY_KEY_ID, "" + masterKey.getKeyID());
        Cursor cursor = context.managedQuery(uri, SECRET_KEY_PROJECTION, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            context.getContentResolver().update(uri, values, null, null);
            return RETURN_UPDATED;
        } else {
            context.getContentResolver().insert(SecretKeys.CONTENT_URI, values);
            return RETURN_OK;
        }
    }

    public static Bundle importKeyRings(Activity context, int type, String filename,
                                        ProgressDialogUpdater progress)
            throws GeneralException, FileNotFoundException, PGPException, IOException {
        Bundle returnData = new Bundle();
        PGPObjectFactory objectFactors = null;

        if (type == TYPE_SECRET) {
            progress.setProgress("importing secret keys...", 0, 100);
        } else {
            progress.setProgress("importing public keys...", 0, 100);
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new GeneralException("external storage not ready");
        }

        FileInputStream fileIn = new FileInputStream(filename);
        InputStream in = PGPUtil.getDecoderStream(fileIn);
        objectFactors = new PGPObjectFactory(in);

        Vector<Object> objects = new Vector<Object>();
        Object obj = objectFactors.nextObject();
        while (obj != null) {
            objects.add(obj);
            obj = objectFactors.nextObject();
        }

        int newKeys = 0;
        int oldKeys = 0;
        for (int i = 0; i < objects.size(); ++i) {
            progress.setProgress(i * 100 / objects.size(), 100);
            obj = objects.get(i);
            PGPPublicKeyRing publicKeyRing;
            PGPSecretKeyRing secretKeyRing;
            int retValue;

            if (type == TYPE_SECRET) {
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

            if (retValue == RETURN_ERROR) {
                throw new GeneralException("error saving some key(s)");
            }

            if (retValue == RETURN_UPDATED) {
                ++oldKeys;
            } else if (retValue == RETURN_OK) {
                ++newKeys;
            }
        }

        progress.setProgress("reloading keys...", 100, 100);
        loadKeyRings(context, type);

        returnData.putInt("added", newKeys);
        returnData.putInt("updated", oldKeys);

        progress.setProgress("done.", 100, 100);

        return returnData;
    }

    public static Bundle exportKeyRings(Activity context, Vector<Object> keys, String filename,
                                        ProgressDialogUpdater progress)
            throws GeneralException, FileNotFoundException, PGPException, IOException {
        Bundle returnData = new Bundle();

        if (keys.size() == 1) {
            progress.setProgress("exporting key...", 0, 100);
        } else {
            progress.setProgress("exporting keys...", 0, 100);
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new GeneralException("external storage not ready");
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

        progress.setProgress("done.", 100, 100);

        return returnData;
    }

    private static void loadKeyRings(Activity context, int type) {
        Cursor cursor;
        if (type == TYPE_SECRET) {
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
                if (type == TYPE_SECRET) {
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

        if (type == TYPE_SECRET) {
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
            userId = context.getResources().getString(R.string.unknown_user_id);
        }
        return userId;
    }

    public static String getMainUserIdSafe(Context context, PGPSecretKey key) {
        String userId = getMainUserId(key);
        if (userId == null) {
            userId = context.getResources().getString(R.string.unknown_user_id);
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

        // special case, this algorithm, no need to look further
        if (key.getAlgorithm() == PGPPublicKey.ELGAMAL_ENCRYPT) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(key.getSignatures())) {
            if (!key.isMasterKey() || sig.getKeyID() == key.getKeyID()) {
                PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();

                if ((hashed.getKeyFlags() &
                        (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0) {
                    return true;
                }
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

        for (PGPSignature sig : new IterableIterator<PGPSignature>(key.getSignatures())) {
            if (!key.isMasterKey() || sig.getKeyID() == key.getKeyID()) {
                PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();

                if ((hashed.getKeyFlags() & KeyFlags.SIGN_DATA) != 0) {
                    return true;
                }
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
        loadKeyRings(context, TYPE_PUBLIC);
    }

    public static void deleteKey(Activity context, PGPSecretKeyRing keyRing) {
        PGPSecretKey masterKey = getMasterKey(keyRing);
        Uri uri = Uri.withAppendedPath(SecretKeys.CONTENT_URI_BY_KEY_ID, "" + masterKey.getKeyID());
        context.getContentResolver().delete(uri, null, null);
        loadKeyRings(context, TYPE_SECRET);
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

    public static void encrypt(InputStream inStream, OutputStream outStream,
                               boolean armored,
                               long encryptionKeyIds[], long signatureKeyId,
                               String signaturePassPhrase,
                               ProgressDialogUpdater progress)
            throws IOException, GeneralException, PGPException, NoSuchProviderException,
            NoSuchAlgorithmException, SignatureException {
        Security.addProvider(new BouncyCastleProvider());

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

        if (encryptionKeyIds == null || encryptionKeyIds.length == 0) {
            throw new GeneralException("no encryption key(s) given");
        }

        if (signatureKeyId != 0) {
            signingKeyRing = findSecretKeyRing(signatureKeyId);
            signingKey = getSigningKey(signatureKeyId);
            if (signingKey == null) {
                throw new GeneralException("signature failed");
            }

            if (signaturePassPhrase == null) {
                throw new GeneralException("no pass phrase given");
            }
            signaturePrivateKey = signingKey.extractPrivateKey(signaturePassPhrase.toCharArray(),
                                                               new BouncyCastleProvider());
        }

        PGPSignatureGenerator signatureGenerator = null;
        progress.setProgress("preparing data...", 0, 100);

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        int n = 0;
        byte[] buffer = new byte[1 << 16];
        while ((n = inStream.read(buffer)) > 0) {
            byteOut.write(buffer, 0, n);
        }
        byteOut.close();
        byte messageData[] = byteOut.toByteArray();

        progress.setProgress("preparing streams...", 20, 100);
        // encryptFile and compress input file content
        PGPEncryptedDataGenerator cPk =
                new PGPEncryptedDataGenerator(PGPEncryptedData.AES_256, true, new SecureRandom(),
                                              new BouncyCastleProvider());

        for (int i = 0; i < encryptionKeyIds.length; ++i) {
            PGPPublicKey key = getEncryptPublicKey(encryptionKeyIds[i]);
            if (key != null) {
                cPk.addMethod(key);
            }
        }
        encryptOut = cPk.open(out, new byte[1 << 16]);

        if (signatureKeyId != 0) {
            progress.setProgress("preparing signature...", 30, 100);
            signatureGenerator =
                    new PGPSignatureGenerator(signingKey.getPublicKey().getAlgorithm(),
                                              HashAlgorithmTags.SHA1,
                                              new BouncyCastleProvider());
            signatureGenerator.initSign(PGPSignature.BINARY_DOCUMENT, signaturePrivateKey);
            String userId = getMainUserId(getMasterKey(signingKeyRing));

            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            spGen.setSignerUserID(false, userId);
            signatureGenerator.setHashedSubpackets(spGen.generate());
        }

        PGPCompressedDataGenerator compressGen =
                new PGPCompressedDataGenerator(PGPCompressedDataGenerator.ZLIB);
        BCPGOutputStream bcpgOut = new BCPGOutputStream(compressGen.open(encryptOut));
        if (signatureKeyId != 0) {
            signatureGenerator.generateOnePassVersion(false).encode(bcpgOut);
        }

        PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
        // file name not needed, so empty string
        OutputStream pOut = literalGen.open(bcpgOut, PGPLiteralData.BINARY, "",
                                            messageData.length, new Date());

        progress.setProgress("encrypting...", 40, 100);
        pOut.write(messageData);

        if (signatureKeyId != 0) {
            progress.setProgress("finishing signature...", 70, 100);
            signatureGenerator.update(messageData);
        }

        literalGen.close();

        if (signatureKeyId != 0) {
            signatureGenerator.generate().encode(pOut);
        }
        compressGen.close();
        encryptOut.close();
        out.close();

        progress.setProgress("done.", 100, 100);
    }

    public static void signText(InputStream inStream, OutputStream outStream,
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
            throw new GeneralException("no signature key given");
        }

        signingKeyRing = findSecretKeyRing(signatureKeyId);
        signingKey = getSigningKey(signatureKeyId);
        if (signingKey == null) {
            throw new GeneralException("signature failed");
        }

        if (signaturePassPhrase == null) {
            throw new GeneralException("no pass phrase given");
        }
        signaturePrivateKey =
                signingKey.extractPrivateKey(signaturePassPhrase.toCharArray(),
                                             new BouncyCastleProvider());

        PGPSignatureGenerator signatureGenerator = null;
        progress.setProgress("preparing data...", 0, 100);

        progress.setProgress("preparing signature...", 30, 100);
        signatureGenerator =
                new PGPSignatureGenerator(signingKey.getPublicKey().getAlgorithm(),
                                          hashAlgorithm,
                                          new BouncyCastleProvider());
        signatureGenerator.initSign(PGPSignature.CANONICAL_TEXT_DOCUMENT, signaturePrivateKey);
        String userId = getMainUserId(getMasterKey(signingKeyRing));

        PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
        spGen.setSignerUserID(false, userId);
        signatureGenerator.setHashedSubpackets(spGen.generate());

        progress.setProgress("signing...", 40, 100);

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

        progress.setProgress("done.", 100, 100);
    }

    public static long getDecryptionKeyId(InputStream inStream)
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
            throw new GeneralException("data not valid encryption data");
        }

        // find the secret key
        PGPSecretKey secretKey = null;
        for (PGPPublicKeyEncryptedData pbe :
                new IterableIterator<PGPPublicKeyEncryptedData>(enc.getEncryptedDataObjects())) {
            secretKey = findSecretKey(pbe.getKeyID());
            if (secretKey != null) {
                break;
            }
        }
        if (secretKey == null) {
            throw new GeneralException("couldn't find a secret key to decrypt");
        }

        return secretKey.getKeyID();
    }

    public static Bundle decrypt(InputStream inStream, OutputStream outStream,
                                 String passPhrase, ProgressDialogUpdater progress)
            throws IOException, GeneralException, PGPException, SignatureException {
        Bundle returnData = new Bundle();
        InputStream in = PGPUtil.getDecoderStream(inStream);
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject();
        long signatureKeyId = 0;

        progress.setProgress("reading data...", 0, 100);

        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        if (enc == null) {
            throw new GeneralException("data not valid encryption data");
        }

        progress.setProgress("finding key...", 10, 100);
        // find the secret key
        PGPPublicKeyEncryptedData pbe = null;
        PGPSecretKey secretKey = null;
        for (PGPPublicKeyEncryptedData encData :
                new IterableIterator<PGPPublicKeyEncryptedData>(enc.getEncryptedDataObjects())) {
            secretKey = findSecretKey(encData.getKeyID());
            if (secretKey != null) {
                pbe = encData;
                break;
            }
        }
        if (secretKey == null) {
            throw new GeneralException("couldn't find a secret key to decrypt");
        }

        progress.setProgress("extracting key...", 20, 100);
        PGPPrivateKey privateKey = null;
        try {
            privateKey = secretKey.extractPrivateKey(passPhrase.toCharArray(),
                                                     new BouncyCastleProvider());
        } catch (PGPException e) {
            throw new PGPException("wrong pass phrase");
        }
        progress.setProgress("decrypting data...", 30, 100);
        InputStream clear = pbe.getDataStream(privateKey, new BouncyCastleProvider());
        PGPObjectFactory plainFact = new PGPObjectFactory(clear);
        Object dataChunk = plainFact.nextObject();
        PGPOnePassSignature signature = null;
        PGPPublicKey signatureKey = null;
        int signatureIndex = -1;

        if (dataChunk instanceof PGPCompressedData) {
            progress.setProgress("decompressing data...", 50, 100);
            PGPObjectFactory fact =
                    new PGPObjectFactory(((PGPCompressedData) dataChunk).getDataStream());
            dataChunk = fact.nextObject();
            plainFact = fact;
        }

        if (dataChunk instanceof PGPOnePassSignatureList) {
            progress.setProgress("processing signature...", 60, 100);
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
        }

        if (dataChunk instanceof PGPLiteralData) {
            progress.setProgress("unpacking data...", 70, 100);
            PGPLiteralData literalData = (PGPLiteralData) dataChunk;
            BufferedOutputStream out = new BufferedOutputStream(outStream);

            byte[] buffer = new byte[1 << 16];
            InputStream dataIn = literalData.getInputStream();

            int bytesRead = 0;
            while ((bytesRead = dataIn.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
                if (signature != null) {
                    try {
                        signature.update(buffer, 0, bytesRead);
                    } catch (SignatureException e) {
                        returnData.putBoolean("signatureSuccess", false);
                        signature = null;
                    }
                }
            }

            out.close();

            if (signature != null) {
                progress.setProgress("verifying signature...", 80, 100);
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
        if (pbe.isIntegrityProtected()) {
            progress.setProgress("verifying integrity...", 90, 100);
            if (!pbe.verify()) {
                System.err.println("message failed integrity check");
            } else {
                System.err.println("message integrity check passed");
            }
        } else {
            System.err.println("no message integrity check");
        }

        progress.setProgress("done.", 100, 100);
        return returnData;
    }

    public static Bundle verifyText(InputStream inStream, OutputStream outStream,
                                    ProgressDialogUpdater progress)
            throws IOException, GeneralException, PGPException, SignatureException {
        Bundle returnData = new Bundle();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ArmoredInputStream aIn = new ArmoredInputStream(inStream);

        progress.setProgress("reading data...", 0, 100);

        // mostly taken from CLearSignedFileProcessor
        ByteArrayOutputStream lineOut = new ByteArrayOutputStream();
        int lookAhead = readInputLine(lineOut, aIn);
        byte[] lineSep = getLineSeparator();

        if (lookAhead != -1 && aIn.isClearText())
        {
            byte[] line = lineOut.toByteArray();
            out.write(line, 0, getLengthWithoutSeparator(line));
            out.write(lineSep);

            while (lookAhead != -1 && aIn.isClearText())
            {
                lookAhead = readInputLine(lineOut, lookAhead, aIn);

                line = lineOut.toByteArray();
                out.write(line, 0, getLengthWithoutSeparator(line));
                out.write(lineSep);
            }
        }

        out.close();

        byte[] clearText = out.toByteArray();
        outStream.write(clearText);

        returnData.putBoolean("signature", true);

        progress.setProgress("processing signature...", 60, 100);
        PGPObjectFactory pgpFact = new PGPObjectFactory(aIn);

        PGPSignatureList sigList = (PGPSignatureList) pgpFact.nextObject();
        if (sigList == null) {
            throw new GeneralException("corrupt data");
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
            progress.setProgress("done.", 100, 100);
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

        progress.setProgress("done.", 100, 100);
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
        throws IOException
    {
        bOut.reset();

        int lookAhead = -1;
        int ch;

        while ((ch = fIn.read()) >= 0)
        {
            bOut.write(ch);
            if (ch == '\r' || ch == '\n')
            {
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
        if (length > 0)
        {
            sig.update(line, 0, length);
        }
    }

    private static void processLine(OutputStream aOut, PGPSignatureGenerator sGen, byte[] line)
        throws SignatureException, IOException {
        int length = getLengthWithoutWhiteSpace(line);
        if (length > 0)
        {
            sGen.update(line, 0, length);
        }

        aOut.write(line, 0, line.length);
    }

    private static int getLengthWithoutSeparator(byte[] line) {
        int    end = line.length - 1;

        while (end >= 0 && isLineEnding(line[end])) {
            end--;
        }

        return end + 1;
    }

    private static boolean isLineEnding(byte b) {
        return b == '\r' || b == '\n';
    }

    private static int getLengthWithoutWhiteSpace(byte[] line) {
        int    end = line.length - 1;

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
