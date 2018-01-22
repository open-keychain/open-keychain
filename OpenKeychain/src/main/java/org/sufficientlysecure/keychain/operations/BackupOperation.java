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

package org.sufficientlysecure.keychain.operations;


import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.PgpSignEncryptResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptData;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptOperation;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.BackupKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.CountingOutputStream;
import org.sufficientlysecure.keychain.util.InputData;
import timber.log.Timber;


/**
 * An operation class which implements high level backup
 * operations.
 * This class receives a source and/or destination of keys as input and performs
 * all steps for this backup.
 *
 * see org.sufficientlysecure.keychain.ui.adapter.ImportKeysAdapter#getSelectedEntries()
 * For the backup operation, the input consists of a set of key ids and
 * either the name of a file or an output uri to write to.
 */
public class BackupOperation extends BaseOperation<BackupKeyringParcel> {

    private static final String[] PROJECTION = new String[] {
            KeyRings.MASTER_KEY_ID,
            KeyRings.HAS_ANY_SECRET
    };
    private static final int INDEX_MASTER_KEY_ID = 0;
    private static final int INDEX_HAS_ANY_SECRET = 1;

    public BackupOperation(Context context, KeyRepository keyRepository, Progressable
            progressable) {
        super(context, keyRepository, progressable);
    }

    public BackupOperation(Context context, KeyRepository keyRepository,
                           Progressable progressable, AtomicBoolean cancelled) {
        super(context, keyRepository, progressable, cancelled);
    }

    @NonNull
    public ExportResult execute(@NonNull BackupKeyringParcel backupInput, @Nullable CryptoInputParcel cryptoInput) {
        return execute(backupInput, cryptoInput, null);
    }

    @NonNull
    public ExportResult execute(@NonNull BackupKeyringParcel backupInput, @Nullable CryptoInputParcel cryptoInput,
                                OutputStream outputStream) {

        OperationLog log = new OperationLog();
        if (backupInput.getMasterKeyIds() != null) {
            log.add(LogType.MSG_BACKUP, 0, backupInput.getMasterKeyIds().length);
        } else {
            log.add(LogType.MSG_BACKUP_ALL, 0);
        }

        try {
            Uri plainUri = null;
            OutputStream plainOut;
            if (backupInput.getIsEncrypted()) {
                if (cryptoInput == null) {
                    throw new IllegalStateException("Encrypted backup must supply cryptoInput parameter");
                }

                plainUri = TemporaryFileProvider.createFile(mContext);
                plainOut = mContext.getContentResolver().openOutputStream(plainUri);
            } else {
                if (backupInput.getOutputUri() == null || outputStream != null) {
                    throw new IllegalArgumentException("Unencrypted export to output stream is not supported!");
                } else {
                    plainOut = mContext.getContentResolver().openOutputStream(backupInput.getOutputUri());
                }
            }

            CountingOutputStream outStream = new CountingOutputStream(new BufferedOutputStream(plainOut));
            boolean backupSuccess = exportKeysToStream(
                    log, backupInput.getMasterKeyIds(), backupInput.getExportSecret(), outStream);

            if (!backupSuccess) {
                // if there was an error, it will be in the log so we just have to return
                return new ExportResult(ExportResult.RESULT_ERROR, log);
            }

            if (!backupInput.getIsEncrypted()) {
                // log.add(LogType.MSG_EXPORT_NO_ENCRYPT, 1);
                log.add(LogType.MSG_BACKUP_SUCCESS, 1);
                return new ExportResult(ExportResult.RESULT_OK, log);
            }

            long exportedDataSize = outStream.getCount();
            PgpSignEncryptResult encryptResult =
                    encryptBackupData(backupInput, cryptoInput, outputStream, plainUri, exportedDataSize);

            if (!encryptResult.success()) {
                log.addByMerge(encryptResult, 1);
                // log.add(LogType.MSG_EXPORT_ERROR_ENCRYPT, 1);
                return new ExportResult(ExportResult.RESULT_ERROR, log);
            }

            log.add(encryptResult, 1);
            log.add(LogType.MSG_BACKUP_SUCCESS, 1);
            return new ExportResult(ExportResult.RESULT_OK, log);

        } catch (FileNotFoundException e) {
            log.add(LogType.MSG_BACKUP_ERROR_URI_OPEN, 1);
            return new ExportResult(ExportResult.RESULT_ERROR, log);
        }

    }

    @NonNull
    private PgpSignEncryptResult encryptBackupData(@NonNull BackupKeyringParcel backupInput,
            @NonNull CryptoInputParcel cryptoInput, @Nullable OutputStream outputStream, Uri plainUri, long exportedDataSize)
            throws FileNotFoundException {
        PgpSignEncryptOperation signEncryptOperation = new PgpSignEncryptOperation(mContext, mKeyRepository, mProgressable, mCancelled);

        PgpSignEncryptData.Builder builder = PgpSignEncryptData.builder();
        builder.setSymmetricPassphrase(cryptoInput.getPassphrase());
        builder.setEnableAsciiArmorOutput(backupInput.getEnableAsciiArmorOutput());
        builder.setAddBackupHeader(true);
        PgpSignEncryptData pgpSignEncryptData = builder.build();

        InputStream inStream = mContext.getContentResolver().openInputStream(plainUri);

        String filename;
        long[] masterKeyIds = backupInput.getMasterKeyIds();
        if (masterKeyIds != null && masterKeyIds.length == 1) {
            filename = Constants.FILE_BACKUP_PREFIX + KeyFormattingUtils.convertKeyIdToHex(
                    masterKeyIds[0]);
        } else {
            filename = Constants.FILE_BACKUP_PREFIX + new SimpleDateFormat("yyyy-MM-dd", Locale
                    .getDefault()).format(new Date());
        }
        filename += backupInput.getExportSecret() ? Constants.FILE_EXTENSION_BACKUP_SECRET : Constants.FILE_EXTENSION_BACKUP_PUBLIC;

        InputData inputData = new InputData(inStream, exportedDataSize, filename);

        OutputStream outStream;
        if (backupInput.getOutputUri() == null) {
            if (outputStream == null) {
                throw new IllegalArgumentException("If output uri is not set, outputStream must not be null!");
            }
            outStream = outputStream;
        } else {
            if (outputStream != null) {
                throw new IllegalArgumentException("If output uri is set, outputStream must null!");
            }
            outStream = mContext.getContentResolver().openOutputStream(backupInput.getOutputUri());
        }

        return signEncryptOperation.execute(
                pgpSignEncryptData, CryptoInputParcel.createCryptoInputParcel(), inputData, outStream);
    }

    boolean exportKeysToStream(OperationLog log, long[] masterKeyIds, boolean exportSecret, OutputStream outStream) {
        // noinspection unused TODO use these in a log entry
        int okSecret = 0, okPublic = 0;

        int progress = 0;

        Cursor cursor = queryForKeys(masterKeyIds);

        if (cursor == null || !cursor.moveToFirst()) {
            log.add(LogType.MSG_BACKUP_ERROR_DB, 1);
            return false; // new ExportResult(ExportResult.RESULT_ERROR, log);
        }

        try {

            int numKeys = cursor.getCount();

            updateProgress(mContext.getResources().getQuantityString(R.plurals.progress_exporting_key, numKeys),
                    0, numKeys);

            // For each public masterKey id
            while (!cursor.isAfterLast()) {

                long masterKeyId = cursor.getLong(INDEX_MASTER_KEY_ID);
                log.add(LogType.MSG_BACKUP_PUBLIC, 1, KeyFormattingUtils.beautifyKeyId(masterKeyId));

                if (writePublicKeyToStream(masterKeyId, log, outStream)) {
                    okPublic += 1;

                    boolean hasSecret = cursor.getInt(INDEX_HAS_ANY_SECRET) > 0;
                    if (exportSecret && hasSecret) {
                        log.add(LogType.MSG_BACKUP_SECRET, 2, KeyFormattingUtils.beautifyKeyId(masterKeyId));
                        if (writeSecretKeyToStream(masterKeyId, log, outStream)) {
                            okSecret += 1;
                        }
                    }
                }

                updateProgress(progress++, numKeys);
                cursor.moveToNext();
            }

            updateProgress(R.string.progress_done, numKeys, numKeys);

        } catch (IOException e) {
            log.add(LogType.MSG_BACKUP_ERROR_IO, 1);
            return false; // new ExportResult(ExportResult.RESULT_ERROR, log);
        } finally {
            // Make sure the stream is closed
            if (outStream != null) try {
                outStream.close();
            } catch (Exception e) {
                Timber.e(e, "error closing stream");
            }
            cursor.close();
        }

        return true;
    }

    private boolean writePublicKeyToStream(long masterKeyId, OperationLog log, OutputStream outStream) throws IOException {
        ArmoredOutputStream arOutStream = null;

        try {
            arOutStream = new ArmoredOutputStream(outStream);
            byte[] data = mKeyRepository.loadPublicKeyRingData(masterKeyId);
            UncachedKeyRing uncachedKeyRing = UncachedKeyRing.decodeFromData(data);
            CanonicalizedPublicKeyRing ring = (CanonicalizedPublicKeyRing) uncachedKeyRing.canonicalize(log, 2, true);
            ring.encode(arOutStream);
        } catch (PgpGeneralException | NotFoundException e) {
            log.add(LogType.MSG_UPLOAD_ERROR_IO, 2);
        } finally {
            if (arOutStream != null) {
                arOutStream.close();
            }
        }
        return true;
    }

    private boolean writeSecretKeyToStream(long masterKeyId, OperationLog log, OutputStream outStream)
            throws IOException {
        ArmoredOutputStream arOutStream = null;

        try {
            arOutStream = new ArmoredOutputStream(outStream);
            byte[] data = mKeyRepository.loadSecretKeyRingData(masterKeyId);
            UncachedKeyRing uncachedKeyRing = UncachedKeyRing.decodeFromData(data);
            CanonicalizedSecretKeyRing ring = (CanonicalizedSecretKeyRing) uncachedKeyRing.canonicalize(log, 2, true);
            ring.encode(arOutStream);
        } catch (PgpGeneralException | NotFoundException e) {
            log.add(LogType.MSG_UPLOAD_ERROR_IO, 2);
        } finally {
            if (arOutStream != null) {
                arOutStream.close();
            }
        }
        return true;
    }

    private Cursor queryForKeys(long[] masterKeyIds) {
        String selection = null, selectionArgs[] = null;

        if (masterKeyIds != null) {
            // convert long[] to String[]
            selectionArgs = new String[masterKeyIds.length];
            for (int i = 0; i < masterKeyIds.length; i++) {
                selectionArgs[i] = Long.toString(masterKeyIds[i]);
            }

            // generates ?,?,? as placeholders for selectionArgs
            String placeholders = TextUtils.join(",",
                    Collections.nCopies(masterKeyIds.length, "?"));

            // put together selection string
            selection = Tables.KEYS + "." + KeyRings.MASTER_KEY_ID
                    + " IN (" + placeholders + ")";
        }

        return mKeyRepository.getContentResolver().query(
                KeyRings.buildUnifiedKeyRingsUri(), PROJECTION, selection, selectionArgs,
                Tables.KEYS + "." + KeyRings.MASTER_KEY_ID
        );
    }

}