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
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;


public class YubiKeyPinWizardFragment extends WizardFragment implements
        YubiKeyPinWizardFragmentViewModel.OnViewModelEventBind {

    private TextView mCreateYubiKeyPin;
    private TextView mCreateYubiKeyAdminPin;
    private YubiKeyPinWizardFragmentViewModel mYubiKeyPinWizardFragmentViewModel;

    /**
     * Creates new instance of this fragment
     */
    public static YubiKeyPinWizardFragment newInstance() {
        return new YubiKeyPinWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mYubiKeyPinWizardFragmentViewModel = new YubiKeyPinWizardFragmentViewModel(this,
                mWizardFragmentListener);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_yubi_pin_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCreateYubiKeyAdminPin = (TextView) view.findViewById(R.id.create_yubi_key_admin_pin);
        mCreateYubiKeyPin = (TextView) view.findViewById(R.id.create_yubi_key_pin);
        mYubiKeyPinWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mYubiKeyPinWizardFragmentViewModel.onDetachFromActivity();
    }

    @Override
    public void updatePinText(CharSequence text) {
        mCreateYubiKeyPin.setText(text);
    }

    @Override
    public void updateAdminPinText(CharSequence text) {
        mCreateYubiKeyAdminPin.setText(text);
    }
}
