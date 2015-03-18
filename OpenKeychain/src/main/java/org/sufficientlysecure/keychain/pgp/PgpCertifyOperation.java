package org.sufficientlysecure.keychain.pgp;


import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.PGPUserAttributeSubpacketVector;
import org.spongycastle.openpgp.operator.jcajce.NfcSyncPGPContentSignerBuilder.NfcInteractionNeeded;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel.CertifyAction;
import org.sufficientlysecure.keychain.service.input.NfcOperationsParcel;
import org.sufficientlysecure.keychain.service.input.NfcOperationsParcel.NfcSignOperationsBuilder;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;


public class PgpCertifyOperation {

    public PgpCertifyResult certify(
            CanonicalizedSecretKey secretKey,
            CanonicalizedPublicKeyRing publicRing,
            OperationLog log,
            int indent,
            CertifyAction action,
            Map<ByteBuffer,byte[]> signedHashes,
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
                Log.d(Constants.TAG, "For NFC: set sig creation time to " + creationTimestamp);
            }
            PGPSignatureSubpacketVector packetVector = spGen.generate();
            signatureGenerator.setHashedSubpackets(packetVector);
        }

        // get the master subkey (which we certify for)
        PGPPublicKey publicKey = publicRing.getPublicKey().getPublicKey();

        NfcSignOperationsBuilder requiredInput = new NfcSignOperationsBuilder(creationTimestamp);

        try {
            if (action.mUserIds != null) {
                log.add(LogType.MSG_CRT_CERTIFY_UIDS, 2, action.mUserIds.size(),
                        KeyFormattingUtils.convertKeyIdToHex(action.mMasterKeyId));

                // fetch public key ring, add the certification and return it
                for (String userId : action.mUserIds) {
                    try {
                        PGPSignature sig = signatureGenerator.generateCertification(userId, publicKey);
                        publicKey = PGPPublicKey.addCertification(publicKey, userId, sig);
                    } catch (NfcInteractionNeeded e) {
                        requiredInput.addHash(e.hashToSign, e.hashAlgo);
                    }
                }

            }

            if (action.mUserAttributes != null) {
                log.add(LogType.MSG_CRT_CERTIFY_UATS, 2, action.mUserAttributes.size(),
                        KeyFormattingUtils.convertKeyIdToHex(action.mMasterKeyId));

                // fetch public key ring, add the certification and return it
                for (WrappedUserAttribute userAttribute : action.mUserAttributes) {
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
            Log.e(Constants.TAG, "signing error", e);
            return new PgpCertifyResult();
        }

        if (!requiredInput.isEmpty()) {
            return new PgpCertifyResult(requiredInput.build());
        }

        PGPPublicKeyRing ring = PGPPublicKeyRing.insertPublicKey(publicRing.getRing(), publicKey);
        return new PgpCertifyResult(new UncachedKeyRing(ring));

    }

    public static class PgpCertifyResult {

        final NfcOperationsParcel mRequiredInput;
        final UncachedKeyRing mCertifiedRing;

        PgpCertifyResult() {
            mRequiredInput = null;
            mCertifiedRing = null;
        }

        PgpCertifyResult(NfcOperationsParcel requiredInput) {
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

        public NfcOperationsParcel getRequiredInput() {
            return mRequiredInput;
        }

    }

}
