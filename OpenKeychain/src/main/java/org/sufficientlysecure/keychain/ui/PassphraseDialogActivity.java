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
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

/**
 * We can not directly create a dialog on the application context.
 * This activity encapsulates a DialogFragment to emulate a dialog.
 */
public class PassphraseDialogActivity extends FragmentActivity {
    public static final String MESSAGE_DATA_PASSPHRASE = "passphrase";

    public static final String EXTRA_SUBKEY_ID = "secret_key_id";

    // special extra for OpenPgpService
    public static final String EXTRA_DATA = "data";

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

        long keyId = getIntent().getLongExtra(EXTRA_SUBKEY_ID, 0);

        Intent serviceIntent = getIntent().getParcelableExtra(EXTRA_DATA);

        show(this, keyId, serviceIntent);
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
                args.putParcelable(EXTRA_DATA, serviceIntent);

                frag.setArguments(args);

                frag.show(context.getSupportFragmentManager(), "passphraseDialog");
            }
        });
    }

    public static class PassphraseDialogFragment extends DialogFragment implements TextView.OnEditorActionListener {
        private EditText mPassphraseEditText;
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
                    R.style.Theme_AppCompat_Light);

            mSubKeyId = getArguments().getLong(EXTRA_SUBKEY_ID);
            mServiceIntent = getArguments().getParcelable(EXTRA_DATA);

            CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);

            alert.setTitle(R.string.title_authentication);

            String userId;
            CanonicalizedSecretKey.SecretKeyType keyType = CanonicalizedSecretKey.SecretKeyType.PASSPHRASE;

            if (mSubKeyId == Constants.key.symmetric || mSubKeyId == Constants.key.none) {
                alert.setMessage(R.string.passphrase_for_symmetric_encryption);
            } else {
                String message;
                try {
                    ProviderHelper helper = new ProviderHelper(activity);
                    mSecretRing = helper.getCanonicalizedSecretKeyRing(
                            KeychainContract.KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(mSubKeyId));
                    // yes the inner try/catch block is necessary, otherwise the final variable
                    // above can't be statically verified to have been set in all cases because
                    // the catch clause doesn't return.
                    try {
                        userId = mSecretRing.getPrimaryUserIdWithFallback();
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
                        case DIVERT_TO_CARD:
                            message = getString(R.string.yubikey_pin, userId);
                            break;
                        default:
                            message = "This should not happen!";
                            break;
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

                alert.setMessage(message);
            }

            LayoutInflater inflater = LayoutInflater.from(theme);
            View view = inflater.inflate(R.layout.passphrase_dialog, null);
            alert.setView(view);

            mPassphraseEditText = (EditText) view.findViewById(R.id.passphrase_passphrase);
            mInput = view.findViewById(R.id.input);
            mProgress = view.findViewById(R.id.progress);

            alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

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

            if (keyType == CanonicalizedSecretKey.SecretKeyType.DIVERT_TO_CARD && Preferences.getPreferences(activity).useNumKeypadForYubikeyPin()) {
                mPassphraseEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            } else {
                mPassphraseEditText.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }

            mPassphraseEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());

            AlertDialog dialog = alert.create();
            dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    activity.getString(android.R.string.ok), (DialogInterface.OnClickListener) null);

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
                    final String passphrase = mPassphraseEditText.getText().toString();

                    // Early breakout if we are dealing with a symmetric key
                    if (mSecretRing == null) {
                        PassphraseCacheService.addCachedPassphrase(getActivity(),
                                Constants.key.symmetric, Constants.key.symmetric,  passphrase,
                                getString(R.string.passp_cache_notif_pwd));

                        finishCaching(passphrase);
                        return;
                    }

                    mInput.setVisibility(View.GONE);
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
                                mProgress.setVisibility(View.GONE);
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

        private void finishCaching(String passphrase) {
            // any indication this isn't needed anymore, don't do it.
            if (mIsCancelled || getActivity() == null) {
                return;
            }

            if (mServiceIntent != null) {
                // TODO: Not routing passphrase through OpenPGP API currently
                // due to security concerns...
                // BUT this means you need to _cache_ passphrases!
                getActivity().setResult(RESULT_OK, mServiceIntent);
            } else {
                // also return passphrase back to activity
                Intent returnIntent = new Intent();
                returnIntent.putExtra(MESSAGE_DATA_PASSPHRASE, passphrase);
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
