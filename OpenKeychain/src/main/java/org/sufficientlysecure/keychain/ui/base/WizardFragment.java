package org.sufficientlysecure.keychain.ui.base;

import android.app.Activity;

import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;

/**
 * Base fragment class for any wizard fragment
 */
public abstract class WizardFragment extends QueueingCryptoOperationFragment<ImportKeyringParcel,
		ImportKeyResult> implements CreateKeyWizardActivity.CreateKeyWizardListener {
	protected WizardFragmentListener mWizardFragmentListener;

	@Override
	public boolean onNextClicked() {
		return false;
	}

	@Override
	public boolean onBackClicked() {
		return true;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mWizardFragmentListener = (WizardFragmentListener) activity;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (mWizardFragmentListener != null) {
			mWizardFragmentListener.onWizardFragmentVisible(this);
		}
	}
	@Override
	public void onQueuedOperationSuccess(ImportKeyResult result) {

	}

	@Override
	public ImportKeyringParcel createOperationInput() {
		return null;
	}

	/**
	 * Helper method to add a new email to the email wizard fragment.
	 */
	public void onRequestAddEmail(String email) {

	}
}
