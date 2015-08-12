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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.adapter.WizardEmailAdapter;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.dialog.AddEmailDialogFragment;
import org.sufficientlysecure.keychain.ui.util.KeyboardUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.widget.EmailEditText;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Wizard fragment that handle the user emails.
 */
public class EmailWizardFragment extends WizardFragment {
    public static final String STATE_SAVE_ADDITIONAL_EMAILS = "STATE_SAVE_ADDITIONAL_EMAILS";

    // NOTE: Do not use more complicated pattern like defined in android.util.Patterns.EMAIL_ADDRESS
    // EMAIL_ADDRESS fails for mails with umlauts for example
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\S]+@[\\S]+\\.[a-z]+$");

    private EmailEditText mCreateKeyEmail;
    private RecyclerView mCreateKeyEmails;
    private WizardEmailAdapter mEmailAdapter;
    private ArrayList<WizardEmailAdapter.ViewModel> mAdditionalEmailModels;

    public static EmailWizardFragment newInstance() {
        return new EmailWizardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_email_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCreateKeyEmails = (RecyclerView) view.findViewById(R.id.create_key_emails);
        mCreateKeyEmail = (EmailEditText) view.findViewById(R.id.create_key_email);
        mCreateKeyEmails.setHasFixedSize(true);
        mCreateKeyEmails.setLayoutManager(new LinearLayoutManager(getActivity()));
        mCreateKeyEmails.setItemAnimator(new DefaultItemAnimator());

        if (savedInstanceState == null) {
            mAdditionalEmailModels = new ArrayList<>();

            if (mWizardFragmentListener.getName() == null) {
                mCreateKeyEmail.requestFocus();
            }
        } else {
            mAdditionalEmailModels = (ArrayList<WizardEmailAdapter.ViewModel>)
                    savedInstanceState.getSerializable(STATE_SAVE_ADDITIONAL_EMAILS);
        }

        mWizardFragmentListener.onHideNavigationButtons(false, false);

        if (mEmailAdapter == null) {
            mEmailAdapter = new WizardEmailAdapter(mAdditionalEmailModels, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addEmail();
                }
            });
        }

        mCreateKeyEmails.setAdapter(mEmailAdapter);
    }

    /**
     * Allows the user to advance to the next wizard step.
     *
     * @return
     */
    @Override
    public boolean onNextClicked() {
        if (isMainEmailValid()) {
            mWizardFragmentListener.setEmail(mCreateKeyEmail.getText());
            mWizardFragmentListener.setAdditionalEmails(getAdditionalEmails());

            if (mWizardFragmentListener.createYubiKey()) {
                hideKeyboard();
            }
            return true;
        }
        return false;
    }

    /**
     * Method that receives an email to be added to the recycler view.
     *
     * @param email
     */
    @Override
    public void onRequestAddEmail(String email) {
        if (checkEmail(email, true)) {
            mEmailAdapter.add(email);
        }
    }

    /**
     * Displays a dialog fragment for the user to input a valid email.
     */
    private void addEmail() {
        AddEmailDialogFragment addEmailDialog = AddEmailDialogFragment.newInstance();
        addEmailDialog.show(getActivity().getSupportFragmentManager(), "addEmailDialog");
    }

    /**
     * Shows the message as a notification.
     *
     * @param message
     */
    public void notifyUser(String message) {
        Notify.create(getActivity(), message, Notify.LENGTH_LONG, Notify.Style.ERROR).show(this);
    }

    /**
     * Displays an error if the email is invalid.
     *
     * @param error
     * @param focus
     */
    public void showEmailError(CharSequence error, boolean focus) {
        if (focus) {
            mCreateKeyEmail.setError(error);
            mCreateKeyEmail.requestFocus();
        } else {
            mCreateKeyEmail.setError(null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_SAVE_ADDITIONAL_EMAILS, mAdditionalEmailModels);
    }

    /**
     * Checks if a given email is valid.
     *
     * @param email
     * @param additionalEmail
     * @return
     */
    public boolean checkEmail(String email, boolean additionalEmail) {
        // check for email format or if the user did any input
        if (!isEmailFormatValid(email)) {
            notifyUser(getString(R.string.create_key_email_invalid_email));
            return false;
        }

        // check for duplicated emails
        if (!additionalEmail && isEmailDuplicatedInsideAdapter(email) ||
                additionalEmail && mCreateKeyEmail.getText().length() > 0 &&
                        email.equals(mCreateKeyEmail.getText().toString()) ||
                additionalEmail && isEmailDuplicatedInsideAdapter(email)) {
            notifyUser(getString(R.string.create_key_email_already_exists_text));
            return false;
        }

        return true;
    }

    /**
     * Checks the email format.
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
        if (!checkEmail(mCreateKeyEmail.getText().toString(), false)) {
            showEmailError(getString(R.string.create_key_empty), false);
            output = false;
        } else {
            showEmailError(null, false);
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

    /**
     * Hides the keyboard.
     */
    public void hideKeyboard() {
        KeyboardUtils.hideKeyboard(getActivity(), getActivity().getCurrentFocus());
    }
}
