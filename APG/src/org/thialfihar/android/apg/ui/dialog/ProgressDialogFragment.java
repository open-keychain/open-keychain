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

package org.thialfihar.android.apg.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;

public class ProgressDialogFragment extends DialogFragment {
    private static final String ARG_MESSAGE_ID = "message_id";
    private static final String ARG_STYLE = "style";

    /**
     * Creates new instance of this fragment
     * 
     * @param id
     * @return
     */
    public static ProgressDialogFragment newInstance(int messageId, int style) {
        ProgressDialogFragment frag = new ProgressDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MESSAGE_ID, messageId);
        args.putInt(ARG_STYLE, style);

        frag.setArguments(args);
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
     * @param messageId
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
     * @param messageId
     * @param progress
     * @param max
     */
    public void setProgress(String message, int progress, int max) {
        ProgressDialog dialog = (ProgressDialog) getDialog();

        dialog.setMessage(message);
        dialog.setProgress(progress);
        dialog.setMax(max);
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();

        ProgressDialog dialog = new ProgressDialog(activity);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        int messageId = getArguments().getInt(ARG_MESSAGE_ID);
        int style = getArguments().getInt(ARG_STYLE);

        dialog.setMessage(getString(messageId));
        dialog.setProgressStyle(style);

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