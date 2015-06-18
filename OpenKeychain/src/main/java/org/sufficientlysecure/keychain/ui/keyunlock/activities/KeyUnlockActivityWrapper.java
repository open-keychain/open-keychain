package org.sufficientlysecure.keychain.ui.keyunlock.activities;


import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.view.WindowManager;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.keyunlock.dialogs.PinUnlockDialog;
import org.sufficientlysecure.keychain.ui.keyunlock.dialogs.PinUnlockDialogViewModel;
import org.sufficientlysecure.keychain.ui.keyunlock.dialogs.UnlockDialog;

/**
 * Activity wrapper for the key unlock dialogs
 */
public class KeyUnlockActivityWrapper extends FragmentActivity {
    public static final String RESULT_CRYPTO_INPUT = "result_data";

    public static final String EXTRA_REQUIRED_INPUT = "required_input";
    public static final String EXTRA_SUBKEY_ID = "secret_key_id";

    // special extra for OpenPgpService
    public static final String EXTRA_SERVICE_INTENT = "data";

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

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
                    keyId = requiredInput.getSubKeyId();
                    break;
                }
                default: {
                    throw new AssertionError("Unsupported required input type!");
                }
            }
        }

        //Pin is the default
        CanonicalizedSecretKey.SecretKeyType keyType = CanonicalizedSecretKey.SecretKeyType.UNAVAILABLE;
         /* Get key type for message */
        // find a master key id for our key
        long masterKeyId = 0;
        try {
            masterKeyId = new ProviderHelper(this).getMasterKeyId(keyId);
            CachedPublicKeyRing keyRing = new ProviderHelper(this).getCachedPublicKeyRing(masterKeyId);
            // get the type of key (from the database)
            keyType = keyRing.getSecretKeyType(keyId);

            UnlockDialog unlockDialog = null;

            switch (keyType) {
              default: {
                    unlockDialog = new PinUnlockDialog();
                }
            }

            if (unlockDialog != null) {
                Intent serviceIntent = getIntent().getParcelableExtra(EXTRA_SERVICE_INTENT);

                Bundle bundle = new Bundle();
                bundle.putSerializable(UnlockDialog.EXTRA_PARAM_OPERATION_TYPE,
                        PinUnlockDialogViewModel.DialogUnlockOperation.DIALOG_UNLOCK_TYPE_UNLOCK_KEY);

                bundle.putLong(EXTRA_SUBKEY_ID, keyId);
                bundle.putParcelable(EXTRA_SERVICE_INTENT, serviceIntent);

                unlockDialog.setArguments(bundle);

                unlockDialog.show(this.getSupportFragmentManager(), "unlockDialog");

            }
            //error handler
        } catch (ProviderHelper.NotFoundException e) {
            e.printStackTrace();
        }
    }
}
