package org.sufficientlysecure.keychain.securitytoken;


import java.io.IOException;

import org.sufficientlysecure.keychain.util.Passphrase;


public class ModifyPinUseCase {
    private static final int MAX_PW3_LENGTH_INDEX = 3;
    private static final int MIN_PW3_LENGTH = 8;

    private final SecurityTokenConnection connection;
    private final Passphrase adminPin;

    public static ModifyPinUseCase create(SecurityTokenConnection connection, Passphrase adminPin) {
        return new ModifyPinUseCase(connection, adminPin);
    }

    private ModifyPinUseCase(SecurityTokenConnection connection,
            Passphrase adminPin) {
        this.connection = connection;
        this.adminPin = adminPin;
    }

    public void modifyPw1andPw3Pins(byte[] newPin, byte[] newAdminPin) throws IOException {
        // Order is important for Gnuk, otherwise it will be set up in "admin less mode".
        // http://www.fsij.org/doc-gnuk/gnuk-passphrase-setting.html#set-up-pw1-pw3-and-reset-code
        modifyPw3Pin(newAdminPin);
        modifyPw1PinWithEffectiveAdminPin(new Passphrase(new String(newAdminPin)), newPin);
    }

    public void modifyPw1Pin(byte[] newPin) throws IOException {
        modifyPw1PinWithEffectiveAdminPin(adminPin, newPin);
    }

    private void modifyPw1PinWithEffectiveAdminPin(Passphrase effectiveAdminPin, byte[] newPin) throws IOException {
        connection.verifyAdminPin(effectiveAdminPin);

        final int MAX_PW1_LENGTH_INDEX = 1;
        byte[] pwStatusBytes = connection.getPwStatusBytes();
        if (newPin.length < 6 || newPin.length > pwStatusBytes[MAX_PW1_LENGTH_INDEX]) {
            throw new IOException("Invalid PIN length");
        }

        CommandApdu changePin = connection.getCommandFactory().createResetPw1Command(newPin);
        ResponseApdu response = connection.communicate(changePin);

        if (!response.isSuccess()) {
            throw new CardException("Failed to change PIN", response.getSw());
        }
    }

    /**
     * Modifies the user's PW3. Before sending, the new PIN will be validated for
     * conformance to the token's requirements for key length.
     */
    private void modifyPw3Pin(byte[] newAdminPin) throws IOException {
        byte[] pwStatusBytes = connection.getPwStatusBytes();

        if (newAdminPin.length < MIN_PW3_LENGTH || newAdminPin.length > pwStatusBytes[MAX_PW3_LENGTH_INDEX]) {
            throw new IOException("Invalid PIN length");
        }

        byte[] pin = adminPin.toStringUnsafe().getBytes();

        CommandApdu changePin = connection.getCommandFactory().createChangePw3Command(pin, newAdminPin);
        ResponseApdu response = connection.communicate(changePin);

        connection.invalidatePw3();

        if (!response.isSuccess()) {
            throw new CardException("Failed to change PIN", response.getSw());
        }
    }
}
