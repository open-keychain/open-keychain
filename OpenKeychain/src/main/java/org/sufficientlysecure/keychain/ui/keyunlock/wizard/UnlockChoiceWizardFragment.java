package org.sufficientlysecure.keychain.ui.keyunlock.wizard;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.keyunlock.base.WizardFragment;

/**
 * Radio based unlock choice fragment
 */
public class UnlockChoiceWizardFragment extends WizardFragment {
    public static final String TAG = "UnlockChoiceWizardFragment";
    private UnlockChoiceWizardFragmentViewModel mUnlockChoiceWizardFragmentViewModel;
    private TextView mWizardUnlockSuggestion;
    private RadioGroup mWizardUnlockChoiceRadioGroup;

    public static UnlockChoiceWizardFragment newInstance() {
        return new UnlockChoiceWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUnlockChoiceWizardFragmentViewModel = new UnlockChoiceWizardFragmentViewModel();
        mUnlockChoiceWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wizard_unlock_choice_fragment, container, false);
        mWizardUnlockSuggestion = (android.widget.TextView) view.findViewById(R.id.wizardUnlockSuggestion);
        mWizardUnlockChoiceRadioGroup = (RadioGroup) view.findViewById(R.id.wizardUnlockChoiceRadioGroup);

        mUnlockChoiceWizardFragmentViewModel.updateUnlockMethodById(mWizardUnlockChoiceRadioGroup.getCheckedRadioButtonId());

        mWizardUnlockChoiceRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mUnlockChoiceWizardFragmentViewModel.updateUnlockMethodById(checkedId);
            }
        });
        return view;
    }

    /**
     * @return
     */
    @Override
    public boolean onNextClicked() {
        if(mUnlockChoiceWizardFragmentViewModel.isUserDataReady()) {
            mWizardFragmentListener.setUnlockMethod(mUnlockChoiceWizardFragmentViewModel.getSecretKeyType());
            return true;
        }
        return false;
    }
}
