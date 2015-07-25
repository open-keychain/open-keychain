/*
 * Copyright (C) 2013-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2015 Adithya Abraham Philip <adithyaphilip@gmail.com>
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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.ContextThemeWrapper;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;

public class RetryUploadDialogActivity extends FragmentActivity {

    public static final String EXTRA_CRYPTO_INPUT = "extra_crypto_input";

    public static final String RESULT_CRYPTO_INPUT = "result_crypto_input";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UploadRetryDialogFragment.newInstance().show(getSupportFragmentManager(),
                "uploadRetryDialog");
    }

    public static class UploadRetryDialogFragment extends DialogFragment {
        public static UploadRetryDialogFragment newInstance() {
            return new UploadRetryDialogFragment();
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(getActivity());

            CustomAlertDialogBuilder dialogBuilder = new CustomAlertDialogBuilder(theme);
            dialogBuilder.setTitle(R.string.retry_up_dialog_title);
            dialogBuilder.setMessage(R.string.retry_up_dialog_message);

            dialogBuilder.setNegativeButton(R.string.retry_up_dialog_btn_cancel,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().setResult(RESULT_CANCELED);
                    getActivity().finish();
                }
            });

            dialogBuilder.setPositiveButton(R.string.retry_up_dialog_btn_reupload,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent();
                    intent.putExtra(RESULT_CRYPTO_INPUT, getActivity()
                            .getIntent().getParcelableExtra(EXTRA_CRYPTO_INPUT));
                    getActivity().setResult(RESULT_OK, intent);
                    getActivity().finish();
                }
            });

            return dialogBuilder.show();
        }
    }
}
