package org.sufficientlysecure.keychain.ui;

import android.content.Intent;

import org.sufficientlysecure.keychain.nfc.NfcActivity;

import java.util.Date;

public class EncryptActivity extends DrawerActivity {

    public static final int REQUEST_CODE_PASSPHRASE = 0x00008001;
    public static final int REQUEST_CODE_NFC = 0x00008002;

    protected void startPassphraseDialog(long subkeyId) {
        Intent data = new Intent();

        // build PendingIntent for Yubikey NFC operations
        Intent intent = new Intent(this, PassphraseDialogActivity.class);
        // pass params through to activity that it can be returned again later to repeat pgp operation
        intent.putExtra(PassphraseDialogActivity.EXTRA_SUBKEY_ID, subkeyId);

//        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivityForResult(intent, REQUEST_CODE_PASSPHRASE);
    }

    protected void startNfcSign(String pin, byte[] hashToSign, int hashAlgo) {
        Intent data = new Intent();

        // build PendingIntent for Yubikey NFC operations
        Intent intent = new Intent(this, NfcActivity.class);
        intent.setAction(NfcActivity.ACTION_SIGN_HASH);

        // pass params through to activity that it can be returned again later to repeat pgp operation
        intent.putExtra(NfcActivity.EXTRA_DATA, data);
        intent.putExtra(NfcActivity.EXTRA_PIN, pin);
        intent.putExtra(NfcActivity.EXTRA_NFC_HASH_TO_SIGN, hashToSign);
        intent.putExtra(NfcActivity.EXTRA_NFC_HASH_ALGO, hashAlgo);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivityForResult(intent, REQUEST_CODE_NFC);
    }


}
