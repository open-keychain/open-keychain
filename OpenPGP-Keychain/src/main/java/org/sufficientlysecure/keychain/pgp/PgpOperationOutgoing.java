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
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedDataGenerator;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
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
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ProgressDialogUpdater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Date;

/**
 * This class uses a Builder pattern!
 */
public class PgpOperationOutgoing {
    private Context context;
    private InputData data;
    private OutputStream outStream;

    private ProgressDialogUpdater progress;
    private boolean enableAsciiArmorOutput;
    private int compressionId;
    private long[] encryptionKeyIds;
    private String encryptionPassphrase;
    private int symmetricEncryptionAlgorithm;
    private long signatureKeyId;
    private int signatureHashAlgorithm;
    private boolean signatureForceV3;
    private String signaturePassphrase;

    private PgpOperationOutgoing(Builder builder) {
        // private Constructor can only be called from Builder
        this.context = builder.context;
        this.data = builder.data;
        this.outStream = builder.outStream;

        this.progress = builder.progress;
        this.enableAsciiArmorOutput = builder.enableAsciiArmorOutput;
        this.compressionId = builder.compressionId;
        this.encryptionKeyIds = builder.encryptionKeyIds;
        this.encryptionPassphrase = builder.encryptionPassphrase;
        this.symmetricEncryptionAlgorithm = builder.symmetricEncryptionAlgorithm;
        this.signatureKeyId = builder.signatureKeyId;
        this.signatureHashAlgorithm = builder.signatureHashAlgorithm;
        this.signatureForceV3 = builder.signatureForceV3;
        this.signaturePassphrase = builder.signaturePassphrase;
    }

    public static class Builder {
        // mandatory parameter
        private Context context;
        private InputData data;
        private OutputStream outStream;

        // optional
        private ProgressDialogUpdater progress = null;
        private boolean enableAsciiArmorOutput = false;
        private int compressionId = Id.choice.compression.none;
        private long[] encryptionKeyIds = new long[0];
        private String encryptionPassphrase = null;
        private int symmetricEncryptionAlgorithm = 0;
        private long signatureKeyId = Id.key.none;
        private int signatureHashAlgorithm = 0;
        private boolean signatureForceV3 = false;
        private String signaturePassphrase = null;

        public Builder(Context context, InputData data, OutputStream outStream) {
            this.context = context;
            this.data = data;
            this.outStream = outStream;
        }

        public Builder progress(ProgressDialogUpdater progress) {
            this.progress = progress;
            return this;
        }

        public Builder enableAsciiArmorOutput(boolean enableAsciiArmorOutput) {
            this.enableAsciiArmorOutput = enableAsciiArmorOutput;
            return this;
        }

        public Builder compressionId(int compressionId) {
            this.compressionId = compressionId;
            return this;
        }

        public Builder encryptionKeyIds(long[] encryptionKeyIds) {
            this.encryptionKeyIds = encryptionKeyIds;
            return this;
        }

        public Builder encryptionPassphrase(String encryptionPassphrase) {
            this.encryptionPassphrase = encryptionPassphrase;
            return this;
        }

        public Builder symmetricEncryptionAlgorithm(int symmetricEncryptionAlgorithm) {
            this.symmetricEncryptionAlgorithm = symmetricEncryptionAlgorithm;
            return this;
        }

        public Builder signatureKeyId(long signatureKeyId) {
            this.signatureKeyId = signatureKeyId;
            return this;
        }

        public Builder signatureHashAlgorithm(int signatureHashAlgorithm) {
            this.signatureHashAlgorithm = signatureHashAlgorithm;
            return this;
        }

        public Builder signatureForceV3(boolean signatureForceV3) {
            this.signatureForceV3 = signatureForceV3;
            return this;
        }

        public Builder signaturePassphrase(String signaturePassphrase) {
            this.signaturePassphrase = signaturePassphrase;
            return this;
        }

        public PgpOperationOutgoing build() {
            return new PgpOperationOutgoing(this);
        }
    }

    public void updateProgress(int message, int current, int total) {
        if (progress != null) {
            progress.setProgress(message, current, total);
        }
    }

    public void updateProgress(int current, int total) {
        if (progress != null) {
            progress.setProgress(current, total);
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
    public void signEncrypt()
            throws IOException, PgpGeneralException, PGPException, NoSuchProviderException,
            NoSuchAlgorithmException, SignatureException {

        boolean enableSignature = signatureKeyId != Id.key.none;
        boolean enableEncryption = (encryptionKeyIds.length != 0 || encryptionPassphrase != null);
        boolean enableCompression = (enableEncryption && compressionId != Id.choice.compression.none);

        Log.d(Constants.TAG, "enableSignature:" + enableSignature
                + "\nenableEncryption:" + enableEncryption
                + "\nenableCompression:" + enableCompression
                + "\nenableAsciiArmorOutput:" + enableAsciiArmorOutput);

        int signatureType;
        if (enableAsciiArmorOutput && enableSignature && !enableEncryption && !enableCompression) {
            // for sign-only ascii text
            signatureType = PGPSignature.CANONICAL_TEXT_DOCUMENT;
        } else {
            signatureType = PGPSignature.BINARY_DOCUMENT;
        }

        ArmoredOutputStream armorOut = null;
        OutputStream out;
        if (enableAsciiArmorOutput) {
            armorOut = new ArmoredOutputStream(outStream);
            armorOut.setHeader("Version", PgpHelper.getFullVersion(context));
            out = armorOut;
        } else {
            out = outStream;
        }

        /* Get keys for signature generation for later usage */
        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;
        if (enableSignature) {
            signingKeyRing = ProviderHelper.getPGPSecretKeyRingByKeyId(context, signatureKeyId);
            signingKey = PgpKeyHelper.getSigningKey(context, signatureKeyId);
            if (signingKey == null) {
                throw new PgpGeneralException(context.getString(R.string.error_signature_failed));
            }

            if (signaturePassphrase == null) {
                throw new PgpGeneralException(
                        context.getString(R.string.error_no_signature_passphrase));
            }

            updateProgress(R.string.progress_extracting_signature_key, 0, 100);

            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                    Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(signaturePassphrase.toCharArray());
            signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
            if (signaturePrivateKey == null) {
                throw new PgpGeneralException(
                        context.getString(R.string.error_could_not_extract_private_key));
            }
        }
        updateProgress(R.string.progress_preparing_streams, 5, 100);

        /* Initialize PGPEncryptedDataGenerator for later usage */
        PGPEncryptedDataGenerator cPk = null;
        if (enableEncryption) {
            // has Integrity packet enabled!
            JcePGPDataEncryptorBuilder encryptorBuilder =
                    new JcePGPDataEncryptorBuilder(symmetricEncryptionAlgorithm)
                            .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME)
                            .setWithIntegrityPacket(true);

            cPk = new PGPEncryptedDataGenerator(encryptorBuilder);

            if (encryptionKeyIds.length == 0) {
                // Symmetric encryption
                Log.d(Constants.TAG, "encryptionKeyIds length is 0 -> symmetric encryption");

                JcePBEKeyEncryptionMethodGenerator symmetricEncryptionGenerator =
                        new JcePBEKeyEncryptionMethodGenerator(encryptionPassphrase.toCharArray());
                cPk.addMethod(symmetricEncryptionGenerator);
            } else {
                // Asymmetric encryption
                for (long id : encryptionKeyIds) {
                    PGPPublicKey key = PgpKeyHelper.getEncryptPublicKey(context, id);
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
                    signingKey.getPublicKey().getAlgorithm(), signatureHashAlgorithm)
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

            if (signatureForceV3) {
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
                compressGen = new PGPCompressedDataGenerator(compressionId);
                bcpgOut = new BCPGOutputStream(compressGen.open(encryptionOut));
            } else {
                bcpgOut = new BCPGOutputStream(encryptionOut);
            }

            if (enableSignature) {
                if (signatureForceV3) {
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
            InputStream in = data.getInputStream();
            while ((n = in.read(buffer)) > 0) {
                pOut.write(buffer, 0, n);

                // update signature buffer if signature is requested
                if (enableSignature) {
                    if (signatureForceV3) {
                        signatureV3Generator.update(buffer, 0, n);
                    } else {
                        signatureGenerator.update(buffer, 0, n);
                    }
                }

                progress += n;
                if (data.getSize() != 0) {
                    updateProgress((int) (20 + (95 - 20) * progress / data.getSize()), 100);
                }
            }

            literalGen.close();
        } else if (enableAsciiArmorOutput && enableSignature && !enableEncryption && !enableCompression) {
            /* sign-only of ascii text */

            updateProgress(R.string.progress_signing, 40, 100);

            // write directly on armor output stream
            armorOut.beginClearText(signatureHashAlgorithm);

            InputStream in = data.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            final byte[] newline = "\r\n".getBytes("UTF-8");

            if (signatureForceV3) {
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
                if (signatureForceV3) {
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
            if (signatureForceV3) {
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
        if (enableAsciiArmorOutput) {
            armorOut.close();
        }

        out.close();
        outStream.close();

        updateProgress(R.string.progress_done, 100, 100);
    }

    // TODO: merge this into signEncrypt method!
    // TODO: allow binary input for this class
    public void generateSignature()
            throws PgpGeneralException, PGPException, IOException, NoSuchAlgorithmException,
            SignatureException {

        OutputStream out;
        if (enableAsciiArmorOutput) {
            // Ascii Armor (Radix-64)
            ArmoredOutputStream armorOut = new ArmoredOutputStream(outStream);
            armorOut.setHeader("Version", PgpHelper.getFullVersion(context));
            out = armorOut;
        } else {
            out = outStream;
        }

        if (signatureKeyId == 0) {
            throw new PgpGeneralException(context.getString(R.string.error_no_signature_key));
        }

        PGPSecretKeyRing signingKeyRing = ProviderHelper.getPGPSecretKeyRingByKeyId(context, signatureKeyId);
        PGPSecretKey signingKey = PgpKeyHelper.getSigningKey(context, signatureKeyId);
        if (signingKey == null) {
            throw new PgpGeneralException(context.getString(R.string.error_signature_failed));
        }

        if (signaturePassphrase == null) {
            throw new PgpGeneralException(context.getString(R.string.error_no_signature_passphrase));
        }

        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(signaturePassphrase.toCharArray());
        PGPPrivateKey signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
        if (signaturePrivateKey == null) {
            throw new PgpGeneralException(
                    context.getString(R.string.error_could_not_extract_private_key));
        }
        updateProgress(R.string.progress_preparing_streams, 0, 100);

        updateProgress(R.string.progress_preparing_signature, 30, 100);

        int type = PGPSignature.CANONICAL_TEXT_DOCUMENT;
//        if (binary) {
//            type = PGPSignature.BINARY_DOCUMENT;
//        }

        // content signer based on signing key algorithm and chosen hash algorithm
        JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(signingKey
                .getPublicKey().getAlgorithm(), signatureHashAlgorithm)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

        PGPSignatureGenerator signatureGenerator = null;
        PGPV3SignatureGenerator signatureV3Generator = null;
        if (signatureForceV3) {
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

        InputStream inStream = data.getInputStream();
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
            if (signatureForceV3) {
                processLine(line, null, signatureV3Generator);
                signatureV3Generator.update(newline);
            } else {
                processLine(line, null, signatureGenerator);
                signatureGenerator.update(newline);
            }
        }
//        }

        BCPGOutputStream bOut = new BCPGOutputStream(out);
        if (signatureForceV3) {
            signatureV3Generator.generate().encode(bOut);
        } else {
            signatureGenerator.generate().encode(bOut);
        }
        out.close();
        outStream.close();

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
