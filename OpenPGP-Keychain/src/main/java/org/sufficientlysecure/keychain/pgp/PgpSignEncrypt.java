/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.pgp;

import android.content.Context;
import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.BCPGOutputStream;
import org.spongycastle.openpgp.*;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.jcajce.*;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ProgressDialogUpdater;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Date;

/**
 * This class uses a Builder pattern!
 */
public class PgpSignEncrypt {
    private Context mContext;
    private InputData mData;
    private OutputStream mOutStream;

    private ProgressDialogUpdater mProgress;
    private boolean mEnableAsciiArmorOutput;
    private int mCompressionId;
    private long[] mEncryptionKeyIds;
    private String mEncryptionPassphrase;
    private int mSymmetricEncryptionAlgorithm;
    private long mSignatureKeyId;
    private int mSignatureHashAlgorithm;
    private boolean mSignatureForceV3;
    private String mSignaturePassphrase;

    private PgpSignEncrypt(Builder builder) {
        // private Constructor can only be called from Builder
        this.mContext = builder.mContext;
        this.mData = builder.mData;
        this.mOutStream = builder.mOutStream;

        this.mProgress = builder.mProgress;
        this.mEnableAsciiArmorOutput = builder.mEnableAsciiArmorOutput;
        this.mCompressionId = builder.mCompressionId;
        this.mEncryptionKeyIds = builder.mEncryptionKeyIds;
        this.mEncryptionPassphrase = builder.mEncryptionPassphrase;
        this.mSymmetricEncryptionAlgorithm = builder.mSymmetricEncryptionAlgorithm;
        this.mSignatureKeyId = builder.mSignatureKeyId;
        this.mSignatureHashAlgorithm = builder.mSignatureHashAlgorithm;
        this.mSignatureForceV3 = builder.mSignatureForceV3;
        this.mSignaturePassphrase = builder.mSignaturePassphrase;
    }

    public static class Builder {
        // mandatory parameter
        private Context mContext;
        private InputData mData;
        private OutputStream mOutStream;

        // optional
        private ProgressDialogUpdater mProgress = null;
        private boolean mEnableAsciiArmorOutput = false;
        private int mCompressionId = Id.choice.compression.none;
        private long[] mEncryptionKeyIds = new long[0];
        private String mEncryptionPassphrase = null;
        private int mSymmetricEncryptionAlgorithm = 0;
        private long mSignatureKeyId = Id.key.none;
        private int mSignatureHashAlgorithm = 0;
        private boolean mSignatureForceV3 = false;
        private String mSignaturePassphrase = null;

        public Builder(Context context, InputData data, OutputStream outStream) {
            this.mContext = context;
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

        public Builder encryptionKeyIds(long[] encryptionKeyIds) {
            this.mEncryptionKeyIds = encryptionKeyIds;
            return this;
        }

        public Builder encryptionPassphrase(String encryptionPassphrase) {
            this.mEncryptionPassphrase = encryptionPassphrase;
            return this;
        }

        public Builder symmetricEncryptionAlgorithm(int symmetricEncryptionAlgorithm) {
            this.mSymmetricEncryptionAlgorithm = symmetricEncryptionAlgorithm;
            return this;
        }

        public Builder signatureKeyId(long signatureKeyId) {
            this.mSignatureKeyId = signatureKeyId;
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

    /**
     * Signs and/or encrypts data based on parameters of class
     *
     * @throws IOException
     * @throws PgpGeneralException
     * @throws PGPException
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     */
    public void execute()
            throws IOException, PgpGeneralException, PGPException, NoSuchProviderException,
            NoSuchAlgorithmException, SignatureException {

        boolean enableSignature = mSignatureKeyId != Id.key.none;
        boolean enableEncryption = (mEncryptionKeyIds.length != 0 || mEncryptionPassphrase != null);
        boolean enableCompression = (enableEncryption && mCompressionId != Id.choice.compression.none);

        Log.d(Constants.TAG, "enableSignature:" + enableSignature
                + "\nenableEncryption:" + enableEncryption
                + "\nenableCompression:" + enableCompression
                + "\nenableAsciiArmorOutput:" + mEnableAsciiArmorOutput);

        int signatureType;
        if (mEnableAsciiArmorOutput && enableSignature && !enableEncryption && !enableCompression) {
            // for sign-only ascii text
            signatureType = PGPSignature.CANONICAL_TEXT_DOCUMENT;
        } else {
            signatureType = PGPSignature.BINARY_DOCUMENT;
        }

        ArmoredOutputStream armorOut = null;
        OutputStream out;
        if (mEnableAsciiArmorOutput) {
            armorOut = new ArmoredOutputStream(mOutStream);
            armorOut.setHeader("Version", PgpHelper.getFullVersion(mContext));
            out = armorOut;
        } else {
            out = mOutStream;
        }

        /* Get keys for signature generation for later usage */
        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;
        if (enableSignature) {
            signingKeyRing = ProviderHelper.getPGPSecretKeyRingByKeyId(mContext, mSignatureKeyId);
            signingKey = PgpKeyHelper.getSigningKey(mContext, mSignatureKeyId);
            if (signingKey == null) {
                throw new PgpGeneralException(mContext.getString(R.string.error_signature_failed));
            }

            if (mSignaturePassphrase == null) {
                throw new PgpGeneralException(
                        mContext.getString(R.string.error_no_signature_passphrase));
            }

            updateProgress(R.string.progress_extracting_signature_key, 0, 100);

            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                    Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(mSignaturePassphrase.toCharArray());
            signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
            if (signaturePrivateKey == null) {
                throw new PgpGeneralException(
                        mContext.getString(R.string.error_could_not_extract_private_key));
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

            if (mEncryptionKeyIds.length == 0) {
                // Symmetric encryption
                Log.d(Constants.TAG, "encryptionKeyIds length is 0 -> symmetric encryption");

                JcePBEKeyEncryptionMethodGenerator symmetricEncryptionGenerator =
                        new JcePBEKeyEncryptionMethodGenerator(mEncryptionPassphrase.toCharArray());
                cPk.addMethod(symmetricEncryptionGenerator);
            } else {
                // Asymmetric encryption
                for (long id : mEncryptionKeyIds) {
                    PGPPublicKey key = PgpKeyHelper.getEncryptPublicKey(mContext, id);
                    if (key != null) {
                        JcePublicKeyKeyEncryptionMethodGenerator pubKeyEncryptionGenerator =
                                new JcePublicKeyKeyEncryptionMethodGenerator(key);
                        cPk.addMethod(pubKeyEncryptionGenerator);
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

            if (mSignatureForceV3) {
                signatureV3Generator = new PGPV3SignatureGenerator(contentSignerBuilder);
                signatureV3Generator.init(signatureType, signaturePrivateKey);
            } else {
                signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
                signatureGenerator.init(signatureType, signaturePrivateKey);

                String userId = PgpKeyHelper.getMainUserId(PgpKeyHelper.getMasterKey(signingKeyRing));
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
        } else if (mEnableAsciiArmorOutput && enableSignature && !enableEncryption && !enableCompression) {
            /* sign-only of ascii text */

            updateProgress(R.string.progress_signing, 40, 100);

            // write directly on armor output stream
            armorOut.beginClearText(mSignatureHashAlgorithm);

            InputStream in = mData.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            final byte[] newline = "\r\n".getBytes("UTF-8");

            if (mSignatureForceV3) {
                processLine(reader.readLine(), armorOut, signatureV3Generator);
            } else {
                processLine(reader.readLine(), armorOut, signatureGenerator);
            }

            while (true) {
                String line = reader.readLine();

                if (line == null) {
                    armorOut.write(newline);
                    break;
                }

                armorOut.write(newline);

                // update signature buffer with input line
                if (mSignatureForceV3) {
                    signatureV3Generator.update(newline);
                    processLine(line, armorOut, signatureV3Generator);
                } else {
                    signatureGenerator.update(newline);
                    processLine(line, armorOut, signatureGenerator);
                }
            }

            armorOut.endClearText();

            pOut = new BCPGOutputStream(armorOut);
        } else {
            // TODO: implement sign-only for files!
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

    // TODO: merge this into execute method!
    // TODO: allow binary input for this class
    public void generateSignature()
            throws PgpGeneralException, PGPException, IOException, NoSuchAlgorithmException,
            SignatureException {

        OutputStream out;
        if (mEnableAsciiArmorOutput) {
            // Ascii Armor (Radix-64)
            ArmoredOutputStream armorOut = new ArmoredOutputStream(mOutStream);
            armorOut.setHeader("Version", PgpHelper.getFullVersion(mContext));
            out = armorOut;
        } else {
            out = mOutStream;
        }

        if (mSignatureKeyId == 0) {
            throw new PgpGeneralException(mContext.getString(R.string.error_no_signature_key));
        }

        PGPSecretKeyRing signingKeyRing =
                ProviderHelper.getPGPSecretKeyRingByKeyId(mContext, mSignatureKeyId);
        PGPSecretKey signingKey = PgpKeyHelper.getSigningKey(mContext, mSignatureKeyId);
        if (signingKey == null) {
            throw new PgpGeneralException(mContext.getString(R.string.error_signature_failed));
        }

        if (mSignaturePassphrase == null) {
            throw new PgpGeneralException(mContext.getString(R.string.error_no_signature_passphrase));
        }

        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(mSignaturePassphrase.toCharArray());
        PGPPrivateKey signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
        if (signaturePrivateKey == null) {
            throw new PgpGeneralException(
                    mContext.getString(R.string.error_could_not_extract_private_key));
        }
        updateProgress(R.string.progress_preparing_streams, 0, 100);

        updateProgress(R.string.progress_preparing_signature, 30, 100);

        int type = PGPSignature.CANONICAL_TEXT_DOCUMENT;
//        if (binary) {
//            type = PGPSignature.BINARY_DOCUMENT;
//        }

        // content signer based on signing key algorithm and chosen hash algorithm
        JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(signingKey
                .getPublicKey().getAlgorithm(), mSignatureHashAlgorithm)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

        PGPSignatureGenerator signatureGenerator = null;
        PGPV3SignatureGenerator signatureV3Generator = null;
        if (mSignatureForceV3) {
            signatureV3Generator = new PGPV3SignatureGenerator(contentSignerBuilder);
            signatureV3Generator.init(type, signaturePrivateKey);
        } else {
            signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
            signatureGenerator.init(type, signaturePrivateKey);

            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            String userId = PgpKeyHelper.getMainUserId(PgpKeyHelper.getMasterKey(signingKeyRing));
            spGen.setSignerUserID(false, userId);
            signatureGenerator.setHashedSubpackets(spGen.generate());
        }

        updateProgress(R.string.progress_signing, 40, 100);

        InputStream inStream = mData.getInputStream();
//        if (binary) {
//            byte[] buffer = new byte[1 << 16];
//            int n = 0;
//            while ((n = inStream.read(buffer)) > 0) {
//                if (signatureForceV3) {
//                    signatureV3Generator.update(buffer, 0, n);
//                } else {
//                    signatureGenerator.update(buffer, 0, n);
//                }
//            }
//        } else {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        final byte[] newline = "\r\n".getBytes("UTF-8");

        String line;
        while ((line = reader.readLine()) != null) {
            if (mSignatureForceV3) {
                processLine(line, null, signatureV3Generator);
                signatureV3Generator.update(newline);
            } else {
                processLine(line, null, signatureGenerator);
                signatureGenerator.update(newline);
            }
        }
//        }

        BCPGOutputStream bOut = new BCPGOutputStream(out);
        if (mSignatureForceV3) {
            signatureV3Generator.generate().encode(bOut);
        } else {
            signatureGenerator.generate().encode(bOut);
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

    private static void processLine(final String pLine, final ArmoredOutputStream pArmoredOutput,
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
