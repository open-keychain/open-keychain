/*
 * Copyright (C) 2017 Christian Hagau <ach@hagau.se>
 * Copyright (C) 2017 Jonas Dippel, Michael Perk
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

package org.openintents.ssh.authentication.response;

import android.app.PendingIntent;
import android.content.Intent;
import org.openintents.ssh.authentication.SshAuthenticationApi;
import org.openintents.ssh.authentication.SshAuthenticationApiError;

public class SigningResponse extends Response {

    private byte[] mSignature;

    public SigningResponse(Intent data) {
        super(data);
    }

    public SigningResponse(PendingIntent pendingIntent) {
        super(pendingIntent);
    }

    public SigningResponse(SshAuthenticationApiError error) {
        super(error);
    }

    public SigningResponse(byte[] signature) {
        super();
        mSignature = signature;
    }

    @Override
    protected void getResults(Intent intent) {
        mSignature = intent.getByteArrayExtra(SshAuthenticationApi.EXTRA_SIGNATURE);
    }

    @Override
    protected void putResults(Intent intent) {
        intent.putExtra(SshAuthenticationApi.EXTRA_SIGNATURE, mSignature);
    }

    public byte[] getSignature() {
        return mSignature;
    }

}
