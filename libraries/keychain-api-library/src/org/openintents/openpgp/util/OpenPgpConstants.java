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

package org.openintents.openpgp.util;

public class OpenPgpConstants {

    public static final String TAG = "OpenPgp API";

    public static final int API_VERSION = 1;
    public static final String SERVICE_INTENT = "org.openintents.openpgp.IOpenPgpService";


    /* Bundle params */
    public static final String PARAMS_API_VERSION = "api_version";
    // request ASCII Armor for output
    // OpenPGP Radix-64, 33 percent overhead compared to binary, see http://tools.ietf.org/html/rfc4880#page-53)
    public static final String PARAMS_REQUEST_ASCII_ARMOR = "ascii_armor";
    // (for encrypt method)
    public static final String PARAMS_USER_IDS = "user_ids";
    public static final String PARAMS_KEY_IDS = "key_ids";
    // optional parameter:
    public static final String PARAMS_PASSPHRASE = "passphrase";

    /* Service Bundle returns */
    public static final String RESULT_CODE = "result_code";
    public static final String RESULT_SIGNATURE = "signature";
    public static final String RESULT_ERRORS = "error";
    public static final String RESULT_INTENT = "intent";

    // get actual error object from RESULT_ERRORS
    public static final int RESULT_CODE_ERROR = 0;
    // success!
    public static final int RESULT_CODE_SUCCESS = 1;
    // executeServiceMethod intent and do it again with params from intent
    public static final int RESULT_CODE_USER_INTERACTION_REQUIRED = 2;

    /* PendingIntent returns */
    public static final String PI_RESULT_PARAMS = "params";

}
