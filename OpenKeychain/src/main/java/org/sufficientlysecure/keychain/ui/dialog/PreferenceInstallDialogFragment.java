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

package org.sufficientlysecure.keychain.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Messenger;
import android.app.DialogFragment;

import org.sufficientlysecure.keychain.ui.util.InstallDialogFragmentHelper;

public class PreferenceInstallDialogFragment extends DialogFragment {

    public static final int MESSAGE_MIDDLE_CLICKED = 1;
    public static final int MESSAGE_DIALOG_DISMISSED = 2;

    /**
     * Creates a dialog which prompts the user to install an application. Consists of two default buttons ("Install"
     * and "Cancel") and an optional third button. Callbacks are provided only for the middle button, if set.
     *
     * @param messenger        required only for callback from middle button if it has been set
     * @param title
     * @param message          content of dialog
     * @param packageToInstall package name of application to install
     * @param middleButton     if not null, adds a third button to the app with a call back
     * @return The dialog to display
     */
    public static PreferenceInstallDialogFragment newInstance(Messenger messenger, int title, int message,
                                                              String packageToInstall, int middleButton, boolean
                                                                      useMiddleButton) {
        PreferenceInstallDialogFragment frag = new PreferenceInstallDialogFragment();
        Bundle args = new Bundle();

        InstallDialogFragmentHelper.wrapIntoArgs(messenger, title, message, packageToInstall, middleButton,
                useMiddleButton, args);

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
        return InstallDialogFragmentHelper.getInstallDialogFromArgs(getArguments(), getActivity(),
                MESSAGE_MIDDLE_CLICKED, MESSAGE_DIALOG_DISMISSED);
    }
}
