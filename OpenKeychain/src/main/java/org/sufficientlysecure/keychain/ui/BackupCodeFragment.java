/*
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.ActionListener;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.widget.ToolableViewAnimator;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.ParcelableHashMap;
import org.sufficientlysecure.keychain.util.ParcelableLong;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class BackupCodeFragment extends CryptoOperationFragment<BackupKeyringParcel, ExportResult>
        implements OnBackStackChangedListener {

    public static final String ARG_BACKUP_CODE = "backup_code";
    public static final String BACK_STACK_INPUT = "state_display";
    public static final String ARG_EXPORT_SECRET = "export_secret";
    public static final String ARG_EXECUTE_BACKUP_OPERATION = "execute_backup_operation";
    public static final String ARG_MASTER_KEY_IDS = "master_key_ids";
    public static final String ARG_CURRENT_STATE = "current_state";
    public static final String ARG_PARCELABLE_PASSPHRASES = "parcelable_passphrases";


    public static final int REQUEST_SAVE = 1;
    public static final String ARG_BACK_STACK = "back_stack";

    // https://github.com/open-keychain/open-keychain/wiki/Backups
    // excludes 0 and O
    private static final char[] mBackupCodeAlphabet =
            new char[]{'1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
                    'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    // argument variables
    private boolean mExportSecret;
    private long[] mMasterKeyIds;
    String mBackupCode;
    private boolean mExecuteBackupOperation;

    private EditText[] mCodeEditText;

    private ToolableViewAnimator mStatusAnimator, mTitleAnimator, mCodeFieldsAnimator;
    private Integer mBackStackLevel;

    private HashMap<Long, Passphrase> mPassphrases;
    private Uri mCachedBackupUri;
    private boolean mShareNotSave;
    private boolean mDebugModeAcceptAnyCode;

    public static BackupCodeFragment newInstance(long[] masterKeyIds, boolean exportSecret,
                                                 HashMap<Long, Passphrase> passphrases,
                                                 boolean executeBackupOperation) {
        BackupCodeFragment frag = new BackupCodeFragment();

        Bundle args = new Bundle();
        args.putString(ARG_BACKUP_CODE, generateRandomBackupCode());
        args.putLongArray(ARG_MASTER_KEY_IDS, masterKeyIds);
        args.putBoolean(ARG_EXPORT_SECRET, exportSecret);
        args.putBoolean(ARG_EXECUTE_BACKUP_OPERATION, executeBackupOperation);
        args.putParcelable(ARG_PARCELABLE_PASSPHRASES, ParcelableHashMap.toParcelableHashMap(passphrases));
        frag.setArguments(args);

        return frag;
    }

    enum BackupCodeState {
        STATE_UNINITIALIZED, STATE_DISPLAY, STATE_INPUT, STATE_INPUT_ERROR, STATE_OK
    }

    BackupCodeState mCurrentState = BackupCodeState.STATE_UNINITIALIZED;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Constants.DEBUG) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (Constants.DEBUG) {
            inflater.inflate(R.menu.backup_fragment_debug_menu, menu);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (Constants.DEBUG && item.getItemId() == R.id.debug_accept_any_log) {
            boolean newCheckedState = !item.isChecked();
            item.setChecked(newCheckedState);
            mDebugModeAcceptAnyCode = newCheckedState;
            if (newCheckedState && TextUtils.isEmpty(mCodeEditText[0].getText())) {
                mCodeEditText[0].setText("ABCD");
                mCodeEditText[1].setText("EFGH");
                mCodeEditText[2].setText("IJKL");
                mCodeEditText[3].setText("MNOP");
                mCodeEditText[4].setText("QRST");
                mCodeEditText[5].setText("UVWX");
                Notify.create(getActivity(), "Actual backup code is all 'A's", Style.WARN).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

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
                mCodeFieldsAnimator.setDisplayedChild(1, false);
                if (mExecuteBackupOperation) {
                    mStatusAnimator.setDisplayedChild(3, animate);
                } else {
                    mStatusAnimator.setDisplayedChild(1, animate);
                }

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

                // special case for remote API, see RemoteBackupActivity
                if (!mExecuteBackupOperation) {
                    // wait for animation to finish...
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startBackup();
                        }
                    }, 2000);
                }

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
        mExecuteBackupOperation = args.getBoolean(ARG_EXECUTE_BACKUP_OPERATION, true);
        ParcelableHashMap<ParcelableLong, Passphrase> parcelablePassphrases = args.getParcelable(ARG_PARCELABLE_PASSPHRASES);
        mPassphrases = ParcelableHashMap.toHashMap(parcelablePassphrases);

        mCodeEditText = new EditText[6];
        mCodeEditText[0] = (EditText) view.findViewById(R.id.backup_code_1);
        mCodeEditText[1] = (EditText) view.findViewById(R.id.backup_code_2);
        mCodeEditText[2] = (EditText) view.findViewById(R.id.backup_code_3);
        mCodeEditText[3] = (EditText) view.findViewById(R.id.backup_code_4);
        mCodeEditText[4] = (EditText) view.findViewById(R.id.backup_code_5);
        mCodeEditText[5] = (EditText) view.findViewById(R.id.backup_code_6);

        {
            TextView[] codeDisplayText = new TextView[6];
            codeDisplayText[0] = (TextView) view.findViewById(R.id.backup_code_display_1);
            codeDisplayText[1] = (TextView) view.findViewById(R.id.backup_code_display_2);
            codeDisplayText[2] = (TextView) view.findViewById(R.id.backup_code_display_3);
            codeDisplayText[3] = (TextView) view.findViewById(R.id.backup_code_display_4);
            codeDisplayText[4] = (TextView) view.findViewById(R.id.backup_code_display_5);
            codeDisplayText[5] = (TextView) view.findViewById(R.id.backup_code_display_6);

            // set backup code in code TextViews
            char[] backupCode = mBackupCode.toCharArray();
            for (int i = 0; i < codeDisplayText.length; i++) {
                codeDisplayText[i].setText(backupCode, i * 5, 4);
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

        view.findViewById(R.id.button_faq).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showFaq();
            }
        });
        return view;
    }

    private void showFaq() {
        HelpActivity.startHelpActivity(getActivity(), HelpActivity.TAB_FAQ);
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
                    if (s.length() > 4) {
                        throw new AssertionError("max length of each field is 4!");
                    }

                    boolean inInputState = mCurrentState == BackupCodeState.STATE_INPUT
                            || mCurrentState == BackupCodeState.STATE_INPUT_ERROR;
                    boolean partIsComplete = s.length() == 4;
                    if (!inInputState || !partIsComplete) {
                        return;
                    }

                    checkIfCodeIsCorrect();
                }
            });

        }
    }

    private void checkIfCodeIsCorrect() {

        if (Constants.DEBUG && mDebugModeAcceptAnyCode) {
            switchState(BackupCodeState.STATE_OK, true);
            return;
        }

        StringBuilder backupCodeInput = new StringBuilder(26);
        for (EditText editText : mCodeEditText) {
            if (editText.getText().length() < 4) {
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
                    boolean cursorAtEnd = (start + count) == 4;

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

        Passphrase passphrase = new Passphrase(mBackupCode);
        if (Constants.DEBUG && mDebugModeAcceptAnyCode) {
            passphrase = new Passphrase("AAAA-AAAA-AAAA-AAAA-AAAA-AAAA");
        }

        // if we don't want to execute the actual operation outside of this activity, drop out here
        if (!mExecuteBackupOperation) {
            ((BackupActivity) getActivity()).handleBackupOperation(passphrase, mPassphrases);
            return;
        }

        if (mCachedBackupUri == null) {
            mCachedBackupUri = TemporaryFileProvider.createFile(activity, filename,
                    Constants.MIME_TYPE_ENCRYPTED_ALTERNATE);

            cryptoOperation(new CryptoInputParcel(passphrase));
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
        return new BackupKeyringParcel(mMasterKeyIds,
                mExportSecret, true, mCachedBackupUri,
                ParcelableHashMap.toParcelableHashMap(mPassphrases));
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

    /**
     * Generate backup code using format defined in
     * https://github.com/open-keychain/open-keychain/wiki/Backups
     */
    @NonNull
    private static String generateRandomBackupCode() {

        Random r = new SecureRandom();

        // simple generation of a 24 character backup code
        StringBuilder code = new StringBuilder(28);
        for (int i = 0; i < 24; i++) {
            if (i == 4 || i == 8 || i == 12 || i == 16 || i == 20) {
                code.append('-');
            }

            code.append(mBackupCodeAlphabet[r.nextInt(mBackupCodeAlphabet.length)]);
        }

        return code.toString();
    }


}
