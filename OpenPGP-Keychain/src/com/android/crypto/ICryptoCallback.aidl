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

import com.android.crypto.CryptoSignatureResult;
import com.android.crypto.CryptoError;

interface ICryptoCallback {

    oneway void onEncryptSignSuccess(in byte[] outputBytes);
    
    oneway void onDecryptVerifySuccess(in byte[] outputBytes, in CryptoSignatureResult signatureResult);


    oneway void onError(in CryptoError error);
    
    oneway void onActivityRequired(in Intent intent);
}