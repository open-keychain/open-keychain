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

package org.sufficientlysecure.keychain.ui.affirmations;

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
import org.sufficientlysecure.keychain.pgp.affirmation.LinkedIdentity;
import org.sufficientlysecure.keychain.pgp.affirmation.resources.GenericHttpsResource;

public class AffirmationCreateHttpsStep1Fragment extends Fragment {

    AffirmationWizard mAffirmationWizard;

    EditText mEditUri;

    /**
     * Creates new instance of this fragment
     */
    public static AffirmationCreateHttpsStep1Fragment newInstance() {
        AffirmationCreateHttpsStep1Fragment frag = new AffirmationCreateHttpsStep1Fragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAffirmationWizard = (AffirmationWizard) getActivity();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.affirmation_create_https_fragment_step1, container, false);

        view.findViewById(R.id.next_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                String uri = "https://" + mEditUri.getText();

                if (!checkUri(uri)) {
                    return;
                }

                String proofNonce = LinkedIdentity.generateNonce();
                String proofText = GenericHttpsResource.generateText(getActivity(),
                        mAffirmationWizard.mFingerprint, proofNonce);

                AffirmationCreateHttpsStep2Fragment frag =
                        AffirmationCreateHttpsStep2Fragment.newInstance(uri, proofNonce, proofText);

                mAffirmationWizard.loadFragment(null, frag, AffirmationWizard.FRAG_ACTION_TO_RIGHT);

            }
        });

        mEditUri = (EditText) view.findViewById(R.id.affirmation_create_https_uri);

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
                                R.drawable.uid_mail_ok, 0);
                    } else {
                        mEditUri.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.uid_mail_bad, 0);
                    }
                } else {
                    // remove drawable if email is empty
                    mEditUri.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }
            }
        });

        mEditUri.setText("mugenguild.com/pgpkey.txt");

        return view;
    }

    private static boolean checkUri(String uri) {
        return Patterns.WEB_URL.matcher(uri).matches();
    }

}
