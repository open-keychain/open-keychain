package org.sufficientlysecure.keychain.livedata;


import java.util.List;

import android.content.ContentResolver;
import android.content.Context;

import org.sufficientlysecure.keychain.livedata.KeyInfoInteractor.KeyInfo;
import org.sufficientlysecure.keychain.livedata.KeyInfoInteractor.KeySelector;
import org.sufficientlysecure.keychain.ui.keyview.loader.AsyncTaskLiveData;


public class KeyInfoLiveData extends AsyncTaskLiveData<List<KeyInfo>> {
    private final KeyInfoInteractor keyInfoInteractor;

    private KeySelector keySelector;

    public KeyInfoLiveData(Context context, ContentResolver contentResolver) {
        super(context, null);

        this.keyInfoInteractor = new KeyInfoInteractor(contentResolver);
    }

    public void setKeySelector(KeySelector keySelector) {
        this.keySelector = keySelector;

        updateDataInBackground();
    }

    @Override
    protected List<KeyInfo> asyncLoadData() {
        if (keySelector == null) {
            return null;
        }
        return keyInfoInteractor.loadKeyInfo(keySelector);
    }
}
