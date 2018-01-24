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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenConnection;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;

public class CreateSecurityTokenPinFragment extends Fragment {

    // view
    CreateKeyActivity mCreateKeyActivity;
    EditText mPin;
    EditText mPinRepeat;
    TextView mAdminPin;
    View mBackButton;
    View mNextButton;

    // top 20 according to http://datagenetics.com/blog/september32012/index.html
    // extended from 4 digits to 6 for our use case
    private static HashSet<String> sPinBlacklist = new HashSet<>(Arrays.asList(
            "123456",
            "111111",
            "000000",
            "121212",
            "777777",
            // "1004", makes no sense as "100004", see blog post
            "200000",
            "444444",
            "222222",
            "696969",
            "999999",
            "333333",
            "555555",
            "666666",
            "111222",
            "131313",
            "888888",
            "654321",
            "200001",
            "101010",
            "XXXXXX" // additional: should not be used, as this PIN is entered for resetting the card
    ));

    /**
     * Creates new instance of this fragment
     */
    public static CreateSecurityTokenPinFragment newInstance() {
        CreateSecurityTokenPinFragment frag = new CreateSecurityTokenPinFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    /**
     * Checks if text of given EditText is not empty. If it is empty an error is
     * set and the EditText gets the focus.
     *
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_yubi_key_pin_fragment, container, false);

        mPin = view.findViewById(R.id.create_yubi_key_pin);
        mPinRepeat = view.findViewById(R.id.create_yubi_key_pin_repeat);
        mAdminPin = view.findViewById(R.id.create_yubi_key_admin_pin);
        mBackButton = view.findViewById(R.id.create_key_back_button);
        mNextButton = view.findViewById(R.id.create_key_next_button);

        if (mCreateKeyActivity.mSecurityTokenPin == null) {
            new AsyncTask<Void, Void, Passphrase>() {
                @Override
                protected Passphrase doInBackground(Void... unused) {
                    if (Constants.DEBUG) {
                        return new Passphrase("12345678");
                    }

                    SecureRandom secureRandom = new SecureRandom();
                    // min = 8, we choose 8
                    String adminPin = "" + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9);

                    return new Passphrase(adminPin);
                }

                @Override
                protected void onPostExecute(Passphrase adminPin) {
                    mCreateKeyActivity.mSecurityTokenAdminPin = adminPin;

                    mAdminPin.setText(mCreateKeyActivity.mSecurityTokenAdminPin.toStringUnsafe());
                }
            }.execute();
        } else {
            mAdminPin.setText(mCreateKeyActivity.mSecurityTokenAdminPin.toStringUnsafe());
        }

        mPin.requestFocus();
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

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }

    private void back() {
        hideKeyboard();
        mCreateKeyActivity.loadFragment(null, FragAction.TO_LEFT);
    }

    private void nextClicked() {
        if (isEditTextNotEmpty(getActivity(), mPin)) {

            if (!areEditTextsEqual(mPin, mPinRepeat)) {
                mPinRepeat.setError(getString(R.string.create_key_passphrases_not_equal));
                mPinRepeat.requestFocus();
                return;
            }

            if (mPin.getText().toString().length() < 6) {
                mPin.setError(getString(R.string.create_key_yubi_key_pin_too_short));
                mPin.requestFocus();
                return;
            }

            if (sPinBlacklist.contains(mPin.getText().toString())) {
                mPin.setError(getString(R.string.create_key_yubi_key_pin_insecure));
                mPin.requestFocus();
                return;
            }

            mCreateKeyActivity.mSecurityTokenPin = new Passphrase(mPin.getText().toString());

            final double version = mCreateKeyActivity.tokenInfo.getOpenPgpVersion();

            Fragment frag;
            if (version >= 3.0) {
                frag = CreateSecurityTokenAlgorithmFragment.newInstance();
            } else {
                frag = CreateKeyFinalFragment.newInstance();
            }
            hideKeyboard();
            mCreateKeyActivity.loadFragment(frag, FragAction.TO_RIGHT);
        }
    }

    private void hideKeyboard() {
        if (getActivity() == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        // check if no view has focus
        View v = getActivity().getCurrentFocus();
        if (v == null)
            return;

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

}
