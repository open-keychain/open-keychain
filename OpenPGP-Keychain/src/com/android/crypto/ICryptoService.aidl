/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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
 
package com.android.crypto;

import com.android.crypto.ICryptoCallback;

/**
 * All methods are oneway, which means they are asynchronous and non-blocking.
 * Results are returned to the callback, which has to be implemented on client side.
 */
interface ICryptoService {
       
    /**
     * Encrypt
     * 
     * @param inputBytes
     *            Byte array you want to encrypt
     * @param encryptionKeyIds
     *            Ids of public keys used for encryption
     * @param handler
     *            Results are returned to this Handler after successful encryption
     */
    oneway void encrypt(in byte[] inputBytes, in String[] encryptionUserIds, in ICryptoCallback callback);
    
    /**
     * Encrypt and sign
     *
     * 
     * 
     * @param inputBytes
     *            Byte array you want to encrypt
     * @param signatureKeyId
     *            Key id of key to sign with
     * @param handler
     *            Results are returned to this Handler after successful encryption and signing
     */
    oneway void encryptAndSign(in byte[] inputBytes, in String[] encryptionUserIds, String signatureUserId, in ICryptoCallback callback);
    
    /**
     * Sign
     *
     * 
     * 
     * @param inputBytes
     *            Byte array you want to encrypt
     * @param signatureId
     *            
     * @param handler
     *            Results are returned to this Handler after successful encryption and signing
     */
    oneway void sign(in byte[] inputBytes, String signatureUserId, in ICryptoCallback callback);
    
    /**
     * Decrypts and verifies given input bytes. If no signature is present this method
     * will only decrypt.
     * 
     * @param inputBytes
     *            Byte array you want to decrypt and verify
     * @param handler
     *            Handler where to return results to after successful encryption
     */
    oneway void decryptAndVerify(in byte[] inputBytes, in ICryptoCallback callback);    
    
}