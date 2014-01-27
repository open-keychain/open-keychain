/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Date;
import java.util.Iterator;

import org.spongycastle.bcpg.ArmoredInputStream;
import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.BCPGInputStream;
import org.spongycastle.bcpg.BCPGOutputStream;

import org.spongycastle.bcpg.SignaturePacket;

import org.spongycastle.bcpg.SignatureSubpacket;
import org.spongycastle.bcpg.SignatureSubpacketTags;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPEncryptedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPOnePassSignature;
import org.spongycastle.openpgp.PGPOnePassSignatureList;
import org.spongycastle.openpgp.PGPPBEEncryptedData;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.PGPV3SignatureGenerator;
import org.spongycastle.openpgp.operator.PBEDataDecryptorFactory;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.spongycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBEKeyEncryptionMethodGenerator;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ProgressDialogUpdater;

import android.content.Context;
import android.os.Bundle;

public class PgpOperation {
    private Context mContext;
    private ProgressDialogUpdater mProgress;
    private InputData mData;
    private OutputStream mOutStream;

    public PgpOperation(Context context, ProgressDialogUpdater progress, InputData data,
            OutputStream outStream) {
        super();
        this.mContext = context;
        this.mProgress = progress;
        this.mData = data;
        this.mOutStream = outStream;
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

    public void signAndEncrypt(boolean useAsciiArmor, int compression, long[] encryptionKeyIds,
            String encryptionPassphrase, int symmetricEncryptionAlgorithm, long signatureKeyId,
            int signatureHashAlgorithm, boolean signatureForceV3, String signaturePassphrase)
            throws IOException, PgpGeneralException, PGPException, NoSuchProviderException,
            NoSuchAlgorithmException, SignatureException {

        if (encryptionKeyIds == null) {
            encryptionKeyIds = new long[0];
        }

        ArmoredOutputStream armorOut = null;
        OutputStream out = null;
        OutputStream encryptOut = null;
        if (useAsciiArmor) {
            armorOut = new ArmoredOutputStream(mOutStream);
            armorOut.setHeader("Version", PgpHelper.getFullVersion(mContext));
            out = armorOut;
        } else {
            out = mOutStream;
        }
        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;

        if (encryptionKeyIds.length == 0 && encryptionPassphrase == null) {
            throw new PgpGeneralException(
                    mContext.getString(R.string.error_no_encryption_keys_or_passphrase));
        }

        if (signatureKeyId != Id.key.none) {
            signingKeyRing = ProviderHelper.getPGPSecretKeyRingByKeyId(mContext, signatureKeyId);
            signingKey = PgpKeyHelper.getSigningKey(mContext, signatureKeyId);
            if (signingKey == null) {
                throw new PgpGeneralException(mContext.getString(R.string.error_signature_failed));
            }

            if (signaturePassphrase == null) {
                throw new PgpGeneralException(
                        mContext.getString(R.string.error_no_signature_passphrase));
            }

            updateProgress(R.string.progress_extracting_signature_key, 0, 100);

            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                    Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(signaturePassphrase.toCharArray());
            signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
            if (signaturePrivateKey == null) {
                throw new PgpGeneralException(
                        mContext.getString(R.string.error_could_not_extract_private_key));
            }
        }
        updateProgress(R.string.progress_preparing_streams, 5, 100);

        // encrypt and compress input file content
        JcePGPDataEncryptorBuilder encryptorBuilder = new JcePGPDataEncryptorBuilder(
                symmetricEncryptionAlgorithm).setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME)
                .setWithIntegrityPacket(true);

        PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(encryptorBuilder);

        if (encryptionKeyIds.length == 0) {
            // Symmetric encryption
            Log.d(Constants.TAG, "encryptionKeyIds length is 0 -> symmetric encryption");

            JcePBEKeyEncryptionMethodGenerator symmetricEncryptionGenerator = new JcePBEKeyEncryptionMethodGenerator(
                    encryptionPassphrase.toCharArray());
            cPk.addMethod(symmetricEncryptionGenerator);
        } else {
            // Asymmetric encryption
            for (long id : encryptionKeyIds) {
                PGPPublicKey key = PgpKeyHelper.getEncryptPublicKey(mContext, id);
                if (key != null) {

                    JcePublicKeyKeyEncryptionMethodGenerator pubKeyEncryptionGenerator = new JcePublicKeyKeyEncryptionMethodGenerator(
                            key);
                    cPk.addMethod(pubKeyEncryptionGenerator);
                }
            }
        }
        encryptOut = cPk.open(out, new byte[1 << 16]);

        PGPSignatureGenerator signatureGenerator = null;
        PGPV3SignatureGenerator signatureV3Generator = null;

        if (signatureKeyId != Id.key.none) {
            updateProgress(R.string.progress_preparing_signature, 10, 100);

            // content signer based on signing key algorithm and choosen hash algorithm
            JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(
                    signingKey.getPublicKey().getAlgorithm(), signatureHashAlgorithm)
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

            if (signatureForceV3) {
                signatureV3Generator = new PGPV3SignatureGenerator(contentSignerBuilder);
                signatureV3Generator.init(PGPSignature.BINARY_DOCUMENT, signaturePrivateKey);
            } else {
                signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
                signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, signaturePrivateKey);

                String userId = PgpKeyHelper.getMainUserId(PgpKeyHelper
                        .getMasterKey(signingKeyRing));
                PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
                spGen.setSignerUserID(false, userId);
                signatureGenerator.setHashedSubpackets(spGen.generate());
            }
        }

        PGPCompressedDataGenerator compressGen = null;
        BCPGOutputStream bcpgOut = null;
        if (compression == Id.choice.compression.none) {
            bcpgOut = new BCPGOutputStream(encryptOut);
        } else {
            compressGen = new PGPCompressedDataGenerator(compression);
            bcpgOut = new BCPGOutputStream(compressGen.open(encryptOut));
        }
        if (signatureKeyId != Id.key.none) {
            if (signatureForceV3) {
                signatureV3Generator.generateOnePassVersion(false).encode(bcpgOut);
            } else {
                signatureGenerator.generateOnePassVersion(false).encode(bcpgOut);
            }
        }

        PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
        // file name not needed, so empty string
        OutputStream pOut = literalGen.open(bcpgOut, PGPLiteralData.BINARY, "", new Date(),
                new byte[1 << 16]);
        updateProgress(R.string.progress_encrypting, 20, 100);

        long done = 0;
        int n = 0;
        byte[] buffer = new byte[1 << 16];
        InputStream in = mData.getInputStream();
        while ((n = in.read(buffer)) > 0) {
            pOut.write(buffer, 0, n);
            if (signatureKeyId != Id.key.none) {
                if (signatureForceV3) {
                    signatureV3Generator.update(buffer, 0, n);
                } else {
                    signatureGenerator.update(buffer, 0, n);
                }
            }
            done += n;
            if (mData.getSize() != 0) {
                updateProgress((int) (20 + (95 - 20) * done / mData.getSize()), 100);
            }
        }

        literalGen.close();

        if (signatureKeyId != Id.key.none) {
            updateProgress(R.string.progress_generating_signature, 95, 100);
            if (signatureForceV3) {
                signatureV3Generator.generate().encode(pOut);
            } else {
                signatureGenerator.generate().encode(pOut);
            }
        }
        if (compressGen != null) {
            compressGen.close();
        }
        encryptOut.close();
        if (useAsciiArmor) {
            armorOut.close();
        }

        updateProgress(R.string.progress_done, 100, 100);
    }

    public void signText(long signatureKeyId, String signaturePassphrase,
            int signatureHashAlgorithm, boolean forceV3Signature) throws PgpGeneralException,
            PGPException, IOException, NoSuchAlgorithmException, SignatureException {

        ArmoredOutputStream armorOut = new ArmoredOutputStream(mOutStream);
        armorOut.setHeader("Version", PgpHelper.getFullVersion(mContext));

        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;

        if (signatureKeyId == 0) {
            armorOut.close();
            throw new PgpGeneralException(mContext.getString(R.string.error_no_signature_key));
        }

        signingKeyRing = ProviderHelper.getPGPSecretKeyRingByKeyId(mContext, signatureKeyId);
        signingKey = PgpKeyHelper.getSigningKey(mContext, signatureKeyId);
        if (signingKey == null) {
            armorOut.close();
            throw new PgpGeneralException(mContext.getString(R.string.error_signature_failed));
        }

        if (signaturePassphrase == null) {
            armorOut.close();
            throw new PgpGeneralException(mContext.getString(R.string.error_no_signature_passphrase));
        }
        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(signaturePassphrase.toCharArray());
        signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
        if (signaturePrivateKey == null) {
            armorOut.close();
            throw new PgpGeneralException(
                    mContext.getString(R.string.error_could_not_extract_private_key));
        }
        updateProgress(R.string.progress_preparing_streams, 0, 100);

        updateProgress(R.string.progress_preparing_signature, 30, 100);

        PGPSignatureGenerator signatureGenerator = null;
        PGPV3SignatureGenerator signatureV3Generator = null;

        // content signer based on signing key algorithm and choosen hash algorithm
        JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(signingKey
                .getPublicKey().getAlgorithm(), signatureHashAlgorithm)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

        if (forceV3Signature) {
            signatureV3Generator = new PGPV3SignatureGenerator(contentSignerBuilder);
            signatureV3Generator.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, signaturePrivateKey);
        } else {
            signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
            signatureGenerator.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, signaturePrivateKey);

            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            String userId = PgpKeyHelper.getMainUserId(PgpKeyHelper.getMasterKey(signingKeyRing));
            spGen.setSignerUserID(false, userId);
            signatureGenerator.setHashedSubpackets(spGen.generate());
        }

        updateProgress(R.string.progress_signing, 40, 100);

        armorOut.beginClearText(signatureHashAlgorithm);

        InputStream inStream = mData.getInputStream();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));

        final byte[] newline = "\r\n".getBytes("UTF-8");

        if (forceV3Signature) {
            processLine(reader.readLine(), armorOut, signatureV3Generator);
        } else {
            processLine(reader.readLine(), armorOut, signatureGenerator);
        }

        while (true) {
            final String line = reader.readLine();

            if (line == null) {
                armorOut.write(newline);
                break;
            }

            armorOut.write(newline);
            if (forceV3Signature) {
                signatureV3Generator.update(newline);
                processLine(line, armorOut, signatureV3Generator);
            } else {
                signatureGenerator.update(newline);
                processLine(line, armorOut, signatureGenerator);
            }
        }

        armorOut.endClearText();

        BCPGOutputStream bOut = new BCPGOutputStream(armorOut);
        if (forceV3Signature) {
            signatureV3Generator.generate().encode(bOut);
        } else {
            signatureGenerator.generate().encode(bOut);
        }
        armorOut.close();

        updateProgress(R.string.progress_done, 100, 100);
    }

    public void generateSignature(boolean armored, boolean binary, long signatureKeyId,
            String signaturePassPhrase, int hashAlgorithm, boolean forceV3Signature)
            throws PgpGeneralException, PGPException, IOException, NoSuchAlgorithmException,
            SignatureException {

        OutputStream out = null;

        // Ascii Armor (Base64)
        ArmoredOutputStream armorOut = null;
        if (armored) {
            armorOut = new ArmoredOutputStream(mOutStream);
            armorOut.setHeader("Version", PgpHelper.getFullVersion(mContext));
            out = armorOut;
        } else {
            out = mOutStream;
        }

        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;

        if (signatureKeyId == 0) {
            throw new PgpGeneralException(mContext.getString(R.string.error_no_signature_key));
        }

        signingKeyRing = ProviderHelper.getPGPSecretKeyRingByKeyId(mContext, signatureKeyId);
        signingKey = PgpKeyHelper.getSigningKey(mContext, signatureKeyId);
        if (signingKey == null) {
            throw new PgpGeneralException(mContext.getString(R.string.error_signature_failed));
        }

        if (signaturePassPhrase == null) {
            throw new PgpGeneralException(mContext.getString(R.string.error_no_signature_passphrase));
        }

        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(signaturePassPhrase.toCharArray());
        signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
        if (signaturePrivateKey == null) {
            throw new PgpGeneralException(
                    mContext.getString(R.string.error_could_not_extract_private_key));
        }
        updateProgress(R.string.progress_preparing_streams, 0, 100);

        updateProgress(R.string.progress_preparing_signature, 30, 100);

        PGPSignatureGenerator signatureGenerator = null;
        PGPV3SignatureGenerator signatureV3Generator = null;

        int type = PGPSignature.CANONICAL_TEXT_DOCUMENT;
        if (binary) {
            type = PGPSignature.BINARY_DOCUMENT;
        }

        // content signer based on signing key algorithm and choosen hash algorithm
        JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(signingKey
                .getPublicKey().getAlgorithm(), hashAlgorithm)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

        if (forceV3Signature) {
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
        if (binary) {
            byte[] buffer = new byte[1 << 16];
            int n = 0;
            while ((n = inStream.read(buffer)) > 0) {
                if (forceV3Signature) {
                    signatureV3Generator.update(buffer, 0, n);
                } else {
                    signatureGenerator.update(buffer, 0, n);
                }
            }
        } else {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
            final byte[] newline = "\r\n".getBytes("UTF-8");

            while (true) {
                final String line = reader.readLine();

                if (line == null) {
                    break;
                }

                if (forceV3Signature) {
                    processLine(line, null, signatureV3Generator);
                    signatureV3Generator.update(newline);
                } else {
                    processLine(line, null, signatureGenerator);
                    signatureGenerator.update(newline);
                }
            }
        }

        BCPGOutputStream bOut = new BCPGOutputStream(out);
        if (forceV3Signature) {
            signatureV3Generator.generate().encode(bOut);
        } else {
            signatureGenerator.generate().encode(bOut);
        }
        out.close();
        mOutStream.close();

        if (mProgress != null)
            mProgress.setProgress(R.string.progress_done, 100, 100);
    }

    public static boolean hasSymmetricEncryption(Context context, InputStream inputStream)
            throws PgpGeneralException, IOException {
        InputStream in = PGPUtil.getDecoderStream(inputStream);
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject();

        // the first object might be a PGP marker packet.
        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        if (enc == null) {
            throw new PgpGeneralException(context.getString(R.string.error_invalid_data));
        }

        Iterator<?> it = enc.getEncryptedDataObjects();
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof PGPPBEEncryptedData) {
                return true;
            }
        }

        return false;
    }

    public Bundle decryptAndVerify(String passphrase, boolean assumeSymmetric) throws IOException,
            PgpGeneralException, PGPException, SignatureException {
        if (passphrase == null) {
            passphrase = "";
        }

        Bundle returnData = new Bundle();
        InputStream in = PGPUtil.getDecoderStream(mData.getInputStream());
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject();
        long signatureKeyId = 0;

        int currentProgress = 0;
        updateProgress(R.string.progress_reading_data, currentProgress, 100);

        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        if (enc == null) {
            throw new PgpGeneralException(mContext.getString(R.string.error_invalid_data));
        }

        InputStream clear = null;
        PGPEncryptedData encryptedData = null;

        currentProgress += 5;

        // TODO: currently we always only look at the first known key or symmetric encryption,
        // there might be more...
        if (assumeSymmetric) {
            PGPPBEEncryptedData pbe = null;
            Iterator<?> it = enc.getEncryptedDataObjects();
            // find secret key
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof PGPPBEEncryptedData) {
                    pbe = (PGPPBEEncryptedData) obj;
                    break;
                }
            }

            if (pbe == null) {
                throw new PgpGeneralException(
                        mContext.getString(R.string.error_no_symmetric_encryption_packet));
            }

            updateProgress(R.string.progress_preparing_streams, currentProgress, 100);

            PGPDigestCalculatorProvider digestCalcProvider = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build();
            PBEDataDecryptorFactory decryptorFactory = new JcePBEDataDecryptorFactoryBuilder(
                    digestCalcProvider).setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(
                    passphrase.toCharArray());

            clear = pbe.getDataStream(decryptorFactory);

            encryptedData = pbe;
            currentProgress += 5;
        } else {
            updateProgress(R.string.progress_finding_key, currentProgress, 100);

            PGPPublicKeyEncryptedData pbe = null;
            PGPSecretKey secretKey = null;
            Iterator<?> it = enc.getEncryptedDataObjects();
            // find secret key
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof PGPPublicKeyEncryptedData) {
                    PGPPublicKeyEncryptedData encData = (PGPPublicKeyEncryptedData) obj;
                    secretKey = ProviderHelper.getPGPSecretKeyByKeyId(mContext, encData.getKeyID());
                    if (secretKey != null) {
                        pbe = encData;
                        break;
                    }
                }
            }

            if (secretKey == null) {
                throw new PgpGeneralException(mContext.getString(R.string.error_no_secret_key_found));
            }

            currentProgress += 5;
            updateProgress(R.string.progress_extracting_key, currentProgress, 100);
            PGPPrivateKey privateKey = null;
            try {
                PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder()
                        .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(
                                passphrase.toCharArray());
                privateKey = secretKey.extractPrivateKey(keyDecryptor);
            } catch (PGPException e) {
                throw new PGPException(mContext.getString(R.string.error_wrong_passphrase));
            }
            if (privateKey == null) {
                throw new PgpGeneralException(
                        mContext.getString(R.string.error_could_not_extract_private_key));
            }
            currentProgress += 5;
            updateProgress(R.string.progress_preparing_streams, currentProgress, 100);

            PublicKeyDataDecryptorFactory decryptorFactory = new JcePublicKeyDataDecryptorFactoryBuilder()
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(privateKey);

            clear = pbe.getDataStream(decryptorFactory);

            encryptedData = pbe;
            currentProgress += 5;
        }

        PGPObjectFactory plainFact = new PGPObjectFactory(clear);
        Object dataChunk = plainFact.nextObject();
        PGPOnePassSignature signature = null;
        PGPPublicKey signatureKey = null;
        int signatureIndex = -1;

        if (dataChunk instanceof PGPCompressedData) {
            updateProgress(R.string.progress_decompressing_data, currentProgress, 100);

            PGPObjectFactory fact = new PGPObjectFactory(
                    ((PGPCompressedData) dataChunk).getDataStream());
            dataChunk = fact.nextObject();
            plainFact = fact;
            currentProgress += 10;
        }

        if (dataChunk instanceof PGPOnePassSignatureList) {
            updateProgress(R.string.progress_processing_signature, currentProgress, 100);

            returnData.putBoolean(KeychainIntentService.RESULT_SIGNATURE, true);
            PGPOnePassSignatureList sigList = (PGPOnePassSignatureList) dataChunk;
            for (int i = 0; i < sigList.size(); ++i) {
                signature = sigList.get(i);
                signatureKey = ProviderHelper
                        .getPGPPublicKeyByKeyId(mContext, signature.getKeyID());
                if (signatureKeyId == 0) {
                    signatureKeyId = signature.getKeyID();
                }
                if (signatureKey == null) {
                    signature = null;
                } else {
                    signatureIndex = i;
                    signatureKeyId = signature.getKeyID();
                    String userId = null;
                    PGPPublicKeyRing signKeyRing = ProviderHelper.getPGPPublicKeyRingByKeyId(
                            mContext, signatureKeyId);
                    if (signKeyRing != null) {
                        userId = PgpKeyHelper.getMainUserId(PgpKeyHelper.getMasterKey(signKeyRing));
                    }
                    returnData.putString(KeychainIntentService.RESULT_SIGNATURE_USER_ID, userId);
                    break;
                }
            }

            returnData.putLong(KeychainIntentService.RESULT_SIGNATURE_KEY_ID, signatureKeyId);

            if (signature != null) {
                JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider = new JcaPGPContentVerifierBuilderProvider()
                        .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

                signature.init(contentVerifierBuilderProvider, signatureKey);
            } else {
                returnData.putBoolean(KeychainIntentService.RESULT_SIGNATURE_UNKNOWN, true);
            }

            dataChunk = plainFact.nextObject();
            currentProgress += 10;
        }

        if (dataChunk instanceof PGPSignatureList) {
            dataChunk = plainFact.nextObject();
        }

        if (dataChunk instanceof PGPLiteralData) {
            updateProgress(R.string.progress_decrypting, currentProgress, 100);

            PGPLiteralData literalData = (PGPLiteralData) dataChunk;
            OutputStream out = mOutStream;

            byte[] buffer = new byte[1 << 16];
            InputStream dataIn = literalData.getInputStream();

            int startProgress = currentProgress;
            int endProgress = 100;
            if (signature != null) {
                endProgress = 90;
            } else if (encryptedData.isIntegrityProtected()) {
                endProgress = 95;
            }
            int n = 0;
            int done = 0;
            long startPos = mData.getStreamPosition();
            while ((n = dataIn.read(buffer)) > 0) {
                out.write(buffer, 0, n);
                done += n;
                if (signature != null) {
                    try {
                        signature.update(buffer, 0, n);
                    } catch (SignatureException e) {
                        returnData
                                .putBoolean(KeychainIntentService.RESULT_SIGNATURE_SUCCESS, false);
                        signature = null;
                    }
                }
                // unknown size, but try to at least have a moving, slowing down progress bar
                currentProgress = startProgress + (endProgress - startProgress) * done
                        / (done + 100000);
                if (mData.getSize() - startPos == 0) {
                    currentProgress = endProgress;
                } else {
                    currentProgress = (int) (startProgress + (endProgress - startProgress)
                            * (mData.getStreamPosition() - startPos) / (mData.getSize() - startPos));
                }
                updateProgress(currentProgress, 100);
            }

            if (signature != null) {
                updateProgress(R.string.progress_verifying_signature, 90, 100);

                PGPSignatureList signatureList = (PGPSignatureList) plainFact.nextObject();
                PGPSignature messageSignature = signatureList.get(signatureIndex);

                //Now check binding signatures
                boolean keyBinding_isok = verifyKeyBinding(mContext, messageSignature, signatureKey);
                boolean sig_isok = signature.verify(messageSignature);
                returnData.putBoolean(KeychainIntentService.RESULT_SIGNATURE_SUCCESS, keyBinding_isok & sig_isok);
            }
        }

        // TODO: add integrity somewhere
        if (encryptedData.isIntegrityProtected()) {
            updateProgress(R.string.progress_verifying_integrity, 95, 100);

            if (encryptedData.verify()) {
                // passed
            } else {
                // failed
            }
        } else {
            // no integrity check
        }

        updateProgress(R.string.progress_done, 100, 100);
        return returnData;
    }

    public Bundle verifyText(boolean lookupUnknownKey) throws IOException, PgpGeneralException,
            PGPException, SignatureException {
        Bundle returnData = new Bundle();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ArmoredInputStream aIn = new ArmoredInputStream(mData.getInputStream());

        updateProgress(R.string.progress_done, 0, 100);

        // mostly taken from ClearSignedFileProcessor
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

        returnData.putBoolean(KeychainIntentService.RESULT_SIGNATURE, true);

        updateProgress(R.string.progress_processing_signature, 60, 100);
        PGPObjectFactory pgpFact = new PGPObjectFactory(aIn);

        PGPSignatureList sigList = (PGPSignatureList) pgpFact.nextObject();
        if (sigList == null) {
            throw new PgpGeneralException(mContext.getString(R.string.error_corrupt_data));
        }
        PGPSignature signature = null;
        long signatureKeyId = 0;
        PGPPublicKey signatureKey = null;
        for (int i = 0; i < sigList.size(); ++i) {
            signature = sigList.get(i);
            signatureKey = ProviderHelper.getPGPPublicKeyByKeyId(mContext, signature.getKeyID());
            if (signatureKeyId == 0) {
                signatureKeyId = signature.getKeyID();
            }
            // if key is not known and we want to lookup unknown ones...
            if (signatureKey == null && lookupUnknownKey) {

                returnData = new Bundle();
                returnData.putLong(KeychainIntentService.RESULT_SIGNATURE_KEY_ID, signatureKeyId);
                returnData.putBoolean(KeychainIntentService.RESULT_SIGNATURE_LOOKUP_KEY, true);

                // return directly now, decrypt will be done again after importing unknown key
                return returnData;
            }

            if (signatureKey == null) {
                signature = null;
            } else {
                signatureKeyId = signature.getKeyID();
                String userId = null;
                PGPPublicKeyRing signKeyRing = ProviderHelper.getPGPPublicKeyRingByKeyId(mContext,
                        signatureKeyId);
                if (signKeyRing != null) {
                    userId = PgpKeyHelper.getMainUserId(PgpKeyHelper.getMasterKey(signKeyRing));
                }
                returnData.putString(KeychainIntentService.RESULT_SIGNATURE_USER_ID, userId);
                break;
            }
        }

        returnData.putLong(KeychainIntentService.RESULT_SIGNATURE_KEY_ID, signatureKeyId);

        if (signature == null) {
            returnData.putBoolean(KeychainIntentService.RESULT_SIGNATURE_UNKNOWN, true);
            if (mProgress != null)
                mProgress.setProgress(R.string.progress_done, 100, 100);
            return returnData;
        }

        JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider = new JcaPGPContentVerifierBuilderProvider()
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

        signature.init(contentVerifierBuilderProvider, signatureKey);

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

        boolean sig_isok = signature.verify();

        //Now check binding signatures
        boolean keyBinding_isok = verifyKeyBinding(mContext, signature, signatureKey);

        returnData.putBoolean(KeychainIntentService.RESULT_SIGNATURE_SUCCESS, sig_isok & keyBinding_isok);

        updateProgress(R.string.progress_done, 100, 100);
        return returnData;
    }

    public boolean verifyKeyBinding(Context mContext, PGPSignature signature, PGPPublicKey signatureKey)
    {
        long signatureKeyId = signature.getKeyID();
        boolean keyBinding_isok = false;
        String userId = null;
        PGPPublicKeyRing signKeyRing = ProviderHelper.getPGPPublicKeyRingByKeyId(mContext,
                signatureKeyId);
        PGPPublicKey mKey = null;
        if (signKeyRing != null) {
            mKey = PgpKeyHelper.getMasterKey(signKeyRing);
        }
        if (signature.getKeyID() != mKey.getKeyID()) {
            keyBinding_isok = verifyKeyBinding(mKey, signatureKey);
        } else { //if the key used to make the signature was the master key, no need to check binding sigs
            keyBinding_isok = true;
        }
        return keyBinding_isok;
    }

    public boolean verifyKeyBinding(PGPPublicKey masterPublicKey, PGPPublicKey signingPublicKey)
    {
        boolean subkeyBinding_isok = false;
        boolean tmp_subkeyBinding_isok = false;
        boolean primkeyBinding_isok = false;
        JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider = new JcaPGPContentVerifierBuilderProvider()
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

        Iterator<PGPSignature> itr = signingPublicKey.getSignatures();

        subkeyBinding_isok = false;
        tmp_subkeyBinding_isok = false;
        primkeyBinding_isok = false;
        while (itr.hasNext()) { //what does gpg do if the subkey binding is wrong?
            //gpg has an invalid subkey binding error on key import I think, but doesn't shout
            //about keys without subkey signing. Can't get it to import a slightly broken one
            //either, so we will err on bad subkey binding here.
            PGPSignature sig = itr.next();
            if (sig.getKeyID() == masterPublicKey.getKeyID() && sig.getSignatureType() == PGPSignature.SUBKEY_BINDING) {
                //check and if ok, check primary key binding.
                try {
		            sig.init(contentVerifierBuilderProvider, masterPublicKey);
		            tmp_subkeyBinding_isok = sig.verifyCertification(masterPublicKey, signingPublicKey);
                } catch (PGPException e) {
                    continue;
                } catch (SignatureException e) {
                    continue;
                }

                if (tmp_subkeyBinding_isok)
                    subkeyBinding_isok = true;
                if (tmp_subkeyBinding_isok) {
                    primkeyBinding_isok = verifyPrimaryBinding(sig.getUnhashedSubPackets(), masterPublicKey, signingPublicKey);
                    if (primkeyBinding_isok)
                        break;
                    primkeyBinding_isok = verifyPrimaryBinding(sig.getHashedSubPackets(), masterPublicKey, signingPublicKey);
                    if (primkeyBinding_isok)
                        break;
                }
            }
        }
        return (subkeyBinding_isok & primkeyBinding_isok);
    }

    private boolean verifyPrimaryBinding(PGPSignatureSubpacketVector Pkts, PGPPublicKey masterPublicKey, PGPPublicKey signingPublicKey)
    {
        boolean primkeyBinding_isok = false;
        JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider = new JcaPGPContentVerifierBuilderProvider()
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        PGPSignatureList eSigList;

            if (Pkts.hasSubpacket(SignatureSubpacketTags.EMBEDDED_SIGNATURE)) {
                try {
                    eSigList = Pkts.getEmbeddedSignatures();
                } catch (IOException e) {
                    return false;
                } catch (PGPException e) {
                    return false;
            }
			for (int j = 0; j < eSigList.size(); ++j) {
				PGPSignature emSig = eSigList.get(j);
	            if (emSig.getSignatureType() == PGPSignature.PRIMARYKEY_BINDING) {
                    try {
						emSig.init(contentVerifierBuilderProvider, signingPublicKey);
						primkeyBinding_isok = emSig.verifyCertification(masterPublicKey, signingPublicKey);
						if (primkeyBinding_isok)
							break;
                    } catch (PGPException e) {
                        continue;
                    } catch (SignatureException e) {
                        continue;
                    }
	            }
			}
        }
        return primkeyBinding_isok;
    }

    private static void processLine(final String pLine, final ArmoredOutputStream pArmoredOutput,
            final PGPSignatureGenerator pSignatureGenerator) throws IOException, SignatureException {

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
            final PGPV3SignatureGenerator pSignatureGenerator) throws IOException,
            SignatureException {

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

    // taken from ClearSignedFileProcessor in BC
    private static void processLine(PGPSignature sig, byte[] line) throws SignatureException,
            IOException {
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
