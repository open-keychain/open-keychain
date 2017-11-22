/*
 * Copyright (C) 2013-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2013-2014 Signe Rüsch
 * Copyright (C) 2013-2014 Philipp Jakubeit
 * Copyright (C) 2016 Nikita Mikhailov <nikita.s.mikhailov@gmail.com>
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


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewAnimator;

import nordpol.android.NfcGuideView;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.securitytoken.KeyType;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenConnection;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.base.BaseSecurityTokenActivity;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.OrientationUtils;
import org.sufficientlysecure.keychain.util.Passphrase;

/**
 * This class provides a communication interface to OpenPGP applications on ISO SmartCard compliant
 * NFC devices.
 * For the full specs, see http://g10code.com/docs/openpgp-card-2.0.pdf
 */
public class SecurityTokenOperationActivity extends BaseSecurityTokenActivity {

    public static final String TAG = "SecurityTokenOperationActivity";

    public static final String EXTRA_REQUIRED_INPUT = "required_input";
    public static final String EXTRA_CRYPTO_INPUT = "crypto_input";

    public static final String RESULT_CRYPTO_INPUT = "result_data";
    public static final String RESULT_TOKEN_INFO = "token_info";

    public ViewAnimator vAnimator;
    public TextView vErrorText;
    public Button vErrorTryAgainButton;
    public NfcGuideView nfcGuideView;

    private RequiredInputParcel mRequiredInput;

    private CryptoInputParcel mInputParcel;
    private SecurityTokenInfo mResultTokenInfo;

    @Override
    protected void initTheme() {
        mThemeChanger = new ThemeChanger(this);
        mThemeChanger.setThemes(R.style.Theme_Keychain_Light_Dialog,
                R.style.Theme_Keychain_Dark_Dialog);
        mThemeChanger.changeTheme();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.TAG, "NfcOperationActivity.onCreate");

        nfcGuideView = (NfcGuideView) findViewById(R.id.nfc_guide_view);

        // prevent annoying orientation changes while fumbling with the device
        OrientationUtils.lockOrientation(this);
        // prevent close when touching outside of the dialog (happens easily when fumbling with the device)
        setFinishOnTouchOutside(false);
        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mInputParcel = getIntent().getParcelableExtra(EXTRA_CRYPTO_INPUT);

        setTitle(R.string.security_token_nfc_text);

        vAnimator = (ViewAnimator) findViewById(R.id.view_animator);
        vAnimator.setDisplayedChild(0);

        nfcGuideView.setCurrentStatus(NfcGuideView.NfcGuideViewStatus.STARTING_POSITION);

        vErrorText = (TextView) findViewById(R.id.security_token_activity_3_error_text);
        vErrorTryAgainButton = (Button) findViewById(R.id.security_token_activity_3_error_try_again);
        vErrorTryAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resumeTagHandling();

                obtainPassphraseIfRequired();
                vAnimator.setDisplayedChild(0);

                nfcGuideView.setVisibility(View.VISIBLE);
                nfcGuideView.setCurrentStatus(NfcGuideView.NfcGuideViewStatus.STARTING_POSITION);
            }
        });
        Button vCancel = (Button) findViewById(R.id.security_token_activity_0_cancel);
        vCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        Intent intent = getIntent();
        Bundle data = intent.getExtras();

        mRequiredInput = data.getParcelable(EXTRA_REQUIRED_INPUT);

        obtainPassphraseIfRequired();
    }

    private void obtainPassphraseIfRequired() {
        // obtain passphrase for this subkey
        if (mRequiredInput.mType != RequiredInputParcel.RequiredInputType.SECURITY_TOKEN_MOVE_KEY_TO_CARD
                && mRequiredInput.mType != RequiredInputParcel.RequiredInputType.SECURITY_TOKEN_RESET_CARD) {
            obtainSecurityTokenPin(mRequiredInput);
            checkPinAvailability();
        } else {
            checkDeviceConnection();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_CODE_PIN == requestCode) {
            checkPinAvailability();
        }
    }

    private void checkPinAvailability() {
        try {
            Passphrase passphrase = PassphraseCacheService.getCachedPassphrase(this,
                    mRequiredInput.getMasterKeyId(), mRequiredInput.getSubKeyId());
            if (passphrase != null) {
                checkDeviceConnection();
            }
        } catch (PassphraseCacheService.KeyNotFoundException e) {
            throw new AssertionError(
                    "tried to find passphrase for non-existing key. this is a programming error!");
        }
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.security_token_operation_activity);
    }

    @Override
    public void onSecurityTokenPreExecute() {
        // start with indeterminate progress
        vAnimator.setDisplayedChild(1);
        nfcGuideView.setCurrentStatus(NfcGuideView.NfcGuideViewStatus.TRANSFERRING);
    }

    @Override
    protected void doSecurityTokenInBackground(SecurityTokenConnection stConnection) throws IOException {

        switch (mRequiredInput.mType) {
            case SECURITY_TOKEN_DECRYPT: {
                long tokenKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(
                        stConnection.getKeyFingerprint(KeyType.ENCRYPT));

                if (tokenKeyId != mRequiredInput.getSubKeyId()) {
                    throw new IOException(getString(R.string.error_wrong_security_token));
                }

                KeyRepository keyRepository =
                        KeyRepository.create(this);
                CanonicalizedPublicKeyRing publicKeyRing;
                try {
                    publicKeyRing = keyRepository.getCanonicalizedPublicKeyRing(
                            KeychainContract.KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(mRequiredInput.getMasterKeyId()));
                } catch (KeyRepository.NotFoundException e) {
                    throw new IOException("Couldn't find subkey for key to token operation.");
                }

                for (int i = 0; i < mRequiredInput.mInputData.length; i++) {
                    byte[] encryptedSessionKey = mRequiredInput.mInputData[i];
                    byte[] decryptedSessionKey = stConnection
                            .decryptSessionKey(encryptedSessionKey, publicKeyRing.getPublicKey(tokenKeyId));
                    mInputParcel = mInputParcel.withCryptoData(encryptedSessionKey, decryptedSessionKey);
                }
                break;
            }
            case SECURITY_TOKEN_SIGN: {
                long tokenKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(
                        stConnection.getKeyFingerprint(KeyType.SIGN));

                if (tokenKeyId != mRequiredInput.getSubKeyId()) {
                    throw new IOException(getString(R.string.error_wrong_security_token));
                }

                mInputParcel = mInputParcel.withSignatureTime(mRequiredInput.mSignatureTime);

                for (int i = 0; i < mRequiredInput.mInputData.length; i++) {
                    byte[] hash = mRequiredInput.mInputData[i];
                    int algo = mRequiredInput.mSignAlgos[i];
                    byte[] signedHash = stConnection.calculateSignature(hash, algo);
                    mInputParcel = mInputParcel.withCryptoData(hash, signedHash);
                }
                break;
            }
            case SECURITY_TOKEN_AUTH: {
                long tokenKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(
                        stConnection.getKeyFingerprint(KeyType.AUTH));

                if (tokenKeyId != mRequiredInput.getSubKeyId()) {
                    throw new IOException(getString(R.string.error_wrong_security_token));
                }

                for (int i = 0; i < mRequiredInput.mInputData.length; i++) {
                    byte[] hash = mRequiredInput.mInputData[i];
                    int algo = mRequiredInput.mSignAlgos[i];
                    byte[] signedHash = stConnection.calculateAuthenticationSignature(hash, algo);
                    mInputParcel = mInputParcel.withCryptoData(hash, signedHash);

                }

                break;
            }
            case SECURITY_TOKEN_MOVE_KEY_TO_CARD: {
                Passphrase adminPin = new Passphrase("12345678");

                KeyRepository keyRepository =
                        KeyRepository.create(this);
                CanonicalizedSecretKeyRing secretKeyRing;
                try {
                    secretKeyRing = keyRepository.getCanonicalizedSecretKeyRing(
                            KeychainContract.KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(mRequiredInput.getMasterKeyId())
                    );
                } catch (KeyRepository.NotFoundException e) {
                    throw new IOException("Couldn't find subkey for key to token operation.");
                }

                byte[] newPin = mRequiredInput.mInputData[0];
                byte[] newAdminPin = mRequiredInput.mInputData[1];

                for (int i = 2; i < mRequiredInput.mInputData.length; i++) {
                    byte[] subkeyBytes = mRequiredInput.mInputData[i];
                    ByteBuffer buf = ByteBuffer.wrap(subkeyBytes);
                    long subkeyId = buf.getLong();

                    CanonicalizedSecretKey key = secretKeyRing.getSecretKey(subkeyId);
                    byte[] tokenSerialNumber = Arrays.copyOf(stConnection.getAid(), 16);

                    Passphrase passphrase;
                    try {
                        passphrase = PassphraseCacheService.getCachedPassphrase(this,
                                mRequiredInput.getMasterKeyId(), mRequiredInput.getSubKeyId());
                    } catch (PassphraseCacheService.KeyNotFoundException e) {
                        throw new IOException("Unable to get cached passphrase!");
                    }

                    stConnection.changeKey(key, passphrase, adminPin);

                    // TODO: Is this really used anywhere?
                    mInputParcel = mInputParcel.withCryptoData(subkeyBytes, tokenSerialNumber);
                }

                // First set Admin PIN, then PIN.
                // Order is important for Gnuk, otherwise it will be set up in "admin less mode".
                // http://www.fsij.org/doc-gnuk/gnuk-passphrase-setting.html#set-up-pw1-pw3-and-reset-code
                stConnection.modifyPw3Pin(newAdminPin, adminPin);
                stConnection.resetPin(newPin, new Passphrase(new String(newAdminPin)));

                SecurityTokenConnection.clearCachedConnections();

                break;
            }
            case SECURITY_TOKEN_RESET_CARD: {
                stConnection.resetAndWipeToken();
                mResultTokenInfo = stConnection.getTokenInfo();

                break;
            }
            default: {
                throw new AssertionError("Unhandled mRequiredInput.mType");
            }
        }

    }

    @Override
    protected final void onSecurityTokenPostExecute(final SecurityTokenConnection stConnection) {
        handleResult(mInputParcel);

        // show finish
        vAnimator.setDisplayedChild(2);

        nfcGuideView.setCurrentStatus(NfcGuideView.NfcGuideViewStatus.DONE);

        if (stConnection.isPersistentConnectionAllowed()) {
            // Just close
            finish();
        } else {
            stConnection.clearSecureMessaging();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    // check all 200ms if Security Token has been taken away
                    while (true) {
                        if (stConnection.isConnected()) {
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException ignored) {
                            }
                        } else {
                            return null;
                        }
                    }
                }

                @Override
                protected void onPostExecute(Void result) {
                    super.onPostExecute(result);
                    finish();
                }
            }.execute();
        }
    }

    /**
     * Defines how the result of this activity is returned.
     * Is overwritten in RemoteSecurityTokenOperationActivity
     */
    protected void handleResult(CryptoInputParcel inputParcel) {
        Intent result = new Intent();
        // send back the CryptoInputParcel we received
        result.putExtra(RESULT_CRYPTO_INPUT, inputParcel);
        if (mResultTokenInfo != null) {
            result.putExtra(RESULT_TOKEN_INFO, mResultTokenInfo);
        }
        setResult(RESULT_OK, result);
    }

    @Override
    protected void onSecurityTokenError(String error) {
        pauseTagHandling();

        vErrorText.setText(error + "\n\n" + getString(R.string.security_token_nfc_try_again_text));
        vAnimator.setDisplayedChild(3);

        nfcGuideView.setVisibility(View.GONE);
    }

    @Override
    public void onSecurityTokenPinError(String error, SecurityTokenInfo tokeninfo) {
        onSecurityTokenError(error);

        // clear (invalid) passphrase
        PassphraseCacheService.clearCachedPassphrase(
                this, mRequiredInput.getMasterKeyId(), mRequiredInput.getSubKeyId());
    }
}
