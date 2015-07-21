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

package org.sufficientlysecure.keychain.ui.util;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.util.Log;

public class InstallDialogFragmentHelper {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_MIDDLE_BUTTON = "middleButton";
    private static final String ARG_INSTALL_PATH = "installPath";
    private static final String ARG_USE_MIDDLE_BUTTON = "useMiddleButton";

    private static final String PLAY_STORE_PATH = "market://search?q=pname:";

    public static void wrapIntoArgs(Messenger messenger, int title, int message, String packageToInstall,
                                      int middleButton, boolean useMiddleButton, Bundle args) {
        args.putParcelable(ARG_MESSENGER, messenger);

        args.putInt(ARG_TITLE, title);
        args.putInt(ARG_MESSAGE, message);
        args.putInt(ARG_MIDDLE_BUTTON, middleButton);
        args.putString(ARG_INSTALL_PATH, PLAY_STORE_PATH + packageToInstall);
        args.putBoolean(ARG_USE_MIDDLE_BUTTON, useMiddleButton);
    }

    public static AlertDialog getInstallDialogFromArgs(Bundle args, final Activity activity,
                                                       final int messengerMiddleButtonClicked,
                                                       final int messengerDialogDimissed) {
        final Messenger messenger = args.getParcelable(ARG_MESSENGER);

        final int title = args.getInt(ARG_TITLE);
        final int message = args.getInt(ARG_MESSAGE);
        final int middleButton = args.getInt(ARG_MIDDLE_BUTTON);
        final String installPath = args.getString(ARG_INSTALL_PATH);
        final boolean useMiddleButton = args.getBoolean(ARG_USE_MIDDLE_BUTTON);

        ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(activity);
        CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder(theme);

        builder.setTitle(title).setMessage(message);

        builder.setNegativeButton(R.string.orbot_install_dialog_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Message msg = Message.obtain();
                        msg.what = messengerDialogDimissed;
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                            Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
                        } catch (NullPointerException e) {
                            Log.w(Constants.TAG, "Messenger is null!", e);
                        }
                    }
                });

        builder.setPositiveButton(R.string.orbot_install_dialog_install,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse(installPath);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        activity.startActivity(intent);

                        Message msg = Message.obtain();
                        msg.what = messengerDialogDimissed;
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

        if (useMiddleButton) {
            builder.setNeutralButton(middleButton,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Message msg = Message.obtain();
                            msg.what = messengerMiddleButtonClicked;
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
