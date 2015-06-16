/* This is the license for Orlib, a free software project to
        provide anonymity on the Internet from a Google Android smartphone.

        For more information about Orlib, see https://guardianproject.info/

        If you got this file as a part of a larger bundle, there may be other
        license terms that you should be aware of.
        ===============================================================================
        Orlib is distributed under this license (aka the 3-clause BSD license)

        Copyright (c) 2009-2010, Nathan Freitas, The Guardian Project

        Redistribution and use in source and binary forms, with or without
        modification, are permitted provided that the following conditions are
        met:

        * Redistributions of source code must retain the above copyright
        notice, this list of conditions and the following disclaimer.

        * Redistributions in binary form must reproduce the above
        copyright notice, this list of conditions and the following disclaimer
        in the documentation and/or other materials provided with the
        distribution.

        * Neither the names of the copyright owners nor the names of its
        contributors may be used to endorse or promote products derived from
        this software without specific prior written permission.

        THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
        "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
        LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
        A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
        OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
        SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
        LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
        DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
        THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
        (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
        OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

        *****
        Orlib contains a binary distribution of the JSocks library:
        http://code.google.com/p/jsocks-mirror/
        which is licensed under the GNU Lesser General Public License:
        http://www.gnu.org/licenses/lgpl.html

        *****
*/

package org.sufficientlysecure.keychain.util.orbot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.dialog.InstallDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.OrbotStartDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.PreferenceInstallDialogFragment;
import org.sufficientlysecure.keychain.util.Preferences;

/**
 * This class is taken from the NetCipher library: https://github.com/guardianproject/NetCipher/
 */
public class OrbotHelper {

    public final static String ORBOT_PACKAGE_NAME = "org.torproject.android";
    public final static String TOR_BIN_PATH = "/data/data/org.torproject.android/app_bin/tor";

    public final static String ACTION_START_TOR = "org.torproject.android.START_TOR";

    public static boolean isOrbotRunning() {
        int procId = TorServiceUtils.findProcessId(TOR_BIN_PATH);

        return (procId != -1);
    }

    public static boolean isOrbotInstalled(Context context) {
        return isAppInstalled(ORBOT_PACKAGE_NAME, context);
    }

    private static boolean isAppInstalled(String uri, Context context) {
        PackageManager pm = context.getPackageManager();
        boolean installed = false;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    /**
     * hack to get around the fact that PreferenceActivity still supports only android.app.DialogFragment
     *
     * @return
     */
    public static android.app.DialogFragment getPreferenceInstallDialogFragment() {
        return PreferenceInstallDialogFragment.newInstance(R.string.orbot_install_dialog_title,
                R.string.orbot_install_dialog_content, ORBOT_PACKAGE_NAME);
    }

    public static DialogFragment getInstallDialogFragment() {
        return InstallDialogFragment.newInstance(R.string.orbot_install_dialog_title,
                R.string.orbot_install_dialog_content, ORBOT_PACKAGE_NAME);
    }

    public static DialogFragment getInstallDialogFragmentWithThirdButton(Messenger messenger, int middleButton) {
        return InstallDialogFragment.newInstance(messenger, R.string.orbot_install_dialog_title,
                R.string.orbot_install_dialog_content, ORBOT_PACKAGE_NAME, middleButton, true);
    }

    public static DialogFragment getOrbotStartDialogFragment(Messenger messenger, int middleButton) {
        return OrbotStartDialogFragment.newInstance(messenger, R.string.orbot_start_dialog_title, R.string
                        .orbot_start_dialog_content,
                middleButton);
    }

    public static Intent getOrbotStartIntent() {
        Intent intent = new Intent(ACTION_START_TOR);
        intent.setPackage(ORBOT_PACKAGE_NAME);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * checks if Tor is enabled and if it is, that Orbot is installed and runnign. Generates appropriate dialogs.
     *
     * @param middleButton         resourceId of string to display as the middle button of install and enable dialogs
     * @param middleButtonRunnable runnable to be executed if the user clicks on the middle button
     * @param proxyPrefs
     * @param fragmentActivity
     * @return true if Tor is not enabled or Tor is enabled and Orbot is installed and running, else false
     */
    public static boolean isOrbotInRequiredState(int middleButton, final Runnable middleButtonRunnable,
                                                 Preferences.ProxyPrefs proxyPrefs, FragmentActivity fragmentActivity) {
        Handler ignoreTorHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // every message received by this handler will mean  the middle button was pressed
                middleButtonRunnable.run();
            }
        };

        if (!proxyPrefs.torEnabled) {
            return true;
        }

        if (!OrbotHelper.isOrbotInstalled(fragmentActivity)) {

            OrbotHelper.getInstallDialogFragmentWithThirdButton(
                    new Messenger(ignoreTorHandler),
                    R.string.orbot_install_dialog_ignore_tor
            ).show(fragmentActivity.getSupportFragmentManager(), "OrbotHelperOrbotInstallDialog");

            return false;
        } else if (!OrbotHelper.isOrbotRunning()) {

            OrbotHelper.getOrbotStartDialogFragment(new Messenger(ignoreTorHandler),
                    R.string.orbot_install_dialog_ignore_tor)
                    .show(fragmentActivity.getSupportFragmentManager(), "OrbotHelperOrbotStartDialog");

            return false;
        } else {
            return true;
        }
    }
}
