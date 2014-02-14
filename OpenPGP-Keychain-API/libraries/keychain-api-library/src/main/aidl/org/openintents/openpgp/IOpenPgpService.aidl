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

import org.openintents.openpgp.OpenPgpData;
import org.openintents.openpgp.IOpenPgpCallback;
import org.openintents.openpgp.IOpenPgpKeyIdsCallback;

interface IOpenPgpService {

    /**
     * Bundle params:
     * api_version 1,2,3,... (current: 1)
     * ascii_armor true/false (for output)
     * key_ids long[] (for encrypt method)
     *
     *
     * Bundle return:
     * result_code RESULT_ERROR=0 (see error), RESULT_OK=1, RESULT_USER_INTERACTION_REQUIRED=2 (execute intent and do it again with params from intent)
     * signature_result OpenPgpSignatureResult
     * error OpenPgpError
     * intent Intent
     *
     */

    Bundle sign(in Bundle params, in ParcelFileDescriptor input, in ParcelFileDescriptor output);

    Bundle encrypt(in Bundle params, in ParcelFileDescriptor input, in ParcelFileDescriptor output);

    Bundle signAndEncrypt(in Bundle params, in ParcelFileDescriptor input, in ParcelFileDescriptor output);

    Bundle decryptAndVerify(in Bundle params, in ParcelFileDescriptor input, in ParcelFileDescriptor output);


/*
    ------------------OLD--------------------------
*/
    /**
     * Sign
     * 
     * After successful signing, callback's onSuccess will contain the resulting output.
     *
     * @param input
     *            OpenPgpData object containing String, byte[], ParcelFileDescriptor, or Uri
     * @param output
     *            Request output format by defining OpenPgpData object
     *            
     *            new OpenPgpData(OpenPgpData.TYPE_STRING)
     *                Returns as String
     *                (OpenPGP Radix-64, 33 percent overhead compared to binary, see http://tools.ietf.org/html/rfc4880#page-53)
     *            new OpenPgpData(OpenPgpData.TYPE_BYTE_ARRAY)
     *                Returns as byte[]
     *            new OpenPgpData(uri)
     *                Writes output to given Uri
     *            new OpenPgpData(fileDescriptor)
     *                Writes output to given ParcelFileDescriptor
     * @param callback
     *            Callback where to return results
     */
    //oneway void sign(in OpenPgpData input, in OpenPgpData output, in IOpenPgpCallback callback);


    /**
     * Encrypt
     * 
     * After successful encryption, callback's onSuccess will contain the resulting output.
     * 
     * @param input
     *            OpenPgpData object containing String, byte[], ParcelFileDescriptor, or Uri
     * @param output
     *            Request output format by defining OpenPgpData object
     *            
     *            new OpenPgpData(OpenPgpData.TYPE_STRING)
     *                Returns as String
     *                (OpenPGP Radix-64, 33 percent overhead compared to binary, see http://tools.ietf.org/html/rfc4880#page-53)
     *            new OpenPgpData(OpenPgpData.TYPE_BYTE_ARRAY)
     *                Returns as byte[]
     *            new OpenPgpData(uri)
     *                Writes output to given Uri
     *            new OpenPgpData(fileDescriptor)
     *                Writes output to given ParcelFileDescriptor
     * @param keyIds
     *            Key Ids of recipients. Can be retrieved with getKeyIds()
     * @param callback
     *            Callback where to return results
     */
    //oneway void encrypt(in OpenPgpData input, in OpenPgpData output, in long[] keyIds, in IOpenPgpCallback callback);
    
    /**
     * Sign then encrypt
     * 
     * After successful signing and encryption, callback's onSuccess will contain the resulting output.
     *
     * @param input
     *            OpenPgpData object containing String, byte[], ParcelFileDescriptor, or Uri
     * @param output
     *            Request output format by defining OpenPgpData object
     *            
     *            new OpenPgpData(OpenPgpData.TYPE_STRING)
     *                Returns as String
     *                (OpenPGP Radix-64, 33 percent overhead compared to binary, see http://tools.ietf.org/html/rfc4880#page-53)
     *            new OpenPgpData(OpenPgpData.TYPE_BYTE_ARRAY)
     *                Returns as byte[]
     *            new OpenPgpData(uri)
     *                Writes output to given Uri
     *            new OpenPgpData(fileDescriptor)
     *                Writes output to given ParcelFileDescriptor
     * @param keyIds
     *            Key Ids of recipients. Can be retrieved with getKeyIds()
     * @param callback
     *            Callback where to return results
     */
    //oneway void signAndEncrypt(in OpenPgpData input, in OpenPgpData output, in long[] keyIds, in IOpenPgpCallback callback);
    
    /**
     * Decrypts and verifies given input bytes. This methods handles encrypted-only, signed-and-encrypted,
     * and also signed-only input.
     * 
     * After successful decryption/verification, callback's onSuccess will contain the resulting output.
     * The signatureResult in onSuccess is only non-null if signed-and-encrypted or signed-only inputBytes were given.
     * 
     * @param input
     *            OpenPgpData object containing String, byte[], ParcelFileDescriptor, or Uri
     * @param output
     *            Request output format by defining OpenPgpData object
     *            
     *            new OpenPgpData(OpenPgpData.TYPE_STRING)
     *                Returns as String
     *                (OpenPGP Radix-64, 33 percent overhead compared to binary, see http://tools.ietf.org/html/rfc4880#page-53)
     *            new OpenPgpData(OpenPgpData.TYPE_BYTE_ARRAY)
     *                Returns as byte[]
     *            new OpenPgpData(uri)
     *                Writes output to given Uri
     *            new OpenPgpData(fileDescriptor)
     *                Writes output to given ParcelFileDescriptor
     * @param callback
     *            Callback where to return results
     */
    //oneway void decryptAndVerify(in OpenPgpData input, in OpenPgpData output, in IOpenPgpCallback callback);
    
    /**
     * Get available key ids based on given user ids
     *
     * @param ids
     *            User Ids (emails) of recipients OR key ids
     * @param allowUserInteraction
     *            Enable user interaction to lookup and import unknown keys
     * @param callback
     *            Callback where to return results (different type than callback in other functions!)
     */
    //oneway void getKeyIds(in String[] ids, in boolean allowUserInteraction, in IOpenPgpKeyIdsCallback callback);
    
}