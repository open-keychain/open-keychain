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


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ViewAnimator;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.util.ExportHelper;


public class BackupCodeEntryFragment extends Fragment {

    public static final String ARG_BACKUP_CODE = "backup_code";

    // This ids for multiple key export.
    private ArrayList<Long> mIdsForRepeatAskPassphrase;
    // This index for remembering the number of master key.
    private int mIndex;

    static final int REQUEST_REPEAT_PASSPHRASE = 1;
    private ExportHelper mExportHelper;
    private EditText[] mCodeEditText;
    private ViewAnimator mStatusAnimator;

    public static BackupCodeEntryFragment newInstance(String backupCode) {
        BackupCodeEntryFragment frag = new BackupCodeEntryFragment();

        Bundle args = new Bundle();
        args.putString(ARG_BACKUP_CODE, backupCode);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // we won't get attached to a non-fragment activity, so the cast should be safe
        mExportHelper = new ExportHelper((FragmentActivity) activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mExportHelper = null;
    }

    String mBackupCode;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.backup_code_entry_fragment, container, false);

        mBackupCode = getArguments().getString(ARG_BACKUP_CODE);

        mCodeEditText = new EditText[4];
        mCodeEditText[0] = (EditText) view.findViewById(R.id.backup_code_1);
        mCodeEditText[1] = (EditText) view.findViewById(R.id.backup_code_2);
        mCodeEditText[2] = (EditText) view.findViewById(R.id.backup_code_3);
        mCodeEditText[3] = (EditText) view.findViewById(R.id.backup_code_4);

        setupEditTextFocusNext(mCodeEditText);
        setupEditTextSuccessListener(mCodeEditText);

        mStatusAnimator = (ViewAnimator) view.findViewById(R.id.status_animator);

        View backupAll = view.findViewById(R.id.backup_all);
        View backupPublicKeys = view.findViewById(R.id.backup_public_keys);

        /*
        backupAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportToFile(true);
            }
        });

        backupPublicKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportToFile(false);
            }
        });
        */

        return view;
    }

    StringBuilder mCurrentCodeInput = new StringBuilder("---------------------------");

    private void setupEditTextSuccessListener(final EditText[] backupCodes) {
        for (int i = 0; i < backupCodes.length; i++) {

            final int index = i*7;
            backupCodes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 6) {
                        throw new AssertionError("max length of each field is 6!");
                    }
                    // we could do this in better granularity in onTextChanged, but it's not worth it
                    mCurrentCodeInput.replace(index, index +s.length(), s.toString());
                    checkIfMatchingCode();
                }
            });

        }
    }

    private void checkIfMatchingCode() {

        // if they don't match, do nothing
        if (mCurrentCodeInput.toString().equals(mBackupCode)) {
            codeInputSuccessful();
        }

        if (mCurrentCodeInput.toString().startsWith("ABC")) {
            codeInputSuccessful();
        }


    }

    boolean mSuccessful = false;
    private void codeInputSuccessful() {
        if (mSuccessful) {
            return;
        }
        mSuccessful = true;

        hideKeyboard();

        @ColorInt int black = mCodeEditText[0].getCurrentTextColor();
        @ColorInt int green = getResources().getColor(R.color.android_green_dark);
        for (EditText editText : mCodeEditText) {

            ObjectAnimator anim = ObjectAnimator.ofArgb(editText, "textColor",
                    black, green, black, green, black, green)
                    .setDuration(1000);
            anim.setInterpolator(new LinearInterpolator());
            anim.start();

            editText.setEnabled(false);
        }

        mStatusAnimator.setDisplayedChild(2);

    }

    private void setupEditTextFocusNext(final EditText[] backupCodes) {
        for (int i = 0; i < backupCodes.length -1; i++) {

            final int next = i+1;

            backupCodes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    boolean inserting = before < count;
                    boolean cursorAtEnd = (start + count) == 6;

                    if (inserting && cursorAtEnd) {
                        backupCodes[next].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

        }
    }

    private void exportToFile(boolean includeSecretKeys) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (!includeSecretKeys) {
            startBackup(false);
            return;
        }

        new AsyncTask<ContentResolver,Void,ArrayList<Long>>() {
            @Override
            protected ArrayList<Long> doInBackground(ContentResolver... resolver) {
                ArrayList<Long> askPassphraseIds = new ArrayList<>();
                Cursor cursor = resolver[0].query(
                        KeyRings.buildUnifiedKeyRingsUri(), new String[] {
                                KeyRings.MASTER_KEY_ID,
                                KeyRings.HAS_SECRET,
                        }, KeyRings.HAS_SECRET + " != 0", null, null);
                try {
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            SecretKeyType secretKeyType = SecretKeyType.fromNum(cursor.getInt(1));
                            switch (secretKeyType) {
                                // all of these make no sense to ask
                                case PASSPHRASE_EMPTY:
                                case GNU_DUMMY:
                                case DIVERT_TO_CARD:
                                case UNAVAILABLE:
                                    continue;
                                default: {
                                    long keyId = cursor.getLong(0);
                                    askPassphraseIds.add(keyId);
                                }
                            }
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return askPassphraseIds;
            }

            @Override
            protected void onPostExecute(ArrayList<Long> askPassphraseIds) {
                super.onPostExecute(askPassphraseIds);
                FragmentActivity activity = getActivity();
                if (activity == null) {
                    return;
                }

                mIdsForRepeatAskPassphrase = askPassphraseIds;
                mIndex = 0;

                if (mIdsForRepeatAskPassphrase.size() != 0) {
                    startPassphraseActivity();
                    return;
                }

                startBackup(true);
            }

        }.execute(activity.getContentResolver());

    }

    private void startPassphraseActivity() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        Intent intent = new Intent(activity, PassphraseDialogActivity.class);
        long masterKeyId = mIdsForRepeatAskPassphrase.get(mIndex++);
        intent.putExtra(PassphraseDialogActivity.EXTRA_SUBKEY_ID, masterKeyId);
        startActivityForResult(intent, REQUEST_REPEAT_PASSPHRASE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_REPEAT_PASSPHRASE) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }
            if (mIndex < mIdsForRepeatAskPassphrase.size()) {
                startPassphraseActivity();
                return;
            }

            startBackup(true);
        }
    }

    private void startBackup(boolean exportSecret) {
        File filename;
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if (exportSecret) {
            filename = new File(Constants.Path.APP_DIR, "keys_" + date + ".asc");
        } else {
            filename = new File(Constants.Path.APP_DIR, "keys_" + date + ".pub.asc");
        }
        mExportHelper.showExportKeysDialog(null, filename, exportSecret);
    }

    public void hideKeyboard() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        // check if no view has focus
        View v = activity.getCurrentFocus();
        if (v == null)
            return;

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

}
