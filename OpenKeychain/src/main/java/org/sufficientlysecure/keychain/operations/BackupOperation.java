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


import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptData;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptOperation;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.BackupKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.CountingOutputStream;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableHashMap;
import org.sufficientlysecure.keychain.util.Passphrase;


/**
 * An operation class which implements high level backup
 * operations.
 * This class receives a source and/or destination of keys as input and performs
 * all steps for this backup.
 *
 * @see org.sufficientlysecure.keychain.ui.adapter.ImportKeysAdapter#getSelectedEntries()
 * For the backup operation, the input consists of a set of key ids and
 * either the name of a file or an output uri to write to.
 */
public class BackupOperation extends BaseOperation<BackupKeyringParcel> {

    private static final String[] PROJECTION = new String[] {
            KeyRings.MASTER_KEY_ID,
            KeyRings.PUBKEY_DATA,
            KeyRings.PRIVKEY_DATA,
            KeyRings.HAS_ANY_SECRET
    };
    private static final int INDEX_MASTER_KEY_ID = 0;
    private static final int INDEX_PUBKEY_DATA = 1;
    private static final int INDEX_SECKEY_DATA = 2;
    private static final int INDEX_HAS_ANY_SECRET = 3;

    public BackupOperation(Context context, ProviderHelper providerHelper, Progressable
            progressable) {
        super(context, providerHelper, progressable);
    }

    public BackupOperation(Context context, ProviderHelper providerHelper,
                           Progressable progressable, AtomicBoolean cancelled) {
        super(context, providerHelper, progressable, cancelled);
    }

    @NonNull
    public ExportResult execute(@NonNull BackupKeyringParcel backupInput, @Nullable CryptoInputParcel cryptoInput) {
        return execute(backupInput, cryptoInput, null);
    }

    @NonNull
    public ExportResult execute(@NonNull BackupKeyringParcel backupInput, @Nullable CryptoInputParcel cryptoInput,
                                OutputStream outputStream) {

        OperationLog log = new OperationLog();
        if (backupInput.mMasterKeyIds != null) {
            log.add(LogType.MSG_BACKUP, 0, backupInput.mMasterKeyIds.length);
        } else {
            log.add(LogType.MSG_BACKUP_ALL, 0);
        }

        try {
            Uri plainUri = null;
            OutputStream plainOut;
            if (backupInput.mIsEncrypted) {
                if (cryptoInput == null) {
                    throw new IllegalStateException("Encrypted backup must supply cryptoInput parameter");
                }

                plainUri = TemporaryFileProvider.createFile(mContext);
                plainOut = mContext.getContentResolver().openOutputStream(plainUri);
            } else {
                if (backupInput.mOutputUri == null || outputStream != null) {
                    throw new IllegalArgumentException("Unencrypted export to output stream is not supported!");
                } else {
                    plainOut = mContext.getContentResolver().openOutputStream(backupInput.mOutputUri);
                }
            }

            CountingOutputStream outStream = new CountingOutputStream(new BufferedOutputStream(plainOut));

            HashMap<Long, Passphrase> passphrases =
                    ParcelableHashMap.toHashMap(backupInput.mParcelablePassphrases);
            boolean backupSuccess = exportKeysToStream(
                    log, backupInput.mMasterKeyIds, backupInput.mExportSecret, passphrases, outStream);

            if (!backupSuccess) {
                // if there was an error, it will be in the log so we just have to return
                return new ExportResult(ExportResult.RESULT_ERROR, log);
            }

            if (!backupInput.mIsEncrypted) {
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
        PgpSignEncryptOperation signEncryptOperation = new PgpSignEncryptOperation(mContext, mProviderHelper, mProgressable, mCancelled);

        PgpSignEncryptData data = new PgpSignEncryptData();
        data.setSymmetricPassphrase(cryptoInput.getPassphrase());
        data.setEnableAsciiArmorOutput(true);
        data.setAddBackupHeader(true);
        PgpSignEncryptInputParcel inputParcel = new PgpSignEncryptInputParcel(data);

        InputStream inStream = mContext.getContentResolver().openInputStream(plainUri);

        String filename;
        if (backupInput.mMasterKeyIds != null && backupInput.mMasterKeyIds.length == 1) {
            filename = Constants.FILE_BACKUP_PREFIX + KeyFormattingUtils.convertKeyIdToHex(backupInput.mMasterKeyIds[0]);
        } else {
            filename = Constants.FILE_BACKUP_PREFIX + new SimpleDateFormat("yyyy-MM-dd", Locale
                    .getDefault()).format(new Date());
        }
        filename += backupInput.mExportSecret ? Constants.FILE_EXTENSION_BACKUP_SECRET : Constants.FILE_EXTENSION_BACKUP_PUBLIC;

        InputData inputData = new InputData(inStream, exportedDataSize, filename);

        OutputStream outStream;
        if (backupInput.mOutputUri == null) {
            if (outputStream == null) {
                throw new IllegalArgumentException("If output uri is not set, outputStream must not be null!");
            }
            outStream = outputStream;
        } else {
            if (outputStream != null) {
                throw new IllegalArgumentException("If output uri is set, outputStream must null!");
            }
            outStream = mContext.getContentResolver().openOutputStream(backupInput.mOutputUri);
        }

        return signEncryptOperation.execute(inputParcel, new CryptoInputParcel(), inputData, outStream);
    }

    boolean exportKeysToStream(OperationLog log, long[] masterKeyIds, boolean exportSecret,
                               HashMap<Long, Passphrase> passphrases, OutputStream outStream) {
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

                long keyId = cursor.getLong(INDEX_MASTER_KEY_ID);
                log.add(LogType.MSG_BACKUP_PUBLIC, 1, KeyFormattingUtils.beautifyKeyId(keyId));

                if (writePublicKeyToStream(log, outStream, cursor)) {
                    okPublic += 1;

                    boolean hasSecret = cursor.getInt(INDEX_HAS_ANY_SECRET) > 0;
                    if (exportSecret && hasSecret) {
                        log.add(LogType.MSG_BACKUP_SECRET, 2, KeyFormattingUtils.beautifyKeyId(keyId));
                        if (!passphrases.containsKey(keyId)) {
                            log.add(LogType.MSG_BACKUP_ERROR_MISSING_PASSPHRASE, 3);
                            continue;
                        }
                        if (writeSecretKeyToStream(log, outStream, cursor, passphrases.get(keyId))) {
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
        } catch (ByteArrayEncryptor.IncorrectPassphraseException e) {
            log.add(LogType.MSG_BACKUP_ERROR_INCORRECT_PASSPHRASE, 1);
            return false;
        } catch (ByteArrayEncryptor.EncryptDecryptException e) {
            log.add(LogType.MSG_BACKUP_ERROR_DECRYPT, 1);
            return false;
        } finally {
            // Make sure the stream is closed
            if (outStream != null) try {
                outStream.close();
            } catch (Exception e) {
                Log.e(Constants.TAG, "error closing stream", e);
            }
            cursor.close();
        }

        return true;
    }

    private boolean writePublicKeyToStream(OperationLog log, OutputStream outStream, Cursor cursor)
            throws IOException {
        ArmoredOutputStream arOutStream = null;

        try {
            arOutStream = new ArmoredOutputStream(outStream);
            byte[] data = cursor.getBlob(INDEX_PUBKEY_DATA);
            CanonicalizedKeyRing ring = UncachedKeyRing.decodeFromData(data).canonicalize(log, 2, true);
            ring.encode(arOutStream);

        } catch (PgpGeneralException e) {
            log.add(LogType.MSG_UPLOAD_ERROR_IO, 2);
        } finally {
            if (arOutStream != null) {
                arOutStream.close();
            }
        }
        return true;
    }

    private boolean writeSecretKeyToStream(OperationLog log, OutputStream outStream,
                                           Cursor cursor, Passphrase passphrase) throws IOException,
            ByteArrayEncryptor.IncorrectPassphraseException, ByteArrayEncryptor.EncryptDecryptException {
        ArmoredOutputStream arOutStream = null;

        try {
            arOutStream = new ArmoredOutputStream(outStream);
            byte[] data = cursor.getBlob(INDEX_SECKEY_DATA);
            data = ByteArrayEncryptor.decryptByteArray(data, passphrase.getCharArray());
            CanonicalizedKeyRing ring = UncachedKeyRing.decodeFromData(data).canonicalize(log, 2, true);
            ring.encode(arOutStream);
        } catch (PgpGeneralException e) {
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

        return mProviderHelper.getContentResolver().query(
                KeyRings.buildUnifiedKeyRingsUri(), PROJECTION, selection, selectionArgs,
                Tables.KEYS + "." + KeyRings.MASTER_KEY_ID
        );
    }

}