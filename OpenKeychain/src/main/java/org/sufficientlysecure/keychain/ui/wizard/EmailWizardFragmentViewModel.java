/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sufficientlysecure.keychain.ui.wizard;

import android.app.Activity;
import android.os.Bundle;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.adapter.WizardEmailAdapter;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.base.WizardFragmentListener;
import org.sufficientlysecure.keychain.ui.util.KeyboardUtils;

import java.util.ArrayList;
import java.util.regex.Pattern;


/**
 * Email Wizard Fragment View Model
 */
public class EmailWizardFragmentViewModel implements BaseViewModel {
    public static final String STATE_SAVE_ADDITIONAL_EMAILS = "STATE_SAVE_ADDITIONAL_EMAILS";
    private ArrayList<WizardEmailAdapter.ViewModel> mAdditionalEmailModels;
    private Activity mActivity;
    private OnViewModelEventBind mOnViewModelEventBind;
    private WizardFragmentListener mWizardFragmentListener;


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

        void requestEmailFocus();

        void hideNavigationButtons(boolean hideBack, boolean hideNext);
    }

    public EmailWizardFragmentViewModel(OnViewModelEventBind viewModelEventBind,
                                        WizardFragmentListener wizardActivity) {
        mWizardFragmentListener = wizardActivity;
        mOnViewModelEventBind = viewModelEventBind;

        if (mOnViewModelEventBind == null || mWizardFragmentListener == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Activity activity) {
        mActivity = activity;

        if (savedInstanceState == null) {
            mAdditionalEmailModels = new ArrayList<>();

            if (mWizardFragmentListener.getName() == null) {
                mOnViewModelEventBind.requestEmailFocus();
            }
        }

        mOnViewModelEventBind.hideNavigationButtons(false, false);
    }

    @Override
    public void saveViewModelState(Bundle outState) {
        outState.putSerializable(STATE_SAVE_ADDITIONAL_EMAILS, mAdditionalEmailModels);
    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mAdditionalEmailModels = (ArrayList<WizardEmailAdapter.ViewModel>)
                    savedInstanceState.getSerializable(STATE_SAVE_ADDITIONAL_EMAILS);
        }
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
            mOnViewModelEventBind.notifyUser(mActivity.getString(R.string.
                    create_key_email_invalid_email));
            return false;
        }

        // check for duplicated emails
        if (!additionalEmail && isEmailDuplicatedInsideAdapter(email) ||
                additionalEmail && mOnViewModelEventBind.getMainEmail().length() > 0 &&
                        email.equals(mOnViewModelEventBind.getMainEmail().toString()) ||
                additionalEmail && isEmailDuplicatedInsideAdapter(email)) {
            mOnViewModelEventBind.notifyUser(mActivity.getString(R.string.
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
            mOnViewModelEventBind.showEmailError(mActivity.
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

    public void hideKeyboard() {
        KeyboardUtils.hideKeyboard(mActivity, mActivity.getCurrentFocus());
    }

    public boolean onNextClicked() {
        if (isMainEmailValid()) {
            mWizardFragmentListener.setEmail(mOnViewModelEventBind.getMainEmail());
            mWizardFragmentListener.setAdditionalEmails(getAdditionalEmails());

            if (mWizardFragmentListener.createYubiKey()) {
                hideKeyboard();
            }
            return true;
        }
        return false;
    }
}
