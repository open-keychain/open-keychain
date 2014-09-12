/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;

public class EncryptTextFragment extends Fragment {
    public static final String ARG_TEXT = "text";

    private TextView mMessage = null;
    private View mEncryptShare;
    private View mEncryptClipboard;

    private EncryptActivityInterface mEncryptInterface;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mEncryptInterface = (EncryptActivityInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement EncryptActivityInterface");
        }
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.encrypt_text_fragment, container, false);

        mMessage = (TextView) view.findViewById(R.id.message);
        mMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mEncryptInterface.setMessage(s.toString());
            }
        });
        mEncryptClipboard = view.findViewById(R.id.action_encrypt_clipboard);
        mEncryptShare = view.findViewById(R.id.action_encrypt_share);
        mEncryptClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEncryptInterface.startEncrypt(false);
            }
        });
        mEncryptShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEncryptInterface.startEncrypt(true);
            }
        });

        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String text = mEncryptInterface.getMessage();
        if (text != null) {
            mMessage.setText(text);
        }
    }
}
