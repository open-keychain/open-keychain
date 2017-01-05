/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.securitytoken.KeyFormat;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenHelper;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.util.Choice;

import java.util.ArrayList;
import java.util.List;

public class CreateSecurityTokenAlgorithmFragment extends Fragment {

    public enum SupportedKeyType {
        RSA_2048, RSA_3072, RSA_4096, ECC_P256, ECC_P384, ECC_P521
    }

    private CreateKeyActivity mCreateKeyActivity;

    private View mBackButton;
    private View mNextButton;

    private Spinner mSignKeySpinner;
    private Spinner mDecKeySpinner;
    private Spinner mAuthKeySpinner;

    /**
     * Creates new instance of this fragment
     */
    public static CreateSecurityTokenAlgorithmFragment newInstance() {
        CreateSecurityTokenAlgorithmFragment frag = new CreateSecurityTokenAlgorithmFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final FragmentActivity context = getActivity();
        View view = inflater.inflate(R.layout.create_yubi_key_algorithm_fragment, container, false);

        mBackButton = view.findViewById(R.id.create_key_back_button);
        mNextButton = view.findViewById(R.id.create_key_next_button);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                back();
            }
        });
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextClicked();
            }
        });

        mSignKeySpinner = (Spinner) view.findViewById(R.id.create_key_yubi_key_algorithm_sign);
        mDecKeySpinner = (Spinner) view.findViewById(R.id.create_key_yubi_key_algorithm_dec);
        mAuthKeySpinner = (Spinner) view.findViewById(R.id.create_key_yubi_key_algorithm_auth);

        ArrayList<Choice<SupportedKeyType>> choices = new ArrayList<>();

        choices.add(new Choice<>(SupportedKeyType.RSA_2048, getResources().getString(
                R.string.rsa_2048), getResources().getString(R.string.rsa_2048_description_html)));
        choices.add(new Choice<>(SupportedKeyType.RSA_3072, getResources().getString(
                R.string.rsa_3072), getResources().getString(R.string.rsa_3072_description_html)));
        choices.add(new Choice<>(SupportedKeyType.RSA_4096, getResources().getString(
                R.string.rsa_4096), getResources().getString(R.string.rsa_4096_description_html)));

        final double version = SecurityTokenHelper.parseOpenPgpVersion(mCreateKeyActivity.mSecurityTokenAid);

        if (version >= 3.0) {
            choices.add(new Choice<>(SupportedKeyType.ECC_P256, getResources().getString(
                    R.string.ecc_p256), getResources().getString(R.string.ecc_p256_description_html)));
            choices.add(new Choice<>(SupportedKeyType.ECC_P384, getResources().getString(
                    R.string.ecc_p384), getResources().getString(R.string.ecc_p384_description_html)));
            choices.add(new Choice<>(SupportedKeyType.ECC_P521, getResources().getString(
                    R.string.ecc_p521), getResources().getString(R.string.ecc_p521_description_html)));
        }

        TwoLineArrayAdapter adapter = new TwoLineArrayAdapter(context,
                android.R.layout.simple_spinner_item, choices);
        mSignKeySpinner.setAdapter(adapter);
        mDecKeySpinner.setAdapter(adapter);
        mAuthKeySpinner.setAdapter(adapter);

        // make ECC nist256 the default for v3.x
        for (int i = 0; i < choices.size(); ++i) {
            if (version >= 3.0) {
                if (choices.get(i).getId() == SupportedKeyType.ECC_P256) {
                    mSignKeySpinner.setSelection(i);
                    mDecKeySpinner.setSelection(i);
                    mAuthKeySpinner.setSelection(i);
                    break;
                }
            } else {
                if (choices.get(i).getId() == SupportedKeyType.RSA_2048) {
                    mSignKeySpinner.setSelection(i);
                    mDecKeySpinner.setSelection(i);
                    mAuthKeySpinner.setSelection(i);
                    break;
                }
            }
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }

    private void back() {
        mCreateKeyActivity.loadFragment(null, FragAction.TO_LEFT);
    }

    private void nextClicked() {
        mCreateKeyActivity.mSecurityTokenSign = KeyFormat.fromCreationKeyType(((Choice<SupportedKeyType>) mSignKeySpinner.getSelectedItem()).getId(), false);
        mCreateKeyActivity.mSecurityTokenDec = KeyFormat.fromCreationKeyType(((Choice<SupportedKeyType>) mDecKeySpinner.getSelectedItem()).getId(), true);
        mCreateKeyActivity.mSecurityTokenAuth = KeyFormat.fromCreationKeyType(((Choice<SupportedKeyType>) mAuthKeySpinner.getSelectedItem()).getId(), false);

        CreateKeyFinalFragment frag = CreateKeyFinalFragment.newInstance();
        mCreateKeyActivity.loadFragment(frag, FragAction.TO_RIGHT);
    }


    private class TwoLineArrayAdapter extends ArrayAdapter<Choice<SupportedKeyType>> {
        TwoLineArrayAdapter(Context context, int resource, List<Choice<SupportedKeyType>> objects) {
            super(context, resource, objects);
        }


        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            // inflate view if not given one
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.two_line_spinner_dropdown_item, parent, false);
            }

            Choice c = this.getItem(position);

            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            text1.setText(c.getName());
            text2.setText(Html.fromHtml(c.getDescription()));

            return convertView;
        }
    }
}
