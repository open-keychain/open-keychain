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
import org.sufficientlysecure.keychain.securitytoken.operations.PsoDecryptTokenOp;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(KeychainTestRunner.class)
public class PsoDecryptTokenOpTest {
    private static final byte[] RSA_ENC_SESSIONKEY_MPI = Hex.decode(
            "07ff7b9ff36f70da1fe7a6b59168c24a7e5b48a938c4f970de46524a06ebf4a9175a9737cf2e6f30957110b31db7" +
                    "0e9a2992401b1d5e99389f976356f4e3a28ff537362e7ce14b81200e21d4f0e77d46bd89f3a54ca06062289148a5938748" +
                    "8ac01d30d2baf58e6b35e32434720473604a9f7d5083ca6d40e4a2dadedd68033a4d4bbdb06d075d6980c0c0ca19078dcd" +
                    "fb9d8cbcb34f28d0b968b6e09eda0e1d3ab6b251eb09f9fb9d9abfeaf9010001733b9015e9e4b6c9df61bbc76041f439d1" +
                    "273e41f5d0e8414a2b8d6d4c7e86f30b94cfba308b38de53d694a8ca15382301ace806c8237641b03525b3e3e8cbb017e2" +
                    "51265229bcbb0da5d5aeb4eafbad9779");
    private SecurityTokenConnection securityTokenConnection;
    private OpenPgpCommandApduFactory commandFactory;
    private PsoDecryptTokenOp useCase;

    private CommandApdu dummyCommandApdu = mock(CommandApdu.class);

    @Before
    public void setUp() throws Exception {
        securityTokenConnection = mock(SecurityTokenConnection.class);

        commandFactory = mock(OpenPgpCommandApduFactory.class);
        when(securityTokenConnection.getCommandFactory()).thenReturn(commandFactory);

        useCase = PsoDecryptTokenOp.create(securityTokenConnection);
    }

    @Test
    public void testRsaDecrypt() throws Exception {
        OpenPgpCapabilities openPgpCapabilities = OpenPgpCapabilities.fromBytes(
                Hex.decode("6e81de4f10d27600012401020000060364311500005f520f0073000080000000000000000000007381b7c00af" +
                                "00000ff04c000ff00ffc106010800001103c206010800001103c306010800001103c407007f7f7f03" +
                                "0303c53c4ec5fee25c4e89654d58cad8492510a89d3c3d8468da7b24e15bfc624c6a792794f15b759" +
                                "9915f703aab55ed25424d60b17026b7b06c6ad4b9be30a3c63c000000000000000000000000000000" +
                                "000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                                "000000000cd0c59cd0f2a59cd0af059cd0c95"
                ));
        when(securityTokenConnection.getOpenPgpCapabilities()).thenReturn(openPgpCapabilities);

        ResponseApdu dummyResponseApdu = ResponseApdu.fromBytes(Hex.decode("010203049000"));

        when(commandFactory.createDecipherCommand(any(byte[].class))).thenReturn(dummyCommandApdu);
        when(securityTokenConnection.communicate(dummyCommandApdu)).thenReturn(dummyResponseApdu);

        byte[] response = useCase.verifyAndDecryptSessionKey(RSA_ENC_SESSIONKEY_MPI, null);

        assertArrayEquals(Hex.decode("01020304"), response);
    }
}