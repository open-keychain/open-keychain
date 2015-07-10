package org.sufficientlysecure.keychain.ui.wizard;

import android.content.Context;
import android.os.Bundle;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.util.Passphrase;

public class YubiKeyPinRepeatWizardFragmentViewModel implements BaseViewModel {
	private OnViewModelEventBind mOnViewModelEventBind;
	private Context mContext;
	private Passphrase mPin;
	private Passphrase mAdminPin;

	public interface OnViewModelEventBind {
		CharSequence getPin();

		CharSequence getAdminPin();

		void onPinError(CharSequence error);

		void onAdminPinError(CharSequence error);
	}

	public YubiKeyPinRepeatWizardFragmentViewModel(OnViewModelEventBind onViewModelEventBind) {
		mOnViewModelEventBind = onViewModelEventBind;
	}

	@Override
	public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {
		mContext = context;

		if (savedInstanceState != null) {
			restoreViewModelState(savedInstanceState);
		}
	}

	@Override
	public void saveViewModelState(Bundle outState) {

	}

	@Override
	public void restoreViewModelState(Bundle savedInstanceState) {

	}

	/**
	 * Sets Previous Yubi key pins.
	 *
	 * @param pin
	 * @param adminPin
	 */
	public void onYubiKeyPinDataSet(Passphrase pin, Passphrase adminPin) {
		mAdminPin = adminPin;
		mPin = pin;
	}

	public boolean onValidatePinData() {
		String pin = mOnViewModelEventBind.getPin().toString();
		String adminPin = mOnViewModelEventBind.getAdminPin().toString();

		/**
		 * This should not happen!
		 */
		if(mPin == null || mAdminPin == null) {
			return false;
		}

		if (pin.isEmpty()) {
			mOnViewModelEventBind.onPinError(mContext.getString(R.string.create_key_empty));
		}
		else if(!pin.equals(mPin.toStringUnsafe())) {
			mOnViewModelEventBind.onPinError(mContext.getString(R.string.create_key_yubi_key_pin_not_correct));
		}
		else if(mAdminPin.isEmpty()) {
			mOnViewModelEventBind.onAdminPinError(mContext.getString(R.string.create_key_empty));
		}
		else if(!adminPin.equals(mAdminPin.toStringUnsafe())) {
			mOnViewModelEventBind.onAdminPinError(mContext.getString(R.string.create_key_yubi_key_pin_not_correct));
		}
		else {
			mOnViewModelEventBind.onAdminPinError(null);
			mOnViewModelEventBind.onPinError(null);
			return true;
		}
		return false;
	}
}
