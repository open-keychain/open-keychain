/*
 * Copyright (C) 2012-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.NfcSyncPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.PGPUtil;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.BaseOperation;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.PgpSignEncryptResult;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing.SecretKeyRingType;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderReader;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class supports a single, low-level, sign/encrypt operation.
 * <p/>
 * The operation of this class takes an Input- and OutputStream plus a
 * PgpSignEncryptInput, and signs and/or encrypts the stream as
 * parametrized in the PgpSignEncryptInput object. It returns its status
 * and a possible detached signature as a SignEncryptResult.
 * <p/>
 * For a high-level operation based on URIs, see SignEncryptOperation.
 *
 * @see PgpSignEncryptInputParcel
 * @see org.sufficientlysecure.keychain.operations.results.PgpSignEncryptResult
 * @see org.sufficientlysecure.keychain.operations.SignEncryptOperation
 */
public class PgpSignEncryptOperation extends BaseOperation<PgpSignEncryptInputParcel> {

    private static byte[] NEW_LINE;

    static {
        try {
            NEW_LINE = "\r\n".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(Constants.TAG, "UnsupportedEncodingException", e);
        }
    }

    public PgpSignEncryptOperation(Context context, ProviderHelper providerHelper, Progressable progressable, AtomicBoolean cancelled) {
        super(context, providerHelper, progressable, cancelled);
    }

    public PgpSignEncryptOperation(Context context, ProviderHelper providerHelper, Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    @NonNull
    @Override
    public PgpSignEncryptResult execute(PgpSignEncryptInputParcel input, CryptoInputParcel cryptoInput) {
        OperationLog log = new OperationLog();

        InputData inputData;
        {
            if (input.getInputBytes() != null) {
                log.add(LogType.MSG_PSE_INPUT_BYTES, 1);
                InputStream is = new ByteArrayInputStream(input.getInputBytes());
                inputData = new InputData(is, input.getInputBytes().length);
            } else {
                log.add(LogType.MSG_PSE_INPUT_URI, 1);
                Uri uri = input.getInputUri();
                try {
                    InputStream is = FileHelper.openInputStreamSafe(mContext.getContentResolver(), uri);
                    long fileSize = FileHelper.getFileSize(mContext, uri, 0);
                    String filename = FileHelper.getFilename(mContext, uri);
                    inputData = new InputData(is, fileSize, filename);
                } catch (FileNotFoundException e) {
                    log.add(LogType.MSG_PSE_ERROR_INPUT_URI_NOT_FOUND, 1);
                    return new PgpSignEncryptResult(SignEncryptResult.RESULT_ERROR, log);
                }
            }
        }

        OutputStream outStream;
        {
            if (input.getOutputUri() != null) {
                try {
                    Uri outputUri = input.getOutputUri();
                    outStream = mContext.getContentResolver().openOutputStream(outputUri);
                } catch (FileNotFoundException e) {
                    log.add(LogType.MSG_PSE_ERROR_OUTPUT_URI_NOT_FOUND, 1);
                    return new PgpSignEncryptResult(SignEncryptResult.RESULT_ERROR, log);
                }
            } else {
                outStream = new ByteArrayOutputStream();
            }
        }

        PgpSignEncryptResult result = executeInternal(input, cryptoInput, inputData, outStream);
        if (outStream instanceof ByteArrayOutputStream) {
            byte[] outputData = ((ByteArrayOutputStream) outStream).toByteArray();
            result.setOutputBytes(outputData);
        }

        return result;
    }

    @NonNull
    public PgpSignEncryptResult execute(PgpSignEncryptInputParcel input, CryptoInputParcel cryptoInput,
                                        InputData inputData, OutputStream outputStream) {
        return executeInternal(input, cryptoInput, inputData, outputStream);
    }

    /**
     * Signs and/or encrypts data based on parameters of class
     */
    private PgpSignEncryptResult executeInternal(PgpSignEncryptInputParcel input, CryptoInputParcel cryptoInput,
                                                 InputData inputData, OutputStream outputStream) {

        int indent = 0;
        OperationLog log = new OperationLog();

        log.add(LogType.MSG_PSE, indent);
        indent += 1;

        PgpSignEncryptData data = input.getData();
        boolean enableSignature = data.getSignatureMasterKeyId() != Constants.key.none;
        boolean enableEncryption = ((data.getEncryptionMasterKeyIds() != null && data.getEncryptionMasterKeyIds().length > 0)
                || data.getSymmetricPassphrase() != null);
        boolean enableCompression = (data.getCompressionAlgorithm() != CompressionAlgorithmTags.UNCOMPRESSED);

        Log.d(Constants.TAG, "enableSignature:" + enableSignature
                + "\nenableEncryption:" + enableEncryption
                + "\nenableCompression:" + enableCompression
                + "\nenableAsciiArmorOutput:" + data.isEnableAsciiArmorOutput()
                + "\nisHiddenRecipients:" + data.isHiddenRecipients());

        // add additional key id to encryption ids (mostly to do self-encryption)
        if (enableEncryption && data.getAdditionalEncryptId() != Constants.key.none) {
            data.setEncryptionMasterKeyIds(Arrays.copyOf(data.getEncryptionMasterKeyIds(), data.getEncryptionMasterKeyIds().length + 1));
            data.getEncryptionMasterKeyIds()[data.getEncryptionMasterKeyIds().length - 1] = data.getAdditionalEncryptId();
        }

        ArmoredOutputStream armorOut = null;
        OutputStream out;
        if (data.isEnableAsciiArmorOutput()) {
            armorOut = new ArmoredOutputStream(new BufferedOutputStream(outputStream, 1 << 16));
            if (data.getVersionHeader() != null) {
                armorOut.setHeader("Version", data.getVersionHeader());
            }
            // if we have a charset, put it in the header
            if (data.getCharset() != null) {
                armorOut.setHeader("Charset", data.getCharset());
            }
            // add proprietary header to indicate that this is a key backup
            if (data.isAddBackupHeader()) {
                armorOut.setHeader("BackupVersion", "2");
            }
            out = armorOut;
        } else {
            out = outputStream;
        }

        /* Get keys for signature generation for later usage */
        CanonicalizedSecretKey signingKey = null;
        if (enableSignature) {

            updateProgress(R.string.progress_extracting_signature_key, 0, 100);

            try {
                long signingMasterKeyId = data.getSignatureMasterKeyId();
                long signingSubKeyId = data.getSignatureSubKeyId();

                CachedPublicKeyRing cachedPublicKeyRing =
                        mProviderHelper.read().getCachedPublicKeyRing(signingMasterKeyId);

                SecretKeyRingType secretKeyRingType = cachedPublicKeyRing.getSecretKeyringType();

                // get keyring passphrase
                Passphrase keyringPassphrase;
                switch (secretKeyRingType) {
                    case PASSPHRASE_EMPTY: {
                        keyringPassphrase = new Passphrase();
                        break;
                    }
                    case PASSPHRASE: {
                        keyringPassphrase = cryptoInput.getPassphrase();
                        if (keyringPassphrase == null) {
                            try {
                                keyringPassphrase = getCachedPassphrase(signingMasterKeyId);
                            } catch (NoSecretKeyException ignored) {
                                // treat as a cache miss for error handling purposes
                            }
                            if (keyringPassphrase == null) {
                                log.add(LogType.MSG_PSE_PENDING_PASSPHRASE, indent + 1);
                                return new PgpSignEncryptResult(log,
                                        RequiredInputParcel.createRequiredKeyringPassphrase(signingMasterKeyId),
                                        cryptoInput);
                            }
                        }
                        break;
                    }
                    default: {
                        throw new AssertionError("Unsupported keyring type");
                    }
                }

                CanonicalizedSecretKeyRing signingKeyRing =
                        mProviderHelper.read().getCanonicalizedSecretKeyRingWithMerge(
                                signingMasterKeyId, keyringPassphrase);
                signingKey = signingKeyRing.getSecretKey(data.getSignatureSubKeyId());

                // Make sure key is not expired or revoked
                if (signingKeyRing.isExpired() || signingKeyRing.isRevoked()
                        || signingKey.isExpired() || signingKey.isRevoked()) {
                    log.add(LogType.MSG_PSE_ERROR_REVOKED_OR_EXPIRED, indent);
                    return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
                }

                // Make sure we are allowed to sign here!
                if (!signingKey.canSign()) {
                    log.add(LogType.MSG_PSE_ERROR_KEY_SIGN, indent);
                    return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
                }

                switch (cachedPublicKeyRing.getSecretKeyType(signingSubKeyId)) {
                    case DIVERT_TO_CARD:
                    case PASSPHRASE_EMPTY: {
                        if (!signingKey.unlock(new Passphrase())) {
                            throw new AssertionError(
                                    "PASSPHRASE_EMPTY/DIVERT_TO_CARD "
                                            + "keyphrase not unlocked with empty passphrase."
                                            + " This is a programming error!");
                        }
                        break;
                    }
                    default: {
                        // other key types should not reach this point
                        log.add(LogType.MSG_PSE_ERROR_UNLOCK_SUBKEY, indent);
                        return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
                    }

                }

            } catch (ProviderReader.NotFoundException e) {
                log.add(LogType.MSG_PSE_ERROR_SIGN_KEY, indent);
                return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
            } catch (PgpGeneralException e) {
                log.add(LogType.MSG_PSE_ERROR_UNLOCK_SUBKEY, indent);
                return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
            } catch (ByteArrayEncryptor.IncorrectPassphraseException e) {
                log.add(LogType.MSG_PSE_ERROR_UNLOCK_KEYRING, indent);
                return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
            } catch (ByteArrayEncryptor.EncryptDecryptException e) {
                log.add(LogType.MSG_PSE_ERROR_DECRYPT_KEYRING, indent);
                return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
            } catch (ProviderReader.FailedMergeException e) {
                log.add(LogType.MSG_PSE_ERROR_MERGE_KEYRING, indent);
                return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
            }

            // Use requested hash algo
            int requestedAlgorithm = data.getSignatureHashAlgorithm();
            if (requestedAlgorithm == PgpSecurityConstants.OpenKeychainHashAlgorithmTags.USE_DEFAULT) {
                data.setSignatureHashAlgorithm(PgpSecurityConstants.DEFAULT_HASH_ALGORITHM);
            }
        }
        updateProgress(R.string.progress_preparing_streams, 2, 100);

        /* Initialize PGPEncryptedDataGenerator for later usage */
        PGPEncryptedDataGenerator cPk = null;
        if (enableEncryption) {

            // Use requested encryption algo
            int algo = data.getSymmetricEncryptionAlgorithm();
            if (algo == PgpSecurityConstants.OpenKeychainSymmetricKeyAlgorithmTags.USE_DEFAULT) {
                algo = PgpSecurityConstants.DEFAULT_SYMMETRIC_ALGORITHM;
            }
            JcePGPDataEncryptorBuilder encryptorBuilder =
                    new JcePGPDataEncryptorBuilder(algo)
                            .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME)
                            .setWithIntegrityPacket(data.isIntegrityProtected());

            cPk = new PGPEncryptedDataGenerator(encryptorBuilder);

            if (data.getSymmetricPassphrase() != null) {
                // Symmetric encryption
                log.add(LogType.MSG_PSE_SYMMETRIC, indent);

                JcePBEKeyEncryptionMethodGenerator symmetricEncryptionGenerator =
                        new JcePBEKeyEncryptionMethodGenerator(data.getSymmetricPassphrase().getCharArray());
                cPk.addMethod(symmetricEncryptionGenerator);
            } else {
                log.add(LogType.MSG_PSE_ASYMMETRIC, indent);

                // Asymmetric encryption
                for (long id : data.getEncryptionMasterKeyIds()) {
                    try {
                        CanonicalizedPublicKeyRing keyRing = mProviderHelper.read().getCanonicalizedPublicKeyRing(
                                KeyRings.buildUnifiedKeyRingUri(id));
                        Set<Long> encryptSubKeyIds = keyRing.getEncryptIds();
                        for (Long subKeyId : encryptSubKeyIds) {
                            CanonicalizedPublicKey key = keyRing.getPublicKey(subKeyId);
                            cPk.addMethod(key.getPubKeyEncryptionGenerator(data.isHiddenRecipients()));
                            log.add(LogType.MSG_PSE_KEY_OK, indent + 1,
                                    KeyFormattingUtils.convertKeyIdToHex(subKeyId));
                        }
                        if (encryptSubKeyIds.isEmpty()) {
                            log.add(LogType.MSG_PSE_KEY_WARN, indent + 1,
                                    KeyFormattingUtils.convertKeyIdToHex(id));
                            return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
                        }
                        // Make sure key is not expired or revoked
                        if (keyRing.isExpired() || keyRing.isRevoked()) {
                            log.add(LogType.MSG_PSE_ERROR_REVOKED_OR_EXPIRED, indent);
                            return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
                        }
                    } catch (ProviderReader.NotFoundException e) {
                        log.add(LogType.MSG_PSE_KEY_UNKNOWN, indent + 1,
                                KeyFormattingUtils.convertKeyIdToHex(id));
                        return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
                    }
                }
            }
        }

        /* Initialize signature generator object for later usage */
        PGPSignatureGenerator signatureGenerator = null;
        if (enableSignature) {
            updateProgress(R.string.progress_preparing_signature, 4, 100);

            try {
                boolean cleartext = data.isCleartextSignature() && data.isEnableAsciiArmorOutput() && !enableEncryption;
                signatureGenerator = signingKey.getDataSignatureGenerator(
                        data.getSignatureHashAlgorithm(), cleartext,
                        cryptoInput.getCryptoData(), cryptoInput.getSignatureTime());
            } catch (PgpGeneralException e) {
                log.add(LogType.MSG_PSE_ERROR_NFC, indent);
                return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
            }
        }

        ProgressScaler progressScaler =
                new ProgressScaler(mProgressable, 8, 95, 100);
        PGPCompressedDataGenerator compressGen = null;
        OutputStream pOut;
        OutputStream encryptionOut = null;
        BCPGOutputStream bcpgOut;

        ByteArrayOutputStream detachedByteOut = null;
        ArmoredOutputStream detachedArmorOut = null;
        BCPGOutputStream detachedBcpgOut = null;

        long opTime, startTime = System.currentTimeMillis();

        try {

            if (enableEncryption) {
                /* actual encryption */
                updateProgress(R.string.progress_encrypting, 8, 100);
                log.add(enableSignature
                                ? LogType.MSG_PSE_SIGCRYPTING
                                : LogType.MSG_PSE_ENCRYPTING,
                        indent
                );
                indent += 1;

                encryptionOut = cPk.open(out, new byte[1 << 16]);

                if (enableCompression) {
                    log.add(LogType.MSG_PSE_COMPRESSING, indent);

                    // Use preferred compression algo
                    int algo = data.getCompressionAlgorithm();
                    if (algo == PgpSecurityConstants.OpenKeychainCompressionAlgorithmTags.USE_DEFAULT) {
                        algo = PgpSecurityConstants.DEFAULT_COMPRESSION_ALGORITHM;
                    }
                    compressGen = new PGPCompressedDataGenerator(algo);
                    bcpgOut = new BCPGOutputStream(compressGen.open(encryptionOut));
                } else {
                    bcpgOut = new BCPGOutputStream(encryptionOut);
                }

                if (enableSignature) {
                    signatureGenerator.generateOnePassVersion(false).encode(bcpgOut);
                }

                PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
                char literalDataFormatTag;
                if (data.isCleartextSignature()) {
                    literalDataFormatTag = PGPLiteralData.UTF8;
                } else {
                    literalDataFormatTag = PGPLiteralData.BINARY;
                }
                pOut = literalGen.open(bcpgOut, literalDataFormatTag,
                        inputData.getOriginalFilename(), new Date(), new byte[1 << 16]);

                long alreadyWritten = 0;
                int length;
                byte[] buffer = new byte[1 << 16];
                InputStream in = new BufferedInputStream(inputData.getInputStream());
                while ((length = in.read(buffer)) > 0) {
                    pOut.write(buffer, 0, length);

                    // update signature buffer if signature is requested
                    if (enableSignature) {
                        signatureGenerator.update(buffer, 0, length);
                    }

                    alreadyWritten += length;
                    if (inputData.getSize() > 0) {
                        long progress = 100 * alreadyWritten / inputData.getSize();
                        progressScaler.setProgress((int) progress, 100);
                    }
                }

                literalGen.close();
                indent -= 1;

            } else if (enableSignature && data.isCleartextSignature() && data.isEnableAsciiArmorOutput()) {
                /* cleartext signature: sign-only of ascii text */

                updateProgress(R.string.progress_signing, 8, 100);
                log.add(LogType.MSG_PSE_SIGNING_CLEARTEXT, indent);

                // write -----BEGIN PGP SIGNED MESSAGE-----
                armorOut.beginClearText(data.getSignatureHashAlgorithm());

                InputStream in = new BufferedInputStream(inputData.getInputStream());
                final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                // update signature buffer with first line
                processLine(reader.readLine(), armorOut, signatureGenerator);

                // TODO: progress: fake annealing?
                while (true) {
                    String line = reader.readLine();

                    // end cleartext signature with newline, see http://tools.ietf.org/html/rfc4880#section-7
                    if (line == null) {
                        armorOut.write(NEW_LINE);
                        break;
                    }

                    armorOut.write(NEW_LINE);

                    // update signature buffer with input line
                    signatureGenerator.update(NEW_LINE);
                    processLine(line, armorOut, signatureGenerator);
                }

                armorOut.endClearText();

                pOut = new BCPGOutputStream(armorOut);
            } else if (enableSignature && data.isDetachedSignature()) {
                /* detached signature */

                updateProgress(R.string.progress_signing, 8, 100);
                log.add(LogType.MSG_PSE_SIGNING_DETACHED, indent);

                InputStream in = new BufferedInputStream(inputData.getInputStream());

                // handle output stream separately for detached signatures
                detachedByteOut = new ByteArrayOutputStream();
                OutputStream detachedOut = detachedByteOut;
                if (data.isEnableAsciiArmorOutput()) {
                    detachedArmorOut = new ArmoredOutputStream(new BufferedOutputStream(detachedOut, 1 << 16));
                    if (data.getVersionHeader() != null) {
                        detachedArmorOut.setHeader("Version", data.getVersionHeader());
                    }

                    detachedOut = detachedArmorOut;
                }
                detachedBcpgOut = new BCPGOutputStream(detachedOut);

                long alreadyWritten = 0;
                int length;
                byte[] buffer = new byte[1 << 16];
                while ((length = in.read(buffer)) > 0) {
                    // no output stream is written, no changed to original data!

                    signatureGenerator.update(buffer, 0, length);

                    alreadyWritten += length;
                    if (inputData.getSize() > 0) {
                        long progress = 100 * alreadyWritten / inputData.getSize();
                        progressScaler.setProgress((int) progress, 100);
                    }
                }

                pOut = null;
            } else if (enableSignature && !data.isCleartextSignature() && !data.isDetachedSignature()) {
                /* sign-only binary (files/data stream) */

                updateProgress(R.string.progress_signing, 8, 100);
                log.add(LogType.MSG_PSE_SIGNING, indent);

                InputStream in = new BufferedInputStream(inputData.getInputStream());

                if (enableCompression) {
                    // Use preferred compression algo
                    int algo = data.getCompressionAlgorithm();
                    if (algo == PgpSecurityConstants.OpenKeychainCompressionAlgorithmTags.USE_DEFAULT) {
                        algo = PgpSecurityConstants.DEFAULT_COMPRESSION_ALGORITHM;
                    }

                    compressGen = new PGPCompressedDataGenerator(algo);
                    bcpgOut = new BCPGOutputStream(compressGen.open(out));
                } else {
                    bcpgOut = new BCPGOutputStream(out);
                }

                signatureGenerator.generateOnePassVersion(false).encode(bcpgOut);

                PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
                pOut = literalGen.open(bcpgOut, PGPLiteralData.BINARY,
                        inputData.getOriginalFilename(), new Date(),
                        new byte[1 << 16]);

                long alreadyWritten = 0;
                int length;
                byte[] buffer = new byte[1 << 16];
                while ((length = in.read(buffer)) > 0) {
                    pOut.write(buffer, 0, length);

                    signatureGenerator.update(buffer, 0, length);

                    alreadyWritten += length;
                    if (inputData.getSize() > 0) {
                        long progress = 100 * alreadyWritten / inputData.getSize();
                        progressScaler.setProgress((int) progress, 100);
                    }
                }

                literalGen.close();
            } else {
                throw new AssertionError("cannot clearsign in non-ascii armored text, this is a bug!");
            }

            if (enableSignature) {
                updateProgress(R.string.progress_generating_signature, 95, 100);
                try {
                    if (detachedBcpgOut != null) {
                        signatureGenerator.generate().encode(detachedBcpgOut);
                    } else {
                        signatureGenerator.generate().encode(pOut);
                    }
                } catch (NfcSyncPGPContentSignerBuilder.NfcInteractionNeeded e) {
                    // this secret key diverts to a OpenPGP card, throw exception with hash that will be signed
                    log.add(LogType.MSG_PSE_PENDING_NFC, indent);
                    return new PgpSignEncryptResult(log, RequiredInputParcel.createSecurityTokenSignOperation(
                            signingKey.getRing().getMasterKeyId(), signingKey.getKeyId(),
                            e.hashToSign, e.hashAlgo, cryptoInput.getSignatureTime()), cryptoInput);
                }
            }

            opTime = System.currentTimeMillis() - startTime;
            Log.d(Constants.TAG, "sign/encrypt time taken: " + String.format("%.2f",
                    opTime / 1000.0) + "s");

            // closing outputs
            // NOTE: closing needs to be done in the correct order!
            if (encryptionOut != null) {
                if (compressGen != null) {
                    compressGen.close();
                }

                encryptionOut.close();
            }
            // Note: Closing ArmoredOutputStream does not close the underlying stream
            if (armorOut != null) {
                armorOut.close();
            }
            // Note: Closing ArmoredOutputStream does not close the underlying stream
            if (detachedArmorOut != null) {
                detachedArmorOut.close();
            }
            // Also closes detachedBcpgOut
            if (detachedByteOut != null) {
                detachedByteOut.close();
            }
            if (out != null) {
                out.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }

        } catch (SignatureException e) {
            log.add(LogType.MSG_PSE_ERROR_SIG, indent);
            return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
        } catch (PGPException e) {
            log.add(LogType.MSG_PSE_ERROR_PGP, indent);
            return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
        } catch (IOException e) {
            log.add(LogType.MSG_PSE_ERROR_IO, indent);
            return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
        }

        updateProgress(R.string.progress_done, 100, 100);

        log.add(LogType.MSG_PSE_OK, indent);
        PgpSignEncryptResult result = new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_OK, log);
        result.mOperationTime = opTime;
        if (detachedByteOut != null) {
            try {
                detachedByteOut.flush();
                detachedByteOut.close();
            } catch (IOException e) {
                // silently catch
            }
            result.setDetachedSignature(detachedByteOut.toByteArray());
            try {
                String digestName = PGPUtil.getDigestName(data.getSignatureHashAlgorithm());
                // construct micalg parameter according to https://tools.ietf.org/html/rfc3156#section-5
                result.setMicAlgDigestName("pgp-" + digestName.toLowerCase());
            } catch (PGPException e) {
                Log.e(Constants.TAG, "error setting micalg parameter!", e);
            }
        }
        return result;
    }

    /**
     * Remove whitespaces on line endings
     */
    private static void processLine(final String pLine, final ArmoredOutputStream pArmoredOutput,
                                    final PGPSignatureGenerator pSignatureGenerator)
            throws IOException, SignatureException {

        if (pLine == null) {
            return;
        }

        final char[] chars = pLine.toCharArray();
        int len = chars.length;

        while (len > 0) {
            if (!Character.isWhitespace(chars[len - 1])) {
                break;
            }
            len--;
        }

        final byte[] data = pLine.substring(0, len).getBytes("UTF-8");

        if (pArmoredOutput != null) {
            pArmoredOutput.write(data);
        }
        pSignatureGenerator.update(data);
    }

}
