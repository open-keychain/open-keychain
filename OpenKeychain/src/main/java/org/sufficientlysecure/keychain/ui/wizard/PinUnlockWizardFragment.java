package org.sufficientlysecure.keychain.ui.wizard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;
import org.sufficientlysecure.keychain.util.Passphrase;

public class PinUnlockWizardFragment extends WizardFragment implements
		PinUnlockWizardFragmentViewModel.OnViewModelEventBind {
	private PinUnlockWizardFragmentViewModel mPinUnlockWizardFragmentViewModel;
	private FeedbackIndicatorView mFeedbackIndicatorView;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPinUnlockWizardFragmentViewModel = new PinUnlockWizardFragmentViewModel(this);
		mPinUnlockWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
				getActivity());
	}

	/**
	 * Handles pin key press.
	 */
	private View.OnClickListener mOnKeyClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			mPinUnlockWizardFragmentViewModel.appendToCurrentKeyword(((TextView) v).getText());
		}
	};

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.unlock_pin_fragment, container, false);

		Button pinUnlockKey = (Button) view.findViewById(R.id.unlockKey0);
		pinUnlockKey.setOnClickListener(mOnKeyClickListener);
		pinUnlockKey = (Button) view.findViewById(R.id.unlockKey9);
		pinUnlockKey.setOnClickListener(mOnKeyClickListener);
		pinUnlockKey = (Button) view.findViewById(R.id.unlockKey8);
		pinUnlockKey.setOnClickListener(mOnKeyClickListener);
		pinUnlockKey = (Button) view.findViewById(R.id.unlockKey7);
		pinUnlockKey.setOnClickListener(mOnKeyClickListener);
		pinUnlockKey = (Button) view.findViewById(R.id.unlockKey6);
		pinUnlockKey.setOnClickListener(mOnKeyClickListener);
		pinUnlockKey = (Button) view.findViewById(R.id.unlockKey5);
		pinUnlockKey.setOnClickListener(mOnKeyClickListener);
		pinUnlockKey = (Button) view.findViewById(R.id.unlockKey4);
		pinUnlockKey.setOnClickListener(mOnKeyClickListener);
		pinUnlockKey = (Button) view.findViewById(R.id.unlockKey3);
		pinUnlockKey.setOnClickListener(mOnKeyClickListener);
		pinUnlockKey = (Button) view.findViewById(R.id.unlockKey2);
		pinUnlockKey.setOnClickListener(mOnKeyClickListener);
		pinUnlockKey = (Button) view.findViewById(R.id.unlockKey1);
		pinUnlockKey.setOnClickListener(mOnKeyClickListener);

		mFeedbackIndicatorView = (FeedbackIndicatorView) view.findViewById(R.id.unlockUserFeedback);

		((TextView) view.findViewById(R.id.unlockTip)).setText(getString(R.string.wizard_unlock_tip));

		if (mWizardFragmentListener != null) {
			mWizardFragmentListener.onHideNavigationButtons(false, false);
		}

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mPinUnlockWizardFragmentViewModel.saveViewModelState(outState);
	}

	@Override
	public boolean onNextClicked() {
		if (!mPinUnlockWizardFragmentViewModel.isOperationCompleted()) {
			mPinUnlockWizardFragmentViewModel.updateOperationState();
			return false;
		} else {
			if (mWizardFragmentListener != null) {
				Passphrase passphrase = new Passphrase(mPinUnlockWizardFragmentViewModel.
						getLastInputKeyWord().toString());
				passphrase.setSecretKeyType(mWizardFragmentListener.getSecretKeyType());
				mWizardFragmentListener.setPassphrase(passphrase);
			}

			//reset the view model because the user can navigate back
			mPinUnlockWizardFragmentViewModel = new PinUnlockWizardFragmentViewModel(this);
			mPinUnlockWizardFragmentViewModel.prepareViewModel(null, getArguments(), getActivity());
			return true;
		}
	}

	/**
	 * Notifies the user of any errors that may have occurred
	 */
	@Override
	public void onOperationStateError(String error) {
		mFeedbackIndicatorView.showWrongTextMessage(error, true);
	}

	@Override
	public void onOperationStateOK(String showText) {
		mFeedbackIndicatorView.showCorrectTextMessage(showText, false);
	}

	/**
	 * Updates the view state by giving feedback to the user.
	 */
	@Override
	public void onOperationStateCompleted(String showText) {
		mFeedbackIndicatorView.showCorrectTextMessage(showText, true);
	}
}
