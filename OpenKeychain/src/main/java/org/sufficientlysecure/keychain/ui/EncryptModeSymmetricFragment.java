/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Passphrase;

public class EncryptModeSymmetricFragment extends Fragment {

    public interface ISymmetric {

        public void onPassphraseChanged(Passphrase passphrase);
    }

    private ISymmetric mEncryptInterface;

    private EditText mPassphrase;
    private EditText mPassphraseAgain;

    /**
     * Creates new instance of this fragment
     */
    public static EncryptModeSymmetricFragment newInstance() {
        EncryptModeSymmetricFragment frag = new EncryptModeSymmetricFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mEncryptInterface = (ISymmetric) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ISymmetric");
        }
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.encrypt_symmetric_fragment, container, false);

        mPassphrase = (EditText) view.findViewById(R.id.passphrase);
        mPassphraseAgain = (EditText) view.findViewById(R.id.passphraseAgain);
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // update passphrase in EncryptActivity
                Passphrase p1 = new Passphrase(mPassphrase.getText());
                Passphrase p2 = new Passphrase(mPassphraseAgain.getText());
                boolean passesEquals = (p1.equals(p2));
                p1.removeFromMemory();
                p2.removeFromMemory();
                if (passesEquals) {
                    mEncryptInterface.onPassphraseChanged(new Passphrase(mPassphrase.getText()));
                } else {
                    mEncryptInterface.onPassphraseChanged(null);
                }
            }
        };
        mPassphrase.addTextChangedListener(textWatcher);
        mPassphraseAgain.addTextChangedListener(textWatcher);

        return view;
    }

}
