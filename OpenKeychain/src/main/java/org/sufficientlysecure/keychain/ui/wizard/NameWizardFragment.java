package org.sufficientlysecure.keychain.ui.wizard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.widget.NameEditText;

public class NameWizardFragment extends WizardFragment
        implements NameWizardFragmentViewModel.OnViewModelEventBind {
    private NameWizardFragmentViewModel mNameWizardFragmentViewModel;
    private org.sufficientlysecure.keychain.ui.widget.NameEditText mCreateKeyName;

    public static NameWizardFragment newInstance() {
        return new NameWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNameWizardFragmentViewModel = new NameWizardFragmentViewModel(this);
        mNameWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wizard_name_fragment, container, false);
        mCreateKeyName = (NameEditText) view.findViewById(R.id.create_key_name);

        if (mWizardFragmentListener != null) {
            mWizardFragmentListener.onHideNavigationButtons(false, false);

            // focus empty edit fields
            if (mWizardFragmentListener.getName() == null) {
                mCreateKeyName.requestFocus();
            }
        }

        return view;
    }

    @Override
    public boolean onNextClicked() {
        if (mNameWizardFragmentViewModel.isEditTextNotEmpty()) {
            mWizardFragmentListener.setUserName(mCreateKeyName.getText());
            return true;
        }
        return false;
    }

    @Override
    public CharSequence getName() {
        return mCreateKeyName.getText();
    }

    @Override
    public void showNameError(CharSequence error, boolean focus) {
        if (focus) {
            mCreateKeyName.setError(error);
            mCreateKeyName.requestFocus();
        } else {
            mCreateKeyName.setError(error);
        }
    }
}
