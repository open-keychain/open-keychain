/*
 * Copyright (C) 2017 Christian Hagau <ach@hagau.se>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.ssh.authentication.response;

import android.app.PendingIntent;
import android.content.Intent;
import org.openintents.ssh.authentication.SshAuthenticationApi;
import org.openintents.ssh.authentication.SshAuthenticationApiError;

public class PublicKeyResponse extends Response {
    public static final int INVALID_ALGORITHM = SshAuthenticationApiError.INVALID_ALGORITHM;

    private byte[] mEncodedPublicKey;
    private int mKeyAlgorithm;

    public PublicKeyResponse(Intent data) {
        super(data);
    }

    public PublicKeyResponse(PendingIntent pendingIntent) {
        super(pendingIntent);
    }

    public PublicKeyResponse(SshAuthenticationApiError error) {
        super(error);
    }

    public PublicKeyResponse(byte[] encodedPublicKey,
                             int keyAlgorithm) {
        super();
        mEncodedPublicKey = encodedPublicKey;
        mKeyAlgorithm = keyAlgorithm;
    }

    @Override
    protected void getResults(Intent intent) {
        mEncodedPublicKey = intent.getByteArrayExtra(SshAuthenticationApi.EXTRA_PUBLIC_KEY);
        mKeyAlgorithm = intent.getIntExtra(SshAuthenticationApi.EXTRA_PUBLIC_KEY_ALGORITHM, INVALID_ALGORITHM);
    }

    @Override
    protected void putResults(Intent intent) {
        intent.putExtra(SshAuthenticationApi.EXTRA_PUBLIC_KEY, mEncodedPublicKey);
        intent.putExtra(SshAuthenticationApi.EXTRA_PUBLIC_KEY_ALGORITHM, mKeyAlgorithm);
    }

    public byte[] getEncodedPublicKey() {
        return mEncodedPublicKey;
    }

    public int getKeyAlgorithm() {
        return mKeyAlgorithm;
    }
}
