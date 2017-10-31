/*
 * Copyright (C) 2016 Nikita Mikhailov <nikita.s.mikhailov@gmail.com>
 * Copyright (C) 2013-2017 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.jcajce.util.MessageDigestUtils;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.operator.PGPPad;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TokenType;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TransportType;
import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.List;


/**
 * This class provides a communication interface to OpenPGP applications on ISO SmartCard compliant
 * devices.
 * For the full specs, see http://g10code.com/docs/openpgp-card-2.0.pdf
 */
public class SecurityTokenConnection {
    private static final int APDU_SW1_RESPONSE_AVAILABLE = 0x61;

    private static final String AID_PREFIX_FIDESMO = "A000000617";

    private static final byte[] BLANK_FINGERPRINT = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private static SecurityTokenConnection sCachedInstance;

    private final JcaKeyFingerprintCalculator fingerprintCalculator = new JcaKeyFingerprintCalculator();

    @NonNull
    private final Transport mTransport;
    @NonNull
    private final Passphrase mPin;
    private final OpenPgpCommandApduFactory commandFactory;

    private TokenType tokenType;
    private CardCapabilities mCardCapabilities;
    private OpenPgpCapabilities mOpenPgpCapabilities;
    private SecureMessaging mSecureMessaging;

    private boolean mPw1ValidatedForSignature;
    private boolean mPw1ValidatedForDecrypt; // Mode 82 does other things; consider renaming?
    private boolean mPw3Validated;

    public static SecurityTokenConnection getInstanceForTransport(Transport transport, Passphrase pin) {
        if (sCachedInstance == null || !sCachedInstance.isPersistentConnectionAllowed() ||
                !sCachedInstance.isConnected() || !sCachedInstance.mTransport.equals(transport)) {
            sCachedInstance = new SecurityTokenConnection(transport, pin, new OpenPgpCommandApduFactory());
        }
        return sCachedInstance;
    }

    @VisibleForTesting
    SecurityTokenConnection(@NonNull Transport transport, @NonNull Passphrase pin,
            OpenPgpCommandApduFactory commandFactory) {
        this.mTransport = transport;
        this.mPin = pin;

        this.commandFactory = commandFactory;
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

    public void changeKey(CanonicalizedSecretKey secretKey, Passphrase passphrase, Passphrase adminPin) throws IOException {
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

        putKey(keyType, secretKey, passphrase, adminPin);
        putData(adminPin, keyType.getFingerprintObjectId(), secretKey.getFingerprint());
        putData(adminPin, keyType.getTimestampObjectId(), timestampBytes);
    }

    private boolean isSlotEmpty(KeyType keyType) throws IOException {
        // Note: special case: This should not happen, but happens with
        // https://github.com/FluffyKaon/OpenPGP-Card, thus for now assume true
        if (getKeyFingerprint(keyType) == null) {
            return true;
        }

        return keyMatchesFingerPrint(keyType, BLANK_FINGERPRINT);
    }

    private boolean keyMatchesFingerPrint(KeyType keyType, byte[] fingerprint) throws IOException {
        return java.util.Arrays.equals(getKeyFingerprint(keyType), fingerprint);
    }

    public void connectIfNecessary(Context context) throws IOException {
        if (isConnected()) {
            return;
        }

        connectToDevice(context);
    }

    /**
     * Connect to device and select pgp applet
     */
    @VisibleForTesting
    void connectToDevice(Context context) throws IOException {
        // Connect on transport layer
        mCardCapabilities = new CardCapabilities();

        mTransport.connect();

        determineTokenType();

        CommandApdu select = commandFactory.createSelectFileOpenPgpCommand();
        ResponseApdu response = communicate(select);  // activate connection

        if (!response.isSuccess()) {
            throw new CardException("Initialization failed!", response.getSw());
        }

        refreshConnectionCapabilities();

        mPw1ValidatedForSignature = false;
        mPw1ValidatedForDecrypt = false;
        mPw3Validated = false;

        if (mOpenPgpCapabilities.isHasSCP11bSM()) {
            try {
                SCP11bSecureMessaging.establish(this, context, commandFactory);
            } catch (SecureMessagingException e) {
                mSecureMessaging = null;
                Log.e(Constants.TAG, "failed to establish secure messaging", e);
            }
        }
    }

    @VisibleForTesting
    void determineTokenType() throws IOException {
        tokenType = mTransport.getTokenTypeIfAvailable();
        if (tokenType != null) {
            return;
        }

        CommandApdu selectFidesmoApdu = commandFactory.createSelectFileCommand(AID_PREFIX_FIDESMO);
        if (communicate(selectFidesmoApdu).isSuccess()) {
            tokenType = TokenType.FIDESMO;
            return;
        }

        /* We could determine if this is a yubikey here. The info isn't used at the moment, so we save the roundtrip
        // AID from https://github.com/Yubico/ykneo-oath/blob/master/build.xml#L16
        CommandApdu selectYubicoApdu = commandFactory.createSelectFileCommand("A000000527200101");
        if (communicate(selectYubicoApdu).isSuccess()) {
            tokenType = TokenType.YUBIKEY_UNKNOWN;
            return;
        }
        */

        tokenType = TokenType.UNKNOWN;
    }

    private void refreshConnectionCapabilities() throws IOException {
        byte[] rawOpenPgpCapabilities = getData(0x00, 0x6E);

        OpenPgpCapabilities openPgpCapabilities = new OpenPgpCapabilities(rawOpenPgpCapabilities);
        setConnectionCapabilities(openPgpCapabilities);
    }

    @VisibleForTesting
    void setConnectionCapabilities(OpenPgpCapabilities openPgpCapabilities) throws IOException {
        this.mOpenPgpCapabilities = openPgpCapabilities;
        this.mCardCapabilities = new CardCapabilities(openPgpCapabilities.getHistoricalBytes());
    }

    public void resetPin(byte[] newPin, Passphrase adminPin) throws IOException {
        if (!mPw3Validated) {
            verifyAdminPin(adminPin);
        }

        final int MAX_PW1_LENGTH_INDEX = 1;
        byte[] pwStatusBytes = getPwStatusBytes();
        if (newPin.length < 6 || newPin.length > pwStatusBytes[MAX_PW1_LENGTH_INDEX]) {
            throw new IOException("Invalid PIN length");
        }

        // Command APDU for RESET RETRY COUNTER command (page 33)
        CommandApdu changePin = commandFactory.createResetPw1Command(newPin);
        ResponseApdu response = communicate(changePin);

        if (!response.isSuccess()) {
            throw new CardException("Failed to change PIN", response.getSw());
        }
    }

    /**
     * Modifies the user's PW3. Before sending, the new PIN will be validated for
     * conformance to the token's requirements for key length.
     *
     * @param newAdminPin The new PW3.
     */
    public void modifyPw3Pin(byte[] newAdminPin, Passphrase adminPin) throws IOException {
        final int MAX_PW3_LENGTH_INDEX = 3;

        byte[] pwStatusBytes = getPwStatusBytes();

        if (newAdminPin.length < 8 || newAdminPin.length > pwStatusBytes[MAX_PW3_LENGTH_INDEX]) {
            throw new IOException("Invalid PIN length");
        }

        byte[] pin = adminPin.toStringUnsafe().getBytes();

        CommandApdu changePin = commandFactory.createChangePw3Command(pin, newAdminPin);
        ResponseApdu response = communicate(changePin);

        if (!response.isSuccess()) {
            throw new CardException("Failed to change PIN", response.getSw());
        }
    }

    /**
     * Call DECIPHER command
     *
     * @param encryptedSessionKey the encoded session key
     * @param publicKey
     * @return the decoded session key
     */
    public byte[] decryptSessionKey(@NonNull byte[] encryptedSessionKey,
                                    CanonicalizedPublicKey publicKey)
            throws IOException {
        final KeyFormat kf = mOpenPgpCapabilities.getFormatForKeyType(KeyType.ENCRYPT);

        if (!mPw1ValidatedForDecrypt) {
            verifyPinForOther();
        }

        byte[] data;
        byte[] dataLen;
        int pLen = 0;

        X9ECParameters x9Params;

        switch (kf.keyFormatType()) {
            case RSAKeyFormatType:
                data = Arrays.copyOfRange(encryptedSessionKey, 2, encryptedSessionKey.length);
                if (data[0] != 0) {
                    data = Arrays.prepend(data, (byte) 0x00);
                }
                break;

            case ECKeyFormatType:
                pLen = ((((encryptedSessionKey[0] & 0xff) << 8) + (encryptedSessionKey[1] & 0xff)) + 7) / 8;
                data = new byte[pLen];

                System.arraycopy(encryptedSessionKey, 2, data, 0, pLen);

                final ECKeyFormat eckf = (ECKeyFormat) kf;
                x9Params = NISTNamedCurves.getByOID(eckf.getCurveOID());

                final ECPoint p = x9Params.getCurve().decodePoint(data);
                if (!p.isValid()) {
                    throw new CardException("Invalid EC point!");
                }

                data = p.getEncoded(false);

                if (data.length < 128) {
                    dataLen = new byte[]{(byte) data.length};
                } else {
                    dataLen = new byte[]{(byte) 0x81, (byte) data.length};
                }
                data = Arrays.concatenate(Hex.decode("86"), dataLen, data);

                if (data.length < 128) {
                    dataLen = new byte[]{(byte) data.length};
                } else {
                    dataLen = new byte[]{(byte) 0x81, (byte) data.length};
                }
                data = Arrays.concatenate(Hex.decode("7F49"), dataLen, data);

                if (data.length < 128) {
                    dataLen = new byte[]{(byte) data.length};
                } else {
                    dataLen = new byte[]{(byte) 0x81, (byte) data.length};
                }
                data = Arrays.concatenate(Hex.decode("A6"), dataLen, data);
                break;

            default:
                throw new CardException("Unknown encryption key type!");
        }

        CommandApdu command = commandFactory.createDecipherCommand(data);
        ResponseApdu response = communicate(command);

        if (!response.isSuccess()) {
            throw new CardException("Deciphering with Security token failed on receive", response.getSw());
        }

        switch (mOpenPgpCapabilities.getFormatForKeyType(KeyType.ENCRYPT).keyFormatType()) {
            case RSAKeyFormatType:
                return response.getData();

            /* From 3.x OpenPGP card specification :
               In case of ECDH the card supports a partial decrypt only.
               With its own private key and the given public key the card calculates a shared secret
               in compliance with the Elliptic Curve Key Agreement Scheme from Diffie-Hellman.
               The shared secret is returned in the response, all other calculation for deciphering
               are done outside of the card.

               The shared secret obtained is a KEK (Key Encryption Key) that is used to wrap the
               session key.

               From rfc6637#section-13 :
               This document explicitly discourages the use of algorithms other than AES as a KEK algorithm.
               */
            case ECKeyFormatType:
                data = response.getData();

                final byte[] keyEnc = new byte[encryptedSessionKey[pLen + 2]];

                System.arraycopy(encryptedSessionKey, 2 + pLen + 1, keyEnc, 0, keyEnc.length);

                try {
                    final MessageDigest kdf = MessageDigest.getInstance(MessageDigestUtils.getDigestName(publicKey.getSecurityTokenHashAlgorithm()));

                    kdf.update(new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 1});
                    kdf.update(data);
                    kdf.update(publicKey.createUserKeyingMaterial(fingerprintCalculator));

                    final byte[] kek = kdf.digest();
                    final Cipher c = Cipher.getInstance("AESWrap");

                    c.init(Cipher.UNWRAP_MODE, new SecretKeySpec(kek, 0, publicKey.getSecurityTokenSymmetricKeySize() / 8, "AES"));

                    final Key paddedSessionKey = c.unwrap(keyEnc, "Session", Cipher.SECRET_KEY);

                    Arrays.fill(kek, (byte) 0);

                    return PGPPad.unpadSessionData(paddedSessionKey.getEncoded());
                } catch (NoSuchAlgorithmException e) {
                    throw new CardException("Unknown digest/encryption algorithm!");
                } catch (NoSuchPaddingException e) {
                    throw new CardException("Unknown padding algorithm!");
                } catch (PGPException e) {
                    throw new CardException(e.getMessage());
                } catch (InvalidKeyException e) {
                    throw new CardException("Invalid KEK!");
                }

            default:
                throw new CardException("Unknown encryption key type!");
        }
    }

    /**
     * Verifies the user's PW1 with the appropriate mode.
     */
    private void verifyPinForSignature() throws IOException {
        byte[] pin = mPin.toStringUnsafe().getBytes();

        ResponseApdu response = communicate(commandFactory.createVerifyPw1ForSignatureCommand(pin));
        if (!response.isSuccess()) {
            throw new CardException("Bad PIN!", response.getSw());
        }

        mPw1ValidatedForSignature = true;
    }

    /**
     * Verifies the user's PW1 with the appropriate mode.
     */
    private void verifyPinForOther() throws IOException {
        byte[] pin = mPin.toStringUnsafe().getBytes();

        // Command APDU for VERIFY command (page 32)
        ResponseApdu response = communicate(commandFactory.createVerifyPw1ForOtherCommand(pin));
        if (!response.isSuccess()) {
            throw new CardException("Bad PIN!", response.getSw());
        }

        mPw1ValidatedForDecrypt = true;
    }

    /**
     * Verifies the user's PW1 or PW3 with the appropriate mode.
     */
    private void verifyAdminPin(Passphrase adminPin) throws IOException {
        // Command APDU for VERIFY command (page 32)
        ResponseApdu response =
                communicate(commandFactory.createVerifyPw3Command(adminPin.toStringUnsafe().getBytes()));
        if (!response.isSuccess()) {
            throw new CardException("Bad PIN!", response.getSw());
        }

        mPw3Validated = true;
    }

    /**
     * Stores a data object on the token. Automatically validates the proper PIN for the operation.
     * Supported for all data objects < 255 bytes in length. Only the cardholder certificate
     * (0x7F21) can exceed this length.
     *
     * @param dataObject The data object to be stored.
     * @param data       The data to store in the object
     */
    private void putData(Passphrase adminPin, int dataObject, byte[] data) throws IOException {
        if (data.length > 254) {
            throw new IOException("Cannot PUT DATA with length > 254");
        }
        // TODO use admin pin regardless, if we have it?
        if (dataObject == 0x0101 || dataObject == 0x0103) {
            if (!mPw1ValidatedForDecrypt) {
                verifyPinForOther();
            }
        } else if (!mPw3Validated) {
            verifyAdminPin(adminPin);
        }

        CommandApdu command = commandFactory.createPutDataCommand(dataObject, data);
        ResponseApdu response = communicate(command); // put data

        if (!response.isSuccess()) {
            throw new CardException("Failed to put data.", response.getSw());
        }
    }

    private void setKeyAttributes(Passphrase adminPin, KeyType keyType, byte[] data) throws IOException {
        if (!mOpenPgpCapabilities.isAttributesChangable()) {
            return;
        }

        putData(adminPin, keyType.getAlgoAttributeSlot(), data);
        refreshConnectionCapabilities();
    }

    /**
     * Puts a key on the token in the given slot.
     *
     * @param slot The slot on the token where the key should be stored:
     *             0xB6: Signature Key
     *             0xB8: Decipherment Key
     *             0xA4: Authentication Key
     */
    @VisibleForTesting
    void putKey(KeyType slot, CanonicalizedSecretKey secretKey, Passphrase passphrase, Passphrase adminPin)
            throws IOException {
        RSAPrivateCrtKey crtSecretKey;
        ECPrivateKey ecSecretKey;
        ECPublicKey ecPublicKey;

        if (!mPw3Validated) {
            verifyAdminPin(adminPin);
        }

        // Now we're ready to communicate with the token.
        byte[] keyBytes = null;

        try {
            secretKey.unlock(passphrase);

            setKeyAttributes(adminPin, slot, SecurityTokenUtils.attributesFromSecretKey(slot, secretKey,
                    mOpenPgpCapabilities.getFormatForKeyType(slot)));

            KeyFormat formatForKeyType = mOpenPgpCapabilities.getFormatForKeyType(slot);
            switch (formatForKeyType.keyFormatType()) {
                case RSAKeyFormatType:
                    if (!secretKey.isRSA()) {
                        throw new IOException("Security Token not configured for RSA key.");
                    }
                    crtSecretKey = secretKey.getSecurityTokenRSASecretKey();

                    // Should happen only rarely; all GnuPG keys since 2006 use public exponent 65537.
                    if (!crtSecretKey.getPublicExponent().equals(new BigInteger("65537"))) {
                        throw new IOException("Invalid public exponent for smart Security Token.");
                    }

                    keyBytes = SecurityTokenUtils.createRSAPrivKeyTemplate(crtSecretKey, slot,
                            (RSAKeyFormat) formatForKeyType);
                    break;

                case ECKeyFormatType:
                    if (!secretKey.isEC()) {
                        throw new IOException("Security Token not configured for EC key.");
                    }

                    secretKey.unlock(passphrase);
                    ecSecretKey = secretKey.getSecurityTokenECSecretKey();
                    ecPublicKey = secretKey.getSecurityTokenECPublicKey();

                    keyBytes = SecurityTokenUtils.createECPrivKeyTemplate(ecSecretKey, ecPublicKey, slot,
                            (ECKeyFormat) formatForKeyType);
                    break;

                default:
                    throw new IOException("Key type unsupported by security token.");
            }
        } catch (PgpGeneralException e) {
            throw new IOException(e.getMessage());
        }

        CommandApdu apdu = commandFactory.createPutKeyCommand(keyBytes);
        ResponseApdu response = communicate(apdu);

        if (!response.isSuccess()) {
            throw new CardException("Key export to Security Token failed", response.getSw());
        }
    }

    /**
     * Return fingerprints of all keys from application specific data stored
     * on tag, or null if data not available.
     *
     * @return The fingerprints of all subkeys in a contiguous byte array.
     */
    public byte[] getFingerprints() throws IOException {
        return mOpenPgpCapabilities.getFingerprints();
    }

    /**
     * Return the PW Status Bytes from the token. This is a simple DO; no TLV decoding needed.
     *
     * @return Seven bytes in fixed format, plus 0x9000 status word at the end.
     */
    private byte[] getPwStatusBytes() throws IOException {
        return mOpenPgpCapabilities.getPwStatusBytes();
    }

    public byte[] getAid() throws IOException {
        return mOpenPgpCapabilities.getAid();
    }

    public String getUrl() throws IOException {
        byte[] data = getData(0x5F, 0x50);
        return new String(data).trim();
    }

    public String getUserId() throws IOException {
        return getHolderName(getData(0x00, 0x65));
    }

    private byte[] getData(int p1, int p2) throws IOException {
        ResponseApdu response = communicate(commandFactory.createGetDataCommand(p1, p2));
        if (!response.isSuccess()) {
            throw new CardException("Failed to get pw status bytes", response.getSw());
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
            verifyPinForSignature();
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

        byte[] data;

        KeyFormat signKeyFormat = mOpenPgpCapabilities.getFormatForKeyType(KeyType.SIGN);
        switch (signKeyFormat.keyFormatType()) {
            case RSAKeyFormatType:
                data = dsi;
                break;
            case ECKeyFormatType:
                data = hash;
                break;
            default:
                throw new IOException("Not supported key type!");
        }

        // Command APDU for PERFORM SECURITY OPERATION: COMPUTE DIGITAL SIGNATURE (page 37)
        CommandApdu command = commandFactory.createComputeDigitalSignatureCommand(data);
        ResponseApdu response = communicate(command);

        if (!response.isSuccess()) {
            throw new CardException("Failed to sign", response.getSw());
        }

        if (!mOpenPgpCapabilities.isPw1ValidForMultipleSignatures()) {
            mPw1ValidatedForSignature = false;
        }

        byte[] signature = response.getData();

        // Make sure the signature we received is actually the expected number of bytes long!
        switch (signKeyFormat.keyFormatType()) {
            case RSAKeyFormatType:
                int modulusLength = ((RSAKeyFormat) signKeyFormat).getModulusLength();
                if (signature.length != (modulusLength / 8)) {
                    throw new IOException("Bad signature length! Expected " + (modulusLength / 8) +
                            " bytes, got " + signature.length);
                }
                break;

            case ECKeyFormatType:
                // "plain" encoding, see https://github.com/open-keychain/open-keychain/issues/2108
                if (signature.length % 2 != 0) {
                    throw new IOException("Bad signature length!");
                }
                final byte[] br = new byte[signature.length / 2];
                final byte[] bs = new byte[signature.length / 2];
                for (int i = 0; i < br.length; ++i) {
                    br[i] = signature[i];
                    bs[i] = signature[br.length + i];
                }
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ASN1OutputStream out = new ASN1OutputStream(baos);
                out.writeObject(new DERSequence(new ASN1Encodable[]{new ASN1Integer(br), new ASN1Integer(bs)}));
                out.flush();
                signature = baos.toByteArray();
                break;
        }

        return signature;
    }

    /**
     * Transceives APDU
     * Splits extended APDU into short APDUs and chains them if necessary
     * Performs GET RESPONSE command(ISO/IEC 7816-4 par.7.6.1) on retrieving if necessary
     *
     * @param apdu short or extended APDU to transceive
     * @return response from the card
     * @throws IOException
     */
    ResponseApdu communicate(CommandApdu apdu) throws IOException {
        if ((mSecureMessaging != null) && mSecureMessaging.isEstablished()) {
            try {
                apdu = mSecureMessaging.encryptAndSign(apdu);
            } catch (SecureMessagingException e) {
                clearSecureMessaging();
                throw new IOException("secure messaging encrypt/sign failure : " + e.getMessage());
            }
        }

        ResponseApdu lastResponse = null;
        // Transmit
        if (mCardCapabilities.hasExtended()) {
            lastResponse = mTransport.transceive(apdu);
        } else if (commandFactory.isSuitableForShortApdu(apdu)) {
            CommandApdu shortApdu = commandFactory.createShortApdu(apdu);
            lastResponse = mTransport.transceive(shortApdu);
        } else if (mCardCapabilities.hasChaining()) {
            List<CommandApdu> chainedApdus = commandFactory.createChainedApdus(apdu);
            for (int i = 0, totalCommands = chainedApdus.size(); i < totalCommands; i++) {
                CommandApdu chainedApdu = chainedApdus.get(i);
                lastResponse = mTransport.transceive(chainedApdu);

                boolean isLastCommand = i < totalCommands - 1;
                if (isLastCommand && !lastResponse.isSuccess()) {
                    throw new UsbTransportException("Failed to chain apdu (last SW: " + lastResponse.getSw() + ")");
                }
            }
        }
        if (lastResponse == null) {
            throw new UsbTransportException("Can't transmit command");
        }

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(lastResponse.getData());

        // Receive
        while (lastResponse.getSw1() == APDU_SW1_RESPONSE_AVAILABLE) {
            // GET RESPONSE ISO/IEC 7816-4 par.7.6.1
            CommandApdu getResponse = commandFactory.createGetResponseCommand(lastResponse.getSw2());
            lastResponse = mTransport.transceive(getResponse);
            result.write(lastResponse.getData());
        }

        result.write(lastResponse.getSw1());
        result.write(lastResponse.getSw2());

        lastResponse = ResponseApdu.fromBytes(result.toByteArray());

        if ((mSecureMessaging != null) && mSecureMessaging.isEstablished()) {
            try {
                lastResponse = mSecureMessaging.verifyAndDecrypt(lastResponse);
            } catch (SecureMessagingException e) {
                clearSecureMessaging();
                throw new IOException("secure messaging verify/decrypt failure : " + e.getMessage());
            }
        }

        return lastResponse;
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
    public byte[] generateKey(Passphrase adminPin, int slot) throws IOException {
        if (slot != 0xB6 && slot != 0xB8 && slot != 0xA4) {
            throw new IOException("Invalid key slot");
        }

        if (!mPw3Validated) {
            verifyAdminPin(adminPin);
        }

        CommandApdu apdu = commandFactory.createGenerateKeyCommand(slot);
        ResponseApdu response = communicate(apdu);

        if (!response.isSuccess()) {
            throw new IOException("On-card key generation failed");
        }

        return response.getData();
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
            // Command APDU for VERIFY command (page 32)
            ResponseApdu response = communicate(commandFactory.createVerifyPw1ForSignatureCommand(pin));
            if (response.isSuccess()) {
                throw new CardException("Should never happen, XXXXXX has been accepted!", response.getSw());
            }
        }

        // try wrong Admin PIN 4 times until counter goes to C0
        byte[] adminPin = "XXXXXXXX".getBytes();
        for (int i = 0; i <= 4; i++) {
            // Command APDU for VERIFY command (page 32)
            ResponseApdu response = communicate(commandFactory.createVerifyPw3Command(adminPin));
            if (response.isSuccess()) { // Should NOT accept!
                throw new CardException("Should never happen, XXXXXXXX has been accepted", response.getSw());
            }
        }

        // secure messaging must be disabled before reactivation
        clearSecureMessaging();

        // reactivate token!
        // NOTE: keep the order here! First execute _both_ reactivate commands. Before checking _both_ responses
        // If a token is in a bad state and reactivate1 fails, it could still be reactivated with reactivate2
        CommandApdu reactivate1 = commandFactory.createReactivate1Command();
        CommandApdu reactivate2 = commandFactory.createReactivate2Command();
        ResponseApdu response1 = communicate(reactivate1);
        ResponseApdu response2 = communicate(reactivate2);
        if (!response1.isSuccess()) {
            throw new CardException("Reactivating failed!", response1.getSw());
        }
        if (!response2.isSuccess()) {
            throw new CardException("Reactivating failed!", response2.getSw());
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
        return mTransport.isPersistentConnectionAllowed() &&
                (mSecureMessaging == null || !mSecureMessaging.isEstablished());
    }

    public boolean isConnected() {
        return mTransport.isConnected();
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void clearSecureMessaging() {
        if (mSecureMessaging != null) {
            mSecureMessaging.clearSession();
        }
        mSecureMessaging = null;
    }

    void setSecureMessaging(final SecureMessaging sm) {
        clearSecureMessaging();
        mSecureMessaging = sm;
    }

    public SecurityTokenInfo getTokenInfo() throws IOException {
        byte[] rawFingerprints = getFingerprints();

        byte[][] fingerprints = new byte[rawFingerprints.length / 20][];
        ByteBuffer buf = ByteBuffer.wrap(rawFingerprints);
        for (int i = 0; i < rawFingerprints.length / 20; i++) {
            fingerprints[i] = new byte[20];
            buf.get(fingerprints[i]);
        }

        byte[] aid = getAid();
        String userId = getUserId();
        String url = getUrl();
        byte[] pwInfo = getPwStatusBytes();

        TransportType transportType = mTransport.getTransportType();

        SecurityTokenInfo info = SecurityTokenInfo
                .create(transportType, tokenType, fingerprints, aid, userId, url, pwInfo[4], pwInfo[6]);

        if (! info.isSecurityTokenSupported()) {
            throw new UnsupportedSecurityTokenException();
        }

        return info;
    }

    public static double parseOpenPgpVersion(final byte[] aid) {
        float minv = aid[7];
        while (minv > 0) minv /= 10.0;
        return aid[6] + minv;
    }
}
