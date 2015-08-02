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


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;

/**
 * Confirmation fragment before creating the key
 */
public class WizardConfirmationFragment extends WizardFragment implements
        WizardConfirmationFragmentViewModel.OnViewModelEventBind {

    private CheckBox mCreateKeyUpload;
    private TextView mCreateKeyEditText;
    private TextView mEmailsText;
    private TextView mNameText;
    private WizardConfirmationFragmentViewModel mWizardConfirmationFragmentViewModel;

    public static WizardConfirmationFragment newInstance() {
        return new WizardConfirmationFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWizardConfirmationFragmentViewModel = new WizardConfirmationFragmentViewModel(this,
                mWizardFragmentListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_confirmation_fragment_, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView textView = (TextView) view.findViewById(R.id.create_key_edit_button);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWizardConfirmationFragmentViewModel.onCreateKeyClicked();
            }
        });

        mCreateKeyEditText = (TextView) view.findViewById(R.id.create_key_edit_text);
        mCreateKeyUpload = (CheckBox) view.findViewById(R.id.create_key_upload);
        mEmailsText = (TextView) view.findViewById(R.id.email);
        mNameText = (TextView) view.findViewById(R.id.name);
        mWizardConfirmationFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mWizardConfirmationFragmentViewModel.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mWizardConfirmationFragmentViewModel.onActivityCreated(savedInstanceState);
    }

    @Override
    public boolean onNextClicked() {
        return mWizardConfirmationFragmentViewModel.onNextClicked();
    }

    @Override
    public void setEmails(CharSequence emails) {
        mEmailsText.setText(emails);
    }

    @Override
    public void setName(CharSequence name) {
        mNameText.setText(name);
    }

    @Override
    public void setCreateKeyEditText(CharSequence text) {
        mCreateKeyEditText.setText(text);
    }

    @Override
    public boolean isUploadOptionChecked() {
        return mCreateKeyUpload.isChecked();
    }

    @Override
    public void setUploadOption(boolean checked) {
        mCreateKeyUpload.setChecked(checked);
    }
}
