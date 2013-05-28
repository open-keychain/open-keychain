package org.sufficientlysecure.keychain.crypto_provider;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.PgpMain;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CryptoActivity extends SherlockFragmentActivity {

    public static final String ACTION_CACHE_PASSPHRASE = "org.sufficientlysecure.keychain.CRYPTO_CACHE_PASSPHRASE";

    public static final String EXTRA_SECRET_KEY_ID = "secret_key_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleActions(getIntent());
    }

    protected void handleActions(Intent intent) {

        // TODO: Important: Check if calling package is in list!

        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (extras == null) {
            extras = new Bundle();
        }

        /**
         * com.android.crypto actions
         */
        if (ACTION_CACHE_PASSPHRASE.equals(action)) {
            long secretKeyId = extras.getLong(EXTRA_SECRET_KEY_ID);

            showPassphraseDialog(secretKeyId);
        } else {
            Log.e(Constants.TAG, "Wrong action!");
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    /**
     * Shows passphrase dialog to cache a new passphrase the user enters for using it later for
     * encryption. Based on mSecretKeyId it asks for a passphrase to open a private key or it asks
     * for a symmetric passphrase
     */
    private void showPassphraseDialog(long secretKeyId) {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    setResult(RESULT_OK);
                    finish();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(this,
                    messenger, secretKeyId);

            passphraseDialog.show(getSupportFragmentManager(), "passphraseDialog");
        } catch (PgpMain.PgpGeneralException e) {
            Log.d(Constants.TAG, "No passphrase for this secret key, encrypt directly!");
            // send message to handler to start encryption directly
            returnHandler.sendEmptyMessage(PassphraseDialogFragment.MESSAGE_OKAY);
        }
    }
}
