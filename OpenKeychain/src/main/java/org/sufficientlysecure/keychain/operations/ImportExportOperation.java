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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserver;
import org.sufficientlysecure.keychain.keyimport.KeybaseKeyserver;
import org.sufficientlysecure.keychain.keyimport.Keyserver;
import org.sufficientlysecure.keychain.keyimport.Keyserver.AddKeyException;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ConsolidateResult;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.ContactSyncAdapterService;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableFileCache;
import org.sufficientlysecure.keychain.util.ParcelableFileCache.IteratorWithSize;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** An operation class which implements high level import and export
 * operations.
 *
 * This class receives a source and/or destination of keys as input and performs
 * all steps for this import or export.
 *
 * For the import operation, the only valid source is an Iterator of
 * ParcelableKeyRing, each of which must contain either a single
 * keyring encoded as bytes, or a unique reference to a keyring
 * on keyservers and/or keybase.io.
 * It is important to note that public keys should generally be imported before
 * secret keys, because some implementations (notably Symantec PGP Desktop) do
 * not include self certificates for user ids in the secret keyring. The import
 * method here will generally import keyrings in the order given by the
 * iterator. so this should be ensured beforehand.
 * @see org.sufficientlysecure.keychain.ui.adapter.ImportKeysAdapter#getSelectedEntries()
 *
 * For the export operation, the input consists of a set of key ids and
 * either the name of a file or an output uri to write to.
 *
 * TODO rework uploadKeyRingToServer
 *
 */
public class ImportExportOperation extends BaseOperation {

    public ImportExportOperation(Context context, ProviderHelper providerHelper, Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    public ImportExportOperation(Context context, ProviderHelper providerHelper,
                                 Progressable progressable, AtomicBoolean cancelled) {
        super(context, providerHelper, progressable, cancelled);
    }

    public void uploadKeyRingToServer(HkpKeyserver server, CanonicalizedPublicKeyRing keyring) throws AddKeyException {
        uploadKeyRingToServer(server, keyring.getUncachedKeyRing());
    }

    public void uploadKeyRingToServer(HkpKeyserver server, UncachedKeyRing keyring) throws AddKeyException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ArmoredOutputStream aos = null;
        try {
            aos = new ArmoredOutputStream(bos);
            keyring.encode(aos);
            aos.close();

            String armoredKey = bos.toString("UTF-8");
            server.add(armoredKey);
        } catch (IOException e) {
            Log.e(Constants.TAG, "IOException", e);
            throw new AddKeyException();
        } finally {
            try {
                if (aos != null) {
                    aos.close();
                }
                bos.close();
            } catch (IOException e) {
                // this is just a finally thing, no matter if it doesn't work out.
            }
        }
    }

    public ImportKeyResult importKeyRings(List<ParcelableKeyRing> entries, String keyServerUri) {

        Iterator<ParcelableKeyRing> it = entries.iterator();
        int numEntries = entries.size();

        return importKeyRings(it, numEntries, keyServerUri);

    }

    public ImportKeyResult importKeyRings(ParcelableFileCache<ParcelableKeyRing> cache, String keyServerUri) {

        // get entries from cached file
        try {
            IteratorWithSize<ParcelableKeyRing> it = cache.readCache();
            int numEntries = it.getSize();

            return importKeyRings(it, numEntries, keyServerUri);
        } catch (IOException e) {

            // Special treatment here, we need a lot
            OperationLog log = new OperationLog();
            log.add(LogType.MSG_IMPORT, 0, 0);
            log.add(LogType.MSG_IMPORT_ERROR_IO, 0, 0);

            return new ImportKeyResult(ImportKeyResult.RESULT_ERROR, log);
        }

    }

    public ImportKeyResult importKeyRings(Iterator<ParcelableKeyRing> entries, int num, String keyServerUri) {
        updateProgress(R.string.progress_importing, 0, 100);

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
                    // from there, so the number of certificates which are merged in later is smaller.

                    // If we have a keyServerUri and a fingerprint or at least a keyId,
                    // download from HKP
                    if (keyServerUri != null
                            && (entry.mKeyIdHex != null || entry.mExpectedFingerprint != null)) {
                        // Make sure we have the keyserver instance cached
                        if (keyServer == null) {
                            log.add(LogType.MSG_IMPORT_KEYSERVER, 1, keyServerUri);
                            keyServer = new HkpKeyserver(keyServerUri);
                        }

                        try {
                            byte[] data;
                            // Download by fingerprint, or keyId - whichever is available
                            if (entry.mExpectedFingerprint != null) {
                                log.add(LogType.MSG_IMPORT_FETCH_KEYSERVER, 2, "0x" + entry.mExpectedFingerprint.substring(24));
                                data = keyServer.get("0x" + entry.mExpectedFingerprint).getBytes();
                            } else {
                                log.add(LogType.MSG_IMPORT_FETCH_KEYSERVER, 2, entry.mKeyIdHex);
                                data = keyServer.get(entry.mKeyIdHex).getBytes();
                            }
                            key = UncachedKeyRing.decodeFromData(data);
                            if (key != null) {
                                log.add(LogType.MSG_IMPORT_FETCH_KEYSERVER_OK, 3);
                            } else {
                                log.add(LogType.MSG_IMPORT_FETCH_ERROR_DECODE, 3);
                            }
                        } catch (Keyserver.QueryFailedException e) {
                            Log.e(Constants.TAG, "query failed", e);
                            log.add(LogType.MSG_IMPORT_FETCH_KEYSERVER_ERROR, 3, e.getMessage());
                        }
                    }

                    // If we have a keybase name, try to fetch from there
                    if (entry.mKeybaseName != null) {
                        // Make sure we have this cached
                        if (keybaseServer == null) {
                            keybaseServer = new KeybaseKeyserver();
                        }

                        try {
                            log.add(LogType.MSG_IMPORT_FETCH_KEYBASE, 2, entry.mKeybaseName);
                            byte[] data = keybaseServer.get(entry.mKeybaseName).getBytes();
                            key = UncachedKeyRing.decodeFromData(data);

                            // If there already is a key (of keybase origin), merge the two
                            if (key != null) {
                                log.add(LogType.MSG_IMPORT_MERGE, 3);
                                UncachedKeyRing merged = UncachedKeyRing.decodeFromData(data);
                                merged = key.merge(merged, log, 4);
                                // If the merge didn't fail, use the new merged key
                                if (merged != null) {
                                    key = merged;
                                }
                            } else {
                                log.add(LogType.MSG_IMPORT_FETCH_ERROR_DECODE, 3);
                                key = UncachedKeyRing.decodeFromData(data);
                            }
                        } catch (Keyserver.QueryFailedException e) {
                            // download failed, too bad. just proceed
                            Log.e(Constants.TAG, "query failed", e);
                            log.add(LogType.MSG_IMPORT_FETCH_KEYSERVER_ERROR, 3);
                        }
                    }
                }

                if (key == null) {
                    log.add(LogType.MSG_IMPORT_FETCH_ERROR, 2);
                    badKeys += 1;
                    continue;
                }

                // If we have an expected fingerprint, make sure it matches
                if (entry.mExpectedFingerprint != null) {
                    if (!key.containsSubkey(entry.mExpectedFingerprint)) {
                        log.add(LogType.MSG_IMPORT_FINGERPRINT_ERROR, 2);
                        badKeys += 1;
                        continue;
                    } else {
                        log.add(LogType.MSG_IMPORT_FINGERPRINT_OK, 2);
                    }
                }

                // Another check if we have been cancelled
                if (checkCancelled()) {
                    cancelled = true;
                    break;
                }

                SaveKeyringResult result;
                mProviderHelper.clearLog();
                if (key.isSecret()) {
                    result = mProviderHelper.saveSecretKeyRing(key,
                            new ProgressScaler(mProgressable, (int)(position*progSteps), (int)((position+1)*progSteps), 100));
                } else {
                    result = mProviderHelper.savePublicKeyRing(key,
                            new ProgressScaler(mProgressable, (int)(position*progSteps), (int)((position+1)*progSteps), 100));
                }
                if (!result.success()) {
                    badKeys += 1;
                } else if (result.updated()) {
                    updatedKeys += 1;
                    importedMasterKeyIds.add(key.getMasterKeyId());
                } else {
                    newKeys += 1;
                    if (key.isSecret()) {
                        secret += 1;
                    }
                    importedMasterKeyIds.add(key.getMasterKeyId());
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
        if (secret > 0) {
            setPreventCancel();
            ConsolidateResult result = mProviderHelper.consolidateDatabaseStep1(mProgressable);
            log.add(result, 1);
        }

        // Special: make sure new data is synced into contacts
        // disabling sync right now since it reduces speed while multi-threading
        // so, we expect calling functions to take care of it. KeychainIntentService handles this
        //ContactSyncAdapterService.requestSync();

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

        // Final log entry, it's easier to do this individually
        if ( (newKeys > 0 || updatedKeys > 0) && badKeys > 0) {
            log.add(LogType.MSG_IMPORT_PARTIAL, 1);
        } else if (newKeys > 0 || updatedKeys > 0) {
            log.add(LogType.MSG_IMPORT_SUCCESS, 1);
        } else {
            log.add(LogType.MSG_IMPORT_ERROR, 1);
        }

        ContactSyncAdapterService.requestSync();

        return new ImportKeyResult(resultType, log, newKeys, updatedKeys, badKeys, secret,
                importedMasterKeyIdsArray);
    }

    public ExportResult exportToFile(long[] masterKeyIds, boolean exportSecret, String outputFile) {

        OperationLog log = new OperationLog();
        if (masterKeyIds != null) {
            log.add(LogType.MSG_EXPORT, 0, masterKeyIds.length);
        } else {
            log.add(LogType.MSG_EXPORT_ALL, 0);
        }

        // do we have a file name?
        if (outputFile == null) {
            log.add(LogType.MSG_EXPORT_ERROR_NO_FILE, 1);
            return new ExportResult(ExportResult.RESULT_ERROR, log);
        }

        // check if storage is ready
        if (!FileHelper.isStorageMounted(outputFile)) {
            log.add(LogType.MSG_EXPORT_ERROR_STORAGE, 1);
            return new ExportResult(ExportResult.RESULT_ERROR, log);
        }

        try {
            OutputStream outStream = new FileOutputStream(outputFile);
            ExportResult result = exportKeyRings(log, masterKeyIds, exportSecret, outStream);
            if (result.cancelled()) {
                //noinspection ResultOfMethodCallIgnored
                new File(outputFile).delete();
            }
            return result;
        } catch (FileNotFoundException e) {
            log.add(LogType.MSG_EXPORT_ERROR_FOPEN, 1);
            return new ExportResult(ExportResult.RESULT_ERROR, log);
        }

    }

    public ExportResult exportToUri(long[] masterKeyIds, boolean exportSecret, Uri outputUri) {

        OperationLog log = new OperationLog();
        if (masterKeyIds != null) {
            log.add(LogType.MSG_EXPORT, 0, masterKeyIds.length);
        } else {
            log.add(LogType.MSG_EXPORT_ALL, 0);
        }

        // do we have a file name?
        if (outputUri == null) {
            log.add(LogType.MSG_EXPORT_ERROR_NO_URI, 1);
            return new ExportResult(ExportResult.RESULT_ERROR, log);
        }

        try {
            OutputStream outStream = mProviderHelper.getContentResolver().openOutputStream(outputUri);
            return exportKeyRings(log, masterKeyIds, exportSecret, outStream);
        } catch (FileNotFoundException e) {
            log.add(LogType.MSG_EXPORT_ERROR_URI_OPEN, 1);
            return new ExportResult(ExportResult.RESULT_ERROR, log);
        }

    }

    ExportResult exportKeyRings(OperationLog log, long[] masterKeyIds, boolean exportSecret,
                                 OutputStream outStream) {

        /* TODO isn't this checked above, with the isStorageMounted call?
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            log.add(LogType.MSG_EXPORT_ERROR_STORAGE, 1);
            return new ExportResult(ExportResult.RESULT_ERROR, log);
        }
        */

        if ( ! BufferedOutputStream.class.isInstance(outStream)) {
            outStream = new BufferedOutputStream(outStream);
        }

        int okSecret = 0, okPublic = 0, progress = 0;

        Cursor cursor = null;
        try {

            String selection = null, ids[] = null;

            if (masterKeyIds != null) {
                // generate placeholders and string selection args
                ids = new String[masterKeyIds.length];
                StringBuilder placeholders = new StringBuilder("?");
                for (int i = 0; i < masterKeyIds.length; i++) {
                    ids[i] = Long.toString(masterKeyIds[i]);
                    if (i != 0) {
                        placeholders.append(",?");
                    }
                }

                // put together selection string
                selection = Tables.KEY_RINGS_PUBLIC + "." + KeyRings.MASTER_KEY_ID
                        + " IN (" + placeholders + ")";
            }

            cursor = mProviderHelper.getContentResolver().query(
                    KeyRings.buildUnifiedKeyRingsUri(), new String[]{
                            KeyRings.MASTER_KEY_ID, KeyRings.PUBKEY_DATA,
                            KeyRings.PRIVKEY_DATA, KeyRings.HAS_ANY_SECRET
                    }, selection, ids, Tables.KEYS + "." + KeyRings.MASTER_KEY_ID
            );

            if (cursor == null || !cursor.moveToFirst()) {
                log.add(LogType.MSG_EXPORT_ERROR_DB, 1);
                return new ExportResult(ExportResult.RESULT_ERROR, log, okPublic, okSecret);
            }

            int numKeys = cursor.getCount();

            updateProgress(
                    mContext.getResources().getQuantityString(R.plurals.progress_exporting_key,
                            numKeys), 0, numKeys);

            // For each public masterKey id
            while (!cursor.isAfterLast()) {

                long keyId = cursor.getLong(0);
                ArmoredOutputStream arOutStream = null;

                // Create an output stream
                try {
                    arOutStream = new ArmoredOutputStream(outStream);

                    log.add(LogType.MSG_EXPORT_PUBLIC, 1, KeyFormattingUtils.beautifyKeyId(keyId));

                    byte[] data = cursor.getBlob(1);
                    CanonicalizedKeyRing ring =
                            UncachedKeyRing.decodeFromData(data).canonicalize(log, 2, true);
                    ring.encode(arOutStream);

                    okPublic += 1;
                } catch (PgpGeneralException e) {
                    log.add(LogType.MSG_EXPORT_ERROR_KEY, 2);
                    updateProgress(progress++, numKeys);
                    continue;
                } finally {
                    // make sure this is closed
                    if (arOutStream != null) {
                        arOutStream.close();
                    }
                    arOutStream = null;
                }

                if (exportSecret && cursor.getInt(3) > 0) {
                    try {
                        arOutStream = new ArmoredOutputStream(outStream);

                        // export secret key part
                        log.add(LogType.MSG_EXPORT_SECRET, 2, KeyFormattingUtils.beautifyKeyId(keyId));
                        byte[] data = cursor.getBlob(2);
                        CanonicalizedKeyRing ring =
                                UncachedKeyRing.decodeFromData(data).canonicalize(log, 2, true);
                        ring.encode(arOutStream);

                        okSecret += 1;
                    } catch (PgpGeneralException e) {
                        log.add(LogType.MSG_EXPORT_ERROR_KEY, 2);
                        updateProgress(progress++, numKeys);
                        continue;
                    } finally {
                        // make sure this is closed
                        if (arOutStream != null) {
                            arOutStream.close();
                        }
                    }
                }

                updateProgress(progress++, numKeys);

                cursor.moveToNext();
            }

            updateProgress(R.string.progress_done, numKeys, numKeys);

        } catch (IOException e) {
            log.add(LogType.MSG_EXPORT_ERROR_IO, 1);
            return new ExportResult(ExportResult.RESULT_ERROR, log, okPublic, okSecret);
        } finally {
            // Make sure the stream is closed
            if (outStream != null) try {
                outStream.close();
            } catch (Exception e) {
                Log.e(Constants.TAG, "error closing stream", e);
            }
            if (cursor != null) {
                cursor.close();
            }
        }


        log.add(LogType.MSG_EXPORT_SUCCESS, 1);
        return new ExportResult(ExportResult.RESULT_OK, log, okPublic, okSecret);

    }

}
