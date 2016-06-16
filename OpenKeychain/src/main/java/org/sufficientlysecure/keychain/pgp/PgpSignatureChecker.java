/*
 * Copyright (C) 2015-2016 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015-2016 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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


import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SignatureException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.operator.PGPContentVerifier;
import org.bouncycastle.openpgp.operator.PGPContentVerifierBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentDigest;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPRawDigestContentVerifierBuilderProvider;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;


/** This class is used to track the state of a single signature verification.
 *
 *
 */
class PgpSignatureChecker {
    private final OpenPgpSignatureResultBuilder signatureResultBuilder;

    private SignatureMode signatureMode;

    private CanonicalizedPublicKey signingKey;
    private Integer hashAlgorithm;
    private Integer signatureIndex;
    private PGPSignature signature;
    private JcaPGPContentDigest digest;

    ProviderHelper mProviderHelper;

    PgpSignatureChecker(ProviderHelper providerHelper, String senderAddress) {
        mProviderHelper = providerHelper;

        signatureResultBuilder = new OpenPgpSignatureResultBuilder(providerHelper);
        signatureResultBuilder.setSenderAddress(senderAddress);
    }

    public boolean initializeSignatureCleartext(Object dataChunk, OperationLog log, int indent) throws PGPException {
        if (signatureMode != null) {
            throw new IllegalStateException("cannot initialize twice!");
        }
        signatureMode = SignatureMode.CLEARTEXT;

        return initializeSignatureNonOnePass(dataChunk, log, indent);
    }

    public boolean initializeSignatureDetached(Object dataChunk, OperationLog log, int indent) throws PGPException {
        if (signatureMode != null) {
            throw new IllegalStateException("cannot initialize twice!");
        }
        signatureMode = SignatureMode.DETACHED;

        return initializeSignatureNonOnePass(dataChunk, log, indent);
    }

    private boolean initializeSignatureNonOnePass(Object dataChunk, OperationLog log, int indent) {
        if (!(dataChunk instanceof PGPSignatureList)) {
            return false;
        }

        PGPSignatureList sigList = (PGPSignatureList) dataChunk;
        findNonOnePassSignature(sigList);

        if (signingKey != null) {
            initializeSignature(log, indent);
        } else if (!sigList.isEmpty()) {
            signatureResultBuilder.setSignatureAvailable(true);
            signatureResultBuilder.setKnownKey(false);
            signatureResultBuilder.setKeyId(sigList.get(0).getKeyID());
        }

        return true;
    }

    boolean initializeSignatureOnePass(Object dataChunk, OperationLog log, int indent) throws PGPException {
        if (signatureMode != null) {
            throw new IllegalStateException("cannot initialize twice!");
        }
        signatureMode = SignatureMode.ONEPASS;

        if (!(dataChunk instanceof PGPOnePassSignatureList)) {
            return false;
        }

        log.add(LogType.MSG_DC_CLEAR_SIGNATURE, indent + 1);

        PGPOnePassSignatureList sigList = (PGPOnePassSignatureList) dataChunk;
        findOnePassSignature(sigList);

        if (signingKey != null) {
            initializeSignature(log, indent);
        } else if (!sigList.isEmpty()) {
            signatureResultBuilder.setSignatureAvailable(true);
            signatureResultBuilder.setKnownKey(false);
            signatureResultBuilder.setKeyId(sigList.get(0).getKeyID());
        }

        return true;

    }

    private void initializeSignature(OperationLog log, int indent) {
        // key found in our database!
        signatureResultBuilder.initValid(signingKey);

        digest = JcaPGPContentDigest.newInstance(
                Constants.BOUNCY_CASTLE_PROVIDER_NAME, hashAlgorithm);

        checkKeySecurity(log, indent);
    }

    private void checkKeySecurity(OperationLog log, int indent) {
        // TODO: checks on signingRing ?
        if (!PgpSecurityConstants.isSecureKey(signingKey)) {
            log.add(LogType.MSG_DC_INSECURE_KEY, indent + 1);
            signatureResultBuilder.setInsecure(true);
        }
    }

    public boolean isInitialized() {
        return signingKey != null;
    }

    private void findOnePassSignature(PGPOnePassSignatureList sigList) {
        // go through all signatures (should be just one), make sure we have
        //  the key and it matches the one we’re looking for
        for (int i = 0; i < sigList.size(); ++i) {
            try {
                long sigKeyId = sigList.get(i).getKeyID();
                CanonicalizedPublicKeyRing signingRing = mProviderHelper.getCanonicalizedPublicKeyRing(
                        KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(sigKeyId)
                );
                CanonicalizedPublicKey keyCandidate = signingRing.getPublicKey(sigKeyId);
                if ( ! keyCandidate.canSign()) {
                    continue;
                }
                signatureIndex = i;
                signingKey = keyCandidate;
                PGPOnePassSignature onePassSignature = sigList.get(i);
                hashAlgorithm = onePassSignature.getHashAlgorithm();
                break;
            } catch (ProviderHelper.NotFoundException e) {
                Log.d(Constants.TAG, "key not found, trying next signature...");
            }
        }
    }

    private void findNonOnePassSignature(PGPSignatureList sigList) {
        // go through all signatures (should be just one), make sure we have
        //  the key and it matches the one we’re looking for
        for (int i = 0; i < sigList.size(); ++i) {
            try {
                long sigKeyId = sigList.get(i).getKeyID();
                CanonicalizedPublicKeyRing signingRing = mProviderHelper.getCanonicalizedPublicKeyRing(
                        KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(sigKeyId)
                );
                CanonicalizedPublicKey keyCandidate = signingRing.getPublicKey(sigKeyId);
                if ( ! keyCandidate.canSign()) {
                    continue;
                }
                signatureIndex = i;
                signingKey = keyCandidate;
                signature = sigList.get(i);
                hashAlgorithm = signature.getHashAlgorithm();
                break;
            } catch (ProviderHelper.NotFoundException e) {
                Log.d(Constants.TAG, "key not found, trying next signature...");
            }
        }
    }

    public void updateSignatureWithCleartext(byte[] clearText) throws IOException, SignatureException {
        if (signatureMode != SignatureMode.CLEARTEXT) {
            throw new IllegalStateException("update with cleartext while not in cleartext mode!");
        }

        InputStream sigIn = new BufferedInputStream(new ByteArrayInputStream(clearText));

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

        int lookAhead = readInputLine(outputBuffer, sigIn);

        processLine(digest, outputBuffer.toByteArray());

        while (lookAhead != -1) {
            lookAhead = readInputLine(outputBuffer, lookAhead, sigIn);

            digest.update((byte) '\r');
            digest.update((byte) '\n');

            processLine(digest, outputBuffer.toByteArray());
        }

    }

    public void updateSignatureData(byte[] buf, int off, int len) {
        if (signatureMode != SignatureMode.DETACHED && signatureMode != SignatureMode.ONEPASS) {
            throw new IllegalStateException("update with data while not in detached or onepass mode!");
        }

        if (digest != null) {
            digest.update(buf, off, len);
        }
    }

    public void verifySignatureDetached(OperationLog log, int indent) throws PGPException {
        if (signatureMode != SignatureMode.DETACHED) {
            throw new IllegalStateException("call to verifySignatureDetached while not in detached mode!");
        }

        log.add(LogType.MSG_DC_CLEAR_SIGNATURE_CHECK, indent);
        verifySignatureInternal(log, indent);
    }

    public void verifySignatureCleartext(OperationLog log, int indent) throws PGPException {
        if (signatureMode != SignatureMode.CLEARTEXT) {
            throw new IllegalStateException("call to verifySignatureCleartext while not in cleartext mode!");
        }

        log.add(LogType.MSG_DC_CLEAR_SIGNATURE_CHECK, indent);
        verifySignatureInternal(log, indent);
    }

    public boolean verifySignatureOnePass(Object o, OperationLog log, int indent) throws PGPException {
        if (signatureMode != SignatureMode.ONEPASS) {
            throw new IllegalStateException("call to verifySignatureOnePass while not in onepass mode!");
        }

        if (!(o instanceof PGPSignatureList)) {
            log.add(LogType.MSG_DC_ERROR_NO_SIGNATURE, indent);
            return false;
        }
        PGPSignatureList signatureList = (PGPSignatureList) o;
        if (signatureList.size() <= signatureIndex) {
            log.add(LogType.MSG_DC_ERROR_NO_SIGNATURE, indent);
            return false;
        }

        // PGPOnePassSignature and PGPSignature packets are "bracketed",
        // so we need to take the last-minus-index'th element here
        signature = signatureList.get(signatureList.size() - 1 - signatureIndex);

        verifySignatureInternal(log, indent);

        return true;
    }

    private void verifySignatureInternal(OperationLog log, int indent) throws PGPException {
        if (signature == null) {
            throw new IllegalStateException("signature must be set at this point!");
        }

        digest.update(signature.getSignatureTrailer());

        PGPContentVerifierBuilder pgpContentVerifierBuilder = new JcaPGPRawDigestContentVerifierBuilderProvider()
                .get(signingKey.getAlgorithm(), signature.getHashAlgorithm());
        PGPContentVerifier pgpContentVerifier = pgpContentVerifierBuilder.build(signingKey.getPublicKey());
        byte[] signedMessageDigest = digest.digest();

        try {
            OutputStream outputStream = pgpContentVerifier.getOutputStream();
            outputStream.write(signedMessageDigest);
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean validSignature = pgpContentVerifier.verify(signature.getSignature());
        if (validSignature) {
            log.add(LogType.MSG_DC_CLEAR_SIGNATURE_OK, indent + 1);
        } else {
            log.add(LogType.MSG_DC_CLEAR_SIGNATURE_BAD, indent + 1);
        }

        // check for insecure hash algorithms
        if (!PgpSecurityConstants.isSecureHashAlgorithm(signature.getHashAlgorithm())) {
            log.add(LogType.MSG_DC_INSECURE_HASH_ALGO, indent + 1);
            signatureResultBuilder.setInsecure(true);
        }

        signatureResultBuilder.setValidSignature(validSignature);
        signatureResultBuilder.setSignedMessageDigest(signedMessageDigest);
    }

    public byte[] getSigningFingerprint() {
        return signingKey.getFingerprint();
    }

    public OpenPgpSignatureResult getSignatureResult() {
        return signatureResultBuilder.build();
    }

    /**
     * Mostly taken from ClearSignedFileProcessor in Bouncy Castle
     */

    private static void processLine(JcaPGPContentDigest digest, byte[] line)
            throws SignatureException {
        int length = getLengthWithoutWhiteSpace(line);
        if (length > 0) {
            digest.update(line, 0, length);
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
                lookAhead = readPastEOL(bOut, ch, fIn);
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
                lookAhead = readPastEOL(bOut, ch, fIn);
                break;
            }
        } while ((ch = fIn.read()) >= 0);

        if (ch < 0) {
            lookAhead = -1;
        }

        return lookAhead;
    }

    private static int readPastEOL(ByteArrayOutputStream bOut, int lastCh, InputStream fIn)
            throws IOException {
        int lookAhead = fIn.read();

        if (lastCh == '\r' && lookAhead == '\n') {
            bOut.write(lookAhead);
            lookAhead = fIn.read();
        }

        return lookAhead;
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

    private enum SignatureMode {
        DETACHED, CLEARTEXT, ONEPASS
    }

}
