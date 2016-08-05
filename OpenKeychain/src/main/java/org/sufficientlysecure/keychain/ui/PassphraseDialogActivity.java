/*
 * Copyright (C) 2014-2016 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
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
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing.SecretKeyRingType;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel.RequiredInputType;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.ui.widget.CacheTTLSpinner;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.IOException;

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
    public static final String EXTRA_PASSPHRASE_TO_TRY = "passphrase_to_try";

    private static final String FRAGMENT_TAG = "passphrase_dialog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // do not allow screenshots of passphrase input
        // to prevent "too easy" passphrase theft by root apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
            );
        }

        RequiredInputParcel requiredInput = getIntent().getParcelableExtra(EXTRA_REQUIRED_INPUT);
        CryptoInputParcel cryptoInputParcel = getIntent().getParcelableExtra(EXTRA_CRYPTO_INPUT);
        if (cryptoInputParcel == null) {
            cryptoInputParcel = new CryptoInputParcel();
            getIntent().putExtra(EXTRA_CRYPTO_INPUT, cryptoInputParcel);
        }

        // blocks key usage before migration is completed
        boolean usingS2k = Preferences.getPreferences(this).isUsingS2k();
        if (requiredInput.mType != RequiredInputType.PASSPHRASE_IMPORT_KEY && usingS2k) {
            setResult(RESULT_CANCELED);
            finish();
        }

        // directly return an empty passphrase if appropriate
        try {
            switch (requiredInput.mType) {
                case PASSPHRASE_IMPORT_KEY:
                case PASSPHRASE_SYMMETRIC:
                case BACKUP_CODE:
                    return;
                case PASSPHRASE_KEYRING_UNLOCK: {
                    CachedPublicKeyRing pubRing =
                            new ProviderHelper(this).getCachedPublicKeyRing(requiredInput.getMasterKeyId());
                    if (pubRing.getSecretKeyringType() == SecretKeyRingType.PASSPHRASE_EMPTY) {
                        returnWithEmptyPassphrase(cryptoInputParcel);
                    }
                    return;
                }
                case PASSPHRASE_SUBKEY_UNLOCK: {
                    if (!requiredInput.hasKeyringPassphrase()) {
                        throw new AssertionError("No keyring passphrase passed! (Should not happen)");
                    }
                    CachedPublicKeyRing pubRing =
                            new ProviderHelper(this).getCachedPublicKeyRing(requiredInput.getMasterKeyId());
                    if (pubRing.getSecretKeyType(requiredInput.getSubKeyId()) == SecretKeyType.PASSPHRASE_EMPTY) {
                        returnWithEmptyPassphrase(cryptoInputParcel);
                    }
                    return;
                }
                default: {
                    throw new AssertionError("Unhandled input type! (Should not happen)");
                }
            }
        } catch (ProviderHelper.NotFoundException e) {
            Log.e(Constants.TAG, "Key not found?!", e);
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void returnWithEmptyPassphrase(CryptoInputParcel cryptoInput) {
        Intent returnIntent = new Intent();
        cryptoInput.mPassphrase = new Passphrase("");
        returnIntent.putExtra(RESULT_CRYPTO_INPUT, cryptoInput);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        /* Show passphrase dialog to cache a new passphrase the user enters for using it later for
         * encryption. Based on the required input type, it asks for a passphrase for either
         * a keyring block, a subkey, or for a symmetric passphrase
         */
        PassphraseDialogFragment frag = new PassphraseDialogFragment();
        frag.setArguments(getIntent().getExtras());
        frag.show(getSupportFragmentManager(), FRAGMENT_TAG);
    }

    @Override
    protected void onPause() {
        super.onPause();

        DialogFragment dialog = (DialogFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
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

        private CanonicalizedSecretKey mSecretKeyToUnlock;

        // for importing keys
        private Passphrase mPassphraseToTry;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();

            ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(activity);

            mRequiredInput = getArguments().getParcelable(EXTRA_REQUIRED_INPUT);
            mPassphraseToTry = getArguments().getParcelable(EXTRA_PASSPHRASE_TO_TRY);

            CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);

            // No title, see http://www.google.com/design/spec/components/dialogs.html#dialogs-alerts
            //alert.setTitle()

            if (mRequiredInput.mType == RequiredInputType.BACKUP_CODE) {
                LayoutInflater inflater = LayoutInflater.from(theme);
                View view = inflater.inflate(R.layout.passphrase_dialog_backup_code, null);
                alert.setView(view);

                mBackupCodeEditText = new EditText[6];
                mBackupCodeEditText[0] = (EditText) view.findViewById(R.id.backup_code_1);
                mBackupCodeEditText[1] = (EditText) view.findViewById(R.id.backup_code_2);
                mBackupCodeEditText[2] = (EditText) view.findViewById(R.id.backup_code_3);
                mBackupCodeEditText[3] = (EditText) view.findViewById(R.id.backup_code_4);
                mBackupCodeEditText[4] = (EditText) view.findViewById(R.id.backup_code_5);
                mBackupCodeEditText[5] = (EditText) view.findViewById(R.id.backup_code_6);

                setupEditTextFocusNext(mBackupCodeEditText);

                AlertDialog dialog = alert.create();
                dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                        activity.getString(R.string.btn_unlock), (DialogInterface.OnClickListener) null);
                return dialog;
            }

            LayoutInflater inflater = LayoutInflater.from(theme);
            mLayout = (ViewAnimator) inflater.inflate(R.layout.passphrase_dialog, null);
            alert.setView(mLayout);

            mPassphraseText = (TextView) mLayout.findViewById(R.id.passphrase_text);
            mPassphraseEditText = (EditText) mLayout.findViewById(R.id.passphrase_passphrase);

            // we hide details about caching if passphrases for this key are already cached
            boolean alreadyCached =
                    mRequiredInput.mType == RequiredInputType.PASSPHRASE_SUBKEY_UNLOCK &&
                    !mRequiredInput.getKeyringPassphrase().isEmpty();
            boolean hideCacheDurationLayout = mRequiredInput.mSkipCaching || alreadyCached;
            View vTimeToLiveLayout = mLayout.findViewById(R.id.remember_layout);
            vTimeToLiveLayout.setVisibility(hideCacheDurationLayout ? View.GONE : View.VISIBLE);

            mTimeToLiveSpinner = (CacheTTLSpinner) mLayout.findViewById(R.id.ttl_spinner);

            alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

            SecretKeyRingType keyRingType = SecretKeyRingType.PASSPHRASE;
            String message;
            String hint;
            ProviderHelper helper = new ProviderHelper(activity);

            try {
                switch (mRequiredInput.mType) {
                    case PASSPHRASE_SYMMETRIC: {
                        message = getString(R.string.passphrase_for_symmetric_encryption);
                        hint = getString(R.string.label_passphrase);
                        break;

                    }
                    case PASSPHRASE_KEYRING_UNLOCK: {
                        CachedPublicKeyRing cachedPublicKeyRing = helper.getCachedPublicKeyRing(
                                KeychainContract.KeyRings.buildUnifiedKeyRingUri(mRequiredInput.getMasterKeyId()));
                        // no need to set mSecretKeyToUnlock, as we are unlocking a keyring

                        String mainUserId = cachedPublicKeyRing.getPrimaryUserIdWithFallback();
                        OpenPgpUtils.UserId mainUserIdSplit = KeyRing.splitUserId(mainUserId);
                        String userId = (mainUserIdSplit.name == null)
                                ? getString(R.string.user_id_no_name)
                                : mainUserIdSplit.name;
                        message = getString(R.string.passphrase_for, userId);
                        hint = getString(R.string.label_passphrase);
                        break;

                    }
                    case PASSPHRASE_IMPORT_KEY: {
                        UncachedKeyRing uKeyRing =
                                UncachedKeyRing.decodeFromData(mRequiredInput.getParcelableKeyRing().mBytes);
                        CanonicalizedSecretKeyRing canonicalizedKeyRing = (CanonicalizedSecretKeyRing)
                                uKeyRing.canonicalize(new OperationResult.OperationLog(), 0);
                        mSecretKeyToUnlock = canonicalizedKeyRing.getSecretKey(mRequiredInput.getSubKeyId());

                        // don't need passphrase for dummy keys
                        if (mSecretKeyToUnlock.isDummy()) {
                            returnWithPassphrase(null);
                        }

                        String mainUserId = canonicalizedKeyRing.getPrimaryUserIdWithFallback();
                        OpenPgpUtils.UserId mainUserIdSplit = KeyRing.splitUserId(mainUserId);
                        String userId = (mainUserIdSplit.name == null)
                                ? getString(R.string.user_id_no_name)
                                : mainUserIdSplit.name;

                        message = getString(R.string.passphrase_for, userId);
                        hint = getString(R.string.label_passphrase);
                        break;

                    }
                    case PASSPHRASE_SUBKEY_UNLOCK: {

                        CanonicalizedSecretKeyRing secretKeyRing = helper.getCanonicalizedSecretKeyRing(
                                mRequiredInput.getMasterKeyId(),
                                mRequiredInput.getKeyringPassphrase()
                        );
                        long subKeyId = mRequiredInput.getSubKeyId();
                        mSecretKeyToUnlock = secretKeyRing.getSecretKey(subKeyId);

                        // cached key operations are less expensive
                        CachedPublicKeyRing cachedPublicKeyRing = helper.getCachedPublicKeyRing(
                                KeychainContract.KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(subKeyId));
                        String mainUserId = cachedPublicKeyRing.getPrimaryUserIdWithFallback();
                        OpenPgpUtils.UserId mainUserIdSplit = KeyRing.splitUserId(mainUserId);
                        String userId = (mainUserIdSplit.name == null)
                                ? getString(R.string.user_id_no_name)
                                : mainUserIdSplit.name;

                        SecretKeyType keyType = cachedPublicKeyRing.getSecretKeyType(subKeyId);
                        switch (keyType) {
                            case DIVERT_TO_CARD:
                                message = getString(R.string.security_token_pin_for, userId);
                                hint = getString(R.string.label_pin);
                                break;
                            default:
                                throw new AssertionError("Unhandled SecretKeyType (should not happen)");
                        }
                        break;

                    }
                    default: {
                        throw new AssertionError("Unknown RequiredInputParcel type (should not happen)");
                    }

                }
            } catch (ByteArrayEncryptor.IncorrectPassphraseException |
                    ByteArrayEncryptor.EncryptDecryptException e) {
                throw new AssertionError("Cannot decrypt || Given passphrase is wrong (programming error)");
            } catch (IOException | PgpGeneralException | PgpKeyNotFoundException |
                    ProviderHelper.NotFoundException e) {
                mLayout.setVisibility(View.GONE);
                alert.setTitle(R.string.title_key_not_found);
                alert.setMessage(getString(R.string.key_not_found, mRequiredInput.getSubKeyId()));
                alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                });
                // hide the positive button whose OnClickListener is overridden in onStart()
                alert.setPositiveButton(null, null);
                alert.setCancelable(false);
                return alert.create();
            }

            mPassphraseText.setText(message);
            mPassphraseEditText.setHint(hint);

            openKeyboard(mPassphraseEditText);

            mPassphraseEditText.setImeActionLabel(getString(android.R.string.ok), EditorInfo.IME_ACTION_DONE);
            mPassphraseEditText.setOnEditorActionListener(this);

            final ImageButton keyboard = (ImageButton) mLayout.findViewById(R.id.passphrase_keyboard);

            // TODO: wip, set this up after adding keyring type to db

            if (mRequiredInput.mType == RequiredInputType.PASSPHRASE_SUBKEY_UNLOCK) {
                if (Preferences.getPreferences(activity).useNumKeypadForSecurityTokenPin()) {
                    mPassphraseEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    keyboard.setImageResource(R.drawable.ic_alphabetical_black_24dp);
                    keyboard.setContentDescription(getString(R.string.passphrase_keyboard_hint_alpha));
                } else {
                    mPassphraseEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    keyboard.setImageResource(R.drawable.ic_numeric_black_24dp);
                    keyboard.setContentDescription(getString(R.string.passphrase_keyboard_hint_numeric));
                }

                keyboard.setVisibility(View.VISIBLE);
                keyboard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Preferences prefs = Preferences.getPreferences(activity);
                        if (prefs.useNumKeypadForSecurityTokenPin()) {
                            prefs.setUseNumKeypadForSecurityTokenPin(false);

                            mPassphraseEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            keyboard.setImageResource(R.drawable.ic_numeric_black_24dp);
                            keyboard.setContentDescription(getString(R.string.passphrase_keyboard_hint_alpha));
                        } else {
                            prefs.setUseNumKeypadForSecurityTokenPin(true);

                            mPassphraseEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                            keyboard.setImageResource(R.drawable.ic_alphabetical_black_24dp);
                            keyboard.setContentDescription(getString(R.string.passphrase_keyboard_hint_numeric));
                        }
                    }
                });
            } else if (keyRingType == CanonicalizedSecretKeyRing.SecretKeyRingType.PIN) {
                mPassphraseEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                keyboard.setVisibility(View.GONE);
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
                        returnWithPassphrase(passphrase);

                        return;
                    }

                    final Passphrase passphrase = new Passphrase(mPassphraseEditText);
                    final int timeToLiveSeconds = mTimeToLiveSpinner.getSelectedTimeToLive();

                    // Early breakout if we are dealing with a symmetric key
                    if (mRequiredInput.mType == RequiredInputType.PASSPHRASE_SYMMETRIC) {
                        if (!mRequiredInput.mSkipCaching) {
                            PassphraseCacheService.addCachedPassphrase(getActivity(),
                                    Constants.key.symmetric, passphrase,
                                    getString(R.string.passp_cache_notif_pwd), timeToLiveSeconds);
                        }

                        returnWithPassphrase(passphrase);
                        return;
                    }

                    mLayout.setDisplayedChild(1);
                    positive.setEnabled(false);

                    new AsyncTask<Void, Void, CanonicalizedSecretKey>() {

                        @Override
                        protected CanonicalizedSecretKey doInBackground(Void... params) {
                            try {
                                // unlocking may take a very long time (100ms to several seconds!)
                                long timeBeforeOperation = System.currentTimeMillis();
                                boolean unlockSucceeded;
                                ProviderHelper helper = new ProviderHelper(getActivity());

                                switch (mRequiredInput.mType) {
                                    case PASSPHRASE_KEYRING_UNLOCK: {
                                        try {
                                            CanonicalizedSecretKeyRing secretKeyRing =
                                                    helper.getCanonicalizedSecretKeyRing(
                                                            mRequiredInput.getMasterKeyId(),
                                                            passphrase
                                                    );
                                            unlockSucceeded = true;
                                            // required to indicate a successful unlock
                                            mSecretKeyToUnlock = secretKeyRing.getSecretKey(mRequiredInput.getMasterKeyId());
                                        } catch (ByteArrayEncryptor.IncorrectPassphraseException e) {
                                            unlockSucceeded = false;
                                        }
                                        break;
                                    }
                                    case PASSPHRASE_IMPORT_KEY:
                                    case PASSPHRASE_SUBKEY_UNLOCK: {
                                        unlockSucceeded = mSecretKeyToUnlock.unlock(passphrase);
                                        break;
                                    }
                                    default: {
                                        throw new AssertionError("Unhandled RequiredInputParcelType! (should not happen)");
                                    }
                                }

                                // if it didn't take that long, give the user time to appreciate the progress bar
                                long operationTime = System.currentTimeMillis() - timeBeforeOperation;
                                if (operationTime < 100) {
                                    try {
                                        Thread.sleep(100 - operationTime);
                                    } catch (InterruptedException e) {
                                        // ignore
                                    }
                                }

                                return unlockSucceeded ? mSecretKeyToUnlock : null;
                            } catch (PgpGeneralException | ProviderHelper.NotFoundException |
                                    ByteArrayEncryptor.EncryptDecryptException e) {
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
                        protected void onPostExecute(CanonicalizedSecretKey result) {
                            super.onPostExecute(result);

                            // if we were cancelled in the meantime, the result isn't relevant anymore
                            if (mIsCancelled | getActivity() == null) {
                                return;
                            }

                            // if the passphrase was wrong, reset and re-enable the dialogue
                            if (result == null) {
                                mPassphraseEditText.setText("");
                                mPassphraseEditText.setError(getString(R.string.wrong_passphrase));
                                mLayout.setDisplayedChild(0);
                                positive.setEnabled(true);
                                return;
                            }

                            // cache the new passphrase as specified in CryptoInputParcel
                            Log.d(Constants.TAG, "Everything okay!");

                            try {
                                if (mRequiredInput.mSkipCaching) {
                                    Log.d(Constants.TAG, "Not caching entered passphrase!");
                                } else if (mRequiredInput.mType == RequiredInputType.PASSPHRASE_SUBKEY_UNLOCK) {
                                    Log.d(Constants.TAG, "Caching entered subkey passphrase");
                                        PassphraseCacheService.addCachedSubKeyPassphrase(getActivity(),
                                                mRequiredInput.getMasterKeyId(), mRequiredInput.getSubKeyId(),
                                                passphrase, result.getRing().getPrimaryUserIdWithFallback(), timeToLiveSeconds);
                                } else {
                                    Log.d(Constants.TAG, "Caching entered passphrase");
                                        PassphraseCacheService.addCachedPassphrase(getActivity(),
                                                mRequiredInput.getMasterKeyId(), passphrase,
                                                result.getRing().getPrimaryUserIdWithFallback(), timeToLiveSeconds);
                                }
                            } catch (PgpKeyNotFoundException e) {
                                Log.e(Constants.TAG, "adding of a passphrase failed", e);
                            }

                            returnWithPassphrase(passphrase);
                        }
                    }.execute();
                }
            });

            if(mPassphraseToTry != null) {
                mPassphraseEditText.setText(mPassphraseToTry.getCharArray(), 0, mPassphraseToTry.length());
                mPassphraseToTry = null;
                positive.performClick();
            }
        }

        private void returnWithPassphrase(Passphrase passphrase) {
            // any indication this isn't needed anymore, don't do it.
            if (mIsCancelled || getActivity() == null) {
                return;
            }
            CryptoInputParcel cryptoParcel = getArguments().getParcelable(EXTRA_CRYPTO_INPUT);

            // noinspection ConstantConditions, non-null cryptoParcel is handled in PassphraseDialogActivity.onCreate()
            cryptoParcel.mPassphrase = passphrase;

            ((PassphraseDialogActivity) getActivity()).handleResult(cryptoParcel);

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
            if (EditorInfo.IME_ACTION_DONE == actionId) {
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
    protected void handleResult(CryptoInputParcel cryptoInputParcel) {
        Intent returnIntent = getIntent();
        returnIntent.putExtra(RESULT_CRYPTO_INPUT, cryptoInputParcel);
        setResult(RESULT_OK, returnIntent);
    }

}
