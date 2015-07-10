package org.sufficientlysecure.keychain.ui.wizard;

import android.content.Context;
import android.os.Bundle;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.adapter.WizardEmailAdapter;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;

import java.util.ArrayList;
import java.util.regex.Pattern;


/**
 * Email Wizard Fragment View Model
 */
public class EmailWizardFragmentViewModel implements BaseViewModel {
	public static final String STATE_SAVE_ADDITIONAL_EMAILS = "STATE_SAVE_ADDITIONAL_EMAILS";
	private ArrayList<WizardEmailAdapter.ViewModel> mAdditionalEmailModels;
	private Context mContext;
	private OnViewModelEventBind mOnViewModelEventBind;

	// NOTE: Do not use more complicated pattern like defined in android.util.Patterns.EMAIL_ADDRESS
	// EMAIL_ADDRESS fails for mails with umlauts for example
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\S]+@[\\S]+\\.[a-z]+$");

	/**
	 * View Model communication
	 */
	public interface OnViewModelEventBind {
		void notifyUser(String message);

		CharSequence getMainEmail();

		void showEmailError(CharSequence error, boolean focus);
	}

	public EmailWizardFragmentViewModel(OnViewModelEventBind viewModelEventBind) {
		mOnViewModelEventBind = viewModelEventBind;

		if (mOnViewModelEventBind == null) {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {
		mContext = context;

		if (savedInstanceState != null) {
			restoreViewModelState(savedInstanceState);
		} else {
			mAdditionalEmailModels = new ArrayList<>();
		}
	}

	@Override
	public void saveViewModelState(Bundle outState) {
		outState.putSerializable(STATE_SAVE_ADDITIONAL_EMAILS, mAdditionalEmailModels);
	}

	@Override
	public void restoreViewModelState(Bundle savedInstanceState) {
		mAdditionalEmailModels = (ArrayList<WizardEmailAdapter.ViewModel>)
				savedInstanceState.getSerializable(STATE_SAVE_ADDITIONAL_EMAILS);

	}

	/**
	 * Checks if a given email is valid
	 *
	 * @param email
	 * @param additionalEmail
	 * @return
	 */
	public boolean checkEmail(String email, boolean additionalEmail) {
		// check for email format or if the user did any input
		if (!isEmailFormatValid(email)) {
			mOnViewModelEventBind.notifyUser(mContext.getString(R.string.
					create_key_email_invalid_email));
			return false;
		}

		// check for duplicated emails
		if (!additionalEmail && isEmailDuplicatedInsideAdapter(email) ||
				additionalEmail && mOnViewModelEventBind.getMainEmail().length() > 0 &&
						email.equals(mOnViewModelEventBind.getMainEmail().toString()) ||
				additionalEmail && isEmailDuplicatedInsideAdapter(email)) {
			mOnViewModelEventBind.notifyUser(mContext.getString(R.string.
					create_key_email_already_exists_text));
			return false;
		}

		return true;

	}

	/**
	 * Checks the email format
	 *
	 * @param email
	 * @return
	 */
	public boolean isEmailFormatValid(String email) {
		// check for email format or if the user did any input
		return !(email.length() == 0 || !EMAIL_PATTERN.matcher(email).matches());
	}


	/**
	 * Checks for duplicated emails inside the additional email adapter.
	 *
	 * @param email
	 * @return
	 */
	public boolean isEmailDuplicatedInsideAdapter(String email) {
		//check for duplicated emails inside the adapter
		for (WizardEmailAdapter.ViewModel model : mAdditionalEmailModels) {
			if (email.equals(model.getEmail())) {
				return true;
			}
		}

		return false;
	}


	/**
	 * Checks if text of given EditText is not empty. If it is empty an error is
	 * set and the EditText gets the focus.
	 *
	 * @return true if EditText is not empty
	 */
	public boolean isMainEmailValid() {
		boolean output = true;
		if (!checkEmail(mOnViewModelEventBind.getMainEmail().toString(), false)) {
			mOnViewModelEventBind.showEmailError(mContext.
					getString(R.string.create_key_empty), false);
			output = false;
		} else {
			mOnViewModelEventBind.showEmailError(null, false);
		}

		return output;
	}

	/**
	 * Returns all additional emails.
	 *
	 * @return
	 */
	public ArrayList<String> getAdditionalEmails() {
		ArrayList<String> emails = new ArrayList<>();
		for (WizardEmailAdapter.ViewModel holder : mAdditionalEmailModels) {
			emails.add(holder.toString());
		}
		return emails;
	}

	public ArrayList<WizardEmailAdapter.ViewModel> getAdditionalEmailModels() {
		return mAdditionalEmailModels;
	}
}
