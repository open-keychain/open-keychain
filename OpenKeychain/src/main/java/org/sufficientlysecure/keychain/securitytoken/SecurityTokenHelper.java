/*
 * Copyright (C) 2016 Nikita Mikhailov <nikita.s.mikhailov@gmail.com>
 * Copyright (C) 2013-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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


package org.sufficientlysecure.keychain.securitytoken;

import android.support.annotation.NonNull;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.Iso7816TLV;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPrivateCrtKey;

import nordpol.Apdu;

/**
 * This class provides a communication interface to OpenPGP applications on ISO SmartCard compliant
 * devices.
 * For the full specs, see http://g10code.com/docs/openpgp-card-2.0.pdf
 */
public class SecurityTokenHelper {
    private static final int MAX_APDU_DATAFIELD_SIZE = 254;
    // Fidesmo constants
    private static final String FIDESMO_APPS_AID_PREFIX = "A000000617";

    private static final byte[] BLANK_FINGERPRINT = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private Transport mTransport;

    private Passphrase mPin;
    private Passphrase mAdminPin;
    private boolean mPw1ValidForMultipleSignatures;
    private boolean mPw1ValidatedForSignature;
    private boolean mPw1ValidatedForDecrypt; // Mode 82 does other things; consider renaming?
    private boolean mPw3Validated;

    protected SecurityTokenHelper() {
    }

    public static SecurityTokenHelper getInstance() {
        return LazyHolder.SECURITY_TOKEN_HELPER;
    }

    private static String getHex(byte[] raw) {
        return new String(Hex.encode(raw));
    }

    private String getHolderName(String name) {
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

    public Passphrase getPin() {
        return mPin;
    }

    public void setPin(final Passphrase pin) {
        this.mPin = pin;
    }

    public Passphrase getAdminPin() {
        return mAdminPin;
    }

    public void setAdminPin(final Passphrase adminPin) {
        this.mAdminPin = adminPin;
    }

    public void changeKey(CanonicalizedSecretKey secretKey, Passphrase passphrase) throws IOException {
        long keyGenerationTimestamp = secretKey.getCreationTime().getTime() / 1000;
        byte[] timestampBytes = ByteBuffer.allocate(4).putInt((int) keyGenerationTimestamp).array();
        KeyType keyType = KeyType.from(secretKey);

        if (keyType == null) {
            throw new IOException("Inappropriate key flags for smart card key.");
        }

        // Slot is empty, or contains this key already. PUT KEY operation is safe
        boolean canPutKey = isSlotEmpty(keyType)
                || keyMatchesFingerPrint(keyType, secretKey.getFingerprint());

        if (!canPutKey) {
            throw new IOException(String.format("Key slot occupied; card must be reset to put new %s key.",
                    keyType.toString()));
        }

        putKey(keyType.getmSlot(), secretKey, passphrase);
        putData(keyType.getmFingerprintObjectId(), secretKey.getFingerprint());
        putData(keyType.getTimestampObjectId(), timestampBytes);
    }

    private boolean isSlotEmpty(KeyType keyType) throws IOException {
        // Note: special case: This should not happen, but happens with
        // https://github.com/FluffyKaon/OpenPGP-Card, thus for now assume true
        if (getKeyFingerprint(keyType) == null) return true;

        return keyMatchesFingerPrint(keyType, BLANK_FINGERPRINT);
    }

    public boolean keyMatchesFingerPrint(KeyType keyType, byte[] fingerprint) throws IOException {
        return java.util.Arrays.equals(getKeyFingerprint(keyType), fingerprint);
    }

    /**
     * Connect to device and select pgp applet
     *
     * @throws IOException
     */
    public void connectToDevice() throws IOException {
        // Connect on transport layer
        mTransport.connect();

        // Connect on smartcard layer

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
        String response = communicate(opening);  // activate connection
        if (!response.endsWith(accepted)) {
            throw new CardException("Initialization failed!", parseCardStatus(response));
        }

        byte[] pwStatusBytes = getPwStatusBytes();
        mPw1ValidForMultipleSignatures = (pwStatusBytes[0] == 1);
        mPw1ValidatedForSignature = false;
        mPw1ValidatedForDecrypt = false;
        mPw3Validated = false;
    }

    /**
     * Parses out the status word from a JavaCard response string.
     *
     * @param response A hex string with the response from the card
     * @return A short indicating the SW1/SW2, or 0 if a status could not be determined.
     */
    private short parseCardStatus(String response) {
        if (response.length() < 4) {
            return 0; // invalid input
        }

        try {
            return Short.parseShort(response.substring(response.length() - 4), 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Modifies the user's PW1 or PW3. Before sending, the new PIN will be validated for
     * conformance to the token's requirements for key length.
     *
     * @param pw     For PW1, this is 0x81. For PW3 (Admin PIN), mode is 0x83.
     * @param newPin The new PW1 or PW3.
     */
    public void modifyPin(int pw, byte[] newPin) throws IOException {
        final int MAX_PW1_LENGTH_INDEX = 1;
        final int MAX_PW3_LENGTH_INDEX = 3;

        byte[] pwStatusBytes = getPwStatusBytes();

        if (pw == 0x81) {
            if (newPin.length < 6 || newPin.length > pwStatusBytes[MAX_PW1_LENGTH_INDEX]) {
                throw new IOException("Invalid PIN length");
            }
        } else if (pw == 0x83) {
            if (newPin.length < 8 || newPin.length > pwStatusBytes[MAX_PW3_LENGTH_INDEX]) {
                throw new IOException("Invalid PIN length");
            }
        } else {
            throw new IOException("Invalid PW index for modify PIN operation");
        }

        byte[] pin;
        if (pw == 0x83) {
            pin = mAdminPin.toStringUnsafe().getBytes();
        } else {
            pin = mPin.toStringUnsafe().getBytes();
        }

        // Command APDU for CHANGE REFERENCE DATA command (page 32)
        String changeReferenceDataApdu = "00" // CLA
                + "24" // INS
                + "00" // P1
                + String.format("%02x", pw) // P2
                + String.format("%02x", pin.length + newPin.length) // Lc
                + getHex(pin)
                + getHex(newPin);
        String response = communicate(changeReferenceDataApdu); // change PIN
        if (!response.equals("9000")) {
            throw new CardException("Failed to change PIN", parseCardStatus(response));
        }
    }

    /**
     * Call DECIPHER command
     *
     * @param encryptedSessionKey the encoded session key
     * @return the decoded session key
     */
    public byte[] decryptSessionKey(@NonNull byte[] encryptedSessionKey) throws IOException {
        if (!mPw1ValidatedForDecrypt) {
            verifyPin(0x82); // (Verify PW1 with mode 82 for decryption)
        }

        int offset = 1; // Skip first byte
        String response = "", status = "";

        // Transmit
        while (offset < encryptedSessionKey.length) {
            boolean isLastCommand = offset + MAX_APDU_DATAFIELD_SIZE < encryptedSessionKey.length;
            String cla = isLastCommand ? "10" : "00";

            int len = Math.min(MAX_APDU_DATAFIELD_SIZE, encryptedSessionKey.length - offset);
            response = communicate(cla + "2a8086" + Hex.toHexString(new byte[]{(byte) len})
                            + Hex.toHexString(encryptedSessionKey, offset, len));
            status = response.substring(response.length() - 4);

            if (!isLastCommand && !response.endsWith("9000")) {
                throw new CardException("Deciphering with Security token failed on transmit", parseCardStatus(response));
            }

            offset += MAX_APDU_DATAFIELD_SIZE;
        }

        // Receive
        String result = getDataField(response);
        while (response.endsWith("61")) {
            response = communicate("00C00000" + status.substring(2));
            status = response.substring(response.length() - 4);
            result += getDataField(response);
        }
        if (!status.equals("9000")) {
            throw new CardException("Deciphering with Security token failed on receive", parseCardStatus(response));
        }

        return Hex.decode(result);
    }

    /**
     * Verifies the user's PW1 or PW3 with the appropriate mode.
     *
     * @param mode For PW1, this is 0x81 for signing, 0x82 for everything else.
     *             For PW3 (Admin PIN), mode is 0x83.
     */
    private void verifyPin(int mode) throws IOException {
        if (mPin != null || mode == 0x83) {

            byte[] pin;
            if (mode == 0x83) {
                pin = mAdminPin.toStringUnsafe().getBytes();
            } else {
                pin = mPin.toStringUnsafe().getBytes();
            }

            // SW1/2 0x9000 is the generic "ok" response, which we expect most of the time.
            // See specification, page 51
            String accepted = "9000";
            String response = tryPin(mode, pin); // login
            if (!response.equals(accepted)) {
                throw new CardException("Bad PIN!", parseCardStatus(response));
            }

            if (mode == 0x81) {
                mPw1ValidatedForSignature = true;
            } else if (mode == 0x82) {
                mPw1ValidatedForDecrypt = true;
            } else if (mode == 0x83) {
                mPw3Validated = true;
            }
        }
    }

    /**
     * Stores a data object on the token. Automatically validates the proper PIN for the operation.
     * Supported for all data objects < 255 bytes in length. Only the cardholder certificate
     * (0x7F21) can exceed this length.
     *
     * @param dataObject The data object to be stored.
     * @param data       The data to store in the object
     */
    private void putData(int dataObject, byte[] data) throws IOException {
        if (data.length > 254) {
            throw new IOException("Cannot PUT DATA with length > 254");
        }
        if (dataObject == 0x0101 || dataObject == 0x0103) {
            if (!mPw1ValidatedForDecrypt) {
                verifyPin(0x82); // (Verify PW1 for non-signing operations)
            }
        } else if (!mPw3Validated) {
            verifyPin(0x83); // (Verify PW3)
        }

        String putDataApdu = "00" // CLA
                + "DA" // INS
                + String.format("%02x", (dataObject & 0xFF00) >> 8) // P1
                + String.format("%02x", dataObject & 0xFF) // P2
                + String.format("%02x", data.length) // Lc
                + getHex(data);

        String response = communicate(putDataApdu); // put data
        if (!response.equals("9000")) {
            throw new CardException("Failed to put data.", parseCardStatus(response));
        }
    }

    /**
     * Puts a key on the token in the given slot.
     *
     * @param slot The slot on the token where the key should be stored:
     *             0xB6: Signature Key
     *             0xB8: Decipherment Key
     *             0xA4: Authentication Key
     */
    private void putKey(int slot, CanonicalizedSecretKey secretKey, Passphrase passphrase)
            throws IOException {
        if (slot != 0xB6 && slot != 0xB8 && slot != 0xA4) {
            throw new IOException("Invalid key slot");
        }

        RSAPrivateCrtKey crtSecretKey;
        try {
            secretKey.unlock(passphrase);
            crtSecretKey = secretKey.getCrtSecretKey();
        } catch (PgpGeneralException e) {
            throw new IOException(e.getMessage());
        }

        // Shouldn't happen; the UI should block the user from getting an incompatible key this far.
        if (crtSecretKey.getModulus().bitLength() > 2048) {
            throw new IOException("Key too large to export to Security Token.");
        }

        // Should happen only rarely; all GnuPG keys since 2006 use public exponent 65537.
        if (!crtSecretKey.getPublicExponent().equals(new BigInteger("65537"))) {
            throw new IOException("Invalid public exponent for smart Security Token.");
        }

        if (!mPw3Validated) {
            verifyPin(0x83); // (Verify PW3 with mode 83)
        }

        byte[] header = Hex.decode(
                "4D82" + "03A2"      // Extended header list 4D82, length of 930 bytes. (page 23)
                        + String.format("%02x", slot) + "00" // CRT to indicate targeted key, no length
                        + "7F48" + "15"      // Private key template 0x7F48, length 21 (decimal, 0x15 hex)
                        + "9103"             // Public modulus, length 3
                        + "928180"           // Prime P, length 128
                        + "938180"           // Prime Q, length 128
                        + "948180"           // Coefficient (1/q mod p), length 128
                        + "958180"           // Prime exponent P (d mod (p - 1)), length 128
                        + "968180"           // Prime exponent Q (d mod (1 - 1)), length 128
                        + "97820100"         // Modulus, length 256, last item in private key template
                        + "5F48" + "820383");// DO 5F48; 899 bytes of concatenated key data will follow
        byte[] dataToSend = new byte[934];
        byte[] currentKeyObject;
        int offset = 0;

        System.arraycopy(header, 0, dataToSend, offset, header.length);
        offset += header.length;
        currentKeyObject = crtSecretKey.getPublicExponent().toByteArray();
        System.arraycopy(currentKeyObject, 0, dataToSend, offset, 3);
        offset += 3;
        // NOTE: For a 2048-bit key, these lengths are fixed. However, bigint includes a leading 0
        // in the array to represent sign, so we take care to set the offset to 1 if necessary.
        currentKeyObject = crtSecretKey.getPrimeP().toByteArray();
        System.arraycopy(currentKeyObject, currentKeyObject.length - 128, dataToSend, offset, 128);
        Arrays.fill(currentKeyObject, (byte) 0);
        offset += 128;
        currentKeyObject = crtSecretKey.getPrimeQ().toByteArray();
        System.arraycopy(currentKeyObject, currentKeyObject.length - 128, dataToSend, offset, 128);
        Arrays.fill(currentKeyObject, (byte) 0);
        offset += 128;
        currentKeyObject = crtSecretKey.getCrtCoefficient().toByteArray();
        System.arraycopy(currentKeyObject, currentKeyObject.length - 128, dataToSend, offset, 128);
        Arrays.fill(currentKeyObject, (byte) 0);
        offset += 128;
        currentKeyObject = crtSecretKey.getPrimeExponentP().toByteArray();
        System.arraycopy(currentKeyObject, currentKeyObject.length - 128, dataToSend, offset, 128);
        Arrays.fill(currentKeyObject, (byte) 0);
        offset += 128;
        currentKeyObject = crtSecretKey.getPrimeExponentQ().toByteArray();
        System.arraycopy(currentKeyObject, currentKeyObject.length - 128, dataToSend, offset, 128);
        Arrays.fill(currentKeyObject, (byte) 0);
        offset += 128;
        currentKeyObject = crtSecretKey.getModulus().toByteArray();
        System.arraycopy(currentKeyObject, currentKeyObject.length - 256, dataToSend, offset, 256);

        String putKeyCommand = "10DB3FFF";
        String lastPutKeyCommand = "00DB3FFF";

        // Now we're ready to communicate with the token.
        offset = 0;
        String response;
        while (offset < dataToSend.length) {
            int dataRemaining = dataToSend.length - offset;
            if (dataRemaining > 254) {
                response = communicate(
                        putKeyCommand + "FE" + Hex.toHexString(dataToSend, offset, 254)
                );
                offset += 254;
            } else {
                int length = dataToSend.length - offset;
                response = communicate(
                        lastPutKeyCommand + String.format("%02x", length)
                                + Hex.toHexString(dataToSend, offset, length));
                offset += length;
            }

            if (!response.endsWith("9000")) {
                throw new CardException("Key export to Security Token failed", parseCardStatus(response));
            }
        }

        // Clear array with secret data before we return.
        Arrays.fill(dataToSend, (byte) 0);
    }

    /**
     * Return fingerprints of all keys from application specific data stored
     * on tag, or null if data not available.
     *
     * @return The fingerprints of all subkeys in a contiguous byte array.
     */
    public byte[] getFingerprints() throws IOException {
        String data = "00CA006E00";
        byte[] buf = mTransport.transceive(Hex.decode(data));

        Iso7816TLV tlv = Iso7816TLV.readSingle(buf, true);
        Log.d(Constants.TAG, "nfcGetFingerprints() Iso7816TLV tlv data:\n" + tlv.prettyPrint());

        Iso7816TLV fptlv = Iso7816TLV.findRecursive(tlv, 0xc5);
        if (fptlv == null) {
            return null;
        }
        return fptlv.mV;
    }

    /**
     * Return the PW Status Bytes from the token. This is a simple DO; no TLV decoding needed.
     *
     * @return Seven bytes in fixed format, plus 0x9000 status word at the end.
     */
    private byte[] getPwStatusBytes() throws IOException {
        String data = "00CA00C400";
        return mTransport.transceive(Hex.decode(data));
    }

    public byte[] getAid() throws IOException {
        String info = "00CA004F00";
        return mTransport.transceive(Hex.decode(info));
    }

    public String getUserId() throws IOException {
        String info = "00CA006500";
        return getHolderName(communicate(info));
    }

    /**
     * Call COMPUTE DIGITAL SIGNATURE command and returns the MPI value
     *
     * @param hash the hash for signing
     * @return a big integer representing the MPI for the given hash
     */
    public byte[] calculateSignature(byte[] hash, int hashAlgo) throws IOException {
        if (!mPw1ValidatedForSignature) {
            verifyPin(0x81); // (Verify PW1 with mode 81 for signing)
        }

        // dsi, including Lc
        String dsi;

        Log.i(Constants.TAG, "Hash: " + hashAlgo);
        switch (hashAlgo) {
            case HashAlgorithmTags.SHA1:
                if (hash.length != 20) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 10!");
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
                    throw new IOException("Bad hash length (" + hash.length + ", expected 20!");
                }
                dsi = "233021300906052B2403020105000414" + getHex(hash);
                break;
            case HashAlgorithmTags.SHA224:
                if (hash.length != 28) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 28!");
                }
                dsi = "2F302D300D06096086480165030402040500041C" + getHex(hash);
                break;
            case HashAlgorithmTags.SHA256:
                if (hash.length != 32) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 32!");
                }
                dsi = "333031300D060960864801650304020105000420" + getHex(hash);
                break;
            case HashAlgorithmTags.SHA384:
                if (hash.length != 48) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 48!");
                }
                dsi = "433041300D060960864801650304020205000430" + getHex(hash);
                break;
            case HashAlgorithmTags.SHA512:
                if (hash.length != 64) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 64!");
                }
                dsi = "533051300D060960864801650304020305000440" + getHex(hash);
                break;
            default:
                throw new IOException("Not supported hash algo!");
        }

        // Command APDU for PERFORM SECURITY OPERATION: COMPUTE DIGITAL SIGNATURE (page 37)
        String apdu =
                "002A9E9A" // CLA, INS, P1, P2
                        + dsi // digital signature input
                        + "00"; // Le

        String response = communicate(apdu);

        if (response.length() < 4) {
            throw new CardException("Bad response", (short) 0);
        }
        // split up response into signature and status
        String status = response.substring(response.length() - 4);
        String signature = response.substring(0, response.length() - 4);

        // while we are getting 0x61 status codes, retrieve more data
        while (status.substring(0, 2).equals("61")) {
            Log.d(Constants.TAG, "requesting more data, status " + status);
            // Send GET RESPONSE command
            response = communicate("00C00000" + status.substring(2));
            status = response.substring(response.length() - 4);
            signature += response.substring(0, response.length() - 4);
        }

        Log.d(Constants.TAG, "final response:" + status);

        if (!mPw1ValidForMultipleSignatures) {
            mPw1ValidatedForSignature = false;
        }

        if (!"9000".equals(status)) {
            throw new CardException("Bad NFC response code: " + status, parseCardStatus(response));
        }

        // Make sure the signature we received is actually the expected number of bytes long!
        if (signature.length() != 256 && signature.length() != 512
                && signature.length() != 768 && signature.length() != 1024) {
            throw new IOException("Bad signature length! Expected 128/256/384/512 bytes, got " + signature.length() / 2);
        }

        return Hex.decode(signature);
    }

    /**
     * Transceive data via NFC encoded as Hex
     */
    private String communicate(String apdu) throws IOException {
        return getHex(mTransport.transceive(Hex.decode(apdu)));
    }

    public Transport getTransport() {
        return mTransport;
    }

    public void setTransport(Transport mTransport) {
        this.mTransport = mTransport;
    }

    public boolean isFidesmoToken() {
        if (isConnected()) { // Check if we can still talk to the card
            try {
                // By trying to select any apps that have the Fidesmo AID prefix we can
                // see if it is a Fidesmo device or not
                byte[] mSelectResponse = mTransport.transceive(Apdu.select(FIDESMO_APPS_AID_PREFIX));
                // Compare the status returned by our select with the OK status code
                return Apdu.hasStatus(mSelectResponse, Apdu.OK_APDU);
            } catch (IOException e) {
                Log.e(Constants.TAG, "Card communication failed!", e);
            }
        }
        return false;
    }

    /**
     * Generates a key on the card in the given slot. If the slot is 0xB6 (the signature key),
     * this command also has the effect of resetting the digital signature counter.
     * NOTE: This does not set the key fingerprint data object! After calling this command, you
     * must construct a public key packet using the returned public key data objects, compute the
     * key fingerprint, and store it on the card using: putData(0xC8, key.getFingerprint())
     *
     * @param slot The slot on the card where the key should be generated:
     *             0xB6: Signature Key
     *             0xB8: Decipherment Key
     *             0xA4: Authentication Key
     * @return the public key data objects, in TLV format. For RSA this will be the public modulus
     * (0x81) and exponent (0x82). These may come out of order; proper TLV parsing is required.
     */
    public byte[] generateKey(int slot) throws IOException {
        if (slot != 0xB6 && slot != 0xB8 && slot != 0xA4) {
            throw new IOException("Invalid key slot");
        }

        if (!mPw3Validated) {
            verifyPin(0x83); // (Verify PW3 with mode 83)
        }

        String generateKeyApdu = "0047800002" + String.format("%02x", slot) + "0000";
        String getResponseApdu = "00C00000";

        String first = communicate(generateKeyApdu);
        String second = communicate(getResponseApdu);

        if (!second.endsWith("9000")) {
            throw new IOException("On-card key generation failed");
        }

        String publicKeyData = getDataField(first) + getDataField(second);

        Log.d(Constants.TAG, "Public Key Data Objects: " + publicKeyData);

        return Hex.decode(publicKeyData);
    }

    private String getDataField(String output) {
        return output.substring(0, output.length() - 4);
    }

    private String tryPin(int mode, byte[] pin) throws IOException {
        // Command APDU for VERIFY command (page 32)
        String login =
                "00" // CLA
                        + "20" // INS
                        + "00" // P1
                        + String.format("%02x", mode) // P2
                        + String.format("%02x", pin.length) // Lc
                        + Hex.toHexString(pin);

        return communicate(login);
    }

    /**
     * Resets security token, which deletes all keys and data objects.
     * This works by entering a wrong PIN and then Admin PIN 4 times respectively.
     * Afterwards, the token is reactivated.
     */
    public void resetAndWipeToken() throws IOException {
        String accepted = "9000";

        // try wrong PIN 4 times until counter goes to C0
        byte[] pin = "XXXXXX".getBytes();
        for (int i = 0; i <= 4; i++) {
            String response = tryPin(0x81, pin);
            if (response.equals(accepted)) { // Should NOT accept!
                throw new CardException("Should never happen, XXXXXX has been accepted!", parseCardStatus(response));
            }
        }

        // try wrong Admin PIN 4 times until counter goes to C0
        byte[] adminPin = "XXXXXXXX".getBytes();
        for (int i = 0; i <= 4; i++) {
            String response = tryPin(0x83, adminPin);
            if (response.equals(accepted)) { // Should NOT accept!
                throw new CardException("Should never happen, XXXXXXXX has been accepted", parseCardStatus(response));
            }
        }

        // reactivate token!
        String reactivate1 = "00" + "e6" + "00" + "00";
        String reactivate2 = "00" + "44" + "00" + "00";
        String response1 = communicate(reactivate1);
        String response2 = communicate(reactivate2);
        if (!response1.equals(accepted) || !response2.equals(accepted)) {
            throw new CardException("Reactivating failed!", parseCardStatus(response1));
        }

    }

    /**
     * Return the fingerprint from application specific data stored on tag, or
     * null if it doesn't exist.
     *
     * @param keyType key type
     * @return The fingerprint of the requested key, or null if not found.
     */
    public byte[] getKeyFingerprint(@NonNull KeyType keyType) throws IOException {
        byte[] data = getFingerprints();
        if (data == null) {
            return null;
        }

        // return the master key fingerprint
        ByteBuffer fpbuf = ByteBuffer.wrap(data);
        byte[] fp = new byte[20];
        fpbuf.position(keyType.getIdx() * 20);
        fpbuf.get(fp, 0, 20);

        return fp;
    }

    public boolean isPersistentConnectionAllowed() {
        return mTransport != null && mTransport.isPersistentConnectionAllowed();
    }

    public boolean isConnected() {
        return mTransport != null && mTransport.isConnected();
    }

    private static class LazyHolder {
        private static final SecurityTokenHelper SECURITY_TOKEN_HELPER = new SecurityTokenHelper();
    }
}
