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
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.dialog.SupportInstallDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.OrbotStartDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.PreferenceInstallDialogFragment;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

import java.util.List;

/**
 * This class is taken from the NetCipher library: https://github.com/guardianproject/NetCipher/
 */
public class OrbotHelper {

    public interface DialogActions {
        void onOrbotStarted();

        void onNeutralButton();

        void onCancel();
    }

    private final static int REQUEST_CODE_STATUS = 100;

    public final static String ORBOT_PACKAGE_NAME = "org.torproject.android";
    public final static String ORBOT_MARKET_URI = "market://details?id=" + ORBOT_PACKAGE_NAME;
    public final static String ORBOT_FDROID_URI = "https://f-droid.org/repository/browse/?fdid="
            + ORBOT_PACKAGE_NAME;
    public final static String ORBOT_PLAY_URI = "https://play.google.com/store/apps/details?id="
            + ORBOT_PACKAGE_NAME;

    /**
     * A request to Orbot to transparently start Tor services
     */
    public final static String ACTION_START = "org.torproject.android.intent.action.START";
    /**
     * {@link Intent} send by Orbot with {@code ON/OFF/STARTING/STOPPING} status
     */
    public final static String ACTION_STATUS = "org.torproject.android.intent.action.STATUS";
    /**
     * {@code String} that contains a status constant: {@link #STATUS_ON},
     * {@link #STATUS_OFF}, {@link #STATUS_STARTING}, or
     * {@link #STATUS_STOPPING}
     */
    public final static String EXTRA_STATUS = "org.torproject.android.intent.extra.STATUS";
    /**
     * A {@link String} {@code packageName} for Orbot to direct its status reply
     * to, used in {@link #ACTION_START} {@link Intent}s sent to Orbot
     */
    public final static String EXTRA_PACKAGE_NAME = "org.torproject.android.intent.extra.PACKAGE_NAME";

    /**
     * All tor-related services and daemons are stopped
     */
    @SuppressWarnings("unused") // we might use this later, sent by Orbot
    public final static String STATUS_OFF = "OFF";
    /**
     * All tor-related services and daemons have completed starting
     */
    public final static String STATUS_ON = "ON";
    @SuppressWarnings("unused") // we might use this later, sent by Orbot
    public final static String STATUS_STARTING = "STARTING";
    @SuppressWarnings("unused") // we might use this later, sent by Orbot
    public final static String STATUS_STOPPING = "STOPPING";
    /**
     * The user has disabled the ability for background starts triggered by
     * apps. Fallback to the old Intent that brings up Orbot.
     */
    public final static String STATUS_STARTS_DISABLED = "STARTS_DISABLED";

    public final static String ACTION_START_TOR = "org.torproject.android.START_TOR";
    /**
     * request code used to start tor
     */
    public final static int START_TOR_RESULT = 0x9234;

    private final static String FDROID_PACKAGE_NAME = "org.fdroid.fdroid";
    private final static String PLAY_PACKAGE_NAME = "com.android.vending";

    private OrbotHelper() {
        // only static utility methods, do not instantiate
    }

    /**
     * Initialize the OrbotStatusReceiver (if not already happened) and check, whether Orbot is
     * running or not.
     * @param context context
     * @return if Orbot is running
     */
    public static boolean isOrbotRunning(Context context) {
        return OrbotStatusReceiver.getInstance().isTorRunning(context);
    }

    public static boolean isOrbotInstalled(Context context) {
        return isAppInstalled(context, ORBOT_PACKAGE_NAME);
    }

    private static boolean isAppInstalled(Context context, String uri) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * First, checks whether Orbot is installed, then checks whether Orbot is
     * running. If Orbot is installed and not running, then an {@link Intent} is
     * sent to request Orbot to start, which will show the main Orbot screen.
     * The result will be returned in
     * {@link Activity#onActivityResult(int requestCode, int resultCode, Intent data)}
     * with a {@code requestCode} of {@code START_TOR_RESULT}
     *
     * @param activity the {@link Activity} that gets the
     *                 {@code START_TOR_RESULT} result
     * @return whether the start request was sent to Orbot
     */
    public static boolean requestShowOrbotStart(Activity activity) {
        if (OrbotHelper.isOrbotInstalled(activity)) {
            if (!OrbotHelper.isOrbotRunning(activity)) {
                Intent intent = getShowOrbotStartIntent();
                activity.startActivityForResult(intent, START_TOR_RESULT);
                return true;
            }
        }
        return false;
    }

    public static Intent getShowOrbotStartIntent() {
        Intent intent = new Intent(ACTION_START_TOR);
        intent.setPackage(ORBOT_PACKAGE_NAME);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * First, checks whether Orbot is installed. If Orbot is installed, then a
     * broadcast {@link Intent} is sent to request Orbot to start transparently
     * in the background. When Orbot receives this {@code Intent}, it will
     * immediately reply to this all with its status via an
     * {@link #ACTION_STATUS} {@code Intent} that is broadcast to the
     * {@code packageName} of the provided {@link Context} (i.e.
     * {@link Context#getPackageName()}.
     *
     * @param context the app {@link Context} will receive the reply
     * @return whether the start request was sent to Orbot
     */
    public static boolean requestStartTor(Context context) {
        if (OrbotHelper.isOrbotInstalled(context)) {
            Log.i("OrbotHelper", "requestStartTor " + context.getPackageName());
            Intent intent = getOrbotStartIntent();
            intent.putExtra(EXTRA_PACKAGE_NAME, context.getPackageName());
            context.sendBroadcast(intent);
            return true;
        }
        return false;
    }

    public static Intent getOrbotStartIntent() {
        Intent intent = new Intent(ACTION_START);
        intent.setPackage(ORBOT_PACKAGE_NAME);
        return intent;
    }

    public static Intent getOrbotInstallIntent(Context context) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(ORBOT_MARKET_URI));

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resInfos = pm.queryIntentActivities(intent, 0);

        String foundPackageName = null;
        for (ResolveInfo r : resInfos) {
            Log.i("OrbotHelper", "market: " + r.activityInfo.packageName);
            if (TextUtils.equals(r.activityInfo.packageName, FDROID_PACKAGE_NAME)
                    || TextUtils.equals(r.activityInfo.packageName, PLAY_PACKAGE_NAME)) {
                foundPackageName = r.activityInfo.packageName;
                break;
            }
        }

        if (foundPackageName == null) {
            intent.setData(Uri.parse(ORBOT_FDROID_URI));
        } else {
            intent.setPackage(foundPackageName);
        }
        return intent;
    }

    /**
     * hack to get around the fact that PreferenceActivity still supports only android.app.DialogFragment
     */
    public static android.app.DialogFragment getPreferenceInstallDialogFragment() {
        return PreferenceInstallDialogFragment.newInstance(R.string.orbot_install_dialog_title,
                R.string.orbot_install_dialog_content, ORBOT_PACKAGE_NAME);
    }

    public static DialogFragment getInstallDialogFragmentWithThirdButton(Messenger messenger, int middleButton) {
        return SupportInstallDialogFragment.newInstance(messenger, R.string.orbot_install_dialog_title,
                R.string.orbot_install_dialog_content, ORBOT_PACKAGE_NAME, middleButton, true);
    }

    public static DialogFragment getOrbotStartDialogFragment(Messenger messenger, int middleButton) {
        return OrbotStartDialogFragment.newInstance(messenger, R.string.orbot_start_dialog_title, R.string
                        .orbot_start_dialog_content,
                middleButton);
    }

    /**
     * checks preferences to see if Orbot is required, and if yes, if it is installed and running
     *
     * @param context used to retrieve preferences
     * @return false if Tor is selected proxy and Orbot is not installed or running, true
     * otherwise
     */
    public static boolean isOrbotInRequiredState(Context context) {
        Preferences.ProxyPrefs proxyPrefs = Preferences.getPreferences(context).getProxyPrefs();
        if (!proxyPrefs.torEnabled) {
            return true;
        } else if (!OrbotHelper.isOrbotInstalled(context) || !OrbotHelper.isOrbotRunning(context)) {
            return false;
        }
        return true;
    }

    /**
     * checks if Tor is enabled and if it is, that Orbot is installed and running. Generates appropriate dialogs.
     *
     * @param middleButton resourceId of string to display as the middle button of install and enable dialogs
     * @param proxyPrefs   proxy preferences used to determine if Tor is required to be started
     * @return true if Tor is not enabled or Tor is enabled and Orbot is installed and running, else false
     */
    public static boolean putOrbotInRequiredState(final int middleButton,
                                                  final DialogActions dialogActions,
                                                  Preferences.ProxyPrefs proxyPrefs,
                                                  final FragmentActivity fragmentActivity) {

        if (!proxyPrefs.torEnabled) {
            return true;
        }

        if (!OrbotHelper.isOrbotInstalled(fragmentActivity)) {
            Handler ignoreTorHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case SupportInstallDialogFragment.MESSAGE_MIDDLE_CLICKED:
                            dialogActions.onNeutralButton();
                            break;
                        case SupportInstallDialogFragment.MESSAGE_DIALOG_DISMISSED:
                            // both install and cancel buttons mean we don't go ahead with an
                            // operation, so it's okay to cancel
                            dialogActions.onCancel();
                            break;
                    }
                }
            };

            OrbotHelper.getInstallDialogFragmentWithThirdButton(
                    new Messenger(ignoreTorHandler),
                    middleButton
            ).show(fragmentActivity.getSupportFragmentManager(), "OrbotHelperOrbotInstallDialog");

            return false;
        } else if (!OrbotHelper.isOrbotRunning(fragmentActivity)) {

            final Handler dialogHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case OrbotStartDialogFragment.MESSAGE_MIDDLE_BUTTON:
                            dialogActions.onNeutralButton();
                            break;
                        case OrbotStartDialogFragment.MESSAGE_DIALOG_CANCELLED:
                            dialogActions.onCancel();
                            break;
                        case OrbotStartDialogFragment.MESSAGE_ORBOT_STARTED:
                            dialogActions.onOrbotStarted();
                            break;
                    }
                }
            };

            new SilentStartManager() {

                @Override
                protected void onOrbotStarted() {
                    dialogActions.onOrbotStarted();
                }

                @Override
                protected void onSilentStartDisabled() {
                    getOrbotStartDialogFragment(new Messenger(dialogHandler), middleButton)
                            .show(fragmentActivity.getSupportFragmentManager(),
                                    "OrbotHelperOrbotStartDialog");
                }
            }.startOrbotAndListen(fragmentActivity, true);

            return false;
        } else {
            return true;
        }
    }

    public static boolean putOrbotInRequiredState(DialogActions dialogActions,
                                                  FragmentActivity fragmentActivity) {
        return putOrbotInRequiredState(R.string.orbot_ignore_tor,
                dialogActions,
                Preferences.getPreferences(fragmentActivity).getProxyPrefs(),
                fragmentActivity);
    }

    /**
     * will attempt a silent start, which if disabled will fallback to the
     * {@link #requestShowOrbotStart(Activity) requestShowOrbotStart} method, which returns the
     * result in {@link Activity#onActivityResult(int requestCode, int resultCode, Intent data)}
     * with a {@code requestCode} of {@code START_TOR_RESULT}, which will have to be implemented by
     * activities wishing to respond to a change in Orbot state.
     */
    public static void bestPossibleOrbotStart(final DialogActions dialogActions,
                                              final Activity activity,
                                              boolean showProgress) {
        new SilentStartManager() {

            @Override
            protected void onOrbotStarted() {
                dialogActions.onOrbotStarted();
            }

            @Override
            protected void onSilentStartDisabled() {
                requestShowOrbotStart(activity);
            }
        }.startOrbotAndListen(activity, showProgress);
    }

    /**
     * base class for listening to silent orbot starts. Also handles display of progress dialog.
     */
    public static abstract class SilentStartManager {

        private ProgressDialog mProgressDialog;

        public void startOrbotAndListen(final Context context, final boolean showProgress) {
            Log.d(Constants.TAG, "starting orbot listener");
            if (showProgress) {
                showProgressDialog(context);
            }

            final BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getStringExtra(OrbotHelper.EXTRA_STATUS)) {
                        case OrbotHelper.STATUS_ON:
                            context.unregisterReceiver(this);
                            // generally Orbot starts working a little after this status is received
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (showProgress) {
                                        mProgressDialog.dismiss();
                                    }
                                    onOrbotStarted();
                                }
                            }, 1000);
                            break;
                        case OrbotHelper.STATUS_STARTS_DISABLED:
                            context.unregisterReceiver(this);
                            if (showProgress) {
                                mProgressDialog.dismiss();
                            }
                            onSilentStartDisabled();
                            break;

                    }
                    Log.d(Constants.TAG, "Orbot silent start broadcast: " +
                            intent.getStringExtra(OrbotHelper.EXTRA_STATUS));
                }
            };
            context.registerReceiver(receiver, new IntentFilter(OrbotHelper.ACTION_STATUS));

            requestStartTor(context);
        }

        private void showProgressDialog(Context context) {
            mProgressDialog = new ProgressDialog(ThemeChanger.getDialogThemeWrapper(context));
            mProgressDialog.setMessage(context.getString(R.string.progress_starting_orbot));
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        protected abstract void onOrbotStarted();

        protected abstract void onSilentStartDisabled();
    }
}
