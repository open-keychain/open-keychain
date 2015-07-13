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

/**
 * Wizard fragment that handle the user emails.
 */
public class EmailWizardFragment extends WizardFragment implements
        EmailWizardFragmentViewModel.OnViewModelEventBind {
    private EmailWizardFragmentViewModel mEmailWizardFragmentViewModel;
    private EmailEditText mCreateKeyEmail;
    private WizardEmailAdapter mEmailAdapter;

    public static EmailWizardFragment newInstance() {
        return new EmailWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEmailWizardFragmentViewModel = new EmailWizardFragmentViewModel(this);
        mEmailWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wizard_email_fragment, container, false);
        RecyclerView createKeyEmails = (RecyclerView) view.findViewById(R.id.create_key_emails);
        mCreateKeyEmail = (EmailEditText) view.findViewById(R.id.create_key_email);

        if (mWizardFragmentListener.getName() == null) {
            mCreateKeyEmail.requestFocus();
        }

        createKeyEmails.setHasFixedSize(true);
        createKeyEmails.setLayoutManager(new LinearLayoutManager(getActivity()));
        createKeyEmails.setItemAnimator(new DefaultItemAnimator());

        if (mEmailAdapter == null) {
            mEmailAdapter = new WizardEmailAdapter(mEmailWizardFragmentViewModel.
                    getAdditionalEmailModels(), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addEmail();
                }
            });
        }

        createKeyEmails.setAdapter(mEmailAdapter);

        if (mWizardFragmentListener != null) {
            mWizardFragmentListener.onHideNavigationButtons(false, false);
        }

        return view;
    }

    @Override
    public boolean onNextClicked() {
        if (mEmailWizardFragmentViewModel.isMainEmailValid()) {
            mWizardFragmentListener.setEmail(mCreateKeyEmail.getText());
            mWizardFragmentListener.setAdditionalEmails(mEmailWizardFragmentViewModel.
                    getAdditionalEmails());

            if (mWizardFragmentListener.createYubiKey()) {
                KeyboardUtils.hideKeyboard(getActivity(), getActivity().getCurrentFocus());
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
        if (mEmailWizardFragmentViewModel.checkEmail(email, true)) {
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

    @Override
    public void notifyUser(String message) {
        Notify.create(getActivity(), message, Notify.LENGTH_LONG, Notify.Style.ERROR).show(this);
    }

    @Override
    public CharSequence getMainEmail() {
        return mCreateKeyEmail.getText();
    }

    @Override
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
        mEmailWizardFragmentViewModel.saveViewModelState(outState);
    }
}
