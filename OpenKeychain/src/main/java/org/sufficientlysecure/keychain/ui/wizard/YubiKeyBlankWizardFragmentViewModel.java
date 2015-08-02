package org.sufficientlysecure.keychain.ui.wizard;

import android.app.Activity;
import android.os.Bundle;

import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.base.WizardFragmentListener;

public class YubiKeyBlankWizardFragmentViewModel implements BaseViewModel {
    private Activity mActivity;
    private OnViewModelEventBind mOnViewModelEventBind;
    private WizardFragmentListener mWizardFragmentListener;

    /**
     * View Model communication
     */
    public interface OnViewModelEventBind {
    }

    public YubiKeyBlankWizardFragmentViewModel(OnViewModelEventBind onViewModelEventBind,
                                               WizardFragmentListener wizardActivity) {
        mOnViewModelEventBind = onViewModelEventBind;
        mWizardFragmentListener = wizardActivity;

        if (mOnViewModelEventBind == null || mWizardFragmentListener == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Activity activity) {
        mActivity = activity;
    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {

    }

    public boolean onBackClicked() {
        if (mActivity.getFragmentManager().getBackStackEntryCount() == 0) {
            mActivity.setResult(Activity.RESULT_CANCELED);
            mActivity.finish();
            return false;
        }
        return true;
    }

    public boolean onNextClicked() {
        mWizardFragmentListener.setUseYubiKey();
        return true;
    }
}
