package org.sufficientlysecure.keychain.ui.keyunlock.wizard;


import android.content.Context;
import android.os.Bundle;

import org.sufficientlysecure.keychain.ui.keyunlock.base.BaseViewModel;

public class NameWizardFragmentViewModel implements BaseViewModel {
    private Context mContext;

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {
        mContext = context;

    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void onViewModelCreated() {

    }

    public boolean isTextEmpty(CharSequence text) {
        return text.length() == 0;
    }
}
