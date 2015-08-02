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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.widget.NameEditText;

public class NameWizardFragment extends WizardFragment
        implements NameWizardFragmentViewModel.OnViewModelEventBind {
    private NameWizardFragmentViewModel mNameWizardFragmentViewModel;
    private org.sufficientlysecure.keychain.ui.widget.NameEditText mCreateKeyName;

    public static NameWizardFragment newInstance() {
        return new NameWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNameWizardFragmentViewModel = new NameWizardFragmentViewModel(this, mWizardFragmentListener);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_name_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCreateKeyName = (NameEditText) view.findViewById(R.id.create_key_name);
        mNameWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Override
    public boolean onNextClicked() {
        return mNameWizardFragmentViewModel.onNextClicked();
    }

    @Override
    public CharSequence getName() {
        return mCreateKeyName.getText();
    }

    @Override
    public void showNameError(CharSequence error, boolean focus) {
        if (focus) {
            mCreateKeyName.setError(error);
            mCreateKeyName.requestFocus();
        } else {
            mCreateKeyName.setError(error);
        }
    }

    @Override
    public void hideNavigationButtons(boolean hideBack, boolean hideNext) {
        mWizardFragmentListener.onHideNavigationButtons(hideBack, hideNext);
    }

    @Override
    public void requestNameFocus() {
        mCreateKeyName.requestFocus();
    }
}
