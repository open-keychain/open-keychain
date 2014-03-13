/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import org.sufficientlysecure.htmltextview.HtmlTextView;
import org.sufficientlysecure.keychain.R;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ShareNfcDialogFragment extends DialogFragment {

    /**
     * Creates new instance of this fragment
     */
    public static ShareNfcDialogFragment newInstance() {
        ShareNfcDialogFragment frag = new ShareNfcDialogFragment();

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        alert.setTitle(R.string.share_nfc_dialog);
        alert.setCancelable(true);

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        HtmlTextView textView = new HtmlTextView(getActivity());
        textView.setPadding(8, 8, 8, 8);
        alert.setView(textView);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            textView.setText(getString(R.string.error) + ": "
                    + getString(R.string.error_jelly_bean_needed));
        } else {
            // check if NFC Adapter is available
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
            if (nfcAdapter == null) {
                textView.setText(getString(R.string.error) + ": "
                        + getString(R.string.error_nfc_needed));
            } else {
                // nfc works...
                textView.setHtmlFromRawResource(getActivity(), R.raw.nfc_beam_share);

                alert.setNegativeButton(R.string.menu_beam_preferences,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intentSettings = new Intent(
                                        Settings.ACTION_NFCSHARING_SETTINGS);
                                startActivity(intentSettings);
                            }
                        });
            }
        }

        // no flickering when clicking textview for Android < 4
        // aboutTextView.setTextColor(getResources().getColor(android.R.color.black));

        return alert.create();
    }
}