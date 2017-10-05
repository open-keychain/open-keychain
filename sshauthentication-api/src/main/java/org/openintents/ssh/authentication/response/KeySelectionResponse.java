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

public class KeySelectionResponse extends Response {

    private String mKeyId;
    private String mKeyDescription;

    public KeySelectionResponse(Intent data) {
        super(data);
    }

    public KeySelectionResponse(PendingIntent pendingIntent) {
        super(pendingIntent);
    }

    public KeySelectionResponse(SshAuthenticationApiError error) {
        super(error);
    }

    public KeySelectionResponse(String keyId, String keyDescription) {
        super();
        mKeyId = keyId;
        mKeyDescription = keyDescription;
    }

    @Override
    protected void getResults(Intent intent) {
        mKeyId = intent.getStringExtra(SshAuthenticationApi.EXTRA_KEY_ID);
        mKeyDescription = intent.getStringExtra(SshAuthenticationApi.EXTRA_KEY_DESCRIPTION);
    }

    @Override
    protected void putResults(Intent intent) {
        intent.putExtra(SshAuthenticationApi.EXTRA_KEY_ID, mKeyId);
        intent.putExtra(SshAuthenticationApi.EXTRA_KEY_DESCRIPTION, mKeyDescription);
    }

    public String getKeyId() {
        return mKeyId;
    }

    public String getKeyDescription() {
        return mKeyDescription;
    }
}
