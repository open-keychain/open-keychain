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

package org.sufficientlysecure.keychain.ui.passphrasedialog;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.WindowManager;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing.SecretKeyRingType;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderReader;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel.RequiredInputType;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

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
                            new ProviderHelper(this).read().getCachedPublicKeyRing(requiredInput.getMasterKeyId());
                    if (pubRing.getSecretKeyringType() == SecretKeyRingType.PASSPHRASE_EMPTY) {
                        returnWithEmptyPassphrase(cryptoInputParcel);
                    }
                    return;
                }
                case PASSPHRASE_TOKEN_UNLOCK: {
                    if (!requiredInput.hasKeyringPassphrase()) {
                        throw new AssertionError("No keyring passphrase passed! (Should not happen)");
                    }
                    CachedPublicKeyRing pubRing =
                            new ProviderHelper(this).read().getCachedPublicKeyRing(requiredInput.getMasterKeyId());
                    if (pubRing.getSecretKeyType(requiredInput.getSubKeyId()) == SecretKeyType.PASSPHRASE_EMPTY) {
                        returnWithEmptyPassphrase(cryptoInputParcel);
                    }
                    return;
                }
                default: {
                    throw new AssertionError("Unhandled input type! (Should not happen)");
                }
            }
        } catch (ProviderReader.NotFoundException e) {
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
        RequiredInputParcel requiredInput = getIntent().getParcelableExtra(EXTRA_REQUIRED_INPUT);
        BasePassphraseDialogFragment frag =  (requiredInput.mType == RequiredInputType.BACKUP_CODE)
                ? new BackupCodePassphraseDialogFragment()
                : new GenericPassphraseDialogFragment();
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
