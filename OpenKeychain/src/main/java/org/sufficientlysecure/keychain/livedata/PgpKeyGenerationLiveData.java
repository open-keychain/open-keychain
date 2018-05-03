package org.sufficientlysecure.keychain.livedata;


import android.content.Context;

import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.keyview.loader.AsyncTaskLiveData;
import org.sufficientlysecure.keychain.util.ProgressScaler;


public class PgpKeyGenerationLiveData extends AsyncTaskLiveData<PgpEditKeyResult> {
    private SaveKeyringParcel saveKeyringParcel;

    public PgpKeyGenerationLiveData(Context context) {
        super(context, null);
    }

    public void setSaveKeyringParcel(SaveKeyringParcel saveKeyringParcel) {
        if (this.saveKeyringParcel == saveKeyringParcel) {
            return;
        }
        this.saveKeyringParcel = saveKeyringParcel;

        updateDataInBackground();
    }

    @Override
    protected PgpEditKeyResult asyncLoadData() {
        if (saveKeyringParcel == null) {
            return null;
        }

        PgpKeyOperation keyOperations = new PgpKeyOperation(new ProgressScaler());
        return keyOperations.createSecretKeyRing(saveKeyringParcel);
    }
}
