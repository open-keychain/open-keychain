/*
 * Copyright (C) 2017 Christian Hagau <ach@hagau.se>
 * Copyright (C) 2017 Michael Perk
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

package org.openintents.ssh.authentication.request;

import android.content.Intent;
import org.openintents.ssh.authentication.SshAuthenticationApi;
import org.openintents.ssh.authentication.SshAuthenticationApiError;

public class SigningRequest extends Request {

    private byte[] mChallenge;
    private String mKeyIdentifier;
    private int mHashAlgorithm;

    public SigningRequest(byte[] challenge, String keyIdentifier, int hashAlgorithm) {
        mHashAlgorithm = hashAlgorithm;
        mKeyIdentifier = keyIdentifier;
        mChallenge = challenge;
    }

    @Override
    protected String getAction() {
        return SshAuthenticationApi.ACTION_SIGN;
    }

    @Override
    protected void getData(Intent intent) {
        mHashAlgorithm = intent.getIntExtra(SshAuthenticationApi.EXTRA_HASH_ALGORITHM, SshAuthenticationApiError.INVALID_HASH_ALGORITHM);
        mKeyIdentifier = intent.getStringExtra(SshAuthenticationApi.EXTRA_KEY_ID);
        mChallenge = intent.getByteArrayExtra(SshAuthenticationApi.EXTRA_CHALLENGE);
    }

    @Override
    protected void putData(Intent request) {
        request.putExtra(SshAuthenticationApi.EXTRA_HASH_ALGORITHM, mHashAlgorithm);
        request.putExtra(SshAuthenticationApi.EXTRA_KEY_ID, mKeyIdentifier);
        request.putExtra(SshAuthenticationApi.EXTRA_CHALLENGE, mChallenge);
    }

    public int getHashAlgorithm() {
        return mHashAlgorithm;
    }

    public String getKeyIdentifier() {
        return mKeyIdentifier;
    }

    public byte[] getChallenge() {
        return mChallenge;
    }

}
