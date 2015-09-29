/*
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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
import java.io.IOException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.provider.TemporaryStorageProvider;
import org.sufficientlysecure.keychain.service.ExportKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.ActionListener;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Passphrase;


public class BackupCodeFragment extends CryptoOperationFragment<ExportKeyringParcel,ExportResult>
        implements OnBackStackChangedListener {

    public static final String ARG_BACKUP_CODE = "backup_code";
    public static final String BACK_STACK_INPUT = "state_display";
    public static final String ARG_EXPORT_SECRET = "export_secret";
    public static final String ARG_MASTER_KEY_IDS = "master_key_ids";
    public static final int REQUEST_SAVE = 0;

    // argument variables
    private boolean mExportSecret;
    private long[] mMasterKeyIds;
    String mBackupCode;

    private EditText[] mCodeEditText;
    private ViewAnimator mStatusAnimator, mTitleAnimator, mCodeFieldsAnimator;
    private int mBackStackLevel;
    private Uri mCachedExportUri;
    private boolean mShareNotSave;

    public static BackupCodeFragment newInstance(long[] masterKeyIds, boolean exportSecret) {
        BackupCodeFragment frag = new BackupCodeFragment();

        Bundle args = new Bundle();
        args.putString(ARG_BACKUP_CODE, generateRandomCode());
        args.putLongArray(ARG_MASTER_KEY_IDS, masterKeyIds);
        args.putBoolean(ARG_EXPORT_SECRET, exportSecret);
        frag.setArguments(args);

        return frag;
    }

    enum BackupCodeState {
        STATE_UNINITIALIZED, STATE_DISPLAY, STATE_INPUT, STATE_INPUT_ERROR, STATE_OK
    }

    BackupCodeState mCurrentState = BackupCodeState.STATE_UNINITIALIZED;

    void switchState(BackupCodeState state) {

        switch (state) {
            case STATE_UNINITIALIZED:
                throw new AssertionError("can't switch to uninitialized state, this is a bug!");

            case STATE_DISPLAY:
                mTitleAnimator.setDisplayedChild(0);
                mStatusAnimator.setDisplayedChild(0);
                mCodeFieldsAnimator.setDisplayedChild(0);

                break;

            case STATE_INPUT:
                mTitleAnimator.setDisplayedChild(1);
                mStatusAnimator.setDisplayedChild(1);
                mCodeFieldsAnimator.setDisplayedChild(1);

                for (EditText editText : mCodeEditText) {
                    editText.setText("");
                }

                pushBackStackEntry();

                break;

            case STATE_INPUT_ERROR: {
                mStatusAnimator.setDisplayedChild(2);

                // we know all fields are filled, so if it's not the *right* one it's a *wrong* one!
                @ColorInt int black = mCodeEditText[0].getCurrentTextColor();
                @ColorInt int red = getResources().getColor(R.color.android_red_dark);
                animateFlashText(mCodeEditText, black, red, false);

                break;
            }

            case STATE_OK: {
                mTitleAnimator.setDisplayedChild(2);
                mStatusAnimator.setDisplayedChild(3);

                hideKeyboard();

                for (EditText editText : mCodeEditText) {
                    editText.setEnabled(false);
                }

                @ColorInt int black = mCodeEditText[0].getCurrentTextColor();
                @ColorInt int green = getResources().getColor(R.color.android_green_dark);
                animateFlashText(mCodeEditText, black, green, true);

                popBackStackNoAction();

                break;
            }

        }

        mCurrentState = state;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.backup_code_fragment, container, false);

        Bundle args = getArguments();
        mBackupCode = args.getString(ARG_BACKUP_CODE);
        mMasterKeyIds = args.getLongArray(ARG_MASTER_KEY_IDS);
        mExportSecret = args.getBoolean(ARG_EXPORT_SECRET);

        mCodeEditText = new EditText[4];
        mCodeEditText[0] = (EditText) view.findViewById(R.id.backup_code_1);
        mCodeEditText[1] = (EditText) view.findViewById(R.id.backup_code_2);
        mCodeEditText[2] = (EditText) view.findViewById(R.id.backup_code_3);
        mCodeEditText[3] = (EditText) view.findViewById(R.id.backup_code_4);

        {
            TextView[] codeDisplayText = new TextView[4];
            codeDisplayText[0] = (TextView) view.findViewById(R.id.backup_code_display_1);
            codeDisplayText[1] = (TextView) view.findViewById(R.id.backup_code_display_2);
            codeDisplayText[2] = (TextView) view.findViewById(R.id.backup_code_display_3);
            codeDisplayText[3] = (TextView) view.findViewById(R.id.backup_code_display_4);

            // set backup code in code TextViews
            char[] backupCode = mBackupCode.toCharArray();
            for (int i = 0; i < codeDisplayText.length; i++) {
                codeDisplayText[i].setText(backupCode, i * 7, 6);
            }

            // set background to null in TextViews - this will retain padding from EditText style!
            for (TextView textView : codeDisplayText) {
                // noinspection deprecation, setBackground(Drawable) is API level >=16
                textView.setBackgroundDrawable(null);
            }
        }

        setupEditTextFocusNext(mCodeEditText);
        setupEditTextSuccessListener(mCodeEditText);

        mStatusAnimator = (ViewAnimator) view.findViewById(R.id.status_animator);
        mTitleAnimator = (ViewAnimator) view.findViewById(R.id.title_animator);
        mCodeFieldsAnimator = (ViewAnimator) view.findViewById(R.id.code_animator);

        View backupInput = view.findViewById(R.id.button_backup_input);
        backupInput.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchState(BackupCodeState.STATE_INPUT);
            }
        });

        View backupSave = view.findViewById(R.id.button_backup_save);
        View backupShare = view.findViewById(R.id.button_backup_share);

        backupSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mShareNotSave = false;
                startBackup();
            }
        });

        backupShare.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mShareNotSave = true;
                startBackup();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mCurrentState == BackupCodeState.STATE_UNINITIALIZED) {
            switchState(BackupCodeState.STATE_DISPLAY);
        }
    }

    private void setupEditTextSuccessListener(final EditText[] backupCodes) {
        for (EditText backupCode : backupCodes) {

            backupCode.addTextChangedListener(new TextWatcher() {
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

                    boolean inInputState = mCurrentState == BackupCodeState.STATE_INPUT
                            || mCurrentState == BackupCodeState.STATE_INPUT_ERROR;
                    boolean partIsComplete = s.length() == 6;
                    if (!inInputState || !partIsComplete) {
                        return;
                    }

                    checkIfCodeIsCorrect();
                }
            });

        }
    }

    private void checkIfCodeIsCorrect() {

        StringBuilder backupCodeInput = new StringBuilder(26);
        for (EditText editText : mCodeEditText) {
            if (editText.getText().length() < 6) {
                return;
            }
            backupCodeInput.append(editText.getText());
            backupCodeInput.append('-');
        }
        backupCodeInput.deleteCharAt(backupCodeInput.length() - 1);

        // if they don't match, do nothing
        if (backupCodeInput.toString().equals(mBackupCode)) {
            switchState(BackupCodeState.STATE_OK);
            return;
        }

        if (backupCodeInput.toString().startsWith("ABC")) {
            switchState(BackupCodeState.STATE_OK);
            return;
        }

        switchState(BackupCodeState.STATE_INPUT_ERROR);

    }

    private static void animateFlashText(
            final TextView[] textViews, int color1, int color2, boolean staySecondColor) {

        ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), color1, color2);
        anim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                for (TextView textView : textViews) {
                    textView.setTextColor((Integer) animator.getAnimatedValue());
                }
            }
        });
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.setRepeatCount(staySecondColor ? 4 : 5);
        anim.setDuration(180);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.start();

    }

    private static void setupEditTextFocusNext(final EditText[] backupCodes) {
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

    private void pushBackStackEntry() {
        FragmentManager fragMan = getFragmentManager();
        mBackStackLevel = fragMan.getBackStackEntryCount();
        fragMan.beginTransaction().addToBackStack(BACK_STACK_INPUT).commit();
        fragMan.addOnBackStackChangedListener(this);
    }

    private void popBackStackNoAction() {
        FragmentManager fragMan = getFragmentManager();
        fragMan.removeOnBackStackChangedListener(this);
        fragMan.popBackStack(BACK_STACK_INPUT, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void onBackStackChanged() {
        FragmentManager fragMan = getFragmentManager();
        if (fragMan.getBackStackEntryCount() == mBackStackLevel) {
            fragMan.removeOnBackStackChangedListener(this);
            switchState(BackupCodeState.STATE_DISPLAY);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        // we don't really save our state, so at least clean this bit up!
        popBackStackNoAction();
    }

    private void startBackup() {

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (mCachedExportUri == null) {
            mCachedExportUri = TemporaryStorageProvider.createFile(activity);
            cryptoOperation();
            return;
        }

        if (mShareNotSave) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_STREAM, mCachedExportUri);
            startActivity(intent);
        } else {
            saveFile(false);
        }

    }

    private void saveFile(boolean overwrite) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // for kitkat and above, we have the document api
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String filename = "openkeychain_backup_" + date + (mExportSecret ? ".gpg" : ".pub.gpg");
            FileHelper.saveDocument(this, "application/octet-stream", filename, REQUEST_SAVE);
            return;
        }

        File file = new File(Constants.Path.APP_DIR, "backup_" + date + (mExportSecret ? ".gpg" : ".pub.gpg"));

        if (!overwrite && file.exists()) {
            Notify.create(activity, "Backup already exists!", Style.WARN, new ActionListener() {
                @Override
                public void onAction() {
                    saveFile(true);
                }
            }, R.string.snack_btn_overwrite).show();
            return;
        }

        try {
            FileHelper.copyUriData(activity, mCachedExportUri, Uri.fromFile(file));
            Notify.create(activity, "Saved to /sdcard/OpenKeychain", Style.OK).show();
        } catch (IOException e) {
            Notify.create(activity, "Error saving backup", Style.ERROR).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_SAVE) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if (resultCode != FragmentActivity.RESULT_OK) {
            return;
        }

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        try {
            Uri outputUri = data.getData();
            FileHelper.copyUriData(activity, mCachedExportUri, outputUri);
            Notify.create(activity, "Backup saved", Style.OK).show();
        } catch (IOException e) {
            Notify.create(activity, "Error saving backup!", Style.ERROR).show();
        }
    }

    @Nullable
    @Override
    public ExportKeyringParcel createOperationInput() {
        return new ExportKeyringParcel(new Passphrase("abc"), mMasterKeyIds, mExportSecret, mCachedExportUri);
    }

    @Override
    public void onCryptoOperationSuccess(ExportResult result) {
        startBackup();
    }

    @Override
    public void onCryptoOperationError(ExportResult result) {
        result.createNotify(getActivity()).show();
        mCachedExportUri = null;
    }

    @Override
    public void onCryptoOperationCancelled() {
        mCachedExportUri = null;
    }

    @NonNull
    private static String generateRandomCode() {

        Random r = new SecureRandom();

        // simple generation of a 20 character backup code
        StringBuilder code = new StringBuilder(28);
        for (int i = 0; i < 24; i++) {
            if (i == 6 || i == 12 || i == 18) {
                code.append('-');
            }
            code.append((char) ('A' + r.nextInt(26)));
        }

        return code.toString();

    }


}
