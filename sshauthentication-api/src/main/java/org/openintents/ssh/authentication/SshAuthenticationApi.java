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

package org.openintents.ssh.authentication;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

public class SshAuthenticationApi {
    private static final String TAG = "SshAuthenticationApi";

    public static final String SERVICE_INTENT = "org.openintents.ssh.authentication.ISshAuthenticationService";

    public static final String EXTRA_API_VERSION = "api_version";
    public static final int API_VERSION = 1;

    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final int RESULT_CODE_ERROR = 0;
    public static final int RESULT_CODE_SUCCESS = 1;
    public static final int RESULT_CODE_USER_INTERACTION_REQUIRED = 2;

    /**
     * ACTION_SIGN
     *
     * Sign a given challenge
     *
     * required extras:
     * String        EXTRA_KEY_ID
     * byte[]        EXTRA_CHALLENGE
     * int           EXTRA_HASH_ALGORITHM
     *
     * returned extras:
     * byte[]        EXTRA_SIGNATURE
     *
     * Note: for EdDSA the hash algorithm is ignored, PureEdDSA has to be implemented
     */
    public static final String ACTION_SIGN = "org.openintents.ssh.action.SIGN";
    public static final String EXTRA_CHALLENGE = "challenge";
    public static final String EXTRA_HASH_ALGORITHM = "hash_algorithm";
    public static final String EXTRA_SIGNATURE = "signature";

    /* hash algorithms used in signature generation */
    public static final int SHA1 = 0;
    public static final int SHA224 = 1;
    public static final int SHA256 = 2;
    public static final int SHA384 = 3;
    public static final int SHA512 = 4;
    public static final int RIPEMD160 = 5;

    /**
     * ACTION_SELECT_KEY
     *
     * Select a key
     *
     * returned extras:
     * String        EXTRA_KEY_ID
     * String        EXTRA_KEY_DESCRIPTION
     */
    public static final String ACTION_SELECT_KEY = "org.openintents.ssh.action.SELECT_KEY";
    public static final String EXTRA_KEY_DESCRIPTION = "key_description";

    /**
     * ACTION_GET_PUBLIC_KEY
     *
     * Get the public key for a key
     *
     * returns the public key encoded according to the ASN.1 type
     * 'SubjectPublicKeyInfo' as defined in the X.509 standard,
     * see RFC5280, RFC3279 and draft-ietf-curdle-pkix
     * and their respective updates
     *
     * required extras:
     * String        EXTRA_KEY_ID
     *
     * returned extras:
     * byte[]        EXTRA_PUBLIC_KEY
     * int           EXTRA_PUBLIC_KEY_ALGORITHM
     */
    public static final String ACTION_GET_PUBLIC_KEY = "org.openintents.ssh.action.GET_PUBLIC_KEY";
    public static final String EXTRA_PUBLIC_KEY = "public_key";
    public static final String EXTRA_PUBLIC_KEY_ALGORITHM = "public_key_algorithm";

    /* public key algorithms */
    public static final int RSA = 0;
    public static final int ECDSA = 1;
    public static final int EDDSA = 2;
    public static final int DSA = 3;

    /**
     * ACTION_GET_SSH_PUBLIC_KEY
     *
     * Get the SSH public key for a key
     *
     * returns the public key in SSH public key format,
     * as described in RFC4253, RFC5656 and draft-ietf-curdle-ssh-ed25519
     * and their respective updates
     *
     * required extras:
     * String        EXTRA_KEY_ID
     *
     * returned extras:
     * String        EXTRA_SSH_PUBLIC_KEY
     */
    public static final String ACTION_GET_SSH_PUBLIC_KEY = "org.openintents.ssh.action.GET_SSH_PUBLIC_KEY";
    public static final String EXTRA_SSH_PUBLIC_KEY = "ssh_public_key";

    /**
     * Error result of type SshAuthenticationApiError
     */
    public static final String EXTRA_ERROR = "error";

    /**
     * Pending Intent requiring user interaction
     */
    public static final String EXTRA_PENDING_INTENT = "intent";

    /**
     * Key identifier
     */
    public static final String EXTRA_KEY_ID = "key_id";


    private final ISshAuthenticationService mService;
    private final Context mContext;


    public SshAuthenticationApi(Context context, ISshAuthenticationService service) {
        mService = service;
        mContext = context;
    }

    public interface ISshAgentCallback {
        void onReturn(final Intent result);
    }

    private class SshAgentAsyncTask extends AsyncTask<Void, Void, Intent> {
        Intent mRequest;
        ISshAgentCallback mCallback;

        private SshAgentAsyncTask(Intent request, ISshAgentCallback callback) {
            mRequest = request;
            mCallback = callback;
        }

        @Override
        protected Intent doInBackground(Void... unused) {
            return executeApi(mRequest);
        }

        protected void onPostExecute(Intent result) {
            mCallback.onReturn(result);
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void executeApiAsync(Intent data, ISshAgentCallback callback) {
        SshAgentAsyncTask task = new SshAgentAsyncTask(data, callback);

        // don't serialize async tasks, always execute them in parallel
        // http://commonsware.com/blog/2012/04/20/asynctask-threading-regression-confirmed.html
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            task.execute();
        }
    }

    public Intent executeApi(Intent data) {
        try {
            // always send version from client
            data.putExtra(SshAuthenticationApi.EXTRA_API_VERSION, SshAuthenticationApi.API_VERSION);

            Intent result;

            // blocks until result is ready
            result = mService.execute(data);

            // set class loader to current context to allow unparcelling of SshAuthenticationApiError
            // http://stackoverflow.com/a/3806769
            result.setExtrasClassLoader(mContext.getClassLoader());

            return result;
        } catch (Exception e) {
            Log.e(TAG, "Exception in executeApi call", e);
            return createExceptionErrorResult(SshAuthenticationApiError.CLIENT_SIDE_ERROR,
                    "Exception in executeApi call", e);
        }
    }

    private Intent createErrorResult(int errorCode, String errorMessage) {
        Log.e(TAG, errorMessage);
        Intent result = new Intent();
        result.putExtra(SshAuthenticationApi.EXTRA_ERROR, new SshAuthenticationApiError(errorCode, errorMessage));
        result.putExtra(SshAuthenticationApi.EXTRA_RESULT_CODE, SshAuthenticationApi.RESULT_CODE_ERROR);
        return result;
    }

    private Intent createExceptionErrorResult(int errorCode, String errorMessage, Exception e) {
        String message = errorMessage + " : " + e.getMessage();
        return createErrorResult(errorCode, message);
    }
}
