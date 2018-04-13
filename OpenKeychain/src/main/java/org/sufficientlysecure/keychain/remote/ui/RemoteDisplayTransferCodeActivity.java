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

package org.sufficientlysecure.keychain.remote.ui;


import java.nio.CharBuffer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.ui.widget.PrefixedEditText;
import org.sufficientlysecure.keychain.util.Numeric9x4PassphraseUtil;
import org.sufficientlysecure.keychain.util.Passphrase;


public class RemoteDisplayTransferCodeActivity extends FragmentActivity {
    public static final String EXTRA_TRANSFER_CODE = "transfer_code";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            DisplayTransferCodeDialogFragment frag = new DisplayTransferCodeDialogFragment();
            frag.setArguments(getIntent().getExtras());
            frag.show(getSupportFragmentManager(), "displayTransferCode");
        }
    }

    public static class DisplayTransferCodeDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();

            Passphrase transferCode = getArguments().getParcelable(EXTRA_TRANSFER_CODE);

            ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(activity);
            CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);

            @SuppressLint("InflateParams")
            View view = LayoutInflater.from(theme).inflate(R.layout.api_display_transfer_code, null, false);
            alert.setView(view);
            alert.setPositiveButton(R.string.button_got_it, (dialog, which) -> dismiss());

            TextView[] transferCodeTextViews = new TextView[9];
            transferCodeTextViews[0] = view.findViewById(R.id.transfer_code_block_1);
            transferCodeTextViews[1] = view.findViewById(R.id.transfer_code_block_2);
            transferCodeTextViews[2] = view.findViewById(R.id.transfer_code_block_3);
            transferCodeTextViews[3] = view.findViewById(R.id.transfer_code_block_4);
            transferCodeTextViews[4] = view.findViewById(R.id.transfer_code_block_5);
            transferCodeTextViews[5] = view.findViewById(R.id.transfer_code_block_6);
            transferCodeTextViews[6] = view.findViewById(R.id.transfer_code_block_7);
            transferCodeTextViews[7] = view.findViewById(R.id.transfer_code_block_8);
            transferCodeTextViews[8] = view.findViewById(R.id.transfer_code_block_9);

            setTransferCode(transferCodeTextViews, transferCode);

            return alert.create();
        }

        private void setTransferCode(TextView[] view, Passphrase transferCode) {
            CharBuffer transferCodeChars = CharBuffer.wrap(transferCode.getCharArray()).asReadOnlyBuffer();
            if (!Numeric9x4PassphraseUtil.isNumeric9x4Passphrase(transferCodeChars)) {
                throw new IllegalStateException("Illegal passphrase format!");
            }

            PrefixedEditText prefixedEditText = (PrefixedEditText) view[0];
            prefixedEditText.setHint("34");
            prefixedEditText.setPrefix(transferCodeChars.subSequence(0, 2));
            prefixedEditText.setText(transferCodeChars.subSequence(2, 4));

            for (int i = 1; i < 9; i++) {
                view[i].setText(transferCodeChars.subSequence(i*5, i*5+4));
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            getActivity().finish();
        }
    }

}
