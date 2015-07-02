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

package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.security.SecureRandom;

public class CreateYubiKeyPinFragment extends Fragment {

    // view
    CreateKeyActivity mCreateKeyActivity;
    TextView mPin;
    TextView mAdminPin;
    View mBackButton;
    View mNextButton;

    /**
     * Creates new instance of this fragment
     */
    public static CreateYubiKeyPinFragment newInstance() {
        CreateYubiKeyPinFragment frag = new CreateYubiKeyPinFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_yubi_key_pin_fragment, container, false);

        mPin = (TextView) view.findViewById(R.id.create_yubi_key_pin);
        mAdminPin = (TextView) view.findViewById(R.id.create_yubi_key_admin_pin);
        mBackButton = view.findViewById(R.id.create_key_back_button);
        mNextButton = view.findViewById(R.id.create_key_next_button);

        if (mCreateKeyActivity.mYubiKeyPin == null) {
            new AsyncTask<Void, Void, Pair<Passphrase, Passphrase>>() {
                @Override
                protected Pair<Passphrase, Passphrase> doInBackground(Void... unused) {
                    SecureRandom secureRandom = new SecureRandom();
                    // min = 6, we choose 6
                    String pin = "" + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9);
                    // min = 8, we choose 10, but 6 are equals the PIN
                    String adminPin = pin + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9)
                            + secureRandom.nextInt(9);

                    return new Pair<>(new Passphrase(pin), new Passphrase(adminPin));
                }

                @Override
                protected void onPostExecute(Pair<Passphrase, Passphrase> pair) {
                    mCreateKeyActivity.mYubiKeyPin = pair.first;
                    mCreateKeyActivity.mYubiKeyAdminPin = pair.second;

                    mPin.setText(mCreateKeyActivity.mYubiKeyPin.toStringUnsafe());
                    mAdminPin.setText(mCreateKeyActivity.mYubiKeyAdminPin.toStringUnsafe());
                }
            }.execute();
        } else {
            mPin.setText(mCreateKeyActivity.mYubiKeyPin.toStringUnsafe());
            mAdminPin.setText(mCreateKeyActivity.mYubiKeyAdminPin.toStringUnsafe());
        }

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }


    private void nextClicked() {
        CreateYubiKeyPinRepeatFragment frag = CreateYubiKeyPinRepeatFragment.newInstance();
        mCreateKeyActivity.loadFragment(frag, FragAction.TO_RIGHT);
    }

    private void back() {
        mCreateKeyActivity.loadFragment(null, FragAction.TO_LEFT);
    }

}
