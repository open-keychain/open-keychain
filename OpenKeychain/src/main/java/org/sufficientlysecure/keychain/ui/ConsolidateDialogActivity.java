/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;

/**
 * We can not directly create a dialog on the application context.
 * This activity encapsulates a DialogFragment to emulate a dialog.
 */
public class ConsolidateDialogActivity extends FragmentActivity {

    MyDialogFragment mDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // this activity itself has no content view (see manifest)

        mDialogFragment = new MyDialogFragment();
        // give all extras through to the fragment
        mDialogFragment.setArguments(getIntent().getExtras());

        mDialogFragment.show(getSupportFragmentManager(), "dialog");
    }

    public static class MyDialogFragment extends DialogFragment {

        /**
         * Creates dialog
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
            ContextThemeWrapper context = new ContextThemeWrapper(getActivity(),
                    R.style.Theme_AppCompat_Light);
            ProgressDialog dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);

            // Disable the back button
            DialogInterface.OnKeyListener keyListener = new DialogInterface.OnKeyListener() {
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

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);

            dismiss();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            Log.d(Constants.TAG, "onDismiss");

            getActivity().finish();
        }

    }

}
