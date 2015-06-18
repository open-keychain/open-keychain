package org.sufficientlysecure.keychain.ui.keyunlock.wizard;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity;
import org.sufficientlysecure.keychain.ui.CreateKeyPassphraseFragment;
import org.sufficientlysecure.keychain.ui.dialog.AddEmailDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.keyunlock.adapter.WizardEmailAdapter;
import org.sufficientlysecure.keychain.ui.keyunlock.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.widget.EmailEditText;

import java.util.ArrayList;

/**
 * Wizard fragment that handle the user emails.
 * TODO: 09/06/2015 Refactor the code and move all checks to the viewModel to add tests later.
 */
public class EmailWizardFragment extends WizardFragment {
    private EmailWizardFragmentViewModel mEmailWizardFragmentViewModel;
    private TextView mEmailWizardFragmentTip;
    private EmailEditText mCreateKeyEmail;
    private RecyclerView mCreateKeyEmails;
    private WizardEmailAdapter mEmailAdapter;

    public static EmailWizardFragment newInstance() {
        return new EmailWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEmailWizardFragmentViewModel = new EmailWizardFragmentViewModel();
        mEmailWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wizard_email_fragment, container, false);
        mCreateKeyEmails = (RecyclerView) view.findViewById(R.id.create_key_emails);
        mCreateKeyEmail = (EmailEditText) view.findViewById(R.id.create_key_email);
        mEmailWizardFragmentTip = (TextView) view.findViewById(R.id.emailWizardFragmentTip);

        mCreateKeyEmails.setHasFixedSize(true);
        mCreateKeyEmails.setLayoutManager(new LinearLayoutManager(getActivity()));
        mCreateKeyEmails.setItemAnimator(new DefaultItemAnimator());

        if (mEmailAdapter == null) {
            mEmailAdapter = new WizardEmailAdapter(mEmailWizardFragmentViewModel.
                    getAdditionalEmailModels(), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addEmail();
                }
            });

            if (mWizardFragmentListener.getAdditionalEmails() != null) {
                mEmailAdapter.addAll(mWizardFragmentListener.getAdditionalEmails());
            }
        }

        mCreateKeyEmails.setAdapter(mEmailAdapter);

        if (mWizardFragmentListener != null) {
            mWizardFragmentListener.onHideNavigationButtons(false);
        }

        return view;
    }

    @Override
    public boolean onNextClicked() {
        if (isMainEmailValid(mCreateKeyEmail)) {
            mWizardFragmentListener.setEmail(mCreateKeyEmail.getText());
            mWizardFragmentListener.setAdditionalEmails(mEmailWizardFragmentViewModel.
                    getAdditionalEmails());
            return true;
        }
        return false;
    }

    /**
     * Checks if a given email is valid
     *
     * @param email
     * @param additionalEmail
     * @return
     */
    private boolean checkEmail(String email, boolean additionalEmail) {
        // check for email format or if the user did any input
        if (!mEmailWizardFragmentViewModel.isEmailFormatValid(email)) {
            Notify.create(getActivity(),
                    getString(R.string.create_key_email_invalid_email),
                    Notify.LENGTH_LONG, Notify.Style.ERROR).show(this);
            return false;
        }

        // check for duplicated emails
        if (!additionalEmail && mEmailWizardFragmentViewModel.isEmailDuplicatedInsideAdapter(email)
                || additionalEmail &&
                mCreateKeyEmail.getText().length() > 0 && email.equals(mCreateKeyEmail.getText().
                toString())) {
            Notify.create(getActivity(),
                    getString(R.string.create_key_email_already_exists_text),
                    Notify.LENGTH_LONG, Notify.Style.ERROR).show(this);
            return false;
        }

        return true;
    }

    /**
     * Checks if text of given EditText is not empty. If it is empty an error is
     * set and the EditText gets the focus.
     *
     * @param editText
     * @return true if EditText is not empty
     */
    private boolean isMainEmailValid(EditText editText) {
        boolean output = true;
        if (!checkEmail(editText.getText().toString(), false)) {
            editText.setError(getString(R.string.create_key_empty));
            editText.requestFocus();
            output = false;
        } else {
            editText.setError(null);
        }

        return output;
    }

    /**
     * Method that receives an email to be added to the recycler view.
     * @param email
     */
    @Override
    public void onRequestAddEmail(String email) {
        if (checkEmail(email, true)) {
            // add new user id
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
}
