package org.sufficientlysecure.keychain.ui.wizard;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;

public class YubiKeyBlankWizardFragment extends WizardFragment {

    /**
     * Creates new instance of this fragment
     */
    public static YubiKeyBlankWizardFragment newInstance() {
        return new YubiKeyBlankWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_yubi_blank_fragment, container, false);
    }

    @Override
    public boolean onBackClicked() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().finish();
            return false;
        }
        return true;
    }

    @Override
    public boolean onNextClicked() {
        mWizardFragmentListener.setUseYubiKey();
        return true;
    }
}
