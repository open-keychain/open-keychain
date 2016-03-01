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
import android.annotation.SuppressLint;
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
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.github.pinball83.maskededittext.MaskedEditText;

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

    private MaskedEditText mCodeEditText;

    private ToolableViewAnimator mStatusAnimator, mTitleAnimator, mCodeFieldsAnimator;
    private Integer mBackStackLevel;

    private Uri mCachedBackupUri;
    private boolean mShareNotSave;
    private boolean mDebugModeAcceptAnyCode;

    public static BackupCodeFragment newInstance(long[] masterKeyIds, boolean exportSecret) {
        BackupCodeFragment frag = new BackupCodeFragment();

        Bundle args = new Bundle();
        args.putString(ARG_BACKUP_CODE, generateRandomBackupCode());
        args.putLongArray(ARG_MASTER_KEY_IDS, masterKeyIds);
        args.putBoolean(ARG_EXPORT_SECRET, exportSecret);
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
            if (newCheckedState) {
                mCodeEditText.setText("ABCD-EFGH-IJKL-MNOP-QRST-UVWX");
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
                // use non-breaking spaces to enlarge the empty EditText appropriately
                String empty = "\u00a0\u00a0\u00a0\u00a0-\u00a0\u00a0\u00a0\u00a0" +
                        "-\u00a0\u00a0\u00a0\u00a0-\u00a0\u00a0\u00a0\u00a0" +
                        "-\u00a0\u00a0\u00a0\u00a0-\u00a0\u00a0\u00a0\u00a0";
                mCodeEditText.setText(empty);

                pushBackStackEntry();

                break;

            case STATE_INPUT_ERROR: {
                mTitleAnimator.setDisplayedChild(1, false);
                mStatusAnimator.setDisplayedChild(2, animate);
                mCodeFieldsAnimator.setDisplayedChild(1, false);

                hideKeyboard();

                if (animate) {
                    @ColorInt int black = mCodeEditText.getCurrentTextColor();
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

                mCodeEditText.setEnabled(false);

                @ColorInt int green = getResources().getColor(R.color.android_green_dark);
                if (animate) {
                    @ColorInt int black = mCodeEditText.getCurrentTextColor();
                    animateFlashText(mCodeEditText, black, green, true);
                } else {
                    mCodeEditText.setTextColor(green);
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

        // NOTE: order of these method calls matter, see setupAutomaticLinebreak()
        mCodeEditText = (MaskedEditText) view.findViewById(R.id.backup_code_input);
        mCodeEditText.setInputType(
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        setupAutomaticLinebreak(mCodeEditText);
        mCodeEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        setupEditTextSuccessListener(mCodeEditText);

        TextView codeDisplayText = (TextView) view.findViewById(R.id.backup_code_display);
        setupAutomaticLinebreak(codeDisplayText);

        // set background to null in TextViews - this will retain padding from EditText style!
        // noinspection deprecation, setBackground(Drawable) is API level >=16
        codeDisplayText.setBackgroundDrawable(null);

        codeDisplayText.setText(mBackupCode);

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

    /**
     * Automatic line break with max 6 lines for smaller displays
     * <p/>
     * NOTE: I was not able to get this behaviour using XML!
     * Looks like the order of these method calls matter, see http://stackoverflow.com/a/11171307
     */
    private void setupAutomaticLinebreak(TextView textview) {
        textview.setSingleLine(true);
        textview.setMaxLines(6);
        textview.setHorizontallyScrolling(false);
    }

    private void setupEditTextSuccessListener(final MaskedEditText backupCode) {
        backupCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String currentBackupCode = backupCode.getText().toString();
                boolean inInputState = mCurrentState == BackupCodeState.STATE_INPUT
                        || mCurrentState == BackupCodeState.STATE_INPUT_ERROR;
                boolean partIsComplete = (currentBackupCode.indexOf(' ') == -1)
                        && (currentBackupCode.indexOf('\u00a0') == -1);
                if (!inInputState || !partIsComplete) {
                    return;
                }

                checkIfCodeIsCorrect(currentBackupCode);
            }
        });
    }

    private void checkIfCodeIsCorrect(String currentBackupCode) {

        if (Constants.DEBUG && mDebugModeAcceptAnyCode) {
            switchState(BackupCodeState.STATE_OK, true);
            return;
        }

        if (currentBackupCode.equals(mBackupCode)) {
            switchState(BackupCodeState.STATE_OK, true);
            return;
        }

        switchState(BackupCodeState.STATE_INPUT_ERROR, true);
    }

    private static void animateFlashText(
            final TextView textView, int color1, int color2, boolean staySecondColor) {

        ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), color1, color2);
        anim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                textView.setTextColor((Integer) animator.getAnimatedValue());
            }
        });
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.setRepeatCount(staySecondColor ? 4 : 5);
        anim.setDuration(180);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.start();

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
        Passphrase passphrase = new Passphrase(mBackupCode);
        if (Constants.DEBUG && mDebugModeAcceptAnyCode) {
            passphrase = new Passphrase("AAAA-AAAA-AAAA-AAAA-AAAA-AAAA");
        }
        return new BackupKeyringParcel(passphrase, mMasterKeyIds, mExportSecret, mCachedBackupUri);
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
