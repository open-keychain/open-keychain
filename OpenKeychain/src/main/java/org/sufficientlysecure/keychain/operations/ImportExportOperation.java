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
import org.sufficientlysecure.keychain.keyimport.Keyserver.AddKeyException;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Log;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class ImportExportOperation extends BaseOperation {

    public ImportExportOperation(Context context, ProviderHelper providerHelper, Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    public ImportExportOperation(Context context, ProviderHelper providerHelper, Progressable progressable, AtomicBoolean cancelled) {
        super(context, providerHelper, progressable, cancelled);
    }

    public void uploadKeyRingToServer(HkpKeyserver server, CanonicalizedPublicKeyRing keyring) throws AddKeyException {
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

    public ImportKeyResult importKeyRings(Iterator<ParcelableKeyRing> entries, int num) {
        updateProgress(R.string.progress_importing, 0, 100);

        OperationLog log = new OperationLog();
        log.add(LogType.MSG_IMPORT, 0, num);

        // If there aren't even any keys, do nothing here.
        if (entries == null || !entries.hasNext()) {
            return new ImportKeyResult(
                    ImportKeyResult.RESULT_FAIL_NOTHING, log, 0, 0, 0, 0,
                    new long[]{});
        }

        int newKeys = 0, oldKeys = 0, badKeys = 0, secret = 0;
        ArrayList<Long> importedMasterKeyIds = new ArrayList<Long>();

        boolean cancelled = false;
        int position = 0;
        double progSteps = 100.0 / num;

        // iterate over all entries
        while (entries.hasNext()) {
            ParcelableKeyRing entry = entries.next();

            // Has this action been cancelled? If so, don't proceed any further
            if (checkCancelled()) {
                cancelled = true;
                break;
            }

            try {
                UncachedKeyRing key = UncachedKeyRing.decodeFromData(entry.getBytes());

                String expectedFp = entry.getExpectedFingerprint();
                if(expectedFp != null) {
                    if(!KeyFormattingUtils.convertFingerprintToHex(key.getFingerprint()).equals(expectedFp)) {
                        Log.d(Constants.TAG, "fingerprint: " + KeyFormattingUtils.convertFingerprintToHex(key.getFingerprint()));
                        Log.d(Constants.TAG, "expected fingerprint: " + expectedFp);
                        Log.e(Constants.TAG, "Actual key fingerprint is not the same as expected!");
                        badKeys += 1;
                        continue;
                    } else {
                        Log.d(Constants.TAG, "Actual key fingerprint matches expected one.");
                    }
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
                    oldKeys += 1;
                    importedMasterKeyIds.add(key.getMasterKeyId());
                } else {
                    newKeys += 1;
                    if (key.isSecret()) {
                        secret += 1;
                    }
                    importedMasterKeyIds.add(key.getMasterKeyId());
                }

                log.add(result, 1);

            } catch (IOException e) {
                Log.e(Constants.TAG, "Encountered bad key on import!", e);
                ++badKeys;
            } catch (PgpGeneralException e) {
                Log.e(Constants.TAG, "Encountered bad key on import!", e);
                ++badKeys;
            }
            // update progress
            position++;
        }

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
        if (badKeys == 0 && newKeys == 0 && oldKeys == 0) {
            resultType = ImportKeyResult.RESULT_FAIL_NOTHING;
        } else {
            if (newKeys > 0) {
                resultType |= ImportKeyResult.RESULT_OK_NEWKEYS;
            }
            if (oldKeys > 0) {
                resultType |= ImportKeyResult.RESULT_OK_UPDATED;
            }
            if (badKeys > 0) {
                resultType |= ImportKeyResult.RESULT_WITH_ERRORS;
                if (newKeys == 0 && oldKeys == 0) {
                    resultType |= ImportKeyResult.RESULT_ERROR;
                }
            }
            if (log.containsWarnings()) {
                resultType |= ImportKeyResult.RESULT_WARNINGS;
            }
        }

        // Final log entry, it's easier to do this individually
        if ( (newKeys > 0 || oldKeys > 0) && badKeys > 0) {
            log.add(LogType.MSG_IMPORT_PARTIAL, 1);
        } else if (newKeys > 0 || oldKeys > 0) {
            log.add(LogType.MSG_IMPORT_SUCCESS, 1);
        } else {
            log.add(LogType.MSG_IMPORT_ERROR, 1);
        }

        return new ImportKeyResult(resultType, log, newKeys, oldKeys, badKeys, secret,
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

    private ExportResult exportKeyRings(OperationLog log, long[] masterKeyIds, boolean exportSecret,
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

            Cursor cursor = mProviderHelper.getContentResolver().query(
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

                // Create an output stream
                ArmoredOutputStream arOutStream = new ArmoredOutputStream(outStream);
                String version = PgpHelper.getVersionForHeader(mContext);
                if (version != null) {
                    arOutStream.setHeader("Version", version);
                }

                long keyId = cursor.getLong(0);

                log.add(LogType.MSG_EXPORT_PUBLIC, 1, KeyFormattingUtils.beautifyKeyId(keyId));

                { // export public key part
                    byte[] data = cursor.getBlob(1);
                    arOutStream.write(data);
                    arOutStream.close();

                    okPublic += 1;
                }

                // export secret key part
                if (exportSecret && cursor.getInt(3) > 0) {
                    log.add(LogType.MSG_EXPORT_SECRET, 2, KeyFormattingUtils.beautifyKeyId(keyId));
                    byte[] data = cursor.getBlob(2);
                    arOutStream.write(data);

                    okSecret += 1;
                }

                updateProgress(progress++, numKeys);

                cursor.moveToNext();
            }

            updateProgress(R.string.progress_done, numKeys, numKeys);

        } catch (IOException e) {
            log.add(LogType.MSG_EXPORT_ERROR_IO, 1);
            return new ExportResult(ExportResult.RESULT_ERROR, log, okPublic, okSecret);
        }


        log.add(LogType.MSG_EXPORT_SUCCESS, 1);
        return new ExportResult(ExportResult.RESULT_OK, log, okPublic, okSecret);

    }

}
