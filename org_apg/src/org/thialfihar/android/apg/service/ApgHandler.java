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

package org.thialfihar.android.apg.service;

import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.ui.ProgressDialogFragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class ApgHandler extends Handler {

    // possible messages send from this service to handler on ui
    public static final int MESSAGE_OKAY = 1;
    public static final int MESSAGE_EXCEPTION = 2;
    public static final int MESSAGE_UPDATE_PROGRESS = 3;

    // possible data keys for messages
    public static final String ERROR = "error";
    public static final String PROGRESS = "progress";
    public static final String PROGRESS_MAX = "max";
    public static final String MESSAGE = "message";
    public static final String MESSAGE_ID = "message_id";
    
    // generate key results
    public static final String NEW_KEY = "new_key";


    Activity mActivity;

    ProgressDialogFragment mProgressDialogFragment;

    public ApgHandler(Activity activity) {
        this.mActivity = activity;
    }

    public ApgHandler(Activity activity, ProgressDialogFragment progressDialogFragment) {
        this.mActivity = activity;
        this.mProgressDialogFragment = progressDialogFragment;
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

            if (data.containsKey(ERROR)) {
                Toast.makeText(mActivity,
                        mActivity.getString(R.string.errorMessage, data.getString(ERROR)),
                        Toast.LENGTH_SHORT).show();
            }

            break;

        case MESSAGE_UPDATE_PROGRESS:
            if (data.containsKey(PROGRESS) && data.containsKey(PROGRESS_MAX)) {

                if (data.containsKey(MESSAGE)) {
                    mProgressDialogFragment.setProgress(data.getString(MESSAGE),
                            data.getInt(PROGRESS), data.getInt(PROGRESS_MAX));
                } else if (data.containsKey(MESSAGE_ID)) {
                    mProgressDialogFragment.setProgress(data.getInt(MESSAGE_ID),
                            data.getInt(PROGRESS), data.getInt(PROGRESS_MAX));

                } else {
                    mProgressDialogFragment.setProgress(data.getInt(PROGRESS),
                            data.getInt(PROGRESS_MAX));
                }

            }

            break;

        default:
            break;
        }
    }
}
