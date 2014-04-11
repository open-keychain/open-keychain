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
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedDataGenerator;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPV3SignatureGenerator;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBEKeyEncryptionMethodGenerator;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ProgressDialogUpdater;

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

/**
 * This class uses a Builder pattern!
 */
public class PgpSignEncrypt {
    private ProviderHelper mProviderHelper;
    private String mVersionHeader;
    private InputData mData;
    private OutputStream mOutStream;

    private ProgressDialogUpdater mProgress;
    private boolean mEnableAsciiArmorOutput;
    private int mCompressionId;
    private long[] mEncryptionMasterKeyIds;
    private String mSymmetricPassphrase;
    private int mSymmetricEncryptionAlgorithm;
    private long mSignatureMasterKeyId;
    private int mSignatureHashAlgorithm;
    private boolean mSignatureForceV3;
    private String mSignaturePassphrase;
    private boolean mEncryptToSigner;
    private boolean mBinaryInput;

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

        this.mProgress = builder.mProgress;
        this.mEnableAsciiArmorOutput = builder.mEnableAsciiArmorOutput;
        this.mCompressionId = builder.mCompressionId;
        this.mEncryptionMasterKeyIds = builder.mEncryptionMasterKeyIds;
        this.mSymmetricPassphrase = builder.mSymmetricPassphrase;
        this.mSymmetricEncryptionAlgorithm = builder.mSymmetricEncryptionAlgorithm;
        this.mSignatureMasterKeyId = builder.mSignatureMasterKeyId;
        this.mSignatureHashAlgorithm = builder.mSignatureHashAlgorithm;
        this.mSignatureForceV3 = builder.mSignatureForceV3;
        this.mSignaturePassphrase = builder.mSignaturePassphrase;
        this.mEncryptToSigner = builder.mEncryptToSigner;
        this.mBinaryInput = builder.mBinaryInput;
    }

    public static class Builder {
        // mandatory parameter
        private ProviderHelper mProviderHelper;
        private String mVersionHeader;
        private InputData mData;
        private OutputStream mOutStream;

        // optional
        private ProgressDialogUpdater mProgress = null;
        private boolean mEnableAsciiArmorOutput = false;
        private int mCompressionId = Id.choice.compression.none;
        private long[] mEncryptionMasterKeyIds = null;
        private String mSymmetricPassphrase = null;
        private int mSymmetricEncryptionAlgorithm = 0;
        private long mSignatureMasterKeyId = Id.key.none;
        private int mSignatureHashAlgorithm = 0;
        private boolean mSignatureForceV3 = false;
        private String mSignaturePassphrase = null;
        private boolean mEncryptToSigner = false;
        private boolean mBinaryInput = false;

        public Builder(ProviderHelper providerHelper, String versionHeader, InputData data, OutputStream outStream) {
            this.mProviderHelper = providerHelper;
            this.mVersionHeader = versionHeader;
            this.mData = data;
            this.mOutStream = outStream;
        }

        public Builder progress(ProgressDialogUpdater progress) {
            this.mProgress = progress;
            return this;
        }

        public Builder enableAsciiArmorOutput(boolean enableAsciiArmorOutput) {
            this.mEnableAsciiArmorOutput = enableAsciiArmorOutput;
            return this;
        }

        public Builder compressionId(int compressionId) {
            this.mCompressionId = compressionId;
            return this;
        }

        public Builder encryptionMasterKeyIds(long[] encryptionMasterKeyIds) {
            this.mEncryptionMasterKeyIds = encryptionMasterKeyIds;
            return this;
        }

        public Builder symmetricPassphrase(String symmetricPassphrase) {
            this.mSymmetricPassphrase = symmetricPassphrase;
            return this;
        }

        public Builder symmetricEncryptionAlgorithm(int symmetricEncryptionAlgorithm) {
            this.mSymmetricEncryptionAlgorithm = symmetricEncryptionAlgorithm;
            return this;
        }

        public Builder signatureMasterKeyId(long signatureMasterKeyId) {
            this.mSignatureMasterKeyId = signatureMasterKeyId;
            return this;
        }

        public Builder signatureHashAlgorithm(int signatureHashAlgorithm) {
            this.mSignatureHashAlgorithm = signatureHashAlgorithm;
            return this;
        }

        public Builder signatureForceV3(boolean signatureForceV3) {
            this.mSignatureForceV3 = signatureForceV3;
            return this;
        }

        public Builder signaturePassphrase(String signaturePassphrase) {
            this.mSignaturePassphrase = signaturePassphrase;
            return this;
        }

        /**
         * TODO: test this option!
         *
         * @param encryptToSigner
         * @return
         */
        public Builder encryptToSigner(boolean encryptToSigner) {
            this.mEncryptToSigner = encryptToSigner;
            return this;
        }

        /**
         * TODO: test this option!
         *
         * @param binaryInput
         * @return
         */
        public Builder binaryInput(boolean binaryInput) {
            this.mBinaryInput = binaryInput;
            return this;
        }

        public PgpSignEncrypt build() {
            return new PgpSignEncrypt(this);
        }
    }

    public void updateProgress(int message, int current, int total) {
        if (mProgress != null) {
            mProgress.setProgress(message, current, total);
        }
    }

    public void updateProgress(int current, int total) {
        if (mProgress != null) {
            mProgress.setProgress(current, total);
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

    public static class NoSigningKeyException extends Exception {
        public NoSigningKeyException() {
        }
    }

    /**
     * Signs and/or encrypts data based on parameters of class
     */
    public void execute()
            throws IOException, PGPException, NoSuchProviderException,
            NoSuchAlgorithmException, SignatureException, KeyExtractionException, NoSigningKeyException, NoPassphraseException {

        boolean enableSignature = mSignatureMasterKeyId != Id.key.none;
        boolean enableEncryption = ((mEncryptionMasterKeyIds != null && mEncryptionMasterKeyIds.length > 0)
                || mSymmetricPassphrase != null);
        boolean enableCompression = (enableEncryption && mCompressionId != Id.choice.compression.none);

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
            armorOut.setHeader("Version", mVersionHeader);
            out = armorOut;
        } else {
            out = mOutStream;
        }

        /* Get keys for signature generation for later usage */
        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;
        if (enableSignature) {
            try {
                signingKeyRing = mProviderHelper.getPGPSecretKeyRing(mSignatureMasterKeyId);
            } catch (ProviderHelper.NotFoundException e) {
                throw new NoSigningKeyException();
//                throw new PgpGeneralException(mContext.getString(R.string.error_signature_failed));
            }
            signingKey = PgpKeyHelper.getSigningKey(signingKeyRing);
            if (signingKey == null) {
                throw new NoSigningKeyException();
//                throw new PgpGeneralException(mContext.getString(R.string.error_signature_failed));
            }

            if (mSignaturePassphrase == null) {
//                throw new PgpGeneralException(
//                        mContext.getString(R.string.error_no_signature_passphrase));
                throw new NoPassphraseException();
            }

            updateProgress(R.string.progress_extracting_signature_key, 0, 100);

            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                    Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(mSignaturePassphrase.toCharArray());
            signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
            if (signaturePrivateKey == null) {
//                throw new PgpGeneralException(
//                        mContext.getString(R.string.error_could_not_extract_private_key));
                throw new KeyExtractionException();
            }
        }
        updateProgress(R.string.progress_preparing_streams, 5, 100);

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
                        PGPPublicKeyRing keyRing = mProviderHelper.getPGPPublicKeyRing(id);
                        PGPPublicKey key = PgpKeyHelper.getEncryptPublicKey(keyRing);
                        if (key != null) {
                            JcePublicKeyKeyEncryptionMethodGenerator pubKeyEncryptionGenerator =
                                    new JcePublicKeyKeyEncryptionMethodGenerator(key);
                            cPk.addMethod(pubKeyEncryptionGenerator);
                        }
                    } catch (ProviderHelper.NotFoundException e) {
                        Log.e(Constants.TAG, "key not found!", e);
                    }
                }
            }
        }

        /* Initialize signature generator object for later usage */
        PGPSignatureGenerator signatureGenerator = null;
        PGPV3SignatureGenerator signatureV3Generator = null;
        if (enableSignature) {
            updateProgress(R.string.progress_preparing_signature, 10, 100);

            // content signer based on signing key algorithm and chosen hash algorithm
            JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(
                    signingKey.getPublicKey().getAlgorithm(), mSignatureHashAlgorithm)
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

            int signatureType;
            if (mEnableAsciiArmorOutput && !enableEncryption) {
                // for sign-only ascii text and sign-only binary input (files)
                signatureType = PGPSignature.CANONICAL_TEXT_DOCUMENT;
            } else {
                signatureType = PGPSignature.BINARY_DOCUMENT;
            }

            if (mSignatureForceV3) {
                signatureV3Generator = new PGPV3SignatureGenerator(contentSignerBuilder);
                signatureV3Generator.init(signatureType, signaturePrivateKey);
            } else {
                signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
                signatureGenerator.init(signatureType, signaturePrivateKey);

                String userId = PgpKeyHelper.getMainUserId(signingKeyRing.getSecretKey());
                PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
                spGen.setSignerUserID(false, userId);
                signatureGenerator.setHashedSubpackets(spGen.generate());
            }
        }

        PGPCompressedDataGenerator compressGen = null;
        OutputStream pOut;
        OutputStream encryptionOut = null;
        BCPGOutputStream bcpgOut;
        if (enableEncryption) {
            /* actual encryption */

            encryptionOut = cPk.open(out, new byte[1 << 16]);

            if (enableCompression) {
                compressGen = new PGPCompressedDataGenerator(mCompressionId);
                bcpgOut = new BCPGOutputStream(compressGen.open(encryptionOut));
            } else {
                bcpgOut = new BCPGOutputStream(encryptionOut);
            }

            if (enableSignature) {
                if (mSignatureForceV3) {
                    signatureV3Generator.generateOnePassVersion(false).encode(bcpgOut);
                } else {
                    signatureGenerator.generateOnePassVersion(false).encode(bcpgOut);
                }
            }

            PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
            // file name not needed, so empty string
            pOut = literalGen.open(bcpgOut, PGPLiteralData.BINARY, "", new Date(),
                    new byte[1 << 16]);
            updateProgress(R.string.progress_encrypting, 20, 100);

            long progress = 0;
            int n;
            byte[] buffer = new byte[1 << 16];
            InputStream in = mData.getInputStream();
            while ((n = in.read(buffer)) > 0) {
                pOut.write(buffer, 0, n);

                // update signature buffer if signature is requested
                if (enableSignature) {
                    if (mSignatureForceV3) {
                        signatureV3Generator.update(buffer, 0, n);
                    } else {
                        signatureGenerator.update(buffer, 0, n);
                    }
                }

                progress += n;
                if (mData.getSize() != 0) {
                    updateProgress((int) (20 + (95 - 20) * progress / mData.getSize()), 100);
                }
            }

            literalGen.close();
        } else if (!mBinaryInput && mEnableAsciiArmorOutput && enableSignature && !enableEncryption && !enableCompression) {
            /* cleartext signature: sign-only of ascii text */

            updateProgress(R.string.progress_signing, 40, 100);

            // write -----BEGIN PGP SIGNED MESSAGE-----
            armorOut.beginClearText(mSignatureHashAlgorithm);

            InputStream in = mData.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            // update signature buffer with first line
            if (mSignatureForceV3) {
                processLineV3(reader.readLine(), armorOut, signatureV3Generator);
            } else {
                processLine(reader.readLine(), armorOut, signatureGenerator);
            }

            while (true) {
                String line = reader.readLine();

                // end cleartext signature with newline, see http://tools.ietf.org/html/rfc4880#section-7
                if (line == null) {
                    armorOut.write(NEW_LINE);
                    break;
                }

                armorOut.write(NEW_LINE);

                // update signature buffer with input line
                if (mSignatureForceV3) {
                    signatureV3Generator.update(NEW_LINE);
                    processLineV3(line, armorOut, signatureV3Generator);
                } else {
                    signatureGenerator.update(NEW_LINE);
                    processLine(line, armorOut, signatureGenerator);
                }
            }

            armorOut.endClearText();


            pOut = new BCPGOutputStream(armorOut);
        } else if (mBinaryInput && enableSignature && !enableEncryption && !enableCompression) {
            // TODO: This part of the code is not tested!!!
            /* sign-only binaries (files) */

            updateProgress(R.string.progress_signing, 40, 100);

            InputStream in = mData.getInputStream();
            if (mEnableAsciiArmorOutput) {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (mSignatureForceV3) {
                        processLineV3(line, null, signatureV3Generator);
                        signatureV3Generator.update(NEW_LINE);
                    } else {
                        processLine(line, null, signatureGenerator);
                        signatureGenerator.update(NEW_LINE);
                    }
                }
            } else {
                byte[] buffer = new byte[1 << 16];
                int n;
                while ((n = in.read(buffer)) > 0) {
                    if (mSignatureForceV3) {
                        signatureV3Generator.update(buffer, 0, n);
                    } else {
                        signatureGenerator.update(buffer, 0, n);
                    }
                }
            }

            pOut = new BCPGOutputStream(out);
        } else {
            pOut = null;
            Log.e(Constants.TAG, "not supported!");
        }

        if (enableSignature) {
            updateProgress(R.string.progress_generating_signature, 95, 100);
            if (mSignatureForceV3) {
                signatureV3Generator.generate().encode(pOut);
            } else {
                signatureGenerator.generate().encode(pOut);
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

    private static void processLineV3(final String pLine, final ArmoredOutputStream pArmoredOutput,
                                      final PGPV3SignatureGenerator pSignatureGenerator)
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
