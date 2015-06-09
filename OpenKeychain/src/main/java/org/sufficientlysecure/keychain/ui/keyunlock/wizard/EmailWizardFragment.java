package org.sufficientlysecure.keychain.ui.keyunlock.wizard;


import android.app.Activity;
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
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.dialog.AddEmailDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.keyunlock.activities.WizardCommonListener;
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
    private WizardCommonListener mWizardCommonListener;
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

            if (mWizardCommonListener.getModel().getAdditionalEmails() != null) {
                mEmailAdapter.addAll(mWizardCommonListener.getModel().getAdditionalEmails());
            }
        }

        mCreateKeyEmails.setAdapter(mEmailAdapter);

        return view;
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
        if (!additionalEmail && isEmailDuplicatedInsideAdapter(email) || additionalEmail &&
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
     * Checks for duplicated emails inside the additional email adapter.
     *
     * @param email
     * @return
     */
    private boolean isEmailDuplicatedInsideAdapter(String email) {
        //check for duplicated emails inside the adapter
        for (WizardEmailAdapter.ViewModel model : mEmailWizardFragmentViewModel.
                getAdditionalEmailModels()) {
            if (email.equals(model.getEmail())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Displays a dialog fragment for the user to input a valid email.
     * TODO: 09/06/2015 Warning, handler is unsafe!
     */
    private void addEmail() {
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == SetPassphraseDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();

                    String email = data.getString(AddEmailDialogFragment.MESSAGE_DATA_EMAIL);

                    if (checkEmail(email, true)) {
                        // add new user id
                        mEmailAdapter.add(email);
                    }
                }
            }
        };
        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        AddEmailDialogFragment addEmailDialog = AddEmailDialogFragment.newInstance(messenger);
        addEmailDialog.show(getActivity().getSupportFragmentManager(), "addEmailDialog");
    }


    /**
     * Returns all additional emails.
     * @return
     */
    private ArrayList<String> getAdditionalEmails() {
        ArrayList<String> emails = new ArrayList<>();
        for (WizardEmailAdapter.ViewModel holder : mEmailWizardFragmentViewModel.
                getAdditionalEmailModels()) {
            emails.add(holder.toString());
        }
        return emails;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mWizardCommonListener = (WizardCommonListener) activity;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }
}
