/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
 
package org.openintents.openpgp;

interface IOpenPgpService {

    /**
     * General extras
     * --------------
     * 
     * params:
     * int                      api_version         (current: 1)
     * boolean                  ascii_armor         true/false (for output)
     * String                   passphrase (for key, optional)
     *
     * Bundle return:
     * int                      result_code         0,1, or 2 (see OpenPgpConstants)
     * OpenPgpSignatureResult   signature_result
     * OpenPgpError             error
     * Intent                   intent
     *
     */

    /**
     * sign only
     */
    Bundle sign(in Bundle params, in ParcelFileDescriptor input, in ParcelFileDescriptor output);

    /**
     * encrypt
     *
     * params:
     * long[]       key_ids
     * or
     * String[]     user_ids (= emails of recipients) (if more than one key has this user_id, an Intent is returned)
     */
    Bundle encrypt(in Bundle params, in ParcelFileDescriptor input, in ParcelFileDescriptor output);

    /**
     * sign and encrypt
     *
     * params:
     * same as in encrypt()
     */
    Bundle signAndEncrypt(in Bundle params, in ParcelFileDescriptor input, in ParcelFileDescriptor output);

    /**
     * Decrypts and verifies given input bytes. This methods handles encrypted-only, signed-and-encrypted,
     * and also signed-only input.
     */
    Bundle decryptAndVerify(in Bundle params, in ParcelFileDescriptor input, in ParcelFileDescriptor output);

}