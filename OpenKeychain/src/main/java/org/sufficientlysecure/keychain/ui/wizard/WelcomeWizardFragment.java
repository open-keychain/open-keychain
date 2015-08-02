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
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;

public class WelcomeWizardFragment extends WizardFragment
        implements WelcomeWizardFragmentViewModel.OnViewModelEventBind {
    private WelcomeWizardFragmentViewModel mWelcomeWizardFragmentViewModel;

    public static WelcomeWizardFragment newInstance() {
        return new WelcomeWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWelcomeWizardFragmentViewModel = new WelcomeWizardFragmentViewModel(this,
                mWizardFragmentListener);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_welcome_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView textView = (TextView) view.findViewById(R.id.create_key_cancel);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWelcomeWizardFragmentViewModel.onCancelClicked();
            }
        });

        textView = (TextView) view.findViewById(R.id.create_key_create_key_button);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWelcomeWizardFragmentViewModel.onCreateKeyClicked();
            }
        });

        textView = (TextView) view.findViewById(R.id.create_key_import_button);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWelcomeWizardFragmentViewModel.onKeyImportClicked();
            }
        });

        textView = (TextView) view.findViewById(R.id.create_key_yubikey_button);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWelcomeWizardFragmentViewModel.onCreateYubiKeyClicked();
            }
        });

        mWelcomeWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mWelcomeWizardFragmentViewModel.onActivityResult(requestCode, resultCode, data);
    }

    public boolean onBackClicked() {
        return mWelcomeWizardFragmentViewModel.onBackClicked();
    }

    @Override
    public void hideNavigationButtons(boolean hideBack, boolean hideNext) {
        mWizardFragmentListener.onHideNavigationButtons(hideBack, hideNext);
    }
}
