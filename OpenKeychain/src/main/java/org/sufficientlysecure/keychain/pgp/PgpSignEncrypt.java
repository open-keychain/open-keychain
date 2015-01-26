/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.BaseOperation;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

/**
 * This class uses a Builder pattern!
 */
public class PgpSignEncrypt extends BaseOperation {
    private String mVersionHeader;
    private InputData mData;
    private OutputStream mOutStream;

    private boolean mEnableAsciiArmorOutput;
    private int mCompressionId;
    private long[] mEncryptionMasterKeyIds;
    private String mSymmetricPassphrase;
    private int mSymmetricEncryptionAlgorithm;
    private long mSignatureMasterKeyId;
    private Long mSignatureSubKeyId;
    private int mSignatureHashAlgorithm;
    private String mSignaturePassphrase;
    private long mAdditionalEncryptId;
    private boolean mCleartextSignature;
    private boolean mDetachedSignature;
    private String mOriginalFilename;
    private boolean mFailOnMissingEncryptionKeyIds;

    private byte[] mNfcSignedHash = null;
    private Date mNfcCreationTimestamp = null;

    private static byte[] NEW_LINE;

    static {
        try {
            NEW_LINE = "\r\n".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(Constants.TAG, "UnsupportedEncodingException", e);
        }
    }

    protected PgpSignEncrypt(Builder builder) {
        super(builder.mContext, builder.mProviderHelper, builder.mProgressable);

        // private Constructor can only be called from Builder
        this.mVersionHeader = builder.mVersionHeader;
        this.mData = builder.mData;
        this.mOutStream = builder.mOutStream;

        this.mEnableAsciiArmorOutput = builder.mEnableAsciiArmorOutput;
        this.mCompressionId = builder.mCompressionId;
        this.mEncryptionMasterKeyIds = builder.mEncryptionMasterKeyIds;
        this.mSymmetricPassphrase = builder.mSymmetricPassphrase;
        this.mSymmetricEncryptionAlgorithm = builder.mSymmetricEncryptionAlgorithm;
        this.mSignatureMasterKeyId = builder.mSignatureMasterKeyId;
        this.mSignatureSubKeyId = builder.mSignatureSubKeyId;
        this.mSignatureHashAlgorithm = builder.mSignatureHashAlgorithm;
        this.mSignaturePassphrase = builder.mSignaturePassphrase;
        this.mAdditionalEncryptId = builder.mAdditionalEncryptId;
        this.mCleartextSignature = builder.mCleartextSignature;
        this.mDetachedSignature = builder.mDetachedSignature;
        this.mNfcSignedHash = builder.mNfcSignedHash;
        this.mNfcCreationTimestamp = builder.mNfcCreationTimestamp;
        this.mOriginalFilename = builder.mOriginalFilename;
        this.mFailOnMissingEncryptionKeyIds = builder.mFailOnMissingEncryptionKeyIds;
    }

    public static class Builder {
        // mandatory parameter
        private Context mContext;
        private ProviderHelper mProviderHelper;
        private Progressable mProgressable;
        private InputData mData;
        private OutputStream mOutStream;

        // optional
        private String mVersionHeader = null;
        private boolean mEnableAsciiArmorOutput = false;
        private int mCompressionId = CompressionAlgorithmTags.UNCOMPRESSED;
        private long[] mEncryptionMasterKeyIds = null;
        private String mSymmetricPassphrase = null;
        private int mSymmetricEncryptionAlgorithm = 0;
        private long mSignatureMasterKeyId = Constants.key.none;
        private Long mSignatureSubKeyId = null;
        private int mSignatureHashAlgorithm = 0;
        private String mSignaturePassphrase = null;
        private long mAdditionalEncryptId = Constants.key.none;
        private boolean mCleartextSignature = false;
        private boolean mDetachedSignature = false;
        private String mOriginalFilename = "";
        private byte[] mNfcSignedHash = null;
        private Date mNfcCreationTimestamp = null;
        private boolean mFailOnMissingEncryptionKeyIds = false;

        public Builder(Context context, ProviderHelper providerHelper, Progressable progressable,
                       InputData data, OutputStream outStream) {
            mContext = context;
            mProviderHelper = providerHelper;
            mProgressable = progressable;

            mData = data;
            mOutStream = outStream;
        }

        public Builder setVersionHeader(String versionHeader) {
            mVersionHeader = versionHeader;
            return this;
        }

        public Builder setEnableAsciiArmorOutput(boolean enableAsciiArmorOutput) {
            mEnableAsciiArmorOutput = enableAsciiArmorOutput;
            return this;
        }

        public Builder setCompressionId(int compressionId) {
            mCompressionId = compressionId;
            return this;
        }

        public Builder setEncryptionMasterKeyIds(long[] encryptionMasterKeyIds) {
            mEncryptionMasterKeyIds = encryptionMasterKeyIds;
            return this;
        }

        public Builder setSymmetricPassphrase(String symmetricPassphrase) {
            mSymmetricPassphrase = symmetricPassphrase;
            return this;
        }

        public Builder setSymmetricEncryptionAlgorithm(int symmetricEncryptionAlgorithm) {
            mSymmetricEncryptionAlgorithm = symmetricEncryptionAlgorithm;
            return this;
        }

        public Builder setSignatureMasterKeyId(long signatureMasterKeyId) {
            mSignatureMasterKeyId = signatureMasterKeyId;
            return this;
        }

        public Builder setSignatureSubKeyId(long signatureSubKeyId) {
            mSignatureSubKeyId = signatureSubKeyId;
            return this;
        }

        public Builder setSignatureHashAlgorithm(int signatureHashAlgorithm) {
            mSignatureHashAlgorithm = signatureHashAlgorithm;
            return this;
        }

        public Builder setSignaturePassphrase(String signaturePassphrase) {
            mSignaturePassphrase = signaturePassphrase;
            return this;
        }

        public Builder setFailOnMissingEncryptionKeyIds(boolean failOnMissingEncryptionKeyIds) {
            mFailOnMissingEncryptionKeyIds = failOnMissingEncryptionKeyIds;
            return this;
        }

        /**
         * Also encrypt with the signing keyring
         *
         * @param additionalEncryptId
         * @return
         */
        public Builder setAdditionalEncryptId(long additionalEncryptId) {
            mAdditionalEncryptId = additionalEncryptId;
            return this;
        }

        public Builder setCleartextSignature(boolean cleartextSignature) {
            mCleartextSignature = cleartextSignature;
            return this;
        }

        public Builder setDetachedSignature(boolean detachedSignature) {
            mDetachedSignature = detachedSignature;
            return this;
        }

        public Builder setOriginalFilename(String originalFilename) {
            mOriginalFilename = originalFilename;
            return this;
        }

        public Builder setNfcState(byte[] signedHash, Date creationTimestamp) {
            mNfcSignedHash = signedHash;
            mNfcCreationTimestamp = creationTimestamp;
            return this;
        }

        public PgpSignEncrypt build() {
            return new PgpSignEncrypt(this);
        }
    }

    /**
     * Signs and/or encrypts data based on parameters of class
     */
    public SignEncryptResult execute() {

        int indent = 0;
        OperationLog log = new OperationLog();

        log.add(LogType.MSG_SE, indent);
        indent += 1;

        boolean enableSignature = mSignatureMasterKeyId != Constants.key.none;
        boolean enableEncryption = ((mEncryptionMasterKeyIds != null && mEncryptionMasterKeyIds.length > 0)
                || mSymmetricPassphrase != null);
        boolean enableCompression = (mCompressionId != CompressionAlgorithmTags.UNCOMPRESSED);

        Log.d(Constants.TAG, "enableSignature:" + enableSignature
                + "\nenableEncryption:" + enableEncryption
                + "\nenableCompression:" + enableCompression
                + "\nenableAsciiArmorOutput:" + mEnableAsciiArmorOutput);

        // add additional key id to encryption ids (mostly to do self-encryption)
        if (enableEncryption && mAdditionalEncryptId != Constants.key.none) {
            mEncryptionMasterKeyIds = Arrays.copyOf(mEncryptionMasterKeyIds, mEncryptionMasterKeyIds.length + 1);
            mEncryptionMasterKeyIds[mEncryptionMasterKeyIds.length - 1] = mAdditionalEncryptId;
        }

        ArmoredOutputStream armorOut = null;
        OutputStream out;
        if (mEnableAsciiArmorOutput) {
            armorOut = new ArmoredOutputStream(mOutStream);
            if (mVersionHeader != null) {
                armorOut.setHeader("Version", mVersionHeader);
            }
            out = armorOut;
        } else {
            out = mOutStream;
        }

        /* Get keys for signature generation for later usage */
        CanonicalizedSecretKey signingKey = null;
        long signKeyId;
        if (enableSignature) {

            try {
                // fetch the indicated master key id (the one whose name we sign in)
                CanonicalizedSecretKeyRing signingKeyRing =
                        mProviderHelper.getCanonicalizedSecretKeyRing(mSignatureMasterKeyId);
                // fetch the specific subkey to sign with, or just use the master key if none specified
                signKeyId = mSignatureSubKeyId != null ? mSignatureSubKeyId : mSignatureMasterKeyId;
                signingKey = signingKeyRing.getSecretKey(signKeyId);
                // make sure it's a signing key alright!
            } catch (ProviderHelper.NotFoundException e) {
                log.add(LogType.MSG_SE_ERROR_SIGN_KEY, indent);
                return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log);
            }

            // Make sure we are allowed to sign here!
            if (!signingKey.canSign()) {
                log.add(LogType.MSG_SE_ERROR_KEY_SIGN, indent);
                return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log);
            }

            // if no passphrase was explicitly set try to get it from the cache service
            if (mSignaturePassphrase == null) {
                try {
                    // returns "" if key has no passphrase
                    mSignaturePassphrase = getCachedPassphrase(signKeyId);
                    // TODO
//                    log.add(LogType.MSG_DC_PASS_CACHED, indent + 1);
                } catch (PassphraseCacheInterface.NoSecretKeyException e) {
                    // TODO
//                    log.add(LogType.MSG_DC_ERROR_NO_KEY, indent + 1);
                    return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log);
                }

                // if passphrase was not cached, return here indicating that a passphrase is missing!
                if (mSignaturePassphrase == null) {
                    log.add(LogType.MSG_SE_PENDING_PASSPHRASE, indent + 1);
                    SignEncryptResult result = new SignEncryptResult(SignEncryptResult.RESULT_PENDING_PASSPHRASE, log);
                    result.setKeyIdPassphraseNeeded(signKeyId);
                    return result;
                }
            }

            updateProgress(R.string.progress_extracting_signature_key, 0, 100);

            try {
                if (!signingKey.unlock(mSignaturePassphrase)) {
                    log.add(LogType.MSG_SE_ERROR_BAD_PASSPHRASE, indent);
                    return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log);
                }
            } catch (PgpGeneralException e) {
                log.add(LogType.MSG_SE_ERROR_UNLOCK, indent);
                return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log);
            }

            // check if hash algo is supported
            LinkedList<Integer> supported = signingKey.getSupportedHashAlgorithms();
            if (!supported.contains(mSignatureHashAlgorithm)) {
                // get most preferred
                mSignatureHashAlgorithm = supported.getLast();
            }
        }
        updateProgress(R.string.progress_preparing_streams, 2, 100);

        /* Initialize PGPEncryptedDataGenerator for later usage */
        PGPEncryptedDataGenerator cPk = null;
        if (enableEncryption) {
            // has Integrity packet enabled!
            JcePGPDataEncryptorBuilder encryptorBuilder =
                    new JcePGPDataEncryptorBuilder(mSymmetricEncryptionAlgorithm)
                            .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME)
                            .setWithIntegrityPacket(true);

            cPk = new PGPEncryptedDataGenerator(encryptorBuilder);

            if (mSymmetricPassphrase != null) {
                // Symmetric encryption
                log.add(LogType.MSG_SE_SYMMETRIC, indent);

                JcePBEKeyEncryptionMethodGenerator symmetricEncryptionGenerator =
                        new JcePBEKeyEncryptionMethodGenerator(mSymmetricPassphrase.toCharArray());
                cPk.addMethod(symmetricEncryptionGenerator);
            } else {
                log.add(LogType.MSG_SE_ASYMMETRIC, indent);

                // Asymmetric encryption
                for (long id : mEncryptionMasterKeyIds) {
                    try {
                        CanonicalizedPublicKeyRing keyRing = mProviderHelper.getCanonicalizedPublicKeyRing(
                                KeyRings.buildUnifiedKeyRingUri(id));
                        CanonicalizedPublicKey key = keyRing.getEncryptionSubKey();
                        cPk.addMethod(key.getPubKeyEncryptionGenerator());
                        log.add(LogType.MSG_SE_KEY_OK, indent + 1,
                                KeyFormattingUtils.convertKeyIdToHex(id));
                    } catch (PgpKeyNotFoundException e) {
                        log.add(LogType.MSG_SE_KEY_WARN, indent + 1,
                                KeyFormattingUtils.convertKeyIdToHex(id));
                        if (mFailOnMissingEncryptionKeyIds) {
                            return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log);
                        }
                    } catch (ProviderHelper.NotFoundException e) {
                        log.add(LogType.MSG_SE_KEY_UNKNOWN, indent + 1,
                                KeyFormattingUtils.convertKeyIdToHex(id));
                        if (mFailOnMissingEncryptionKeyIds) {
                            return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log);
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
                boolean cleartext = mCleartextSignature && mEnableAsciiArmorOutput && !enableEncryption;
                signatureGenerator = signingKey.getSignatureGenerator(
                        mSignatureHashAlgorithm, cleartext, mNfcSignedHash, mNfcCreationTimestamp);
            } catch (PgpGeneralException e) {
                log.add(LogType.MSG_SE_ERROR_NFC, indent);
                return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log);
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
                                ? LogType.MSG_SE_SIGCRYPTING
                                : LogType.MSG_SE_ENCRYPTING,
                        indent
                );
                indent += 1;

                encryptionOut = cPk.open(out, new byte[1 << 16]);

                if (enableCompression) {
                    log.add(LogType.MSG_SE_COMPRESSING, indent);
                    compressGen = new PGPCompressedDataGenerator(mCompressionId);
                    bcpgOut = new BCPGOutputStream(compressGen.open(encryptionOut));
                } else {
                    bcpgOut = new BCPGOutputStream(encryptionOut);
                }

                if (enableSignature) {
                    signatureGenerator.generateOnePassVersion(false).encode(bcpgOut);
                }

                PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
                char literalDataFormatTag;
                if (mCleartextSignature) {
                    literalDataFormatTag = PGPLiteralData.UTF8;
                } else {
                    literalDataFormatTag = PGPLiteralData.BINARY;
                }
                pOut = literalGen.open(bcpgOut, literalDataFormatTag, mOriginalFilename, new Date(),
                        new byte[1 << 16]);

                long alreadyWritten = 0;
                int length;
                byte[] buffer = new byte[1 << 16];
                InputStream in = mData.getInputStream();
                while ((length = in.read(buffer)) > 0) {
                    pOut.write(buffer, 0, length);

                    // update signature buffer if signature is requested
                    if (enableSignature) {
                        signatureGenerator.update(buffer, 0, length);
                    }

                    alreadyWritten += length;
                    if (mData.getSize() > 0) {
                        long progress = 100 * alreadyWritten / mData.getSize();
                        progressScaler.setProgress((int) progress, 100);
                    }
                }

                literalGen.close();
                indent -= 1;

            } else if (enableSignature && mCleartextSignature && mEnableAsciiArmorOutput) {
                /* cleartext signature: sign-only of ascii text */

                updateProgress(R.string.progress_signing, 8, 100);
                log.add(LogType.MSG_SE_SIGNING, indent);

                // write -----BEGIN PGP SIGNED MESSAGE-----
                armorOut.beginClearText(mSignatureHashAlgorithm);

                InputStream in = mData.getInputStream();
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
            } else if (enableSignature && mDetachedSignature) {
                /* detached signature */

                updateProgress(R.string.progress_signing, 8, 100);
                log.add(LogType.MSG_SE_SIGNING, indent);

                InputStream in = mData.getInputStream();

                // handle output stream separately for detached signatures
                detachedByteOut = new ByteArrayOutputStream();
                OutputStream detachedOut = detachedByteOut;
                if (mEnableAsciiArmorOutput) {
                    detachedArmorOut = new ArmoredOutputStream(detachedOut);
                    if (mVersionHeader != null) {
                        detachedArmorOut.setHeader("Version", mVersionHeader);
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
                    if (mData.getSize() > 0) {
                        long progress = 100 * alreadyWritten / mData.getSize();
                        progressScaler.setProgress((int) progress, 100);
                    }
                }

                pOut = null;
            } else if (enableSignature && !mCleartextSignature && !mDetachedSignature) {
                /* sign-only binary (files/data stream) */

                updateProgress(R.string.progress_signing, 8, 100);
                log.add(LogType.MSG_SE_SIGNING, indent);

                InputStream in = mData.getInputStream();

                if (enableCompression) {
                    compressGen = new PGPCompressedDataGenerator(mCompressionId);
                    bcpgOut = new BCPGOutputStream(compressGen.open(out));
                } else {
                    bcpgOut = new BCPGOutputStream(out);
                }

                signatureGenerator.generateOnePassVersion(false).encode(bcpgOut);

                PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
                pOut = literalGen.open(bcpgOut, PGPLiteralData.BINARY, mOriginalFilename, new Date(),
                        new byte[1 << 16]);

                long alreadyWritten = 0;
                int length;
                byte[] buffer = new byte[1 << 16];
                while ((length = in.read(buffer)) > 0) {
                    pOut.write(buffer, 0, length);

                    signatureGenerator.update(buffer, 0, length);

                    alreadyWritten += length;
                    if (mData.getSize() > 0) {
                        long progress = 100 * alreadyWritten / mData.getSize();
                        progressScaler.setProgress((int) progress, 100);
                    }
                }

                literalGen.close();
            } else {
                pOut = null;
                // TODO: Is this log right?
                log.add(LogType.MSG_SE_CLEARSIGN_ONLY, indent);
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
                    log.add(LogType.MSG_SE_PENDING_NFC, indent);
                    SignEncryptResult result =
                            new SignEncryptResult(SignEncryptResult.RESULT_PENDING_NFC, log);
                    // Note that the checked key here is the master key, not the signing key
                    // (although these are always the same on Yubikeys)
                    result.setNfcData(mSignatureSubKeyId, e.hashToSign, e.hashAlgo, e.creationTimestamp, mSignaturePassphrase);
                    Log.d(Constants.TAG, "e.hashToSign" + Hex.toHexString(e.hashToSign));
                    return result;
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
            if (mOutStream != null) {
                mOutStream.close();
            }

        } catch (SignatureException e) {
            log.add(LogType.MSG_SE_ERROR_SIG, indent);
            return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log);
        } catch (PGPException e) {
            log.add(LogType.MSG_SE_ERROR_PGP, indent);
            return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log);
        } catch (IOException e) {
            log.add(LogType.MSG_SE_ERROR_IO, indent);
            return new SignEncryptResult(SignEncryptResult.RESULT_ERROR, log);
        }

        updateProgress(R.string.progress_done, 100, 100);

        log.add(LogType.MSG_SE_OK, indent);
        SignEncryptResult result = new SignEncryptResult(SignEncryptResult.RESULT_OK, log);
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
