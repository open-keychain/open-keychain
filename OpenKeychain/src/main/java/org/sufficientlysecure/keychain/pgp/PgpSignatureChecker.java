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
import java.security.SignatureException;

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderReader;
import org.sufficientlysecure.keychain.util.Log;


/** This class is used to track the state of a single signature verification.
 *
 *
 */
class PgpSignatureChecker {

    private final OpenPgpSignatureResultBuilder signatureResultBuilder;

    private CanonicalizedPublicKey signingKey;

    private int signatureIndex;
    PGPOnePassSignature onePassSignature;
    PGPSignature signature;

    ProviderHelper mProviderHelper;

    PgpSignatureChecker(ProviderHelper providerHelper, String senderAddress) {
        mProviderHelper = providerHelper;

        signatureResultBuilder = new OpenPgpSignatureResultBuilder(providerHelper);
        signatureResultBuilder.setSenderAddress(senderAddress);
    }

    boolean initializeSignature(Object dataChunk, OperationLog log, int indent) throws PGPException {

        if (!(dataChunk instanceof PGPSignatureList)) {
            return false;
        }

        PGPSignatureList sigList = (PGPSignatureList) dataChunk;
        findAvailableSignature(sigList);

        if (signingKey != null) {

            // key found in our database!
            signatureResultBuilder.initValid(signingKey);

            JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider =
                    new JcaPGPContentVerifierBuilderProvider()
                            .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
            signature.init(contentVerifierBuilderProvider, signingKey.getPublicKey());
            checkKeySecurity(log, indent);


        } else if (!sigList.isEmpty()) {

            signatureResultBuilder.setSignatureAvailable(true);
            signatureResultBuilder.setKnownKey(false);
            signatureResultBuilder.setKeyId(sigList.get(0).getKeyID());

        }

        return true;

    }

    boolean initializeOnePassSignature(Object dataChunk, OperationLog log, int indent) throws PGPException {

        if (!(dataChunk instanceof PGPOnePassSignatureList)) {
            return false;
        }

        log.add(LogType.MSG_DC_CLEAR_SIGNATURE, indent + 1);

        PGPOnePassSignatureList sigList = (PGPOnePassSignatureList) dataChunk;
        findAvailableSignature(sigList);

        if (signingKey != null) {

            // key found in our database!
            signatureResultBuilder.initValid(signingKey);

            JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider =
                    new JcaPGPContentVerifierBuilderProvider()
                            .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
            onePassSignature.init(contentVerifierBuilderProvider, signingKey.getPublicKey());

            checkKeySecurity(log, indent);

        } else if (!sigList.isEmpty()) {

            signatureResultBuilder.setSignatureAvailable(true);
            signatureResultBuilder.setKnownKey(false);
            signatureResultBuilder.setKeyId(sigList.get(0).getKeyID());

        }

        return true;

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

    private void findAvailableSignature(PGPOnePassSignatureList sigList) {
        // go through all signatures (should be just one), make sure we have
        //  the key and it matches the one we’re looking for
        for (int i = 0; i < sigList.size(); ++i) {
            try {
                long sigKeyId = sigList.get(i).getKeyID();
                CanonicalizedPublicKeyRing signingRing = mProviderHelper.read().getCanonicalizedPublicKeyRing(
                        KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(sigKeyId)
                );
                CanonicalizedPublicKey keyCandidate = signingRing.getPublicKey(sigKeyId);
                if ( ! keyCandidate.canSign()) {
                    continue;
                }
                signatureIndex = i;
                signingKey = keyCandidate;
                onePassSignature = sigList.get(i);
                return;
            } catch (ProviderReader.NotFoundException e) {
                Log.d(Constants.TAG, "key not found, trying next signature...");
            }
        }
    }

    public void findAvailableSignature(PGPSignatureList sigList) {
        // go through all signatures (should be just one), make sure we have
        //  the key and it matches the one we’re looking for
        for (int i = 0; i < sigList.size(); ++i) {
            try {
                long sigKeyId = sigList.get(i).getKeyID();
                CanonicalizedPublicKeyRing signingRing = mProviderHelper.read().getCanonicalizedPublicKeyRing(
                        KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(sigKeyId)
                );
                CanonicalizedPublicKey keyCandidate = signingRing.getPublicKey(sigKeyId);
                if ( ! keyCandidate.canSign()) {
                    continue;
                }
                signatureIndex = i;
                signingKey = keyCandidate;
                signature = sigList.get(i);
                return;
            } catch (ProviderReader.NotFoundException e) {
                Log.d(Constants.TAG, "key not found, trying next signature...");
            }
        }
    }

    public void updateSignatureWithCleartext(byte[] clearText) throws IOException, SignatureException {

        InputStream sigIn = new BufferedInputStream(new ByteArrayInputStream(clearText));

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

        int lookAhead = readInputLine(outputBuffer, sigIn);

        processLine(signature, outputBuffer.toByteArray());

        while (lookAhead != -1) {
            lookAhead = readInputLine(outputBuffer, lookAhead, sigIn);

            signature.update((byte) '\r');
            signature.update((byte) '\n');

            processLine(signature, outputBuffer.toByteArray());
        }

    }

    public void updateSignatureData(byte[] buf, int off, int len) {
        if (signature != null) {
            signature.update(buf, off, len);
        } else if (onePassSignature != null) {
            onePassSignature.update(buf, off, len);
        }
    }

    void verifySignature(OperationLog log, int indent) throws PGPException {

        log.add(LogType.MSG_DC_CLEAR_SIGNATURE_CHECK, indent);

        // Verify signature
        boolean validSignature = signature.verify();
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

    }

    boolean verifySignatureOnePass(Object o, OperationLog log, int indent) throws PGPException {

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
        PGPSignature messageSignature = signatureList.get(signatureList.size() - 1 - signatureIndex);

        // Verify signature
        boolean validSignature = onePassSignature.verify(messageSignature);
        if (validSignature) {
            log.add(LogType.MSG_DC_CLEAR_SIGNATURE_OK, indent + 1);
        } else {
            log.add(LogType.MSG_DC_CLEAR_SIGNATURE_BAD, indent + 1);
        }

        // check for insecure hash algorithms
        if (!PgpSecurityConstants.isSecureHashAlgorithm(onePassSignature.getHashAlgorithm())) {
            log.add(LogType.MSG_DC_INSECURE_HASH_ALGO, indent + 1);
            signatureResultBuilder.setInsecure(true);
        }

        signatureResultBuilder.setValidSignature(validSignature);

        return true;

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

}
