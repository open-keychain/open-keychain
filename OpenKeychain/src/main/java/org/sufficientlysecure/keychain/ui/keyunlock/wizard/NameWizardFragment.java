package org.sufficientlysecure.keychain.ui.keyunlock.wizard;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.keyunlock.activities.WizardCommonListener;
import org.sufficientlysecure.keychain.ui.keyunlock.base.WizardFragment;

public class NameWizardFragment extends WizardFragment {
    private WizardCommonListener mWizardCommonListener;

    public static NameWizardFragment newInstance() {
        return new NameWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_name_fragment, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mWizardCommonListener = (WizardCommonListener) activity;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }
}
