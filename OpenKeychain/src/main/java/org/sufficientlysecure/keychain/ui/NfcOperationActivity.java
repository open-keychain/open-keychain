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
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.remote.CryptoInputParcelCacheService;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.base.BaseNfcActivity;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

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

    private static final byte[] BLANK_FINGERPRINT = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};

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
        if (mRequiredInput.mType != RequiredInputParcel.RequiredInputType.NFC_MOVE_KEY_TO_CARD) {
            obtainYubiKeyPin(mRequiredInput);
        }
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
            case NFC_MOVE_KEY_TO_CARD: {
                ProviderHelper providerHelper = new ProviderHelper(this);
                CanonicalizedSecretKeyRing secretKeyRing;
                try {
                    secretKeyRing = providerHelper.getCanonicalizedSecretKeyRing(
                            KeychainContract.KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(mRequiredInput.getMasterKeyId())
                    );
                } catch (ProviderHelper.NotFoundException e) {
                    throw new IOException("Couldn't find subkey for key to card operation.");
                }

                for (int i = 0; i < mRequiredInput.mInputHashes.length; i++) {
                    byte[] subkeyBytes = mRequiredInput.mInputHashes[i];
                    ByteBuffer buf = ByteBuffer.wrap(subkeyBytes);
                    long subkeyId = buf.getLong();

                    CanonicalizedSecretKey key = secretKeyRing.getSecretKey(subkeyId);

                    long keyGenerationTimestampMillis = key.getCreationTime().getTime();
                    long keyGenerationTimestamp = keyGenerationTimestampMillis / 1000;
                    byte[] timestampBytes = ByteBuffer.allocate(4).putInt((int) keyGenerationTimestamp).array();
                    byte[] cardSerialNumber = Arrays.copyOf(nfcGetAid(), 16);

                    Passphrase passphrase;
                    try {
                        passphrase = PassphraseCacheService.getCachedPassphrase(this,
                                mRequiredInput.getMasterKeyId(), mRequiredInput.getSubKeyId());
                    } catch (PassphraseCacheService.KeyNotFoundException e) {
                        throw new IOException("Unable to get cached passphrase!");
                    }

                    if (key.canSign() || key.canCertify()) {
                        if (shouldPutKey(key.getFingerprint(), 0)) {
                            nfcPutKey(0xB6, key, passphrase);
                            nfcPutData(0xCE, timestampBytes);
                            nfcPutData(0xC7, key.getFingerprint());
                        } else {
                            throw new IOException("Key slot occupied; card must be reset to put new signature key.");
                        }
                    } else if (key.canEncrypt()) {
                        if (shouldPutKey(key.getFingerprint(), 1)) {
                            nfcPutKey(0xB8, key, passphrase);
                            nfcPutData(0xCF, timestampBytes);
                            nfcPutData(0xC8, key.getFingerprint());
                        } else {
                            throw new IOException("Key slot occupied; card must be reset to put new decryption key.");
                        }
                    } else if (key.canAuthenticate()) {
                        if (shouldPutKey(key.getFingerprint(), 2)) {
                            nfcPutKey(0xA4, key, passphrase);
                            nfcPutData(0xD0, timestampBytes);
                            nfcPutData(0xC9, key.getFingerprint());
                        } else {
                            throw new IOException("Key slot occupied; card must be reset to put new authentication key.");
                        }
                    } else {
                        throw new IOException("Inappropriate key flags for smart card key.");
                    }

                    inputParcel.addCryptoData(subkeyBytes, cardSerialNumber);
                }
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

    private boolean shouldPutKey(byte[] fingerprint, int idx) throws IOException {
        byte[] cardFingerprint = nfcGetFingerprint(idx);
        // Slot is empty, or contains this key already. PUT KEY operation is safe
        if (Arrays.equals(cardFingerprint, BLANK_FINGERPRINT) ||
            Arrays.equals(cardFingerprint, fingerprint)) {
            return true;
        }

        // Slot already contains a different key; don't overwrite it.
        return false;
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
