package org.sufficientlysecure.keychain.javacard;

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

public class BaseJavacardDevice implements JavacardDevice {
    private static final byte[] BLANK_FINGERPRINT = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private final Transport mTransport;

    private Passphrase mPin;
    private Passphrase mAdminPin;
    private boolean mPw1ValidForMultipleSignatures;
    private boolean mPw1ValidatedForSignature;
    private boolean mPw1ValidatedForDecrypt; // Mode 82 does other things; consider renaming?
    private boolean mPw3Validated;
    private boolean mTagHandlingEnabled;

    public BaseJavacardDevice(final Transport mTransport) {
        this.mTransport = mTransport;
    }

    private static String getHex(byte[] raw) {
        return new String(Hex.encode(raw));
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
        boolean canPutKey = !containsKey(keyType)
                || keyMatchesFingerPrint(keyType, secretKey.getFingerprint());
        if (!canPutKey) {
            throw new IOException(String.format("Key slot occupied; card must be reset to put new %s key.",
                    keyType.toString()));
        }

        nfcPutKey(keyType.getmSlot(), secretKey, passphrase);
        nfcPutData(keyType.getmFingerprintObjectId(), secretKey.getFingerprint());
        nfcPutData(keyType.getTimestampObjectId(), timestampBytes);
    }

    public boolean containsKey(KeyType keyType) throws IOException {
        return keyMatchesFingerPrint(keyType, BLANK_FINGERPRINT);
    }

    public boolean keyMatchesFingerPrint(KeyType keyType, byte[] fingerprint) throws IOException {
        return java.util.Arrays.equals(nfcGetFingerprint(keyType.getIdx()), fingerprint);
    }

    public void connectToDevice() throws IOException {
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
        String response = nfcCommunicate(opening);  // activate connection
        if (!response.endsWith(accepted)) {
            throw new CardException("Initialization failed!", parseCardStatus(response));
        }

        byte[] pwStatusBytes = nfcGetPwStatusBytes();
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

    /**
     * Modifies the user's PW1 or PW3. Before sending, the new PIN will be validated for
     * conformance to the card's requirements for key length.
     *
     * @param pinType For PW1, this is 0x81. For PW3 (Admin PIN), mode is 0x83.
     * @param newPin  The new PW1 or PW3.
     */
    public void nfcModifyPIN(PinType pinType, byte[] newPin) throws IOException {
        final int MAX_PW1_LENGTH_INDEX = 1;
        final int MAX_PW3_LENGTH_INDEX = 3;

        byte[] pwStatusBytes = nfcGetPwStatusBytes();
        byte[] oldPin;

        if (pinType == PinType.BASIC) {
            if (newPin.length < 6 || newPin.length > pwStatusBytes[MAX_PW1_LENGTH_INDEX]) {
                throw new IOException("Invalid PIN length");
            }
            oldPin = mPin.toStringUnsafe().getBytes();
        } else {
            if (newPin.length < 8 || newPin.length > pwStatusBytes[MAX_PW3_LENGTH_INDEX]) {
                throw new IOException("Invalid PIN length");
            }
            oldPin = mAdminPin.toStringUnsafe().getBytes();
        }

        // Command APDU for CHANGE REFERENCE DATA command (page 32)
        String changeReferenceDataApdu = "00" // CLA
                + "24" // INS
                + "00" // P1
                + String.format("%02x", pinType.getmMode()) // P2
                + String.format("%02x", oldPin.length + newPin.length) // Lc
                + getHex(oldPin)
                + getHex(newPin);
        String response = nfcCommunicate(changeReferenceDataApdu); // change PIN
        if (!response.equals("9000")) {
            throw new PinException("Failed to change PIN", parseCardStatus(response));
        }
    }

    /**
     * Calls to calculate the signature and returns the MPI value
     *
     * @param encryptedSessionKey the encoded session key
     * @return the decoded session key
     */
    public byte[] decryptSessionKey(byte[] encryptedSessionKey) throws IOException {
        if (!mPw1ValidatedForDecrypt) {
            nfcVerifyPIN(0x82); // (Verify PW1 with mode 82 for decryption)
        }

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

        String first = nfcCommunicate(firstApdu + getHex(one));
        String second = nfcCommunicate(secondApdu + getHex(two) + le);

        String decryptedSessionKey = nfcGetDataField(second);

        Log.d(Constants.TAG, "decryptedSessionKey: " + decryptedSessionKey);

        return Hex.decode(decryptedSessionKey);
    }

    /**
     * Verifies the user's PW1 or PW3 with the appropriate mode.
     *
     * @param mode For PW1, this is 0x81 for signing, 0x82 for everything else.
     *             For PW3 (Admin PIN), mode is 0x83.
     */
    public void nfcVerifyPIN(int mode) throws IOException {
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

            // Command APDU for VERIFY command (page 32)
            String login =
                    "00" // CLA
                            + "20" // INS
                            + "00" // P1
                            + String.format("%02x", mode) // P2
                            + String.format("%02x", pin.length) // Lc
                            + Hex.toHexString(pin);
            String response = nfcCommunicate(login); // login
            if (!response.equals(accepted)) {
                throw new PinException("Bad PIN!", parseCardStatus(response));
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
     * Stores a data object on the card. Automatically validates the proper PIN for the operation.
     * Supported for all data objects < 255 bytes in length. Only the cardholder certificate
     * (0x7F21) can exceed this length.
     *
     * @param dataObject The data object to be stored.
     * @param data       The data to store in the object
     */
    public void nfcPutData(int dataObject, byte[] data) throws IOException {
        if (data.length > 254) {
            throw new IOException("Cannot PUT DATA with length > 254");
        }
        if (dataObject == 0x0101 || dataObject == 0x0103) {
            if (!mPw1ValidatedForDecrypt) {
                nfcVerifyPIN(0x82); // (Verify PW1 for non-signing operations)
            }
        } else if (!mPw3Validated) {
            nfcVerifyPIN(0x83); // (Verify PW3)
        }

        String putDataApdu = "00" // CLA
                + "DA" // INS
                + String.format("%02x", (dataObject & 0xFF00) >> 8) // P1
                + String.format("%02x", dataObject & 0xFF) // P2
                + String.format("%02x", data.length) // Lc
                + getHex(data);

        String response = nfcCommunicate(putDataApdu); // put data
        if (!response.equals("9000")) {
            throw new CardException("Failed to put data.", parseCardStatus(response));
        }
    }

    /**
     * Puts a key on the card in the given slot.
     *
     * @param slot The slot on the card where the key should be stored:
     *             0xB6: Signature Key
     *             0xB8: Decipherment Key
     *             0xA4: Authentication Key
     */
    public void nfcPutKey(int slot, CanonicalizedSecretKey secretKey, Passphrase passphrase)
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
            throw new IOException("Key too large to export to smart card.");
        }

        // Should happen only rarely; all GnuPG keys since 2006 use public exponent 65537.
        if (!crtSecretKey.getPublicExponent().equals(new BigInteger("65537"))) {
            throw new IOException("Invalid public exponent for smart card key.");
        }

        if (!mPw3Validated) {
            nfcVerifyPIN(0x83); // (Verify PW3 with mode 83)
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

        // Now we're ready to communicate with the card.
        offset = 0;
        String response;
        while (offset < dataToSend.length) {
            int dataRemaining = dataToSend.length - offset;
            if (dataRemaining > 254) {
                response = nfcCommunicate(
                        putKeyCommand + "FE" + Hex.toHexString(dataToSend, offset, 254)
                );
                offset += 254;
            } else {
                int length = dataToSend.length - offset;
                response = nfcCommunicate(
                        lastPutKeyCommand + String.format("%02x", length)
                                + Hex.toHexString(dataToSend, offset, length));
                offset += length;
            }

            if (!response.endsWith("9000")) {
                throw new CardException("Key export to card failed", parseCardStatus(response));
            }
        }

        // Clear array with secret data before we return.
        Arrays.fill(dataToSend, (byte) 0);
    }

    /**
     * Return the key id from application specific data stored on tag, or null
     * if it doesn't exist.
     *
     * @param idx Index of the key to return the fingerprint from.
     * @return The long key id of the requested key, or null if not found.
     */
    public Long nfcGetKeyId(int idx) throws IOException {
        byte[] fp = nfcGetFingerprint(idx);
        if (fp == null) {
            return null;
        }
        ByteBuffer buf = ByteBuffer.wrap(fp);
        // skip first 12 bytes of the fingerprint
        buf.position(12);
        // the last eight bytes are the key id (big endian, which is default order in ByteBuffer)
        return buf.getLong();
    }

    /**
     * Return fingerprints of all keys from application specific data stored
     * on tag, or null if data not available.
     *
     * @return The fingerprints of all subkeys in a contiguous byte array.
     */
    public byte[] getFingerprints() throws IOException {
        String data = "00CA006E00";
        byte[] buf = mTransport.sendAndReceive(Hex.decode(data));

        Iso7816TLV tlv = Iso7816TLV.readSingle(buf, true);
        Log.d(Constants.TAG, "nfc tlv data:\n" + tlv.prettyPrint());

        Iso7816TLV fptlv = Iso7816TLV.findRecursive(tlv, 0xc5);
        if (fptlv == null) {
            return null;
        }
        return fptlv.mV;
    }

    /**
     * Return the PW Status Bytes from the card. This is a simple DO; no TLV decoding needed.
     *
     * @return Seven bytes in fixed format, plus 0x9000 status word at the end.
     */
    public byte[] nfcGetPwStatusBytes() throws IOException {
        String data = "00CA00C400";
        return mTransport.sendAndReceive(Hex.decode(data));
    }

    /**
     * Return the fingerprint from application specific data stored on tag, or
     * null if it doesn't exist.
     *
     * @param idx Index of the key to return the fingerprint from.
     * @return The fingerprint of the requested key, or null if not found.
     */
    public byte[] nfcGetFingerprint(int idx) throws IOException {
        byte[] data = getFingerprints();

        // return the master key fingerprint
        ByteBuffer fpbuf = ByteBuffer.wrap(data);
        byte[] fp = new byte[20];
        fpbuf.position(idx * 20);
        fpbuf.get(fp, 0, 20);

        return fp;
    }

    public byte[] getAid() throws IOException {
        String info = "00CA004F00";
        return mTransport.sendAndReceive(Hex.decode(info));
    }

    public String getUserId() throws IOException {
        String info = "00CA006500";
        return nfcGetHolderName(nfcCommunicate(info));
    }

    /**
     * Calls to calculate the signature and returns the MPI value
     *
     * @param hash the hash for signing
     * @return a big integer representing the MPI for the given hash
     */
    public byte[] nfcCalculateSignature(byte[] hash, int hashAlgo) throws IOException {
        if (!mPw1ValidatedForSignature) {
            nfcVerifyPIN(0x81); // (Verify PW1 with mode 81 for signing)
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

        String response = nfcCommunicate(apdu);

        // split up response into signature and status
        String status = response.substring(response.length() - 4);
        String signature = response.substring(0, response.length() - 4);

        // while we are getting 0x61 status codes, retrieve more data
        while (status.substring(0, 2).equals("61")) {
            Log.d(Constants.TAG, "requesting more data, status " + status);
            // Send GET RESPONSE command
            response = nfcCommunicate("00C00000" + status.substring(2));
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
        if (signature.length() != 256 && signature.length() != 512) {
            throw new IOException("Bad signature length! Expected 128 or 256 bytes, got " + signature.length() / 2);
        }

        return Hex.decode(signature);
    }

    public String nfcGetHolderName(String name) {
        String slength;
        int ilength;
        name = name.substring(6);
        slength = name.substring(0, 2);
        ilength = Integer.parseInt(slength, 16) * 2;
        name = name.substring(2, ilength + 2);
        name = (new String(Hex.decode(name))).replace('<', ' ');
        return (name);
    }

    private String nfcGetDataField(String output) {
        return output.substring(0, output.length() - 4);
    }

    public String nfcCommunicate(String apdu) throws IOException, TransportIoException {
        return getHex(mTransport.sendAndReceive(Hex.decode(apdu)));
    }

    public boolean isConnected() {
        return mTransport.isConnected();
    }
}
