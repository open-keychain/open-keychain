/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.KeychainService;

/**
 * meant to be used
 */
public class ProgressDialogFragment extends DialogFragment {
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_STYLE = "style";
    private static final String ARG_CANCELABLE = "cancelable";
    private static final String ARG_SERVICE_TYPE = "service_class";

    boolean mCanCancel = false, mPreventCancel = false, mIsCancelled = false;

    /**
     * creates a new instance of this fragment
     * @param message the message to be displayed initially above the progress bar
     * @param style the progress bar style, as defined in ProgressDialog (horizontal or spinner)
     * @param cancelable should we let the user cancel this operation
     * @return
     */
    public static ProgressDialogFragment newInstance(String message, int style, boolean cancelable) {
        ProgressDialogFragment frag = new ProgressDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        args.putInt(ARG_STYLE, style);
        args.putBoolean(ARG_CANCELABLE, cancelable);

        frag.setArguments(args);

        return frag;
    }

    public void setProgress(int messageId, int progress, int max) {
        setProgress(getString(messageId), progress, max);
    }

    public void setProgress(int progress, int max) {
        ProgressDialog dialog = (ProgressDialog) getDialog();

        if (mIsCancelled || dialog == null) {
            return;
        }

        dialog.setProgress(progress);
        dialog.setMax(max);
    }

    public void setProgress(String message, int progress, int max) {
        ProgressDialog dialog = (ProgressDialog) getDialog();

        if (mIsCancelled || dialog == null) {
            return;
        }

        dialog.setMessage(message);
        dialog.setProgress(progress);
        dialog.setMax(max);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        // if the progress dialog is displayed from the application class, design is missing
        // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper context = new ContextThemeWrapper(activity,
                R.style.Theme_AppCompat_Light);

        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        // We never use the builtin cancel method
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        String message = getArguments().getString(ARG_MESSAGE);
        int style = getArguments().getInt(ARG_STYLE);
        mCanCancel = getArguments().getBoolean(ARG_CANCELABLE);

        dialog.setMessage(message);
        dialog.setProgressStyle(style);

        // If this is supposed to be cancelable, add our (custom) cancel mechanic
        if (mCanCancel) {
            // Just show the button, take care of the onClickListener afterwards (in onStart)
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                    activity.getString(R.string.progress_cancel), (DialogInterface.OnClickListener) null);
        }

        // Disable the back button regardless
        OnKeyListener keyListener = new OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (mCanCancel) {
                        ((ProgressDialog) dialog).getButton(
                                DialogInterface.BUTTON_NEGATIVE).performClick();
                    }
                    // return true, indicating we handled this
                    return true;
                }
                return false;
            }
        };
        dialog.setOnKeyListener(keyListener);

        return dialog;
    }

    public void setPreventCancel(boolean preventCancel) {
        // Don't care if we can't cancel anymore either way!
        if (mIsCancelled || ! mCanCancel) {
            return;
        }

        mPreventCancel = preventCancel;
        ProgressDialog dialog = (ProgressDialog) getDialog();
        if (dialog == null) {
            return;
        }

        final Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        negative.setEnabled(mIsCancelled && !preventCancel);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Override the default behavior so the dialog is NOT dismissed on click
        final Button negative = ((ProgressDialog) getDialog()).getButton(DialogInterface.BUTTON_NEGATIVE);
        negative.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // nvm if we are already cancelled, or weren't able to begin with
                if (mIsCancelled || ! mCanCancel) {
                    return;
                }

                // Remember this, and don't allow another click
                mIsCancelled = true;
                negative.setClickable(false);
                negative.setTextColor(Color.GRAY);

                // send a cancel message. note that this message will be handled by
                // KeychainService.onStartCommand, which runs in this thread,
                // not the service one, and will not queue up a command.
                Intent serviceIntent = new Intent(getActivity(), KeychainService.class);

                serviceIntent.setAction(KeychainService.ACTION_CANCEL);
                getActivity().startService(serviceIntent);

                // Set the progress bar accordingly
                ProgressDialog dialog = (ProgressDialog) getDialog();
                if (dialog == null) {
                    return;
                }

                dialog.setIndeterminate(true);
                dialog.setMessage(getString(R.string.progress_cancelling));


            }
        });

    }

}
