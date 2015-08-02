package org.sufficientlysecure.keychain.ui.wizard;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.ui.ImportKeysActivity;
import org.sufficientlysecure.keychain.ui.MainActivity;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.base.WizardFragmentListener;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

public class WelcomeWizardFragmentViewModel implements BaseViewModel {
    public static final int REQUEST_CODE_IMPORT_KEY = 0x00007012;
    private Activity mActivity;
    private OnViewModelEventBind mOnViewModelEventBind;
    private WizardFragmentListener mWizardFragmentListener;

    /**
     * View Model communication
     */
    public interface OnViewModelEventBind {
        void hideNavigationButtons(boolean hideBack, boolean hideNext);
    }

    public WelcomeWizardFragmentViewModel(OnViewModelEventBind onViewModelEventBind,
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
        mOnViewModelEventBind.hideNavigationButtons(true, true);
    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_IMPORT_KEY) {
            if (resultCode == Activity.RESULT_OK) {
                if (mWizardFragmentListener != null) {
                    if (mWizardFragmentListener.isFirstTime()) {
                        Preferences prefs = Preferences.getPreferences(mActivity);
                        prefs.setFirstTime(false);
                        Intent intent = new Intent(mActivity, MainActivity.class);
                        intent.putExtras(data);
                        mActivity.startActivity(intent);
                        mActivity.finish();
                    } else {
                        // just finish activity and return data
                        mActivity.setResult(Activity.RESULT_OK, data);
                        mActivity.finish();
                    }
                }
            }
        } else {
            Log.e(Constants.TAG, "No valid request code!");
        }
    }

    public void onCancelClicked() {
        mWizardFragmentListener.cancelRequest();
    }

    public void onCreateKeyClicked() {
        mWizardFragmentListener.onAdvanceToNextWizardStep();
    }

    public void onKeyImportClicked() {
        Intent intent = new Intent(mActivity, ImportKeysActivity.class);
        intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN);
        mActivity.startActivityForResult(intent, REQUEST_CODE_IMPORT_KEY);
    }

    public void onCreateYubiKeyClicked() {
        if (mWizardFragmentListener != null) {
            mWizardFragmentListener.setUseYubiKey();
            mWizardFragmentListener.onAdvanceToNextWizardStep();
        }
    }

    public boolean onBackClicked() {
        return false;
    }
}
