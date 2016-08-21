package org.sufficientlysecure.keychain.operations;

import android.content.Context;
import android.support.annotation.NonNull;
import org.sufficientlysecure.keychain.operations.results.ChangePassphraseWorkflowResult;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.ChangePassphraseWorkflowParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;

public class ChangePassphraseWorkflowOperation extends BaseOperation<ChangePassphraseWorkflowParcel> {
    public static final String CACHE_FILE_NAME = "change_workflow.pcl";

    public ChangePassphraseWorkflowOperation(Context context, ProviderHelper providerHelper,
                                             Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    @NonNull
    @Override
    public ChangePassphraseWorkflowResult execute(ChangePassphraseWorkflowParcel changeParcel,
                                                  CryptoInputParcel cryptoInputParcel) {
        mProgressable.setPreventCancel();
        return mProviderHelper.changePassphraseWorkflowOperation(
                mProgressable,
                CACHE_FILE_NAME,
                changeParcel.mPassphrases,
                changeParcel.mMasterPassphrase,
                changeParcel.mToSinglePassphraseWorkflow
        );
    }

}
