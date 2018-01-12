package org.sufficientlysecure.keychain.securitytoken.operations;


import java.io.IOException;

import org.sufficientlysecure.keychain.securitytoken.CardException;
import org.sufficientlysecure.keychain.securitytoken.CommandApdu;
import org.sufficientlysecure.keychain.securitytoken.ResponseApdu;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenConnection;


public class ResetAndWipeTokenOp {
    private final SecurityTokenConnection connection;

    public static ResetAndWipeTokenOp create(SecurityTokenConnection connection) {
        return new ResetAndWipeTokenOp(connection);
    }

    private ResetAndWipeTokenOp(SecurityTokenConnection connection) {
        this.connection = connection;
    }

    /**
     * Resets security token, which deletes all keys and data objects.
     * This works by entering a wrong PIN and then Admin PIN 4 times respectively.
     * Afterwards, the token is reactivated.
     */
    public void resetAndWipeToken() throws IOException {
        // try wrong PIN 4 times until counter goes to C0
        byte[] pin = "XXXXXX".getBytes();
        CommandApdu verifyPw1ForSignatureCommand =
                connection.getCommandFactory().createVerifyPw1ForSignatureCommand(pin);
        for (int i = 0; i <= 4; i++) {
            // Command APDU for VERIFY command (page 32)
            ResponseApdu response = connection.communicate(verifyPw1ForSignatureCommand);
            if (response.isSuccess()) {
                throw new CardException("Should never happen, XXXXXX has been accepted!", response.getSw());
            }
        }

        // try wrong Admin PIN 4 times until counter goes to C0
        byte[] adminPin = "XXXXXXXX".getBytes();
        CommandApdu verifyPw3Command = connection.getCommandFactory().createVerifyPw3Command(adminPin);
        for (int i = 0; i <= 4; i++) {
            // Command APDU for VERIFY command (page 32)
            ResponseApdu response = connection.communicate(
                    verifyPw3Command);
            if (response.isSuccess()) { // Should NOT accept!
                throw new CardException("Should never happen, XXXXXXXX has been accepted", response.getSw());
            }
        }

        // secure messaging must be disabled before reactivation
        connection.clearSecureMessaging();

        // reactivate token!
        // NOTE: keep the order here! First execute _both_ reactivate commands. Before checking _both_ responses
        // If a token is in a bad state and reactivate1 fails, it could still be reactivated with reactivate2
        CommandApdu reactivate1 = connection.getCommandFactory().createReactivate1Command();
        ResponseApdu response1 = connection.communicate(reactivate1);
        if (!response1.isSuccess()) {
            throw new CardException("Reactivating failed!", response1.getSw());
        }

        CommandApdu reactivate2 = connection.getCommandFactory().createReactivate2Command();
        ResponseApdu response2 = connection.communicate(reactivate2);
        if (!response2.isSuccess()) {
            throw new CardException("Reactivating failed!", response2.getSw());
        }

        connection.refreshConnectionCapabilities();
    }
}
