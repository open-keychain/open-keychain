package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.S2K;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.util.IterableIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class UncachedSecretKeyRing extends UncachedKeyRing {

    UncachedSecretKeyRing(PGPSecretKeyRing secretRing) {
        super(secretRing);
    }

    public ArrayList<Long> getAvailableSubkeys() {
        ArrayList<Long> result = new ArrayList<Long>();
        // then, mark exactly the keys we have available
        for (PGPSecretKey sub : new IterableIterator<PGPSecretKey>(
                ((PGPSecretKeyRing) mRing).getSecretKeys())) {
            S2K s2k = sub.getS2K();
            // Set to 1, except if the encryption type is GNU_DUMMY_S2K
            if(s2k == null || s2k.getType() != S2K.GNU_DUMMY_S2K) {
                result.add(sub.getKeyID());
            }
        }
        return result;
    }

}
