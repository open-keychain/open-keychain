/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import timber.log.Timber;


public class ServiceProgressHandler extends Handler {

    // possible messages sent from this service to handler on ui
    public enum MessageStatus {
        UNKNOWN,
        OKAY,
        EXCEPTION,
        UPDATE_PROGRESS,
        PREVENT_CANCEL;

        private static final MessageStatus[] values = values();

        public static MessageStatus fromInt(int n) {
            if (n < 0 || n >= values.length) {
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

    public static final String TAG_PROGRESS_DIALOG = "progressDialog";

    FragmentActivity mActivity;

    public ServiceProgressHandler(FragmentActivity activity) {
        mActivity = activity;
    }

    public void showProgressDialog() {
        showProgressDialog("", ProgressDialog.STYLE_SPINNER, false);
    }

    public void showProgressDialog(
            String progressDialogMessage, int progressDialogStyle, boolean cancelable) {

        final ProgressDialogFragment frag = ProgressDialogFragment.newInstance(
                progressDialogMessage,
                progressDialogStyle,
                cancelable);

        // TODO: This is a hack!, see
        // http://stackoverflow.com/questions/10114324/show-dialogfragment-from-onactivityresult
        final FragmentManager manager = mActivity.getSupportFragmentManager();
        Handler handler = new Handler();
        handler.post(new Runnable() {
            public void run() {
                frag.show(manager, TAG_PROGRESS_DIALOG);
            }
        });

    }

    @Override
    public void handleMessage(Message message) {
        Bundle data = message.getData();

        MessageStatus status = MessageStatus.fromInt(message.arg1);
        switch (status) {
            case OKAY:
                dismissAllowingStateLoss();

                break;

            case EXCEPTION:
                dismissAllowingStateLoss();

                // show error from service
                if (data.containsKey(DATA_ERROR)) {
                    Notify.create(mActivity,
                            mActivity.getString(R.string.error_message, data.getString(DATA_ERROR)),
                            Notify.Style.ERROR).show();
                }

                break;

            case UPDATE_PROGRESS:
                if (data.containsKey(DATA_PROGRESS) && data.containsKey(DATA_PROGRESS_MAX)) {

                    String msg = null;
                    int progress = data.getInt(DATA_PROGRESS);
                    int max = data.getInt(DATA_PROGRESS_MAX);

                    // update progress from service
                    if (data.containsKey(DATA_MESSAGE)) {
                        msg = data.getString(DATA_MESSAGE);
                    } else if (data.containsKey(DATA_MESSAGE_ID)) {
                        msg = mActivity.getString(data.getInt(DATA_MESSAGE_ID));
                    }

                    onSetProgress(msg, progress, max);

                }

                break;

            case PREVENT_CANCEL:
                setPreventCancel(true);
                break;

            default:
                Timber.e("unknown handler message!");
                break;
        }
    }

    private void setPreventCancel(boolean preventCancel) {
        ProgressDialogFragment progressDialogFragment =
                (ProgressDialogFragment) mActivity.getSupportFragmentManager()
                        .findFragmentByTag("progressDialog");

        if (progressDialogFragment == null) {
            return;
        }

        progressDialogFragment.setPreventCancel(preventCancel);
    }

    protected void dismissAllowingStateLoss() {
        ProgressDialogFragment progressDialogFragment =
                (ProgressDialogFragment) mActivity.getSupportFragmentManager()
                        .findFragmentByTag("progressDialog");

        if (progressDialogFragment == null) {
            return;
        }

        progressDialogFragment.dismissAllowingStateLoss();
    }


    protected void onSetProgress(String msg, int progress, int max) {

        ProgressDialogFragment progressDialogFragment =
                (ProgressDialogFragment) mActivity.getSupportFragmentManager()
                        .findFragmentByTag("progressDialog");

        if (progressDialogFragment == null) {
            return;
        }

        if (msg != null) {
            progressDialogFragment.setProgress(msg, progress, max);
        } else {
            progressDialogFragment.setProgress(progress, max);
        }

    }

}
