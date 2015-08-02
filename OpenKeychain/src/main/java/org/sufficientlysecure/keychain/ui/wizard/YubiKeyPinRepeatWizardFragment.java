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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;

public class YubiKeyPinRepeatWizardFragment extends WizardFragment
        implements YubiKeyPinRepeatWizardFragmentViewModel.OnViewModelEventBind {

    private YubiKeyPinRepeatWizardFragmentViewModel mYubiKeyPinRepeatWizardFragmentViewModel;
    private EditText mCreateYubiKeyPinRepeat;
    private EditText mCreateYubiKeyAdminPinRepeat;

    public static YubiKeyPinRepeatWizardFragment newInstance() {
        return new YubiKeyPinRepeatWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mYubiKeyPinRepeatWizardFragmentViewModel = new YubiKeyPinRepeatWizardFragmentViewModel(this,
                mWizardFragmentListener);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_yubi_pin_repeat_feagment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCreateYubiKeyAdminPinRepeat = (EditText) view.findViewById(R.id.create_yubi_key_admin_pin_repeat);
        mCreateYubiKeyPinRepeat = (EditText) view.findViewById(R.id.create_yubi_key_pin_repeat);

        mYubiKeyPinRepeatWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Override
    public boolean onNextClicked() {
        return mYubiKeyPinRepeatWizardFragmentViewModel.onNextClicked();
    }

    @Override
    public CharSequence getPin() {
        return mCreateYubiKeyPinRepeat.getText();
    }

    @Override
    public CharSequence getAdminPin() {
        return mCreateYubiKeyAdminPinRepeat.getText();
    }

    @Override
    public void onPinError(CharSequence error) {
        if (error != null) {
            mCreateYubiKeyPinRepeat.setError(error);
            mCreateYubiKeyPinRepeat.requestFocus();
        } else {
            mCreateYubiKeyPinRepeat.setError(error);
        }
    }

    @Override
    public void onAdminPinError(CharSequence error) {
        if (error != null) {
            mCreateYubiKeyAdminPinRepeat.setError(error);
            mCreateYubiKeyAdminPinRepeat.requestFocus();
        } else {
            mCreateYubiKeyAdminPinRepeat.setError(error);
        }
    }
}
