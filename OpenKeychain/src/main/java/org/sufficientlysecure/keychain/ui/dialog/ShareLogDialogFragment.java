/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;

public class ShareLogDialogFragment extends DialogFragment {
    private static final String ARG_STREAM = "stream";

    public static ShareLogDialogFragment newInstance(Uri stream) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_STREAM, stream);

        ShareLogDialogFragment fragment = new ShareLogDialogFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Uri stream = getArguments().getParcelable(ARG_STREAM);

        ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(getActivity());

        CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder(theme);
        builder.setTitle(R.string.share_log_dialog_title)
                .setMessage(R.string.share_log_dialog_message)
                .setNegativeButton(R.string.share_log_dialog_cancel_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dismiss();
                            }
                        })
                .setPositiveButton(R.string.share_log_dialog_share_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dismiss();
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.putExtra(Intent.EXTRA_STREAM, stream);
                                intent.setType("text/plain");
                                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(intent);
                            }
                        });

        return builder.show();
    }

}
