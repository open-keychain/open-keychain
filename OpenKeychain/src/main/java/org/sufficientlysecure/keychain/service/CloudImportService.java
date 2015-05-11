/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.ImportExportOperation;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableFileCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * When this service is started it will initiate a multi-threaded key import and when done it will
 * shut itself down.
 */
public class CloudImportService extends Service implements Progressable {

    // required as extras from intent
    public static final String EXTRA_MESSENGER = "messenger";
    public static final String EXTRA_DATA = "data";

    // required by data bundle
    public static final String IMPORT_KEY_LIST = "import_key_list";
    public static final String IMPORT_KEY_SERVER = "import_key_server";

    // indicates a request to cancel the import
    public static final String ACTION_CANCEL = Constants.INTENT_PREFIX + "CANCEL";

    // tells the spawned threads whether the user has requested a cancel
    private static AtomicBoolean mActionCancelled = new AtomicBoolean(false);

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Used to accumulate the results of individual key imports
     */
    private class KeyImportAccumulator {
        private OperationResult.OperationLog mImportLog = new OperationResult.OperationLog();
        private int mTotalKeys;
        private int mImportedKeys = 0;
        private Progressable mImportProgressable;
        ArrayList<Long> mImportedMasterKeyIds = new ArrayList<Long>();
        private int mBadKeys = 0;
        private int mNewKeys = 0;
        private int mUpdatedKeys = 0;
        private int mSecret = 0;
        private int mResultType = 0;

        public KeyImportAccumulator(int totalKeys) {
            mTotalKeys = totalKeys;
            // ignore updates from ImportExportOperation for now
            mImportProgressable = new Progressable() {
                @Override
                public void setProgress(String message, int current, int total) {

                }

                @Override
                public void setProgress(int resourceId, int current, int total) {

                }

                @Override
                public void setProgress(int current, int total) {

                }

                @Override
                public void setPreventCancel() {

                }
            };
        }

        public Progressable getImportProgressable() {
            return mImportProgressable;
        }

        public int getTotalKeys() {
            return mTotalKeys;
        }

        public int getImportedKeys() {
            return mImportedKeys;
        }

        public synchronized void accumulateKeyImport(ImportKeyResult result) {
            mImportedKeys++;
            mImportLog.addAll(result.getLog().toList());//accumulates log
            mBadKeys += result.mBadKeys;
            mNewKeys += result.mNewKeys;
            mUpdatedKeys += result.mUpdatedKeys;
            mSecret += result.mSecret;

            long[] masterKeyIds = result.getImportedMasterKeyIds();
            for (long masterKeyId : masterKeyIds) {
                mImportedMasterKeyIds.add(masterKeyId);
            }

            // if any key import has been cancelled, set result type to cancelled
            // resultType is added to in getConsolidatedKayImport to account for remaining factors
            mResultType |= result.getResult() & ImportKeyResult.RESULT_CANCELLED;
        }

        /**
         * returns accumulated result of all imports so far
         */
        public ImportKeyResult getConsolidatedImportKeyResult() {

            // adding required information to mResultType
            // special case,no keys requested for import
            if (mBadKeys == 0 && mNewKeys == 0 && mUpdatedKeys == 0) {
                mResultType = ImportKeyResult.RESULT_FAIL_NOTHING;
            } else {
                if (mNewKeys > 0) {
                    mResultType |= ImportKeyResult.RESULT_OK_NEWKEYS;
                }
                if (mUpdatedKeys > 0) {
                    mResultType |= ImportKeyResult.RESULT_OK_UPDATED;
                }
                if (mBadKeys > 0) {
                    mResultType |= ImportKeyResult.RESULT_WITH_ERRORS;
                    if (mNewKeys == 0 && mUpdatedKeys == 0) {
                        mResultType |= ImportKeyResult.RESULT_ERROR;
                    }
                }
                if (mImportLog.containsWarnings()) {
                    mResultType |= ImportKeyResult.RESULT_WARNINGS;
                }
            }

            long masterKeyIds[] = new long[mImportedMasterKeyIds.size()];
            for (int i = 0; i < masterKeyIds.length; i++) {
                masterKeyIds[i] = mImportedMasterKeyIds.get(i);
            }

            return new ImportKeyResult(mResultType, mImportLog, mNewKeys, mUpdatedKeys, mBadKeys,
                    mSecret, masterKeyIds);
        }

        public boolean isImportFinished() {
            return mTotalKeys == mImportedKeys;
        }
    }

    private KeyImportAccumulator mKeyImportAccumulator;

    Messenger mMessenger;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (ACTION_CANCEL.equals(intent.getAction())) {
            mActionCancelled.set(true);
            return Service.START_NOT_STICKY;
        }

        mActionCancelled.set(false);//we haven't been cancelled, yet

        Bundle extras = intent.getExtras();

        mMessenger = (Messenger) extras.get(EXTRA_MESSENGER);

        Bundle data = extras.getBundle(EXTRA_DATA);

        final String keyServer = data.getString(IMPORT_KEY_SERVER);
        // keyList being null (in case key list to be reaad from cache) is checked by importKeys
        final ArrayList<ParcelableKeyRing> keyList = data.getParcelableArrayList(IMPORT_KEY_LIST);

        // Adding keys to the ThreadPoolExecutor takes time, we don't want to block the main thread
        Thread baseImportThread = new Thread(new Runnable() {

            @Override
            public void run() {
                importKeys(keyList, keyServer);
            }
        });
        baseImportThread.start();
        return Service.START_NOT_STICKY;
    }

    public void importKeys(ArrayList<ParcelableKeyRing> keyList, final String keyServer) {
        ParcelableFileCache<ParcelableKeyRing> cache =
                new ParcelableFileCache<>(this, "key_import.pcl");
        int totKeys = 0;
        Iterator<ParcelableKeyRing> keyListIterator = null;
        // either keyList or cache must be null, no guarantees otherwise
        if (keyList == null) {//export from cache, copied from ImportExportOperation.importKeyRings

            try {
                ParcelableFileCache.IteratorWithSize<ParcelableKeyRing> it = cache.readCache();
                keyListIterator = it;
                totKeys = it.getSize();
            } catch (IOException e) {

                // Special treatment here, we need a lot
                OperationResult.OperationLog log = new OperationResult.OperationLog();
                log.add(OperationResult.LogType.MSG_IMPORT, 0, 0);
                log.add(OperationResult.LogType.MSG_IMPORT_ERROR_IO, 0, 0);

                keyImportFailed(new ImportKeyResult(ImportKeyResult.RESULT_ERROR, log));
            }
        } else {
            keyListIterator = keyList.iterator();
            totKeys = keyList.size();
        }


        if (keyListIterator != null) {
            mKeyImportAccumulator = new KeyImportAccumulator(totKeys);
            setProgress(0, totKeys);

            final int maxThreads = 200;
            ExecutorService importExecutor = new ThreadPoolExecutor(0, maxThreads,
                    30L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>());

            while (keyListIterator.hasNext()) {

                final ParcelableKeyRing pkRing = keyListIterator.next();

                Runnable importOperationRunnable = new Runnable() {

                    @Override
                    public void run() {
                        ImportKeyResult result = null;
                        try {
                            ImportExportOperation importExportOperation = new ImportExportOperation(
                                    CloudImportService.this,
                                    new ProviderHelper(CloudImportService.this),
                                    mKeyImportAccumulator.getImportProgressable(),
                                    mActionCancelled);

                            ArrayList<ParcelableKeyRing> list = new ArrayList<>();
                            list.add(pkRing);
                            result = importExportOperation.importKeyRings(list,
                                    keyServer);
                        } finally {
                            // in the off-chance that importKeyRings does something to crash the
                            // thread before it can call singleKeyRingImportCompleted, our imported
                            // key count will go wrong. This will cause the service to never die,
                            // and the progress dialog to stay displayed. The finally block was
                            // originally meant to ensure singleKeyRingImportCompleted was called,
                            // and checks for null were to be introduced, but in such a scenario,
                            // knowing an uncaught error exists in importKeyRings is more important.

                            // if a null gets passed, something wrong is happening. We want a crash.

                            singleKeyRingImportCompleted(result);
                        }
                    }
                };

                importExecutor.execute(importOperationRunnable);
            }
        }
    }

    private synchronized void singleKeyRingImportCompleted(ImportKeyResult result) {
        // increase imported key count and accumulate log and bad, new etc. key counts from result
        mKeyImportAccumulator.accumulateKeyImport(result);

        setProgress(mKeyImportAccumulator.getImportedKeys(), mKeyImportAccumulator.getTotalKeys());

        if (mKeyImportAccumulator.isImportFinished()) {
            ContactSyncAdapterService.requestSync();

            sendMessageToHandler(ServiceProgressHandler.MessageStatus.OKAY,
                    mKeyImportAccumulator.getConsolidatedImportKeyResult());

            stopSelf();//we're done here
        }
    }

    private void keyImportFailed(ImportKeyResult result) {
        sendMessageToHandler(ServiceProgressHandler.MessageStatus.OKAY, result);
    }

    private void sendMessageToHandler(ServiceProgressHandler.MessageStatus status, Integer arg2, Bundle data) {

        Message msg = Message.obtain();
        assert msg != null;
        msg.arg1 = status.ordinal();
        if (arg2 != null) {
            msg.arg2 = arg2;
        }
        if (data != null) {
            msg.setData(data);
        }

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w(Constants.TAG, "Messenger is null!", e);
        }
    }

    private void sendMessageToHandler(ServiceProgressHandler.MessageStatus status, OperationResult data) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(OperationResult.EXTRA_RESULT, data);
        sendMessageToHandler(status, null, bundle);
    }

    private void sendMessageToHandler(ServiceProgressHandler.MessageStatus status, Bundle data) {
        sendMessageToHandler(status, null, data);
    }

    private void sendMessageToHandler(ServiceProgressHandler.MessageStatus status) {
        sendMessageToHandler(status, null, null);
    }

    /**
     * Set progress of ProgressDialog by sending message to handler on UI thread
     */
    @Override
    public synchronized void setProgress(String message, int progress, int max) {
        Log.d(Constants.TAG, "Send message by setProgress with progress=" + progress + ", max="
                + max);

        Bundle data = new Bundle();
        if (message != null) {
            data.putString(ServiceProgressHandler.DATA_MESSAGE, message);
        }
        data.putInt(ServiceProgressHandler.DATA_PROGRESS, progress);
        data.putInt(ServiceProgressHandler.DATA_PROGRESS_MAX, max);

        sendMessageToHandler(ServiceProgressHandler.MessageStatus.UPDATE_PROGRESS, null, data);
    }

    @Override
    public synchronized void setProgress(int resourceId, int progress, int max) {
        setProgress(getString(resourceId), progress, max);
    }

    @Override
    public synchronized void setProgress(int progress, int max) {
        setProgress(null, progress, max);
    }

    @Override
    public synchronized void setPreventCancel() {
        sendMessageToHandler(ServiceProgressHandler.MessageStatus.PREVENT_CANCEL);
    }
}
