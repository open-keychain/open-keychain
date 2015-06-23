package org.sufficientlysecure.keychain.operations;

import android.content.Context;

import org.sufficientlysecure.keychain.operations.results.ConsolidateResult;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.ConsolidateInputParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;

public class ConsolidateOperation extends BaseOperation<ConsolidateInputParcel> {

    public ConsolidateOperation(Context context, ProviderHelper providerHelper, Progressable
            progressable) {
        super(context, providerHelper, progressable);
    }

    @Override
    public ConsolidateResult execute(ConsolidateInputParcel consolidateInputParcel,
                                     CryptoInputParcel cryptoInputParcel) {
        if (consolidateInputParcel.mConsolidateRecovery) {
            return mProviderHelper.consolidateDatabaseStep2(mProgressable);
        } else {
            return mProviderHelper.consolidateDatabaseStep1(mProgressable);
        }
    }
}
