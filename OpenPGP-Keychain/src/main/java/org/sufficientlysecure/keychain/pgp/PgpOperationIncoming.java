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
import android.os.Bundle;

import org.spongycastle.bcpg.ArmoredInputStream;
import org.spongycastle.bcpg.SignatureSubpacketTags;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPOnePassSignature;
import org.spongycastle.openpgp.PGPOnePassSignatureList;
import org.spongycastle.openpgp.PGPPBEEncryptedData;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.PBEDataDecryptorFactory;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.spongycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ProgressDialogUpdater;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SignatureException;
import java.util.Iterator;

/**
 * TODO: make builder pattern like in PgpOperationOutgoing
 */
public class PgpOperationIncoming {
    private Context mContext;
    private ProgressDialogUpdater mProgress;
    private InputData mData;
    private OutputStream mOutStream;

    public PgpOperationIncoming(Context context, ProgressDialogUpdater progress, InputData data,
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
        // automatically works with ascii armor input and binary
        InputStream in = PGPUtil.getDecoderStream(mData.getInputStream());
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
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
            throw new PgpGeneralException(mContext.getString(R.string.error_invalid_data));
        }

        InputStream clear;
        PGPEncryptedData encryptedData;

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

        long signatureKeyId = 0;
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

            int n;
//            int progress = 0;
            long startPos = mData.getStreamPosition();
            while ((n = dataIn.read(buffer)) > 0) {
                out.write(buffer, 0, n);
//                progress += n;
                if (signature != null) {
                    try {
                        signature.update(buffer, 0, n);
                    } catch (SignatureException e) {
                        returnData
                                .putBoolean(KeychainIntentService.RESULT_SIGNATURE_SUCCESS, false);
                        signature = null;
                    }
                }
                // TODO: dead code?!
                // unknown size, but try to at least have a moving, slowing down progress bar
//                currentProgress = startProgress + (endProgress - startProgress) * progress
//                        / (progress + 100000);
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

        // TODO: test if this integrity really check works!
        if (encryptedData.isIntegrityProtected()) {
            updateProgress(R.string.progress_verifying_integrity, 95, 100);

            if (encryptedData.verify()) {
                // passed
                Log.d(Constants.TAG, "Integrity verification: success!");
            } else {
                // failed
                Log.d(Constants.TAG, "Integrity verification: failed!");
                throw new PgpGeneralException(mContext.getString(R.string.error_integrity_check_failed));
            }
        } else {
            // no integrity check
            Log.e(Constants.TAG, "Encrypted data was not integrity protected!");
        }

        updateProgress(R.string.progress_done, 100, 100);
        return returnData;
    }

    public Bundle verifyText() throws IOException, PgpGeneralException,
            PGPException, SignatureException {
        Bundle returnData = new Bundle();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ArmoredInputStream aIn = new ArmoredInputStream(mData.getInputStream());

        updateProgress(R.string.progress_done, 0, 100);

        // mostly taken from pg/src/main/java/org/spongycastle/openpgp/examples/ClearSignedFileProcessor.java
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

    public boolean verifyKeyBinding(Context mContext, PGPSignature signature, PGPPublicKey signatureKey) {
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

    public boolean verifyKeyBinding(PGPPublicKey masterPublicKey, PGPPublicKey signingPublicKey) {
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

    private boolean verifyPrimaryBinding(PGPSignatureSubpacketVector Pkts, PGPPublicKey masterPublicKey, PGPPublicKey signingPublicKey) {
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
