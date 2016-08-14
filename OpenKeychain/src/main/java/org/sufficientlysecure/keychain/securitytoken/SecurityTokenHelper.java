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
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;
import org.sufficientlysecure.keychain.util.Iso7816TLV;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.SecurityTokenUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPrivateCrtKey;

/**
 * This class provides a communication interface to OpenPGP applications on ISO SmartCard compliant
 * devices.
 * For the full specs, see http://g10code.com/docs/openpgp-card-2.0.pdf
 */
public class SecurityTokenHelper {
    private static final int MAX_APDU_NC = 255;
    private static final int MAX_APDU_NC_EXT = 65535;

    private static final int MAX_APDU_NE = 256;
    private static final int MAX_APDU_NE_EXT = 65536;

    private static final int APDU_SW_SUCCESS = 0x9000;
    private static final int APDU_SW1_RESPONSE_AVAILABLE = 0x61;

    private static final int MASK_CLA_CHAINING = 1 << 4;

    // Fidesmo constants
    private static final String FIDESMO_APPS_AID_PREFIX = "A000000617";

    private static final byte[] BLANK_FINGERPRINT = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private Transport mTransport;
    private CardCapabilities mCardCapabilities;
    private OpenPgpCapabilities mOpenPgpCapabilities;

    private Passphrase mPin;
    private Passphrase mAdminPin;
    private boolean mPw1ValidatedForSignature;
    private boolean mPw1ValidatedForDecrypt; // Mode 82 does other things; consider renaming?
    private boolean mPw3Validated;

    protected SecurityTokenHelper() {
    }

    public static SecurityTokenHelper getInstance() {
        return LazyHolder.SECURITY_TOKEN_HELPER;
    }

    private String getHolderName(byte[] name) {
        try {
            return (new String(name, 4, name[3])).replace('<', ' ');
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

        putKey(keyType, secretKey, passphrase);
        putData(keyType.getFingerprintObjectId(), secretKey.getFingerprint());
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
        mCardCapabilities = new CardCapabilities();

        mTransport.connect();

        // Connect on smartcard layer
        // Command APDU (page 51) for SELECT FILE command (page 29)
        CommandAPDU select = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, Hex.decode("D27600012401"));
        ResponseAPDU response = communicate(select);  // activate connection

        if (response.getSW() != APDU_SW_SUCCESS) {
            throw new CardException("Initialization failed!", response.getSW());
        }

        mOpenPgpCapabilities = new OpenPgpCapabilities(getData(0x00, 0x6E));
        mCardCapabilities = new CardCapabilities(mOpenPgpCapabilities.getHistoricalBytes());

        mPw1ValidatedForSignature = false;
        mPw1ValidatedForDecrypt = false;
        mPw3Validated = false;
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
        CommandAPDU changePin = new CommandAPDU(0x00, 0x24, 0x00, pw, Arrays.concatenate(pin, newPin));
        ResponseAPDU response = communicate(changePin);

        if (response.getSW() != APDU_SW_SUCCESS) {
            throw new CardException("Failed to change PIN", response.getSW());
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

        // Transmit
        byte[] data = Arrays.copyOfRange(encryptedSessionKey, 2, encryptedSessionKey.length);
        if (data[0] != 0) {
            data = Arrays.prepend(data, (byte) 0x00);
        }

        CommandAPDU command = new CommandAPDU(0x00, 0x2A, 0x80, 0x86, data, MAX_APDU_NE_EXT);
        ResponseAPDU response = communicate(command);

        if (response.getSW() != APDU_SW_SUCCESS) {
            throw new CardException("Deciphering with Security token failed on receive", response.getSW());
        }

        return response.getData();
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

            ResponseAPDU response = tryPin(mode, pin);// login
            if (response.getSW() != APDU_SW_SUCCESS) {
                throw new CardException("Bad PIN!", response.getSW());
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

        CommandAPDU command = new CommandAPDU(0x00, 0xDA, (dataObject & 0xFF00) >> 8, dataObject & 0xFF, data);
        ResponseAPDU response = communicate(command); // put data

        if (response.getSW() != APDU_SW_SUCCESS) {
            throw new CardException("Failed to put data.", response.getSW());
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
    private void putKey(KeyType slot, CanonicalizedSecretKey secretKey, Passphrase passphrase)
            throws IOException {
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


        // Now we're ready to communicate with the token.
        byte[] bytes = SecurityTokenUtils.createPrivKeyTemplate(crtSecretKey, slot,
                mOpenPgpCapabilities.getFormatForKeyType(slot));

        CommandAPDU apdu = new CommandAPDU(0x00, 0xDB, 0x3F, 0xFF, bytes);
        ResponseAPDU response = communicate(apdu);

        if (response.getSW() != APDU_SW_SUCCESS) {
            throw new CardException("Key export to Security Token failed", response.getSW());
        }
    }

    /**
     * Return fingerprints of all keys from application specific data stored
     * on tag, or null if data not available.
     *
     * @return The fingerprints of all subkeys in a contiguous byte array.
     */
    public byte[] getFingerprints() throws IOException {
        CommandAPDU apdu = new CommandAPDU(0x00, 0xCA, 0x00, 0x6E, MAX_APDU_NE_EXT);
        ResponseAPDU response = communicate(apdu);

        if (response.getSW() != APDU_SW_SUCCESS) {
            throw new CardException("Failed to get fingerprints", response.getSW());
        }

        Iso7816TLV[] tlvList = Iso7816TLV.readList(response.getData(), true);
        Iso7816TLV fingerPrintTlv = null;

        for (Iso7816TLV tlv : tlvList) {
            Log.d(Constants.TAG, "nfcGetFingerprints() Iso7816TLV tlv data:\n" + tlv.prettyPrint());

            Iso7816TLV matchingTlv = Iso7816TLV.findRecursive(tlv, 0xc5);
            if (matchingTlv != null) {
                fingerPrintTlv = matchingTlv;
            }
        }

        if (fingerPrintTlv == null) {
            return null;
        }
        return fingerPrintTlv.mV;
    }

    /**
     * Return the PW Status Bytes from the token. This is a simple DO; no TLV decoding needed.
     *
     * @return Seven bytes in fixed format, plus 0x9000 status word at the end.
     */
    private byte[] getPwStatusBytes() throws IOException {
        return getData(0x00, 0xC4);
    }

    public byte[] getAid() throws IOException {
        return getData(0x00, 0x4F);
    }

    public String getUserId() throws IOException {
        return getHolderName(getData(0x00, 0x65));
    }

    private byte[] getData(int p1, int p2) throws IOException {
        ResponseAPDU response = communicate(new CommandAPDU(0x00, 0xCA, p1, p2, MAX_APDU_NE_EXT));
        if (response.getSW() != APDU_SW_SUCCESS) {
            throw new CardException("Failed to get pw status bytes", response.getSW());
        }
        return response.getData();
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

        byte[] dsi;

        Log.i(Constants.TAG, "Hash: " + hashAlgo);
        switch (hashAlgo) {
            case HashAlgorithmTags.SHA1:
                if (hash.length != 20) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 10!");
                }
                dsi = Arrays.concatenate(Hex.decode(
                        "3021" // Tag/Length of Sequence, the 0x21 includes all following 33 bytes
                        + "3009" // Tag/Length of Sequence, the 0x09 are the following header bytes
                        + "0605" + "2B0E03021A" // OID of SHA1
                        + "0500" // TLV coding of ZERO
                        + "0414"), hash); // 0x14 are 20 hash bytes
                break;
            case HashAlgorithmTags.RIPEMD160:
                if (hash.length != 20) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 20!");
                }
                dsi = Arrays.concatenate(Hex.decode("3021300906052B2403020105000414"), hash);
                break;
            case HashAlgorithmTags.SHA224:
                if (hash.length != 28) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 28!");
                }
                dsi = Arrays.concatenate(Hex.decode("302D300D06096086480165030402040500041C"), hash);
                break;
            case HashAlgorithmTags.SHA256:
                if (hash.length != 32) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 32!");
                }
                dsi = Arrays.concatenate(Hex.decode("3031300D060960864801650304020105000420"), hash);
                break;
            case HashAlgorithmTags.SHA384:
                if (hash.length != 48) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 48!");
                }
                dsi = Arrays.concatenate(Hex.decode("3041300D060960864801650304020205000430"), hash);
                break;
            case HashAlgorithmTags.SHA512:
                if (hash.length != 64) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 64!");
                }
                dsi = Arrays.concatenate(Hex.decode("3051300D060960864801650304020305000440"), hash);
                break;
            default:
                throw new IOException("Not supported hash algo!");
        }

        // Command APDU for PERFORM SECURITY OPERATION: COMPUTE DIGITAL SIGNATURE (page 37)
        CommandAPDU command = new CommandAPDU(0x00, 0x2A, 0x9E, 0x9A, dsi, MAX_APDU_NE_EXT);
        ResponseAPDU response = communicate(command);

        if (response.getSW() != APDU_SW_SUCCESS) {
            throw new CardException("Failed to sign", response.getSW());
        }

        if (!mOpenPgpCapabilities.isPw1ValidForMultipleSignatures()) {
            mPw1ValidatedForSignature = false;
        }

        byte[] signature = response.getData();

        // Make sure the signature we received is actually the expected number of bytes long!
        if (signature.length != 128 && signature.length != 256
                && signature.length != 384 && signature.length != 512) {
            throw new IOException("Bad signature length! Expected 128/256/384/512 bytes, got " + signature.length);
        }

        return signature;
    }


    /**
     * Transceives APDU
     * Splits extended APDU into short APDUs and chains them if necessary
     * Performs GET RESPONSE command(ISO/IEC 7816-4 par.7.6.1) on retrieving if necessary
     * @param apdu short or extended APDU to transceive
     * @return response from the card
     * @throws IOException
     */
    private ResponseAPDU communicate(CommandAPDU apdu) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        ResponseAPDU lastResponse = null;
        // Transmit
        if (mCardCapabilities.hasExtended()) {
            lastResponse = mTransport.transceive(apdu);
        } else if (apdu.getData().length <= MAX_APDU_NC) {
            int ne = Math.min(apdu.getNe(), MAX_APDU_NE);
            lastResponse = mTransport.transceive(new CommandAPDU(apdu.getCLA(), apdu.getINS(),
                    apdu.getP1(), apdu.getP2(), apdu.getData(), ne));
        } else if (apdu.getData().length > MAX_APDU_NC && mCardCapabilities.hasChaining()) {
            int offset = 0;
            byte[] data = apdu.getData();
            int ne = Math.min(apdu.getNe(), MAX_APDU_NE);
            while (offset < data.length) {
                int curLen = Math.min(MAX_APDU_NC, data.length - offset);
                boolean last = offset + curLen >= data.length;
                int cla = apdu.getCLA() + (last ? 0 : MASK_CLA_CHAINING);

                lastResponse = mTransport.transceive(new CommandAPDU(cla, apdu.getINS(), apdu.getP1(),
                        apdu.getP2(), Arrays.copyOfRange(data, offset, offset + curLen), ne));

                if (!last && lastResponse.getSW() != APDU_SW_SUCCESS) {
                    throw new UsbTransportException("Failed to chain apdu");
                }

                offset += curLen;
            }
        }
        if (lastResponse == null) {
            throw new UsbTransportException("Can't transmit command");
        }

        result.write(lastResponse.getData());

        // Receive
        while (lastResponse.getSW1() == APDU_SW1_RESPONSE_AVAILABLE) {
            // GET RESPONSE ISO/IEC 7816-4 par.7.6.1
            CommandAPDU getResponse = new CommandAPDU(0x00, 0xC0, 0x00, 0x00, lastResponse.getSW2());
            lastResponse = mTransport.transceive(getResponse);
            result.write(lastResponse.getData());
        }

        result.write(lastResponse.getSW1());
        result.write(lastResponse.getSW2());

        return new ResponseAPDU(result.toByteArray());
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
                CommandAPDU apdu = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, Hex.decode(FIDESMO_APPS_AID_PREFIX));
                return communicate(apdu).getSW() == APDU_SW_SUCCESS;
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

        CommandAPDU apdu = new CommandAPDU(0x00, 0x47, 0x80, 0x00, new byte[]{(byte) slot, 0x00}, MAX_APDU_NE_EXT);
        ResponseAPDU response = communicate(apdu);

        if (response.getSW() != APDU_SW_SUCCESS) {
            throw new IOException("On-card key generation failed");
        }

        return response.getData();
    }

    private ResponseAPDU tryPin(int mode, byte[] pin) throws IOException {
        // Command APDU for VERIFY command (page 32)
        return communicate(new CommandAPDU(0x00, 0x20, 0x00, mode, pin));
    }

    /**
     * Resets security token, which deletes all keys and data objects.
     * This works by entering a wrong PIN and then Admin PIN 4 times respectively.
     * Afterwards, the token is reactivated.
     */
    public void resetAndWipeToken() throws IOException {
        // try wrong PIN 4 times until counter goes to C0
        byte[] pin = "XXXXXX".getBytes();
        for (int i = 0; i <= 4; i++) {
            ResponseAPDU response = tryPin(0x81, pin);
            if (response.getSW() == APDU_SW_SUCCESS) { // Should NOT accept!
                throw new CardException("Should never happen, XXXXXX has been accepted!", response.getSW());
            }
        }

        // try wrong Admin PIN 4 times until counter goes to C0
        byte[] adminPin = "XXXXXXXX".getBytes();
        for (int i = 0; i <= 4; i++) {
            ResponseAPDU response = tryPin(0x83, adminPin);
            if (response.getSW() == APDU_SW_SUCCESS) { // Should NOT accept!
                throw new CardException("Should never happen, XXXXXXXX has been accepted", response.getSW());
            }
        }

        // reactivate token!
        // NOTE: keep the order here! First execute _both_ reactivate commands. Before checking _both_ responses
        // If a token is in a bad state and reactivate1 fails, it could still be reactivated with reactivate2
        CommandAPDU reactivate1 = new CommandAPDU(0x00, 0xE6, 0x00, 0x00);
        CommandAPDU reactivate2 = new CommandAPDU(0x00, 0x44, 0x00, 0x00);
        ResponseAPDU response1 = communicate(reactivate1);
        ResponseAPDU response2 = communicate(reactivate2);
        if (response1.getSW() != APDU_SW_SUCCESS) {
            throw new CardException("Reactivating failed!", response1.getSW());
        }
        if (response2.getSW() != APDU_SW_SUCCESS) {
            throw new CardException("Reactivating failed!", response2.getSW());
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
