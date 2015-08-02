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
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.base.WizardFragmentListener;

public class NameWizardFragmentViewModel implements BaseViewModel {
    private Activity mActivity;
    private OnViewModelEventBind mOnViewModelEventBind;
    private WizardFragmentListener mWizardFragmentListener;

    /**
     * View Model communication
     */
    public interface OnViewModelEventBind {

        CharSequence getName();

        void showNameError(CharSequence error, boolean focus);

        void hideNavigationButtons(boolean hideBack, boolean hideNext);

        void requestNameFocus();
    }

    public NameWizardFragmentViewModel(OnViewModelEventBind viewModelEventBind,
                                       WizardFragmentListener wizardActivity) {
        mOnViewModelEventBind = viewModelEventBind;
        mWizardFragmentListener = wizardActivity;

        if (mOnViewModelEventBind == null || mWizardFragmentListener == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Activity activity) {
        mActivity = activity;

        // focus empty edit fields
        if (mWizardFragmentListener.getName() == null) {
            mOnViewModelEventBind.requestNameFocus();
        }

        mOnViewModelEventBind.hideNavigationButtons(false, false);
    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {

    }

    /**
     * Checks if the text is empty.
     *
     * @param text
     * @return
     */
    public boolean isTextEmpty(CharSequence text) {
        return text.length() == 0;
    }

    /**
     * Checks if the edit text is not empty. If it is empty an error is
     * set and the EditText gets the focus.
     *
     * @return true if EditText is not empty
     */
    public boolean isEditTextNotEmpty() {
        if (isTextEmpty(mOnViewModelEventBind.getName())) {
            mOnViewModelEventBind.showNameError(mActivity.getString(R.string.
                    create_key_empty), true);
            return false;
        } else {
            mOnViewModelEventBind.showNameError(null, false);
            return true;
        }
    }

    public boolean onNextClicked() {
        if (isEditTextNotEmpty()) {
            mWizardFragmentListener.setUserName(mOnViewModelEventBind.getName());
            return true;
        }
        return false;
    }
}
