package org.sufficientlysecure.keychain.ui.keyunlock.base;

import android.app.Activity;
import android.support.v4.app.Fragment;
import org.sufficientlysecure.keychain.ui.keyunlock.activities.CreateKeyWizardListener;


/**
 * Base fragment class for any wizard fragment
 */
public class WizardFragment extends Fragment implements CreateKeyWizardListener {
    protected WizardFragmentListener mWizardFragmentListener;

    @Override
    public boolean onNextClicked() {
        return false;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mWizardFragmentListener = (WizardFragmentListener)activity;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(mWizardFragmentListener != null) {
            mWizardFragmentListener.onWizardFragmentVisible(this);
        }
    }
}
