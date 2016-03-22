/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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

package org.sufficientlysecure.keychain.operations;


import java.io.IOException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.FacebookKeyserver;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserver;
import org.sufficientlysecure.keychain.keyimport.KeybaseKeyserver;
import org.sufficientlysecure.keychain.keyimport.Keyserver;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ConsolidateResult;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.ContactSyncAdapterService;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableFileCache;
import org.sufficientlysecure.keychain.util.ParcelableFileCache.IteratorWithSize;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.ProgressScaler;
import org.sufficientlysecure.keychain.util.orbot.OrbotHelper;

/**
 * An operation class which implements high level import
 * operations.
 * This class receives a source and/or destination of keys as input and performs
 * all steps for this import.
 * For the import operation, the only valid source is an Iterator of
 * ParcelableKeyRing, each of which must contain either a single
 * keyring encoded as bytes, or a unique reference to a keyring
 * on keyservers and/or keybase.io.
 * It is important to note that public keys should generally be imported before
 * secret keys, because some implementations (notably Symantec PGP Desktop) do
 * not include self certificates for user ids in the secret keyring. The import
 * method here will generally import keyrings in the order given by the
 * iterator, so this should be ensured beforehand.
 *
 * @see org.sufficientlysecure.keychain.ui.adapter.ImportKeysAdapter#getSelectedEntries()
 */
public class ImportOperation extends BaseOperation<ImportKeyringParcel> {

    public static final int MAX_THREADS = 10;

    public ImportOperation(Context context, ProviderHelper providerHelper, Progressable
            progressable) {
        super(context, providerHelper, progressable);
    }

    public ImportOperation(Context context, ProviderHelper providerHelper,
                           Progressable progressable, AtomicBoolean cancelled) {
        super(context, providerHelper, progressable, cancelled);
    }

    // Overloaded functions for using progressable supplied in constructor during import
    public ImportKeyResult serialKeyRingImport(Iterator<ParcelableKeyRing> entries, int num,
                                               String keyServerUri, Proxy proxy) {
        return serialKeyRingImport(entries, num, keyServerUri, mProgressable, proxy);
    }

    @NonNull
    private ImportKeyResult serialKeyRingImport(ParcelableFileCache<ParcelableKeyRing> cache,
                                                String keyServerUri, Proxy proxy) {

        // get entries from cached file
        try {
            IteratorWithSize<ParcelableKeyRing> it = cache.readCache();
            int numEntries = it.getSize();

            return serialKeyRingImport(it, numEntries, keyServerUri, mProgressable, proxy);
        } catch (IOException e) {

            // Special treatment here, we need a lot
            OperationLog log = new OperationLog();
            log.add(LogType.MSG_IMPORT, 0, 0);
            log.add(LogType.MSG_IMPORT_ERROR_IO, 0, 0);

            return new ImportKeyResult(ImportKeyResult.RESULT_ERROR, log);
        }

    }

    /**
     * Since the introduction of multithreaded import, we expect calling functions to handle the
     * contact-to-key sync i.e ContactSyncAdapterService.requestContactsSync()
     *
     * @param entries      keys to import
     * @param num          number of keys to import
     * @param keyServerUri contains uri of keyserver to import from, if it is an import from cloud
     * @param progressable Allows multi-threaded import to supply a progressable that ignores the
     *                     progress of a single key being imported
     */
    @NonNull
    private ImportKeyResult serialKeyRingImport(Iterator<ParcelableKeyRing> entries, int num,
                                                String keyServerUri, Progressable progressable,
                                                @NonNull Proxy proxy) {
        if (progressable != null) {
            progressable.setProgress(R.string.progress_importing, 0, 100);
        }

        OperationLog log = new OperationLog();
        log.add(LogType.MSG_IMPORT, 0, num);

        // If there aren't even any keys, do nothing here.
        if (entries == null || !entries.hasNext()) {
            return new ImportKeyResult(ImportKeyResult.RESULT_FAIL_NOTHING, log);
        }

        int newKeys = 0, updatedKeys = 0, badKeys = 0, secret = 0;
        ArrayList<Long> importedMasterKeyIds = new ArrayList<>();

        boolean cancelled = false;
        int position = 0;
        double progSteps = 100.0 / num;

        KeybaseKeyserver keybaseServer = null;
        FacebookKeyserver facebookServer = null;
        HkpKeyserver keyServer = null;

        // iterate over all entries
        while (entries.hasNext()) {
            ParcelableKeyRing entry = entries.next();

            // Has this action been cancelled? If so, don't proceed any further
            if (checkCancelled()) {
                cancelled = true;
                break;
            }

            try {

                UncachedKeyRing key = null;

                // If there is already byte data, use that
                if (entry.mBytes != null) {
                    key = UncachedKeyRing.decodeFromData(entry.mBytes);
                }
                // Otherwise, we need to fetch the data from a server first
                else {

                    // We fetch from keyservers first, because we tend to get more certificates
                    // from there, so the number of certificates which are merged in later is
                    // smaller.

                    // If we have a keyServerUri and a fingerprint or at least a keyId,
                    // download from HKP
                    if (keyServerUri != null
                            && (entry.mKeyIdHex != null || entry.mExpectedFingerprint != null)) {
                        // Make sure we have the keyserver instance cached
                        if (keyServer == null) {
                            log.add(LogType.MSG_IMPORT_KEYSERVER, 1, keyServerUri);
                            keyServer = new HkpKeyserver(keyServerUri, proxy);
                        }

                        try {
                            byte[] data;
                            // Download by fingerprint, or keyId - whichever is available
                            if (entry.mExpectedFingerprint != null) {
                                log.add(LogType.MSG_IMPORT_FETCH_KEYSERVER, 2, "0x" +
                                        entry.mExpectedFingerprint.substring(24));
                                data = keyServer.get("0x" + entry.mExpectedFingerprint).getBytes();
                            } else {
                                log.add(LogType.MSG_IMPORT_FETCH_KEYSERVER, 2, entry.mKeyIdHex);
                                data = keyServer.get(entry.mKeyIdHex).getBytes();
                            }
                            key = UncachedKeyRing.decodeFromData(data, entry.mExpectedFingerprint, entry.mKeyIdHex);
                            if (key != null) {
                                log.add(LogType.MSG_IMPORT_FETCH_KEYSERVER_OK, 3);
                            } else {
                                log.add(LogType.MSG_IMPORT_FETCH_ERROR_DECODE, 3);
                            }
                        } catch (Keyserver.QueryFailedException e) {
                            Log.d(Constants.TAG, "query failed", e);
                            log.add(LogType.MSG_IMPORT_FETCH_ERROR_KEYSERVER, 3, e.getMessage());
                        }
                    }

                    // If we have a keybase name, try to fetch from there
                    if (entry.mKeybaseName != null) {
                        // Make sure we have this cached
                        if (keybaseServer == null) {
                            keybaseServer = new KeybaseKeyserver(proxy);
                        }

                        try {
                            log.add(LogType.MSG_IMPORT_FETCH_KEYBASE, 2, entry.mKeybaseName);
                            byte[] data = keybaseServer.get(entry.mKeybaseName).getBytes();
                            UncachedKeyRing keybaseKey = UncachedKeyRing.decodeFromData(data);

                            if (keybaseKey != null) {
                                log.add(LogType.MSG_IMPORT_FETCH_KEYSERVER_OK, 3);
                            } else {
                                log.add(LogType.MSG_IMPORT_FETCH_ERROR_DECODE, 3);
                            }

                            // If there already is a key, merge the two
                            if (key != null && keybaseKey != null) {
                                log.add(LogType.MSG_IMPORT_MERGE, 3);
                                keybaseKey = key.merge(keybaseKey, log, 4);
                                // If the merge didn't fail, use the new merged key
                                if (keybaseKey != null) {
                                    key = keybaseKey;
                                } else {
                                    log.add(LogType.MSG_IMPORT_MERGE_ERROR, 4);
                                }
                            } else if (keybaseKey != null) {
                                key = keybaseKey;
                            }
                        } catch (Keyserver.QueryFailedException e) {
                            // download failed, too bad. just proceed
                            Log.e(Constants.TAG, "query failed", e);
                            log.add(LogType.MSG_IMPORT_FETCH_ERROR_KEYSERVER, 3, e.getMessage());
                        }
                    }

                    // if the key is from Facebook, fetch from there
                    if (entry.mFbUsername != null) {
                        // Make sure we have this cached
                        if (facebookServer == null) {
                            facebookServer = new FacebookKeyserver(proxy);
                        }

                        try {
                            log.add(LogType.MSG_IMPORT_FETCH_FACEBOOK, 2, entry.mFbUsername);
                            byte[] data = facebookServer.get(entry.mFbUsername).getBytes();
                            UncachedKeyRing facebookKey = UncachedKeyRing.decodeFromData(data);

                            if (facebookKey != null) {
                                log.add(LogType.MSG_IMPORT_FETCH_KEYSERVER_OK, 3);
                            } else {
                                log.add(LogType.MSG_IMPORT_FETCH_ERROR_DECODE, 3);
                            }

                            // If there already is a key, merge the two
                            if (key != null && facebookKey != null) {
                                log.add(LogType.MSG_IMPORT_MERGE, 3);
                                facebookKey = key.merge(facebookKey, log, 4);
                                // If the merge didn't fail, use the new merged key
                                if (facebookKey != null) {
                                    key = facebookKey;
                                } else {
                                    log.add(LogType.MSG_IMPORT_MERGE_ERROR, 4);
                                }
                            } else if (facebookKey != null) {
                                key = facebookKey;
                            }
                        } catch (Keyserver.QueryFailedException e) {
                            // download failed, too bad. just proceed
                            Log.e(Constants.TAG, "query failed", e);
                            log.add(LogType.MSG_IMPORT_FETCH_ERROR_KEYSERVER, 3, e.getMessage());
                        }
                    }
                }

                if (key == null) {
                    log.add(LogType.MSG_IMPORT_FETCH_ERROR, 2);
                    badKeys += 1;
                    continue;
                }

                // never import secret keys from keyserver!
                if (entry.mBytes == null && key.isSecret()) {
                    log.add(LogType.MSG_IMPORT_FETCH_ERROR_KEYSERVER_SECRET, 2);
                    badKeys += 1;
                    continue;
                }

                SaveKeyringResult result;
                // synchronizing prevents https://github.com/open-keychain/open-keychain/issues/1221
                // and https://github.com/open-keychain/open-keychain/issues/1480
                synchronized (mProviderHelper) {
                    mProviderHelper.clearLog();
                    if (key.isSecret()) {
                        result = mProviderHelper.saveSecretKeyRing(key,
                                new ProgressScaler(progressable, (int) (position * progSteps),
                                        (int) ((position + 1) * progSteps), 100));
                    } else {
                        result = mProviderHelper.savePublicKeyRing(key,
                                new ProgressScaler(progressable, (int) (position * progSteps),
                                        (int) ((position + 1) * progSteps), 100), entry.mExpectedFingerprint);
                    }
                }
                if (!result.success()) {
                    badKeys += 1;
                } else {
                    if (result.updated()) {
                        updatedKeys += 1;
                        importedMasterKeyIds.add(key.getMasterKeyId());
                    } else {
                        newKeys += 1;
                        if (key.isSecret()) {
                            secret += 1;
                        }
                        importedMasterKeyIds.add(key.getMasterKeyId());
                    }
                    if (entry.mBytes == null) {
                        // synonymous to isDownloadFromKeyserver.
                        // If no byte data was supplied, import from keyserver took place
                        // this prevents file imports being noted as keyserver imports
                        mProviderHelper.renewKeyLastUpdatedTime(key.getMasterKeyId(),
                                GregorianCalendar.getInstance().getTimeInMillis(),
                                TimeUnit.MILLISECONDS);
                    }
                }

                log.add(result, 2);
            } catch (IOException | PgpGeneralException e) {
                Log.e(Constants.TAG, "Encountered bad key on import!", e);
                ++badKeys;
            }
            // update progress
            position++;
        }

        // Special: consolidate on secret key import (cannot be cancelled!)
        // synchronized on mProviderHelper to prevent
        // https://github.com/open-keychain/open-keychain/issues/1221 since a consolidate deletes
        // and re-inserts keys, which could conflict with a parallel db key update
        if (secret > 0) {
            setPreventCancel();
            ConsolidateResult result;
            synchronized (mProviderHelper) {
                result = mProviderHelper.consolidateDatabaseStep1(progressable);
            }
            log.add(result, 1);
        }

        // Special: make sure new data is synced into contacts
        // disabling sync right now since it reduces speed while multi-threading
        // so, we expect calling functions to take care of it. KeychainService handles this
        // ContactSyncAdapterService.requestContactsSync();

        // convert to long array
        long[] importedMasterKeyIdsArray = new long[importedMasterKeyIds.size()];
        for (int i = 0; i < importedMasterKeyIds.size(); ++i) {
            importedMasterKeyIdsArray[i] = importedMasterKeyIds.get(i);
        }

        int resultType = 0;
        if (cancelled) {
            log.add(LogType.MSG_OPERATION_CANCELLED, 1);
            resultType |= ImportKeyResult.RESULT_CANCELLED;
        }

        // special return case: no new keys at all
        if (badKeys == 0 && newKeys == 0 && updatedKeys == 0) {
            resultType = ImportKeyResult.RESULT_FAIL_NOTHING;
        } else {
            if (newKeys > 0) {
                resultType |= ImportKeyResult.RESULT_OK_NEWKEYS;
            }
            if (updatedKeys > 0) {
                resultType |= ImportKeyResult.RESULT_OK_UPDATED;
            }
            if (badKeys > 0) {
                resultType |= ImportKeyResult.RESULT_WITH_ERRORS;
                if (newKeys == 0 && updatedKeys == 0) {
                    resultType |= ImportKeyResult.RESULT_ERROR;
                }
            }
            if (log.containsWarnings()) {
                resultType |= ImportKeyResult.RESULT_WARNINGS;
            }
        }

        if (!cancelled) {
            // Final log entry, it's easier to do this individually
            if ((newKeys > 0 || updatedKeys > 0) && badKeys > 0) {
                log.add(LogType.MSG_IMPORT_PARTIAL, 1);
            } else if (newKeys > 0 || updatedKeys > 0) {
                log.add(LogType.MSG_IMPORT_SUCCESS, 1);
            } else {
                log.add(LogType.MSG_IMPORT_ERROR, 1);
            }
        }

        return new ImportKeyResult(resultType, log, newKeys, updatedKeys, badKeys, secret,
                importedMasterKeyIdsArray);
    }

    @NonNull
    @Override
    public ImportKeyResult execute(ImportKeyringParcel importInput, CryptoInputParcel cryptoInput) {
        ArrayList<ParcelableKeyRing> keyList = importInput.mKeyList;
        String keyServer = importInput.mKeyserver;

        ImportKeyResult result;

        if (keyList == null) {// import from file, do serially
            ParcelableFileCache<ParcelableKeyRing> cache = new ParcelableFileCache<>(mContext,
                    "key_import.pcl");

            result = serialKeyRingImport(cache, null, null);
        } else {
            Proxy proxy;
            if (cryptoInput.getParcelableProxy() == null) {
                // explicit proxy not set
                if(!OrbotHelper.isOrbotInRequiredState(mContext)) {
                    // show dialog to enable/install dialog
                    return new ImportKeyResult(null,
                            RequiredInputParcel.createOrbotRequiredOperation(), cryptoInput);
                }
                proxy = Preferences.getPreferences(mContext).getProxyPrefs().getProxy();
            } else {
                proxy = cryptoInput.getParcelableProxy().getProxy();
            }

            result = multiThreadedKeyImport(keyList.iterator(), keyList.size(), keyServer, proxy);
        }

        ContactSyncAdapterService.requestContactsSync();
        return result;
    }

    @NonNull
    private ImportKeyResult multiThreadedKeyImport(@NonNull Iterator<ParcelableKeyRing> keyListIterator,
                                                   int totKeys, final String keyServer,
                                                   final Proxy proxy) {
        Log.d(Constants.TAG, "Multi-threaded key import starting");
        KeyImportAccumulator accumulator = new KeyImportAccumulator(totKeys, mProgressable);

        final ProgressScaler ignoreProgressable = new ProgressScaler();

        ExecutorService importExecutor = new ThreadPoolExecutor(0, MAX_THREADS, 30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

        ExecutorCompletionService<ImportKeyResult> importCompletionService =
                new ExecutorCompletionService<>(importExecutor);

        while (keyListIterator.hasNext()) { // submit all key rings to be imported

            final ParcelableKeyRing pkRing = keyListIterator.next();

            Callable<ImportKeyResult> importOperationCallable = new Callable<ImportKeyResult>
                    () {

                @Override
                public ImportKeyResult call() {

                    if (checkCancelled()) {
                        return null;
                    }

                    ArrayList<ParcelableKeyRing> list = new ArrayList<>();
                    list.add(pkRing);

                    return serialKeyRingImport(list.iterator(), 1, keyServer, ignoreProgressable, proxy);
                }
            };

            importCompletionService.submit(importOperationCallable);
        }

        while (!accumulator.isImportFinished()) { // accumulate the results of each import
            try {
                accumulator.accumulateKeyImport(importCompletionService.take().get());
            } catch (InterruptedException | ExecutionException e) {
                Log.e(Constants.TAG, "A key could not be imported during multi-threaded " +
                        "import", e);
                // do nothing?
                if (e instanceof ExecutionException) {
                    // Since serialKeyRingImport does not throw any exceptions, this is what
                    // would have happened if
                    // we were importing the key on this thread
                    throw new RuntimeException();
                }
            }
        }
        return accumulator.getConsolidatedResult();

    }

    /**
     * Used to accumulate the results of individual key imports
     */
    public static class KeyImportAccumulator {
        private OperationResult.OperationLog mImportLog = new OperationResult.OperationLog();
        Progressable mProgressable;
        private int mTotalKeys;
        private int mImportedKeys = 0;
        ArrayList<Long> mImportedMasterKeyIds = new ArrayList<>();
        private int mBadKeys = 0;
        private int mNewKeys = 0;
        private int mUpdatedKeys = 0;
        private int mSecret = 0;
        private int mResultType = 0;
        private boolean mHasCancelledResult;

        /**
         * Accumulates keyring imports and updates the progressable whenever a new key is imported.
         * Also sets the progress to 0 on instantiation.
         *
         * @param totalKeys            total number of keys to be imported
         * @param externalProgressable the external progressable to be updated every time a key
         *                             is imported
         */
        public KeyImportAccumulator(int totalKeys, Progressable externalProgressable) {
            mTotalKeys = totalKeys;
            mProgressable = externalProgressable;
            if (mProgressable != null) {
                mProgressable.setProgress(0, totalKeys);
            }
        }

        public void accumulateKeyImport(ImportKeyResult result) {
            mImportedKeys++;

            if (result == null) {
                return;
            }

            if (mProgressable != null) {
                mProgressable.setProgress(mImportedKeys, mTotalKeys);
            }

            boolean notCancelledOrFirstCancelled = !result.cancelled() || !mHasCancelledResult;
            if (notCancelledOrFirstCancelled) {
                mImportLog.addAll(result.getLog().toList()); //accumulates log
                if (result.cancelled()) {
                    mHasCancelledResult = true;
                }
            }

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
        public ImportKeyResult getConsolidatedResult() {

            // adding required information to mResultType
            // special case,no keys requested for import
            if (mBadKeys == 0 && mNewKeys == 0 && mUpdatedKeys == 0
                    && (mResultType & ImportKeyResult.RESULT_CANCELLED)
                    != ImportKeyResult.RESULT_CANCELLED) {
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

}
