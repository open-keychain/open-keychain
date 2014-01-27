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
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.view.KeyEvent;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class ProgressDialogFragment extends SherlockDialogFragment {
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