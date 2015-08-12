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
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.util.KeyboardUtils;
import org.sufficientlysecure.keychain.util.Passphrase;

public class YubiKeyPinRepeatWizardFragment extends WizardFragment {

    private EditText mPin;
    private EditText mAdminPin;

    public static YubiKeyPinRepeatWizardFragment newInstance() {
        return new YubiKeyPinRepeatWizardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_yubi_pin_repeat_feagment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAdminPin = (EditText) view.findViewById(R.id.create_yubi_key_admin_pin_repeat);
        mPin = (EditText) view.findViewById(R.id.create_yubi_key_pin_repeat);
    }

    /**
     * Allows the user to advance to the next wizard step.
     *
     * @return
     */
    @Override
    public boolean onNextClicked() {
        final Activity activity = getActivity();
        if (onValidatePinData()) {
            KeyboardUtils.hideKeyboard(activity, activity.getCurrentFocus());
            return true;
        }
        return false;
    }

    /**
     * Checks if text of given EditText is not empty. If it is empty an error is
     * set and the EditText gets the focus.
     *
     * @param context
     * @param editText
     * @return true if EditText is not empty
     */
    private static boolean isEditTextNotEmpty(Context context, EditText editText) {
        boolean output = true;
        if (editText.getText().length() == 0) {
            editText.setError(context.getString(R.string.create_key_empty));
            editText.requestFocus();
            output = false;
        } else {
            editText.setError(null);
        }

        return output;
    }

    private static boolean checkPin(Context context, EditText editText1, String pin) {
        boolean output = editText1.getText().toString().equals(pin);

        if (!output) {
            editText1.setError(context.getString(R.string.create_key_yubi_key_pin_not_correct));
            editText1.requestFocus();
        } else {
            editText1.setError(null);
        }

        return output;
    }

    public boolean onValidatePinData() {
        if (isEditTextNotEmpty(getActivity(), mPin)
                && checkPin(getActivity(), mPin, mWizardFragmentListener.getYubiKeyPin().toStringUnsafe())
                && isEditTextNotEmpty(getActivity(), mAdminPin)
                && checkPin(getActivity(), mAdminPin, mWizardFragmentListener.getYubiKeyAdminPin().toStringUnsafe())) {
            return true;
        }
        return false;
    }

}
