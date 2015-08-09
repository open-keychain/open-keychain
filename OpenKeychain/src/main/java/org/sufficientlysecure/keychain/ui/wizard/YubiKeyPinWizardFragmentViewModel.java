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

import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.base.WizardFragmentListener;
import org.sufficientlysecure.keychain.ui.tasks.YubiKeyPinAsyncTask;
import org.sufficientlysecure.keychain.util.Passphrase;


public class YubiKeyPinWizardFragmentViewModel implements BaseViewModel,
        YubiKeyPinAsyncTask.OnYubiKeyPinAsyncTaskListener {
    private YubiKeyPinAsyncTask mYubiKeyPinAsyncTask;
    private OnViewModelEventBind mOnViewModelEventBind;
    private WizardFragmentListener mWizardFragmentListener;
    private Activity mActivity;

    private Passphrase mPin;
    private Passphrase mAdminPin;

    public interface OnViewModelEventBind {
        void updatePinText(CharSequence text);

        void updateAdminPinText(CharSequence text);
    }

    public YubiKeyPinWizardFragmentViewModel(OnViewModelEventBind onViewModelEventBind,
                                             WizardFragmentListener wizardActivity) {
        mWizardFragmentListener = wizardActivity;
        mOnViewModelEventBind = onViewModelEventBind;

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

    @Override
    public void onYubiKeyPinTaskResult(Passphrase pin, Passphrase adminPin) {
        mOnViewModelEventBind.updateAdminPinText(adminPin.toStringUnsafe());
        mOnViewModelEventBind.updatePinText(pin.toStringUnsafe());
    }

    /**
     * Sets Yubi key pins.
     *
     * @param pin
     * @param adminPin
     */
    public void onYubiKeyPinDataSet(Passphrase pin, Passphrase adminPin) {
        mAdminPin = adminPin;
        mPin = pin;

        if (pin == null) {
            launchYubiKeyTask();
        } else {
            mOnViewModelEventBind.updateAdminPinText(mAdminPin.toStringUnsafe());
            mOnViewModelEventBind.updatePinText(mPin.toStringUnsafe());
        }
    }

    /**
     * Launches the Yubi Key Pin task to generate the pins.
     */
    public void launchYubiKeyTask() {
        if (mYubiKeyPinAsyncTask != null) {
            mYubiKeyPinAsyncTask.setOnYubiKeyPinAsyncTaskListener(null);
            mYubiKeyPinAsyncTask.cancel(true);
            mYubiKeyPinAsyncTask = null;
        }

        mYubiKeyPinAsyncTask = new YubiKeyPinAsyncTask(this);
        mYubiKeyPinAsyncTask.execute();
    }

    public void onDetachFromActivity() {
        if (mYubiKeyPinAsyncTask != null) {
            mYubiKeyPinAsyncTask.setOnYubiKeyPinAsyncTaskListener(null);
            mYubiKeyPinAsyncTask.cancel(true);
            mYubiKeyPinAsyncTask = null;
        }
    }
}
