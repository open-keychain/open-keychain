package org.sufficientlysecure.keychain.pgp;


import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUserAttributeSubpacketVector;
import org.sufficientlysecure.keychain.util.Utf8Util;


@SuppressWarnings("unchecked") // BouncyCastle doesn't do generics here :(
class PGPPublicKeyUtils {

    static PGPPublicKey keepOnlyRawUserId(PGPPublicKey masterPublicKey, byte[] rawUserIdToKeep) {
        boolean elementToKeepFound = false;

        Iterator<byte[]> it = masterPublicKey.getRawUserIDs();
        while (it.hasNext()) {
            byte[] rawUserId = it.next();
            if (Arrays.equals(rawUserId, rawUserIdToKeep)) {
                elementToKeepFound = true;
            } else {
                masterPublicKey = PGPPublicKey.removeCertification(masterPublicKey, rawUserId);
            }
        }

        if (!elementToKeepFound) {
            throw new NoSuchElementException();
        }
        return masterPublicKey;
    }

    static PGPPublicKey keepOnlyUserId(PGPPublicKey masterPublicKey, String userIdToKeep) {
        boolean elementToKeepFound = false;

        Iterator<byte[]> it = masterPublicKey.getRawUserIDs();
        while (it.hasNext()) {
            byte[] rawUserId = it.next();
            String userId = Utf8Util.fromUTF8ByteArrayReplaceBadEncoding(rawUserId);
            if (userId.contains(userIdToKeep)) {
                elementToKeepFound = true;
            } else {
                masterPublicKey = PGPPublicKey.removeCertification(masterPublicKey, rawUserId);
            }
        }

        if (!elementToKeepFound) {
            throw new NoSuchElementException();
        }
        return masterPublicKey;
    }

    static PGPPublicKey keepOnlySelfCertsForUserIds(PGPPublicKey masterPubKey) {
        long masterKeyId = masterPubKey.getKeyID();

        Iterator<byte[]> it = masterPubKey.getRawUserIDs();
        while (it.hasNext()) {
            byte[] rawUserId = it.next();
            masterPubKey = keepOnlySelfCertsForRawUserId(masterPubKey, masterKeyId, rawUserId);
        }

        return masterPubKey;
    }

    private static PGPPublicKey keepOnlySelfCertsForRawUserId(
            PGPPublicKey masterPubKey, long masterKeyId, byte[] rawUserId) {
        Iterator<PGPSignature> it = masterPubKey.getSignaturesForID(rawUserId);
        while (it.hasNext()) {
            PGPSignature sig = it.next();
            if (sig.getKeyID() != masterKeyId) {
                masterPubKey = PGPPublicKey.removeCertification(masterPubKey, sig);
            }
        }
        return masterPubKey;
    }

    static PGPPublicKey removeAllUserAttributes(PGPPublicKey masterPubKey) {
        Iterator<PGPUserAttributeSubpacketVector> it = masterPubKey.getUserAttributes();

        while (it.hasNext()) {
            masterPubKey = PGPPublicKey.removeCertification(masterPubKey, it.next());
        }

        return masterPubKey;
    }

    static PGPPublicKey removeAllDirectKeyCerts(PGPPublicKey masterPubKey) {
        Iterator<PGPSignature> it = masterPubKey.getSignaturesOfType(PGPSignature.DIRECT_KEY);

        while (it.hasNext()) {
            masterPubKey = PGPPublicKey.removeCertification(masterPubKey, it.next());
        }

        return masterPubKey;
    }
}
