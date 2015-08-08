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


import android.app.Activity;
import android.os.Bundle;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.base.WizardFragmentListener;

public class UnlockChoiceWizardFragmentViewModel implements BaseViewModel {
    public static final String STATE_SAVE_UNLOCK_METHOD = "STATE_SAVE_UNLOCK_METHOD";
    private CanonicalizedSecretKey.SecretKeyType mSecretKeyType;
    private OnViewModelEventBind mOnViewModelEventBind;
    private WizardFragmentListener mWizardFragmentListener;

    /**
     * View Model communication
     */
    public interface OnViewModelEventBind {
        void hideNavigationButtons(boolean hideBack, boolean hideNext);
    }

    public UnlockChoiceWizardFragmentViewModel(OnViewModelEventBind onViewModelEventBind,
                                               WizardFragmentListener wizardActivity) {
        mOnViewModelEventBind = onViewModelEventBind;
        mWizardFragmentListener = wizardActivity;

        if (mOnViewModelEventBind == null || mWizardFragmentListener == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Activity activity) {
        mOnViewModelEventBind.hideNavigationButtons(false, false);
    }

    @Override
    public void saveViewModelState(Bundle outState) {
        outState.putSerializable(STATE_SAVE_UNLOCK_METHOD, mSecretKeyType);
    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mSecretKeyType = (CanonicalizedSecretKey.SecretKeyType) savedInstanceState.
                    getSerializable(STATE_SAVE_UNLOCK_METHOD);
        }
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
        }
    }

    /**
     * Performs operations when the user clicks on the wizard next button.
     * The view model itself will callback to the wizard activity for non ui methods.
     *
     * @return
     */
    public boolean onNextClicked() {
        mWizardFragmentListener.setUnlockMethod(mSecretKeyType);
        return true;
    }
}
