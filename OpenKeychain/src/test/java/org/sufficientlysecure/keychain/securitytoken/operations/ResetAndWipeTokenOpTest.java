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


import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.securitytoken.CommandApdu;
import org.sufficientlysecure.keychain.securitytoken.OpenPgpCapabilities;
import org.sufficientlysecure.keychain.securitytoken.OpenPgpCommandApduFactory;
import org.sufficientlysecure.keychain.securitytoken.ResponseApdu;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenConnection;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("WeakerAccess")
@RunWith(KeychainTestRunner.class)
public class ResetAndWipeTokenOpTest {
    static final ResponseApdu RESPONSE_APDU_SUCCESS = ResponseApdu.fromBytes(Hex.decode("9000"));
    static final ResponseApdu RESPONSE_APDU_BAD_PW = ResponseApdu.fromBytes(Hex.decode("63C0"));

    SecurityTokenConnection securityTokenConnection;
    OpenPgpCommandApduFactory commandFactory;
    ResetAndWipeTokenOp useCase;

    @Before
    public void setUp() throws Exception {
        securityTokenConnection = mock(SecurityTokenConnection.class);

        commandFactory = mock(OpenPgpCommandApduFactory.class);
        when(securityTokenConnection.getCommandFactory()).thenReturn(commandFactory);

        useCase = ResetAndWipeTokenOp.create(securityTokenConnection);
    }

    @Test
    public void resetAndWipeToken() throws Exception {
        OpenPgpCapabilities openPgpCapabilities = OpenPgpCapabilities.fromBytes(
                Hex.decode("6e81de4f10d27600012401020000060364311500005f520f0073000080000000000000000000007381b7c00af" +
                        "00000ff04c000ff00ffc106010800001103c206010800001103c306010800001103c407007f7f7f03" +
                        "0303c53c4ec5fee25c4e89654d58cad8492510a89d3c3d8468da7b24e15bfc624c6a792794f15b759" +
                        "9915f703aab55ed25424d60b17026b7b06c6ad4b9be30a3c63c000000000000000000000000000000" +
                        "000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                        "000000000cd0c59cd0f2a59cd0af059cd0c95"
                ));
        when(securityTokenConnection.getOpenPgpCapabilities()).thenReturn(openPgpCapabilities);

        CommandApdu verifyPw1Apdu = mock(CommandApdu.class);
        CommandApdu verifyPw3Apdu = mock(CommandApdu.class);
        when(commandFactory.createVerifyPw1ForSignatureCommand(any(byte[].class))).thenReturn(verifyPw1Apdu);
        when(commandFactory.createVerifyPw3Command(any(byte[].class))).thenReturn(verifyPw3Apdu);
        when(securityTokenConnection.communicate(verifyPw1Apdu)).thenReturn(RESPONSE_APDU_BAD_PW);
        when(securityTokenConnection.communicate(verifyPw3Apdu)).thenReturn(RESPONSE_APDU_BAD_PW);

        CommandApdu reactivate1Apdu = mock(CommandApdu.class);
        CommandApdu reactivate2Apdu = mock(CommandApdu.class);
        when(commandFactory.createReactivate1Command()).thenReturn(reactivate1Apdu);
        when(commandFactory.createReactivate2Command()).thenReturn(reactivate2Apdu);
        when(securityTokenConnection.communicate(reactivate1Apdu)).thenReturn(RESPONSE_APDU_SUCCESS);
        when(securityTokenConnection.communicate(reactivate2Apdu)).thenReturn(RESPONSE_APDU_SUCCESS);


        useCase.resetAndWipeToken();


        verify(securityTokenConnection).communicate(reactivate1Apdu);
        verify(securityTokenConnection).communicate(reactivate2Apdu);
        verify(securityTokenConnection).refreshConnectionCapabilities();
    }

}