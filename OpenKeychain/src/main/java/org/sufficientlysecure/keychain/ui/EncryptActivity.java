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
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.view.View;

import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.PgpSignEncryptResult;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.pgp.SignEncryptParcel;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;

import java.util.Date;

public abstract class EncryptActivity extends BaseActivity {

    public static final int REQUEST_CODE_PASSPHRASE = 0x00008001;
    public static final int REQUEST_CODE_NFC = 0x00008002;

    // For NFC data
    protected String mSigningKeyPassphrase = null;
    protected Date mNfcTimestamp = null;
    protected byte[] mNfcHash = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenDialogClose(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        }, false);
    }

    protected void startPassphraseDialog(long subkeyId) {
        Intent intent = new Intent(this, PassphraseDialogActivity.class);
        intent.putExtra(PassphraseDialogActivity.EXTRA_SUBKEY_ID, subkeyId);
        startActivityForResult(intent, REQUEST_CODE_PASSPHRASE);
    }

    protected void startNfcSign(long keyId, String pin, byte[] hashToSign, int hashAlgo) {
        // build PendingIntent for Yubikey NFC operations
        Intent intent = new Intent(this, NfcActivity.class);
        intent.setAction(NfcActivity.ACTION_SIGN_HASH);

        // pass params through to activity that it can be returned again later to repeat pgp operation
        intent.putExtra(NfcActivity.EXTRA_DATA, new Intent()); // not used, only relevant to OpenPgpService
        intent.putExtra(NfcActivity.EXTRA_KEY_ID, keyId);
        intent.putExtra(NfcActivity.EXTRA_PIN, pin);
        intent.putExtra(NfcActivity.EXTRA_NFC_HASH_TO_SIGN, hashToSign);
        intent.putExtra(NfcActivity.EXTRA_NFC_HASH_ALGO, hashAlgo);

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

        Bundle data = new Bundle();
        data.putParcelable(KeychainIntentService.SIGN_ENCRYPT_PARCEL, createEncryptBundle());
        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after encrypting is done in KeychainIntentService
        KeychainIntentServiceHandler serviceHandler = new KeychainIntentServiceHandler(this,
                getString(R.string.progress_encrypting), ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    SignEncryptResult result =
                            message.getData().getParcelable(SignEncryptResult.EXTRA_RESULT);

                    PgpSignEncryptResult pgpResult = result.getPending();

                    if (pgpResult != null && pgpResult.isPending()) {
                        if ((pgpResult.getResult() & PgpSignEncryptResult.RESULT_PENDING_PASSPHRASE) ==
                                PgpSignEncryptResult.RESULT_PENDING_PASSPHRASE) {
                            startPassphraseDialog(pgpResult.getKeyIdPassphraseNeeded());
                        } else if ((pgpResult.getResult() & PgpSignEncryptResult.RESULT_PENDING_NFC) ==
                                PgpSignEncryptResult.RESULT_PENDING_NFC) {

                            mNfcTimestamp = pgpResult.getNfcTimestamp();
                            startNfcSign(pgpResult.getNfcKeyId(), pgpResult.getNfcPassphrase(),
                                    pgpResult.getNfcHash(), pgpResult.getNfcAlgo());
                        } else {
                            throw new RuntimeException("Unhandled pending result!");
                        }
                        return;
                    }

                    if (result.success()) {
                        onEncryptSuccess(result);
                    } else {
                        result.createNotify(EncryptActivity.this).show();
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

    protected abstract void onEncryptSuccess(SignEncryptResult result);

    protected abstract SignEncryptParcel createEncryptBundle();

}
