/**
 * Copyright (c) 2013-2014 Philipp Jakubeit, Signe Rüsch, Dominik Schürmann
 *
 * Licensed under the Bouncy Castle License (MIT license). See LICENSE file for details.
 */

package org.sufficientlysecure.keychain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.remote.CryptoInputParcelCacheService;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.base.BaseNfcActivity;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.IOException;

/**
 * This class provides a communication interface to OpenPGP applications on ISO SmartCard compliant
 * NFC devices.
 * <p/>
 * For the full specs, see http://g10code.com/docs/openpgp-card-2.0.pdf
 */
public class NfcOperationActivity extends BaseNfcActivity {

    public static final String EXTRA_REQUIRED_INPUT = "required_input";

    // passthrough for OpenPgpService
    public static final String EXTRA_SERVICE_INTENT = "data";

    public static final String RESULT_DATA = "result_data";

    private RequiredInputParcel mRequiredInput;
    private Intent mServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.TAG, "NfcOperationActivity.onCreate");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        Bundle data = intent.getExtras();

        mRequiredInput = data.getParcelable(EXTRA_REQUIRED_INPUT);
        mServiceIntent = data.getParcelable(EXTRA_SERVICE_INTENT);

        // obtain passphrase for this subkey
        obtainYubiKeyPin(mRequiredInput);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.nfc_activity);
    }

    @Override
    protected void onNfcPerform() throws IOException {

        CryptoInputParcel inputParcel = new CryptoInputParcel(mRequiredInput.mSignatureTime);

        switch (mRequiredInput.mType) {
            case NFC_DECRYPT: {
                for (int i = 0; i < mRequiredInput.mInputHashes.length; i++) {
                    byte[] hash = mRequiredInput.mInputHashes[i];
                    byte[] decryptedSessionKey = nfcDecryptSessionKey(hash);
                    inputParcel.addCryptoData(hash, decryptedSessionKey);
                }
                break;
            }
            case NFC_SIGN: {
                for (int i = 0; i < mRequiredInput.mInputHashes.length; i++) {
                    byte[] hash = mRequiredInput.mInputHashes[i];
                    int algo = mRequiredInput.mSignAlgos[i];
                    byte[] signedHash = nfcCalculateSignature(hash, algo);
                    inputParcel.addCryptoData(hash, signedHash);
                }
                break;
            }
        }

        if (mServiceIntent != null) {
            CryptoInputParcelCacheService.addCryptoInputParcel(this, mServiceIntent, inputParcel);
            setResult(RESULT_OK, mServiceIntent);
        } else {
            Intent result = new Intent();
            result.putExtra(NfcOperationActivity.RESULT_DATA, inputParcel);
            setResult(RESULT_OK, result);
        }

        finish();
    }

    @Override
    public void handlePinError() {

        // avoid a loop
        Preferences prefs = Preferences.getPreferences(this);
        if (prefs.useDefaultYubiKeyPin()) {
            toast(getString(R.string.error_pin_nodefault));
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // clear (invalid) passphrase
        PassphraseCacheService.clearCachedPassphrase(
                this, mRequiredInput.getMasterKeyId(), mRequiredInput.getSubKeyId());

        obtainYubiKeyPin(mRequiredInput);
    }

}
