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
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;

/**
 * Wizard fragment that handles the user unlock choice.
 */
public class UnlockChoiceWizardFragment extends WizardFragment {
    public static final String STATE_SAVE_UNLOCK_METHOD = "STATE_SAVE_UNLOCK_METHOD";
    private CanonicalizedSecretKey.SecretKeyType mSecretKeyType;
    private Handler mHandler;
    private Runnable mRunnable;

    public static UnlockChoiceWizardFragment newInstance() {
        return new UnlockChoiceWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mSecretKeyType = (CanonicalizedSecretKey.SecretKeyType) savedInstanceState.
                    getSerializable(STATE_SAVE_UNLOCK_METHOD);
        }

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                mWizardFragmentListener.onHideNavigationButtons(false, false);
            }
        };
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

        updateUnlockMethodById(wizardUnlockChoiceRadioGroup.
                getCheckedRadioButtonId());

        wizardUnlockChoiceRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateUnlockMethodById(checkedId);
            }
        });
    }

    /**
     * Allows the user to advance to the next wizard step.
     *
     * @return
     */
    @Override
    public boolean onNextClicked() {
        mWizardFragmentListener.setUnlockMethod(mSecretKeyType);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_SAVE_UNLOCK_METHOD, mSecretKeyType);
    }

    /**
     * Updates the chosen unlock method.
     *
     * @param id
     */
    public void updateUnlockMethodById(int id) {
        if (id == R.id.radioPinUnlock) {
            mSecretKeyType = CanonicalizedSecretKey.SecretKeyType.PIN;
        } else if (id == R.id.radioPatternUnlock) {
            mSecretKeyType = CanonicalizedSecretKey.SecretKeyType.PATTERN;
        } else if (id == R.id.radioNFCUnlock) {
            mSecretKeyType = CanonicalizedSecretKey.SecretKeyType.NFC_TAG;
        } else if(id == R.id.radioPassphraseUnlock) {
            mSecretKeyType = CanonicalizedSecretKey.SecretKeyType.PASSPHRASE;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mHandler != null) {
            mHandler.postDelayed(mRunnable, 500);
        }
    }
}
