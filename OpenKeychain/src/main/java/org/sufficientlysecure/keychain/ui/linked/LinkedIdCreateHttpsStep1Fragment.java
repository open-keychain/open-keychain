/*
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.ui.linked;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.linked.resources.GenericHttpsResource;

public class LinkedIdCreateHttpsStep1Fragment extends Fragment {

    LinkedIdWizard mLinkedIdWizard;

    EditText mEditUri;

    public static LinkedIdCreateHttpsStep1Fragment newInstance() {
        LinkedIdCreateHttpsStep1Fragment frag = new LinkedIdCreateHttpsStep1Fragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLinkedIdWizard = (LinkedIdWizard) getActivity();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.linked_create_https_fragment_step1, container, false);

        view.findViewById(R.id.next_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                String uri = "https://" + mEditUri.getText();

                if (!checkUri(uri)) {
                    return;
                }

                String proofText = GenericHttpsResource.generateText(getActivity(),
                        mLinkedIdWizard.mFingerprint);

                LinkedIdCreateHttpsStep2Fragment frag =
                        LinkedIdCreateHttpsStep2Fragment.newInstance(uri, proofText);

                mLinkedIdWizard.loadFragment(null, frag, LinkedIdWizard.FRAG_ACTION_TO_RIGHT);

            }
        });

        view.findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLinkedIdWizard.loadFragment(null, null, LinkedIdWizard.FRAG_ACTION_TO_LEFT);
            }
        });

        mEditUri = (EditText) view.findViewById(R.id.linked_create_https_uri);

        mEditUri.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String uri = "https://" + editable;
                if (uri.length() > 0) {
                    if (checkUri(uri)) {
                        mEditUri.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.ic_stat_retyped_ok, 0);
                    } else {
                        mEditUri.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.ic_stat_retyped_bad, 0);
                    }
                } else {
                    // remove drawable if email is empty
                    mEditUri.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }
            }
        });

        // mEditUri.setText("mugenguild.com/pgpkey.txt");

        return view;
    }

    private static boolean checkUri(String uri) {
        return Patterns.WEB_URL.matcher(uri).matches();
    }

}
