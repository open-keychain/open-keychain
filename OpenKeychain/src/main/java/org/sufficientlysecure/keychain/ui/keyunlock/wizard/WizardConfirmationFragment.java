package org.sufficientlysecure.keychain.ui.keyunlock.wizard;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.keyunlock.base.WizardFragment;

public class WizardConfirmationFragment extends WizardFragment {
    public static WizardConfirmationFragment newInstance() {
        return new WizardConfirmationFragment();
    }

    public WizardConfirmationFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.wizard_confirmation_fragment_, container, false);
    }
}
