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
import java.util.List;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TokenType;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TransportType;
import org.sufficientlysecure.keychain.util.Passphrase;
import timber.log.Timber;


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

    // region connection management

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

        smEstablishIfAvailable(context);
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

    public void refreshConnectionCapabilities() throws IOException {
        byte[] rawOpenPgpCapabilities = readData(0x00, 0x6E);

        OpenPgpCapabilities openPgpCapabilities = OpenPgpCapabilities.fromBytes(rawOpenPgpCapabilities);
        setConnectionCapabilities(openPgpCapabilities);
    }

    @VisibleForTesting
    void setConnectionCapabilities(OpenPgpCapabilities openPgpCapabilities) throws IOException {
        this.openPgpCapabilities = openPgpCapabilities;
        this.cardCapabilities = new CardCapabilities(openPgpCapabilities.getHistoricalBytes());
    }

    // endregion

    // region communication

    /**
     * Transceives APDU
     * Splits extended APDU into short APDUs and chains them if necessary
     * Performs GET RESPONSE command(ISO/IEC 7816-4 par.7.6.1) on retrieving if necessary
     *
     * @param commandApdu short or extended APDU to transceive
     * @return response from the card
     */
    public ResponseApdu communicate(CommandApdu commandApdu) throws IOException {
        commandApdu = smEncryptIfAvailable(commandApdu);

        ResponseApdu lastResponse;

        lastResponse = transceiveWithChaining(commandApdu);
        lastResponse = readChainedResponseIfAvailable(lastResponse);

        lastResponse = smDecryptIfAvailable(lastResponse);

        return lastResponse;
    }

    @NonNull
    private ResponseApdu transceiveWithChaining(CommandApdu commandApdu) throws IOException {
        if (cardCapabilities.hasExtended()) {
            return transport.transceive(commandApdu);
        } else if (commandFactory.isSuitableForShortApdu(commandApdu)) {
            CommandApdu shortApdu = commandFactory.createShortApdu(commandApdu);
            return transport.transceive(shortApdu);
        } else if (cardCapabilities.hasChaining()) {
            ResponseApdu lastResponse = null;

            List<CommandApdu> chainedApdus = commandFactory.createChainedApdus(commandApdu);
            for (int i = 0, totalCommands = chainedApdus.size(); i < totalCommands; i++) {
                CommandApdu chainedApdu = chainedApdus.get(i);
                lastResponse = transport.transceive(chainedApdu);

                boolean isLastCommand = (i == totalCommands - 1);
                if (!isLastCommand && !lastResponse.isSuccess()) {
                    throw new IOException("Failed to chain apdu " +
                            "(" + i + "/" + (totalCommands-1) + ", last SW: " + lastResponse.getSw() + ")");
                }
            }

            if (lastResponse == null) {
                throw new IllegalStateException();
            }

            return lastResponse;
        } else {
            throw new IOException("Command too long, and chaining unavailable");
        }
    }

    @NonNull
    private ResponseApdu readChainedResponseIfAvailable(ResponseApdu lastResponse) throws IOException {
        if (lastResponse.getSw1() != APDU_SW1_RESPONSE_AVAILABLE) {
            return lastResponse;
        }

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(lastResponse.getData());

        do {
            // GET RESPONSE ISO/IEC 7816-4 par.7.6.1
            CommandApdu getResponse = commandFactory.createGetResponseCommand(lastResponse.getSw2());
            lastResponse = transport.transceive(getResponse);
            result.write(lastResponse.getData());
        } while (lastResponse.getSw1() == APDU_SW1_RESPONSE_AVAILABLE);

        result.write(lastResponse.getSw1());
        result.write(lastResponse.getSw2());

        return ResponseApdu.fromBytes(result.toByteArray());
    }

    // endregion

    // region secure messaging

    private void smEstablishIfAvailable(Context context) throws IOException {
        if (!openPgpCapabilities.isHasScp11bSm()) {
            return;
        }

        try {
            secureMessaging = SCP11bSecureMessaging.establish(this, context, commandFactory);
        } catch (SecureMessagingException e) {
            secureMessaging = null;
            Timber.e(e, "failed to establish secure messaging");
        }
    }

    private CommandApdu smEncryptIfAvailable(CommandApdu apdu) throws IOException {
        if (secureMessaging == null || !secureMessaging.isEstablished()) {
            return apdu;
        }
        try {
            return secureMessaging.encryptAndSign(apdu);
        } catch (SecureMessagingException e) {
            clearSecureMessaging();
            throw new IOException("secure messaging encrypt/sign failure : " + e.getMessage());
        }
    }

    private ResponseApdu smDecryptIfAvailable(ResponseApdu response) throws IOException {
        if (secureMessaging == null || !secureMessaging.isEstablished()) {
            return response;
        }
        try {
            return secureMessaging.verifyAndDecrypt(response);
        } catch (SecureMessagingException e) {
            clearSecureMessaging();
            throw new IOException("secure messaging verify/decrypt failure : " + e.getMessage());
        }
    }

    public void clearSecureMessaging() {
        if (secureMessaging != null) {
            secureMessaging.clearSession();
        }
        secureMessaging = null;
    }

    // endregion

    // region pin management

    public void verifyPinForSignature() throws IOException {
        if (isPw1ValidatedForSignature) {
            return;
        }
        if (cachedPin == null) {
            throw new IllegalStateException("Connection not initialized with Pin!");
        }

        byte[] pin = cachedPin.toStringUnsafe().getBytes();

        CommandApdu verifyPw1ForSignatureCommand = commandFactory.createVerifyPw1ForSignatureCommand(pin);
        ResponseApdu response = communicate(verifyPw1ForSignatureCommand);
        if (!response.isSuccess()) {
            throw new CardException("Bad PIN!", response.getSw());
        }

        isPw1ValidatedForSignature = true;
    }

    public void verifyPinForOther() throws IOException {
        if (isPw1ValidatedForOther) {
            return;
        }
        if (cachedPin == null) {
            throw new IllegalStateException("Connection not initialized with Pin!");
        }

        byte[] pin = cachedPin.toStringUnsafe().getBytes();

        CommandApdu verifyPw1ForOtherCommand = commandFactory.createVerifyPw1ForOtherCommand(pin);
        ResponseApdu response = communicate(verifyPw1ForOtherCommand);
        if (!response.isSuccess()) {
            throw new CardException("Bad PIN!", response.getSw());
        }

        isPw1ValidatedForOther = true;
    }

    public void verifyAdminPin(Passphrase adminPin) throws IOException {
        if (isPw3Validated) {
            return;
        }

        CommandApdu verifyPw3Command = commandFactory.createVerifyPw3Command(adminPin.toStringUnsafe().getBytes());
        ResponseApdu response = communicate(verifyPw3Command);
        if (!response.isSuccess()) {
            throw new CardException("Bad PIN!", response.getSw());
        }

        isPw3Validated = true;
    }

    public void invalidateSingleUsePw1() {
        if (!openPgpCapabilities.isPw1ValidForMultipleSignatures()) {
            isPw1ValidatedForSignature = false;
        }
    }

    public void invalidatePw3() {
        isPw3Validated = false;
    }

    // endregion

    private byte[] readData(int p1, int p2) throws IOException {
        ResponseApdu response = communicate(commandFactory.createGetDataCommand(p1, p2));
        if (!response.isSuccess()) {
            throw new CardException("Failed to get pw status bytes", response.getSw());
        }
        return response.getData();
    }


    private String readUrl() throws IOException {
        byte[] data = readData(0x5F, 0x50);
        return new String(data).trim();
    }

    private byte[] readUserId() throws IOException {
        return readData(0x00, 0x65);
    }

    public SecurityTokenInfo readTokenInfo() throws IOException {
        byte[][] fingerprints = new byte[3][];
        fingerprints[0] = openPgpCapabilities.getFingerprintSign();
        fingerprints[1] = openPgpCapabilities.getFingerprintEncrypt();
        fingerprints[2] = openPgpCapabilities.getFingerprintAuth();

        byte[] aid = openPgpCapabilities.getAid();
        String userId = parseHolderName(readUserId());
        String url = readUrl();
        int pw1TriesLeft = openPgpCapabilities.getPw1TriesLeft();
        int pw3TriesLeft = openPgpCapabilities.getPw3TriesLeft();
        boolean hasLifeCycleManagement = cardCapabilities.hasLifeCycleManagement();

        TransportType transportType = transport.getTransportType();

        return SecurityTokenInfo.create(transportType, tokenType, fingerprints, aid, userId, url, pw1TriesLeft,
                pw3TriesLeft, hasLifeCycleManagement);
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

    public OpenPgpCapabilities getOpenPgpCapabilities() {
        return openPgpCapabilities;
    }

    public OpenPgpCommandApduFactory getCommandFactory() {
        return commandFactory;
    }


    private static String parseHolderName(byte[] name) {
        try {
            return (new String(name, 4, name[3])).replace('<', ' ');
        } catch (IndexOutOfBoundsException e) {
            // try-catch for https://github.com/FluffyKaon/OpenPGP-Card
            // Note: This should not happen, but happens with
            // https://github.com/FluffyKaon/OpenPGP-Card, thus return an empty string for now!

            Timber.e(e, "Couldn't get holder name, returning empty string!");
            return "";
        }
    }
}
