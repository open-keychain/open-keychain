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
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.widget.EmailEditText;

/**
 * Wizard fragment that handle the user emails.
 */
public class EmailWizardFragment extends WizardFragment implements
        EmailWizardFragmentViewModel.OnViewModelEventBind {

    private EmailWizardFragmentViewModel mEmailWizardFragmentViewModel;
    private EmailEditText mCreateKeyEmail;
    private RecyclerView mCreateKeyEmails;
    private WizardEmailAdapter mEmailAdapter;

    public static EmailWizardFragment newInstance() {
        return new EmailWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEmailWizardFragmentViewModel = new EmailWizardFragmentViewModel(this, mWizardFragmentListener);
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
        mEmailWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(), getActivity());

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
        }

        mCreateKeyEmails.setAdapter(mEmailAdapter);
    }

    @Override
    public boolean onNextClicked() {
        return mEmailWizardFragmentViewModel.onNextClicked();
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

    @Override
    public void requestEmailFocus() {
        mCreateKeyEmail.requestFocus();
    }

    @Override
    public void hideNavigationButtons(boolean hideBack, boolean hideNext) {
        mWizardFragmentListener.onHideNavigationButtons(hideBack, hideNext);
    }
}
