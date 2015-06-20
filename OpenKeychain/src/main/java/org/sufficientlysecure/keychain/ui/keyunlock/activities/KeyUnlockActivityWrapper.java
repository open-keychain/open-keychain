package org.sufficientlysecure.keychain.ui.keyunlock.activities;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.WindowManager;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.keyunlock.dialogs.PinUnlockDialog;
import org.sufficientlysecure.keychain.ui.keyunlock.dialogs.PinUnlockDialogViewModel;
import org.sufficientlysecure.keychain.ui.keyunlock.dialogs.UnlockDialog;

/**
 * Activity wrapper for the key unlock dialogs
 */
public class KeyUnlockActivityWrapper extends FragmentActivity {

    private KeyUnlockActivityWrapperViewModel mKeyUnlockActivityWrapperViewModel;
    public static final String RESULT_CRYPTO_INPUT = "result_data";
    public static final String EXTRA_REQUIRED_INPUT = "required_input";
    public static final String EXTRA_SUBKEY_ID = "secret_key_id";
    public static final String EXTRA_KEY_TYPE = "secret_key_type";

    // special extra for OpenPgpService
    public static final String EXTRA_SERVICE_INTENT = "data";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mKeyUnlockActivityWrapperViewModel = new KeyUnlockActivityWrapperViewModel();
        mKeyUnlockActivityWrapperViewModel.prepareViewModel(savedInstanceState,
                getIntent().getExtras(), this);

        // do not allow screenshots of passphrase input
        // to prevent "too easy" passphrase theft by root apps
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
            );
        }

        switch (mKeyUnlockActivityWrapperViewModel.getSecretKeyType()) {
            case PIN: {
                showUnlockDialog(new PinUnlockDialog());
            }
            break;
            default: {
                throw new AssertionError("Unhandled SecretKeyType (should not happen)");
            }
        }
    }

    public void showUnlockDialog(UnlockDialog unlockDialog) {
        if (unlockDialog != null) {
            Intent serviceIntent = getIntent().getParcelableExtra(EXTRA_SERVICE_INTENT);

            Bundle bundle = new Bundle();
            bundle.putSerializable(UnlockDialog.EXTRA_PARAM_OPERATION_TYPE,
                    PinUnlockDialogViewModel.DialogUnlockOperation.DIALOG_UNLOCK_TYPE_UNLOCK_KEY);

            bundle.putSerializable(EXTRA_KEY_TYPE, mKeyUnlockActivityWrapperViewModel.getSecretKeyType());
            bundle.putLong(EXTRA_SUBKEY_ID, mKeyUnlockActivityWrapperViewModel.getKeyId());
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
}
