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

package org.sufficientlysecure.keychain.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.view.ContextThemeWrapper;

import org.sufficientlysecure.keychain.Constants.NotificationIds;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.network.orbot.OrbotHelper;
import org.sufficientlysecure.keychain.util.ResourceUtils;
import timber.log.Timber;


/**
 * Simply encapsulates a dialog. If orbot is not installed, it shows an install dialog, else a
 * dialog to enable orbot.
 */
public class OrbotRequiredDialogActivity extends FragmentActivity
        implements OrbotHelper.DialogActions {

    public static final int MESSAGE_ORBOT_STARTED = 1;
    public static final int MESSAGE_ORBOT_IGNORE = 2;
    public static final int MESSAGE_DIALOG_CANCEL = 3;

    // if suppplied and true will start Orbot directly without showing dialog
    public static final String EXTRA_START_ORBOT = "start_orbot";
    // used for communicating results when triggered from a service
    public static final String EXTRA_MESSENGER = "messenger";

    // to provide any previous crypto input into which proxy preference is merged
    public static final String EXTRA_CRYPTO_INPUT = "extra_crypto_input";

    public static final String RESULT_CRYPTO_INPUT = "result_crypto_input";

    private CryptoInputParcel mCryptoInputParcel;
    private Messenger mMessenger;

    private ProgressDialog mShowOrbotProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCryptoInputParcel = getIntent().getParcelableExtra(EXTRA_CRYPTO_INPUT);
        if (mCryptoInputParcel == null) {
            // compatibility with usages that don't use a CryptoInputParcel
            mCryptoInputParcel = CryptoInputParcel.createCryptoInputParcel();
        }

        mMessenger = getIntent().getParcelableExtra(EXTRA_MESSENGER);

        boolean startOrbotDirect = getIntent().getBooleanExtra(EXTRA_START_ORBOT, false);
        if (startOrbotDirect) {
            ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(this);
            mShowOrbotProgressDialog = new ProgressDialog(theme);
            mShowOrbotProgressDialog.setTitle(R.string.progress_starting_orbot);
            mShowOrbotProgressDialog.setCancelable(false);
            mShowOrbotProgressDialog.show();
            OrbotHelper.bestPossibleOrbotStart(this, this, false);
        } else {
            showDialog();
        }
    }

    /**
     * Displays an install or start orbot dialog (or silent orbot start) depending on orbot's
     * presence and state
     */
    public void showDialog() {
        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {

                if (OrbotHelper.putOrbotInRequiredState(OrbotRequiredDialogActivity.this,
                        OrbotRequiredDialogActivity.this)) {
                    // no action required after all
                    onOrbotStarted();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case OrbotHelper.START_TOR_RESULT: {
                dismissOrbotProgressDialog();
                // unfortunately, this result is returned immediately and not when Orbot is started
                // 10s is approximately the longest time Orbot has taken to start
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onOrbotStarted(); // assumption that orbot was started
                    }
                }, 10000);
            }
        }
    }

    /**
     * for when Orbot is started without showing the dialog by the EXTRA_START_ORBOT intent extra
     */
    private void dismissOrbotProgressDialog() {
        if (mShowOrbotProgressDialog != null) {
            mShowOrbotProgressDialog.dismiss();
        }
    }

    @Override
    public void onOrbotStarted() {
        dismissOrbotProgressDialog();
        sendMessage(MESSAGE_ORBOT_STARTED);
        Intent intent = new Intent();
        // send back unmodified CryptoInputParcel for a retry
        intent.putExtra(RESULT_CRYPTO_INPUT, mCryptoInputParcel);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onNeutralButton() {
        sendMessage(MESSAGE_ORBOT_IGNORE);
        Intent intent = new Intent();
        mCryptoInputParcel = mCryptoInputParcel.withParcelableProxy(ParcelableProxy.getForNoProxy());
        intent.putExtra(RESULT_CRYPTO_INPUT, mCryptoInputParcel);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onCancel() {
        sendMessage(MESSAGE_DIALOG_CANCEL);
        finish();
    }

    private void sendMessage(int what) {
        if (mMessenger != null) {
            Message msg = Message.obtain();
            msg.what = what;
            try {
                mMessenger.send(msg);
            } catch (RemoteException e) {
                Timber.e(e, "Could not deliver message");
            }
        }
    }

    public static void showOrbotRequiredNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NotificationIds.KEYSERVER_SYNC_FAIL_ORBOT, createOrbotNotification(context));
        }
    }

    private static Notification createOrbotNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_notify_24dp)
                .setLargeIcon(ResourceUtils.getDrawableAsNotificationBitmap(context, R.mipmap.ic_launcher))
                .setContentTitle(context.getString(R.string.keyserver_sync_orbot_notif_title))
                .setContentText(context.getString(R.string.keyserver_sync_orbot_notif_msg))
                .setAutoCancel(true);

        Intent startOrbotIntent = new Intent(context, OrbotRequiredDialogActivity.class);
        startOrbotIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startOrbotIntent.putExtra(OrbotRequiredDialogActivity.EXTRA_START_ORBOT, true);
        PendingIntent startOrbotPi = PendingIntent.getActivity(
                context, 0, startOrbotIntent, PendingIntent.FLAG_CANCEL_CURRENT
        );

        builder.addAction(R.drawable.ic_stat_tor,
                context.getString(R.string.keyserver_sync_orbot_notif_start),
                startOrbotPi
        );
        builder.setContentIntent(startOrbotPi);

        return builder.build();
    }

}