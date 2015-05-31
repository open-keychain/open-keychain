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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
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
import android.widget.TextView;
import android.widget.Toast;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper.NotFoundException;
import org.sufficientlysecure.keychain.remote.CryptoInputParcelCacheService;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

/**
 * We can not directly create a dialog on the application context.
 * This activity encapsulates a DialogFragment to emulate a dialog.
 */
public class PassphraseDialogActivity extends FragmentActivity {
    public static final String RESULT_CRYPTO_INPUT = "result_data";

    public static final String EXTRA_REQUIRED_INPUT = "required_input";
    public static final String EXTRA_SUBKEY_ID = "secret_key_id";

    // special extra for OpenPgpService
    public static final String EXTRA_SERVICE_INTENT = "data";

    private static final int REQUEST_CODE_ENTER_PATTERN = 2;

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

        // this activity itself has no content view (see manifest)

        long keyId;
        if (getIntent().hasExtra(EXTRA_SUBKEY_ID)) {
            keyId = getIntent().getLongExtra(EXTRA_SUBKEY_ID, 0);
        } else {
            RequiredInputParcel requiredInput = getIntent().getParcelableExtra(EXTRA_REQUIRED_INPUT);
            switch (requiredInput.mType) {
                case PASSPHRASE_SYMMETRIC: {
                    keyId = Constants.key.symmetric;
                    break;
                }
                case PASSPHRASE: {

                    // handle empty passphrases by directly returning an empty crypto input parcel
                    try {
                        CanonicalizedSecretKeyRing pubRing =
                                new ProviderHelper(this).getCanonicalizedSecretKeyRing(
                                        requiredInput.getMasterKeyId());
                        // use empty passphrase for empty passphrase
                        if (pubRing.getSecretKey(requiredInput.getSubKeyId()).getSecretKeyType() ==
                                SecretKeyType.PASSPHRASE_EMPTY) {
                            // also return passphrase back to activity
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra(RESULT_CRYPTO_INPUT, new CryptoInputParcel(new Passphrase("")));
                            setResult(RESULT_OK, returnIntent);
                            finish();
                            return;
                        }
                    } catch (NotFoundException e) {
                        Log.e(Constants.TAG, "Key not found?!", e);
                        setResult(RESULT_CANCELED);
                        finish();
                        return;
                    }

                    keyId = requiredInput.getSubKeyId();
                    break;
                }
                default: {
                    throw new AssertionError("Unsupported required input type for PassphraseDialogActivity!");
                }
            }
        }

        Intent serviceIntent = getIntent().getParcelableExtra(EXTRA_SERVICE_INTENT);

        show(this, keyId, serviceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_ENTER_PATTERN: {
                /*
                 * NOTE that there are 4 possible result codes!!!
                 */
                switch (resultCode) {
                    case RESULT_OK:
                        // The user passed
                        break;
                    case RESULT_CANCELED:
                        // The user cancelled the task
                        break;
//                    case LockPatternActivity.RESULT_FAILED:
//                        // The user failed to enter the pattern
//                        break;
//                    case LockPatternActivity.RESULT_FORGOT_PATTERN:
//                        // The user forgot the pattern and invoked your recovery Activity.
//                        break;
                }

                /*
                 * In any case, there's always a key EXTRA_RETRY_COUNT, which holds
                 * the number of tries that the user did.
                 */
//                int retryCount = data.getIntExtra(
//                        LockPatternActivity.EXTRA_RETRY_COUNT, 0);

                break;
            }
        }
    }

    /**
     * Shows passphrase dialog to cache a new passphrase the user enters for using it later for
     * encryption. Based on mSecretKeyId it asks for a passphrase to open a private key or it asks
     * for a symmetric passphrase
     */
    public static void show(final FragmentActivity context, final long keyId, final Intent serviceIntent) {
        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
                // do NOT check if the key even needs a passphrase. that's not our job here.
                PassphraseDialogFragment frag = new PassphraseDialogFragment();
                Bundle args = new Bundle();
                args.putLong(EXTRA_SUBKEY_ID, keyId);
                args.putParcelable(EXTRA_SERVICE_INTENT, serviceIntent);
                frag.setArguments(args);
                frag.show(context.getSupportFragmentManager(), "passphraseDialog");
            }
        });
    }

    public static class PassphraseDialogFragment extends DialogFragment implements TextView.OnEditorActionListener {
        private EditText mPassphraseEditText;
        private TextView mPassphraseText;
        private View mInput, mProgress;

        private CanonicalizedSecretKeyRing mSecretRing = null;
        private boolean mIsCancelled = false;
        private long mSubKeyId;

        private Intent mServiceIntent;

        /**
         * Creates dialog
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();

            // if the dialog is displayed from the application class, design is missing
            // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
            ContextThemeWrapper theme = new ContextThemeWrapper(activity,
                    R.style.Theme_AppCompat_Light_Dialog);

            mSubKeyId = getArguments().getLong(EXTRA_SUBKEY_ID);
            mServiceIntent = getArguments().getParcelable(EXTRA_SERVICE_INTENT);

            CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);

            // No title, see http://www.google.com/design/spec/components/dialogs.html#dialogs-alerts
            //alert.setTitle()

            LayoutInflater inflater = LayoutInflater.from(theme);
            View view = inflater.inflate(R.layout.passphrase_dialog, null);
            alert.setView(view);

            mPassphraseText = (TextView) view.findViewById(R.id.passphrase_text);
            mPassphraseEditText = (EditText) view.findViewById(R.id.passphrase_passphrase);
            mInput = view.findViewById(R.id.input);
            mProgress = view.findViewById(R.id.progress);

            alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

            String userId;
            CanonicalizedSecretKey.SecretKeyType keyType = CanonicalizedSecretKey.SecretKeyType.PASSPHRASE;

            String message;
            if (mSubKeyId == Constants.key.symmetric || mSubKeyId == Constants.key.none) {
                message = getString(R.string.passphrase_for_symmetric_encryption);
            } else {
                try {
                    ProviderHelper helper = new ProviderHelper(activity);
                    mSecretRing = helper.getCanonicalizedSecretKeyRing(
                            KeychainContract.KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(mSubKeyId));
                    // yes the inner try/catch block is necessary, otherwise the final variable
                    // above can't be statically verified to have been set in all cases because
                    // the catch clause doesn't return.
                    try {
                        String mainUserId = mSecretRing.getPrimaryUserIdWithFallback();
                        KeyRing.UserId mainUserIdSplit = KeyRing.splitUserId(mainUserId);
                        if (mainUserIdSplit.name != null) {
                            userId = mainUserIdSplit.name;
                        } else {
                            userId = getString(R.string.user_id_no_name);
                        }
                    } catch (PgpKeyNotFoundException e) {
                        userId = null;
                    }

                    /* Get key type for message */
                    // find a master key id for our key
                    long masterKeyId = new ProviderHelper(activity).getMasterKeyId(mSubKeyId);
                    CachedPublicKeyRing keyRing = new ProviderHelper(activity).getCachedPublicKeyRing(masterKeyId);
                    // get the type of key (from the database)
                    keyType = keyRing.getSecretKeyType(mSubKeyId);
                    switch (keyType) {
                        case PASSPHRASE:
                            message = getString(R.string.passphrase_for, userId);
                            break;
                        case PIN:
                            message = getString(R.string.pin_for, userId);
                            break;
                        case DIVERT_TO_CARD:
                            message = getString(R.string.yubikey_pin_for, userId);
                            break;
                        default:
                            throw new AssertionError("Unhandled SecretKeyType (should not happen)");
                    }

                } catch (ProviderHelper.NotFoundException e) {
                    alert.setTitle(R.string.title_key_not_found);
                    alert.setMessage(getString(R.string.key_not_found, mSubKeyId));
                    alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    });
                    alert.setCancelable(false);
                    return alert.create();
                }
            }

            mPassphraseText.setText(message);

            if (keyType == CanonicalizedSecretKey.SecretKeyType.PATTERN) {
                // start pattern dialog and show progress circle here...
//                Intent patternActivity = new Intent(getActivity(), LockPatternActivity.class);
//                patternActivity.putExtra(LockPatternActivity.EXTRA_PATTERN, "123");
//                startActivityForResult(patternActivity, REQUEST_CODE_ENTER_PATTERN);
                mInput.setVisibility(View.INVISIBLE);
                mProgress.setVisibility(View.VISIBLE);
            } else {
                // Hack to open keyboard.
                // This is the only method that I found to work across all Android versions
                // http://turbomanage.wordpress.com/2012/05/02/show-soft-keyboard-automatically-when-edittext-receives-focus/
                // Notes: * onCreateView can't be used because we want to add buttons to the dialog
                //        * opening in onActivityCreated does not work on Android 4.4
                mPassphraseEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        mPassphraseEditText.post(new Runnable() {
                            @Override
                            public void run() {
                                if (getActivity() == null || mPassphraseEditText == null) {
                                    return;
                                }
                                InputMethodManager imm = (InputMethodManager) getActivity()
                                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.showSoftInput(mPassphraseEditText, InputMethodManager.SHOW_IMPLICIT);
                            }
                        });
                    }
                });
                mPassphraseEditText.requestFocus();

                mPassphraseEditText.setImeActionLabel(getString(android.R.string.ok), EditorInfo.IME_ACTION_DONE);
                mPassphraseEditText.setOnEditorActionListener(this);

                if (keyType == CanonicalizedSecretKey.SecretKeyType.DIVERT_TO_CARD && Preferences.getPreferences(activity).useNumKeypadForYubiKeyPin()) {
                    mPassphraseEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else if (keyType == CanonicalizedSecretKey.SecretKeyType.PIN) {
                    mPassphraseEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    mPassphraseEditText.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }

                mPassphraseEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }

            AlertDialog dialog = alert.create();
            dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    activity.getString(R.string.btn_unlock), (DialogInterface.OnClickListener) null);

            return dialog;
        }

        @Override
        public void onStart() {
            super.onStart();

            // Override the default behavior so the dialog is NOT dismissed on click
            final Button positive = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Passphrase passphrase = new Passphrase(mPassphraseEditText);

                    // Early breakout if we are dealing with a symmetric key
                    if (mSecretRing == null) {
                        PassphraseCacheService.addCachedPassphrase(getActivity(),
                                Constants.key.symmetric, Constants.key.symmetric, passphrase,
                                getString(R.string.passp_cache_notif_pwd));

                        finishCaching(passphrase);
                        return;
                    }

                    mInput.setVisibility(View.INVISIBLE);
                    mProgress.setVisibility(View.VISIBLE);
                    positive.setEnabled(false);

                    new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Void... params) {
                            try {
                                // wait some 100ms here, give the user time to appreciate the progress bar
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    // never mind
                                }
                                // make sure this unlocks
                                return mSecretRing.getSecretKey(mSubKeyId).unlock(passphrase);
                            } catch (PgpGeneralException e) {
                                Toast.makeText(getActivity(), R.string.error_could_not_extract_private_key,
                                        Toast.LENGTH_SHORT).show();

                                getActivity().setResult(RESULT_CANCELED);
                                dismiss();
                                getActivity().finish();
                                return false;
                            }
                        }

                        /** Handle a good or bad passphrase. This happens in the UI thread!  */
                        @Override
                        protected void onPostExecute(Boolean result) {
                            super.onPostExecute(result);

                            // if we were cancelled in the meantime, the result isn't relevant anymore
                            if (mIsCancelled) {
                                return;
                            }

                            // if the passphrase was wrong, reset and re-enable the dialogue
                            if (!result) {
                                mPassphraseEditText.setText("");
                                mPassphraseEditText.setError(getString(R.string.wrong_passphrase));
                                mInput.setVisibility(View.VISIBLE);
                                mProgress.setVisibility(View.INVISIBLE);
                                positive.setEnabled(true);
                                return;
                            }

                            // cache the new passphrase
                            Log.d(Constants.TAG, "Everything okay! Caching entered passphrase");

                            try {
                                PassphraseCacheService.addCachedPassphrase(getActivity(),
                                        mSecretRing.getMasterKeyId(), mSubKeyId, passphrase,
                                        mSecretRing.getPrimaryUserIdWithFallback());
                            } catch (PgpKeyNotFoundException e) {
                                Log.e(Constants.TAG, "adding of a passphrase failed", e);
                            }

                            finishCaching(passphrase);
                        }
                    }.execute();
                }
            });
        }

        private void finishCaching(Passphrase passphrase) {
            // any indication this isn't needed anymore, don't do it.
            if (mIsCancelled || getActivity() == null) {
                return;
            }

            CryptoInputParcel inputParcel = new CryptoInputParcel(null, passphrase);
            if (mServiceIntent != null) {
                CryptoInputParcelCacheService.addCryptoInputParcel(getActivity(), mServiceIntent, inputParcel);
                getActivity().setResult(RESULT_OK, mServiceIntent);
            } else {
                // also return passphrase back to activity
                Intent returnIntent = new Intent();
                returnIntent.putExtra(RESULT_CRYPTO_INPUT, inputParcel);
                getActivity().setResult(RESULT_OK, returnIntent);
            }

            dismiss();
            getActivity().finish();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);

            // note we need no synchronization here, this variable is only accessed in the ui thread
            mIsCancelled = true;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            if (getActivity() == null) {
                return;
            }

            hideKeyboard();

            getActivity().setResult(RESULT_CANCELED);
            getActivity().finish();
        }

        private void hideKeyboard() {
            if (getActivity() == null) {
                return;
            }

            InputMethodManager inputManager = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);

            inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }

        /**
         * Associate the "done" button on the soft keyboard with the okay button in the view
         */
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (EditorInfo.IME_ACTION_DONE == actionId) {
                AlertDialog dialog = ((AlertDialog) getDialog());
                Button bt = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

                bt.performClick();
                return true;
            }
            return false;
        }

    }


}
