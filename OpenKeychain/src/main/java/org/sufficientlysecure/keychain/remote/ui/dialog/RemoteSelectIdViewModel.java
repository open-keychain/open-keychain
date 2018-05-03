package org.sufficientlysecure.keychain.remote.ui.dialog;


import android.arch.lifecycle.ViewModel;
import android.content.Context;

import org.sufficientlysecure.keychain.livedata.KeyInfoLiveData;
import org.sufficientlysecure.keychain.livedata.PgpKeyGenerationLiveData;


public class RemoteSelectIdViewModel extends ViewModel {

    private KeyInfoLiveData keyInfo;
    private PgpKeyGenerationLiveData keyGenerationData;
    private boolean listAllKeys;

    public KeyInfoLiveData getKeyInfo(Context context) {
        if (keyInfo == null) {
            keyInfo = new KeyInfoLiveData(context, context.getContentResolver());
        }
        return keyInfo;
    }

    public PgpKeyGenerationLiveData getKeyGenerationLiveData(Context context) {
        if (keyGenerationData == null) {
            keyGenerationData = new PgpKeyGenerationLiveData(context);
        }
        return keyGenerationData;
    }

    public boolean isListAllKeys() {
        return listAllKeys;
    }

    public void setListAllKeys(boolean listAllKeys) {
        this.listAllKeys = listAllKeys;
    }

}
