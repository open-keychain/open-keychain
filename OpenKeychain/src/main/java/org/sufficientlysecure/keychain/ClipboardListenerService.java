package org.sufficientlysecure.keychain;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.ui.DecryptActivity;
import org.sufficientlysecure.keychain.ui.ImportKeysActivity;


public class ClipboardListenerService extends Service implements ClipboardManager.OnPrimaryClipChangedListener {
    private static final String ACTION_CANCEL = "action_cancel";

    private static int lastNotificationId = 0;

    private ClipboardManager clipboardManager;
    public static final long[] VIBRATE_PATTERN = new long[] { 500, 100 };

    @Override
    public void onCreate() {
        super.onCreate();

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            stopSelf();
            return;
        }

        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }

        Log.d(Constants.TAG, "clipboard monitor registered");
        clipboardManager.addPrimaryClipChangedListener(this);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopSelf();
            }
        }, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (clipboardManager != null) {
            Log.d(Constants.TAG, "clipboard monitor stopped");
            clipboardManager.removePrimaryClipChangedListener(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onPrimaryClipChanged() {
        Log.d(Constants.TAG, "onPrimaryClipChanged");

        ClipData clip = clipboardManager.getPrimaryClip();

        boolean isFromOpenKeychain = Constants.CLIPBOARD_LABEL.equals(clip.getDescription().getLabel());
        if (isFromOpenKeychain && !Constants.DEBUG) {
            Log.d(Constants.TAG, "Ignoring clipboard change from OpenKeychain (in non debug build)");
            return;
        }

        new ClipboardProcessAsyncTask(getBaseContext()).execute(clip);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_CANCEL.equals(action)) {
            Uri data = intent.getData();
            if (data != null) {
                Log.d(Constants.TAG, "Action cancelled, deleting clipboard data");
                getContentResolver().delete(data, null, null);
            }
        }

        return START_STICKY;
    }

    private static class ClipboardProcessAsyncTask extends AsyncTask<ClipData,Void,Uri> {
        @SuppressLint("StaticFieldLeak")
        private final Context context;
        private boolean isPgpKey;
        private boolean isPgpMessage;

        ClipboardProcessAsyncTask(Context context) {
            this.context = context;
        }

        @Override
        protected Uri doInBackground(ClipData... clip) {
            ClipData clipData = clip[0];

            CharSequence clipboardText = clipData.getItemAt(0).coerceToText(context);
            isPgpMessage = PgpHelper.PGP_MESSAGE.matcher(clipboardText).matches();
            isPgpKey = PgpHelper.PGP_PUBLIC_KEY.matcher(clipboardText).matches();
            if (isPgpKey) {
                clipboardText = PgpHelper.getPgpPublicKeyContent(clipboardText);
            } else if (isPgpMessage) {
                clipboardText = PgpHelper.getPgpMessageContent(clipboardText);
            } else {
                return null;
            }

            if (clipboardText == null) {
                Log.e(Constants.TAG, "clipboard content looked ok, but could not be parsed");
                return null;
            }

            try {
                Uri file = TemporaryFileProvider.createFile(context);

                OutputStream outputStream = context.getContentResolver().openOutputStream(file);
                if (outputStream == null) {
                    return null;
                }

                outputStream = new BufferedOutputStream(outputStream);
                outputStream.write(clipboardText.toString().getBytes());
                outputStream.close();

                return file;
            } catch (IOException e) {
                Log.e(Constants.TAG, "exception parsing message", e);
                return null;
            }
        }

        @TargetApi(VERSION_CODES.LOLLIPOP)
        @Override
        protected void onPostExecute(final Uri result) {
            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (result == null) {
                return;
            }

            Intent intent;
            if (isPgpKey) {
                intent = new Intent(context, ImportKeysActivity.class);
                intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY);
            } else if (isPgpMessage) {
                intent = new Intent(context, DecryptActivity.class);
                intent.setAction(Constants.DECRYPT_DATA);
            } else {
                throw new IllegalStateException("must be either pgp key or message");
            }
            intent.setData(result);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

            Intent cancelIntent = new Intent(context, ClipboardListenerService.class);
            cancelIntent.setAction(ACTION_CANCEL);
            cancelIntent.setData(result);
            PendingIntent cancelPendingIntent =
                    PendingIntent.getService(context, 0, cancelIntent, PendingIntent.FLAG_ONE_SHOT);

            String title = isPgpKey ?
                    context.getString(R.string.notify_clipboard_key_title) :
                    context.getString(R.string.notify_clipboard_msg_title);
            String text = isPgpKey ?
                    context.getString(R.string.notify_clipboard_key_text) :
                    context.getString(R.string.notify_clipboard_msg_text);

            Notification notification = new Builder(context)
                    .setVibrate(VIBRATE_PATTERN)
                    .setSmallIcon(R.drawable.ic_stat_notify_24dp)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setCategory(Notification.CATEGORY_MESSAGE)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setContentIntent(pendingIntent)
                    .setDeleteIntent(cancelPendingIntent)
                    .setAutoCancel(true)
                    .setLocalOnly(true)
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(false)
                    .build();

            final int notificationId = ++lastNotificationId;
            notificationManager.notify(notificationId, notification);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    notificationManager.cancel(notificationId);
                }
            }, 7_000);
       }
    }

}