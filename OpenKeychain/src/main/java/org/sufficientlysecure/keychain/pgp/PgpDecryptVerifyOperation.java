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

import android.content.Context;
import android.support.annotation.NonNull;
import android.webkit.MimeTypeMap;

import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpMetadata;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.spongycastle.bcpg.ArmoredInputStream;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPDataValidationException;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyValidationException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPOnePassSignature;
import org.spongycastle.openpgp.PGPOnePassSignatureList;
import org.spongycastle.openpgp.PGPPBEEncryptedData;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.spongycastle.openpgp.operator.PBEDataDecryptorFactory;
import org.spongycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.spongycastle.openpgp.operator.jcajce.CachingDataDecryptorFactory;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.spongycastle.util.encoders.DecoderException;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Constants.key;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.BaseOperation;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SignatureException;
import java.util.Date;
import java.util.Iterator;

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

                    if (input.isSignedLiteralData()) {
                        return verifySignedLiteralData(input, aIn, outputStream, 0);
                    } else if (aIn.isClearText()) {
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

    /**Verify signed plaintext data (PGP/INLINE). */
    @NonNull
    private DecryptVerifyResult verifySignedLiteralData(
            PgpDecryptVerifyInputParcel input, InputStream in, OutputStream out, int indent)
            throws IOException, PGPException {
        OperationLog log = new OperationLog();
        log.add(LogType.MSG_VL, indent);

        // thinking that the proof-fetching operation is going to take most of the time
        updateProgress(R.string.progress_reading_data, 75, 100);

        JcaPGPObjectFactory pgpF = new JcaPGPObjectFactory(in);
        Object o = pgpF.nextObject();
        if (o instanceof PGPCompressedData) {
            log.add(LogType.MSG_DC_CLEAR_DECOMPRESS, indent + 1);

            pgpF = new JcaPGPObjectFactory(((PGPCompressedData) o).getDataStream());
            o = pgpF.nextObject();
            updateProgress(R.string.progress_decompressing_data, 80, 100);
        }

        // all we want to see is a OnePassSignatureList followed by LiteralData
        if (!(o instanceof PGPOnePassSignatureList)) {
            log.add(LogType.MSG_VL_ERROR_MISSING_SIGLIST, indent);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }
        PGPOnePassSignatureList sigList = (PGPOnePassSignatureList) o;

        // go through all signatures (should be just one), make sure we have
        //  the key and it matches the one we’re looking for
        CanonicalizedPublicKeyRing signingRing = null;
        CanonicalizedPublicKey signingKey = null;
        int signatureIndex = -1;
        for (int i = 0; i < sigList.size(); ++i) {
            try {
                long sigKeyId = sigList.get(i).getKeyID();
                signingRing = mProviderHelper.getCanonicalizedPublicKeyRing(
                        KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(sigKeyId)
                );
                signingKey = signingRing.getPublicKey(sigKeyId);
                signatureIndex = i;
            } catch (ProviderHelper.NotFoundException e) {
                Log.d(Constants.TAG, "key not found, trying next signature...");
            }
        }

        // there has to be a key, and it has to be the right one
        if (signingKey == null) {
            log.add(LogType.MSG_VL_ERROR_MISSING_KEY, indent);
            Log.d(Constants.TAG, "Failed to find key in signed-literal message");
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }

        String fingerprint = KeyFormattingUtils.convertFingerprintToHex(signingRing.getFingerprint());
        if (!(input.getRequiredSignerFingerprint().equals(fingerprint))) {
            log.add(LogType.MSG_VL_ERROR_MISSING_KEY, indent);
            Log.d(Constants.TAG, "Fingerprint mismatch; wanted " + input.getRequiredSignerFingerprint() +
                    " got " + fingerprint + "!");
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }

        OpenPgpSignatureResultBuilder signatureResultBuilder = new OpenPgpSignatureResultBuilder();

        PGPOnePassSignature signature = sigList.get(signatureIndex);
        signatureResultBuilder.initValid(signingRing, signingKey);

        JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider =
                new JcaPGPContentVerifierBuilderProvider()
                        .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        signature.init(contentVerifierBuilderProvider, signingKey.getPublicKey());

        o = pgpF.nextObject();

        if (!(o instanceof PGPLiteralData)) {
            log.add(LogType.MSG_VL_ERROR_MISSING_LITERAL, indent);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }

        PGPLiteralData literalData = (PGPLiteralData) o;

        log.add(LogType.MSG_DC_CLEAR_DATA, indent + 1);
        updateProgress(R.string.progress_decrypting, 85, 100);

        InputStream dataIn = literalData.getInputStream();

        int length;
        byte[] buffer = new byte[1 << 16];
        while ((length = dataIn.read(buffer)) > 0) {
            out.write(buffer, 0, length);
            signature.update(buffer, 0, length);
        }

        updateProgress(R.string.progress_verifying_signature, 95, 100);
        log.add(LogType.MSG_VL_CLEAR_SIGNATURE_CHECK, indent + 1);

        PGPSignatureList signatureList = (PGPSignatureList) pgpF.nextObject();
        PGPSignature messageSignature = signatureList.get(signatureIndex);

        // Verify signature and check binding signatures
        boolean validSignature = signature.verify(messageSignature);
        if (validSignature) {
            log.add(LogType.MSG_DC_CLEAR_SIGNATURE_OK, indent + 1);
        } else {
            log.add(LogType.MSG_DC_CLEAR_SIGNATURE_BAD, indent + 1);
        }
        signatureResultBuilder.setValidSignature(validSignature);

        OpenPgpSignatureResult signatureResult = signatureResultBuilder.build();

        if (signatureResult.getResult() != OpenPgpSignatureResult.RESULT_VALID_CONFIRMED
                && signatureResult.getResult() != OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED) {
            log.add(LogType.MSG_VL_ERROR_INTEGRITY_CHECK, indent);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }

        updateProgress(R.string.progress_done, 100, 100);

        log.add(LogType.MSG_VL_OK, indent);

        // Return a positive result, with metadata and verification info
        DecryptVerifyResult result = new DecryptVerifyResult(DecryptVerifyResult.RESULT_OK, log);
        result.setSignatureResult(signatureResult);
        result.setDecryptionResult(
                new OpenPgpDecryptionResult(OpenPgpDecryptionResult.RESULT_NOT_ENCRYPTED));
        return result;
    }


    /** Decrypt and/or verify binary or ascii armored pgp data. */
    @NonNull
    private DecryptVerifyResult decryptVerify(
            PgpDecryptVerifyInputParcel input, CryptoInputParcel cryptoInput,
            InputStream in, OutputStream out, int indent) throws IOException, PGPException {

        OpenPgpSignatureResultBuilder signatureResultBuilder = new OpenPgpSignatureResultBuilder();
        OpenPgpDecryptionResultBuilder decryptionResultBuilder = new OpenPgpDecryptionResultBuilder();
        OperationLog log = new OperationLog();

        log.add(LogType.MSG_DC, indent);
        indent += 1;

        JcaPGPObjectFactory pgpF = new JcaPGPObjectFactory(in);
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject();

        int currentProgress = 0;
        updateProgress(R.string.progress_reading_data, currentProgress, 100);

        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        if (enc == null) {
            log.add(LogType.MSG_DC_ERROR_INVALID_DATA, indent);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }

        InputStream clear;
        PGPEncryptedData encryptedData;

        PGPPublicKeyEncryptedData encryptedDataAsymmetric = null;
        PGPPBEEncryptedData encryptedDataSymmetric = null;
        CanonicalizedSecretKey secretEncryptionKey = null;
        Iterator<?> it = enc.getEncryptedDataObjects();
        boolean asymmetricPacketFound = false;
        boolean symmetricPacketFound = false;
        boolean anyPacketFound = false;

        // If the input stream is armored, and there is a charset specified, take a note for later
        // https://tools.ietf.org/html/rfc4880#page56
        String charset = null;
        if (in instanceof ArmoredInputStream) {
            ArmoredInputStream aIn = (ArmoredInputStream) in;
            if (aIn.getArmorHeaders() != null) {
                for (String header : aIn.getArmorHeaders()) {
                    String[] pieces = header.split(":", 2);
                    if (pieces.length == 2 && "charset".equalsIgnoreCase(pieces[0])) {
                        charset = pieces[1].trim();
                        break;
                    }
                }
                if (charset != null) {
                    log.add(LogType.MSG_DC_CHARSET, indent, charset);
                }
            }
        }

        Passphrase passphrase = null;
        boolean skippedDisallowedKey = false;

        // go through all objects and find one we can decrypt
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof PGPPublicKeyEncryptedData) {
                anyPacketFound = true;

                currentProgress += 2;
                updateProgress(R.string.progress_finding_key, currentProgress, 100);

                PGPPublicKeyEncryptedData encData = (PGPPublicKeyEncryptedData) obj;
                long subKeyId = encData.getKeyID();

                // TODO: currently only tries the first PGPPublicKeyEncryptedData, we need to check all others!
                // decrypt using hidden recipients (no subkey ids specified in OpenPGP packet)
                if (subKeyId == 0L) {
                    Log.d(Constants.TAG, "cryptoInput.getDecryptHiddenRecipientsIndex() "+cryptoInput.getDecryptHiddenRecipientsIndex() );
                    // on first pass, start with index 0
                    if (cryptoInput.getDecryptHiddenRecipientsIndex() == null) {
                        cryptoInput.setDecryptHiddenRecipientsIndex(0);
                    }

                    try {
                        subKeyId = mProviderHelper.getEncryptionSubkeyIdForHiddenRecipient(
                                cryptoInput.getDecryptHiddenRecipientsIndex());
                    } catch (ProviderHelper.NotFoundException e) {
                        // TODO: new log type
                        Log.d(Constants.TAG, "All available encrypt subkeys have been tried!");
                        log.add(LogType.MSG_DC_ERROR_NO_KEY, indent + 1);
                        return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
                    }
                }

                log.add(LogType.MSG_DC_ASYM, indent,
                        KeyFormattingUtils.convertKeyIdToHex(subKeyId));

                CanonicalizedSecretKeyRing secretKeyRing;
                try {
                    // get actual keyring object based on sub key id
                    secretKeyRing = mProviderHelper.getCanonicalizedSecretKeyRing(
                            KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(subKeyId)
                    );
                } catch (ProviderHelper.NotFoundException e) {
                    // continue with the next packet in the while loop
                    log.add(LogType.MSG_DC_ASKIP_NO_KEY, indent + 1);
                    continue;
                }
                if (secretKeyRing == null) {
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
                        skippedDisallowedKey = true;
                        log.add(LogType.MSG_DC_ASKIP_NOT_ALLOWED, indent + 1);
                        continue;
                    }
                }

                // get subkey which has been used for this encryption packet
                secretEncryptionKey = secretKeyRing.getSecretKey(subKeyId);
                if (secretEncryptionKey == null) {
                    // should actually never happen, so no need to be more specific.
                    log.add(LogType.MSG_DC_ASKIP_NO_KEY, indent + 1);
                    continue;
                }

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
                        return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
                    }

                    // if passphrase was not cached, return here indicating that a passphrase is missing!
                    if (passphrase == null) {
                        log.add(LogType.MSG_DC_PENDING_PASSPHRASE, indent + 1);
                        return new DecryptVerifyResult(log,
                                RequiredInputParcel.createRequiredDecryptPassphrase(
                                    secretKeyRing.getMasterKeyId(), secretEncryptionKey.getKeyId()),
                                cryptoInput);
                    }
                }

                // check for insecure encryption key
                if ( ! PgpSecurityConstants.isSecureKey(secretEncryptionKey)) {
                    log.add(LogType.MSG_DC_INSECURE_KEY, indent + 1);
                    decryptionResultBuilder.setInsecure(true);
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
                        return new DecryptVerifyResult(log,
                                RequiredInputParcel.createRequiredSymmetricPassphrase(),
                                cryptoInput);
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

        log.add(LogType.MSG_DC_PREP_STREAMS, indent);

        // we made sure above one of these two would be true
        int symmetricEncryptionAlgo;
        if (symmetricPacketFound) {
            currentProgress += 2;
            updateProgress(R.string.progress_preparing_streams, currentProgress, 100);

            PGPDigestCalculatorProvider digestCalcProvider = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build();
            PBEDataDecryptorFactory decryptorFactory = new JcePBEDataDecryptorFactoryBuilder(
                    digestCalcProvider).setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(
                    passphrase.getCharArray());

            try {
                clear = encryptedDataSymmetric.getDataStream(decryptorFactory);
            } catch (PGPDataValidationException e) {
                log.add(LogType.MSG_DC_ERROR_SYM_PASSPHRASE, indent +1);
                return new DecryptVerifyResult(log,
                        RequiredInputParcel.createRequiredSymmetricPassphrase(), cryptoInput);
            }

            encryptedData = encryptedDataSymmetric;

            symmetricEncryptionAlgo = encryptedDataSymmetric.getSymmetricAlgorithm(decryptorFactory);
        } else if (asymmetricPacketFound) {
            currentProgress += 2;
            updateProgress(R.string.progress_extracting_key, currentProgress, 100);

            try {
                log.add(LogType.MSG_DC_UNLOCKING, indent + 1);
                if (!secretEncryptionKey.unlock(passphrase)) {
                    log.add(LogType.MSG_DC_ERROR_BAD_PASSPHRASE, indent + 1);
                    return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
                }
            } catch (PgpGeneralException e) {
                log.add(LogType.MSG_DC_ERROR_EXTRACT_KEY, indent + 1);
                return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
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
                return new DecryptVerifyResult(log, RequiredInputParcel.createNfcDecryptOperation(
                        secretEncryptionKey.getRing().getMasterKeyId(),
                        secretEncryptionKey.getKeyId(), encryptedDataAsymmetric.getSessionKey()[0]
                ),
                        cryptoInput);

            }

//            try {
//                clear = encryptedDataAsymmetric.getDataStream(decryptorFactory);
//            } catch (PGPKeyValidationException | ArrayIndexOutOfBoundsException e) {
//                log.add(LogType.MSG_DC_ERROR_CORRUPT_DATA, indent + 1);
//                return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
//            }

            try {
                clear = encryptedDataAsymmetric.getDataStream(decryptorFactory);
            } catch (PGPKeyValidationException | ArrayIndexOutOfBoundsException | ClassCastException e) {

                // try next subkey in next pass
                cryptoInput.setDecryptHiddenRecipientsIndex(
                        cryptoInput.getDecryptHiddenRecipientsIndex() + 1
                );

                return new DecryptVerifyResult(log,
                        RequiredInputParcel.createRestartCryptoOperation(),
                        cryptoInput);
//                log.add(LogType.MSG_DC_ERROR_CORRUPT_DATA, indent + 1);
//                return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
            }


            symmetricEncryptionAlgo = encryptedDataAsymmetric.getSymmetricAlgorithm(decryptorFactory);

            cryptoInput.addCryptoData(decryptorFactory.getCachedSessionKeys());

            encryptedData = encryptedDataAsymmetric;
        } else {
            // there wasn't even any useful data
            if (!anyPacketFound) {
                log.add(LogType.MSG_DC_ERROR_NO_DATA, indent + 1);
                return new DecryptVerifyResult(DecryptVerifyResult.RESULT_NO_DATA, log);
            }
            // there was data but key wasn't allowed
            if (skippedDisallowedKey) {
                log.add(LogType.MSG_DC_ERROR_NO_KEY, indent + 1);
                return new DecryptVerifyResult(DecryptVerifyResult.RESULT_KEY_DISALLOWED, log);
            }
            // no packet has been found where we have the corresponding secret key in our db
            log.add(LogType.MSG_DC_ERROR_NO_KEY, indent + 1);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }
        decryptionResultBuilder.setEncrypted(true);

        // Check for insecure encryption algorithms!
        if (!PgpSecurityConstants.isSecureSymmetricAlgorithm(symmetricEncryptionAlgo)) {
            log.add(LogType.MSG_DC_INSECURE_SYMMETRIC_ENCRYPTION_ALGO, indent + 1);
            decryptionResultBuilder.setInsecure(true);
        }

        JcaPGPObjectFactory plainFact = new JcaPGPObjectFactory(clear);
        Object dataChunk = plainFact.nextObject();
        int signatureIndex = -1;
        CanonicalizedPublicKeyRing signingRing = null;
        CanonicalizedPublicKey signingKey = null;

        log.add(LogType.MSG_DC_CLEAR, indent);
        indent += 1;

        if (dataChunk instanceof PGPCompressedData) {
            log.add(LogType.MSG_DC_CLEAR_DECOMPRESS, indent + 1);
            currentProgress += 2;
            updateProgress(R.string.progress_decompressing_data, currentProgress, 100);

            PGPCompressedData compressedData = (PGPCompressedData) dataChunk;

            JcaPGPObjectFactory fact = new JcaPGPObjectFactory(compressedData.getDataStream());
            dataChunk = fact.nextObject();
            plainFact = fact;
        }

        PGPOnePassSignature signature = null;
        if (dataChunk instanceof PGPOnePassSignatureList) {
            log.add(LogType.MSG_DC_CLEAR_SIGNATURE, indent + 1);
            currentProgress += 2;
            updateProgress(R.string.progress_processing_signature, currentProgress, 100);

            PGPOnePassSignatureList sigList = (PGPOnePassSignatureList) dataChunk;

            // NOTE: following code is similar to processSignature, but for PGPOnePassSignature

            // go through all signatures
            // and find out for which signature we have a key in our database
            for (int i = 0; i < sigList.size(); ++i) {
                try {
                    long sigKeyId = sigList.get(i).getKeyID();
                    signingRing = mProviderHelper.getCanonicalizedPublicKeyRing(
                            KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(sigKeyId)
                    );
                    signingKey = signingRing.getPublicKey(sigKeyId);
                    signatureIndex = i;
                } catch (ProviderHelper.NotFoundException e) {
                    Log.d(Constants.TAG, "key not found, trying next signature...");
                }
            }

            if (signingKey != null) {
                // key found in our database!
                signature = sigList.get(signatureIndex);

                signatureResultBuilder.initValid(signingRing, signingKey);

                JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider =
                        new JcaPGPContentVerifierBuilderProvider()
                                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
                signature.init(contentVerifierBuilderProvider, signingKey.getPublicKey());
            } else {
                // no key in our database -> return "unknown pub key" status including the first key id
                if (!sigList.isEmpty()) {
                    signatureResultBuilder.setSignatureAvailable(true);
                    signatureResultBuilder.setKnownKey(false);
                    signatureResultBuilder.setKeyId(sigList.get(0).getKeyID());
                }
            }

            // check for insecure signing key
            // TODO: checks on signingRing ?
            if (signingKey != null && ! PgpSecurityConstants.isSecureKey(signingKey)) {
                log.add(LogType.MSG_DC_INSECURE_KEY, indent + 1);
                signatureResultBuilder.setInsecure(true);
            }

            dataChunk = plainFact.nextObject();
        }

        if (dataChunk instanceof PGPSignatureList) {
            // skip
            dataChunk = plainFact.nextObject();
        }

        OpenPgpMetadata metadata;

        if (dataChunk instanceof PGPLiteralData) {
            log.add(LogType.MSG_DC_CLEAR_DATA, indent + 1);
            indent += 2;
            currentProgress += 4;
            updateProgress(R.string.progress_decrypting, currentProgress, 100);

            PGPLiteralData literalData = (PGPLiteralData) dataChunk;

            String originalFilename = literalData.getFileName();
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
            log.add(LogType.MSG_DC_CLEAR_META_MIME, indent + 1,
                    mimeType);
            log.add(LogType.MSG_DC_CLEAR_META_TIME, indent + 1,
                    new Date(literalData.getModificationTime().getTime()).toString());

            // return here if we want to decrypt the metadata only
            if (input.isDecryptMetadataOnly()) {

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
                        originalSize == null ? 0 : originalSize);

                log.add(LogType.MSG_DC_OK_META_ONLY, indent);
                DecryptVerifyResult result =
                        new DecryptVerifyResult(DecryptVerifyResult.RESULT_OK, log);
                result.setCharset(charset);
                result.setDecryptionMetadata(metadata);
                return result;
            }

            int endProgress;
            if (signature != null) {
                endProgress = 90;
            } else if (encryptedData.isIntegrityProtected()) {
                endProgress = 95;
            } else {
                endProgress = 100;
            }
            ProgressScaler progressScaler =
                    new ProgressScaler(mProgressable, currentProgress, endProgress, 100);

            InputStream dataIn = literalData.getInputStream();

            long alreadyWritten = 0;
            long wholeSize = 0; // TODO inputData.getSize() - inputData.getStreamPosition();
            int length;
            byte[] buffer = new byte[1 << 16];
            while ((length = dataIn.read(buffer)) > 0) {
                // Log.d(Constants.TAG, "read bytes: " + length);
                if (out != null) {
                    out.write(buffer, 0, length);
                }

                // update signature buffer if signature is also present
                if (signature != null) {
                    signature.update(buffer, 0, length);
                }

                alreadyWritten += length;
                if (wholeSize > 0) {
                    long progress = 100 * alreadyWritten / wholeSize;
                    // stop at 100% for wrong file sizes...
                    if (progress > 100) {
                        progress = 100;
                    }
                    progressScaler.setProgress((int) progress, 100);
                }
                // TODO: slow annealing to fake a progress?
            }

            metadata = new OpenPgpMetadata(
                    originalFilename,
                    mimeType,
                    literalData.getModificationTime().getTime(),
                    alreadyWritten);

            if (signature != null) {
                updateProgress(R.string.progress_verifying_signature, 90, 100);
                log.add(LogType.MSG_DC_CLEAR_SIGNATURE_CHECK, indent);

                PGPSignatureList signatureList = (PGPSignatureList) plainFact.nextObject();
                PGPSignature messageSignature = signatureList.get(signatureIndex);

                // TODO: what about binary signatures?

                // Verify signature
                boolean validSignature = signature.verify(messageSignature);
                if (validSignature) {
                    log.add(LogType.MSG_DC_CLEAR_SIGNATURE_OK, indent + 1);
                } else {
                    log.add(LogType.MSG_DC_CLEAR_SIGNATURE_BAD, indent + 1);
                }

                // check for insecure hash algorithms
                if (!PgpSecurityConstants.isSecureHashAlgorithm(signature.getHashAlgorithm())) {
                    log.add(LogType.MSG_DC_INSECURE_HASH_ALGO, indent + 1);
                    signatureResultBuilder.setInsecure(true);
                }

                signatureResultBuilder.setValidSignature(validSignature);
            }

            indent -= 1;
        } else {
            // If there is no literalData, we don't have any metadata
            metadata = null;
        }

        if (encryptedData.isIntegrityProtected()) {
            updateProgress(R.string.progress_verifying_integrity, 95, 100);

            if (encryptedData.verify()) {
                log.add(LogType.MSG_DC_INTEGRITY_CHECK_OK, indent);
            } else {
                log.add(LogType.MSG_DC_ERROR_INTEGRITY_CHECK, indent);
                return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
            }
        } else {
            // If no valid signature is present:
            // Handle missing integrity protection like failed integrity protection!
            // The MDC packet can be stripped by an attacker!
            Log.d(Constants.TAG, "MDC fail");
            if (!signatureResultBuilder.isValidSignature()) {
                log.add(LogType.MSG_DC_INSECURE_MDC_MISSING, indent);
                decryptionResultBuilder.setInsecure(true);
            }
        }

        updateProgress(R.string.progress_done, 100, 100);

        log.add(LogType.MSG_DC_OK, indent);

        // Return a positive result, with metadata and verification info
        DecryptVerifyResult result = new DecryptVerifyResult(DecryptVerifyResult.RESULT_OK, log);
        result.setCachedCryptoInputParcel(cryptoInput);
        result.setSignatureResult(signatureResultBuilder.build());
        result.setCharset(charset);
        result.setDecryptionResult(decryptionResultBuilder.build());
        result.setDecryptionMetadata(metadata);
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

        OpenPgpSignatureResultBuilder signatureResultBuilder = new OpenPgpSignatureResultBuilder();

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

        byte[] clearText = out.toByteArray();
        if (outputStream != null) {
            outputStream.write(clearText);
            outputStream.close();
        }

        updateProgress(R.string.progress_processing_signature, 60, 100);
        JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(aIn);

        PGPSignatureList sigList = (PGPSignatureList) pgpFact.nextObject();
        if (sigList == null) {
            log.add(LogType.MSG_DC_ERROR_INVALID_DATA, 0);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }

        PGPSignature signature = processPGPSignatureList(sigList, signatureResultBuilder, log, indent);

        if (signature != null) {
            try {
                updateProgress(R.string.progress_verifying_signature, 90, 100);
                log.add(LogType.MSG_DC_CLEAR_SIGNATURE_CHECK, indent);

                InputStream sigIn = new BufferedInputStream(new ByteArrayInputStream(clearText));

                lookAhead = readInputLine(lineOut, sigIn);

                processLine(signature, lineOut.toByteArray());

                if (lookAhead != -1) {
                    do {
                        lookAhead = readInputLine(lineOut, lookAhead, sigIn);

                        signature.update((byte) '\r');
                        signature.update((byte) '\n');

                        processLine(signature, lineOut.toByteArray());
                    } while (lookAhead != -1);
                }

                // Verify signature and check binding signatures
                boolean validSignature = signature.verify();
                if (validSignature) {
                    log.add(LogType.MSG_DC_CLEAR_SIGNATURE_OK, indent + 1);
                } else {
                    log.add(LogType.MSG_DC_CLEAR_SIGNATURE_BAD, indent + 1);
                }

                // check for insecure hash algorithms
                if (!PgpSecurityConstants.isSecureHashAlgorithm(signature.getHashAlgorithm())) {
                    log.add(LogType.MSG_DC_INSECURE_HASH_ALGO, indent + 1);
                    signatureResultBuilder.setInsecure(true);
                }

                signatureResultBuilder.setValidSignature(validSignature);

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
        result.setSignatureResult(signatureResultBuilder.build());
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

        OpenPgpSignatureResultBuilder signatureResultBuilder = new OpenPgpSignatureResultBuilder();

        updateProgress(R.string.progress_processing_signature, 0, 100);
        InputStream detachedSigIn = new ByteArrayInputStream(input.getDetachedSignature());
        detachedSigIn = PGPUtil.getDecoderStream(detachedSigIn);

        JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(detachedSigIn);

        PGPSignatureList sigList;
        Object o = pgpFact.nextObject();
        if (o instanceof PGPCompressedData) {
            PGPCompressedData c1 = (PGPCompressedData) o;
            pgpFact = new JcaPGPObjectFactory(c1.getDataStream());
            sigList = (PGPSignatureList) pgpFact.nextObject();
        } else if (o instanceof PGPSignatureList) {
            sigList = (PGPSignatureList) o;
        } else {
            log.add(LogType.MSG_DC_ERROR_INVALID_DATA, 0);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }

        PGPSignature signature = processPGPSignatureList(sigList, signatureResultBuilder, log, indent);

        if (signature != null) {
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
                signature.update(buffer, 0, length);

                alreadyWritten += length;
                if (wholeSize > 0) {
                    long progress = 100 * alreadyWritten / wholeSize;
                    // stop at 100% for wrong file sizes...
                    if (progress > 100) {
                        progress = 100;
                    }
                    progressScaler.setProgress((int) progress, 100);
                }
                // TODO: slow annealing to fake a progress?
            }

            updateProgress(R.string.progress_verifying_signature, 90, 100);
            log.add(LogType.MSG_DC_CLEAR_SIGNATURE_CHECK, indent);

            // Verify signature and check binding signatures
            boolean validSignature = signature.verify();
            if (validSignature) {
                log.add(LogType.MSG_DC_CLEAR_SIGNATURE_OK, indent + 1);
            } else {
                log.add(LogType.MSG_DC_CLEAR_SIGNATURE_BAD, indent + 1);
            }

            // check for insecure hash algorithms
            if (!PgpSecurityConstants.isSecureHashAlgorithm(signature.getHashAlgorithm())) {
                log.add(LogType.MSG_DC_INSECURE_HASH_ALGO, indent + 1);
                signatureResultBuilder.setInsecure(true);
            }

            signatureResultBuilder.setValidSignature(validSignature);
        }

        updateProgress(R.string.progress_done, 100, 100);

        log.add(LogType.MSG_DC_OK, indent);

        DecryptVerifyResult result = new DecryptVerifyResult(DecryptVerifyResult.RESULT_OK, log);
        result.setSignatureResult(signatureResultBuilder.build());
        result.setDecryptionResult(
                new OpenPgpDecryptionResult(OpenPgpDecryptionResult.RESULT_NOT_ENCRYPTED));
        return result;
    }

    private PGPSignature processPGPSignatureList(
            PGPSignatureList sigList, OpenPgpSignatureResultBuilder signatureResultBuilder,
            OperationLog log, int indent)
            throws PGPException {
        CanonicalizedPublicKeyRing signingRing = null;
        CanonicalizedPublicKey signingKey = null;
        int signatureIndex = -1;

        // go through all signatures
        // and find out for which signature we have a key in our database
        for (int i = 0; i < sigList.size(); ++i) {
            try {
                long sigKeyId = sigList.get(i).getKeyID();
                signingRing = mProviderHelper.getCanonicalizedPublicKeyRing(
                        KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(sigKeyId)
                );
                signingKey = signingRing.getPublicKey(sigKeyId);
                signatureIndex = i;
            } catch (ProviderHelper.NotFoundException e) {
                Log.d(Constants.TAG, "key not found, trying next signature...");
            }
        }

        PGPSignature signature = null;

        if (signingKey != null) {
            // key found in our database!
            signature = sigList.get(signatureIndex);

            signatureResultBuilder.initValid(signingRing, signingKey);

            JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider =
                    new JcaPGPContentVerifierBuilderProvider()
                            .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
            signature.init(contentVerifierBuilderProvider, signingKey.getPublicKey());
        } else {
            // no key in our database -> return "unknown pub key" status including the first key id
            if (!sigList.isEmpty()) {
                signatureResultBuilder.setSignatureAvailable(true);
                signatureResultBuilder.setKnownKey(false);
                signatureResultBuilder.setKeyId(sigList.get(0).getKeyID());
            }
        }

        // check for insecure signing key
        // TODO: checks on signingRing ?
        if (signingKey != null && ! PgpSecurityConstants.isSecureKey(signingKey)) {
            log.add(LogType.MSG_DC_INSECURE_KEY, indent + 1);
            signatureResultBuilder.setInsecure(true);
        }

        return signature;
    }

    /**
     * Mostly taken from ClearSignedFileProcessor in Bouncy Castle
     */
    private static void processLine(PGPSignature sig, byte[] line)
            throws SignatureException {
        int length = getLengthWithoutWhiteSpace(line);
        if (length > 0) {
            sig.update(line, 0, length);
        }
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

    private static int getLengthWithoutWhiteSpace(byte[] line) {
        int end = line.length - 1;

        while (end >= 0 && isWhiteSpace(line[end])) {
            end--;
        }

        return end + 1;
    }

    private static boolean isWhiteSpace(byte b) {
        return b == '\r' || b == '\n' || b == '\t' || b == ' ';
    }

    private static byte[] getLineSeparator() {
        String nl = System.getProperty("line.separator");
        return nl.getBytes();
    }
}
