package org.sufficientlysecure.keychain.ui.wizard;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;

/**
 * Radio based unlock choice fragment
 */
public class UnlockChoiceWizardFragment extends WizardFragment {
	private UnlockChoiceWizardFragmentViewModel mUnlockChoiceWizardFragmentViewModel;

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

		RadioGroup wizardUnlockChoiceRadioGroup = (RadioGroup) view.
				findViewById(R.id.wizardUnlockChoiceRadioGroup);

		mUnlockChoiceWizardFragmentViewModel.updateUnlockMethodById(wizardUnlockChoiceRadioGroup.
				getCheckedRadioButtonId());

		wizardUnlockChoiceRadioGroup.setOnCheckedChangeListener(
				new RadioGroup.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						mUnlockChoiceWizardFragmentViewModel.updateUnlockMethodById(checkedId);
					}
				});

		if (mWizardFragmentListener != null) {
			mWizardFragmentListener.onHideNavigationButtons(false, false);
		}

		return view;
	}

	/**
	 * @return
	 */
	@Override
	public boolean onNextClicked() {
		if (mUnlockChoiceWizardFragmentViewModel.isUserDataReady()) {
			mWizardFragmentListener.setUnlockMethod(mUnlockChoiceWizardFragmentViewModel.
					getSecretKeyType());
			return true;
		}
		return false;
	}
}
