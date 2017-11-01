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

package org.openintents.ssh.authentication.response;

import android.app.PendingIntent;
import android.content.Intent;
import org.openintents.ssh.authentication.SshAuthenticationApi;
import org.openintents.ssh.authentication.SshAuthenticationApiError;

public abstract class Response {
    public static final int RESULT_CODE_ERROR = SshAuthenticationApi.RESULT_CODE_ERROR;
    public static final int RESULT_CODE_SUCCESS = SshAuthenticationApi.RESULT_CODE_SUCCESS;
    public static final int RESULT_CODE_USER_INTERACTION_REQUIRED = SshAuthenticationApi.RESULT_CODE_USER_INTERACTION_REQUIRED;

    protected int mResultCode;

    protected PendingIntent mPendingIntent;

    protected SshAuthenticationApiError mError;

    protected Response() {
        mResultCode = RESULT_CODE_SUCCESS;
    }

    public Response(Intent data) {
        populateFromIntent(data);
    }

    public Response(PendingIntent pendingIntent) {
        mPendingIntent = pendingIntent;
        mResultCode = RESULT_CODE_USER_INTERACTION_REQUIRED;
    }

    public Response(SshAuthenticationApiError error) {
        mError = error;
        mResultCode = RESULT_CODE_ERROR;
    }

    private void populateFromIntent(Intent intent) {
        int resultCode = intent.getIntExtra(SshAuthenticationApi.EXTRA_RESULT_CODE, SshAuthenticationApi.RESULT_CODE_ERROR);

        switch (resultCode) {
            case SshAuthenticationApi.RESULT_CODE_SUCCESS:
                mResultCode = RESULT_CODE_SUCCESS;
                getResults(intent);
                break;
            case SshAuthenticationApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                mResultCode = RESULT_CODE_USER_INTERACTION_REQUIRED;
                mPendingIntent = intent.getParcelableExtra(SshAuthenticationApi.EXTRA_PENDING_INTENT);
                break;
            case SshAuthenticationApi.RESULT_CODE_ERROR:
                mResultCode = RESULT_CODE_ERROR;
                mError = intent.getParcelableExtra(SshAuthenticationApi.EXTRA_ERROR);
        }
    }

    public Intent toIntent() {
        Intent intent = new Intent();

        switch (mResultCode) {
            case RESULT_CODE_SUCCESS:
                intent.putExtra(SshAuthenticationApi.EXTRA_RESULT_CODE, SshAuthenticationApi.RESULT_CODE_SUCCESS);
                putResults(intent);
                break;
            case RESULT_CODE_USER_INTERACTION_REQUIRED:
                intent.putExtra(SshAuthenticationApi.EXTRA_RESULT_CODE, SshAuthenticationApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                intent.putExtra(SshAuthenticationApi.EXTRA_PENDING_INTENT, mPendingIntent);
                break;
            case RESULT_CODE_ERROR:
                intent.putExtra(SshAuthenticationApi.EXTRA_RESULT_CODE, SshAuthenticationApi.RESULT_CODE_ERROR);
                intent.putExtra(SshAuthenticationApi.EXTRA_ERROR, mError);
        }

        return intent;
    }

    public int getResultCode() {
        return mResultCode;
    }

    public PendingIntent getPendingIntent() {
        return mPendingIntent;
    }

    public SshAuthenticationApiError getError() {
        return mError;
    }

    protected abstract void getResults(Intent intent);

    protected abstract void putResults(Intent intent);

}
