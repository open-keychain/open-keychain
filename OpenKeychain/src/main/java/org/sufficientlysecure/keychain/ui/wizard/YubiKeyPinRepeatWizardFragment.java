package org.sufficientlysecure.keychain.ui.wizard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.util.KeyboardUtils;


public class YubiKeyPinRepeatWizardFragment extends WizardFragment
		implements YubiKeyPinRepeatWizardFragmentViewModel.OnViewModelEventBind {

	private YubiKeyPinRepeatWizardFragmentViewModel mYubiKeyPinRepeatWizardFragmentViewModel;
	private EditText mCreateYubiKeyPinRepeat;
	private EditText mCreateYubiKeyAdminPinRepeat;

	public static YubiKeyPinRepeatWizardFragment newInstance() {
		return new YubiKeyPinRepeatWizardFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mYubiKeyPinRepeatWizardFragmentViewModel = new YubiKeyPinRepeatWizardFragmentViewModel(this);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.wizard_yubi_pin_repeat_feagment, container, false);
		mCreateYubiKeyAdminPinRepeat = (EditText) view.findViewById(R.id.create_yubi_key_admin_pin_repeat);
		mCreateYubiKeyPinRepeat = (EditText) view.findViewById(R.id.create_yubi_key_pin_repeat);

		return view;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mYubiKeyPinRepeatWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
				getActivity());
		mYubiKeyPinRepeatWizardFragmentViewModel.onYubiKeyPinDataSet(
				mWizardFragmentListener.getYubiKeyPin(), mWizardFragmentListener.getYubiKeyAdminPin());
	}

	@Override
	public boolean onNextClicked() {
		if (mYubiKeyPinRepeatWizardFragmentViewModel.onValidatePinData()) {
			KeyboardUtils.hideKeyboard(getActivity(), getActivity().getCurrentFocus());
			return true;
		}
		return false;
	}

	@Override
	public CharSequence getPin() {
		return mCreateYubiKeyPinRepeat.getText();
	}

	@Override
	public CharSequence getAdminPin() {
		return mCreateYubiKeyAdminPinRepeat.getText();
	}

	@Override
	public void onPinError(CharSequence error) {
		if (error != null) {
			mCreateYubiKeyPinRepeat.setError(error);
			mCreateYubiKeyPinRepeat.requestFocus();
		} else {
			mCreateYubiKeyPinRepeat.setError(error);
		}
	}

	@Override
	public void onAdminPinError(CharSequence error) {
		if (error != null) {
			mCreateYubiKeyAdminPinRepeat.setError(error);
			mCreateYubiKeyAdminPinRepeat.requestFocus();
		} else {
			mCreateYubiKeyAdminPinRepeat.setError(error);
		}
	}
}
