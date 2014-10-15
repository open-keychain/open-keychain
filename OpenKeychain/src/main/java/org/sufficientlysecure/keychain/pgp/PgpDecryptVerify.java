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
import android.webkit.MimeTypeMap;

import org.openintents.openpgp.OpenPgpMetadata;
import org.spongycastle.bcpg.ArmoredInputStream;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPOnePassSignature;
import org.spongycastle.openpgp.PGPOnePassSignatureList;
import org.spongycastle.openpgp.PGPPBEEncryptedData;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.PBEDataDecryptorFactory;
import org.spongycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.spongycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.spongycastle.openpgp.operator.jcajce.NfcSyncPublicKeyDataDecryptorFactoryBuilder;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.BaseOperation;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.security.SignatureException;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * This class uses a Builder pattern!
 */
public class PgpDecryptVerify extends BaseOperation {

    private InputData mData;
    private OutputStream mOutStream;

    private boolean mAllowSymmetricDecryption;
    private String mPassphrase;
    private Set<Long> mAllowedKeyIds;
    private boolean mDecryptMetadataOnly;
    private byte[] mDecryptedSessionKey;

    private PgpDecryptVerify(Builder builder) {
        super(builder.mContext, builder.mProviderHelper, builder.mProgressable);

        // private Constructor can only be called from Builder
        this.mData = builder.mData;
        this.mOutStream = builder.mOutStream;

        this.mAllowSymmetricDecryption = builder.mAllowSymmetricDecryption;
        this.mPassphrase = builder.mPassphrase;
        this.mAllowedKeyIds = builder.mAllowedKeyIds;
        this.mDecryptMetadataOnly = builder.mDecryptMetadataOnly;
        this.mDecryptedSessionKey = builder.mDecryptedSessionKey;
    }

    public static class Builder {
        // mandatory parameter
        private Context mContext;
        private ProviderHelper mProviderHelper;
        private InputData mData;
        private OutputStream mOutStream;

        // optional
        private Progressable mProgressable = null;
        private boolean mAllowSymmetricDecryption = true;
        private String mPassphrase = null;
        private Set<Long> mAllowedKeyIds = null;
        private boolean mDecryptMetadataOnly = false;
        private byte[] mDecryptedSessionKey = null;

        public Builder(Context context, ProviderHelper providerHelper,
                       Progressable progressable,
                       InputData data, OutputStream outStream) {
            mContext = context;
            mProviderHelper = providerHelper;
            mProgressable = progressable;
            mData = data;
            mOutStream = outStream;
        }

        public Builder setAllowSymmetricDecryption(boolean allowSymmetricDecryption) {
            mAllowSymmetricDecryption = allowSymmetricDecryption;
            return this;
        }

        public Builder setPassphrase(String passphrase) {
            mPassphrase = passphrase;
            return this;
        }

        /**
         * Allow these key ids alone for decryption.
         * This means only ciphertexts encrypted for one of these private key can be decrypted.
         */
        public Builder setAllowedKeyIds(Set<Long> allowedKeyIds) {
            mAllowedKeyIds = allowedKeyIds;
            return this;
        }

        /**
         * If enabled, the actual decryption/verification of the content will not be executed.
         * The metadata only will be decrypted and returned.
         */
        public Builder setDecryptMetadataOnly(boolean decryptMetadataOnly) {
            mDecryptMetadataOnly = decryptMetadataOnly;
            return this;
        }

        public Builder setNfcState(byte[] decryptedSessionKey) {
            mDecryptedSessionKey = decryptedSessionKey;
            return this;
        }

        public PgpDecryptVerify build() {
            return new PgpDecryptVerify(this);
        }
    }

    /**
     * Decrypts and/or verifies data based on parameters of class
     */
    public DecryptVerifyResult execute() {
        try {
            // automatically works with ascii armor input and binary
            InputStream in = PGPUtil.getDecoderStream(mData.getInputStream());

            if (in instanceof ArmoredInputStream) {
                ArmoredInputStream aIn = (ArmoredInputStream) in;
                // it is ascii armored
                Log.d(Constants.TAG, "ASCII Armor Header Line: " + aIn.getArmorHeaderLine());

                if (aIn.isClearText()) {
                    // a cleartext signature, verify it with the other method
                    return verifyCleartextSignature(aIn, 0);
                }
                // else: ascii armored encryption! go on...
            }

            return decryptVerify(in, 0);
        } catch (PGPException e) {
            Log.d(Constants.TAG, "PGPException", e);
            OperationLog log = new OperationLog();
            log.add(LogType.MSG_DC_ERROR_PGP_EXCEPTION, 1);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        } catch (IOException e) {
            Log.d(Constants.TAG, "IOException", e);
            OperationLog log = new OperationLog();
            log.add(LogType.MSG_DC_ERROR_IO, 1);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }
    }

    /**
     * Decrypt and/or verifies binary or ascii armored pgp
     */
    private DecryptVerifyResult decryptVerify(InputStream in, int indent) throws IOException, PGPException {

        OperationLog log = new OperationLog();

        log.add(LogType.MSG_DC, indent);
        indent += 1;

        PGPObjectFactory pgpF = new PGPObjectFactory(in, new JcaKeyFingerprintCalculator());
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
            log.add(LogType.MSG_DC_ERROR_INVALID_SIGLIST, indent);
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
                    log.add(LogType.MSG_DC_ASKIP_NO_KEY, indent +1);
                    continue;
                }
                if (secretKeyRing == null) {
                    // continue with the next packet in the while loop
                    log.add(LogType.MSG_DC_ASKIP_NO_KEY, indent +1);
                    continue;
                }
                // get subkey which has been used for this encryption packet
                secretEncryptionKey = secretKeyRing.getSecretKey(subKeyId);
                if (secretEncryptionKey == null) {
                    // should actually never happen, so no need to be more specific.
                    log.add(LogType.MSG_DC_ASKIP_NO_KEY, indent +1);
                    continue;
                }

                // allow only specific keys for decryption?
                if (mAllowedKeyIds != null) {
                    long masterKeyId = secretKeyRing.getMasterKeyId();
                    Log.d(Constants.TAG, "encData.getKeyID(): " + subKeyId);
                    Log.d(Constants.TAG, "mAllowedKeyIds: " + mAllowedKeyIds);
                    Log.d(Constants.TAG, "masterKeyId: " + masterKeyId);

                    if (!mAllowedKeyIds.contains(masterKeyId)) {
                        // this key is in our db, but NOT allowed!
                        // continue with the next packet in the while loop
                        log.add(LogType.MSG_DC_ASKIP_NOT_ALLOWED, indent +1);
                        continue;
                    }
                }

                /* secret key exists in database and is allowed! */
                asymmetricPacketFound = true;

                encryptedDataAsymmetric = encData;

                // if no passphrase was explicitly set try to get it from the cache service
                if (mPassphrase == null) {
                    try {
                        // returns "" if key has no passphrase
                        mPassphrase = getCachedPassphrase(subKeyId);
                        log.add(LogType.MSG_DC_PASS_CACHED, indent +1);
                    } catch (PassphraseCacheInterface.NoSecretKeyException e) {
                        log.add(LogType.MSG_DC_ERROR_NO_KEY, indent +1);
                        return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
                    }

                    // if passphrase was not cached, return here indicating that a passphrase is missing!
                    if (mPassphrase == null) {
                        log.add(LogType.MSG_DC_PENDING_PASSPHRASE, indent +1);
                        DecryptVerifyResult result =
                                new DecryptVerifyResult(DecryptVerifyResult.RESULT_PENDING_ASYM_PASSPHRASE, log);
                        result.setKeyIdPassphraseNeeded(subKeyId);
                        return result;
                    }
                }

                // break out of while, only decrypt the first packet where we have a key
                break;

            } else if (obj instanceof PGPPBEEncryptedData) {
                anyPacketFound = true;

                log.add(LogType.MSG_DC_SYM, indent);

                if (! mAllowSymmetricDecryption) {
                    log.add(LogType.MSG_DC_SYM_SKIP, indent +1);
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
                if (mPassphrase == null) {
                    log.add(LogType.MSG_DC_PENDING_PASSPHRASE, indent +1);
                    return new DecryptVerifyResult(DecryptVerifyResult.RESULT_PENDING_SYM_PASSPHRASE, log);
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
        if (symmetricPacketFound) {
            currentProgress += 2;
            updateProgress(R.string.progress_preparing_streams, currentProgress, 100);

            PGPDigestCalculatorProvider digestCalcProvider = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build();
            PBEDataDecryptorFactory decryptorFactory = new JcePBEDataDecryptorFactoryBuilder(
                    digestCalcProvider).setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(
                    mPassphrase.toCharArray());

            clear = encryptedDataSymmetric.getDataStream(decryptorFactory);
            encryptedData = encryptedDataSymmetric;

        } else if (asymmetricPacketFound) {
            currentProgress += 2;
            updateProgress(R.string.progress_extracting_key, currentProgress, 100);

            try {
                log.add(LogType.MSG_DC_UNLOCKING, indent +1);
                if (!secretEncryptionKey.unlock(mPassphrase)) {
                    log.add(LogType.MSG_DC_ERROR_BAD_PASSPHRASE, indent +1);
                    return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
                }
            } catch (PgpGeneralException e) {
                log.add(LogType.MSG_DC_ERROR_EXTRACT_KEY, indent +1);
                return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
            }

            currentProgress += 2;
            updateProgress(R.string.progress_preparing_streams, currentProgress, 100);

            try {
                PublicKeyDataDecryptorFactory decryptorFactory
                        = secretEncryptionKey.getDecryptorFactory(mDecryptedSessionKey);
                clear = encryptedDataAsymmetric.getDataStream(decryptorFactory);
            } catch (NfcSyncPublicKeyDataDecryptorFactoryBuilder.NfcInteractionNeeded e) {
                log.add(LogType.MSG_DC_PENDING_NFC, indent +1);
                DecryptVerifyResult result =
                        new DecryptVerifyResult(DecryptVerifyResult.RESULT_PENDING_NFC, log);
                result.setNfcState(secretEncryptionKey.getKeyId(), e.encryptedSessionKey, mPassphrase);
                return result;
            }
            encryptedData = encryptedDataAsymmetric;
        } else {
            // If we didn't find any useful data, error out
            // no packet has been found where we have the corresponding secret key in our db
            log.add(
                    anyPacketFound ? LogType.MSG_DC_ERROR_NO_KEY : LogType.MSG_DC_ERROR_NO_DATA, indent +1);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }

        PGPObjectFactory plainFact = new PGPObjectFactory(clear, new JcaKeyFingerprintCalculator());
        Object dataChunk = plainFact.nextObject();
        OpenPgpSignatureResultBuilder signatureResultBuilder = new OpenPgpSignatureResultBuilder();
        int signatureIndex = -1;
        CanonicalizedPublicKeyRing signingRing = null;
        CanonicalizedPublicKey signingKey = null;

        log.add(LogType.MSG_DC_CLEAR, indent);
        indent += 1;

        if (dataChunk instanceof PGPCompressedData) {
            log.add(LogType.MSG_DC_CLEAR_DECOMPRESS, indent +1);
            currentProgress += 2;
            updateProgress(R.string.progress_decompressing_data, currentProgress, 100);

            PGPCompressedData compressedData = (PGPCompressedData) dataChunk;

            PGPObjectFactory fact = new PGPObjectFactory(compressedData.getDataStream(), new JcaKeyFingerprintCalculator());
            dataChunk = fact.nextObject();
            plainFact = fact;
        }

        PGPOnePassSignature signature = null;
        if (dataChunk instanceof PGPOnePassSignatureList) {
            log.add(LogType.MSG_DC_CLEAR_SIGNATURE, indent +1);
            currentProgress += 2;
            updateProgress(R.string.progress_processing_signature, currentProgress, 100);

            PGPOnePassSignatureList sigList = (PGPOnePassSignatureList) dataChunk;

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

            dataChunk = plainFact.nextObject();
        }

        if (dataChunk instanceof PGPSignatureList) {
            // skip
            dataChunk = plainFact.nextObject();
        }

        OpenPgpMetadata metadata;

        if (dataChunk instanceof PGPLiteralData) {
            log.add(LogType.MSG_DC_CLEAR_DATA, indent +1);
            indent += 2;
            currentProgress += 4;
            updateProgress(R.string.progress_decrypting, currentProgress, 100);

            PGPLiteralData literalData = (PGPLiteralData) dataChunk;

            // TODO: how to get the real original size?
            // this is the encrypted size so if we enable compression this value is wrong!
            long originalSize = mData.getSize() - mData.getStreamPosition();
            if (originalSize < 0) {
                originalSize = 0;
            }

            String originalFilename = literalData.getFileName();
            String mimeType = null;
            if (literalData.getFormat() == PGPLiteralData.TEXT
                    || literalData.getFormat() == PGPLiteralData.UTF8) {
                mimeType = "text/plain";
            } else {
                // TODO: better would be: https://github.com/open-keychain/open-keychain/issues/753

                // try to guess from file ending
                String extension = MimeTypeMap.getFileExtensionFromUrl(originalFilename);
                if (extension != null) {
                    MimeTypeMap mime = MimeTypeMap.getSingleton();
                    mimeType = mime.getMimeTypeFromExtension(extension);
                }
                if (mimeType == null) {
                    mimeType = URLConnection.guessContentTypeFromName(originalFilename);
                }
                if (mimeType == null) {
                    mimeType = "*/*";
                }
            }

            metadata = new OpenPgpMetadata(
                    originalFilename,
                    mimeType,
                    literalData.getModificationTime().getTime(),
                    originalSize);

            if ( ! originalFilename.equals("")) {
                log.add(LogType.MSG_DC_CLEAR_META_FILE, indent + 1, originalFilename);
            }
            log.add(LogType.MSG_DC_CLEAR_META_MIME, indent +1,
                    mimeType);
            log.add(LogType.MSG_DC_CLEAR_META_TIME, indent +1,
                    new Date(literalData.getModificationTime().getTime()).toString());
            if (originalSize != 0) {
                log.add(LogType.MSG_DC_CLEAR_META_SIZE, indent + 1,
                        Long.toString(originalSize));
            }

            // return here if we want to decrypt the metadata only
            if (mDecryptMetadataOnly) {
                log.add(LogType.MSG_DC_OK_META_ONLY, indent);
                DecryptVerifyResult result =
                        new DecryptVerifyResult(DecryptVerifyResult.RESULT_OK, log);
                result.setDecryptMetadata(metadata);
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
            long wholeSize = mData.getSize() - mData.getStreamPosition();
            int length;
            byte[] buffer = new byte[1 << 16];
            while ((length = dataIn.read(buffer)) > 0) {
                mOutStream.write(buffer, 0, length);

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
                } else {
                    // TODO: slow annealing to fake a progress?
                }
            }

            if (signature != null) {
                updateProgress(R.string.progress_verifying_signature, 90, 100);
                log.add(LogType.MSG_DC_CLEAR_SIGNATURE_CHECK, indent);

                PGPSignatureList signatureList = (PGPSignatureList) plainFact.nextObject();
                PGPSignature messageSignature = signatureList.get(signatureIndex);

                // these are not cleartext signatures!
                // TODO: what about binary signatures?
                signatureResultBuilder.setSignatureOnly(false);

                // Verify signature and check binding signatures
                boolean validSignature = signature.verify(messageSignature);
                if (validSignature) {
                    log.add(LogType.MSG_DC_CLEAR_SIGNATURE_OK, indent +1);
                } else {
                    log.add(LogType.MSG_DC_CLEAR_SIGNATURE_BAD, indent +1);
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
            if (!signatureResultBuilder.isValidSignature()) {
                log.add(LogType.MSG_DC_ERROR_INTEGRITY_CHECK, indent);
                return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
            }
        }

        updateProgress(R.string.progress_done, 100, 100);

        log.add(LogType.MSG_DC_OK, indent);

        // Return a positive result, with metadata and verification info
        DecryptVerifyResult result =
                new DecryptVerifyResult(DecryptVerifyResult.RESULT_OK, log);
        result.setDecryptMetadata(metadata);
        result.setSignatureResult(signatureResultBuilder.build());
        return result;

    }

    /**
     * This method verifies cleartext signatures
     * as defined in http://tools.ietf.org/html/rfc4880#section-7
     * <p/>
     * The method is heavily based on
     * pg/src/main/java/org/spongycastle/openpgp/examples/ClearSignedFileProcessor.java
     */
    private DecryptVerifyResult verifyCleartextSignature(ArmoredInputStream aIn, int indent)
            throws IOException, PGPException {

        OperationLog log = new OperationLog();

        OpenPgpSignatureResultBuilder signatureResultBuilder = new OpenPgpSignatureResultBuilder();
        // cleartext signatures are never encrypted ;)
        signatureResultBuilder.setSignatureOnly(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        updateProgress(R.string.progress_done, 0, 100);

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
        mOutStream.write(clearText);

        updateProgress(R.string.progress_processing_signature, 60, 100);
        PGPObjectFactory pgpFact = new PGPObjectFactory(aIn, new JcaKeyFingerprintCalculator());

        PGPSignatureList sigList = (PGPSignatureList) pgpFact.nextObject();
        if (sigList == null) {
            log.add(LogType.MSG_DC_ERROR_INVALID_SIGLIST, 0);
            return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
        }

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
                signatureResultBuilder.setValidSignature(validSignature);

            } catch (SignatureException e) {
                Log.d(Constants.TAG, "SignatureException", e);
                return new DecryptVerifyResult(DecryptVerifyResult.RESULT_ERROR, log);
            }
        }

        updateProgress(R.string.progress_done, 100, 100);

        log.add(LogType.MSG_DC_OK, indent);

        DecryptVerifyResult result = new DecryptVerifyResult(DecryptVerifyResult.RESULT_OK, log);
        result.setSignatureResult(signatureResultBuilder.build());
        return result;
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
                lookAhead = readPassedEOL(bOut, ch, fIn);
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
                lookAhead = readPassedEOL(bOut, ch, fIn);
                break;
            }
        } while ((ch = fIn.read()) >= 0);

        if (ch < 0) {
            lookAhead = -1;
        }

        return lookAhead;
    }

    private static int readPassedEOL(ByteArrayOutputStream bOut, int lastCh, InputStream fIn)
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
        byte[] nlBytes = new byte[nl.length()];

        for (int i = 0; i != nlBytes.length; i++) {
            nlBytes[i] = (byte) nl.charAt(i);
        }

        return nlBytes;
    }
}
