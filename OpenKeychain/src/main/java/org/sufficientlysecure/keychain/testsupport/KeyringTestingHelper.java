package org.sufficientlysecure.keychain.testsupport;

import android.content.Context;

import org.sufficientlysecure.keychain.pgp.NullProgressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.remote.AppSettings;
import org.sufficientlysecure.keychain.service.OperationResults;

/**
 * Helper for tests of the Keyring import in ProviderHelper.
 */
public class KeyringTestingHelper {

    private final Context context;

    public KeyringTestingHelper(Context robolectricContext) {
        this.context = robolectricContext;
    }

    public boolean addKeyring() throws Exception {

        ProviderHelper providerHelper = new ProviderHelper(context);

//        providerHelper.insertApiApp(new AppSettings("robo-test-package", new byte[]{5, 4, 3, 2, 1}));

        byte[] data = TestDataUtil.readFully(getClass().getResourceAsStream("/public-key-for-sample.blob"));
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
