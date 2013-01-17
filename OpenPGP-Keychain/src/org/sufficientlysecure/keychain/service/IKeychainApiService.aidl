/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
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
 
package org.sufficientlysecure.keychain.service;

import org.sufficientlysecure.keychain.service.handler.IKeychainEncryptHandler;
import org.sufficientlysecure.keychain.service.handler.IKeychainDecryptHandler;
import org.sufficientlysecure.keychain.service.handler.IKeychainGetDecryptionKeyIdHandler;

/**
 * All methods are oneway, which means they are asynchronous and non-blocking.
 * Results are returned into given Handler, which has to be implemented on client side.
 */
interface IKeychainApiService {
       
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
     *            Results are returned to this Handler after successful encryption
     */
    oneway void encryptAsymmetric(in byte[] inputBytes, in String inputUri, in boolean useAsciiArmor,
            in int compression, in long[] encryptionKeyIds, in int symmetricEncryptionAlgorithm,
            in IKeychainEncryptHandler handler);
    
    /**
     * Same as encryptAsymmetric but using a passphrase for symmetric encryption
     *
     * @param encryptionPassphrase
     *            Passphrase for direct symmetric encryption using symmetricEncryptionAlgorithm
     */
    oneway void encryptSymmetric(in byte[] inputBytes, in String inputUri, in boolean useAsciiArmor,
            in int compression, in String encryptionPassphrase, in int symmetricEncryptionAlgorithm,
            in IKeychainEncryptHandler handler);
    
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
     *            Results are returned to this Handler after successful encryption and signing
     */
    oneway void encryptAndSignAsymmetric(in byte[] inputBytes, in String inputUri,
            in boolean useAsciiArmor, in int compression, in long[] encryptionKeyIds,
            in int symmetricEncryptionAlgorithm, in long signatureKeyId, in int signatureHashAlgorithm,
            in boolean signatureForceV3, in String signaturePassphrase,
            in IKeychainEncryptHandler handler);
    
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
            in IKeychainEncryptHandler handler);
    
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
            in String keyPassphrase, in IKeychainDecryptHandler handler);
    
    /**
     * Same as decryptAndVerifyAsymmetric but for symmetric decryption.
     *
     * @param encryptionPassphrase
     *            Passphrase to decrypt
     */
    oneway void decryptAndVerifySymmetric(in byte[] inputBytes, in String inputUri,
            in String encryptionPassphrase, in IKeychainDecryptHandler handler);
    
    /**
     *
     */
    oneway void getDecryptionKeyId(in byte[] inputBytes, in String inputUri,
            in IKeychainGetDecryptionKeyIdHandler handler);
    
    
}