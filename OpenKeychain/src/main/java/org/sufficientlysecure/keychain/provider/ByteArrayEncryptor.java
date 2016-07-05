package org.sufficientlysecure.keychain.provider;


import org.sufficientlysecure.keychain.util.Log;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
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
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

public final class ByteArrayEncryptor {
    private static final int ITERATION_COUNT = 1000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = KEY_LENGTH / 8;
    private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String TAG = ByteArrayEncryptor.class.getSimpleName();

    private ByteArrayEncryptor() {}

    public static byte[] encryptByteArray(byte[] keyData, char[] passphrase)
            throws EncryptDecryptException{
        try {
            // handle empty passphrase
            if (passphrase.length == 0) {
                return new EncryptedData(keyData, new byte[0], new byte[0]).toBytes();
            }

            keyData = keyData.clone();
            SecureRandom random = new SecureRandom();

            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            SecretKey key = derivePbkfd2Key(salt, passphrase);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            byte[] iv = new byte[cipher.getBlockSize()];
            random.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);

            byte[] encryptedKeyRing = cipher.doFinal(keyData);

            EncryptedData keyRing = new EncryptedData(encryptedKeyRing, iv, salt);
            return keyRing.toBytes();

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

        KeySpec keySpec = new PBEKeySpec(passphrase, salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static byte[] decryptByteArray(byte[] storedData, char[] passphrase)
            throws EncryptDecryptException, IncorrectPassphraseException {
        try {
            storedData = storedData.clone();
            EncryptedData encryptedData = EncryptedData.fromBytes(storedData);

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
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
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

        public byte[] toBytes() throws IOException {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = null;
            try {
                objectStream = new ObjectOutputStream(byteStream);
                objectStream.writeObject(this);

                return byteStream.toByteArray();
            } finally {
                byteStream.close();
                if(objectStream != null) {
                    objectStream.close();
                }
            }
        }

        public static EncryptedData fromBytes(byte[] storedData) throws IOException, ClassNotFoundException {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(storedData);
            ObjectInputStream objectStream = null;
            try {
                objectStream = new ObjectInputStream(byteStream);
                return (EncryptedData) objectStream.readObject();
            } finally {
                byteStream.close();
                if(objectStream != null) {
                    objectStream.close();
                }
            }
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
