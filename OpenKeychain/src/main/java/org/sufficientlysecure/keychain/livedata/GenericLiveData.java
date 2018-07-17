package org.sufficientlysecure.keychain.livedata;


import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;

import org.sufficientlysecure.keychain.daos.DatabaseNotifyManager;
import org.sufficientlysecure.keychain.ui.keyview.loader.AsyncTaskLiveData;


public class GenericLiveData<T> extends AsyncTaskLiveData<T> {
    private GenericDataLoader<T> genericDataLoader;
    private Long minLoadTime;

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

    public void setMinLoadTime(Long minLoadTime) {
        this.minLoadTime = minLoadTime;
    }

    @Override
    protected T asyncLoadData() {
        long startTime = SystemClock.elapsedRealtime();

        T result = genericDataLoader.loadData();

        try {
            long elapsedTime = SystemClock.elapsedRealtime() - startTime;
            if (minLoadTime != null && elapsedTime < minLoadTime) {
                Thread.sleep(minLoadTime - elapsedTime);
            }
        } catch (InterruptedException e) {
            // nvm
        }

        return result;
    }

    public interface GenericDataLoader<T> {
        T loadData();
    }

}
