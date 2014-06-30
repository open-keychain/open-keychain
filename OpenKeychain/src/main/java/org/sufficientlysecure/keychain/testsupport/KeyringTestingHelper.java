package org.sufficientlysecure.keychain.testsupport;

import android.content.Context;

import org.sufficientlysecure.keychain.pgp.NullProgressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.OperationResults;

import java.util.Collection;

/**
 * Helper for tests of the Keyring import in ProviderHelper.
 */
public class KeyringTestingHelper {

    private final Context context;

    public KeyringTestingHelper(Context robolectricContext) {
        this.context = robolectricContext;
    }

    public boolean addKeyring(Collection<String> blobFiles) throws Exception {

        ProviderHelper providerHelper = new ProviderHelper(context);

        byte[] data = TestDataUtil.readAllFully(blobFiles);
        UncachedKeyRing ring = UncachedKeyRing.decodeFromData(data);
        long masterKeyId = ring.getMasterKeyId();

        // Should throw an exception; key is not yet saved
        retrieveKeyAndExpectNotFound(providerHelper, masterKeyId);

        OperationResults.SaveKeyringResult saveKeyringResult = providerHelper.savePublicKeyRing(ring, new NullProgressable());

        boolean saveSuccess = saveKeyringResult.success();

        // Now re-retrieve the saved key. Should not throw an exception.
        providerHelper.getWrappedPublicKeyRing(masterKeyId);

        // A different ID should still fail
        retrieveKeyAndExpectNotFound(providerHelper, masterKeyId - 1);

        return saveSuccess;
    }


    private void retrieveKeyAndExpectNotFound(ProviderHelper providerHelper, long masterKeyId) {
        try {
            providerHelper.getWrappedPublicKeyRing(masterKeyId);
            throw new AssertionError("Was expecting the previous call to fail!");
        } catch (ProviderHelper.NotFoundException expectedException) {
            // good
        }
    }
}
