/**
 * Copyright (c) 2013-2014 Philipp Jakubeit, Signe Rüsch, Dominik Schürmann
 *
 * Licensed under the Bouncy Castle License (MIT license). See LICENSE file for details.
 */

package org.sufficientlysecure.keychain.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.base.BaseNfcActivity;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;

/**
 * This class provides a communication interface to OpenPGP applications on ISO SmartCard compliant
 * NFC devices.
 *
 * For the full specs, see http://g10code.com/docs/openpgp-card-2.0.pdf
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
public class NfcOperationActivity extends BaseNfcActivity {

    public static final String EXTRA_REQUIRED_INPUT = "required_input";

    public static final String RESULT_DATA = "result_data";

    RequiredInputParcel mRequiredInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.TAG, "NfcOperationActivity.onCreate");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        Bundle data = intent.getExtras();

        mRequiredInput = data.getParcelable(EXTRA_REQUIRED_INPUT);

        // obtain passphrase for this subkey
        obtainYubikeyPin(RequiredInputParcel.createRequiredPassphrase(mRequiredInput));
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.nfc_activity);
    }

    @Override
    protected void onNfcPerform() throws IOException {

        CryptoInputParcel resultData = new CryptoInputParcel(mRequiredInput.mSignatureTime);

        switch (mRequiredInput.mType) {

            case NFC_DECRYPT:
                for (int i = 0; i < mRequiredInput.mInputHashes.length; i++) {
                    byte[] hash = mRequiredInput.mInputHashes[i];
                    byte[] decryptedSessionKey = nfcDecryptSessionKey(hash);
                    resultData.addCryptoData(hash, decryptedSessionKey);
                }
                break;

            case NFC_SIGN:
                for (int i = 0; i < mRequiredInput.mInputHashes.length; i++) {
                    byte[] hash = mRequiredInput.mInputHashes[i];
                    int algo = mRequiredInput.mSignAlgos[i];
                    byte[] signedHash = nfcCalculateSignature(hash, algo);
                    resultData.addCryptoData(hash, signedHash);
                }
                break;
        }

        // give data through for new service call
        Intent result = new Intent();
        result.putExtra(NfcOperationActivity.RESULT_DATA, resultData);
        setResult(RESULT_OK, result);
        finish();

    }
}
