/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Passphrase;

public class EncryptModeSymmetricFragment extends EncryptModeFragment {

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.encrypt_symmetric_fragment, container, false);

        mPassphrase = (EditText) view.findViewById(R.id.passphrase);
        mPassphraseAgain = (EditText) view.findViewById(R.id.passphraseAgain);

        return view;
    }

    @Override
    public boolean isAsymmetric() {
        return false;
    }

    @Override
    public long getAsymmetricSigningKeyId() {
        throw new UnsupportedOperationException("should never happen, this is a programming error!");
    }

    @Override
    public long[] getAsymmetricEncryptionKeyIds() {
        throw new UnsupportedOperationException("should never happen, this is a programming error!");
    }

    @Override
    public String[] getAsymmetricEncryptionUserIds() {
        throw new UnsupportedOperationException("should never happen, this is a programming error!");
    }

    @Override
    public Passphrase getSymmetricPassphrase() {
        Passphrase p1 = null, p2 = null;
        try {
            p1 = new Passphrase(mPassphrase.getText());
            p2 = new Passphrase(mPassphraseAgain.getText());
            if (!p1.equals(p2)) {
                return null;
            }
            return new Passphrase(mPassphrase.getText());
        } finally {
            if (p1 != null) {
                p1.removeFromMemory();
            }
            if (p2 != null) {
                p2.removeFromMemory();
            }
        }
    }

}
