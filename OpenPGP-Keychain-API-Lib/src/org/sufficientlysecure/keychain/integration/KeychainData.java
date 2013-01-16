/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2011 K-9 Mail Contributors
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

package org.sufficientlysecure.keychain.integration;

import java.io.Serializable;
import java.util.Arrays;

public class KeychainData implements Serializable {
    private static final long serialVersionUID = 6314045536270848410L;
    protected long[] mPublicKeyIds = null;
    protected String[] mPublicUserIds = null;
    protected long mSecretKeyId = 0;
    protected String mSecretKeyUserId = null;
    protected boolean mSignatureSuccess = false;
    protected boolean mSignatureUnknown = false;
    protected String mDecryptedData = null;
    protected String mEncryptedData = null;

    public void setSecretKeyId(long keyId) {
        mSecretKeyId = keyId;
    }

    public long getSecretKeyId() {
        return mSecretKeyId;
    }

    public void setPublicKeyIds(long[] keyIds) {
        mPublicKeyIds = keyIds;
    }

    public long[] getPublicKeys() {
        return mPublicKeyIds;
    }

    public void setPublicUserIds(String[] userIds) {
        mPublicUserIds = userIds;
    }

    public String[] getPublicUserIds() {
        return mPublicUserIds;
    }

    public boolean hasSecretKey() {
        return mSecretKeyId != 0;
    }

    public boolean hasPublicKeys() {
        return (mPublicKeyIds != null) && (mPublicKeyIds.length > 0);
    }

    public String getEncryptedData() {
        return mEncryptedData;
    }

    public void setEncryptedData(String data) {
        mEncryptedData = data;
    }

    public String getDecryptedData() {
        return mDecryptedData;
    }

    public void setDecryptedData(String data) {
        mDecryptedData = data;
    }

    public void setSecretKeyUserId(String userId) {
        mSecretKeyUserId = userId;
    }

    public String getSecretKeyUserId() {
        return mSecretKeyUserId;
    }

    public boolean getSignatureSuccess() {
        return mSignatureSuccess;
    }

    public void setSignatureSuccess(boolean success) {
        mSignatureSuccess = success;
    }

    public boolean getSignatureUnknown() {
        return mSignatureUnknown;
    }

    public void setSignatureUnknown(boolean unknown) {
        mSignatureUnknown = unknown;
    }

    @Override
    public String toString() {
        String output = "mPublicKeyIds: " + Arrays.toString(mPublicKeyIds) + "\nmSecretKeyId: "
                + mSecretKeyId + "\nmSecretKeyUserId: " + mSecretKeyUserId
                + "\nmSignatureSuccess: " + mSignatureSuccess + "\nmSignatureUnknown: "
                + mSignatureUnknown + "\nmDecryptedData: " + mDecryptedData + "\nmEncryptedData: "
                + mEncryptedData;

        return output;
    }
}