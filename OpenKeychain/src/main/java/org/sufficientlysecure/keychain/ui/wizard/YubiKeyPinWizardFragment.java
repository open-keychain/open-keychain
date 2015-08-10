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
import org.sufficientlysecure.keychain.ui.tasks.YubiKeyPinAsyncTask;
import org.sufficientlysecure.keychain.util.Passphrase;


public class YubiKeyPinWizardFragment extends WizardFragment
        implements YubiKeyPinAsyncTask.OnYubiKeyPinAsyncTaskListener {
    private TextView mCreateYubiKeyPin;
    private TextView mCreateYubiKeyAdminPin;
    private Passphrase mPin;
    private Passphrase mAdminPin;
    private YubiKeyPinAsyncTask mYubiKeyPinAsyncTask;

    /**
     * Creates new instance of this fragment
     */
    public static YubiKeyPinWizardFragment newInstance() {
        return new YubiKeyPinWizardFragment();
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

        onYubiKeyPinDataSet(mWizardFragmentListener.getYubiKeyPin(), mWizardFragmentListener.getYubiKeyAdminPin());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mYubiKeyPinAsyncTask != null) {
            mYubiKeyPinAsyncTask.setOnYubiKeyPinAsyncTaskListener(null);
            mYubiKeyPinAsyncTask.cancel(true);
            mYubiKeyPinAsyncTask = null;
        }
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
            mCreateYubiKeyAdminPin.setText(mAdminPin.toStringUnsafe());
            mCreateYubiKeyPin.setText(mPin.toStringUnsafe());
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

    @Override
    public void onYubiKeyPinTaskResult(Passphrase pin, Passphrase adminPin) {
        mCreateYubiKeyAdminPin.setText(adminPin.toStringUnsafe());
        mCreateYubiKeyPin.setText(pin.toStringUnsafe());
    }
}
