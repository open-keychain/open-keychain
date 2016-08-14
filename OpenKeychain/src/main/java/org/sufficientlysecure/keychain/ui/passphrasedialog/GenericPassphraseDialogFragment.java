package org.sufficientlysecure.keychain.ui.passphrasedialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
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
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderReader;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.ui.widget.CacheTTLSpinner;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.IOException;

public class GenericPassphraseDialogFragment extends BasePassphraseDialogFragment {
    private RequiredInputParcel mRequiredInput;

    private EditText mPassphraseEditText;
    private ViewAnimator mLayout;
    private CacheTTLSpinner mTimeToLiveSpinner;

    private CanonicalizedSecretKey mSecretKeyToUnlock;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        ProviderHelper helper = new ProviderHelper(activity);
        ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(activity);
        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);
        mRequiredInput = getArguments().getParcelable(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT);

        // No title, see http://www.google.com/design/spec/components/dialogs.html#dialogs-alerts
        //alert.setTitle()
        LayoutInflater inflater = LayoutInflater.from(theme);
        mLayout = (ViewAnimator) inflater.inflate(R.layout.passphrase_dialog, null);
        alert.setView(mLayout);

        TextView passphraseText = (TextView) mLayout.findViewById(R.id.passphrase_text);
        mPassphraseEditText = (EditText) mLayout.findViewById(R.id.passphrase_passphrase);
        mTimeToLiveSpinner = (CacheTTLSpinner) mLayout.findViewById(R.id.ttl_spinner);

        View vTimeToLiveLayout = mLayout.findViewById(R.id.remember_layout);
        vTimeToLiveLayout.setVisibility(mRequiredInput.mSkipCaching ? View.GONE : View.VISIBLE);
        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        // set these to the right value
        CanonicalizedSecretKeyRing.SecretKeyRingType keyRingType = CanonicalizedSecretKeyRing.SecretKeyRingType.PASSPHRASE;
        mSecretKeyToUnlock = null;
        String message;
        String hint;
        {
            try {
                switch (mRequiredInput.mType) {
                    case PASSPHRASE_SYMMETRIC: {
                        message = getString(R.string.passphrase_for_symmetric_encryption);
                        hint = getString(R.string.label_passphrase);
                        break;

                    }
                    case PASSPHRASE_KEYRING_UNLOCK: {
                        CachedPublicKeyRing cachedPublicKeyRing = helper.mReader.getCachedPublicKeyRing(
                                KeychainContract.KeyRings.buildUnifiedKeyRingUri(mRequiredInput.getMasterKeyId()));

                        // no need to set mSecretKeyToUnlock, as we are unlocking a keyring
                        keyRingType = cachedPublicKeyRing.getSecretKeyringType();
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
                    case PASSPHRASE_TOKEN_UNLOCK: {

                        CanonicalizedSecretKeyRing secretKeyRing = helper.mReader.getCanonicalizedSecretKeyRing(
                                mRequiredInput.getMasterKeyId(),
                                mRequiredInput.getKeyringPassphrase()
                        );
                        long subKeyId = mRequiredInput.getSubKeyId();
                        mSecretKeyToUnlock = secretKeyRing.getSecretKey(subKeyId);

                        // cached key operations are less expensive
                        CachedPublicKeyRing cachedPublicKeyRing = helper.mReader.getCachedPublicKeyRing(
                                KeychainContract.KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(subKeyId));
                        String mainUserId = cachedPublicKeyRing.getPrimaryUserIdWithFallback();
                        OpenPgpUtils.UserId mainUserIdSplit = KeyRing.splitUserId(mainUserId);
                        String userId = (mainUserIdSplit.name == null)
                                ? getString(R.string.user_id_no_name)
                                : mainUserIdSplit.name;

                        CanonicalizedSecretKey.SecretKeyType keyType = cachedPublicKeyRing.getSecretKeyType(subKeyId);
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
                    ProviderReader.NotFoundException e) {
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
        }

        passphraseText.setText(message);
        mPassphraseEditText.setHint(hint);
        openKeyboard(mPassphraseEditText);
        mPassphraseEditText.setImeActionLabel(getString(android.R.string.ok), EditorInfo.IME_ACTION_DONE);
        mPassphraseEditText.setOnEditorActionListener(this);
        mPassphraseEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());

        // set keyboard input related views
        {
            final ImageButton keyboard = (ImageButton) mLayout.findViewById(R.id.passphrase_keyboard);

            if (mRequiredInput.mType == RequiredInputParcel.RequiredInputType.PASSPHRASE_TOKEN_UNLOCK) {
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
        }

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

    public void onStart() {
        super.onStart();

        // Override the default behavior so the dialog is NOT dismissed on click
        final Button positive = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
        positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final Passphrase passphrase = new Passphrase(mPassphraseEditText);
                final int timeToLiveSeconds = mTimeToLiveSpinner.getSelectedTimeToLive();

                // Early breakout if we are dealing with a symmetric key
                if (mRequiredInput.mType == RequiredInputParcel.RequiredInputType.PASSPHRASE_SYMMETRIC) {
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
                                                helper.mReader.getCanonicalizedSecretKeyRing(
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
                                case PASSPHRASE_TOKEN_UNLOCK: {
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
                        } catch (PgpGeneralException | ProviderReader.NotFoundException |
                                ByteArrayEncryptor.EncryptDecryptException e) {
                            Toast.makeText(getActivity(), R.string.error_could_not_extract_private_key,
                                    Toast.LENGTH_SHORT).show();

                            getActivity().setResult(Activity.RESULT_CANCELED);
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
                            } else if (mRequiredInput.mType == RequiredInputParcel.RequiredInputType.PASSPHRASE_TOKEN_UNLOCK) {
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

        // for use when importing keys
        Passphrase passphraseToTry = getArguments().getParcelable(PassphraseDialogActivity.EXTRA_PASSPHRASE_TO_TRY);
        if(passphraseToTry != null) {
            mPassphraseEditText.setText(passphraseToTry.getCharArray(), 0, passphraseToTry.length());
            positive.performClick();
        }
    }
}
