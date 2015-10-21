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

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.BackupKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.ActionListener;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.widget.ToolableViewAnimator;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Passphrase;

public class BackupCodeFragment extends CryptoOperationFragment<BackupKeyringParcel, ExportResult>
        implements OnBackStackChangedListener {

    public static final String ARG_BACKUP_CODE = "backup_code";
    public static final String BACK_STACK_INPUT = "state_display";
    public static final String ARG_EXPORT_SECRET = "export_secret";
    public static final String ARG_MASTER_KEY_IDS = "master_key_ids";

    public static final String ARG_CURRENT_STATE = "current_state";

    public static final int REQUEST_SAVE = 1;
    public static final String ARG_BACK_STACK = "back_stack";

    // argument variables
    private boolean mExportSecret;
    private long[] mMasterKeyIds;
    String mBackupCode;

    private EditText[] mCodeEditText;
    private ToolableViewAnimator mStatusAnimator, mTitleAnimator, mCodeFieldsAnimator;

    private Integer mBackStackLevel;

    private Uri mCachedBackupUri;
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

    void switchState(BackupCodeState state, boolean animate) {

        switch (state) {
            case STATE_UNINITIALIZED:
                throw new AssertionError("can't switch to uninitialized state, this is a bug!");

            case STATE_DISPLAY:
                mTitleAnimator.setDisplayedChild(0, animate);
                mStatusAnimator.setDisplayedChild(0, animate);
                mCodeFieldsAnimator.setDisplayedChild(0, animate);

                break;

            case STATE_INPUT:
                mTitleAnimator.setDisplayedChild(1, animate);
                mStatusAnimator.setDisplayedChild(1, animate);
                mCodeFieldsAnimator.setDisplayedChild(1, animate);

                for (EditText editText : mCodeEditText) {
                    editText.setText("");
                }

                pushBackStackEntry();

                break;

            case STATE_INPUT_ERROR: {
                mTitleAnimator.setDisplayedChild(1, false);
                mStatusAnimator.setDisplayedChild(2, animate);
                mCodeFieldsAnimator.setDisplayedChild(1, false);

                hideKeyboard();

                if (animate) {
                    @ColorInt int black = mCodeEditText[0].getCurrentTextColor();
                    @ColorInt int red = getResources().getColor(R.color.android_red_dark);
                    animateFlashText(mCodeEditText, black, red, false);
                }

                break;
            }

            case STATE_OK: {
                mTitleAnimator.setDisplayedChild(2, animate);
                mStatusAnimator.setDisplayedChild(3, animate);
                mCodeFieldsAnimator.setDisplayedChild(1, false);

                hideKeyboard();

                for (EditText editText : mCodeEditText) {
                    editText.setEnabled(false);
                }

                @ColorInt int green = getResources().getColor(R.color.android_green_dark);
                if (animate) {
                    @ColorInt int black = mCodeEditText[0].getCurrentTextColor();
                    animateFlashText(mCodeEditText, black, green, true);
                } else {
                    for (TextView textView : mCodeEditText) {
                        textView.setTextColor(green);
                    }
                }

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

        mStatusAnimator = (ToolableViewAnimator) view.findViewById(R.id.status_animator);
        mTitleAnimator = (ToolableViewAnimator) view.findViewById(R.id.title_animator);
        mCodeFieldsAnimator = (ToolableViewAnimator) view.findViewById(R.id.code_animator);

        View backupInput = view.findViewById(R.id.button_backup_input);
        backupInput.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchState(BackupCodeState.STATE_INPUT, true);
            }
        });

        view.findViewById(R.id.button_backup_save).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mShareNotSave = false;
                startBackup();
            }
        });

        view.findViewById(R.id.button_backup_share).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mShareNotSave = true;
                startBackup();
            }
        });

        view.findViewById(R.id.button_backup_back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragMan = getFragmentManager();
                if (fragMan != null) {
                    fragMan.popBackStack();
                }
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            int savedBackStack = savedInstanceState.getInt(ARG_BACK_STACK);
            if (savedBackStack >= 0) {
                mBackStackLevel = savedBackStack;
                // unchecked use, we know that this one is available in onViewCreated
                getFragmentManager().addOnBackStackChangedListener(this);
            }
            BackupCodeState savedState = BackupCodeState.values()[savedInstanceState.getInt(ARG_CURRENT_STATE)];
            switchState(savedState, false);
        } else if (mCurrentState == BackupCodeState.STATE_UNINITIALIZED) {
            switchState(BackupCodeState.STATE_DISPLAY, true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_CURRENT_STATE, mCurrentState.ordinal());
        outState.putInt(ARG_BACK_STACK, mBackStackLevel == null ? -1 : mBackStackLevel);
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
            switchState(BackupCodeState.STATE_OK, true);
            return;
        }

        switchState(BackupCodeState.STATE_INPUT_ERROR, true);

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
        for (int i = 0; i < backupCodes.length - 1; i++) {

            final int next = i + 1;

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
        if (mBackStackLevel != null) {
            return;
        }
        FragmentManager fragMan = getFragmentManager();
        mBackStackLevel = fragMan.getBackStackEntryCount();
        fragMan.beginTransaction().addToBackStack(BACK_STACK_INPUT).commit();
        fragMan.addOnBackStackChangedListener(this);
    }

    private void popBackStackNoAction() {
        FragmentManager fragMan = getFragmentManager();
        fragMan.removeOnBackStackChangedListener(this);
        fragMan.popBackStackImmediate(BACK_STACK_INPUT, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        mBackStackLevel = null;
    }

    @Override
    public void onBackStackChanged() {
        FragmentManager fragMan = getFragmentManager();
        if (mBackStackLevel != null && fragMan.getBackStackEntryCount() == mBackStackLevel) {
            fragMan.removeOnBackStackChangedListener(this);
            switchState(BackupCodeState.STATE_DISPLAY, true);
            mBackStackLevel = null;
        }
    }

    private void startBackup() {

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String filename = Constants.FILE_ENCRYPTED_BACKUP_PREFIX + date
                + (mExportSecret ? Constants.FILE_EXTENSION_ENCRYPTED_BACKUP_SECRET
                : Constants.FILE_EXTENSION_ENCRYPTED_BACKUP_PUBLIC);

        if (mCachedBackupUri == null) {
            mCachedBackupUri = TemporaryFileProvider.createFile(activity, filename,
                    Constants.MIME_TYPE_ENCRYPTED_ALTERNATE);
            cryptoOperation();
            return;
        }

        if (mShareNotSave) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(Constants.MIME_TYPE_ENCRYPTED_ALTERNATE);
            intent.putExtra(Intent.EXTRA_STREAM, mCachedBackupUri);
            startActivity(intent);
        } else {
            saveFile(filename, false);
        }

    }

    private void saveFile(final String filename, boolean overwrite) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        // for kitkat and above, we have the document api
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            FileHelper.saveDocument(this, filename, Constants.MIME_TYPE_ENCRYPTED_ALTERNATE, REQUEST_SAVE);
            return;
        }

        File file = new File(Constants.Path.APP_DIR, filename);

        if (!overwrite && file.exists()) {
            Notify.create(activity, R.string.snack_backup_exists, Style.WARN, new ActionListener() {
                @Override
                public void onAction() {
                    saveFile(filename, true);
                }
            }, R.string.snack_btn_overwrite).show();
            return;
        }

        try {
            FileHelper.copyUriData(activity, mCachedBackupUri, Uri.fromFile(file));
            Notify.create(activity, R.string.snack_backup_saved_dir, Style.OK).show();
        } catch (IOException e) {
            Notify.create(activity, R.string.snack_backup_error_saving, Style.ERROR).show();
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
            FileHelper.copyUriData(activity, mCachedBackupUri, outputUri);
            Notify.create(activity, R.string.snack_backup_saved, Style.OK).show();
        } catch (IOException e) {
            Notify.create(activity, R.string.snack_backup_error_saving, Style.ERROR).show();
        }
    }

    @Nullable
    @Override
    public BackupKeyringParcel createOperationInput() {
        return new BackupKeyringParcel(new Passphrase(mBackupCode), mMasterKeyIds, mExportSecret, mCachedBackupUri);
    }

    @Override
    public void onCryptoOperationSuccess(ExportResult result) {
        startBackup();
    }

    @Override
    public void onCryptoOperationError(ExportResult result) {
        result.createNotify(getActivity()).show();
        mCachedBackupUri = null;
    }

    @Override
    public void onCryptoOperationCancelled() {
        mCachedBackupUri = null;
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
