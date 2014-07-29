/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.service;

import android.app.Activity;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Notify;

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

    public KeychainIntentServiceHandler(Activity activity,
                                        ProgressDialogFragment progressDialogFragment) {
        this.mActivity = activity;
        this.mProgressDialogFragment = progressDialogFragment;
    }

    public KeychainIntentServiceHandler(Activity activity, String progressDialogMessage,
                                        int progressDialogStyle) {
        this(activity, progressDialogMessage, progressDialogStyle, false, null);
    }

    public KeychainIntentServiceHandler(Activity activity, String progressDialogMessage,
                                        int progressDialogStyle, boolean cancelable,
                                        OnCancelListener onCancelListener) {
        this.mActivity = activity;
        this.mProgressDialogFragment = ProgressDialogFragment.newInstance(
                progressDialogMessage,
                progressDialogStyle,
                cancelable,
                onCancelListener);
    }

    public void showProgressDialog(FragmentActivity activity) {
        // TODO: This is a hack!, see
        // http://stackoverflow.com/questions/10114324/show-dialogfragment-from-onactivityresult
        final FragmentManager manager = activity.getSupportFragmentManager();
        Handler handler = new Handler();
        handler.post(new Runnable() {
            public void run() {
                mProgressDialogFragment.show(manager, "progressDialog");
            }
        });
    }

    @Override
    public void handleMessage(Message message) {
        Bundle data = message.getData();

        switch (message.arg1) {
            case MESSAGE_OKAY:
                mProgressDialogFragment.dismissAllowingStateLoss();

                break;

            case MESSAGE_EXCEPTION:
                mProgressDialogFragment.dismissAllowingStateLoss();

                // show error from service
                if (data.containsKey(DATA_ERROR)) {
                    Notify.showNotify(mActivity,
                            mActivity.getString(R.string.error_message, data.getString(DATA_ERROR)),
                            Notify.Style.ERROR);
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
                Log.e(Constants.TAG, "unknown handler message!");
                break;
        }
    }
}
