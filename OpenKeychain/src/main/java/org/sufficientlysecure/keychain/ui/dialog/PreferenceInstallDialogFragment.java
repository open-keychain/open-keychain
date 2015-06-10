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
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.app.DialogFragment;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

public class PreferenceInstallDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_MIDDLE_BUTTON = "middleButton";
    private static final String ARG_INSTALL_PATH = "installPath";
    private static final String ARG_USE_MIDDLE_BUTTON = "installPath";

    public static final String PLAY_STORE_PATH = "market://search?q=pname:";

    public static final int MESSAGE_MIDDLE_CLICKED = 1;

    /**
     * Creates a dialog which prompts the user to install an application. Consists of two default buttons ("Install"
     * and "Cancel") and an optional third button. Callbacks are provided only for the middle button, if set.
     *
     * @param messenger required only for callback from middle button if it has been set
     * @param title
     * @param message content of dialog
     * @param packageToInstall package name of application to install
     * @param middleButton if not null, adds a third button to the app with a call back
     * @return The dialog to display
     */
    public static PreferenceInstallDialogFragment newInstance(Messenger messenger, int title, int message,
                                                 String packageToInstall, int middleButton, boolean useMiddleButton) {
        PreferenceInstallDialogFragment frag = new PreferenceInstallDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MESSENGER, messenger);

        args.putInt(ARG_TITLE, title);
        args.putInt(ARG_MESSAGE, message);
        args.putInt(ARG_MIDDLE_BUTTON, middleButton);
        args.putString(ARG_INSTALL_PATH, PLAY_STORE_PATH + packageToInstall);
        args.putBoolean(ARG_USE_MIDDLE_BUTTON, useMiddleButton);

        frag.setArguments(args);

        return frag;
    }

    /**
     * To create a DialogFragment with only two buttons
     *
     * @param title
     * @param message
     * @param packageToInstall
     * @return
     */
    public static PreferenceInstallDialogFragment newInstance(int title, int message,
                                                    String packageToInstall) {
        return newInstance(null, title, message, packageToInstall, -1, false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        final Messenger messenger = getArguments().getParcelable(ARG_MESSENGER);

        final String title = getArguments().getString(ARG_TITLE);
        final String message = getArguments().getString(ARG_MESSAGE);
        final String installPath = getArguments().getString(ARG_INSTALL_PATH);
        final String middleButton = getArguments().getString(ARG_MIDDLE_BUTTON);
        final boolean useMiddleButton = getArguments().getBoolean(ARG_USE_MIDDLE_BUTTON);

        CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder(activity);

        builder.setTitle(title).setMessage(message);

        builder.setNegativeButton(R.string.orbot_install_dialog_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        builder.setPositiveButton(R.string.orbot_install_dialog_install,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse(installPath);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        activity.startActivity(intent);
                    }
                }
        );

        if (useMiddleButton) {
            builder.setNeutralButton(middleButton,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Message msg = new Message();
                            msg.what=MESSAGE_MIDDLE_CLICKED;
                            try {
                                messenger.send(msg);
                            } catch (RemoteException e) {
                                Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
                            } catch (NullPointerException e) {
                                Log.w(Constants.TAG, "Messenger is null!", e);
                            }
                        }
                    }
            );
        }

        return builder.show();
    }
}
