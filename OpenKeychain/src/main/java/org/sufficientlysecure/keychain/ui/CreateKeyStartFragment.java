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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.ui.dialog.AddEmailDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.widget.EmailEditText;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

import java.util.ArrayList;
import java.util.List;

public class CreateKeyStartFragment extends Fragment {

    CreateKeyActivity mCreateKeyActivity;

    View mCreateKey;
    View mImportKey;
    View mYubiKey;
    TextView mCancel;
    public static final int REQUEST_CODE_CREATE_OR_IMPORT_KEY = 0x00007012;

    /**
     * Creates new instance of this fragment
     */
    public static CreateKeyStartFragment newInstance() {
        CreateKeyStartFragment frag = new CreateKeyStartFragment();

        Bundle args = new Bundle();

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_key_start_fragment, container, false);

        mCreateKey = view.findViewById(R.id.create_key_create_key_button);
        mImportKey = view.findViewById(R.id.create_key_import_button);
        mYubiKey = view.findViewById(R.id.create_key_yubikey_button);
        mCancel = (TextView) view.findViewById(R.id.create_key_cancel);

        if (mCreateKeyActivity.mFirstTime) {
            mCancel.setText(R.string.first_time_skip);
        } else {
            mCancel.setText(R.string.btn_do_not_save);
        }

        mCreateKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CreateKeyNameFragment frag = CreateKeyNameFragment.newInstance();
                mCreateKeyActivity.loadFragment(frag, FragAction.TO_RIGHT);
            }
        });

        mYubiKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CreateKeyYubiWaitFragment frag = new CreateKeyYubiWaitFragment();
                mCreateKeyActivity.loadFragment(frag, FragAction.TO_RIGHT);
            }
        });

        mImportKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mCreateKeyActivity, ImportKeysActivity.class);
                intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN);
                startActivityForResult(intent, REQUEST_CODE_CREATE_OR_IMPORT_KEY);
            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishSetup(null);
            }
        });

        return view;
    }


    private void finishSetup(Intent srcData) {
        if (mCreateKeyActivity.mFirstTime) {
            Preferences prefs = Preferences.getPreferences(mCreateKeyActivity);
            prefs.setFirstTime(false);
        }
        Intent intent = new Intent(mCreateKeyActivity, MainActivity.class);
        // give intent through to display notify
        if (srcData != null) {
            intent.putExtras(srcData);
        }
        startActivity(intent);
        mCreateKeyActivity.finish();
    }

    // workaround for https://code.google.com/p/android/issues/detail?id=61394
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        return keyCode == KeyEvent.KEYCODE_MENU || super.onKeyDown(keyCode, event);
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CREATE_OR_IMPORT_KEY) {
            if (resultCode == Activity.RESULT_OK) {
                finishSetup(data);
            }
        } else {
            Log.e(Constants.TAG, "No valid request code!");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }

}
