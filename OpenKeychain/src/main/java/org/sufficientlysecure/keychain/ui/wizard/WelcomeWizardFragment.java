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
package org.sufficientlysecure.keychain.ui.wizard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.ImportKeysActivity;
import org.sufficientlysecure.keychain.ui.MainActivity;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

public class WelcomeWizardFragment extends WizardFragment {
    public static final int REQUEST_CODE_IMPORT_KEY = 0x00007012;

    public static WelcomeWizardFragment newInstance() {
        return new WelcomeWizardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_welcome_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView textView = (TextView) view.findViewById(R.id.create_key_cancel);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancelClicked();
            }
        });

        if (mWizardFragmentListener.isFirstTime()) {
            textView.setText(R.string.first_time_skip);
        } else {
            textView.setText(R.string.btn_do_not_save);
        }

        textView = (TextView) view.findViewById(R.id.create_key_create_key_button);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateKeyClicked();
            }
        });

        textView = (TextView) view.findViewById(R.id.create_key_import_button);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onKeyImportClicked();
            }
        });

        textView = (TextView) view.findViewById(R.id.create_key_yubikey_button);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateYubiKeyClicked();
            }
        });


        mWizardFragmentListener.onHideNavigationButtons(true, true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Activity activity = getActivity();
        if (requestCode == REQUEST_CODE_IMPORT_KEY) {
            if (resultCode == Activity.RESULT_OK) {
                if (mWizardFragmentListener != null) {
                    if (mWizardFragmentListener.isFirstTime()) {
                        Preferences prefs = Preferences.getPreferences(activity);
                        prefs.setFirstTime(false);
                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.putExtras(data);
                        startActivity(intent);
                        activity.finish();
                    } else {
                        // just finish activity and return data
                        activity.setResult(Activity.RESULT_OK, data);
                        activity.finish();
                    }
                }
            }
        } else {
            Log.e(Constants.TAG, "No valid request code!");
        }
    }

    public boolean onBackClicked() {
        return false;
    }

    public void onCancelClicked() {
        mWizardFragmentListener.cancelRequest();
    }

    public void onCreateKeyClicked() {
        mWizardFragmentListener.onAdvanceToNextWizardStep();
    }

    public void onKeyImportClicked() {
        Activity activity = getActivity();
        Intent intent = new Intent(activity, ImportKeysActivity.class);
        intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN);
        activity.startActivityForResult(intent, REQUEST_CODE_IMPORT_KEY);
    }

    public void onCreateYubiKeyClicked() {
        if (mWizardFragmentListener != null) {
            mWizardFragmentListener.setUseYubiKey();
            mWizardFragmentListener.onAdvanceToNextWizardStep();
        }
    }
}
