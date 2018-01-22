/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketVector;
import org.bouncycastle.openpgp.PGPUserAttributeSubpacketVector;
import org.bouncycastle.openpgp.operator.jcajce.NfcSyncPGPContentSignerBuilder.NfcInteractionNeeded;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel.CertifyAction;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel.SecurityTokenSignOperationsBuilder;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import timber.log.Timber;


public class PgpCertifyOperation {

    public PgpCertifyResult certify(
            CanonicalizedSecretKey secretKey,
            CanonicalizedPublicKeyRing publicRing,
            OperationLog log,
            int indent,
            CertifyAction action,
            Map<ByteBuffer, byte[]> signedHashes,
            Date creationTimestamp) {

        if (!secretKey.isMasterKey()) {
            throw new AssertionError("tried to certify with non-master key, this is a programming error!");
        }
        if (publicRing.getMasterKeyId() == secretKey.getKeyId()) {
            throw new AssertionError("key tried to self-certify, this is a programming error!");
        }

        // create a signatureGenerator from the supplied masterKeyId and passphrase
        PGPSignatureGenerator signatureGenerator = secretKey.getCertSignatureGenerator(signedHashes);

        { // supply signatureGenerator with a SubpacketVector
            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            if (creationTimestamp != null) {
                spGen.setSignatureCreationTime(false, creationTimestamp);
                Timber.d("For NFC: set sig creation time to " + creationTimestamp);
            }
            PGPSignatureSubpacketVector packetVector = spGen.generate();
            signatureGenerator.setHashedSubpackets(packetVector);
        }

        // get the master subkey (which we certify for)
        PGPPublicKey publicKey = publicRing.getPublicKey().getPublicKey();

        SecurityTokenSignOperationsBuilder requiredInput = new SecurityTokenSignOperationsBuilder(creationTimestamp,
                publicKey.getKeyID(), publicKey.getKeyID());

        try {
            ArrayList<String> userIds = action.getUserIds();
            if (userIds != null && !userIds.isEmpty()) {
                log.add(LogType.MSG_CRT_CERTIFY_UIDS, 2, userIds.size(),
                        KeyFormattingUtils.convertKeyIdToHex(action.getMasterKeyId()));

                // fetch public key ring, add the certification and return it
                for (String userId : userIds) {
                    try {
                        PGPSignature sig = signatureGenerator.generateCertification(userId, publicKey);
                        publicKey = PGPPublicKey.addCertification(publicKey, userId, sig);
                    } catch (NfcInteractionNeeded e) {
                        requiredInput.addHash(e.hashToSign, e.hashAlgo);
                    }
                }

            }

            ArrayList<WrappedUserAttribute> userAttributes = action.getUserAttributes();
            if (userAttributes != null && !userAttributes.isEmpty()) {
                log.add(LogType.MSG_CRT_CERTIFY_UATS, 2, userAttributes.size(),
                        KeyFormattingUtils.convertKeyIdToHex(action.getMasterKeyId()));

                // fetch public key ring, add the certification and return it
                for (WrappedUserAttribute userAttribute : userAttributes) {
                    PGPUserAttributeSubpacketVector vector = userAttribute.getVector();
                    try {
                        PGPSignature sig = signatureGenerator.generateCertification(vector, publicKey);
                        publicKey = PGPPublicKey.addCertification(publicKey, vector, sig);
                    } catch (NfcInteractionNeeded e) {
                        requiredInput.addHash(e.hashToSign, e.hashAlgo);
                    }
                }

            }
        } catch (PGPException e) {
            Timber.e(e, "signing error");
            return new PgpCertifyResult();
        }

        if (!requiredInput.isEmpty()) {
            return new PgpCertifyResult(requiredInput.build());
        }

        PGPPublicKeyRing ring = PGPPublicKeyRing.insertPublicKey(publicRing.getRing(), publicKey);
        return new PgpCertifyResult(new UncachedKeyRing(ring));

    }

    public static class PgpCertifyResult {

        final RequiredInputParcel mRequiredInput;
        final UncachedKeyRing mCertifiedRing;

        PgpCertifyResult() {
            mRequiredInput = null;
            mCertifiedRing = null;
        }

        PgpCertifyResult(RequiredInputParcel requiredInput) {
            mRequiredInput = requiredInput;
            mCertifiedRing = null;
        }

        PgpCertifyResult(UncachedKeyRing certifiedRing) {
            mRequiredInput = null;
            mCertifiedRing = certifiedRing;
        }

        public boolean success() {
            return mCertifiedRing != null || mRequiredInput != null;
        }

        public boolean nfcInputRequired() {
            return mRequiredInput != null;
        }

        public UncachedKeyRing getCertifiedRing() {
            return mCertifiedRing;
        }

        public RequiredInputParcel getRequiredInput() {
            return mRequiredInput;
        }

    }

}
