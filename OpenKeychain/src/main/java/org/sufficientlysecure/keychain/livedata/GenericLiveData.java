package org.sufficientlysecure.keychain.livedata;


import android.content.Context;
import android.net.Uri;

import org.sufficientlysecure.keychain.daos.DatabaseNotifyManager;
import org.sufficientlysecure.keychain.ui.keyview.loader.AsyncTaskLiveData;


public class GenericLiveData<T> extends AsyncTaskLiveData<T> {
    private GenericDataLoader<T> genericDataLoader;

    public GenericLiveData(Context context, GenericDataLoader<T> genericDataLoader) {
        super(context, null);
        this.genericDataLoader = genericDataLoader;
    }

    public GenericLiveData(Context context, Uri notifyUri, GenericDataLoader<T> genericDataLoader) {
        super(context, notifyUri);
        this.genericDataLoader = genericDataLoader;
    }

    public GenericLiveData(Context context, long notifyMasterKeyId, GenericDataLoader<T> genericDataLoader) {
        super(context, DatabaseNotifyManager.getNotifyUriMasterKeyId(notifyMasterKeyId));
        this.genericDataLoader = genericDataLoader;
    }

    @Override
    protected T asyncLoadData() {
        return genericDataLoader.loadData();
    }

    public interface GenericDataLoader<T> {
        T loadData();
    }
}
