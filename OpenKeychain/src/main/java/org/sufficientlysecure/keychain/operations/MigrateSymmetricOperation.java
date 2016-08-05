package org.sufficientlysecure.keychain.operations;

import android.content.Context;
import android.support.annotation.NonNull;
import org.sufficientlysecure.keychain.operations.results.MigrateSymmetricResult;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.MigrateSymmetricInputParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;

public class MigrateSymmetricOperation extends BaseOperation<MigrateSymmetricInputParcel> {
    public static final String CACHE_FILE_NAME = "migrate_to_symmetric.pcl";

    public MigrateSymmetricOperation(Context context, ProviderHelper providerHelper, Progressable
            progressable) {
        super(context, providerHelper, progressable);
    }

    @NonNull
    @Override
    public MigrateSymmetricResult execute(MigrateSymmetricInputParcel migrateParcel,
                                          CryptoInputParcel cryptoInputParcel) {
        mProgressable.setPreventCancel();
        return mProviderHelper.migrateSymmetricOperation(mProgressable, CACHE_FILE_NAME, migrateParcel.mKeyringPassphrasesList);
    }

    public static class CreateSecretCacheOperation extends BaseOperation<MigrateSymmetricInputParcel.CreateSecretCacheParcel> {

        public CreateSecretCacheOperation(Context context, ProviderHelper providerHelper, Progressable
                progressable) {
            super(context, providerHelper, progressable);
        }

        @NonNull
        public MigrateSymmetricResult execute(MigrateSymmetricInputParcel.CreateSecretCacheParcel createSecretCacheParcel,
                                              CryptoInputParcel cryptoInputParcel) {
            mProgressable.setPreventCancel();
            return mProviderHelper.createSecretKeyRingCache(mProgressable, CACHE_FILE_NAME);
        }
    }
}
