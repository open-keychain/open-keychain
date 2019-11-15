package org.sufficientlysecure.keychain.ui.keyview;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import android.content.Context;

import org.sufficientlysecure.keychain.livedata.GenericLiveData;
import org.sufficientlysecure.keychain.livedata.GenericLiveData.GenericDataLoader;
import org.sufficientlysecure.keychain.daos.DatabaseNotifyManager;


/** A simple generic ViewModel that can be used if exactly one field of data needs to be stored. */
public class GenericViewModel extends ViewModel {
    private LiveData genericLiveData;

    public <T> LiveData<T> getGenericLiveData(Context context, GenericDataLoader<T> func) {
        if (genericLiveData == null) {
            genericLiveData = new GenericLiveData<>(context, DatabaseNotifyManager.getNotifyUriAllKeys(), func);
        }
        // noinspection unchecked
        return genericLiveData;
    }
}
