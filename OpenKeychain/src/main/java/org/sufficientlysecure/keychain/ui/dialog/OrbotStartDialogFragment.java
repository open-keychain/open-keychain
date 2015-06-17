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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.orbot.OrbotHelper;

/**
 * displays a dialog asking the user to enable Tor
 */
public class OrbotStartDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_MIDDLE_BUTTON = "middleButton";

    public static final int MESSAGE_MIDDLE_BUTTON = 1;

    public static OrbotStartDialogFragment newInstance(Messenger messenger, int title, int message, int middleButton) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_MESSENGER, messenger);
        args.putInt(ARG_TITLE, title);
        args.putInt(ARG_MESSAGE, message);
        args.putInt(ARG_MIDDLE_BUTTON, middleButton);

        OrbotStartDialogFragment fragment = new OrbotStartDialogFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Messenger messenger = getArguments().getParcelable(ARG_MESSENGER);
        int title = getArguments().getInt(ARG_TITLE);
        final int message = getArguments().getInt(ARG_MESSAGE);
        int middleButton = getArguments().getInt(ARG_MIDDLE_BUTTON);

        CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder(getActivity());
        builder.setTitle(title).setMessage(message);

        builder.setNegativeButton(R.string.orbot_start_dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.setPositiveButton(R.string.orbot_start_dialog_start, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getActivity().startActivityForResult(OrbotHelper.getOrbotStartIntent(), 1);
            }
        });

        builder.setNeutralButton(middleButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Message msg = new Message();
                msg.what = MESSAGE_MIDDLE_BUTTON;
                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
                } catch (NullPointerException e) {
                    Log.w(Constants.TAG, "Messenger is null!", e);
                }
            }
        });

        return builder.show();
    }
}
