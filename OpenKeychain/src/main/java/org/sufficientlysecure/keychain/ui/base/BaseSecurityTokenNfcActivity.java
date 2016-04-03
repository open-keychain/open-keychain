/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2013-2014 Signe Rüsch
 * Copyright (C) 2013-2014 Philipp Jakubeit
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

package org.sufficientlysecure.keychain.ui.base;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.os.AsyncTask;
import android.os.Bundle;

import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.javacard.BaseJavacardDevice;
import org.sufficientlysecure.keychain.javacard.JavacardDevice;
import org.sufficientlysecure.keychain.javacard.NfcTransport;
import org.sufficientlysecure.keychain.javacard.OnDiscoveredUsbDeviceListener;
import org.sufficientlysecure.keychain.javacard.UsbConnectionManager;
import org.sufficientlysecure.keychain.javacard.UsbTransport;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity;
import org.sufficientlysecure.keychain.ui.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.ui.ViewKeyActivity;
import org.sufficientlysecure.keychain.ui.dialog.FidesmoInstallDialog;
import org.sufficientlysecure.keychain.ui.dialog.FidesmoPgpInstallDialog;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.IOException;

import nordpol.IsoCard;
import nordpol.android.AndroidCard;
import nordpol.android.OnDiscoveredTagListener;
import nordpol.android.TagDispatcher;

public abstract class BaseSecurityTokenNfcActivity extends BaseActivity
        implements OnDiscoveredTagListener, OnDiscoveredUsbDeviceListener {
    public static final int REQUEST_CODE_PIN = 1;

    public static final String EXTRA_TAG_HANDLING_ENABLED = "tag_handling_enabled";

    private static final String FIDESMO_APP_PACKAGE = "com.fidesmo.sec.android";

    public JavacardDevice mJavacardDevice;
    protected TagDispatcher mTagDispatcher;
    protected UsbConnectionManager mUsbDispatcher;
    private boolean mTagHandlingEnabled;

    private byte[] mNfcFingerprints;
    private String mNfcUserId;
    private byte[] mNfcAid;

    /**
     * Override to change UI before NFC handling (UI thread)
     */
    protected void onNfcPreExecute() {
    }

    /**
     * Override to implement NFC operations (background thread)
     */
    protected void doNfcInBackground() throws IOException {
        mNfcFingerprints = mJavacardDevice.getFingerprints();
        mNfcUserId = mJavacardDevice.getUserId();
        mNfcAid = mJavacardDevice.getAid();
    }

    /**
     * Override to handle result of NFC operations (UI thread)
     */
    protected void onNfcPostExecute() {

        final long subKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(mNfcFingerprints);

        try {
            CachedPublicKeyRing ring = new ProviderHelper(this).getCachedPublicKeyRing(
                    KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(subKeyId));
            long masterKeyId = ring.getMasterKeyId();

            Intent intent = new Intent(this, ViewKeyActivity.class);
            intent.setData(KeyRings.buildGenericKeyRingUri(masterKeyId));
            intent.putExtra(ViewKeyActivity.EXTRA_SECURITY_TOKEN_AID, mNfcAid);
            intent.putExtra(ViewKeyActivity.EXTRA_SECURITY_TOKEN_USER_ID, mNfcUserId);
            intent.putExtra(ViewKeyActivity.EXTRA_SECURITY_TOKEN_FINGERPRINTS, mNfcFingerprints);
            startActivity(intent);
        } catch (PgpKeyNotFoundException e) {
            Intent intent = new Intent(this, CreateKeyActivity.class);
            intent.putExtra(CreateKeyActivity.EXTRA_NFC_AID, mNfcAid);
            intent.putExtra(CreateKeyActivity.EXTRA_NFC_USER_ID, mNfcUserId);
            intent.putExtra(CreateKeyActivity.EXTRA_NFC_FINGERPRINTS, mNfcFingerprints);
            startActivity(intent);
        }
    }

    /**
     * Override to use something different than Notify (UI thread)
     */
    protected void onNfcError(String error) {
        Notify.create(this, error, Style.WARN).show();
    }

    /**
     * Override to do something when PIN is wrong, e.g., clear passphrases (UI thread)
     */
    protected void onNfcPinError(String error) {
        onNfcError(error);
    }

    public void tagDiscovered(final Tag tag) {
        // Actual NFC operations are executed in doInBackground to not block the UI thread
        if(!mTagHandlingEnabled)
            return;
        new AsyncTask<Void, Void, IOException>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                onNfcPreExecute();
            }

            @Override
            protected IOException doInBackground(Void... params) {
                try {
                    handleTagDiscovered(tag);
                } catch (IOException e) {
                    return e;
                }

                return null;
            }

            @Override
            protected void onPostExecute(IOException exception) {
                super.onPostExecute(exception);

                if (exception != null) {
                    handleNfcError(exception);
                    return;
                }

                onNfcPostExecute();
            }
        }.execute();
    }


    public void usbDeviceDiscovered(final UsbDevice device) {
        // Actual NFC operations are executed in doInBackground to not block the UI thread
        if(!mTagHandlingEnabled)
            return;
        new AsyncTask<Void, Void, IOException>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                onNfcPreExecute();
            }

            @Override
            protected IOException doInBackground(Void... params) {
                try {
                    handleUsbDevice(device);
                } catch (IOException e) {
                    return e;
                }

                return null;
            }

            @Override
            protected void onPostExecute(IOException exception) {
                super.onPostExecute(exception);

                if (exception != null) {
                    handleNfcError(exception);
                    return;
                }

                onNfcPostExecute();
            }
        }.execute();
    }

    protected void pauseTagHandling() {
        mTagHandlingEnabled = false;
    }

    protected void resumeTagHandling() {
        mTagHandlingEnabled = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTagDispatcher = TagDispatcher.get(this, this, false, false, true, false);
        mUsbDispatcher = new UsbConnectionManager(this, this);

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            mTagHandlingEnabled = savedInstanceState.getBoolean(EXTRA_TAG_HANDLING_ENABLED);
        } else {
            mTagHandlingEnabled = true;
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            throw new AssertionError("should not happen: NfcOperationActivity.onCreate is called instead of onNewIntent!");
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(EXTRA_TAG_HANDLING_ENABLED, mTagHandlingEnabled);
    }

    /**
     * This activity is started as a singleTop activity.
     * All new NFC Intents which are delivered to this activity are handled here
     */
    @Override
    public void onNewIntent(final Intent intent) {
        if (!mTagDispatcher.interceptIntent(intent)) {
            mUsbDispatcher.interceptIntent(intent);
        }
    }

    private void handleNfcError(IOException e) {

        if (e instanceof TagLostException) {
            onNfcError(getString(R.string.security_token_error_tag_lost));
            return;
        }

        if (e instanceof IsoDepNotSupportedException) {
            onNfcError(getString(R.string.security_token_error_iso_dep_not_supported));
            return;
        }

        short status;
        if (e instanceof CardException) {
            status = ((CardException) e).getResponseCode();
        } else {
            status = -1;
        }

        // Wrong PIN, a status of 63CX indicates X attempts remaining.
        if ((status & (short) 0xFFF0) == 0x63C0) {
            int tries = status & 0x000F;
            // hook to do something different when PIN is wrong
            onNfcPinError(getResources().getQuantityString(R.plurals.security_token_error_pin, tries, tries));
            return;
        }

        // Otherwise, all status codes are fixed values.
        switch (status) {
            // These errors should not occur in everyday use; if they are returned, it means we
            // made a mistake sending data to the token, or the token is misbehaving.
            case 0x6A80: {
                onNfcError(getString(R.string.security_token_error_bad_data));
                break;
            }
            case 0x6883: {
                onNfcError(getString(R.string.security_token_error_chaining_error));
                break;
            }
            case 0x6B00: {
                onNfcError(getString(R.string.security_token_error_header, "P1/P2"));
                break;
            }
            case 0x6D00: {
                onNfcError(getString(R.string.security_token_error_header, "INS"));
                break;
            }
            case 0x6E00: {
                onNfcError(getString(R.string.security_token_error_header, "CLA"));
                break;
            }
            // These error conditions are more likely to be experienced by an end user.
            case 0x6285: {
                onNfcError(getString(R.string.security_token_error_terminated));
                break;
            }
            case 0x6700: {
                onNfcPinError(getString(R.string.security_token_error_wrong_length));
                break;
            }
            case 0x6982: {
                onNfcError(getString(R.string.security_token_error_security_not_satisfied));
                break;
            }
            case 0x6983: {
                onNfcError(getString(R.string.security_token_error_authentication_blocked));
                break;
            }
            case 0x6985: {
                onNfcError(getString(R.string.security_token_error_conditions_not_satisfied));
                break;
            }
            // 6A88 is "Not Found" in the spec, but Yubikey also returns 6A83 for this in some cases.
            case 0x6A88:
            case 0x6A83: {
                onNfcError(getString(R.string.security_token_error_data_not_found));
                break;
            }
            // 6F00 is a JavaCard proprietary status code, SW_UNKNOWN, and usually represents an
            // unhandled exception on the security token.
            case 0x6F00: {
                onNfcError(getString(R.string.security_token_error_unknown));
                break;
            }
            // 6A82 app not installed on security token!
            case 0x6A82: {
                if (mJavacardDevice.isFidesmoToken()) {
                    // Check if the Fidesmo app is installed
                    if (isAndroidAppInstalled(FIDESMO_APP_PACKAGE)) {
                        promptFidesmoPgpInstall();
                    } else {
                        promptFidesmoAppInstall();
                    }
                } else { // Other (possibly) compatible hardware
                    onNfcError(getString(R.string.security_token_error_pgp_app_not_installed));
                }
                break;
            }
            default: {
                onNfcError(getString(R.string.security_token_error, e.getMessage()));
                break;
            }
        }

    }

    /**
     * Called when the system is about to start resuming a previous activity,
     * disables NFC Foreground Dispatch
     */
    public void onPause() {
        super.onPause();
        Log.d(Constants.TAG, "BaseNfcActivity.onPause");

        mTagDispatcher.disableExclusiveNfc();
        mUsbDispatcher.stopListeningForDevices();
    }

    /**
     * Called when the activity will start interacting with the user,
     * enables NFC Foreground Dispatch
     */
    public void onResume() {
        super.onResume();
        Log.d(Constants.TAG, "BaseNfcActivity.onResume");
        mTagDispatcher.enableExclusiveNfc();
        mUsbDispatcher.startListeningForDevices();
    }

    protected void obtainSecurityTokenPin(RequiredInputParcel requiredInput) {

        try {
            Passphrase passphrase = PassphraseCacheService.getCachedPassphrase(this,
                    requiredInput.getMasterKeyId(), requiredInput.getSubKeyId());
            if (passphrase != null) {
                mJavacardDevice.setPin(passphrase);
                return;
            }

            Intent intent = new Intent(this, PassphraseDialogActivity.class);
            intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT,
                    RequiredInputParcel.createRequiredPassphrase(requiredInput));
            startActivityForResult(intent, REQUEST_CODE_PIN);
        } catch (PassphraseCacheService.KeyNotFoundException e) {
            throw new AssertionError(
                    "tried to find passphrase for non-existing key. this is a programming error!");
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PIN: {
                if (resultCode != Activity.RESULT_OK) {
                    setResult(resultCode);
                    finish();
                    return;
                }
                CryptoInputParcel input = data.getParcelableExtra(PassphraseDialogActivity.RESULT_CRYPTO_INPUT);
                mJavacardDevice.setPin(input.getPassphrase());
                break;
            }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /** Handle NFC communication and return a result.
     *
     * This method is called by onNewIntent above upon discovery of an NFC tag.
     * It handles initialization and login to the application, subsequently
     * calls either nfcCalculateSignature() or nfcDecryptSessionKey(), then
     * finishes the activity with an appropriate result.
     *
     * On general communication, see also
     * http://www.cardwerk.com/smartcards/smartcard_standard_ISO7816-4_annex-a.aspx
     *
     * References to pages are generally related to the OpenPGP Application
     * on ISO SmartCard Systems specification.
     *
     */
    protected void handleTagDiscovered(Tag tag) throws IOException {

        // Connect to the detected tag, setting a couple of settings
        IsoCard isoCard = AndroidCard.get(tag);
        if (isoCard == null) {
            throw new IsoDepNotSupportedException("Tag does not support ISO-DEP (ISO 14443-4)");
        }

        mJavacardDevice = new BaseJavacardDevice(new NfcTransport(isoCard));
        mJavacardDevice.connectToDevice();

        doNfcInBackground();
    }

    protected void handleUsbDevice(UsbDevice device) throws IOException {
        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
        mJavacardDevice = new BaseJavacardDevice(new UsbTransport(device, usbManager));
        mJavacardDevice.connectToDevice();

        doNfcInBackground();
    }

    public boolean isNfcConnected() {
        return mJavacardDevice.isConnected();
    }

    /**
     * Parses out the status word from a JavaCard response string.
     *
     * @param response A hex string with the response from the token
     * @return A short indicating the SW1/SW2, or 0 if a status could not be determined.
     */
    short parseCardStatus(String response) {
        if (response.length() < 4) {
            return 0; // invalid input
        }

        try {
            return Short.parseShort(response.substring(response.length() - 4), 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getHolderName(String name) {
        try {
            String slength;
            int ilength;
            name = name.substring(6);
            slength = name.substring(0, 2);
            ilength = Integer.parseInt(slength, 16) * 2;
            name = name.substring(2, ilength + 2);
            name = (new String(Hex.decode(name))).replace('<', ' ');
            return name;
        } catch (IndexOutOfBoundsException e) {
            // try-catch for https://github.com/FluffyKaon/OpenPGP-Card
            // Note: This should not happen, but happens with
            // https://github.com/FluffyKaon/OpenPGP-Card, thus return an empty string for now!

            Log.e(Constants.TAG, "Couldn't get holder name, returning empty string!", e);
            return "";
        }
    }

    public static String getHex(byte[] raw) {
        return new String(Hex.encode(raw));
    }

    public class IsoDepNotSupportedException extends IOException {

        public IsoDepNotSupportedException(String detailMessage) {
            super(detailMessage);
        }

    }

    public class CardException extends IOException {
        private short mResponseCode;

        public CardException(String detailMessage, short responseCode) {
            super(detailMessage);
            mResponseCode = responseCode;
        }

        public short getResponseCode() {
            return mResponseCode;
        }

    }

    /**
     * Ask user if she wants to install PGP onto her Fidesmo token
      */
    private void promptFidesmoPgpInstall() {
        FidesmoPgpInstallDialog fidesmoPgpInstallDialog = new FidesmoPgpInstallDialog();
        fidesmoPgpInstallDialog.show(getSupportFragmentManager(), "fidesmoPgpInstallDialog");
    }

    /**
     * Show a Dialog to the user informing that Fidesmo App must be installed and with option
     * to launch the Google Play store.
     */
    private void promptFidesmoAppInstall() {
        FidesmoInstallDialog fidesmoInstallDialog = new FidesmoInstallDialog();
        fidesmoInstallDialog.show(getSupportFragmentManager(), "fidesmoInstallDialog");
    }

    /**
     * Use the package manager to detect if an application is installed on the phone
     * @param uri an URI identifying the application's package
     * @return 'true' if the app is installed
     */
    private boolean isAndroidAppInstalled(String uri) {
        PackageManager mPackageManager = getPackageManager();
        boolean mAppInstalled;
        try {
            mPackageManager.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            mAppInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(Constants.TAG, "App not installed on Android device");
            mAppInstalled = false;
        }
        return mAppInstalled;
    }
}
