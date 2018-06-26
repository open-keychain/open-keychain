package org.sufficientlysecure.keychain.ui.keyview;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;

import org.sufficientlysecure.keychain.livedata.GenericLiveData;
import org.sufficientlysecure.keychain.livedata.GenericLiveData.GenericDataLoader;


/** A simple generic ViewModel that can be used if exactly one field of data needs to be stored. */
public class GenericViewModel extends ViewModel {
    private LiveData genericLiveData;

    public <T> LiveData<T> getGenericLiveData(Context context, GenericDataLoader<T> func) {
        if (genericLiveData == null) {
            genericLiveData = new GenericLiveData<>(context, null, func);
        }
        // noinspection unchecked
        return genericLiveData;
    }
}
