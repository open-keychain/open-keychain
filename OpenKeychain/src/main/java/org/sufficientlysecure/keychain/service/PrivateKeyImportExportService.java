package org.sufficientlysecure.keychain.service;


import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import com.cryptolib.SecureDataSocket;
import com.cryptolib.SecureDataSocketException;

import org.sufficientlysecure.keychain.util.FileHelper;

import java.io.IOException;
import java.util.concurrent.Semaphore;

public class PrivateKeyImportExportService extends IntentService {
    public static final String EXTRA_EXPORT_KEY = "export_key";

    public static final String EXPORT_ACTION_UPDATE_CONNECTION_DETAILS = "export_update_connection_details";
    public static final String EXPORT_ACTION_SHOW_PHRASE = "export_show_phrase";
    public static final String EXPORT_ACTION_KEY = "export_key";
    public static final String EXPORT_ACTION_FINISHED = "export_finished";
    public static final String EXPORT_EXTRA = "export_extra";

    public static final String EXPORT_ACTION_MANUAL_MODE = "export_manual_mode";
    public static final String EXPORT_ACTION_PHRASES_MATCHED = "export_phrases_matched";
    public static final String EXPORT_ACTION_CACHED_URI = "export_cached_uri";

    private static final int PORT = 5891;

    private SecureDataSocket mSecureDataSocket;
    private Semaphore mLock;
    private String mConnectionDetails = null;
    private boolean mPhrasesMatched;

    private LocalBroadcastManager mBroadcaster;
    private Uri mCachedUri;

    public PrivateKeyImportExportService() {
        super("PrivateKeyImportExportService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBroadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean export = intent.getBooleanExtra(EXTRA_EXPORT_KEY, false);

        IntentFilter filter = new IntentFilter();
        filter.addAction(PrivateKeyImportExportService.EXPORT_ACTION_MANUAL_MODE);
        filter.addAction(PrivateKeyImportExportService.EXPORT_ACTION_PHRASES_MATCHED);
        filter.addAction(PrivateKeyImportExportService.EXPORT_ACTION_CACHED_URI);

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);

        if (export) {
            exportKey();
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    private void exportKey() {
        mLock = new Semaphore(0);

        try {
            mSecureDataSocket = new SecureDataSocket(PORT);
            mConnectionDetails = mSecureDataSocket.prepareServerWithClientCamera();
        } catch (SecureDataSocketException e) {
            e.printStackTrace();
        }

        broadcastExport(EXPORT_ACTION_UPDATE_CONNECTION_DETAILS, mConnectionDetails);

        boolean exportedViaQrCode = false;
        try {
            mSecureDataSocket.setupServerWithClientCamera();

            exportedViaQrCode = true;
            broadcastExport(EXPORT_ACTION_KEY, null);
        } catch (SecureDataSocketException e) {
            // this exception is thrown when socket is closed (user switches from QR code to manual ip input)
            exportWithoutQrCode();
        }

        if (exportedViaQrCode || mPhrasesMatched) {
            lock();

            try {
                byte[] exportData = FileHelper.readBytesFromUri(this, mCachedUri);
                mSecureDataSocket.write(exportData);
            } catch (SecureDataSocketException | IOException e) {
                e.printStackTrace();
            }
        }

        broadcastExport(EXPORT_ACTION_FINISHED, null);
    }

    private void exportWithoutQrCode() {
        String phrase = null;
        try {
            phrase = mSecureDataSocket.setupServerNoClientCamera();
        } catch (SecureDataSocketException e) {
            e.printStackTrace();
        }

        broadcastExport(EXPORT_ACTION_SHOW_PHRASE, phrase);

        lock();

        try {
            mSecureDataSocket.comparedPhrases(mPhrasesMatched);
        } catch (SecureDataSocketException e) {
            e.printStackTrace();
        }

        if (mPhrasesMatched) {
            broadcastExport(EXPORT_ACTION_KEY, null);
        }
    }

    private void lock() {
        boolean interrupted;
        do {
            try {
                mLock.acquire();
                interrupted = false;
            } catch (InterruptedException e) {
                interrupted = true;
                e.printStackTrace();
            }
        } while (interrupted);
    }

    private void broadcastExport(String action, String extra) {
        Intent intent = new Intent(action);
        intent.putExtra(EXPORT_EXTRA, extra);

        mBroadcaster.sendBroadcast(intent);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case EXPORT_ACTION_MANUAL_MODE:
                    if (mSecureDataSocket != null) {
                        mSecureDataSocket.close();
                    }
                    break;
                case EXPORT_ACTION_PHRASES_MATCHED:
                    mLock.release();
                    mPhrasesMatched = intent.getBooleanExtra(EXPORT_EXTRA, false);
                    break;
                case EXPORT_ACTION_CACHED_URI:
                    mLock.release();
                    mCachedUri = intent.getParcelableExtra(EXPORT_EXTRA);
                    break;
            }
        }
    };
}
