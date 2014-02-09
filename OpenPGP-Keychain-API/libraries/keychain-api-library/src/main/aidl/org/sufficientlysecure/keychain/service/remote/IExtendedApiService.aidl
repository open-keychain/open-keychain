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
 
package org.sufficientlysecure.keychain.service.remote;

import org.sufficientlysecure.keychain.service.remote.IExtendedApiCallback;

/**
 * All methods are oneway, which means they are asynchronous and non-blocking.
 * Results are returned to the callback, which has to be implemented on client side.
 */
interface IExtendedApiService {
       
    /**
     * Symmetric Encrypt
     * 
     * @param inputBytes
     *            Byte array you want to encrypt
     * @param passphrase
     *            symmetric passhprase
     * @param callback
     *            Callback where to return results
     */
    oneway void encrypt(in byte[] inputBytes, in String passphrase, in IExtendedApiCallback callback);
    
    /**
     * Generates self signed X509 certificate signed by OpenPGP private key (from app settings)
     *
     * @param subjAltNameURI
     * @param callback
     *            Callback where to return results
     */
    oneway void selfSignedX509Cert(in String subjAltNameURI, in IExtendedApiCallback callback);
    
}