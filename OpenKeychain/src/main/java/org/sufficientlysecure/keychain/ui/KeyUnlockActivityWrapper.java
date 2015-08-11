/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.WindowManager;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.base.BaseNfcActivity;
import org.sufficientlysecure.keychain.ui.dialog.NFCUnlockDialog;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseUnlockDialog;
import org.sufficientlysecure.keychain.ui.dialog.PatternUnlockDialog;
import org.sufficientlysecure.keychain.ui.dialog.PinUnlockDialog;
import org.sufficientlysecure.keychain.ui.dialog.UnlockDialog;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity.NfcListenerFragment;

import java.io.IOException;

/**
 * Activity wrapper for the key unlock dialogs
 */
public class KeyUnlockActivityWrapper extends BaseNfcActivity {
    public static final String RESULT_CRYPTO_INPUT = "result_data";
    public static final String EXTRA_REQUIRED_INPUT = "required_input";
    public static final String EXTRA_SUBKEY_ID = "secret_key_id";
    public static final String EXTRA_KEY_TYPE = "secret_key_type";
    public static final String EXTRA_CRYPTO_INPUT = "crypto_input";
    private long mKeyId = Constants.key.none;
    private UnlockDialog mUnlockDialog;

    // special extra for OpenPgpService
    public static final String EXTRA_SERVICE_INTENT = "data";

    //Passphrase is the default
    private CanonicalizedSecretKey.SecretKeyType mKeyType = CanonicalizedSecretKey.SecretKeyType.
            PASSPHRASE;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getIntent().getExtras().containsKey(EXTRA_SUBKEY_ID)) {
            mKeyId = getIntent().getExtras().getLong(EXTRA_SUBKEY_ID);

            RequiredInputParcel requiredInput = getIntent().getExtras().getParcelable(EXTRA_REQUIRED_INPUT);
            if (requiredInput != null) {
                switch (requiredInput.mType) {
                    case PASSPHRASE_SYMMETRIC: {
                        mKeyId = Constants.key.symmetric;
                        break;
                    }
                    case PASSPHRASE: {
                        mKeyId = requiredInput.getSubKeyId();
                        break;
                    }
                    default: {
                        throw new AssertionError("Unsupported required input type!");
                    }
                }
            }
        }

        obtainSecretKeyType();

        // do not allow screenshots of passphrase input
        // to prevent "too easy" passphrase theft by root apps
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
            );
        }

        startUnlockOperation();
    }

    /**
     * Obtains the secret key type.
     */
    public void obtainSecretKeyType() {
        // find a master key id for our key
        if (mKeyId != Constants.key.symmetric && mKeyId != Constants.key.none) {
            try {
                long masterKeyId = new ProviderHelper(this).getMasterKeyId(mKeyId);
                CachedPublicKeyRing keyRing = new ProviderHelper(this).
                        getCachedPublicKeyRing(masterKeyId);

                // get the type of key (from the database)
                mKeyType = keyRing.getSecretKeyType(mKeyId);
                //error handler
            } catch (ProviderHelper.NotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Starts the unlock operation
     */
    public void startUnlockOperation() {
        switch (mKeyType) {
            case PASSPHRASE: {
                showUnlockDialog(new PassphraseUnlockDialog());
            }
            break;
            case PIN: {
                showUnlockDialog(new PinUnlockDialog());
            }
            break;
            case PATTERN: {
                showUnlockDialog(new PatternUnlockDialog());
            }
            break;
            case NFC: {
                showUnlockDialog(new NFCUnlockDialog());
            }
            break;
            default: {
                throw new AssertionError("Unhandled SecretKeyType (should not happen)");
            }
        }
    }

    /**
     * Shows an unlock dialog.
     *
     * @param unlockDialog
     */
    public void showUnlockDialog(UnlockDialog unlockDialog) {
        if (unlockDialog != null) {
            mUnlockDialog = unlockDialog;
            Intent serviceIntent = getIntent().getParcelableExtra(EXTRA_SERVICE_INTENT);

            Bundle bundle = new Bundle();
            bundle.putSerializable(EXTRA_KEY_TYPE, mKeyType);
            bundle.putLong(EXTRA_SUBKEY_ID, mKeyId);
            bundle.putParcelable(EXTRA_SERVICE_INTENT, serviceIntent);
            unlockDialog.setArguments(bundle);

            show(unlockDialog, this, bundle);
        }
    }

    /**
     * Shows passphrase dialog to cache a new passphrase the user enters for using it later for
     * encryption. Based on mSecretKeyId it asks for a passphrase to open a private key or it asks
     * for a symmetric passphrase
     */
    public static void show(final UnlockDialog unlockDialog, final FragmentActivity fragmentActivity,
                            final Bundle bundle) {
        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
                // do NOT check if the key even needs a passphrase. that's not our job here.
                if (bundle != null) {
                    unlockDialog.setArguments(bundle);
                }
                unlockDialog.show(fragmentActivity.getSupportFragmentManager(), "passphraseDialog");
            }
        });
    }

    //NFC STUFF
    @Override
    protected void doNfcInBackground() throws IOException {
        super.doNfcInBackground();
        if (mUnlockDialog != null && mUnlockDialog instanceof NfcListenerFragment) {
            ((NfcListenerFragment) mUnlockDialog).doNfcInBackground();
        }
    }

    @Override
    protected void onNfcPreExecute() throws IOException {
        if (mUnlockDialog != null && mUnlockDialog instanceof NfcListenerFragment) {
            ((NfcListenerFragment) mUnlockDialog).onNfcPreExecute();
        }
    }

    @Override
    protected void onNfcPostExecute() throws IOException {
        if (mUnlockDialog != null && mUnlockDialog instanceof NfcListenerFragment) {
            ((NfcListenerFragment) mUnlockDialog).onNfcPostExecute();
        }
    }

    @Override
    protected void handleTagDiscoveredIntent(Intent intent) throws IOException {
        super.handleTagDiscoveredIntent(intent);
        if (mUnlockDialog != null && mUnlockDialog instanceof NfcListenerFragment) {
            ((NfcListenerFragment) mUnlockDialog).onNfcTagDiscovery(intent);
        }
    }

    @Override
    protected void handleNfcError(Exception e) {
        super.handleNfcError(e);
        if (mUnlockDialog != null && mUnlockDialog instanceof NfcListenerFragment) {
            ((NfcListenerFragment) mUnlockDialog).onNfcError(e);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof UnlockDialog) {
            mUnlockDialog = (UnlockDialog) fragment;
        }
    }
}
