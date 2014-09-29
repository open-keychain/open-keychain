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

import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Iso7816TLV;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * This class provides a communication interface to OpenPGP applications on ISO SmartCard compliant
 * NFC devices.
 *
 * For the full specs, see http://g10code.com/docs/openpgp-card-2.0.pdf
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
public class NfcActivity extends ActionBarActivity {

    // actions
    public static final String ACTION_SIGN_HASH = "sign_hash";
    public static final String ACTION_DECRYPT_SESSION_KEY = "decrypt_session_key";

    // always
    public static final String EXTRA_PIN = "pin";
    public static final String EXTRA_FINGERPRINT = "fingerprint";
    // special extra for OpenPgpService
    public static final String EXTRA_DATA = "data";

    // sign
    public static final String EXTRA_NFC_HASH_TO_SIGN = "nfc_hash";
    public static final String EXTRA_NFC_HASH_ALGO = "nfc_hash_algo";

    // decrypt
    public static final String EXTRA_NFC_ENC_SESSION_KEY = "encrypted_session_key";

    private Intent mServiceIntent;

    private static final int TIMEOUT = 100000;

    private NfcAdapter mNfcAdapter;
    private IsoDep mIsoDep;
    private String mAction;

    private String mPin;

    private byte[] mFingerprint;

    // sign
    private byte[] mHashToSign;
    private int mHashAlgo;

    // decrypt
    private byte[] mEncryptedSessionKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.TAG, "NfcActivity.onCreate");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.nfc_activity);

        Intent intent = getIntent();
        Bundle data = intent.getExtras();
        String action = intent.getAction();

        // TODO check fingerprint
        // mFingerprint = data.getByteArray(EXTRA_FINGERPRINT);

        if (ACTION_SIGN_HASH.equals(action)) {
            mAction = action;
            mPin = data.getString(EXTRA_PIN);
            mHashToSign = data.getByteArray(EXTRA_NFC_HASH_TO_SIGN);
            mHashAlgo = data.getInt(EXTRA_NFC_HASH_ALGO);
            mServiceIntent = data.getParcelable(EXTRA_DATA);

            Log.d(Constants.TAG, "NfcActivity mAction: " + mAction);
            Log.d(Constants.TAG, "NfcActivity mPin: " + mPin);
            Log.d(Constants.TAG, "NfcActivity mHashToSign as hex: " + getHex(mHashToSign));
            Log.d(Constants.TAG, "NfcActivity mServiceIntent: " + mServiceIntent.toString());
        } else if (ACTION_DECRYPT_SESSION_KEY.equals(action)) {
            mAction = action;
            mPin = data.getString(EXTRA_PIN);
            mEncryptedSessionKey = data.getByteArray(EXTRA_NFC_ENC_SESSION_KEY);
            mServiceIntent = data.getParcelable(EXTRA_DATA);

            Log.d(Constants.TAG, "NfcActivity mAction: " + mAction);
            Log.d(Constants.TAG, "NfcActivity mPin: " + mPin);
            Log.d(Constants.TAG, "NfcActivity mEncryptedSessionKey as hex: " + getHex(mEncryptedSessionKey));
            Log.d(Constants.TAG, "NfcActivity mServiceIntent: " + mServiceIntent.toString());
        } else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            Log.e(Constants.TAG, "This should not happen! NfcActivity.onCreate() is being called instead of onNewIntent()!");
            toast("This should not happen! Please create a new bug report that the NFC screen is restarted!");
            finish();
        } else {
            Log.d(Constants.TAG, "Action not supported: " + action);
        }
    }

    /**
     * Called when the system is about to start resuming a previous activity,
     * disables NFC Foreground Dispatch
     */
    public void onPause() {
        super.onPause();
        Log.d(Constants.TAG, "NfcActivity.onPause");

        disableNfcForegroundDispatch();
    }

    /**
     * Called when the activity will start interacting with the user,
     * enables NFC Foreground Dispatch
     */
    public void onResume() {
        super.onResume();
        Log.d(Constants.TAG, "NfcActivity.onResume");

        enableNfcForegroundDispatch();
    }

    /**
     * This activity is started as a singleTop activity.
     * All new NFC Intents which are delivered to this activity are handled here
     */
    public void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            try {
                handleNdefDiscoveredIntent(intent);
            } catch (IOException e) {
                Log.e(Constants.TAG, "Connection error!", e);
                toast("Connection Error: " + e.getMessage());
                setResult(RESULT_CANCELED, mServiceIntent);
                finish();
            }
        }
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
    private void handleNdefDiscoveredIntent(Intent intent) throws IOException {

        Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        // Connect to the detected tag, setting a couple of settings
        mIsoDep = IsoDep.get(detectedTag);
        mIsoDep.setTimeout(TIMEOUT); // timeout is set to 100 seconds to avoid cancellation during calculation
        mIsoDep.connect();

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
        if ( ! card(opening).equals(accepted)) { // activate connection
            toast("Opening Error!");
            setResult(RESULT_CANCELED, mServiceIntent);
            finish();
            return;
        }

        // Command APDU for VERIFY command (page 32)
        String login =
              "00" // CLA
            + "20" // INS
            + "00" // P1
            + "82" // P2 (PW1)
            + String.format("%02x", mPin.length()) // Lc
            + Hex.toHexString(mPin.getBytes());
        if ( ! card(login).equals(accepted)) { // login
            toast("Wrong PIN!");
            setResult(RESULT_CANCELED, mServiceIntent);
            finish();
            return;
        }

        if (ACTION_SIGN_HASH.equals(mAction)) {
            // returns signed hash
            byte[] signedHash = nfcCalculateSignature(mHashToSign, mHashAlgo);

            if (signedHash == null) {
                setResult(RESULT_CANCELED, mServiceIntent);
                finish();
                return;
            }

            Log.d(Constants.TAG, "NfcActivity signedHash as hex: " + getHex(signedHash));

            // give data through for new service call
            // OpenPgpApi.EXTRA_NFC_SIGNED_HASH
            mServiceIntent.putExtra("nfc_signed_hash", signedHash);
            setResult(RESULT_OK, mServiceIntent);
            finish();

        } else if (ACTION_DECRYPT_SESSION_KEY.equals(mAction)) {
            byte[] decryptedSessionKey = nfcDecryptSessionKey(mEncryptedSessionKey);

            // give data through for new service call
            // OpenPgpApi.EXTRA_NFC_DECRYPTED_SESSION_KEY
            mServiceIntent.putExtra("nfc_decrypted_session_key", decryptedSessionKey);
            setResult(RESULT_OK, mServiceIntent);
            finish();
        }
    }

    /**
     * Gets the user ID
     *
     * @return the user id as "name <email>"
     * @throws java.io.IOException
     */
    public String getUserId() throws IOException {
        String info = "00CA006500";
        String data = "00CA005E00";
        return getName(card(info)) + " <" + (new String(Hex.decode(getDataField(card(data))))) + ">";
    }

    /** Return the key id from application specific data stored on tag, or null
     * if it doesn't exist.
     *
     * @param idx Index of the key to return the fingerprint from.
     * @return The long key id of the requested key, or null if not found.
     */
    public static Long nfcGetKeyId(IsoDep isoDep, int idx) throws IOException {
        byte[] fp = nfcGetFingerprint(isoDep, idx);
        if (fp == null) {
            return null;
        }
        ByteBuffer buf = ByteBuffer.wrap(fp);
        // skip first 12 bytes of the fingerprint
        buf.position(12);
        // the last eight bytes are the key id (big endian, which is default order in ByteBuffer)
        return buf.getLong();
    }

    /** Return fingerprints of all keys from application specific data stored
     * on tag, or null if data not available.
     *
     * @return The fingerprints of all subkeys in a contiguous byte array.
     */
    public static byte[] nfcGetFingerprints(IsoDep isoDep) throws IOException {
        String data = "00CA006E00";
        byte[] buf = isoDep.transceive(Hex.decode(data));

        Iso7816TLV tlv = Iso7816TLV.readSingle(buf, true);
        Log.d(Constants.TAG, "nfc tlv data:\n" + tlv.prettyPrint());

        Iso7816TLV fptlv = Iso7816TLV.findRecursive(tlv, 0xc5);
        if (fptlv == null) {
            return null;
        }

        return fptlv.mV;
    }

    /** Return the fingerprint from application specific data stored on tag, or
     * null if it doesn't exist.
     *
     * @param idx Index of the key to return the fingerprint from.
     * @return The fingerprint of the requested key, or null if not found.
     */
    public static byte[] nfcGetFingerprint(IsoDep isoDep, int idx) throws IOException {
        byte[] data = nfcGetFingerprints(isoDep);

        // return the master key fingerprint
        ByteBuffer fpbuf = ByteBuffer.wrap(data);
        byte[] fp = new byte[20];
        fpbuf.get(fp, 20*idx, 20);

        return fp;
    }

    /**
     * Calls to calculate the signature and returns the MPI value
     *
     * @param hash the hash for signing
     * @return a big integer representing the MPI for the given hash
     * @throws java.io.IOException
     */
    public byte[] nfcCalculateSignature(byte[] hash, int hashAlgo) throws IOException {

        // dsi, including Lc
        String dsi;

        Log.i(Constants.TAG, "Hash: " + hashAlgo);
        switch (hashAlgo) {
            case HashAlgorithmTags.SHA1:
                if (hash.length != 20) {
                    throw new RuntimeException("Bad hash length (" + hash.length + ", expected 10!");
                }
                dsi = "23" // Lc
                        + "3021" // Tag/Length of Sequence, the 0x21 includes all following 33 bytes
                        + "3009" // Tag/Length of Sequence, the 0x09 are the following header bytes
                        + "0605" + "2B0E03021A" // OID of SHA1
                        + "0500" // TLV coding of ZERO
                        + "0414" + getHex(hash); // 0x14 are 20 hash bytes
                break;
            case HashAlgorithmTags.RIPEMD160:
                if (hash.length != 20) {
                    throw new RuntimeException("Bad hash length (" + hash.length + ", expected 20!");
                }
                dsi = "233021300906052B2403020105000414" + getHex(hash);
                break;
            case HashAlgorithmTags.SHA224:
                if (hash.length != 28) {
                    throw new RuntimeException("Bad hash length (" + hash.length + ", expected 28!");
                }
                dsi = "2F302D300D06096086480165030402040500041C" + getHex(hash);
                break;
            case HashAlgorithmTags.SHA256:
                if (hash.length != 32) {
                    throw new RuntimeException("Bad hash length (" + hash.length + ", expected 32!");
                }
                dsi = "333031300D060960864801650304020105000420" + getHex(hash);
                break;
            case HashAlgorithmTags.SHA384:
                if (hash.length != 48) {
                    throw new RuntimeException("Bad hash length (" + hash.length + ", expected 48!");
                }
                dsi = "433041300D060960864801650304020205000430" + getHex(hash);
                break;
            case HashAlgorithmTags.SHA512:
                if (hash.length != 64) {
                    throw new RuntimeException("Bad hash length (" + hash.length + ", expected 64!");
                }
                dsi = "533051300D060960864801650304020305000440" + getHex(hash);
                break;
            default:
                throw new RuntimeException("Not supported hash algo!");
        }

        // Command APDU for PERFORM SECURITY OPERATION: COMPUTE DIGITAL SIGNATURE (page 37)
        String apdu  =
                  "002A9E9A" // CLA, INS, P1, P2
                + dsi // digital signature input
                + "00"; // Le

        String response = card(apdu);

        // split up response into signature and status
        String status = response.substring(response.length()-4);
        String signature = response.substring(0, response.length() - 4);

        // while we are getting 0x61 status codes, retrieve more data
        while (status.substring(0, 2).equals("61")) {
            Log.d(Constants.TAG, "requesting more data, status " + status);
            // Send GET RESPONSE command
            response = card("00C00000" + status.substring(2));
            status = response.substring(response.length()-4);
            signature += response.substring(0, response.length()-4);
        }

        Log.d(Constants.TAG, "final response:" + status);

        if ( ! status.equals("9000")) {
            toast("Bad NFC response code: " + status);
            return null;
        }

        // Make sure the signature we received is actually the expected number of bytes long!
        if (signature.length() != 256 && signature.length() != 512) {
            toast("Bad signature length! Expected 128 or 256 bytes, got " + signature.length() / 2);
            return null;
        }

        return Hex.decode(signature);
    }

    /**
     * Calls to calculate the signature and returns the MPI value
     *
     * @param encryptedSessionKey the encoded session key
     * @return the decoded session key
     * @throws java.io.IOException
     */
    public byte[] nfcDecryptSessionKey(byte[] encryptedSessionKey) throws IOException {
        String firstApdu = "102a8086fe";
        String secondApdu = "002a808603";
        String le = "00";

        byte[] one = new byte[254];
        // leave out first byte:
        System.arraycopy(encryptedSessionKey, 1, one, 0, one.length);

        byte[] two = new byte[encryptedSessionKey.length - 1 - one.length];
        for (int i = 0; i < two.length; i++) {
            two[i] = encryptedSessionKey[i + one.length + 1];
        }

        String first = card(firstApdu + getHex(one));
        String second = card(secondApdu + getHex(two) + le);

        String decryptedSessionKey = getDataField(second);

        Log.d(Constants.TAG, "decryptedSessionKey: " + decryptedSessionKey);

        return Hex.decode(decryptedSessionKey);
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
        Intent nfcI = new Intent(this, NfcActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent nfcPendingIntent = PendingIntent.getActivity(this, 0, nfcI, PendingIntent.FLAG_CANCEL_CURRENT);
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
    public String getName(String name) {
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
    private String getDataField(String output) {
        return output.substring(0, output.length() - 4);
    }

    /**
     * Communicates with the OpenPgpCard via the APDU
     *
     * @param hex the hexadecimal APDU
     * @return The answer from the card
     * @throws java.io.IOException throws an exception if something goes wrong
     */
    public String card(String hex) throws IOException {
        return getHex(mIsoDep.transceive(Hex.decode(hex)));
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
