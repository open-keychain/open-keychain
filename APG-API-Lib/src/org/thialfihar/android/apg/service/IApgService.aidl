package org.thialfihar.android.apg.service;

import org.thialfihar.android.apg.service.IApgEncryptDecryptHandler;
import org.thialfihar.android.apg.service.IApgSignVerifyHandler;
import org.thialfihar.android.apg.service.IApgHelperHandler;

/**
 * All methods are oneway, which means they are asynchronous and non-blocking.
 * Results are returned into given Handler, which has to be implemented on client side.
 */
interface IApgService {
       
    /**
     * Encrypt
     *
     * Either inputBytes or inputUri is given, the other should be null.
     * 
     * @param inputBytes
     *            Byte array you want to encrypt
     * @param inputUri
     *            Blob in ContentProvider you want to encrypt
     * @param useAsciiArmor 
     *            Convert bytes to ascii armored text to guard against encoding problems
     * @param compression
     *            Compression: 0x21070001: none, 1: Zip, 2: Zlib, 3: BZip2
     * @param encryptionKeyIds
     *            Ids of public keys used for encryption
     * @param symmetricEncryptionAlgorithm
     *            7: AES-128, 8: AES-192, 9: AES-256, 4: Blowfish, 10: Twofish, 3: CAST5,
     *            6: DES, 2: Triple DES, 1: IDEA
     * @param handler
     *            Results are returned to this IApgEncryptDecryptHandler Handler
     *            to onSuccessEncrypt(in byte[] output), after successful encryption
     */
    oneway void encryptAsymmetric(in byte[] inputBytes, in String inputUri, in boolean useAsciiArmor,
            in int compression, in long[] encryptionKeyIds, in int symmetricEncryptionAlgorithm,
            in IApgEncryptDecryptHandler handler);
    
    /**
     * Same as encryptAsymmetric but using a passphrase for symmetric encryption
     *
     * @param encryptionPassphrase
     *            Passphrase for direct symmetric encryption using symmetricEncryptionAlgorithm
     */
    oneway void encryptSymmetric(in byte[] inputBytes, in String inputUri, in boolean useAsciiArmor,
            in int compression, in String encryptionPassphrase, in int symmetricEncryptionAlgorithm,
            in IApgEncryptDecryptHandler handler);
    
    /**
     * Encrypt and sign
     *
     * Either inputBytes or inputUri is given, the other should be null.
     * 
     * @param inputBytes
     *            Byte array you want to encrypt
     * @param inputUri
     *            Blob in ContentProvider you want to encrypt
     * @param useAsciiArmor 
     *            Convert bytes to ascii armored text to guard against encoding problems
     * @param compression
     *            Compression: 0x21070001: none, 1: Zip, 2: Zlib, 3: BZip2
     * @param encryptionKeyIds
     *            Ids of public keys used for encryption
     * @param symmetricEncryptionAlgorithm
     *            7: AES-128, 8: AES-192, 9: AES-256, 4: Blowfish, 10: Twofish, 3: CAST5,
     *            6: DES, 2: Triple DES, 1: IDEA
     * @param signatureKeyId
     *            Key id of key to sign with
     * @param signatureHashAlgorithm
     *            1: MD5, 3: RIPEMD-160, 2: SHA-1, 11: SHA-224, 8: SHA-256, 9: SHA-384,
     *            10: SHA-512
     * @param signatureForceV3
     *            Force V3 signatures
     * @param signaturePassphrase
     *            Passphrase to unlock signature key
     * @param handler
     *            Results are returned to this IApgEncryptDecryptHandler Handler
     *            to onSuccessEncrypt(in byte[] output), after successful encryption and signing
     */
    oneway void encryptAndSignAsymmetric(in byte[] inputBytes, in String inputUri,
            in boolean useAsciiArmor, in int compression, in long[] encryptionKeyIds,
            in int symmetricEncryptionAlgorithm, in long signatureKeyId, in int signatureHashAlgorithm,
            in boolean signatureForceV3, in String signaturePassphrase,
            in IApgEncryptDecryptHandler handler);
    
    /**
     * Same as encryptAndSignAsymmetric but using a passphrase for symmetric encryption
     *
     * @param encryptionPassphrase
     *            Passphrase for direct symmetric encryption using symmetricEncryptionAlgorithm
     */
    oneway void encryptAndSignSymmetric(in byte[] inputBytes, in String inputUri,
            in boolean useAsciiArmor, in int compression, in String encryptionPassphrase,
            in int symmetricEncryptionAlgorithm, in long signatureKeyId, in int signatureHashAlgorithm,
            in boolean signatureForceV3, in String signaturePassphrase,
            in IApgEncryptDecryptHandler handler);
    
    /**
     * Decrypts and verifies given input bytes. If no signature is present this method
     * will only decrypt.
     * 
     * @param inputBytes
     *            Byte array you want to decrypt and verify
     * @param inputUri
     *            Blob in ContentProvider you want to decrypt and verify
     * @param keyPassphrase
     *            Passphrase to unlock secret key for decryption.
     * @param handler
     *            Handler where to return results to after successful encryption
     */
    oneway void decryptAndVerifyAsymmetric(in byte[] inputBytes, in String inputUri,
            in String keyPassphrase, in IApgEncryptDecryptHandler handler);
    
    /**
     * Same as decryptAndVerifyAsymmetric but for symmetric decryption.
     *
     * @param encryptionPassphrase
     *            Passphrase to decrypt
     */
    oneway void decryptAndVerifySymmetric(in byte[] inputBytes, in String inputUri,
            in String encryptionPassphrase, in IApgEncryptDecryptHandler handler);
    
    /**
     *
     */
    oneway void getDecryptionKey(in byte[] inputBytes, in String inputUri,
            in IApgHelperHandler handler);
    
    
}