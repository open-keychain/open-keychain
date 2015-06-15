package org.sufficientlysecure.keychain.ui.keyunlock.base;

import android.content.Context;
import android.os.Bundle;

/**
 * Interface for View Model implementations
 */
public interface BaseViewModel {
    void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context);
    void saveViewModelState(Bundle outState);
    void restoreViewModelState(Bundle savedInstanceState);
    void onViewModelCreated();
}
