package org.sufficientlysecure.keychain.testsupport;

import org.spongycastle.bcpg.BCPGKey;
import org.spongycastle.bcpg.PublicKeyAlgorithmTags;
import org.spongycastle.bcpg.PublicKeyPacket;
import org.spongycastle.bcpg.RSAPublicBCPGKey;
import org.spongycastle.bcpg.SignatureSubpacket;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.PGPUserAttributeSubpacketVector;
import org.spongycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.service.OperationResultParcel;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * Created by art on 28/06/14.
 */
public class UncachedKeyringTestingHelper {

    public static boolean compareRing(UncachedKeyRing keyRing1, UncachedKeyRing keyRing2) {
        OperationResultParcel.OperationLog operationLog = new OperationResultParcel.OperationLog();
        UncachedKeyRing canonicalized = keyRing1.canonicalize(operationLog, 0);

        if (canonicalized == null) {
            throw new AssertionError("Canonicalization failed; messages: [" + operationLog.toString() + "]");
        }

        return TestDataUtil.iterEquals(canonicalized.getPublicKeys(), keyRing2.getPublicKeys(), new
                TestDataUtil.EqualityChecker<UncachedPublicKey>() {
                    @Override
                    public boolean areEquals(UncachedPublicKey lhs, UncachedPublicKey rhs) {
                        return comparePublicKey(lhs, rhs);
                    }
                });
    }

    public static boolean comparePublicKey(UncachedPublicKey key1, UncachedPublicKey key2) {
        boolean equal = true;

        if (key1.canAuthenticate() != key2.canAuthenticate()) {
            return false;
        }
        if (key1.canCertify() != key2.canCertify()) {
            return false;
        }
        if (key1.canEncrypt() != key2.canEncrypt()) {
            return false;
        }
        if (key1.canSign() != key2.canSign()) {
            return false;
        }
        if (key1.getAlgorithm() != key2.getAlgorithm()) {
            return false;
        }
        if (key1.getBitStrength() != key2.getBitStrength()) {
            return false;
        }
        if (!TestDataUtil.equals(key1.getCreationTime(), key2.getCreationTime())) {
            return false;
        }
        if (!TestDataUtil.equals(key1.getExpiryTime(), key2.getExpiryTime())) {
            return false;
        }
        if (!Arrays.equals(key1.getFingerprint(), key2.getFingerprint())) {
            return false;
        }
        if (key1.getKeyId() != key2.getKeyId()) {
            return false;
        }
        if (key1.getKeyUsage() != key2.getKeyUsage()) {
            return false;
        }
        if (!TestDataUtil.equals(key1.getPrimaryUserId(), key2.getPrimaryUserId())) {
            return false;
        }

        // Ooops, getPublicKey is due to disappear. But then how to compare?
        if (!keysAreEqual(key1.getPublicKey(), key2.getPublicKey())) {
            return false;
        }

        return equal;
    }

    public static boolean keysAreEqual(PGPPublicKey a, PGPPublicKey b) {

        if (a.getAlgorithm() != b.getAlgorithm()) {
            return false;
        }

        if (a.getBitStrength() != b.getBitStrength()) {
            return false;
        }

        if (!TestDataUtil.equals(a.getCreationTime(), b.getCreationTime())) {
            return false;
        }

        if (!Arrays.equals(a.getFingerprint(), b.getFingerprint())) {
            return false;
        }

        if (a.getKeyID() != b.getKeyID()) {
            return false;
        }

        if (!pubKeyPacketsAreEqual(a.getPublicKeyPacket(), b.getPublicKeyPacket())) {
            return false;
        }

        if (a.getVersion() != b.getVersion()) {
            return false;
        }

        if (a.getValidDays() != b.getValidDays()) {
            return false;
        }

        if (a.getValidSeconds() != b.getValidSeconds()) {
            return false;
        }

        if (!Arrays.equals(a.getTrustData(), b.getTrustData())) {
            return false;
        }

        if (!TestDataUtil.iterEquals(a.getUserIDs(), b.getUserIDs())) {
            return false;
        }

        if (!TestDataUtil.iterEquals(a.getUserAttributes(), b.getUserAttributes(),
                new TestDataUtil.EqualityChecker<PGPUserAttributeSubpacketVector>() {
                    public boolean areEquals(PGPUserAttributeSubpacketVector lhs, PGPUserAttributeSubpacketVector rhs) {
                        // For once, BC defines equals, so we use it implicitly.
                        return TestDataUtil.equals(lhs, rhs);
                    }
                }
        )) {
            return false;
        }


        if (!TestDataUtil.iterEquals(a.getSignatures(), b.getSignatures(),
                new TestDataUtil.EqualityChecker<PGPSignature>() {
                    public boolean areEquals(PGPSignature lhs, PGPSignature rhs) {
                        return signaturesAreEqual(lhs, rhs);
                    }
                }
        )) {
            return false;
        }

        return true;
    }

    public static boolean signaturesAreEqual(PGPSignature a, PGPSignature b) {

        if (a.getVersion() != b.getVersion()) {
            return false;
        }

        if (a.getKeyAlgorithm() != b.getKeyAlgorithm()) {
            return false;
        }

        if (a.getHashAlgorithm() != b.getHashAlgorithm()) {
            return false;
        }

        if (a.getSignatureType() != b.getSignatureType()) {
            return false;
        }

        try {
            if (!Arrays.equals(a.getSignature(), b.getSignature())) {
                return false;
            }
        } catch (PGPException ex) {
            throw new RuntimeException(ex);
        }

        if (a.getKeyID() != b.getKeyID()) {
            return false;
        }

        if (!TestDataUtil.equals(a.getCreationTime(), b.getCreationTime())) {
            return false;
        }

        if (!Arrays.equals(a.getSignatureTrailer(), b.getSignatureTrailer())) {
            return false;
        }

        if (!subPacketVectorsAreEqual(a.getHashedSubPackets(), b.getHashedSubPackets())) {
            return false;
        }

        if (!subPacketVectorsAreEqual(a.getUnhashedSubPackets(), b.getUnhashedSubPackets())) {
            return false;
        }

        return true;
    }

    private static boolean subPacketVectorsAreEqual(PGPSignatureSubpacketVector aHashedSubPackets, PGPSignatureSubpacketVector bHashedSubPackets) {
        for (int i = 0; i < Byte.MAX_VALUE; i++) {
            if (!TestDataUtil.iterEquals(Arrays.asList(aHashedSubPackets.getSubpackets(i)).iterator(),
                    Arrays.asList(bHashedSubPackets.getSubpackets(i)).iterator(),
                    new TestDataUtil.EqualityChecker<SignatureSubpacket>() {
                        @Override
                        public boolean areEquals(SignatureSubpacket lhs, SignatureSubpacket rhs) {
                            return signatureSubpacketsAreEqual(lhs, rhs);
                        }
                    }
            )) {
                return false;
            }

        }
        return true;
    }

    private static boolean signatureSubpacketsAreEqual(SignatureSubpacket lhs, SignatureSubpacket rhs) {
        if (lhs.getType() != rhs.getType()) {
            return false;
        }
        if (!Arrays.equals(lhs.getData(), rhs.getData())) {
            return false;
        }
        return true;
    }

    public static boolean pubKeyPacketsAreEqual(PublicKeyPacket a, PublicKeyPacket b) {

        if (a.getAlgorithm() != b.getAlgorithm()) {
            return false;
        }

        if (!bcpgKeysAreEqual(a.getKey(), b.getKey())) {
            return false;
        }

        if (!TestDataUtil.equals(a.getTime(), b.getTime())) {
            return false;
        }

        if (a.getValidDays() != b.getValidDays()) {
            return false;
        }

        if (a.getVersion() != b.getVersion()) {
            return false;
        }

        return true;
    }

    public static boolean bcpgKeysAreEqual(BCPGKey a, BCPGKey b) {

        if (!TestDataUtil.equals(a.getFormat(), b.getFormat())) {
            return false;
        }

        if (!Arrays.equals(a.getEncoded(), b.getEncoded())) {
            return false;
        }

        return true;
    }


    public void doTestCanonicalize(UncachedKeyRing inputKeyRing, UncachedKeyRing expectedKeyRing) {
        if (!compareRing(inputKeyRing, expectedKeyRing)) {
            throw new AssertionError("Expected [" + inputKeyRing + "] to match [" + expectedKeyRing + "]");
        }
    }

}
