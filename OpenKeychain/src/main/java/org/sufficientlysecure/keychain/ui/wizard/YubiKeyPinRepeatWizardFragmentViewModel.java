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
import org.sufficientlysecure.keychain.ui.util.KeyboardUtils;
import org.sufficientlysecure.keychain.util.Passphrase;

public class YubiKeyPinRepeatWizardFragmentViewModel implements BaseViewModel {
    private OnViewModelEventBind mOnViewModelEventBind;
    private WizardFragmentListener mWizardFragmentListener;
    private Activity mActivity;
    private Passphrase mPin;
    private Passphrase mAdminPin;

    public interface OnViewModelEventBind {
        CharSequence getPin();

        CharSequence getAdminPin();

        void onPinError(CharSequence error);

        void onAdminPinError(CharSequence error);
    }

    public YubiKeyPinRepeatWizardFragmentViewModel(OnViewModelEventBind onViewModelEventBind,
                                                   WizardFragmentListener wizardActivity) {
        mOnViewModelEventBind = onViewModelEventBind;
        mWizardFragmentListener = wizardActivity;

        if (mOnViewModelEventBind == null || mWizardFragmentListener == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Activity activity) {
        mActivity = activity;
        onYubiKeyPinDataSet(mWizardFragmentListener.getYubiKeyPin(),
                mWizardFragmentListener.getYubiKeyAdminPin());
    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {

    }

    /**
     * Sets Previous Yubi key pins.
     *
     * @param pin
     * @param adminPin
     */
    public void onYubiKeyPinDataSet(Passphrase pin, Passphrase adminPin) {
        mAdminPin = adminPin;
        mPin = pin;
    }

    public boolean onValidatePinData() {
        String pin = mOnViewModelEventBind.getPin().toString();
        String adminPin = mOnViewModelEventBind.getAdminPin().toString();

        /**
         * This should not happen!
         */
        if (mPin == null || mAdminPin == null) {
            return false;
        }

        if (pin.isEmpty()) {
            mOnViewModelEventBind.onPinError(mActivity.getString(R.string.create_key_empty));
        } else if (!pin.equals(mPin.toStringUnsafe())) {
            mOnViewModelEventBind.onPinError(mActivity.getString(R.string.create_key_yubi_key_pin_not_correct));
        } else if (mAdminPin.isEmpty()) {
            mOnViewModelEventBind.onAdminPinError(mActivity.getString(R.string.create_key_empty));
        } else if (!adminPin.equals(mAdminPin.toStringUnsafe())) {
            mOnViewModelEventBind.onAdminPinError(mActivity.getString(R.string.create_key_yubi_key_pin_not_correct));
        } else {
            mOnViewModelEventBind.onAdminPinError(null);
            mOnViewModelEventBind.onPinError(null);
            return true;
        }
        return false;
    }

    public boolean onNextClicked() {
        if (onValidatePinData()) {
            KeyboardUtils.hideKeyboard(mActivity, mActivity.getCurrentFocus());
            return true;
        }
        return false;
    }
}
