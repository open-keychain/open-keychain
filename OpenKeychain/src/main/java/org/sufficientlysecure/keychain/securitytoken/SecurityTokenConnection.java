/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TokenType;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TransportType;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;


/**
 * This class provides a communication interface to OpenPGP applications on ISO SmartCard compliant
 * devices.
 * For the full specs, see http://g10code.com/docs/openpgp-card-2.0.pdf
 */
public class SecurityTokenConnection {
    private static final int APDU_SW1_RESPONSE_AVAILABLE = 0x61;

    private static final String AID_PREFIX_FIDESMO = "A000000617";

    private static SecurityTokenConnection sCachedInstance;

    @NonNull
    private final Transport transport;
    @Nullable
    private final Passphrase cachedPin;
    private final OpenPgpCommandApduFactory commandFactory;

    private TokenType tokenType;
    private CardCapabilities cardCapabilities;
    private OpenPgpCapabilities openPgpCapabilities;

    private SecureMessaging secureMessaging;

    private boolean isPw1ValidatedForSignature; // Mode 81
    private boolean isPw1ValidatedForOther; // Mode 82
    private boolean isPw3Validated;


    public static SecurityTokenConnection getInstanceForTransport(
            @NonNull Transport transport, @Nullable Passphrase pin) {
        if (sCachedInstance == null || !sCachedInstance.isPersistentConnectionAllowed() ||
                !sCachedInstance.isConnected() || !sCachedInstance.transport.equals(transport) ||
                (pin != null && !pin.equals(sCachedInstance.cachedPin))) {
            sCachedInstance = new SecurityTokenConnection(transport, pin, new OpenPgpCommandApduFactory());
        }
        return sCachedInstance;
    }

    public static void clearCachedConnections() {
        sCachedInstance = null;
    }


    @VisibleForTesting
    SecurityTokenConnection(@NonNull Transport transport, @Nullable Passphrase pin,
            OpenPgpCommandApduFactory commandFactory) {
        this.transport = transport;
        this.cachedPin = pin;

        this.commandFactory = commandFactory;
    }

    OpenPgpCapabilities getOpenPgpCapabilities() {
        return openPgpCapabilities;
    }

    OpenPgpCommandApduFactory getCommandFactory() {
        return commandFactory;
    }

    void maybeInvalidatePw1() {
        if (!openPgpCapabilities.isPw1ValidForMultipleSignatures()) {
            isPw1ValidatedForSignature = false;
        }
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
        transport.connect();

        // dummy instance for initial communicate() calls
        cardCapabilities = new CardCapabilities();

        determineTokenType();

        CommandApdu select = commandFactory.createSelectFileOpenPgpCommand();
        ResponseApdu response = communicate(select);  // activate connection

        if (!response.isSuccess()) {
            throw new CardException("Initialization failed!", response.getSw());
        }

        refreshConnectionCapabilities();

        isPw1ValidatedForSignature = false;
        isPw1ValidatedForOther = false;
        isPw3Validated = false;

        if (openPgpCapabilities.isHasSCP11bSM()) {
            try {
                SCP11bSecureMessaging.establish(this, context, commandFactory);
            } catch (SecureMessagingException e) {
                secureMessaging = null;
                Log.e(Constants.TAG, "failed to establish secure messaging", e);
            }
        }
    }

    @VisibleForTesting
    void determineTokenType() throws IOException {
        tokenType = transport.getTokenTypeIfAvailable();
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

    void refreshConnectionCapabilities() throws IOException {
        byte[] rawOpenPgpCapabilities = getData(0x00, 0x6E);

        OpenPgpCapabilities openPgpCapabilities = new OpenPgpCapabilities(rawOpenPgpCapabilities);
        setConnectionCapabilities(openPgpCapabilities);
    }

    @VisibleForTesting
    void setConnectionCapabilities(OpenPgpCapabilities openPgpCapabilities) throws IOException {
        this.openPgpCapabilities = openPgpCapabilities;
        this.cardCapabilities = new CardCapabilities(openPgpCapabilities.getHistoricalBytes());
    }

    public void resetPin(byte[] newPin, Passphrase adminPin) throws IOException {
        if (!isPw3Validated) {
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

        isPw3Validated = false;

        if (!response.isSuccess()) {
            throw new CardException("Failed to change PIN", response.getSw());
        }
    }

    /**
     * Verifies the user's PW1 with the appropriate mode.
     */
    void verifyPinForSignature() throws IOException {
        if (isPw1ValidatedForSignature) {
            return;
        }

        if (cachedPin == null) {
            throw new IllegalStateException("Connection not initialized with Pin!");
        }
        byte[] pin = cachedPin.toStringUnsafe().getBytes();

        ResponseApdu response = communicate(commandFactory.createVerifyPw1ForSignatureCommand(pin));
        if (!response.isSuccess()) {
            throw new CardException("Bad PIN!", response.getSw());
        }

        isPw1ValidatedForSignature = true;
    }

    /**
     * Verifies the user's PW1 with the appropriate mode.
     */
    void verifyPinForOther() throws IOException {
        if (isPw1ValidatedForOther) {
            return;
        }
        if (cachedPin == null) {
            throw new IllegalStateException("Connection not initialized with Pin!");
        }

        byte[] pin = cachedPin.toStringUnsafe().getBytes();

        // Command APDU for VERIFY command (page 32)
        ResponseApdu response = communicate(commandFactory.createVerifyPw1ForOtherCommand(pin));
        if (!response.isSuccess()) {
            throw new CardException("Bad PIN!", response.getSw());
        }

        isPw1ValidatedForOther = true;
    }

    /**
     * Verifies the user's PW1 or PW3 with the appropriate mode.
     */
    void verifyAdminPin(Passphrase adminPin) throws IOException {
        if (isPw3Validated) {
            return;
        }
        // Command APDU for VERIFY command (page 32)
        ResponseApdu response =
                communicate(commandFactory.createVerifyPw3Command(adminPin.toStringUnsafe().getBytes()));
        if (!response.isSuccess()) {
            throw new CardException("Bad PIN!", response.getSw());
        }

        isPw3Validated = true;
    }

    /**
     * Return fingerprints of all keys from application specific data stored
     * on tag, or null if data not available.
     *
     * @return The fingerprints of all subkeys in a contiguous byte array.
     */
    public byte[] getFingerprints() throws IOException {
        return openPgpCapabilities.getFingerprints();
    }

    /**
     * Return the PW Status Bytes from the token. This is a simple DO; no TLV decoding needed.
     *
     * @return Seven bytes in fixed format, plus 0x9000 status word at the end.
     */
    private byte[] getPwStatusBytes() throws IOException {
        return openPgpCapabilities.getPwStatusBytes();
    }

    public byte[] getAid() throws IOException {
        return openPgpCapabilities.getAid();
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
     * Transceives APDU
     * Splits extended APDU into short APDUs and chains them if necessary
     * Performs GET RESPONSE command(ISO/IEC 7816-4 par.7.6.1) on retrieving if necessary
     *
     * @param apdu short or extended APDU to transceive
     * @return response from the card
     * @throws IOException
     */
    ResponseApdu communicate(CommandApdu apdu) throws IOException {
        if ((secureMessaging != null) && secureMessaging.isEstablished()) {
            try {
                apdu = secureMessaging.encryptAndSign(apdu);
            } catch (SecureMessagingException e) {
                clearSecureMessaging();
                throw new IOException("secure messaging encrypt/sign failure : " + e.getMessage());
            }
        }

        ResponseApdu lastResponse = null;
        // Transmit
        if (cardCapabilities.hasExtended()) {
            lastResponse = transport.transceive(apdu);
        } else if (commandFactory.isSuitableForShortApdu(apdu)) {
            CommandApdu shortApdu = commandFactory.createShortApdu(apdu);
            lastResponse = transport.transceive(shortApdu);
        } else if (cardCapabilities.hasChaining()) {
            List<CommandApdu> chainedApdus = commandFactory.createChainedApdus(apdu);
            for (int i = 0, totalCommands = chainedApdus.size(); i < totalCommands; i++) {
                CommandApdu chainedApdu = chainedApdus.get(i);
                lastResponse = transport.transceive(chainedApdu);

                boolean isLastCommand = (i == totalCommands - 1);
                if (!isLastCommand && !lastResponse.isSuccess()) {
                    throw new IOException("Failed to chain apdu " +
                            "(" + i + "/" + (totalCommands-1) + ", last SW: " + lastResponse.getSw() + ")");
                }
            }
        }
        if (lastResponse == null) {
            throw new IOException("Can't transmit command");
        }

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(lastResponse.getData());

        // Receive
        while (lastResponse.getSw1() == APDU_SW1_RESPONSE_AVAILABLE) {
            // GET RESPONSE ISO/IEC 7816-4 par.7.6.1
            CommandApdu getResponse = commandFactory.createGetResponseCommand(lastResponse.getSw2());
            lastResponse = transport.transceive(getResponse);
            result.write(lastResponse.getData());
        }

        result.write(lastResponse.getSw1());
        result.write(lastResponse.getSw2());

        lastResponse = ResponseApdu.fromBytes(result.toByteArray());

        if ((secureMessaging != null) && secureMessaging.isEstablished()) {
            try {
                lastResponse = secureMessaging.verifyAndDecrypt(lastResponse);
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

        if (!isPw3Validated) {
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

        refreshConnectionCapabilities();
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
        return transport.isPersistentConnectionAllowed() &&
                (secureMessaging == null || !secureMessaging.isEstablished());
    }

    public boolean isConnected() {
        return transport.isConnected();
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void clearSecureMessaging() {
        if (secureMessaging != null) {
            secureMessaging.clearSession();
        }
        secureMessaging = null;
    }

    void setSecureMessaging(SecureMessaging sm) {
        clearSecureMessaging();
        secureMessaging = sm;
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
        boolean hasLifeCycleManagement = cardCapabilities.hasLifeCycleManagement();

        TransportType transportType = transport.getTransportType();

        return SecurityTokenInfo
                .create(transportType, tokenType, fingerprints, aid, userId, url, pwInfo[4], pwInfo[6],
                        hasLifeCycleManagement);
    }
}
