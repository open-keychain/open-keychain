/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.service;

import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;
import org.sufficientlysecure.keychain.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

public class KeychainIntentServiceHandler extends Handler {

    // possible messages send from this service to handler on ui
    public static final int MESSAGE_OKAY = 1;
    public static final int MESSAGE_EXCEPTION = 2;
    public static final int MESSAGE_UPDATE_PROGRESS = 3;

    // possible data keys for messages
    public static final String DATA_ERROR = "error";
    public static final String DATA_PROGRESS = "progress";
    public static final String DATA_PROGRESS_MAX = "max";
    public static final String DATA_MESSAGE = "message";
    public static final String DATA_MESSAGE_ID = "message_id";

    Activity mActivity;
    ProgressDialogFragment mProgressDialogFragment;

    public KeychainIntentServiceHandler(Activity activity) {
        this.mActivity = activity;
    }

    public KeychainIntentServiceHandler(Activity activity, ProgressDialogFragment progressDialogFragment) {
        this.mActivity = activity;
        this.mProgressDialogFragment = progressDialogFragment;
    }

    public KeychainIntentServiceHandler(Activity activity, int progressDialogMessageId, int progressDialogStyle) {
        this.mActivity = activity;
        this.mProgressDialogFragment = ProgressDialogFragment.newInstance(progressDialogMessageId,
                progressDialogStyle);
    }

    public void showProgressDialog(FragmentActivity activity) {
        mProgressDialogFragment.show(activity.getSupportFragmentManager(), "progressDialog");
    }

    @Override
    public void handleMessage(Message message) {
        Bundle data = message.getData();

        switch (message.arg1) {
        case MESSAGE_OKAY:
            mProgressDialogFragment.dismiss();

            break;

        case MESSAGE_EXCEPTION:
            mProgressDialogFragment.dismiss();

            // show error from service
            if (data.containsKey(DATA_ERROR)) {
                Toast.makeText(mActivity,
                        mActivity.getString(R.string.errorMessage, data.getString(DATA_ERROR)),
                        Toast.LENGTH_SHORT).show();
            }

            break;

        case MESSAGE_UPDATE_PROGRESS:
            if (data.containsKey(DATA_PROGRESS) && data.containsKey(DATA_PROGRESS_MAX)) {

                // update progress from service
                if (data.containsKey(DATA_MESSAGE)) {
                    mProgressDialogFragment.setProgress(data.getString(DATA_MESSAGE),
                            data.getInt(DATA_PROGRESS), data.getInt(DATA_PROGRESS_MAX));
                } else if (data.containsKey(DATA_MESSAGE_ID)) {
                    mProgressDialogFragment.setProgress(data.getInt(DATA_MESSAGE_ID),
                            data.getInt(DATA_PROGRESS), data.getInt(DATA_PROGRESS_MAX));
                } else {
                    mProgressDialogFragment.setProgress(data.getInt(DATA_PROGRESS),
                            data.getInt(DATA_PROGRESS_MAX));
                }
            }

            break;

        default:
            break;
        }
    }
}
