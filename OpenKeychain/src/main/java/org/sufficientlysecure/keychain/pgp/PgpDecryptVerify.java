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
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SignatureException;
import java.util.Iterator;
import java.util.Set;

/**
 * This class uses a Builder pattern!
 */
public class PgpDecryptVerify {
    private ProviderHelper mProviderHelper;
    private PassphraseCache mPassphraseCache;
    private InputData mData;
    private OutputStream mOutStream;

    private Progressable mProgressable;
    private boolean mAllowSymmetricDecryption;
    private String mPassphrase;
    private Set<Long> mAllowedKeyIds;

    private PgpDecryptVerify(Builder builder) {
        // private Constructor can only be called from Builder
        this.mProviderHelper = builder.mProviderHelper;
        this.mPassphraseCache = builder.mPassphraseCache;
        this.mData = builder.mData;
        this.mOutStream = builder.mOutStream;

        this.mProgressable = builder.mProgressable;
        this.mAllowSymmetricDecryption = builder.mAllowSymmetricDecryption;
        this.mPassphrase = builder.mPassphrase;
        this.mAllowedKeyIds = builder.mAllowedKeyIds;
    }

    public static class Builder {
        // mandatory parameter
        private ProviderHelper mProviderHelper;
        private PassphraseCache mPassphraseCache;
        private InputData mData;
        private OutputStream mOutStream;

        // optional
        private Progressable mProgressable = null;
        private boolean mAllowSymmetricDecryption = true;
        private String mPassphrase = null;
        private Set<Long> mAllowedKeyIds = null;

        public Builder(ProviderHelper providerHelper, PassphraseCache passphraseCache,
                       InputData data, OutputStream outStream) {
            this.mProviderHelper = providerHelper;
            this.mPassphraseCache = passphraseCache;
            this.mData = data;
            this.mOutStream = outStream;
        }

        public Builder setProgressable(Progressable progressable) {
            mProgressable = progressable;
            return this;
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
         *
         * @param allowedKeyIds
         * @return
         */
        public Builder setAllowedKeyIds(Set<Long> allowedKeyIds) {
            this.mAllowedKeyIds = allowedKeyIds;
            return this;
        }

        public PgpDecryptVerify build() {
            return new PgpDecryptVerify(this);
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

    public interface PassphraseCache {
        public String getCachedPassphrase(long masterKeyId);
    }

    public static class InvalidDataException extends Exception {
        public InvalidDataException() {
        }
    }

    public static class KeyExtractionException extends Exception {
        public KeyExtractionException() {
        }
    }

    public static class WrongPassphraseException extends Exception {
        public WrongPassphraseException() {
        }
    }

    public static class NoSecretKeyException extends Exception {
        public NoSecretKeyException() {
        }
    }

    public static class IntegrityCheckFailedException extends Exception {
        public IntegrityCheckFailedException() {
        }
    }

    /**
     * Decrypts and/or verifies data based on parameters of class
     */
    public PgpDecryptVerifyResult execute()
            throws IOException, PGPException, SignatureException,
            WrongPassphraseException, NoSecretKeyException, KeyExtractionException,
            InvalidDataException, IntegrityCheckFailedException {
        // automatically works with ascii armor input and binary
        InputStream in = PGPUtil.getDecoderStream(mData.getInputStream());
        if (in instanceof ArmoredInputStream) {
            ArmoredInputStream aIn = (ArmoredInputStream) in;
            // it is ascii armored
            Log.d(Constants.TAG, "ASCII Armor Header Line: " + aIn.getArmorHeaderLine());

            if (aIn.isClearText()) {
                // a cleartext signature, verify it with the other method
                return verifyCleartextSignature(aIn);
            }
            // else: ascii armored encryption! go on...
        }

        return decryptVerify(in);
    }

    /**
     * Decrypt and/or verifies binary or ascii armored pgp
     */
    private PgpDecryptVerifyResult decryptVerify(InputStream in)
            throws IOException, PGPException, SignatureException,
            WrongPassphraseException, KeyExtractionException, NoSecretKeyException,
            InvalidDataException, IntegrityCheckFailedException {
        PgpDecryptVerifyResult result = new PgpDecryptVerifyResult();

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
            throw new InvalidDataException();
        }

        InputStream clear;
        PGPEncryptedData encryptedData;

        currentProgress += 5;

        PGPPublicKeyEncryptedData encryptedDataAsymmetric = null;
        PGPPBEEncryptedData encryptedDataSymmetric = null;
        WrappedSecretKey secretEncryptionKey = null;
        Iterator<?> it = enc.getEncryptedDataObjects();
        boolean asymmetricPacketFound = false;
        boolean symmetricPacketFound = false;
        // go through all objects and find one we can decrypt
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof PGPPublicKeyEncryptedData) {
                updateProgress(R.string.progress_finding_key, currentProgress, 100);

                PGPPublicKeyEncryptedData encData = (PGPPublicKeyEncryptedData) obj;

                WrappedSecretKeyRing secretKeyRing;
                try {
                    // get actual keyring object based on master key id
                    secretKeyRing = mProviderHelper.getWrappedSecretKeyRing(
                            KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(encData.getKeyID())
                    );
                } catch (ProviderHelper.NotFoundException e) {
                    // continue with the next packet in the while loop
                    continue;
                }
                if (secretKeyRing == null) {
                    // continue with the next packet in the while loop
                    continue;
                }
                // get subkey which has been used for this encryption packet
                secretEncryptionKey = secretKeyRing.getSubKey(encData.getKeyID());
                if (secretEncryptionKey == null) {
                    // continue with the next packet in the while loop
                    continue;
                }

                /* secret key exists in database! */
                long masterKeyId = secretEncryptionKey.getRing().getMasterKeyId();

                // allow only specific keys for decryption?
                if (mAllowedKeyIds != null) {
                    Log.d(Constants.TAG, "encData.getKeyID():" + encData.getKeyID());
                    Log.d(Constants.TAG, "allowedKeyIds: " + mAllowedKeyIds);
                    Log.d(Constants.TAG, "masterKeyId: " + masterKeyId);

                    if (!mAllowedKeyIds.contains(masterKeyId)) {
                        // this key is in our db, but NOT allowed!
                        // continue with the next packet in the while loop
                        continue;
                    }
                }

                /* secret key exists in database and is allowed! */
                asymmetricPacketFound = true;

                encryptedDataAsymmetric = encData;

                // if no passphrase was explicitly set try to get it from the cache service
                if (mPassphrase == null) {
                    // returns "" if key has no passphrase
                    mPassphrase = mPassphraseCache.getCachedPassphrase(masterKeyId);

                    // if passphrase was not cached, return here
                    // indicating that a passphrase is missing!
                    if (mPassphrase == null) {
                        result.setKeyIdPassphraseNeeded(masterKeyId);
                        result.setStatus(PgpDecryptVerifyResult.KEY_PASSHRASE_NEEDED);
                        return result;
                    }
                }

                // break out of while, only decrypt the first packet where we have a key
                // TODO???: There could be more pgp objects, which are not decrypted!
                break;
            } else if (mAllowSymmetricDecryption && obj instanceof PGPPBEEncryptedData) {
                /*
                 * When mAllowSymmetricDecryption == true and we find a data packet here,
                 * we do not search for other available asymmetric packets!
                 */
                symmetricPacketFound = true;

                encryptedDataSymmetric = (PGPPBEEncryptedData) obj;

                // if no passphrase is given, return here
                // indicating that a passphrase is missing!
                if (mPassphrase == null) {
                    result.setStatus(PgpDecryptVerifyResult.SYMMETRIC_PASSHRASE_NEEDED);
                    return result;
                }

                // break out of while, only decrypt the first packet
                // TODO???: There could be more pgp objects, which are not decrypted!
                break;
            }
        }

        if (symmetricPacketFound) {
            updateProgress(R.string.progress_preparing_streams, currentProgress, 100);

            PGPDigestCalculatorProvider digestCalcProvider = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build();
            PBEDataDecryptorFactory decryptorFactory = new JcePBEDataDecryptorFactoryBuilder(
                    digestCalcProvider).setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(
                    mPassphrase.toCharArray());

            clear = encryptedDataSymmetric.getDataStream(decryptorFactory);

            encryptedData = encryptedDataSymmetric;
            currentProgress += 5;
        } else if (asymmetricPacketFound) {
            currentProgress += 5;
            updateProgress(R.string.progress_extracting_key, currentProgress, 100);
            try {
                if (!secretEncryptionKey.unlock(mPassphrase)) {
                    throw new WrongPassphraseException();
                }
            } catch(PgpGeneralException e) {
                throw new KeyExtractionException();
            }
            currentProgress += 5;
            updateProgress(R.string.progress_preparing_streams, currentProgress, 100);

            PublicKeyDataDecryptorFactory decryptorFactory = secretEncryptionKey.getDecryptorFactory();

            clear = encryptedDataAsymmetric.getDataStream(decryptorFactory);

            encryptedData = encryptedDataAsymmetric;
            currentProgress += 5;
        } else {
            // no packet has been found where we have the corresponding secret key in our db
            throw new NoSecretKeyException();
        }

        PGPObjectFactory plainFact = new PGPObjectFactory(clear);
        Object dataChunk = plainFact.nextObject();
        OpenPgpSignatureResultBuilder signatureResultBuilder = new OpenPgpSignatureResultBuilder();
        int signatureIndex = -1;
        WrappedPublicKeyRing signingRing = null;
        WrappedPublicKey signingKey = null;

        if (dataChunk instanceof PGPCompressedData) {
            updateProgress(R.string.progress_decompressing_data, currentProgress, 100);

            PGPObjectFactory fact = new PGPObjectFactory(
                    ((PGPCompressedData) dataChunk).getDataStream());
            dataChunk = fact.nextObject();
            plainFact = fact;
            currentProgress += 10;
        }

        PGPOnePassSignature signature = null;

        if (dataChunk instanceof PGPOnePassSignatureList) {
            updateProgress(R.string.progress_processing_signature, currentProgress, 100);

            PGPOnePassSignatureList sigList = (PGPOnePassSignatureList) dataChunk;

            // go through all signatures
            // and find out for which signature we have a key in our database
            for (int i = 0; i < sigList.size(); ++i) {
                try {
                    long sigKeyId = sigList.get(i).getKeyID();
                    signingRing = mProviderHelper.getWrappedPublicKeyRing(
                            KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(sigKeyId)
                    );
                    signingKey = signingRing.getSubkey(sigKeyId);
                    signatureIndex = i;
                } catch (ProviderHelper.NotFoundException e) {
                    Log.d(Constants.TAG, "key not found!");
                    // try next one...
                }
            }

            if (signingKey != null) {
                // key found in our database!
                signature = sigList.get(signatureIndex);

                signatureResultBuilder.signatureAvailable(true);
                signatureResultBuilder.knownKey(true);
                signatureResultBuilder.keyId(signingRing.getMasterKeyId());
                try {
                    signatureResultBuilder.userId(signingRing.getPrimaryUserId());
                } catch(PgpGeneralException e) {
                    Log.d(Constants.TAG, "No primary user id in key " + signingRing.getMasterKeyId());
                }
                signatureResultBuilder.signatureKeyCertified(signingRing.getVerified() > 0);

                signingKey.initSignature(signature);
            } else {
                // no key in our database -> return "unknown pub key" status including the first key id
                if (!sigList.isEmpty()) {
                    signatureResultBuilder.signatureAvailable(true);
                    signatureResultBuilder.knownKey(false);
                    signatureResultBuilder.keyId(sigList.get(0).getKeyID());
                }
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
            // TODO: progress calculation is broken here! Try to rework it based on commented code!
//            int progress = 0;
            long startPos = mData.getStreamPosition();
            while ((n = dataIn.read(buffer)) > 0) {
                mOutStream.write(buffer, 0, n);
//                progress += n;
                if (signature != null) {
                    try {
                        signature.update(buffer, 0, n);
                    } catch (SignatureException e) {
                        Log.d(Constants.TAG, "SIGNATURE_ERROR");
                        signatureResultBuilder.validSignature(false);
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

                // these are not cleartext signatures!
                // TODO: what about binary signatures?
                signatureResultBuilder.signatureOnly(false);

                // Verify signature and check binding signatures
                boolean validSignature = signature.verify(messageSignature);
                boolean validKeyBinding = signingRing.verifySubkeyBinding(signingKey);

                signatureResultBuilder.validSignature(validSignature);
                signatureResultBuilder.validKeyBinding(validKeyBinding);
            }
        }

        if (encryptedData.isIntegrityProtected()) {
            updateProgress(R.string.progress_verifying_integrity, 95, 100);

            if (encryptedData.verify()) {
                // passed
                Log.d(Constants.TAG, "Integrity verification: success!");
            } else {
                // failed
                Log.d(Constants.TAG, "Integrity verification: failed!");
                throw new IntegrityCheckFailedException();
            }
        } else {
            // no integrity check
            Log.e(Constants.TAG, "Encrypted data was not integrity protected!");
            // TODO: inform user?
        }

        updateProgress(R.string.progress_done, 100, 100);

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
    private PgpDecryptVerifyResult verifyCleartextSignature(ArmoredInputStream aIn)
            throws IOException, PGPException, SignatureException, InvalidDataException {
        PgpDecryptVerifyResult result = new PgpDecryptVerifyResult();
        OpenPgpSignatureResultBuilder signatureResultBuilder = new OpenPgpSignatureResultBuilder();
        // cleartext signatures are never encrypted ;)
        signatureResultBuilder.signatureOnly(true);

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
        PGPObjectFactory pgpFact = new PGPObjectFactory(aIn);

        PGPSignatureList sigList = (PGPSignatureList) pgpFact.nextObject();
        if (sigList == null) {
            throw new InvalidDataException();
        }

        WrappedPublicKeyRing signingRing = null;
        WrappedPublicKey signingKey = null;
        int signatureIndex = -1;

        // go through all signatures
        // and find out for which signature we have a key in our database
        for (int i = 0; i < sigList.size(); ++i) {
            try {
                long sigKeyId = sigList.get(i).getKeyID();
                signingRing = mProviderHelper.getWrappedPublicKeyRing(
                        KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(sigKeyId)
                );
                signingKey = signingRing.getSubkey(sigKeyId);
                signatureIndex = i;
            } catch (ProviderHelper.NotFoundException e) {
                Log.d(Constants.TAG, "key not found!");
                // try next one...
            }
        }

        PGPSignature signature = null;

        if (signingKey != null) {
            // key found in our database!
            signature = sigList.get(signatureIndex);

            signatureResultBuilder.signatureAvailable(true);
            signatureResultBuilder.knownKey(true);
            signatureResultBuilder.keyId(signingRing.getMasterKeyId());
            try {
                signatureResultBuilder.userId(signingRing.getPrimaryUserId());
            } catch(PgpGeneralException e) {
                Log.d(Constants.TAG, "No primary user id in key " + signingRing.getMasterKeyId());
            }
            signatureResultBuilder.signatureKeyCertified(signingRing.getVerified() > 0);

            signingKey.initSignature(signature);
        } else {
            // no key in our database -> return "unknown pub key" status including the first key id
            if (!sigList.isEmpty()) {
                signatureResultBuilder.signatureAvailable(true);
                signatureResultBuilder.knownKey(false);
                signatureResultBuilder.keyId(sigList.get(0).getKeyID());
            }
        }

        if (signature != null) {
            updateProgress(R.string.progress_verifying_signature, 90, 100);

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
            boolean validKeyBinding = signingRing.verifySubkeyBinding(signingKey);

            signatureResultBuilder.validSignature(validSignature);
            signatureResultBuilder.validKeyBinding(validKeyBinding);
        }

        result.setSignatureResult(signatureResultBuilder.build());

        updateProgress(R.string.progress_done, 100, 100);
        return result;
    }

    /**
     * Mostly taken from ClearSignedFileProcessor in Bouncy Castle
     *
     * @param sig
     * @param line
     * @throws SignatureException
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
