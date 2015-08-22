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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.util.KeyboardUtils;
import org.sufficientlysecure.keychain.ui.widget.PassphraseEditText;
import org.sufficientlysecure.keychain.util.Passphrase;

/**
 * Wizard fragment that handles the Passphrase configuration.
 */
public class PassphraseUnlockWizardFragment extends WizardFragment {
    private PassphraseEditText mPassphraseEdit;
    private EditText mPassphraseEditAgain;
    private CheckBox mShowPassphrase;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_passphrase_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPassphraseEdit = (PassphraseEditText) view.findViewById(R.id.create_key_passphrase);
        mPassphraseEditAgain = (EditText) view.findViewById(R.id.create_key_passphrase_again);
        mShowPassphrase = (CheckBox) view.findViewById(R.id.create_key_show_passphrase);

        // initial values
        // TODO: using String here is unsafe...
        if (mWizardFragmentListener.getPassphrase() != null) {
            mPassphraseEdit.setText(mWizardFragmentListener.getPassphrase().toStringUnsafe());
            mPassphraseEditAgain.setText(mWizardFragmentListener.getPassphrase().toStringUnsafe());
        }

        mPassphraseEdit.requestFocus();
        mShowPassphrase.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mPassphraseEdit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    mPassphraseEditAgain.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    mPassphraseEdit.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    mPassphraseEditAgain.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isEditTextNotEmpty(getActivity(), mPassphraseEdit)) {
                    mPassphraseEditAgain.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    return;
                }

                if (areEditTextsEqual(mPassphraseEdit, mPassphraseEditAgain)) {
                    mPassphraseEditAgain.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_stat_retyped_ok, 0);
                } else {
                    mPassphraseEditAgain.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_stat_retyped_bad, 0);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        mPassphraseEdit.addTextChangedListener(textWatcher);
        mPassphraseEditAgain.addTextChangedListener(textWatcher);

    }

    @Override
    public boolean onNextClicked() {
        if (isEditTextNotEmpty(getActivity(), mPassphraseEdit)) {
            if (!areEditTextsEqual(mPassphraseEdit, mPassphraseEditAgain)) {
                mPassphraseEditAgain.setError(getActivity().getApplicationContext().getString(R.string.create_key_passphrases_not_equal));
                mPassphraseEditAgain.requestFocus();
                return false;
            }

            mPassphraseEditAgain.setError(null);
            // save state
            mWizardFragmentListener.setPassphrase(new Passphrase(mPassphraseEdit));
            KeyboardUtils.hideKeyboard(getActivity(), getActivity().getCurrentFocus());
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

    private static boolean areEditTextsEqual(EditText editText1, EditText editText2) {
        Passphrase p1 = new Passphrase(editText1);
        Passphrase p2 = new Passphrase(editText2);
        return (p1.equals(p2));
    }

}
