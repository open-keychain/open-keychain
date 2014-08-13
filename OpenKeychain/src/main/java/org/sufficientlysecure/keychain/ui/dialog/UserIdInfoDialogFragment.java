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

package org.sufficientlysecure.keychain.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;

public class UserIdInfoDialogFragment extends DialogFragment {
    private static final String ARG_IS_REVOKED = "is_revoked";
    private static final String ARG_IS_VERIFIED = "is_verified";

    /**
     * Creates new instance of this dialog fragment
     */
    public static UserIdInfoDialogFragment newInstance(boolean isRevoked, int isVerified) {
        UserIdInfoDialogFragment frag = new UserIdInfoDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_REVOKED, isRevoked);
        args.putInt(ARG_IS_VERIFIED, isVerified);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        int isVerified = getArguments().getInt(ARG_IS_VERIFIED);
        boolean isRevoked = getArguments().getBoolean(ARG_IS_REVOKED);

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(activity);

        String title;
        String message;
        if (isRevoked) {
            title = getString(R.string.user_id_info_revoked_title);
            message = getString(R.string.user_id_info_revoked_text);
        } else {
            switch (isVerified) {
                case KeychainContract.Certs.VERIFIED_SECRET:
                    title = getString(R.string.user_id_info_verified_title);
                    message = getString(R.string.user_id_info_verified_text);
                    break;
                case KeychainContract.Certs.VERIFIED_SELF:
                    title = getString(R.string.user_id_info_not_verified_title);
                    message = getString(R.string.user_id_info_not_verified_text);
                    break;
                default:
                    title = getString(R.string.user_id_info_invalid_title);
                    message = getString(R.string.user_id_info_invalid_text);
                    break;
            }
        }

        alert.setTitle(title);
        alert.setMessage(message);

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        return alert.show();
    }

}
