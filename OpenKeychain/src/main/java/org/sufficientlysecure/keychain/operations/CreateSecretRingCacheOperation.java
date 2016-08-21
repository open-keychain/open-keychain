package org.sufficientlysecure.keychain.operations;

import android.content.Context;
import android.support.annotation.NonNull;
import org.sufficientlysecure.keychain.operations.results.CreateSecretKeyRingCacheResult;
import org.sufficientlysecure.keychain.operations.results.MigrateSymmetricResult;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.CreateSecretKeyRingCacheParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;

/**
 * Caches all secret key rings into an internal file
 */
public class CreateSecretRingCacheOperation extends BaseOperation<CreateSecretKeyRingCacheParcel> {

    public CreateSecretRingCacheOperation(Context context, ProviderHelper providerHelper, Progressable
            progressable) {
        super(context, providerHelper, progressable);
    }

    @NonNull
    public CreateSecretKeyRingCacheResult execute(CreateSecretKeyRingCacheParcel createSecretCacheParcel,
                                          CryptoInputParcel cryptoInputParcel) {
        mProgressable.setPreventCancel();
        return mProviderHelper.createSecretKeyRingCache(mProgressable, createSecretCacheParcel.mFileName);
    }
}
