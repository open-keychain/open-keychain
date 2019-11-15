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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.daos.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
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
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.BackupKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.CountingOutputStream;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Numeric9x4PassphraseUtil;
import org.sufficientlysecure.keychain.util.Passphrase;
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
    // this is a very simple matcher, we only need basic sanitization
    private static final Pattern HEADER_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+: [^\\n]+");

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
            boolean backupSuccess = exportKeysToStream(log, backupInput.getMasterKeyIds(),
                    backupInput.getExportSecret(), backupInput.getExportPublic(), outStream, backupInput.getExtraHeaders());

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
        Passphrase passphrase = cryptoInput.getPassphrase();
        builder.setSymmetricPassphrase(passphrase);
        builder.setEnableAsciiArmorOutput(backupInput.getEnableAsciiArmorOutput());
        boolean isNumeric9x4Passphrase = passphrase != null && Numeric9x4PassphraseUtil.isNumeric9x4Passphrase(passphrase);
        if (isNumeric9x4Passphrase) {
            builder.setPassphraseFormat("numeric9x4");
            char[] passphraseChars = passphrase.getCharArray();
            builder.setPassphraseBegin("" + passphraseChars[0] + passphraseChars[1]);
        }
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

    boolean exportKeysToStream(OperationLog log, long[] masterKeyIds, boolean exportSecret, boolean exportPublic,
            OutputStream outStream, List<String> extraSecretKeyHeaders) {
        // noinspection unused TODO use these in a log entry
        int okSecret = 0, okPublic = 0;
        int progress = 0;

        try {
            List<UnifiedKeyInfo> unifiedKeyInfos;
            if (masterKeyIds == null) {
                unifiedKeyInfos = mKeyRepository.getAllUnifiedKeyInfo();
            } else {
                unifiedKeyInfos = mKeyRepository.getUnifiedKeyInfo(masterKeyIds);
            }
            int numKeys = unifiedKeyInfos.size();

            updateProgress(numKeys == 1 ? R.string.progress_exporting_key : R.string.progress_exporting_key, 0, numKeys);

            // For each public masterKey id
            for (UnifiedKeyInfo keyInfo : unifiedKeyInfos) {
                log.add(LogType.MSG_BACKUP_PUBLIC, 1, KeyFormattingUtils.beautifyKeyId(keyInfo.master_key_id()));

                boolean publicKeyWriteOk = false;
                if (exportPublic) {
                    publicKeyWriteOk = writePublicKeyToStream(keyInfo.master_key_id(), log, outStream);
                    if (publicKeyWriteOk) {
                        okPublic += 1;
                    }
                }

                if (publicKeyWriteOk || !exportPublic) {
                    if (exportSecret && keyInfo.has_any_secret()) {
                        log.add(LogType.MSG_BACKUP_SECRET, 2, KeyFormattingUtils.beautifyKeyId(keyInfo.master_key_id()));
                        if (writeSecretKeyToStream(keyInfo.master_key_id(), log, outStream, extraSecretKeyHeaders)) {
                            okSecret += 1;
                        }
                        extraSecretKeyHeaders = null;
                    }
                }

                updateProgress(progress++, numKeys);
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

    private boolean writeSecretKeyToStream(long masterKeyId, OperationLog log, OutputStream outStream,
            List<String> extraSecretKeyHeaders)
            throws IOException {
        ArmoredOutputStream arOutStream = null;

        try {
            arOutStream = new ArmoredOutputStream(outStream);
            if (extraSecretKeyHeaders != null) {
                addExtraHeadersToStream(arOutStream, extraSecretKeyHeaders);
            }

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

    private void addExtraHeadersToStream(ArmoredOutputStream arOutStream, List<String> headers) {
        for (String header : headers) {
            if (!HEADER_PATTERN.matcher(header).matches()) {
                throw new IllegalArgumentException("bad header format");
            }
            int sep = header.indexOf(':');
            arOutStream.setHeader(header.substring(0, sep), header.substring(sep + 2));
        }
    }

}