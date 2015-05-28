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

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.BCPGOutputStream;
import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedDataGenerator;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.operator.jcajce.JcePBEKeyEncryptionMethodGenerator;
import org.spongycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.NfcSyncPGPContentSignerBuilder;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.BaseOperation;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.PgpSignEncryptResult;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
public class PgpSignEncryptOperation extends BaseOperation {

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

    /**
     * Signs and/or encrypts data based on parameters of class
     */
    public PgpSignEncryptResult execute(PgpSignEncryptInputParcel input, CryptoInputParcel cryptoInput,
                                     InputData inputData, OutputStream outputStream) {

        int indent = 0;
        OperationLog log = new OperationLog();

        log.add(LogType.MSG_PSE, indent);
        indent += 1;

        boolean enableSignature = input.getSignatureMasterKeyId() != Constants.key.none;
        boolean enableEncryption = ((input.getEncryptionMasterKeyIds() != null && input.getEncryptionMasterKeyIds().length > 0)
                || input.getSymmetricPassphrase() != null);
        boolean enableCompression = (input.getCompressionId() != CompressionAlgorithmTags.UNCOMPRESSED);

        Log.d(Constants.TAG, "enableSignature:" + enableSignature
                + "\nenableEncryption:" + enableEncryption
                + "\nenableCompression:" + enableCompression
                + "\nenableAsciiArmorOutput:" + input.isEnableAsciiArmorOutput()
                + "\nisHiddenRecipients:" + input.isHiddenRecipients());

        // add additional key id to encryption ids (mostly to do self-encryption)
        if (enableEncryption && input.getAdditionalEncryptId() != Constants.key.none) {
            input.setEncryptionMasterKeyIds(Arrays.copyOf(input.getEncryptionMasterKeyIds(), input.getEncryptionMasterKeyIds().length + 1));
            input.getEncryptionMasterKeyIds()[input.getEncryptionMasterKeyIds().length - 1] = input.getAdditionalEncryptId();
        }

        ArmoredOutputStream armorOut = null;
        OutputStream out;
        if (input.isEnableAsciiArmorOutput()) {
            armorOut = new ArmoredOutputStream(new BufferedOutputStream(outputStream, 1 << 16));
            if (input.getVersionHeader() != null) {
                armorOut.setHeader("Version", input.getVersionHeader());
            }
            // if we have a charset, put it in the header
            if (input.getCharset() != null) {
                armorOut.setHeader("Charset", input.getCharset());
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
                // fetch the indicated master key id (the one whose name we sign in)
                CanonicalizedSecretKeyRing signingKeyRing =
                        mProviderHelper.getCanonicalizedSecretKeyRing(input.getSignatureMasterKeyId());

                // fetch the specific subkey to sign with, or just use the master key if none specified
                signingKey = signingKeyRing.getSecretKey(input.getSignatureSubKeyId());

                // Make sure we are allowed to sign here!
                if (!signingKey.canSign()) {
                    log.add(LogType.MSG_PSE_ERROR_KEY_SIGN, indent);
                    return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
                }

                switch (signingKey.getSecretKeyType()) {
                    case DIVERT_TO_CARD:
                    case PASSPHRASE_EMPTY: {
                        if (!signingKey.unlock(new Passphrase())) {
                            throw new AssertionError(
                                    "PASSPHRASE_EMPTY/DIVERT_TO_CARD keyphrase not unlocked with empty passphrase."
                                            + " This is a programming error!");
                        }
                        break;
                    }

                    case PIN:
                    case PATTERN:
                    case PASSPHRASE: {
                        Passphrase localPassphrase = cryptoInput.getPassphrase();
                        if (localPassphrase == null) {
                            try {
                                localPassphrase = getCachedPassphrase(signingKeyRing.getMasterKeyId(), signingKey.getKeyId());
                            } catch (PassphraseCacheInterface.NoSecretKeyException ignored) {
                            }
                        }
                        if (localPassphrase == null) {
                            log.add(LogType.MSG_PSE_PENDING_PASSPHRASE, indent + 1);
                            return new PgpSignEncryptResult(log, RequiredInputParcel.createRequiredSignPassphrase(
                                    signingKeyRing.getMasterKeyId(), signingKey.getKeyId(),
                                    cryptoInput.getSignatureTime()));
                        }
                        if (!signingKey.unlock(localPassphrase)) {
                            log.add(LogType.MSG_PSE_ERROR_BAD_PASSPHRASE, indent);
                            return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
                        }
                        break;
                    }

                    case GNU_DUMMY: {
                        log.add(LogType.MSG_PSE_ERROR_UNLOCK, indent);
                        return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
                    }
                    default: {
                        throw new AssertionError("Unhandled SecretKeyType! (should not happen)");
                    }

                }

            } catch (ProviderHelper.NotFoundException e) {
                log.add(LogType.MSG_PSE_ERROR_SIGN_KEY, indent);
                return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
            } catch (PgpGeneralException e) {
                log.add(LogType.MSG_PSE_ERROR_UNLOCK, indent);
                return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
            }

            // Use preferred hash algo
            int requestedAlgorithm = input.getSignatureHashAlgorithm();
            ArrayList<Integer> supported = signingKey.getSupportedHashAlgorithms();
            if (requestedAlgorithm == PgpConstants.OpenKeychainHashAlgorithmTags.USE_PREFERRED) {
                // get most preferred
                input.setSignatureHashAlgorithm(supported.get(0));
            } else if (!supported.contains(requestedAlgorithm)) {
                log.add(LogType.MSG_PSE_ERROR_HASH_ALGO, indent);
                return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
            }
        }
        updateProgress(R.string.progress_preparing_streams, 2, 100);

        /* Initialize PGPEncryptedDataGenerator for later usage */
        PGPEncryptedDataGenerator cPk = null;
        if (enableEncryption) {

            // Use preferred encryption algo
            int algo = input.getSymmetricEncryptionAlgorithm();
            if (algo == PgpConstants.OpenKeychainSymmetricKeyAlgorithmTags.USE_PREFERRED) {
                // get most preferred
                // TODO: get from recipients
                algo = PgpConstants.sPreferredSymmetricAlgorithms.get(0);
            }
            // has Integrity packet enabled!
            JcePGPDataEncryptorBuilder encryptorBuilder =
                    new JcePGPDataEncryptorBuilder(algo)
                            .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME)
                            .setWithIntegrityPacket(true);

            cPk = new PGPEncryptedDataGenerator(encryptorBuilder);

            if (input.getSymmetricPassphrase() != null) {
                // Symmetric encryption
                log.add(LogType.MSG_PSE_SYMMETRIC, indent);

                JcePBEKeyEncryptionMethodGenerator symmetricEncryptionGenerator =
                        new JcePBEKeyEncryptionMethodGenerator(input.getSymmetricPassphrase().getCharArray());
                cPk.addMethod(symmetricEncryptionGenerator);
            } else {
                log.add(LogType.MSG_PSE_ASYMMETRIC, indent);

                // Asymmetric encryption
                for (long id : input.getEncryptionMasterKeyIds()) {
                    try {
                        CanonicalizedPublicKeyRing keyRing = mProviderHelper.getCanonicalizedPublicKeyRing(
                                KeyRings.buildUnifiedKeyRingUri(id));
                        CanonicalizedPublicKey key = keyRing.getEncryptionSubKey();
                        cPk.addMethod(key.getPubKeyEncryptionGenerator(input.isHiddenRecipients()));
                        log.add(LogType.MSG_PSE_KEY_OK, indent + 1,
                                KeyFormattingUtils.convertKeyIdToHex(id));
                    } catch (PgpKeyNotFoundException e) {
                        log.add(LogType.MSG_PSE_KEY_WARN, indent + 1,
                                KeyFormattingUtils.convertKeyIdToHex(id));
                        if (input.isFailOnMissingEncryptionKeyIds()) {
                            return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
                        }
                    } catch (ProviderHelper.NotFoundException e) {
                        log.add(LogType.MSG_PSE_KEY_UNKNOWN, indent + 1,
                                KeyFormattingUtils.convertKeyIdToHex(id));
                        if (input.isFailOnMissingEncryptionKeyIds()) {
                            return new PgpSignEncryptResult(PgpSignEncryptResult.RESULT_ERROR, log);
                        }
                    }
                }
            }
        }

        /* Initialize signature generator object for later usage */
        PGPSignatureGenerator signatureGenerator = null;
        if (enableSignature) {
            updateProgress(R.string.progress_preparing_signature, 4, 100);

            try {
                boolean cleartext = input.isCleartextSignature() && input.isEnableAsciiArmorOutput() && !enableEncryption;
                signatureGenerator = signingKey.getDataSignatureGenerator(
                        input.getSignatureHashAlgorithm(), cleartext,
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
                    compressGen = new PGPCompressedDataGenerator(input.getCompressionId());
                    bcpgOut = new BCPGOutputStream(compressGen.open(encryptionOut));
                } else {
                    bcpgOut = new BCPGOutputStream(encryptionOut);
                }

                if (enableSignature) {
                    signatureGenerator.generateOnePassVersion(false).encode(bcpgOut);
                }

                PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
                char literalDataFormatTag;
                if (input.isCleartextSignature()) {
                    literalDataFormatTag = PGPLiteralData.UTF8;
                } else {
                    literalDataFormatTag = PGPLiteralData.BINARY;
                }
                pOut = literalGen.open(bcpgOut, literalDataFormatTag,
                        inputData.getOriginalFilename(), new Date(), new byte[1 << 16]);

                long alreadyWritten = 0;
                int length;
                byte[] buffer = new byte[1 << 16];
                InputStream in = inputData.getInputStream();
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

            } else if (enableSignature && input.isCleartextSignature() && input.isEnableAsciiArmorOutput()) {
                /* cleartext signature: sign-only of ascii text */

                updateProgress(R.string.progress_signing, 8, 100);
                log.add(LogType.MSG_PSE_SIGNING_CLEARTEXT, indent);

                // write -----BEGIN PGP SIGNED MESSAGE-----
                armorOut.beginClearText(input.getSignatureHashAlgorithm());

                InputStream in = inputData.getInputStream();
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
            } else if (enableSignature && input.isDetachedSignature()) {
                /* detached signature */

                updateProgress(R.string.progress_signing, 8, 100);
                log.add(LogType.MSG_PSE_SIGNING_DETACHED, indent);

                InputStream in = inputData.getInputStream();

                // handle output stream separately for detached signatures
                detachedByteOut = new ByteArrayOutputStream();
                OutputStream detachedOut = detachedByteOut;
                if (input.isEnableAsciiArmorOutput()) {
                    detachedArmorOut = new ArmoredOutputStream(new BufferedOutputStream(detachedOut, 1 << 16));
                    if (input.getVersionHeader() != null) {
                        detachedArmorOut.setHeader("Version", input.getVersionHeader());
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
            } else if (enableSignature && !input.isCleartextSignature() && !input.isDetachedSignature()) {
                /* sign-only binary (files/data stream) */

                updateProgress(R.string.progress_signing, 8, 100);
                log.add(LogType.MSG_PSE_SIGNING, indent);

                InputStream in = inputData.getInputStream();

                if (enableCompression) {
                    compressGen = new PGPCompressedDataGenerator(input.getCompressionId());
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
                pOut = null;
                // TODO: Is this log right?
                log.add(LogType.MSG_PSE_CLEARSIGN_ONLY, indent);
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
                    return new PgpSignEncryptResult(log, RequiredInputParcel.createNfcSignOperation(
                            signingKey.getRing().getMasterKeyId(), signingKey.getKeyId(),
                            e.hashToSign, e.hashAlgo, cryptoInput.getSignatureTime()));
                }
            }

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
        if (detachedByteOut != null) {
            try {
                detachedByteOut.flush();
                detachedByteOut.close();
            } catch (IOException e) {
                // silently catch
            }
            result.setDetachedSignature(detachedByteOut.toByteArray());
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
