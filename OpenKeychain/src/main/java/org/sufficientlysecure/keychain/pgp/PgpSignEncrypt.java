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
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

/**
 * This class uses a Builder pattern!
 */
public class PgpSignEncrypt {
    private ProviderHelper mProviderHelper;
    private String mVersionHeader;
    private InputData mData;
    private OutputStream mOutStream;

    private Progressable mProgressable;
    private boolean mEnableAsciiArmorOutput;
    private int mCompressionId;
    private long[] mEncryptionMasterKeyIds;
    private String mSymmetricPassphrase;
    private int mSymmetricEncryptionAlgorithm;
    private long mSignatureMasterKeyId;
    private int mSignatureHashAlgorithm;
    private String mSignaturePassphrase;
    private boolean mEncryptToSigner;
    private boolean mCleartextInput;
    private String mOriginalFilename;

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

    private PgpSignEncrypt(Builder builder) {
        // private Constructor can only be called from Builder
        this.mProviderHelper = builder.mProviderHelper;
        this.mVersionHeader = builder.mVersionHeader;
        this.mData = builder.mData;
        this.mOutStream = builder.mOutStream;

        this.mProgressable = builder.mProgressable;
        this.mEnableAsciiArmorOutput = builder.mEnableAsciiArmorOutput;
        this.mCompressionId = builder.mCompressionId;
        this.mEncryptionMasterKeyIds = builder.mEncryptionMasterKeyIds;
        this.mSymmetricPassphrase = builder.mSymmetricPassphrase;
        this.mSymmetricEncryptionAlgorithm = builder.mSymmetricEncryptionAlgorithm;
        this.mSignatureMasterKeyId = builder.mSignatureMasterKeyId;
        this.mSignatureHashAlgorithm = builder.mSignatureHashAlgorithm;
        this.mSignaturePassphrase = builder.mSignaturePassphrase;
        this.mEncryptToSigner = builder.mEncryptToSigner;
        this.mCleartextInput = builder.mCleartextInput;
        this.mNfcSignedHash = builder.mNfcSignedHash;
        this.mNfcCreationTimestamp = builder.mNfcCreationTimestamp;
        this.mOriginalFilename = builder.mOriginalFilename;
    }

    public static class Builder {
        // mandatory parameter
        private ProviderHelper mProviderHelper;
        private InputData mData;
        private OutputStream mOutStream;

        // optional
        private String mVersionHeader = null;
        private Progressable mProgressable = null;
        private boolean mEnableAsciiArmorOutput = false;
        private int mCompressionId = CompressionAlgorithmTags.UNCOMPRESSED;
        private long[] mEncryptionMasterKeyIds = null;
        private String mSymmetricPassphrase = null;
        private int mSymmetricEncryptionAlgorithm = 0;
        private long mSignatureMasterKeyId = Constants.key.none;
        private int mSignatureHashAlgorithm = 0;
        private String mSignaturePassphrase = null;
        private boolean mEncryptToSigner = false;
        private boolean mCleartextInput = false;
        private String mOriginalFilename = "";
        private byte[] mNfcSignedHash = null;
        private Date mNfcCreationTimestamp = null;

        public Builder(ProviderHelper providerHelper, InputData data, OutputStream outStream) {
            mProviderHelper = providerHelper;
            mData = data;
            mOutStream = outStream;
        }

        public Builder setVersionHeader(String versionHeader) {
            mVersionHeader = versionHeader;
            return this;
        }

        public Builder setProgressable(Progressable progressable) {
            mProgressable = progressable;
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
            this.mSignatureMasterKeyId = signatureMasterKeyId;
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

        /**
         * Also encrypt with the signing keyring
         *
         * @param encryptToSigner
         * @return
         */
        public Builder setEncryptToSigner(boolean encryptToSigner) {
            mEncryptToSigner = encryptToSigner;
            return this;
        }

        /**
         * TODO: test this option!
         *
         * @param cleartextInput
         * @return
         */
        public Builder setCleartextInput(boolean cleartextInput) {
            mCleartextInput = cleartextInput;
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

    public void updateProgress(int message, int current, int total) {
        if (mProgressable != null) {
            mProgressable.setProgress(message, current, total);
        }
    }

    public void updateProgress(int current, int total) {
        if (mProgressable != null) {
            mProgressable.setProgress(current, total);
        }
    }

    public static class KeyExtractionException extends Exception {
        public KeyExtractionException() {
        }
    }

    public static class NoPassphraseException extends Exception {
        public NoPassphraseException() {
        }
    }

    public static class WrongPassphraseException extends Exception {
        public WrongPassphraseException() {
        }
    }

    public static class NoSigningKeyException extends Exception {
        public NoSigningKeyException() {
        }
    }

    public static class NeedNfcDataException extends Exception {
        public byte[] mHashToSign;
        public int mHashAlgo;
        public Date mCreationTimestamp;

        public NeedNfcDataException(byte[] hashToSign, int hashAlgo, Date creationTimestamp) {
            mHashToSign = hashToSign;
            mHashAlgo = hashAlgo;
            mCreationTimestamp = creationTimestamp;
        }
    }

    /**
     * Signs and/or encrypts data based on parameters of class
     */
    public void execute()
            throws IOException, PGPException, NoSuchProviderException,
            NoSuchAlgorithmException, SignatureException, KeyExtractionException, NoSigningKeyException, NoPassphraseException, NeedNfcDataException, WrongPassphraseException {

        boolean enableSignature = mSignatureMasterKeyId != Constants.key.none;
        boolean enableEncryption = ((mEncryptionMasterKeyIds != null && mEncryptionMasterKeyIds.length > 0)
                || mSymmetricPassphrase != null);
        boolean enableCompression = (mCompressionId != CompressionAlgorithmTags.UNCOMPRESSED);

        Log.d(Constants.TAG, "enableSignature:" + enableSignature
                + "\nenableEncryption:" + enableEncryption
                + "\nenableCompression:" + enableCompression
                + "\nenableAsciiArmorOutput:" + mEnableAsciiArmorOutput);

        // add signature key id to encryption ids (self-encrypt)
        if (enableEncryption && enableSignature && mEncryptToSigner) {
            mEncryptionMasterKeyIds = Arrays.copyOf(mEncryptionMasterKeyIds, mEncryptionMasterKeyIds.length + 1);
            mEncryptionMasterKeyIds[mEncryptionMasterKeyIds.length - 1] = mSignatureMasterKeyId;
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
        if (enableSignature) {
            CanonicalizedSecretKeyRing signingKeyRing;
            try {
                signingKeyRing = mProviderHelper.getCanonicalizedSecretKeyRing(mSignatureMasterKeyId);
            } catch (ProviderHelper.NotFoundException e) {
                throw new NoSigningKeyException();
            }
            try {
                signingKey = signingKeyRing.getSigningSubKey();
            } catch (PgpGeneralException e) {
                throw new NoSigningKeyException();
            }

            if (mSignaturePassphrase == null) {
                throw new NoPassphraseException();
            }

            updateProgress(R.string.progress_extracting_signature_key, 0, 100);

            try {
                if (!signingKey.unlock(mSignaturePassphrase)) {
                    throw new WrongPassphraseException();
                }
            } catch (PgpGeneralException e) {
                throw new KeyExtractionException();
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
                Log.d(Constants.TAG, "encryptionMasterKeyIds length is 0 -> symmetric encryption");

                JcePBEKeyEncryptionMethodGenerator symmetricEncryptionGenerator =
                        new JcePBEKeyEncryptionMethodGenerator(mSymmetricPassphrase.toCharArray());
                cPk.addMethod(symmetricEncryptionGenerator);
            } else {
                // Asymmetric encryption
                for (long id : mEncryptionMasterKeyIds) {
                    try {
                        CanonicalizedPublicKeyRing keyRing = mProviderHelper.getCanonicalizedPublicKeyRing(
                                KeyRings.buildUnifiedKeyRingUri(id));
                        CanonicalizedPublicKey key = keyRing.getEncryptionSubKey();
                        cPk.addMethod(key.getPubKeyEncryptionGenerator());
                    } catch (PgpGeneralException e) {
                        Log.e(Constants.TAG, "key not found!", e);
                    } catch (ProviderHelper.NotFoundException e) {
                        Log.e(Constants.TAG, "key not found!", e);
                    }
                }
            }
        }

        /* Initialize signature generator object for later usage */
        PGPSignatureGenerator signatureGenerator = null;
        if (enableSignature) {
            updateProgress(R.string.progress_preparing_signature, 4, 100);

            try {
                boolean cleartext = mCleartextInput && mEnableAsciiArmorOutput && !enableEncryption;
                signatureGenerator = signingKey.getSignatureGenerator(
                        mSignatureHashAlgorithm, cleartext, mNfcSignedHash, mNfcCreationTimestamp);
            } catch (PgpGeneralException e) {
                // TODO throw correct type of exception (which shouldn't be PGPException)
                throw new KeyExtractionException();
            }
        }

        ProgressScaler progressScaler =
                new ProgressScaler(mProgressable, 8, 95, 100);
        PGPCompressedDataGenerator compressGen = null;
        OutputStream pOut = null;
        OutputStream encryptionOut = null;
        BCPGOutputStream bcpgOut;

        if (enableEncryption) {
            /* actual encryption */
            updateProgress(R.string.progress_encrypting, 8, 100);

            encryptionOut = cPk.open(out, new byte[1 << 16]);

            if (enableCompression) {
                compressGen = new PGPCompressedDataGenerator(mCompressionId);
                bcpgOut = new BCPGOutputStream(compressGen.open(encryptionOut));
            } else {
                bcpgOut = new BCPGOutputStream(encryptionOut);
            }

            if (enableSignature) {
                signatureGenerator.generateOnePassVersion(false).encode(bcpgOut);
            }

            PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
            pOut = literalGen.open(bcpgOut, PGPLiteralData.BINARY, mOriginalFilename, new Date(),
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
        } else if (enableSignature && mCleartextInput && mEnableAsciiArmorOutput) {
            /* cleartext signature: sign-only of ascii text */

            updateProgress(R.string.progress_signing, 8, 100);

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
        } else if (enableSignature && !mCleartextInput) {
            /* sign-only binary (files/data stream) */

            updateProgress(R.string.progress_signing, 8, 100);

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
            Log.e(Constants.TAG, "not supported!");
        }

        if (enableSignature) {
            updateProgress(R.string.progress_generating_signature, 95, 100);
            try {
                signatureGenerator.generate().encode(pOut);
            } catch (NfcSyncPGPContentSignerBuilder.NfcInteractionNeeded e) {
                // this secret key diverts to a OpenPGP card, throw exception with hash that will be signed
                throw new NeedNfcDataException(e.hashToSign, e.hashAlgo, e.creationTimestamp);
            }
        }

        // closing outputs
        // NOTE: closing needs to be done in the correct order!
        // TODO: closing bcpgOut and pOut???
        if (enableEncryption) {
            if (enableCompression) {
                compressGen.close();
            }

            encryptionOut.close();
        }
        if (mEnableAsciiArmorOutput) {
            armorOut.close();
        }

        out.close();
        mOutStream.close();

        updateProgress(R.string.progress_done, 100, 100);
    }

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
