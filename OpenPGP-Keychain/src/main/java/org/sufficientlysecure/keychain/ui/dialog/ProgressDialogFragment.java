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

package org.sufficientlysecure.keychain.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;

import org.sufficientlysecure.keychain.R;

public class ProgressDialogFragment extends DialogFragment {
    private static final String ARG_MESSAGE_ID = "message_id";
    private static final String ARG_STYLE = "style";
    private static final String ARG_CANCELABLE = "cancelable";

    private OnCancelListener mOnCancelListener;

    /**
     * Creates new instance of this fragment
     *
     * @param messageId
     * @param style
     * @param cancelable
     * @return
     */
    public static ProgressDialogFragment newInstance(int messageId, int style, boolean cancelable,
                                                     OnCancelListener onCancelListener) {
        ProgressDialogFragment frag = new ProgressDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MESSAGE_ID, messageId);
        args.putInt(ARG_STYLE, style);
        args.putBoolean(ARG_CANCELABLE, cancelable);

        frag.setArguments(args);
        frag.mOnCancelListener = onCancelListener;

        return frag;
    }

    /**
     * Updates progress of dialog
     *
     * @param messageId
     * @param progress
     * @param max
     */
    public void setProgress(int messageId, int progress, int max) {
        setProgress(getString(messageId), progress, max);
    }

    /**
     * Updates progress of dialog
     *
     * @param progress
     * @param max
     */
    public void setProgress(int progress, int max) {
        ProgressDialog dialog = (ProgressDialog) getDialog();

        dialog.setProgress(progress);
        dialog.setMax(max);
    }

    /**
     * Updates progress of dialog
     *
     * @param message
     * @param progress
     * @param max
     */
    public void setProgress(String message, int progress, int max) {
        ProgressDialog dialog = (ProgressDialog) getDialog();

        dialog.setMessage(message);
        dialog.setProgress(progress);
        dialog.setMax(max);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        if (this.mOnCancelListener != null)
            this.mOnCancelListener.onCancel(dialog);
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        ProgressDialog dialog = new ProgressDialog(activity);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        int messageId = getArguments().getInt(ARG_MESSAGE_ID);
        int style = getArguments().getInt(ARG_STYLE);
        boolean cancelable = getArguments().getBoolean(ARG_CANCELABLE);
        dialog.setMessage(getString(messageId));
        dialog.setProgressStyle(style);

        if (cancelable) {
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                    activity.getString(R.string.progress_cancel),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
        }

        // Disable the back button
        OnKeyListener keyListener = new OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    return true;
                }
                return false;
            }

        };
        dialog.setOnKeyListener(keyListener);

        return dialog;
    }
}