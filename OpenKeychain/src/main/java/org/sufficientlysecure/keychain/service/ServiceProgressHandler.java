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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.CertifyResult;
import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.Log;

public class ServiceProgressHandler extends Handler {

    // possible messages sent from this service to handler on ui
    public static enum MessageStatus{
        UNKNOWN,
        OKAY,
        EXCEPTION,
        UPDATE_PROGRESS,
        PREVENT_CANCEL;

        private static final MessageStatus[] values = values();

        public static MessageStatus fromInt(int n)
        {
            if(n < 0 || n >= values.length) {
                return UNKNOWN;
            } else {
                return values[n];
            }
        }
    }

    // possible data keys for messages
    public static final String DATA_ERROR = "error";
    public static final String DATA_PROGRESS = "progress";
    public static final String DATA_PROGRESS_MAX = "max";
    public static final String DATA_MESSAGE = "message";
    public static final String DATA_MESSAGE_ID = "message_id";

    // keybase proof specific
    public static final String KEYBASE_PROOF_URL = "keybase_proof_url";
    public static final String KEYBASE_PRESENCE_URL = "keybase_presence_url";
    public static final String KEYBASE_PRESENCE_LABEL = "keybase_presence_label";

    Activity mActivity;
    ProgressDialogFragment mProgressDialogFragment;

    public ServiceProgressHandler(Activity activity) {
        this.mActivity = activity;
    }

    public ServiceProgressHandler(Activity activity,
                                  ProgressDialogFragment progressDialogFragment) {
        this.mActivity = activity;
        this.mProgressDialogFragment = progressDialogFragment;
    }

    public ServiceProgressHandler(Activity activity,
                                  String progressDialogMessage,
                                  int progressDialogStyle,
                                  ProgressDialogFragment.ServiceType serviceType) {
        this(activity, progressDialogMessage, progressDialogStyle, false, serviceType);
    }

    public ServiceProgressHandler(Activity activity,
                                  String progressDialogMessage,
                                  int progressDialogStyle,
                                  boolean cancelable,
                                  ProgressDialogFragment.ServiceType serviceType) {
        this.mActivity = activity;
        this.mProgressDialogFragment = ProgressDialogFragment.newInstance(
                progressDialogMessage,
                progressDialogStyle,
                cancelable,
                serviceType);
    }

    public void showProgressDialog(FragmentActivity activity) {
        if (mProgressDialogFragment == null) {
            return;
        }

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

        if (mProgressDialogFragment == null) {
            // Log.e(Constants.TAG,
            // "Progress has not been updated because mProgressDialogFragment was null!");
            return;
        }

        MessageStatus status = MessageStatus.fromInt(message.arg1);
        switch (status) {
            case OKAY:
                mProgressDialogFragment.dismissAllowingStateLoss();

                break;

            case EXCEPTION:
                mProgressDialogFragment.dismissAllowingStateLoss();

                // show error from service
                if (data.containsKey(DATA_ERROR)) {
                    Notify.create(mActivity,
                            mActivity.getString(R.string.error_message, data.getString(DATA_ERROR)),
                            Notify.Style.ERROR).show();
                }

                break;

            case UPDATE_PROGRESS:
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

            case PREVENT_CANCEL:
                mProgressDialogFragment.setPreventCancel(true);
                break;

            default:
                Log.e(Constants.TAG, "unknown handler message!");
                break;
        }
    }
}
