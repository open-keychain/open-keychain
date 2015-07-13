package org.sufficientlysecure.keychain.ui.wizard;


import android.content.Context;
import android.os.Bundle;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;

public class NameWizardFragmentViewModel implements BaseViewModel {
    private Context mContext;
    private OnViewModelEventBind mOnViewModelEventBind;

    /**
     * View Model communication
     */
    public interface OnViewModelEventBind {

        CharSequence getName();

        void showNameError(CharSequence error, boolean focus);
    }

    public NameWizardFragmentViewModel(OnViewModelEventBind viewModelEventBind) {
        mOnViewModelEventBind = viewModelEventBind;

        if (mOnViewModelEventBind == null) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {
        mContext = context;
    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {

    }

    /**
     * Checks if the text is empty.
     *
     * @param text
     * @return
     */
    public boolean isTextEmpty(CharSequence text) {
        return text.length() == 0;
    }

    /**
     * Checks if the edit text is not empty. If it is empty an error is
     * set and the EditText gets the focus.
     *
     * @return true if EditText is not empty
     */
    public boolean isEditTextNotEmpty() {
        if (isTextEmpty(mOnViewModelEventBind.getName())) {
            mOnViewModelEventBind.showNameError(mContext.getString(R.string.
                    create_key_empty), true);
            return false;
        } else {
            mOnViewModelEventBind.showNameError(null, false);
            return true;
        }
    }
}
