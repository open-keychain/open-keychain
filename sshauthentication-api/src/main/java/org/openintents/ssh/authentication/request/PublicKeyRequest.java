/*
 * Copyright (C) 2017 Christian Hagau <ach@hagau.se>
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

public class PublicKeyRequest extends Request {

    private String mKeyId;

    public PublicKeyRequest(String keyId) {
        mKeyId = keyId;
    }

    @Override
    protected void getData(Intent intent) {
        mKeyId = intent.getStringExtra(SshAuthenticationApi.EXTRA_KEY_ID);
    }

    @Override
    protected void putData(Intent request) {
        request.putExtra(SshAuthenticationApi.EXTRA_KEY_ID, mKeyId);
    }

    @Override
    protected String getAction() {
        return SshAuthenticationApi.ACTION_GET_PUBLIC_KEY;
    }

    public String getKeyID() {
        return mKeyId;
    }

}
