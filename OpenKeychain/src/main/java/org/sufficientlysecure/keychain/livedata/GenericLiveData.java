package org.sufficientlysecure.keychain.livedata;


import android.content.Context;
import android.net.Uri;

import org.sufficientlysecure.keychain.ui.keyview.loader.AsyncTaskLiveData;


public class GenericLiveData<T> extends AsyncTaskLiveData<T> {
    private GenericDataLoader<T> genericDataLoader;

    public GenericLiveData(Context context, Uri uri, GenericDataLoader<T> genericDataLoader) {
        super(context, uri);
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
