/*
 * Copyright (C) 2018 Schürmann & Breitmoser GbR
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

package org.sufficientlysecure.keychain.securitytoken.operations;


import java.io.IOException;

import org.sufficientlysecure.keychain.securitytoken.CardException;
import org.sufficientlysecure.keychain.securitytoken.CommandApdu;
import org.sufficientlysecure.keychain.securitytoken.ResponseApdu;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenConnection;


public class ResetAndWipeTokenOp {
    private static final byte[] INVALID_PIN = "XXXXXXXXXXX".getBytes();

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
        exhausePw1Tries();
        exhaustPw3Tries();

        // secure messaging must be disabled before reactivation
        connection.clearSecureMessaging();

        // NOTE: keep the order here! First execute _both_ reactivate commands. Before checking _both_ responses
        // If a token is in a bad state and reactivate1 fails, it could still be reactivated with reactivate2
        CommandApdu reactivate1 = connection.getCommandFactory().createReactivate1Command();
        connection.communicate(reactivate1);

        CommandApdu reactivate2 = connection.getCommandFactory().createReactivate2Command();
        ResponseApdu response2 = connection.communicate(reactivate2);
        if (!response2.isSuccess()) {
            throw new CardException("Reactivating failed!", response2.getSw());
        }

        connection.refreshConnectionCapabilities();
    }

    private void exhausePw1Tries() throws IOException {
        CommandApdu verifyPw1ForSignatureCommand =
                connection.getCommandFactory().createVerifyPw1ForSignatureCommand(INVALID_PIN);

        int pw1TriesLeft = Math.max(3, connection.getOpenPgpCapabilities().getPw1TriesLeft());
        for (int i = 0; i < pw1TriesLeft; i++) {
            ResponseApdu response = connection.communicate(verifyPw1ForSignatureCommand);
            if (response.isSuccess()) {
                throw new CardException("Should never happen, PIN XXXXXXXX has been accepted!", response.getSw());
            }
        }
    }

    private void exhaustPw3Tries() throws IOException {
        CommandApdu verifyPw3Command = connection.getCommandFactory().createVerifyPw3Command(INVALID_PIN);

        int pw3TriesLeft = Math.max(3, connection.getOpenPgpCapabilities().getPw3TriesLeft());
        for (int i = 0; i < pw3TriesLeft; i++) {
            ResponseApdu response = connection.communicate(verifyPw3Command);
            if (response.isSuccess()) { // Should NOT accept!
                throw new CardException("Should never happen, PIN XXXXXXXX has been accepted!", response.getSw());
            }
        }
    }
}
