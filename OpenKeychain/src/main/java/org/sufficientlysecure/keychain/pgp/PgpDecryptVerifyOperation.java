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

package org.sufficientlysecure.keychain.pgp;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SignatureException;
import java.util.Date;
import java.util.Iterator;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpMetadata;
import org.spongycastle.bcpg.ArmoredInputStream;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPDataValidationException;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyValidationException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPPBEEncryptedData;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.spongycastle.openpgp.operator.PBEDataDecryptorFactory;
import org.spongycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.spongycastle.openpgp.operator.jcajce.CachingDataDecryptorFactory;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.spongycastle.util.encoders.DecoderException;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Constants.key;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.BaseOperation;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ProgressScaler;

public class PgpDecryptVerifyOperation extends BaseOperation<PgpDecryptVerifyInputParcel> {

    public PgpDecryptVerifyOperation(Context context, ProviderHelper providerHelper, Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    /** Decrypts and/or verifies data based on parameters of PgpDecryptVerifyInputParcel. */
    @NonNull
    public DecryptVerifyResult execute(PgpDecryptVerifyInputParcel input, CryptoInputParcel cryptoInput) {
        InputData inputData;
        OutputStream outputStream;

        if (input.getInputBytes() != null) {
            byte[] inputBytes = input.getInputBytes();
            inputData = new InputData(new ByteArrayInputStream(inputBytes), inputBytes.length);
        } else {
            try {
                InputStream inputStream = mContext.getContentResolver().openInputStream(input.getInputUri());
                long inputSize = FileHelper.getFileSize(mContext, input.getInputUri(), 0);
                inputData = new InputData(inputStream, inputSize);
            } catch (FileNotFoundException e) {
                Log.e(Constants.TAG, "Input URI could not be opened: " + input.getInputUri(), e);
                OperationLog log = new OperationLog();
                log.add(LogType.MSG_DC_ERROR_INPUT, 1);
                return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
            }
        }

        if (input.getOutputUri() == null) {
            outputStream = new ByteArrayOutputStream();
        } else {
            try {
                outputStream = mContext.getContentResolver().openOutputStream(input.getOutputUri());
            } catch (FileNotFoundException e) {
                Log.e(Constants.TAG, "Output URI could not be opened: " + input.getOutputUri(), e);
                OperationLog log = new OperationLog();
                log.add(LogType.MSG_DC_ERROR_IO, 1);
                return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
            }
        }

        DecryptVerifyResult result = executeInternal(input, cryptoInput, inputData, outputStream);
        if (outputStream instanceof ByteArrayOutputStream) {
            byte[] outputData = ((ByteArrayOutputStream) outputStream).toByteArray();
            result.setOutputBytes(outputData);
        }

        return result;

    }

    @NonNull
    public DecryptVerifyResult execute(PgpDecryptVerifyInputParcel input, CryptoInputParcel cryptoInput,
            InputData inputData, OutputStream outputStream) {
        return executeInternal(input, cryptoInput, inputData, outputStream);
    }

    @NonNull
    private DecryptVerifyResult executeInternal(PgpDecryptVerifyInputParcel input, CryptoInputParcel cryptoInput,
            InputData inputData, OutputStream outputStream) {
        try {
            if (input.getDetachedSignature() != null) {
                Log.d(Constants.TAG, "Detached signature present, verifying with this signature only");

                return verifyDetachedSignature(input, inputData, outputStream, 0);
            } else {
                // automatically works with PGP ascii armor and PGP binary
                InputStream in = PGPUtil.getDecoderStream(inputData.getInputStream());

                if (in instanceof ArmoredInputStream) {
                    ArmoredInputStream aIn = (ArmoredInputStream) in;
                    // it is ascii armored
                    Log.d(Constants.TAG, "ASCII Armor Header Line: " + aIn.getArmorHeaderLine());

                    if (aIn.isClearText()) {
                        // a cleartext signature, verify it with the other method
                        return verifyCleartextSignature(aIn, outputStream, 0);
                    } else {
                        // else: ascii armored encryption! go on...
                        return decryptVerify(input, cryptoInput, in, outputStream, 0);
                    }
                } else {
                    return decryptVerify(input, cryptoInput, in, outputStream, 0);
                }
            }
        } catch (PGPException e) {
            Log.d(Constants.TAG, "PGPException", e);
            OperationLog log = new OperationLog();
            log.add(LogType.MSG_DC_ERROR_PGP_EXCEPTION, 1);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        } catch (DecoderException | ArrayIndexOutOfBoundsException e) {
            // these can happen if assumptions in JcaPGPObjectFactory.nextObject() aren't
            // fulfilled, so we need to catch them here to handle this gracefully
            Log.d(Constants.TAG, "data error", e);
            OperationLog log = new OperationLog();
            log.add(LogType.MSG_DC_ERROR_IO, 1);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        } catch (IOException e) {
            Log.d(Constants.TAG, "IOException", e);
            OperationLog log = new OperationLog();
            log.add(LogType.MSG_DC_ERROR_IO, 1);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }
    }

    private static class EncryptStreamResult {

        // this is non-null iff an error occured, return directly
        DecryptVerifyResult errorResult;

        // for verification
        PGPEncryptedData encryptedData;
        InputStream cleartextStream;

        int symmetricEncryptionAlgo = 0;

        boolean skippedDisallowedKey = false;
        boolean insecureEncryptionKey = false;

        // convenience method to return with error
        public EncryptStreamResult with(DecryptVerifyResult result) {
            errorResult = result;
            return this;
        }

    }

    private static class ArmorHeaders {
        String charset = null;
        Integer backupVersion = null;
    }

    private ArmorHeaders parseArmorHeaders(InputStream in, OperationLog log, int indent) {
        ArmorHeaders armorHeaders = new ArmorHeaders();

        // If the input stream is armored, and there is a charset specified, take a note for later
        // https://tools.ietf.org/html/rfc4880#page56
        if (in instanceof ArmoredInputStream) {
            ArmoredInputStream aIn = (ArmoredInputStream) in;
            if (aIn.getArmorHeaders() != null) {
                for (String header : aIn.getArmorHeaders()) {
                    String[] pieces = header.split(":", 2);
                    if (pieces.length != 2
                            || TextUtils.isEmpty(pieces[0])
                            || TextUtils.isEmpty(pieces[1])) {
                        continue;
                    }

                    switch (pieces[0].toLowerCase()) {
                        case "charset": {
                            armorHeaders.charset = pieces[1].trim();
                            break;
                        }
                        case "backupversion": {
                            try {
                                armorHeaders.backupVersion = Integer.valueOf(pieces[1].trim());
                            } catch (NumberFormatException e) {
                                continue;
                            }
                            break;
                        }
                        default: {
                            // continue;
                        }
                    }
                }
                if (armorHeaders.charset != null) {
                    log.add(LogType.MSG_DC_CHARSET, indent, armorHeaders.charset);
                }
                if (armorHeaders.backupVersion != null) {
                    log.add(LogType.MSG_DC_BACKUP_VERSION, indent, Integer.toString(armorHeaders.backupVersion));
                }
            }
        }

        return armorHeaders;
    }

    /** Decrypt and/or verify binary or ascii armored pgp data. */
    @NonNull
    private DecryptVerifyResult decryptVerify(
            PgpDecryptVerifyInputParcel input, CryptoInputParcel cryptoInput,
            InputStream in, OutputStream out, int indent) throws IOException, PGPException {

        OperationLog log = new OperationLog();

        log.add(LogType.MSG_DC, indent);
        indent += 1;

        int currentProgress = 0;
        updateProgress(R.string.progress_reading_data, currentProgress, 100);

        // parse ASCII Armor headers
        ArmorHeaders armorHeaders = parseArmorHeaders(in, log, indent);
        String charset = armorHeaders.charset;
        boolean useBackupCode = false;
        if (armorHeaders.backupVersion != null && armorHeaders.backupVersion == 1) {
            useBackupCode = true;
        }

        OpenPgpDecryptionResultBuilder decryptionResultBuilder = new OpenPgpDecryptionResultBuilder();

        JcaPGPObjectFactory plainFact;
        Object dataChunk;
        EncryptStreamResult esResult = null;
        { // resolve encrypted (symmetric and asymmetric) packets
            JcaPGPObjectFactory pgpF = new JcaPGPObjectFactory(in);
            Object obj = pgpF.nextObject();

            if (obj instanceof PGPEncryptedDataList) {
                esResult = handleEncryptedPacket(
                        input, cryptoInput, (PGPEncryptedDataList) obj, log, indent,
                        currentProgress, useBackupCode);

                // if there is an error, nothing left to do here
                if (esResult.errorResult != null) {
                    return esResult.errorResult;
                }

                // if this worked out so far, the data is encrypted
                decryptionResultBuilder.setEncrypted(true);

                if (esResult.insecureEncryptionKey) {
                    log.add(LogType.MSG_DC_INSECURE_SYMMETRIC_ENCRYPTION_ALGO, indent + 1);
                    decryptionResultBuilder.setInsecure(true);
                }

                // Check for insecure encryption algorithms!
                if (!PgpSecurityConstants.isSecureSymmetricAlgorithm(esResult.symmetricEncryptionAlgo)) {
                    log.add(LogType.MSG_DC_INSECURE_SYMMETRIC_ENCRYPTION_ALGO, indent + 1);
                    decryptionResultBuilder.setInsecure(true);
                }

                plainFact = new JcaPGPObjectFactory(esResult.cleartextStream);
                dataChunk = plainFact.nextObject();

            } else {
                decryptionResultBuilder.setEncrypted(false);

                plainFact = pgpF;
                dataChunk = obj;
            }

        }

        log.add(LogType.MSG_DC_PREP_STREAMS, indent);

        log.add(LogType.MSG_DC_CLEAR, indent);
        indent += 1;

        // resolve compressed data
        if (dataChunk instanceof PGPCompressedData) {
            log.add(LogType.MSG_DC_CLEAR_DECOMPRESS, indent + 1);
            currentProgress += 2;
            updateProgress(R.string.progress_decompressing_data, currentProgress, 100);

            PGPCompressedData compressedData = (PGPCompressedData) dataChunk;

            JcaPGPObjectFactory fact = new JcaPGPObjectFactory(compressedData.getDataStream());
            dataChunk = fact.nextObject();
            plainFact = fact;
        }

        PgpSignatureChecker signatureChecker = new PgpSignatureChecker(mProviderHelper);
        if (signatureChecker.initializeOnePassSignature(dataChunk, log, indent +1)) {
            dataChunk = plainFact.nextObject();
        }

        if (dataChunk instanceof PGPSignatureList) {
            // skip
            dataChunk = plainFact.nextObject();
        }

        OpenPgpMetadata metadata;

        if ( ! (dataChunk instanceof PGPLiteralData)) {

            log.add(LogType.MSG_DC_ERROR_INVALID_DATA, indent);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);

        }

        log.add(LogType.MSG_DC_CLEAR_DATA, indent + 1);
        indent += 2;
        currentProgress += 4;
        updateProgress(R.string.progress_decrypting, currentProgress, 100);

        PGPLiteralData literalData = (PGPLiteralData) dataChunk;

        String originalFilename = literalData.getFileName();
        // reject filenames with slashes completely (path traversal issue)
        if (originalFilename.contains("/")) {
            originalFilename = "";
        }
        String mimeType = null;
        if (literalData.getFormat() == PGPLiteralData.TEXT
                || literalData.getFormat() == PGPLiteralData.UTF8) {
            mimeType = "text/plain";
        } else {
            // try to guess from file ending
            String extension = MimeTypeMap.getFileExtensionFromUrl(originalFilename);
            if (extension != null) {
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                mimeType = mime.getMimeTypeFromExtension(extension);
            }
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
        }

        if (!"".equals(originalFilename)) {
            log.add(LogType.MSG_DC_CLEAR_META_FILE, indent + 1, originalFilename);
        }
        log.add(LogType.MSG_DC_CLEAR_META_TIME, indent + 1,
                new Date(literalData.getModificationTime().getTime()).toString());

        // return here if we want to decrypt the metadata only
        if (input.isDecryptMetadataOnly()) {

            log.add(LogType.MSG_DC_CLEAR_META_MIME, indent + 1, mimeType);

            // this operation skips the entire stream to find the data length!
            Long originalSize = literalData.findDataLength();

            if (originalSize != null) {
                log.add(LogType.MSG_DC_CLEAR_META_SIZE, indent + 1,
                        Long.toString(originalSize));
            } else {
                log.add(LogType.MSG_DC_CLEAR_META_SIZE_UNKNOWN, indent + 1);
            }

            metadata = new OpenPgpMetadata(
                    originalFilename,
                    mimeType,
                    literalData.getModificationTime().getTime(),
                    originalSize == null ? 0 : originalSize,
                    charset);

            log.add(LogType.MSG_DC_OK_META_ONLY, indent);
            DecryptVerifyResult result =
                    new DecryptVerifyResult(DecryptVerifyResult.RESULT_OK, log);
            result.setDecryptionMetadata(metadata);
            return result;
        }

        ProgressScaler progressScaler =
                new ProgressScaler(mProgressable, currentProgress, 95, 100);

        InputStream dataIn = literalData.getInputStream();

        long alreadyWritten = 0;
        long wholeSize = 0; // TODO inputData.getSize() - inputData.getStreamPosition();
        int length;
        byte[] buffer = new byte[1 << 16];
        byte[] firstBytes = new byte[48];
        while ((length = dataIn.read(buffer)) > 0) {
            // Log.d(Constants.TAG, "read bytes: " + length);
            if (out != null) {
                out.write(buffer, 0, length);
            }

            // update signature buffer if signature is also present
            signatureChecker.updateSignatureData(buffer, 0, length);

            // note down first couple of bytes for "magic bytes" file type detection
            if (alreadyWritten == 0) {
                System.arraycopy(buffer, 0, firstBytes, 0, length > firstBytes.length ? firstBytes.length : length);
            }

            alreadyWritten += length;
            // noinspection ConstantConditions, TODO progress
            if (wholeSize > 0) {
                long progress = 100 * alreadyWritten / wholeSize;
                // stop at 100% for wrong file sizes...
                if (progress > 100) {
                    progress = 100;
                }
                progressScaler.setProgress((int) progress, 100);
            }
        }

        // special treatment to detect pgp mime types
        if (matchesPrefix(firstBytes, "-----BEGIN PGP PUBLIC KEY BLOCK-----")
                || matchesPrefix(firstBytes, "-----BEGIN PGP PRIVATE KEY BLOCK-----")) {
            mimeType = Constants.MIME_TYPE_KEYS;
        } else if (matchesPrefix(firstBytes, "-----BEGIN PGP MESSAGE-----")) {
            // this is NOT application/pgp-encrypted, see RFC 3156!
            mimeType = Constants.MIME_TYPE_ENCRYPTED_ALTERNATE;
        }

        log.add(LogType.MSG_DC_CLEAR_META_MIME, indent + 1, mimeType);

        metadata = new OpenPgpMetadata(
                originalFilename, mimeType, literalData.getModificationTime().getTime(), alreadyWritten, charset);

        if (signatureChecker.isInitialized()) {

            Object o = plainFact.nextObject();
            boolean signatureCheckOk = signatureChecker.verifySignatureOnePass(o, log, indent + 1);

            if (!signatureCheckOk) {
                return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
            }

        }

        indent -= 1;

        if (esResult != null) {
            if (esResult.encryptedData.isIntegrityProtected()) {
                updateProgress(R.string.progress_verifying_integrity, 95, 100);

                if (esResult.encryptedData.verify()) {
                    log.add(LogType.MSG_DC_INTEGRITY_CHECK_OK, indent);
                } else {
                    log.add(LogType.MSG_DC_ERROR_INTEGRITY_CHECK, indent);
                    return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
                }
            } else if ( ! signatureChecker.isInitialized() ) {
                // If no signature is present, we *require* an MDC!
                // Handle missing integrity protection like failed integrity protection!
                // The MDC packet can be stripped by an attacker!
                log.add(LogType.MSG_DC_INSECURE_MDC_MISSING, indent);
                decryptionResultBuilder.setInsecure(true);
            }
        }

        updateProgress(R.string.progress_done, 100, 100);

        log.add(LogType.MSG_DC_OK, indent);

        // Return a positive result, with metadata and verification info
        DecryptVerifyResult result = new DecryptVerifyResult(DecryptVerifyResult.RESULT_OK, log);

        result.setCachedCryptoInputParcel(cryptoInput);
        result.setSignatureResult(signatureChecker.getSignatureResult());
        result.setDecryptionResult(decryptionResultBuilder.build());
        result.setDecryptionMetadata(metadata);

        return result;

    }

    private EncryptStreamResult handleEncryptedPacket(PgpDecryptVerifyInputParcel input, CryptoInputParcel cryptoInput,
            PGPEncryptedDataList enc, OperationLog log, int indent, int currentProgress, boolean useBackupCode) throws PGPException {

        EncryptStreamResult result = new EncryptStreamResult();

        boolean asymmetricPacketFound = false;
        boolean symmetricPacketFound = false;
        boolean anyPacketFound = false;

        PGPPublicKeyEncryptedData encryptedDataAsymmetric = null;
        PGPPBEEncryptedData encryptedDataSymmetric = null;
        CanonicalizedSecretKey secretEncryptionKey = null;

        Passphrase passphrase = null;

        Iterator<?> it = enc.getEncryptedDataObjects();

        // go through all objects and find one we can decrypt
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof PGPPublicKeyEncryptedData) {
                anyPacketFound = true;

                currentProgress += 2;
                updateProgress(R.string.progress_finding_key, currentProgress, 100);

                PGPPublicKeyEncryptedData encData = (PGPPublicKeyEncryptedData) obj;
                long subKeyId = encData.getKeyID();

                log.add(LogType.MSG_DC_ASYM, indent,
                        KeyFormattingUtils.convertKeyIdToHex(subKeyId));

                CanonicalizedSecretKeyRing secretKeyRing;
                try {
                    // get actual keyring object based on master key id
                    secretKeyRing = mProviderHelper.getCanonicalizedSecretKeyRing(
                            KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(subKeyId)
                    );
                } catch (ProviderHelper.NotFoundException e) {
                    // continue with the next packet in the while loop
                    log.add(LogType.MSG_DC_ASKIP_NO_KEY, indent + 1);
                    continue;
                }

                // allow only specific keys for decryption?
                if (input.getAllowedKeyIds() != null) {
                    long masterKeyId = secretKeyRing.getMasterKeyId();
                    Log.d(Constants.TAG, "encData.getKeyID(): " + subKeyId);
                    Log.d(Constants.TAG, "mAllowedKeyIds: " + input.getAllowedKeyIds());
                    Log.d(Constants.TAG, "masterKeyId: " + masterKeyId);

                    if (!input.getAllowedKeyIds().contains(masterKeyId)) {
                        // this key is in our db, but NOT allowed!
                        // continue with the next packet in the while loop
                        result.skippedDisallowedKey = true;
                        log.add(LogType.MSG_DC_ASKIP_NOT_ALLOWED, indent + 1);
                        continue;
                    }
                }

                // get subkey which has been used for this encryption packet
                secretEncryptionKey = secretKeyRing.getSecretKey(subKeyId);

                /* secret key exists in database and is allowed! */
                asymmetricPacketFound = true;

                encryptedDataAsymmetric = encData;

                if (secretEncryptionKey.getSecretKeyType() == SecretKeyType.DIVERT_TO_CARD) {
                    passphrase = null;
                } else if (cryptoInput.hasPassphrase()) {
                    passphrase = cryptoInput.getPassphrase();
                } else {
                    // if no passphrase was explicitly set try to get it from the cache service
                    try {
                        // returns "" if key has no passphrase
                        passphrase = getCachedPassphrase(subKeyId);
                        log.add(LogType.MSG_DC_PASS_CACHED, indent + 1);
                    } catch (PassphraseCacheInterface.NoSecretKeyException e) {
                        log.add(LogType.MSG_DC_ERROR_NO_KEY, indent + 1);
                        return result.with(new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log));
                    }

                    // if passphrase was not cached, return here indicating that a passphrase is missing!
                    if (passphrase == null) {
                        log.add(LogType.MSG_DC_PENDING_PASSPHRASE, indent + 1);
                        return result.with(new DecryptVerifyResult(log,
                                RequiredInputParcel.createRequiredDecryptPassphrase(
                                        secretKeyRing.getMasterKeyId(), secretEncryptionKey.getKeyId()),
                                cryptoInput));
                    }
                }

                // check for insecure encryption key
                if ( ! PgpSecurityConstants.isSecureKey(secretEncryptionKey)) {
                    log.add(LogType.MSG_DC_INSECURE_KEY, indent + 1);
                    result.insecureEncryptionKey = true;
                }

                // break out of while, only decrypt the first packet where we have a key
                break;

            } else if (obj instanceof PGPPBEEncryptedData) {
                anyPacketFound = true;

                log.add(LogType.MSG_DC_SYM, indent);

                if (!input.isAllowSymmetricDecryption()) {
                    log.add(LogType.MSG_DC_SYM_SKIP, indent + 1);
                    continue;
                }

                /*
                 * When mAllowSymmetricDecryption == true and we find a data packet here,
                 * we do not search for other available asymmetric packets!
                 */
                symmetricPacketFound = true;

                encryptedDataSymmetric = (PGPPBEEncryptedData) obj;

                // if no passphrase is given, return here
                // indicating that a passphrase is missing!
                if (!cryptoInput.hasPassphrase()) {

                    try {
                        passphrase = getCachedPassphrase(key.symmetric);
                        log.add(LogType.MSG_DC_PASS_CACHED, indent + 1);
                    } catch (PassphraseCacheInterface.NoSecretKeyException e) {
                        // nvm
                    }

                    if (passphrase == null) {
                        log.add(LogType.MSG_DC_PENDING_PASSPHRASE, indent + 1);
                        RequiredInputParcel requiredInputParcel = useBackupCode ?
                                RequiredInputParcel.createRequiredBackupCode() :
                                RequiredInputParcel.createRequiredSymmetricPassphrase();
                        return result.with(new DecryptVerifyResult(log,
                                requiredInputParcel,
                                cryptoInput));
                    }

                } else {
                    passphrase = cryptoInput.getPassphrase();
                }

                // break out of while, only decrypt the first packet
                break;
            }
        }

        // More data, just acknowledge and ignore.
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof PGPPublicKeyEncryptedData) {
                PGPPublicKeyEncryptedData encData = (PGPPublicKeyEncryptedData) obj;
                long subKeyId = encData.getKeyID();
                log.add(LogType.MSG_DC_TRAIL_ASYM, indent,
                        KeyFormattingUtils.convertKeyIdToHex(subKeyId));
            } else if (obj instanceof PGPPBEEncryptedData) {
                log.add(LogType.MSG_DC_TRAIL_SYM, indent);
            } else {
                log.add(LogType.MSG_DC_TRAIL_UNKNOWN, indent);
            }
        }

        // we made sure above one of these two would be true
        if (symmetricPacketFound) {
            currentProgress += 2;
            updateProgress(R.string.progress_preparing_streams, currentProgress, 100);

            PGPDigestCalculatorProvider digestCalcProvider = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build();
            PBEDataDecryptorFactory decryptorFactory = new JcePBEDataDecryptorFactoryBuilder(
                    digestCalcProvider).setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(
                    passphrase.getCharArray());

            try {
                result.cleartextStream = encryptedDataSymmetric.getDataStream(decryptorFactory);
            } catch (PGPDataValidationException e) {
                log.add(LogType.MSG_DC_ERROR_SYM_PASSPHRASE, indent + 1);
                RequiredInputParcel requiredInputParcel = useBackupCode ?
                        RequiredInputParcel.createRequiredBackupCode() :
                        RequiredInputParcel.createRequiredSymmetricPassphrase();
                return result.with(new DecryptVerifyResult(log, requiredInputParcel, cryptoInput));
            }

            result.encryptedData = encryptedDataSymmetric;

            result.symmetricEncryptionAlgo = encryptedDataSymmetric.getSymmetricAlgorithm(decryptorFactory);
        } else if (asymmetricPacketFound) {
            currentProgress += 2;
            updateProgress(R.string.progress_extracting_key, currentProgress, 100);

            try {
                log.add(LogType.MSG_DC_UNLOCKING, indent + 1);
                if (!secretEncryptionKey.unlock(passphrase)) {
                    log.add(LogType.MSG_DC_ERROR_BAD_PASSPHRASE, indent + 1);
                    return result.with(new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log));
                }
            } catch (PgpGeneralException e) {
                log.add(LogType.MSG_DC_ERROR_EXTRACT_KEY, indent + 1);
                return result.with(new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log));
            }

            currentProgress += 2;
            updateProgress(R.string.progress_preparing_streams, currentProgress, 100);

            CachingDataDecryptorFactory decryptorFactory
                    = secretEncryptionKey.getCachingDecryptorFactory(cryptoInput);

            // special case: if the decryptor does not have a session key cached for this encrypted
            // data, and can't actually decrypt on its own, return a pending intent
            if (!decryptorFactory.canDecrypt()
                    && !decryptorFactory.hasCachedSessionData(encryptedDataAsymmetric)) {

                log.add(LogType.MSG_DC_PENDING_NFC, indent + 1);
                return result.with(new DecryptVerifyResult(log, RequiredInputParcel.createNfcDecryptOperation(
                        secretEncryptionKey.getRing().getMasterKeyId(),
                        secretEncryptionKey.getKeyId(), encryptedDataAsymmetric.getSessionKey()[0]
                ), cryptoInput));

            }

            try {
                result.cleartextStream = encryptedDataAsymmetric.getDataStream(decryptorFactory);
            } catch (PGPKeyValidationException | ArrayIndexOutOfBoundsException e) {
                log.add(LogType.MSG_DC_ERROR_CORRUPT_DATA, indent + 1);
                return result.with(new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log));
            }

            result.symmetricEncryptionAlgo = encryptedDataAsymmetric.getSymmetricAlgorithm(decryptorFactory);
            result.encryptedData = encryptedDataAsymmetric;

            cryptoInput.addCryptoData(decryptorFactory.getCachedSessionKeys());

        } else {
            // there wasn't even any useful data
            if (!anyPacketFound) {
                log.add(LogType.MSG_DC_ERROR_NO_DATA, indent + 1);
                return result.with(new DecryptVerifyResult(DecryptVerifyResult.RESULT_NO_DATA, log));
            }
            // there was data but key wasn't allowed
            if (result.skippedDisallowedKey) {
                log.add(LogType.MSG_DC_ERROR_NO_KEY, indent + 1);
                return result.with(new DecryptVerifyResult(DecryptVerifyResult.RESULT_KEY_DISALLOWED, log));
            }
            // no packet has been found where we have the corresponding secret key in our db
            log.add(LogType.MSG_DC_ERROR_NO_KEY, indent + 1);
            return result.with(new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log));
        }

        return result;

    }

    /**
     * This method verifies cleartext signatures
     * as defined in http://tools.ietf.org/html/rfc4880#section-7
     * <p/>
     * The method is heavily based on
     * pg/src/main/java/org/spongycastle/openpgp/examples/ClearSignedFileProcessor.java
     */
    @NonNull
    private DecryptVerifyResult verifyCleartextSignature(
            ArmoredInputStream aIn, OutputStream outputStream, int indent) throws IOException, PGPException {

        OperationLog log = new OperationLog();

        byte[] clearText;
        { // read cleartext
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            updateProgress(R.string.progress_reading_data, 0, 100);

            ByteArrayOutputStream lineOut = new ByteArrayOutputStream();
            int lookAhead = readInputLine(lineOut, aIn);
            byte[] lineSep = getLineSeparator();

            byte[] line = lineOut.toByteArray();
            out.write(line, 0, getLengthWithoutSeparator(line));
            out.write(lineSep);

            while (lookAhead != -1 && aIn.isClearText()) {
                lookAhead = readInputLine(lineOut, lookAhead, aIn);
                line = lineOut.toByteArray();
                out.write(line, 0, getLengthWithoutSeparator(line));
                out.write(lineSep);
            }

            out.close();
            clearText = out.toByteArray();
        }

        if (outputStream != null) {
            outputStream.write(clearText);
            outputStream.close();
        }

        updateProgress(R.string.progress_processing_signature, 60, 100);
        JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(aIn);

        PgpSignatureChecker signatureChecker = new PgpSignatureChecker(mProviderHelper);

        Object o = pgpFact.nextObject();
        if (!signatureChecker.initializeSignature(o, log, indent+1)) {
            log.add(LogType.MSG_DC_ERROR_INVALID_DATA, 0);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }

        if (signatureChecker.isInitialized()) {
            try {
                updateProgress(R.string.progress_verifying_signature, 90, 100);

                signatureChecker.updateSignatureWithCleartext(clearText);
                signatureChecker.verifySignature(log, indent);

            } catch (SignatureException e) {
                Log.d(Constants.TAG, "SignatureException", e);
                return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
            }
        }

        updateProgress(R.string.progress_done, 100, 100);

        log.add(LogType.MSG_DC_OK, indent);

        OpenPgpMetadata metadata = new OpenPgpMetadata(
                "",
                "text/plain",
                -1,
                clearText.length);

        DecryptVerifyResult result = new DecryptVerifyResult(DecryptVerifyResult.RESULT_OK, log);
        result.setSignatureResult(signatureChecker.getSignatureResult());
        result.setDecryptionResult(
                new OpenPgpDecryptionResult(OpenPgpDecryptionResult.RESULT_NOT_ENCRYPTED));
        result.setDecryptionMetadata(metadata);
        return result;
    }

    @NonNull
    private DecryptVerifyResult verifyDetachedSignature(
            PgpDecryptVerifyInputParcel input, InputData inputData, OutputStream out, int indent)
            throws IOException, PGPException {

        OperationLog log = new OperationLog();

        updateProgress(R.string.progress_processing_signature, 0, 100);
        InputStream detachedSigIn = new ByteArrayInputStream(input.getDetachedSignature());
        detachedSigIn = PGPUtil.getDecoderStream(detachedSigIn);

        JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(detachedSigIn);

        Object o = pgpFact.nextObject();
        if (o instanceof PGPCompressedData) {
            PGPCompressedData c1 = (PGPCompressedData) o;
            pgpFact = new JcaPGPObjectFactory(c1.getDataStream());
            o = pgpFact.nextObject();
        }

        PgpSignatureChecker signatureChecker = new PgpSignatureChecker(mProviderHelper);

        if ( ! signatureChecker.initializeSignature(o, log, indent+1)) {
            log.add(LogType.MSG_DC_ERROR_INVALID_DATA, 0);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }

        if (signatureChecker.isInitialized()) {

            updateProgress(R.string.progress_reading_data, 60, 100);

            ProgressScaler progressScaler = new ProgressScaler(mProgressable, 60, 90, 100);
            long alreadyWritten = 0;
            long wholeSize = inputData.getSize() - inputData.getStreamPosition();
            int length;
            byte[] buffer = new byte[1 << 16];
            InputStream in = inputData.getInputStream();
            while ((length = in.read(buffer)) > 0) {
                if (out != null) {
                    out.write(buffer, 0, length);
                }

                // update signature buffer if signature is also present
                signatureChecker.updateSignatureData(buffer, 0, length);

                alreadyWritten += length;
                if (wholeSize > 0) {
                    long progress = 100 * alreadyWritten / wholeSize;
                    // stop at 100% for wrong file sizes...
                    if (progress > 100) {
                        progress = 100;
                    }
                    progressScaler.setProgress((int) progress, 100);
                }
            }

            updateProgress(R.string.progress_verifying_signature, 90, 100);
            log.add(LogType.MSG_DC_CLEAR_SIGNATURE_CHECK, indent);

            signatureChecker.verifySignature(log, indent);

        }

        updateProgress(R.string.progress_done, 100, 100);

        log.add(LogType.MSG_DC_OK, indent);

        // TODO return metadata object?

        DecryptVerifyResult result = new DecryptVerifyResult(DecryptVerifyResult.RESULT_OK, log);
        result.setSignatureResult(signatureChecker.getSignatureResult());
        result.setDecryptionResult(
                new OpenPgpDecryptionResult(OpenPgpDecryptionResult.RESULT_NOT_ENCRYPTED));
        return result;
    }

    private static int readInputLine(ByteArrayOutputStream bOut, InputStream fIn)
            throws IOException {
        bOut.reset();

        int lookAhead = -1;
        int ch;

        while ((ch = fIn.read()) >= 0) {
            bOut.write(ch);
            if (ch == '\r' || ch == '\n') {
                lookAhead = readPastEOL(bOut, ch, fIn);
                break;
            }
        }

        return lookAhead;
    }

    private static int readInputLine(ByteArrayOutputStream bOut, int lookAhead, InputStream fIn)
            throws IOException {
        bOut.reset();

        int ch = lookAhead;

        do {
            bOut.write(ch);
            if (ch == '\r' || ch == '\n') {
                lookAhead = readPastEOL(bOut, ch, fIn);
                break;
            }
        } while ((ch = fIn.read()) >= 0);

        if (ch < 0) {
            lookAhead = -1;
        }

        return lookAhead;
    }

    private static int readPastEOL(ByteArrayOutputStream bOut, int lastCh, InputStream fIn)
            throws IOException {
        int lookAhead = fIn.read();

        if (lastCh == '\r' && lookAhead == '\n') {
            bOut.write(lookAhead);
            lookAhead = fIn.read();
        }

        return lookAhead;
    }

    private static int getLengthWithoutSeparator(byte[] line) {
        int end = line.length - 1;

        while (end >= 0 && isLineEnding(line[end])) {
            end--;
        }

        return end + 1;
    }

    private static boolean isLineEnding(byte b) {
        return b == '\r' || b == '\n';
    }

    private static byte[] getLineSeparator() {
        String nl = System.getProperty("line.separator");
        return nl.getBytes();
    }

    /// Convenience method - Trivially checks if a byte array matches the bytes of a plain text string
    // Assumes data.length >= needle.length()
    static boolean matchesPrefix(byte[] data, String needle) {
        byte[] needleBytes = needle.getBytes();
        for (int i = 0; i < needle.length(); i++) {
            if (data[i] != needleBytes[i]) {
                return false;
            }
        }
        return true;
    }

}
