package org.sufficientlysecure.keychain.testsupport;

import android.content.Context;
import android.net.Uri;

import org.sufficientlysecure.keychain.pgp.WrappedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;

/**
 * Created by art on 21/06/14.
 */
class ProviderHelperStub extends ProviderHelper {
    public ProviderHelperStub(Context context) {
        super(context);
    }

    @Override
    public WrappedPublicKeyRing getWrappedPublicKeyRing(Uri id) throws NotFoundException {
        byte[] data = TestDataUtil.readFully(getClass().getResourceAsStream("/public-key-for-sample.blob"));
        return new WrappedPublicKeyRing(data, false, 0);
    }
}
