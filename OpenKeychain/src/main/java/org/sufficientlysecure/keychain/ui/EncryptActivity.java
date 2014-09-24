package org.sufficientlysecure.keychain.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.results.SignEncryptResult;

import java.util.Date;

public abstract class EncryptActivity extends DrawerActivity {

    public static final int REQUEST_CODE_PASSPHRASE = 0x00008001;
    public static final int REQUEST_CODE_NFC = 0x00008002;

    // For NFC data
    protected String mSigningKeyPassphrase = null;
    protected Date mNfcTimestamp = null;
    protected byte[] mNfcHash = null;

    protected void startPassphraseDialog(long subkeyId) {
        Intent intent = new Intent(this, PassphraseDialogActivity.class);
        intent.putExtra(PassphraseDialogActivity.EXTRA_SUBKEY_ID, subkeyId);
        startActivityForResult(intent, REQUEST_CODE_PASSPHRASE);
    }

    protected void startNfcSign(String pin, byte[] hashToSign, int hashAlgo) {
        // build PendingIntent for Yubikey NFC operations
        Intent intent = new Intent(this, NfcActivity.class);
        intent.setAction(NfcActivity.ACTION_SIGN_HASH);

        // pass params through to activity that it can be returned again later to repeat pgp operation
        intent.putExtra(NfcActivity.EXTRA_DATA, new Intent()); // not used, only relevant to OpenPgpService
        intent.putExtra(NfcActivity.EXTRA_PIN, pin);
        intent.putExtra(NfcActivity.EXTRA_NFC_HASH_TO_SIGN, hashToSign);
        intent.putExtra(NfcActivity.EXTRA_NFC_HASH_ALGO, hashAlgo);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivityForResult(intent, REQUEST_CODE_NFC);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PASSPHRASE: {
                if (resultCode == RESULT_OK && data != null) {
                    mSigningKeyPassphrase = data.getStringExtra(PassphraseDialogActivity.MESSAGE_DATA_PASSPHRASE);
                    startEncrypt();
                    return;
                }
                break;
            }

            case REQUEST_CODE_NFC: {
                if (resultCode == RESULT_OK && data != null) {
                    mNfcHash = data.getByteArrayExtra(OpenPgpApi.EXTRA_NFC_SIGNED_HASH);
                    startEncrypt();
                    return;
                }
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
                break;
            }
        }
    }

    public void startEncrypt() {
        if (!inputIsValid()) {
            // Notify was created by inputIsValid.
            return;
        }

        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(this, KeychainIntentService.class);
        intent.setAction(KeychainIntentService.ACTION_SIGN_ENCRYPT);
        intent.putExtra(KeychainIntentService.EXTRA_DATA, createEncryptBundle());

        // Message is received after encrypting is done in KeychainIntentService
        KeychainIntentServiceHandler serviceHandler = new KeychainIntentServiceHandler(this,
                getString(R.string.progress_encrypting), ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    SignEncryptResult pgpResult =
                            message.getData().getParcelable(SignEncryptResult.EXTRA_RESULT);

                    if (pgpResult.isPending()) {
                        if ((pgpResult.getResult() & SignEncryptResult.RESULT_PENDING_PASSPHRASE) ==
                                SignEncryptResult.RESULT_PENDING_PASSPHRASE) {
                            startPassphraseDialog(pgpResult.getKeyIdPassphraseNeeded());
                        } else if ((pgpResult.getResult() & SignEncryptResult.RESULT_PENDING_NFC) ==
                                SignEncryptResult.RESULT_PENDING_NFC) {

                            mNfcTimestamp = pgpResult.getNfcTimestamp();
                            startNfcSign(pgpResult.getNfcPassphrase(), pgpResult.getNfcHash(), pgpResult.getNfcAlgo());
                        } else {
                            throw new RuntimeException("Unhandled pending result!");
                        }
                        return;
                    }

                    if (pgpResult.success()) {
                        onEncryptSuccess(message, pgpResult);
                    } else {
                        pgpResult.createNotify(EncryptActivity.this).show();
                    }

                    // no matter the result, reset parameters
                    mSigningKeyPassphrase = null;
                    mNfcHash = null;
                    mNfcTimestamp = null;
                }
            }
        };
        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(serviceHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        serviceHandler.showProgressDialog(this);

        // start service with intent
        startService(intent);
    }

    protected abstract boolean inputIsValid();

    protected abstract void onEncryptSuccess(Message message, SignEncryptResult result);

    protected abstract Bundle createEncryptBundle();

}
