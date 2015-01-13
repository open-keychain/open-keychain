/**
 * Copyright (c) 2013-2014 Philipp Jakubeit, Signe Rüsch, Dominik Schürmann
 *
 * Licensed under the Bouncy Castle License (MIT license). See LICENSE file for details.
 */

package org.sufficientlysecure.keychain.ui;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.WindowManager;
import android.widget.Toast;

import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Iso7816TLV;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class provides a communication interface to OpenPGP applications on ISO SmartCard compliant
 * NFC devices.
 *
 * For the full specs, see http://g10code.com/docs/openpgp-card-2.0.pdf
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
public class NfcIntentActivity extends BaseActivity {

    // special extra for OpenPgpService
    public static final String EXTRA_DATA = "data";

    private Intent mServiceIntent;

    private static final int TIMEOUT = 100000;

    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.TAG, "NfcActivity.onCreate");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        Bundle data = intent.getExtras();
        String action = intent.getAction();

        Log.d(Constants.TAG, action);
        Log.d(Constants.TAG, intent.getDataString());

        // TODO check fingerprint
        // mFingerprint = data.getByteArray(EXTRA_FINGERPRINT);

        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Log.d(Constants.TAG, "Action not supported: " + action);
            finish();
        }

        try {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            // Connect to the detected tag, setting a couple of settings
            IsoDep isoDep = IsoDep.get(detectedTag);
            isoDep.setTimeout(TIMEOUT); // timeout is set to 100 seconds to avoid cancellation during calculation
            isoDep.connect();

            nfcGreet(isoDep);
            // nfcPin(isoDep, "yoloswag");
            nfcGetFingerprint(isoDep);

        } catch (IOException e) {
            Log.e(Constants.TAG, "IOException!", e);
            finish();
        }
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.nfc_activity);
    }

    /**
     * Called when the system is about to start resuming a previous activity,
     * disables NFC Foreground Dispatch
     */
    public void onPause() {
        super.onPause();
        Log.d(Constants.TAG, "NfcActivity.onPause");

        // disableNfcForegroundDispatch();
    }

    /**
     * Called when the activity will start interacting with the user,
     * enables NFC Foreground Dispatch
     */
    public void onResume() {
        super.onResume();
        Log.d(Constants.TAG, "NfcActivity.onResume");

        // enableNfcForegroundDispatch();
    }

    /** Handle NFC communication and return a result.
     *
     * This method is called by onNewIntent above upon discovery of an NFC tag.
     * It handles initialization and login to the application, subsequently
     * calls either nfcCalculateSignature() or nfcDecryptSessionKey(), then
     * finishes the activity with an appropiate result.
     *
     * On general communication, see also
     * http://www.cardwerk.com/smartcards/smartcard_standard_ISO7816-4_annex-a.aspx
     *
     * References to pages are generally related to the OpenPGP Application
     * on ISO SmartCard Systems specification.
     *
     */
    private void nfcGreet(IsoDep isoDep) throws IOException {

        // SW1/2 0x9000 is the generic "ok" response, which we expect most of the time.
        // See specification, page 51
        String accepted = "9000";

        // Command APDU (page 51) for SELECT FILE command (page 29)
        String opening =
                "00" // CLA
                        + "A4" // INS
                        + "04" // P1
                        + "00" // P2
                        + "06" // Lc (number of bytes)
                        + "D27600012401" // Data (6 bytes)
                        + "00"; // Le
        if (!card(isoDep, opening).equals(accepted)) { // activate connection
            toast("Opening Error!");
            setResult(RESULT_CANCELED, mServiceIntent);
            finish();
        }
    }

    private void nfcPin(IsoDep isoDep, String pin) throws IOException {

        String data = "00CA006E00";
        String fingerprint = card(isoDep, data);

        // Command APDU for VERIFY command (page 32)
        String login =
              "00" // CLA
            + "20" // INS
            + "00" // P1
            + "82" // P2 (PW1)
            + String.format("%02x", pin.length()) // Lc
            + Hex.toHexString(pin.getBytes());
        if ( ! card(isoDep, login).equals("9000")) { // login
            toast("Pin Error!");
            setResult(RESULT_CANCELED, mServiceIntent);
            finish();
        }

    }

    /**
     * Gets the user ID
     *
     * @return the user id as "name <email>"
     * @throws java.io.IOException
     */
    public static String getUserId(IsoDep isoDep) throws IOException {
        String info = "00CA006500";
        String data = "00CA005E00";
        return getName(card(isoDep, info)) + " <" + (new String(Hex.decode(getDataField(card(isoDep, data))))) + ">";
    }

    /**
     * Gets the long key ID
     *
     * @return the long key id (last 16 bytes of the fingerprint)
     * @throws java.io.IOException
     */
    public static long getKeyId(IsoDep isoDep) throws IOException {
        String keyId = nfcGetFingerprint(isoDep).substring(24);
        Log.d(Constants.TAG, "keyId: " + keyId);
        return Long.parseLong(keyId, 16);
    }

    /**
     * Gets the fingerprint of the signature key
     *
     * @return the fingerprint
     * @throws java.io.IOException
     */
    public static String nfcGetFingerprint(IsoDep isoDep) throws IOException {
        String data = "00CA006E00";
        byte[] buf = isoDep.transceive(Hex.decode(data));

        Iso7816TLV tlv = Iso7816TLV.readSingle(buf, true);
        Log.d(Constants.TAG, "nfc tlv data:\n" + tlv.prettyPrint());

        Iso7816TLV fptlv = Iso7816TLV.findRecursive(tlv, 0xc5);
        if (fptlv != null) {
            ByteBuffer fpbuf = ByteBuffer.wrap(fptlv.mV);
            byte[] fp = new byte[20];
            fpbuf.get(fp);
            Log.d(Constants.TAG, "fingerprint 1: " + KeyFormattingUtils.convertFingerprintToHex(fp));
            fpbuf.get(fp);
            Log.d(Constants.TAG, "fingerprint 2: " + KeyFormattingUtils.convertFingerprintToHex(fp));
            fpbuf.get(fp);
            Log.d(Constants.TAG, "fingerprint 3: " + KeyFormattingUtils.convertFingerprintToHex(fp));
        }

        return "nope";
    }

    /**
     * Prints a message to the screen
     *
     * @param text the text which should be contained within the toast
     */
    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    /**
     * Receive new NFC Intents to this activity only by enabling foreground dispatch.
     * This can only be done in onResume!
     */
    public void enableNfcForegroundDispatch() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        Intent nfcI = new Intent(this, NfcIntentActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent nfcPendingIntent = PendingIntent.getActivity(this, 0, nfcI, 0);
        IntentFilter[] writeTagFilters = new IntentFilter[]{
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        };

        // https://code.google.com/p/android/issues/detail?id=62918
        // maybe mNfcAdapter.enableReaderMode(); ?
        try {
            mNfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, writeTagFilters, null);
        } catch (IllegalStateException e) {
            Log.i(Constants.TAG, "NfcForegroundDispatch Error!", e);
        }
        Log.d(Constants.TAG, "NfcForegroundDispatch has been enabled!");
    }

    /**
     * Disable foreground dispatch in onPause!
     */
    public void disableNfcForegroundDispatch() {
        mNfcAdapter.disableForegroundDispatch(this);
        Log.d(Constants.TAG, "NfcForegroundDispatch has been disabled!");
    }

    /**
     * Gets the name of the user out of the raw card output regarding card holder related data
     *
     * @param name the raw card holder related data from the card
     * @return the name given in this data
     */
    public static  String getName(String name) {
        String slength;
        int ilength;
        name = name.substring(6);
        slength = name.substring(0, 2);
        ilength = Integer.parseInt(slength, 16) * 2;
        name = name.substring(2, ilength + 2);
        name = (new String(Hex.decode(name))).replace('<', ' ');
        return (name);
    }

    /**
     * Reduces the raw data from the card by four characters
     *
     * @param output the raw data from the card
     * @return the data field of that data
     */
    private static String getDataField(String output) {
        return output.substring(0, output.length() - 4);
    }

    /**
     * Communicates with the OpenPgpCard via the APDU
     *
     * @param hex the hexadecimal APDU
     * @return The answer from the card
     * @throws java.io.IOException throws an exception if something goes wrong
     */
    public static String card(IsoDep isoDep, String hex) throws IOException {
        return getHex(isoDep.transceive(Hex.decode(hex)));
    }

    /**
     * Converts a byte array into an hex string
     *
     * @param raw the byte array representation
     * @return the  hexadecimal string representation
     */
    public static String getHex(byte[] raw) {
        return new String(Hex.encode(raw));
    }

}
