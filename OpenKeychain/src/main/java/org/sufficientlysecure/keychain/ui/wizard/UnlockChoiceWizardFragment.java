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
import android.widget.RadioGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;

/**
 * Radio based unlock choice fragment
 */
public class UnlockChoiceWizardFragment extends WizardFragment
        implements UnlockChoiceWizardFragmentViewModel.OnViewModelEventBind {
    private UnlockChoiceWizardFragmentViewModel mUnlockChoiceWizardFragmentViewModel;

    public static UnlockChoiceWizardFragment newInstance() {
        return new UnlockChoiceWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUnlockChoiceWizardFragmentViewModel = new UnlockChoiceWizardFragmentViewModel(this,
                mWizardFragmentListener);

        mUnlockChoiceWizardFragmentViewModel.restoreViewModelState(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_unlock_choice_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RadioGroup wizardUnlockChoiceRadioGroup = (RadioGroup) view.
                findViewById(R.id.wizardUnlockChoiceRadioGroup);

        mUnlockChoiceWizardFragmentViewModel.updateUnlockMethodById(wizardUnlockChoiceRadioGroup.
                getCheckedRadioButtonId());

        wizardUnlockChoiceRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mUnlockChoiceWizardFragmentViewModel.updateUnlockMethodById(checkedId);
            }
        });

        mUnlockChoiceWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Override
    public boolean onNextClicked() {
        return mUnlockChoiceWizardFragmentViewModel.onNextClicked();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mUnlockChoiceWizardFragmentViewModel.saveViewModelState(outState);
    }

    @Override
    public void hideNavigationButtons(boolean hideBack, boolean hideNext) {
        mWizardFragmentListener.onHideNavigationButtons(hideBack, hideNext);
    }
}
