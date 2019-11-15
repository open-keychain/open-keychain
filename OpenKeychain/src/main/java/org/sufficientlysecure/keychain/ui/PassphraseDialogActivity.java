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


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.daos.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel.RequiredInputType;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.ui.widget.CacheTTLSpinner;
import org.sufficientlysecure.keychain.ui.widget.PrefixedEditText;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


/**
 * We can not directly create a dialog on the application context.
 * This activity encapsulates a DialogFragment to emulate a dialog.
 * NOTE: If no CryptoInputParcel is passed via EXTRA_CRYPTO_INPUT, the CryptoInputParcel is created
 * internally and is NOT meant to be used by signing operations before adding a signature time
 */
public class PassphraseDialogActivity extends FragmentActivity {

    public static final String RESULT_CRYPTO_INPUT = "result_data";

    public static final String EXTRA_REQUIRED_INPUT = "required_input";
    public static final String EXTRA_CRYPTO_INPUT = "crypto_input";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // do not allow screenshots of passphrase input
        // to prevent "too easy" passphrase theft by root apps
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
            );
        }

        CryptoInputParcel cryptoInputParcel = getIntent().getParcelableExtra(EXTRA_CRYPTO_INPUT);
        if (cryptoInputParcel == null) {
            cryptoInputParcel = CryptoInputParcel.createCryptoInputParcel();
            getIntent().putExtra(EXTRA_CRYPTO_INPUT, cryptoInputParcel);
        }

        // this activity itself has no content view (see manifest)
        RequiredInputParcel requiredInput = getIntent().getParcelableExtra(EXTRA_REQUIRED_INPUT);
        if (requiredInput.mType != RequiredInputType.PASSPHRASE) {
            return;
        }

        // handle empty passphrases by directly returning an empty crypto input parcel
        try {
            KeyRepository keyRepository = KeyRepository.create(this);
            // use empty passphrase for empty passphrase
            if (keyRepository.getSecretKeyType(requiredInput.getSubKeyId()) == SecretKeyType.PASSPHRASE_EMPTY) {
                // also return passphrase back to activity
                Intent returnIntent = new Intent();
                cryptoInputParcel = cryptoInputParcel.withPassphrase(new Passphrase(""), requiredInput.getSubKeyId());
                returnIntent.putExtra(RESULT_CRYPTO_INPUT, cryptoInputParcel);
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        } catch (NotFoundException e) {
            Timber.e(e, "Key not found?!");
            setResult(RESULT_CANCELED);
            finish();
        }

    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        /* Show passphrase dialog to cache a new passphrase the user enters for using it later for
         * encryption. Based on mSecretKeyId it asks for a passphrase to open a private key or it asks
         * for a symmetric passphrase
         */
        PassphraseDialogFragment frag = new PassphraseDialogFragment();
        frag.setArguments(getIntent().getExtras());
        frag.show(getSupportFragmentManager(), "passphraseDialog");
    }

    @Override
    protected void onPause() {
        super.onPause();

        DialogFragment dialog = (DialogFragment) getSupportFragmentManager().findFragmentByTag("passphraseDialog");
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public static class PassphraseDialogFragment extends DialogFragment implements TextView.OnEditorActionListener {
        private EditText mPassphraseEditText;
        private TextView mPassphraseText;
        private EditText[] mBackupCodeEditText;

        private boolean mIsCancelled = false;
        private RequiredInputParcel mRequiredInput;

        private ViewAnimator mLayout;
        private CacheTTLSpinner mTimeToLiveSpinner;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();

            ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(activity);

            mRequiredInput = getArguments().getParcelable(EXTRA_REQUIRED_INPUT);

            CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);

            // No title, see http://www.google.com/design/spec/components/dialogs.html#dialogs-alerts
            //alert.setTitle()

            if (mRequiredInput.mType == RequiredInputType.BACKUP_CODE) {
                LayoutInflater inflater = LayoutInflater.from(theme);
                View view = inflater.inflate(R.layout.passphrase_dialog_backup_code, null);
                alert.setView(view);

                mBackupCodeEditText = new EditText[6];
                mBackupCodeEditText[0] = view.findViewById(R.id.backup_code_1);
                mBackupCodeEditText[1] = view.findViewById(R.id.backup_code_2);
                mBackupCodeEditText[2] = view.findViewById(R.id.backup_code_3);
                mBackupCodeEditText[3] = view.findViewById(R.id.backup_code_4);
                mBackupCodeEditText[4] = view.findViewById(R.id.backup_code_5);
                mBackupCodeEditText[5] = view.findViewById(R.id.backup_code_6);

                setupEditTextFocusNext(mBackupCodeEditText, false);

                AlertDialog dialog = alert.create();
                dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                        activity.getString(R.string.btn_unlock), (DialogInterface.OnClickListener) null);
                return dialog;
            }

            if (mRequiredInput.mType == RequiredInputType.NUMERIC_9X4 ||
                    mRequiredInput.mType == RequiredInputType.NUMERIC_9X4_AUTOCRYPT) {
                LayoutInflater inflater = LayoutInflater.from(theme);
                View view = inflater.inflate(R.layout.passphrase_dialog_numeric_9x4, null);
                alert.setView(view);

                mBackupCodeEditText = new EditText[9];
                mBackupCodeEditText[0] = view.findViewById(R.id.transfer_code_block_1);
                mBackupCodeEditText[1] = view.findViewById(R.id.transfer_code_block_2);
                mBackupCodeEditText[2] = view.findViewById(R.id.transfer_code_block_3);
                mBackupCodeEditText[3] = view.findViewById(R.id.transfer_code_block_4);
                mBackupCodeEditText[4] = view.findViewById(R.id.transfer_code_block_5);
                mBackupCodeEditText[5] = view.findViewById(R.id.transfer_code_block_6);
                mBackupCodeEditText[6] = view.findViewById(R.id.transfer_code_block_7);
                mBackupCodeEditText[7] = view.findViewById(R.id.transfer_code_block_8);
                mBackupCodeEditText[8] = view.findViewById(R.id.transfer_code_block_9);

                if (mRequiredInput.hasPassphraseBegin()) {
                    String beginChars = mRequiredInput.getPassphraseBegin();
                    int inputLength = 4 - beginChars.length();
                    setupEditTextFocusNext(mBackupCodeEditText, true);

                    PrefixedEditText prefixEditText = (PrefixedEditText) mBackupCodeEditText[0];
                    if (beginChars.matches("\\d\\d")) {
                        prefixEditText.setPrefix(beginChars);
                        prefixEditText.setHint("1234".substring(inputLength));
                        prefixEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(inputLength) });
                    }
                } else {
                    setupEditTextFocusNext(mBackupCodeEditText, false);
                }

                if (mRequiredInput.mType == RequiredInputType.NUMERIC_9X4_AUTOCRYPT) {
                    TextView promptText = view.findViewById(R.id.passphrase_text);
                    promptText.setText(R.string.passphrase_transfer_autocrypt);
                }

                AlertDialog dialog = alert.create();
                dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                        activity.getString(R.string.btn_proceed), (DialogInterface.OnClickListener) null);
                return dialog;
            }

            LayoutInflater inflater = LayoutInflater.from(theme);
            mLayout = (ViewAnimator) inflater.inflate(R.layout.passphrase_dialog, null);
            alert.setView(mLayout);

            mPassphraseText = mLayout.findViewById(R.id.passphrase_text);
            mPassphraseEditText = mLayout.findViewById(R.id.passphrase_passphrase);

            View vTimeToLiveLayout = mLayout.findViewById(R.id.remember_layout);
            vTimeToLiveLayout.setVisibility(mRequiredInput.mSkipCaching ? View.GONE : View.VISIBLE);

            mTimeToLiveSpinner = mLayout.findViewById(R.id.ttl_spinner);
            int ttlSeconds = Preferences.getPreferences(getContext()).getCacheTtlSeconds();
            mTimeToLiveSpinner.setSelectedTimeToLive(ttlSeconds);

            alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

            String userId;
            CanonicalizedSecretKey.SecretKeyType keyType = CanonicalizedSecretKey.SecretKeyType.PASSPHRASE;

            String message;
            String hint;
            if (mRequiredInput.mType == RequiredInputType.PASSPHRASE_SYMMETRIC) {
                message = getString(R.string.passphrase_for_symmetric_encryption);
                hint = getString(R.string.label_passphrase);
            } else {
                try {
                    long[] subKeyIds = mRequiredInput.getSubKeyIds();
                    if (subKeyIds.length > 1) {
                        message = getString(R.string.passphrase_for_any);
                        hint = getString(R.string.label_passphrase);
                    } else {
                        long subKeyId = subKeyIds[0];

                        KeyRepository keyRepository = KeyRepository.create(getContext());
                        Long masterKeyId = keyRepository.getMasterKeyIdBySubkeyId(subKeyId);
                        UnifiedKeyInfo unifiedKeyInfo = keyRepository.getUnifiedKeyInfo(masterKeyId);
                        if (unifiedKeyInfo == null) {
                            throw new NotFoundException();
                        }
                        // yes the inner try/catch block is necessary, otherwise the final variable
                        // above can't be statically verified to have been set in all cases because
                        // the catch clause doesn't return.
                        String mainUserId = unifiedKeyInfo.user_id();
                        OpenPgpUtils.UserId mainUserIdSplit = KeyRing.splitUserId(mainUserId);
                        if (mainUserIdSplit.name != null) {
                            userId = mainUserIdSplit.name;
                        } else {
                            userId = getString(R.string.user_id_no_name);
                        }

                        keyType = keyRepository.getSecretKeyType(subKeyId);
                        switch (keyType) {
                            case PASSPHRASE:
                                message = getString(R.string.passphrase_for, userId);
                                hint = getString(R.string.label_passphrase);
                                break;
                            case DIVERT_TO_CARD:
                                message = getString(R.string.security_token_pin_for, userId);
                                hint = getString(R.string.label_pin);
                                break;
                            // special case: empty passphrase just returns the empty passphrase
                            case PASSPHRASE_EMPTY:
                                finishCaching(new Passphrase(""), subKeyId);
                            default:
                                throw new AssertionError("Unhandled SecretKeyType (should not happen)");
                        }
                    }
                } catch (NotFoundException e) {
                    alert.setTitle(R.string.title_key_not_found);
                    alert.setMessage(getString(R.string.key_not_found, mRequiredInput.getSubKeyId()));
                    alert.setPositiveButton(android.R.string.ok, (dialog, which) -> dismiss());
                    alert.setCancelable(false);
                    return alert.create();
                }
            }

            mPassphraseText.setText(message);
            mPassphraseEditText.setHint(hint);

            openKeyboard(mPassphraseEditText);

            mPassphraseEditText.setImeActionLabel(getString(android.R.string.ok), EditorInfo.IME_ACTION_DONE);
            mPassphraseEditText.setOnEditorActionListener(this);

            final ImageButton keyboard = mLayout.findViewById(R.id.passphrase_keyboard);

            if (keyType == CanonicalizedSecretKey.SecretKeyType.DIVERT_TO_CARD) {
                if (Preferences.getPreferences(activity).useNumKeypadForSecurityTokenPin()) {
                    setKeyboardNumeric(keyboard);
                } else {
                    setKeyboardAlphabetic(keyboard);
                }

                keyboard.setVisibility(View.VISIBLE);
                keyboard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Preferences prefs = Preferences.getPreferences(activity);
                        if (prefs.useNumKeypadForSecurityTokenPin()) {
                            prefs.setUseNumKeypadForSecurityTokenPin(false);

                            setKeyboardAlphabetic(keyboard);
                        } else {
                            prefs.setUseNumKeypadForSecurityTokenPin(true);

                            setKeyboardNumeric(keyboard);
                        }
                    }
                });

            } else {
                mPassphraseEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                keyboard.setVisibility(View.GONE);
            }

            mPassphraseEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());

            AlertDialog dialog = alert.create();
            dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    activity.getString(R.string.btn_unlock), (DialogInterface.OnClickListener) null);

            return dialog;
        }

        private void setKeyboardNumeric(ImageButton keyboard) {
            mPassphraseEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            // somehow InputType.TYPE_TEXT_VARIATION_PASSWORD is not enough...
            mPassphraseEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            keyboard.setImageResource(R.drawable.ic_alphabetical_black_24dp);
            keyboard.setContentDescription(getString(R.string.passphrase_keyboard_hint_alpha));
        }

        private void setKeyboardAlphabetic(ImageButton keyboard) {
            mPassphraseEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            // somehow InputType.TYPE_TEXT_VARIATION_PASSWORD is not enough...
            mPassphraseEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            keyboard.setImageResource(R.drawable.ic_numeric_black_24dp);
            keyboard.setContentDescription(getString(R.string.passphrase_keyboard_hint_numeric));
        }

        /**
         * Hack to open keyboard.
         * This is the only method that I found to work across all Android versions
         * http://turbomanage.wordpress.com/2012/05/02/show-soft-keyboard-automatically-when-edittext-receives-focus/
         * Notes:
         * * onCreateView can't be used because we want to add buttons to the dialog
         * * opening in onActivityCreated does not work on Android 4.4
         */
        private void openKeyboard(final TextView textView) {
            textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    textView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() == null || textView == null) {
                                return;
                            }
                            InputMethodManager imm = (InputMethodManager) getActivity()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(textView, InputMethodManager.SHOW_IMPLICIT);
                        }
                    });
                }
            });
            textView.requestFocus();
        }

        private static void setupEditTextFocusNext(EditText[] backupCodes, boolean hasPrefix) {
            for (int i = 0; i < backupCodes.length - 1; i++) {
                int idx = i;

                backupCodes[i].addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        boolean inserting = before < count;
                        int maxLen = hasPrefix && idx == 0 ? 2 : 4;
                        boolean cursorAtEnd = (start + count) == maxLen;

                        if (inserting && cursorAtEnd) {
                            backupCodes[idx + 1].requestFocus();
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });
            }
        }

        @Override
        public void onStart() {
            super.onStart();

            // Override the default behavior so the dialog is NOT dismissed on click
            final Button positive = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (mRequiredInput.mType == RequiredInputType.BACKUP_CODE) {
                        StringBuilder backupCodeInput = new StringBuilder(26);
                        for (EditText editText : mBackupCodeEditText) {
                            if (editText.getText().length() < 4) {
                                return;
                            }
                            backupCodeInput.append(editText.getText());
                            backupCodeInput.append('-');
                        }
                        backupCodeInput.deleteCharAt(backupCodeInput.length() - 1);

                        Passphrase passphrase = new Passphrase(backupCodeInput.toString());
                        finishCaching(passphrase, null);

                        return;
                    }

                    if (mRequiredInput.mType == RequiredInputType.NUMERIC_9X4 ||
                            mRequiredInput.mType == RequiredInputType.NUMERIC_9X4_AUTOCRYPT) {
                        StringBuilder backupCodeInput = new StringBuilder(36);
                        if (mRequiredInput.hasPassphraseBegin()) {
                            backupCodeInput.append(mRequiredInput.getPassphraseBegin());
                        }
                        for (EditText editText : mBackupCodeEditText) {
                            if (editText.getText().length() != 2 && editText.getText().length() != 4) {
                                return;
                            }
                            backupCodeInput.append(editText.getText());
                            backupCodeInput.append('-');
                        }
                        backupCodeInput.deleteCharAt(backupCodeInput.length() - 1);

                        Passphrase passphrase = new Passphrase(backupCodeInput.toString());
                        finishCaching(passphrase, null);

                        return;
                    }

                    final Passphrase passphrase = new Passphrase(mPassphraseEditText);
                    final int timeToLiveSeconds = mTimeToLiveSpinner.getSelectedTimeToLive();

                    Preferences.getPreferences(getContext()).setCacheTtlSeconds(timeToLiveSeconds);

                    // Early breakout if we are dealing with a symmetric key
                    if (mRequiredInput.mType == RequiredInputType.PASSPHRASE_SYMMETRIC) {
                        if (!mRequiredInput.mSkipCaching) {
                            PassphraseCacheService.addCachedPassphrase(getActivity(),
                                    Constants.key.symmetric, Constants.key.symmetric, passphrase,
                                    getString(R.string.passp_cache_notif_pwd), timeToLiveSeconds);
                        }

                        finishCaching(passphrase, null);
                        return;
                    }

                    checkPassphraseAndFinishCaching(positive, passphrase, timeToLiveSeconds);
                }

            });
        }

        private void checkPassphraseAndFinishCaching(final Button positive, final Passphrase passphrase,
                final int timeToLiveSeconds) {
            mLayout.setDisplayedChild(1);
            positive.setEnabled(false);

            new AsyncTask<Void, Void, CanonicalizedSecretKey>() {

                @Override
                protected CanonicalizedSecretKey doInBackground(Void... params) {
                    try {
                        long timeBeforeOperation = SystemClock.elapsedRealtime();

                        CanonicalizedSecretKey canonicalizedSecretKey = null;
                        for (long subKeyId : mRequiredInput.getSubKeyIds()) {
                            KeyRepository keyRepository = KeyRepository.create(getContext());
                            Long masterKeyId = keyRepository.getMasterKeyIdBySubkeyId(subKeyId);
                            if (masterKeyId == null) {
                                continue;
                            }
                            CanonicalizedSecretKeyRing secretKeyRing = keyRepository.getCanonicalizedSecretKeyRing(masterKeyId);
                            CanonicalizedSecretKey secretKeyToUnlock = secretKeyRing.getSecretKey(subKeyId);

                            // this is the operation may take a very long time (100ms to several seconds!)
                            boolean unlockSucceeded = secretKeyToUnlock.unlock(passphrase);
                            if (unlockSucceeded) {
                                canonicalizedSecretKey = secretKeyToUnlock;
                            }
                        }

                        // if it didn't take that long, give the user time to appreciate the progress bar
                        long operationTime = SystemClock.elapsedRealtime() - timeBeforeOperation;
                        if (operationTime < 100) {
                            try {
                                Thread.sleep(100 - operationTime);
                            } catch (InterruptedException e) {
                                // ignore
                            }
                        }

                        return canonicalizedSecretKey;
                    } catch (NotFoundException | PgpGeneralException e) {
                        Toast.makeText(getActivity(), R.string.error_could_not_extract_private_key,
                                Toast.LENGTH_SHORT).show();

                        getActivity().setResult(RESULT_CANCELED);
                        dismiss();
                        getActivity().finish();
                        return null;
                    }
                }

                /** Handle a good or bad passphrase. This happens in the UI thread!  */
                @Override
                protected void onPostExecute(CanonicalizedSecretKey unlockedKey) {
                    super.onPostExecute(unlockedKey);

                    // if we were cancelled in the meantime, the result isn't relevant anymore
                    if (mIsCancelled) {
                        return;
                    }

                    // if the passphrase was wrong, reset and re-enable the dialogue
                    if (unlockedKey == null) {
                        mPassphraseEditText.setText("");
                        mPassphraseEditText.setError(getString(R.string.wrong_passphrase));
                        mLayout.setDisplayedChild(0);
                        positive.setEnabled(true);
                        return;
                    }

                    // cache the new passphrase as specified in CryptoInputParcel
                    Timber.d("Everything okay!");

                    if (mRequiredInput.mSkipCaching) {
                        Timber.d("Not caching entered passphrase!");
                    } else {
                        Timber.d("Caching entered passphrase");

                        PassphraseCacheService.addCachedPassphrase(getActivity(),
                                unlockedKey.getRing().getMasterKeyId(), unlockedKey.getKeyId(), passphrase,
                                unlockedKey.getRing().getPrimaryUserIdWithFallback(), timeToLiveSeconds);
                    }

                    finishCaching(passphrase, unlockedKey.getKeyId());
                }
            }.execute();
        }

        private void finishCaching(Passphrase passphrase, Long subKeyId) {
            // any indication this isn't needed anymore, don't do it.
            if (mIsCancelled || getActivity() == null) {
                return;
            }

            CryptoInputParcel inputParcel = getArguments().getParcelable(EXTRA_CRYPTO_INPUT);
            // noinspection ConstantConditions, we handle the non-null case in PassphraseDialogActivity.onCreate()
            inputParcel = inputParcel.withPassphrase(passphrase, subKeyId);

            ((PassphraseDialogActivity) getActivity()).handleResult(inputParcel);

            dismiss();
            getActivity().finish();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);

            // note we need no synchronization here, this variable is only accessed in the ui thread
            mIsCancelled = true;

            getActivity().setResult(RESULT_CANCELED);
            getActivity().finish();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            hideKeyboard();
        }

        private void hideKeyboard() {
            if (getActivity() == null) {
                return;
            }

            InputMethodManager inputManager = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);

            inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // Associate the "done" button on the soft keyboard with the okay button in the view
            // and the Enter Key, in case a hard keyboard is used
            if (EditorInfo.IME_ACTION_DONE == actionId ||
                    (actionId == EditorInfo.IME_ACTION_UNSPECIFIED &&
                            event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                AlertDialog dialog = ((AlertDialog) getDialog());
                Button bt = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

                bt.performClick();
                return true;
            }
            return false;
        }

    }

    /**
     * Defines how the result of this activity is returned.
     * Is overwritten in RemotePassphraseDialogActivity
     */
    protected void handleResult(CryptoInputParcel inputParcel) {
        // also return passphrase back to activity
        Intent returnIntent = new Intent();
        returnIntent.putExtra(RESULT_CRYPTO_INPUT, inputParcel);
        setResult(RESULT_OK, returnIntent);
    }

}
