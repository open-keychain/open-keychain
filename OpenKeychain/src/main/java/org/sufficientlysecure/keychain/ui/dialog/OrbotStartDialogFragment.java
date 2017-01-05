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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.network.orbot.OrbotHelper;

/**
 * displays a dialog asking the user to enable Tor
 */
public class OrbotStartDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_MIDDLE_BUTTON = "middleButton";

    private static final int ORBOT_REQUEST_CODE = 1;

    public static final int MESSAGE_MIDDLE_BUTTON = 1;
    public static final int MESSAGE_DIALOG_CANCELLED = 2; // for either cancel or enable pressed
    public static final int MESSAGE_ORBOT_STARTED = 3; // for either cancel or enable pressed

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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Messenger messenger = getArguments().getParcelable(ARG_MESSENGER);
        int title = getArguments().getInt(ARG_TITLE);
        final int message = getArguments().getInt(ARG_MESSAGE);
        int middleButton = getArguments().getInt(ARG_MIDDLE_BUTTON);
        final Activity activity = getActivity();

        ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(activity);

        CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder(theme);
        builder.setTitle(title).setMessage(message);

        builder.setNegativeButton(R.string.orbot_start_dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Message msg = Message.obtain();
                msg.what = MESSAGE_DIALOG_CANCELLED;
                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
                } catch (NullPointerException e) {
                    Log.w(Constants.TAG, "Messenger is null!", e);
                }

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

        builder.setPositiveButton(R.string.orbot_start_dialog_start, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // actual onClick defined in onStart, this is just to make the button appear
            }
        });

        return builder.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        //super.onStart() is where dialog.show() is actually called on the underlying dialog,
        // so we have to do it after this point
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    startActivityForResult(OrbotHelper.getShowOrbotStartIntent(),
                            ORBOT_REQUEST_CODE);
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ORBOT_REQUEST_CODE) {
            // assume Orbot was started
            final Messenger messenger = getArguments().getParcelable(ARG_MESSENGER);

            Message msg = Message.obtain();
            msg.what = MESSAGE_ORBOT_STARTED;
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
            } catch (NullPointerException e) {
                Log.w(Constants.TAG, "Messenger is null!", e);
            }
            dismiss();
        }
    }
}
