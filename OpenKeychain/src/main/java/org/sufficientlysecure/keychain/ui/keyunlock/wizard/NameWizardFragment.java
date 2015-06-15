package org.sufficientlysecure.keychain.ui.keyunlock.wizard;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.keyunlock.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.widget.NameEditText;

public class NameWizardFragment extends WizardFragment {
    private NameWizardFragmentViewModel mNameWizardFragmentViewModel;
    private org.sufficientlysecure.keychain.ui.widget.NameEditText mCreateKeyName;

    public static NameWizardFragment newInstance() {
        return new NameWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNameWizardFragmentViewModel = new NameWizardFragmentViewModel();
        mNameWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.wizard_name_fragment, container, false);
        mCreateKeyName = (NameEditText) view.findViewById(R.id.create_key_name);

        // focus empty edit fields
        if (mWizardFragmentListener.getName() == null) {
            mCreateKeyName.requestFocus();
        }

        return view;
    }

    /**
     * Checks if text of given EditText is not empty. If it is empty an error is
     * set and the EditText gets the focus.
     *
     * @param editText
     * @return true if EditText is not empty
     */
    private boolean isEditTextNotEmpty(EditText editText) {
        if(mNameWizardFragmentViewModel.isTextEmpty(editText.getText())) {
            editText.setError(getActivity().getString(R.string.create_key_empty));
            editText.requestFocus();
            return false;
        }
        else {
            editText.setError(null);
            return true;
        }
    }

    @Override
    public boolean onNextClicked() {
        if(isEditTextNotEmpty(mCreateKeyName))
        {
            mWizardFragmentListener.setUserName(mCreateKeyName.getText());
            return true;
        }
        return false;
    }
}
