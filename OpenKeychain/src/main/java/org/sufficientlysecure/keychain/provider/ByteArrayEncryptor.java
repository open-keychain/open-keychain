package org.sufficientlysecure.keychain.provider;


import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.PGPDataValidationException;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPBEEncryptedData;
import org.bouncycastle.openpgp.jcajce.JcaSkipMarkerPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PBEDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.PgpSecurityConstants;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Iterator;

public final class ByteArrayEncryptor {
    private static final String SYMMETRIC_KEY_ALGORITHM = "AES";
    private static final int SYMMETRIC_KEY_LENGTH = 256;
    private static final String SYMMETRIC_KEY_CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private static final String TAG = ByteArrayEncryptor.class.getSimpleName();

    private ByteArrayEncryptor() {}

    public static byte[] encryptByteArray(byte[] inputData, char[] passphrase)
            throws EncryptDecryptException {
        ByteArrayInputStream byteArrayInput = new ByteArrayInputStream(inputData);
        JcePGPDataEncryptorBuilder encryptorBuilder =
                new JcePGPDataEncryptorBuilder(PgpSecurityConstants.DEFAULT_SYMMETRIC_ALGORITHM)
                        .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME)
                        .setWithIntegrityPacket(true);
        PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(encryptorBuilder);
        JcePBEKeyEncryptionMethodGenerator symmetricEncryptionGenerator =
                new JcePBEKeyEncryptionMethodGenerator(passphrase);
        cPk.addMethod(symmetricEncryptionGenerator);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            OutputStream encryptionOut = new BCPGOutputStream(cPk.open(outputStream, new byte[1 << 16]));

            int length;
            byte[] buffer = new byte[1 << 16];

            InputStream in = new BufferedInputStream(byteArrayInput);
            while ((length = in.read(buffer)) > 0) {
                encryptionOut.write(buffer, 0, length);
            }

            encryptionOut.close();
            outputStream.close();
        } catch (IOException | PGPException e) {
            if (e instanceof IOException) {
                Log.e(TAG, "IO error");
            } else {
                Log.e(TAG, "PGPException error");
            }
            throw new EncryptDecryptException(e);
        }
        return outputStream.toByteArray();
    }

    public static byte[] decryptByteArray(byte[] inputData, char[] passphrase)
            throws EncryptDecryptException, IncorrectPassphraseException {
        try {
            inputData = inputData.clone();
            InputStream in = new ByteArrayInputStream(inputData);

            JcaSkipMarkerPGPObjectFactory pgpF = new JcaSkipMarkerPGPObjectFactory(in);
            PGPEncryptedDataList enc = (PGPEncryptedDataList) pgpF.nextObject();
            Iterator<?> it = enc.getEncryptedDataObjects();
            PGPPBEEncryptedData encryptedDataSymmetric = (PGPPBEEncryptedData) it.next();

            PGPDigestCalculatorProvider digestCalcProvider = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build();
            PBEDataDecryptorFactory decryptorFactory = new JcePBEDataDecryptorFactoryBuilder(
                    digestCalcProvider).setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(passphrase);

            try {
                in = encryptedDataSymmetric.getDataStream(decryptorFactory);
            } catch (PGPDataValidationException e) {
                throw new IncorrectPassphraseException();
            }

            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            int length;
            byte[] buffer = new byte[8192];
            while ((length = in.read(buffer)) > 0) {
                    byteOut.write(buffer, 0, length);
            }

            return byteOut.toByteArray();
        } catch (IOException | PGPException e) {
            throw new EncryptDecryptException(e);
        }
    }


    public static byte[] encryptWithMasterKey(byte[] inputData, SecretKey key) throws EncryptDecryptException {
        try {
            inputData = inputData.clone();
            SecureRandom random = new SecureRandom();

            Cipher cipher = Cipher.getInstance(SYMMETRIC_KEY_CIPHER_TRANSFORMATION);
            byte[] iv = new byte[cipher.getBlockSize()];
            random.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);

            byte[] encryptedBytes = cipher.doFinal(inputData);

            EncryptedData keyRing = new EncryptedData(encryptedBytes, iv);
            return serializableToBytes(keyRing);

        } catch (IOException | GeneralSecurityException e) {
            if(e instanceof IOException) {
                Log.e(TAG, "serialization error");
            } else {
                Log.e(TAG, "encrypt error");
            }
            throw new EncryptDecryptException(e);
        }
    }

    public static byte[] decryptWithMasterKey(byte[] inputData, SecretKey key)
            throws EncryptDecryptException, IncorrectPassphraseException {
        try {
            inputData = inputData.clone();
            EncryptedData encryptedData = (EncryptedData) bytesToSerializable(inputData);

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
            byte[] keyBytes = serializableToBytes(secretKey);
            return encryptByteArray(keyBytes, passphrase.getCharArray());
        } catch (IOException e) {
            throw new EncryptDecryptException(e);
        }
    }

    private static byte[] serializableToBytes(Serializable obj) throws IOException {
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

    public static Serializable bytesToSerializable(byte[] storedData) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(storedData);
        ObjectInputStream objectStream = null;
        try {
            objectStream = new ObjectInputStream(byteStream);
            return (Serializable) objectStream.readObject();
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
        private static final long serialVersionUID = 1L;

        public EncryptedData(byte[] encryptedKeyData, byte[] iv) {
            mBytes = encryptedKeyData;
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
