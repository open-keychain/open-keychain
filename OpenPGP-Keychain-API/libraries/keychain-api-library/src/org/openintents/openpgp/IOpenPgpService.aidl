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
     * Bundle params:
     * int          api_version (required)
     * boolean      ascii_armor (request ascii armor for ouput)
     *
     * returned Bundle:
     * int          result_code (0, 1, or 2 (see OpenPgpConstants))
     * OpenPgpError error       (if result_code == 0)
     * Intent       intent      (if result_code == 2)
     *
     */

    /**
     * Sign only
     *
     * optional params:
     * String       passphrase  (for key passphrase)
     */
    Bundle sign(in Bundle params, in ParcelFileDescriptor input, in ParcelFileDescriptor output);

    /**
     * Encrypt
     *
     * Bundle params:
     * long[]       key_ids
     * or
     * String[]     user_ids    (= emails of recipients) (if more than one key has this user_id, a PendingIntent is returned)
     *
     * optional params:
     * String       passphrase  (for key passphrase)
     */
    Bundle encrypt(in Bundle params, in ParcelFileDescriptor input, in ParcelFileDescriptor output);

    /**
     * Sign and encrypt
     *
     * Bundle params:
     * same as in encrypt()
     */
    Bundle signAndEncrypt(in Bundle params, in ParcelFileDescriptor input, in ParcelFileDescriptor output);

    /**
     * Decrypts and verifies given input bytes. This methods handles encrypted-only, signed-and-encrypted,
     * and also signed-only input.
     *
     * returned Bundle:
     * OpenPgpSignatureResult   signature_result
     */
    Bundle decryptAndVerify(in Bundle params, in ParcelFileDescriptor input, in ParcelFileDescriptor output);

    /**
     * Retrieves key ids based on given user ids (=emails)
     *
     * Bundle params:
     * String[]     user_ids
     *
     * returned Bundle:
     * long[]       key_ids
     */
    Bundle getKeyIds(in Bundle params);

}