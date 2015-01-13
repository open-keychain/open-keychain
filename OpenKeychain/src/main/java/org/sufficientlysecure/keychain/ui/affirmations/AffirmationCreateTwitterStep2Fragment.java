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
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity;

public class AffirmationCreateTwitterStep2Fragment extends Fragment {

    private static final int REQUEST_CODE_OUTPUT = 0x00007007;

    public static final String HANDLE = "uri", NONCE = "nonce", TEXT = "text";

    AffirmationWizard mAffirmationWizard;

    EditText mEditTweetCustom, mEditTweetPreview;
    ImageView mVerifyImage;
    View mVerifyProgress;
    TextView mVerifyStatus, mEditTweetTextLen;

    String mResourceHandle;
    String mResourceNonce, mResourceString;

    /**
     * Creates new instance of this fragment
     */
    public static AffirmationCreateTwitterStep2Fragment newInstance
            (String handle, String proofNonce, String proofText) {

        AffirmationCreateTwitterStep2Fragment frag = new AffirmationCreateTwitterStep2Fragment();

        Bundle args = new Bundle();
        args.putString(HANDLE, handle);
        args.putString(NONCE, proofNonce);
        args.putString(TEXT, proofText);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.affirmation_create_twitter_fragment_step2, container, false);

        mResourceHandle = getArguments().getString(HANDLE);
        mResourceNonce = getArguments().getString(NONCE);
        mResourceString = getArguments().getString(TEXT);

        view.findViewById(R.id.next_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                AffirmationCreateTwitterStep3Fragment frag =
                        AffirmationCreateTwitterStep3Fragment.newInstance(mResourceHandle,
                                mResourceNonce, mResourceString,
                                mEditTweetCustom.getText().toString());

                mAffirmationWizard.loadFragment(null, frag, AffirmationWizard.FRAG_ACTION_TO_RIGHT);
            }
        });

        view.findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAffirmationWizard.loadFragment(null, null, AffirmationWizard.FRAG_ACTION_TO_LEFT);
            }
        });

        mVerifyImage = (ImageView) view.findViewById(R.id.verify_image);
        mVerifyProgress = view.findViewById(R.id.verify_progress);
        mVerifyStatus = (TextView) view.findViewById(R.id.verify_status);

        mEditTweetPreview = (EditText) view.findViewById(R.id.linked_create_twitter_preview);
        mEditTweetPreview.setText(mResourceString);

        mEditTweetCustom = (EditText) view.findViewById(R.id.linked_create_twitter_custom);
        mEditTweetCustom.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(139 - mResourceString.length())
        });

        mEditTweetTextLen = (TextView) view.findViewById(R.id.linked_create_twitter_textlen);
        mEditTweetTextLen.setText(mResourceString.length() + "/140");

        mEditTweetCustom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable != null && editable.length() > 0) {
                    String str = editable + " " + mResourceString;
                    mEditTweetPreview.setText(str);

                    mEditTweetTextLen.setText(str.length() + "/140");
                    mEditTweetTextLen.setTextColor(getResources().getColor(str.length() == 140
                            ? R.color.android_red_dark
                            : R.color.primary_dark_material_light));


                } else {
                    mEditTweetPreview.setText(mResourceString);
                    mEditTweetTextLen.setText(mResourceString.length() + "/140");
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAffirmationWizard = (AffirmationWizard) getActivity();
    }

}
