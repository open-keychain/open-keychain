package org.sufficientlysecure.keychain.provider;


import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpSecurityConstants;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Date;

public final class ByteArrayEncryptor {
    private static final int ITERATION_COUNT = 1000;
    private static final int PBKFD2_KEY_LENGTH = 256;
    private static final int SALT_LENGTH = PBKFD2_KEY_LENGTH / 8;
    private static final String PBKFD2_CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private static final String SYMMETRIC_KEY_ALGORITHM = "AES";
    private static final int SYMMETRIC_KEY_LENGTH = 256;
    private static final String SYMMETRIC_KEY_CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private static final String TAG = ByteArrayEncryptor.class.getSimpleName();

    private ByteArrayEncryptor() {}

    public static byte[] encryptByteArray(byte[] forEncrypting, char[] passphrase)
            throws EncryptDecryptException{
        try {
            // handle empty passphrase
            if (passphrase.length == 0) {
                return toBytes(new EncryptedData(forEncrypting, new byte[0], new byte[0]));
            }

            forEncrypting = forEncrypting.clone();
            SecureRandom random = new SecureRandom();

            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            SecretKey key = derivePbkfd2Key(salt, passphrase);
            Cipher cipher = Cipher.getInstance(PBKFD2_CIPHER_TRANSFORMATION);
            byte[] iv = new byte[cipher.getBlockSize()];
            random.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);

            byte[] encryptedKeyRing = cipher.doFinal(forEncrypting);

            EncryptedData keyRing = new EncryptedData(encryptedKeyRing, iv, salt);
            return toBytes(keyRing);

        } catch (IOException | GeneralSecurityException e) {
            if(e instanceof IOException) {
                Log.e(TAG, "serialization error");
            } else {
                Log.e(TAG, "encrypt error");
            }
            throw new EncryptDecryptException(e);
        }
    }

    private static SecretKey derivePbkfd2Key(byte[] salt, char[] passphrase) throws GeneralSecurityException {

        KeySpec keySpec = new PBEKeySpec(passphrase, salt, ITERATION_COUNT, PBKFD2_KEY_LENGTH);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static byte[] decryptByteArray(byte[] forDecrypting, char[] passphrase)
            throws EncryptDecryptException, IncorrectPassphraseException {
        try {
            forDecrypting = forDecrypting.clone();
            EncryptedData encryptedData = (EncryptedData) fromBytes(forDecrypting);

            // handle empty passphrase
            boolean usesEmptyPassphrase = (encryptedData.mIv.length == 0 && encryptedData.mSalt.length == 0);
            if (usesEmptyPassphrase && passphrase.length == 0) {
                return encryptedData.mBytes;
            } else if (usesEmptyPassphrase && passphrase.length > 0) {
                throw new IncorrectPassphraseException();
            } else if (passphrase.length == 0) {
                throw new IncorrectPassphraseException();
            }

            SecretKey key = derivePbkfd2Key(encryptedData.mSalt, passphrase);
            Cipher cipher = Cipher.getInstance(PBKFD2_CIPHER_TRANSFORMATION);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(encryptedData.mIv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
            return cipher.doFinal(encryptedData.mBytes);

        } catch (IOException | ClassNotFoundException | GeneralSecurityException e) {
            if (e instanceof BadPaddingException) {
                throw new IncorrectPassphraseException();
            } else {
                if(e instanceof IOException || e instanceof ClassNotFoundException) {
                    Log.e(TAG, "serialization error");
                } else {
                    Log.e(TAG, "encrypt error");
                }
                throw new EncryptDecryptException(e);
            }
        }
    }

    public static byte[] encryptWithMasterKey(byte[] forEncrypting, SecretKey key) throws EncryptDecryptException {
        try {
            forEncrypting = forEncrypting.clone();
            SecureRandom random = new SecureRandom();

            Cipher cipher = Cipher.getInstance(SYMMETRIC_KEY_CIPHER_TRANSFORMATION);
            byte[] iv = new byte[cipher.getBlockSize()];
            random.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);

            byte[] encryptedKeyRing = cipher.doFinal(forEncrypting);

            EncryptedData keyRing = new EncryptedData(encryptedKeyRing, iv, null);
            return toBytes(keyRing);

        } catch (IOException | GeneralSecurityException e) {
            if(e instanceof IOException) {
                Log.e(TAG, "serialization error");
            } else {
                Log.e(TAG, "encrypt error");
            }
            throw new EncryptDecryptException(e);
        }
    }

    public static byte[] decryptWithMasterKey(byte[] forDecrypting, SecretKey key)
            throws EncryptDecryptException, IncorrectPassphraseException {
        try {
            forDecrypting = forDecrypting.clone();
            EncryptedData encryptedData = (EncryptedData) fromBytes(forDecrypting);

            Cipher cipher = Cipher.getInstance(SYMMETRIC_KEY_CIPHER_TRANSFORMATION);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(encryptedData.mIv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
            return cipher.doFinal(encryptedData.mBytes);

        } catch (IOException | ClassNotFoundException | GeneralSecurityException e) {
            if (e instanceof BadPaddingException) {
                throw new IncorrectPassphraseException();
            } else {
                if(e instanceof IOException || e instanceof ClassNotFoundException) {
                    Log.e(TAG, "serialization error");
                } else {
                    Log.e(TAG, "encrypt error");
                }
                throw new EncryptDecryptException(e);
            }
        }
    }

    private static SecretKey generateSymmetricKey() throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance(SYMMETRIC_KEY_ALGORITHM);
        keyGen.init(SYMMETRIC_KEY_LENGTH);
        return keyGen.generateKey();
    }

    /**
     * Returns a randomly generated symmetric key, which is encrypted
     * by the given passphrase
     */
    public static byte[] getNewEncryptedSymmetricKey(Passphrase passphrase) throws EncryptDecryptException {
        try {
            SecretKey secretKey = generateSymmetricKey();
            return encryptSymmetricKey(passphrase, secretKey);
        } catch (GeneralSecurityException | EncryptDecryptException e) {
            throw new EncryptDecryptException(e);
        }
    }

    // Encrypts secret key with given passphrase
    public static byte[] encryptSymmetricKey(Passphrase passphrase, SecretKey secretKey) throws EncryptDecryptException {
        try {
            byte[] keyBytes = toBytes(secretKey);
            return encryptByteArray(keyBytes, passphrase.getCharArray());
        } catch (IOException e) {
            throw new EncryptDecryptException(e);
        }
    }

    // TODO: wip, shift to util class for serializables?
    public static byte[] toBytes(Serializable obj) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = null;
        try {
            objectStream = new ObjectOutputStream(byteStream);
            objectStream.writeObject(obj);

            return byteStream.toByteArray();
        } finally {
            byteStream.close();
            if(objectStream != null) {
                objectStream.close();
            }
        }
    }

    // TODO: wip, shift to util class for serializables?
    public static Object fromBytes(byte[] storedData) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(storedData);
        ObjectInputStream objectStream = null;
        try {
            objectStream = new ObjectInputStream(byteStream);
            return objectStream.readObject();
        } finally {
            byteStream.close();
            if(objectStream != null) {
                objectStream.close();
            }
        }
    }

    private static class EncryptedData implements Serializable {
        private byte[] mBytes;
        private byte[] mIv;
        private byte[] mSalt;
        private static final long serialVersionUID = 1L;

        public EncryptedData(byte[] encryptedKeyData, byte[] iv, byte[] salt) {
            mBytes = encryptedKeyData;
            mSalt = salt;
            mIv = iv;
        }
    }

    public static class IncorrectPassphraseException extends Exception {
        public IncorrectPassphraseException(){
            super();
        }
    }

    public static class EncryptDecryptException extends Exception {
        public EncryptDecryptException(Throwable cause){
            super(cause);
        }
    }
}
