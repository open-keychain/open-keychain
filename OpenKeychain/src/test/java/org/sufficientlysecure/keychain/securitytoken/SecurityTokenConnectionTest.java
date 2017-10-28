package org.sufficientlysecure.keychain.securitytoken;


import java.io.IOException;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.internal.verification.VerificationModeFactory;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TokenType;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TransportType;
import org.sufficientlysecure.keychain.util.Passphrase;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(KeychainTestRunner.class)
public class SecurityTokenConnectionTest {

    private Transport transport;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;

        transport = mock(Transport.class);
        when(transport.getTransportType()).thenReturn(TransportType.USB);
        when(transport.getTokenTypeIfAvailable()).thenReturn(TokenType.YUBIKEY_NEO);
    }

    @Test
    public void test_connectToDevice() throws Exception {
        SecurityTokenConnection securityTokenConnection =
                new SecurityTokenConnection(transport, new Passphrase("123456"), new OpenPgpCommandApduFactory());
        String[] dialog = {
                "00a4040006d27600012401", // select openpgp applet
                "9000",
                "00ca006e00", // get application related data
                "6e81de4f10d27600012401020000060364311500005f520f0073000080000000000000000000007381b7c00af" +
                        "00000ff04c000ff00ffc106010800001103c206010800001103c306010800001103c407007f7f7f03030" +
                        "3c53c4ec5fee25c4e89654d58cad8492510a89d3c3d8468da7b24e15bfc624c6a792794f15b7599915f7" +
                        "03aab55ed25424d60b17026b7b06c6ad4b9be30a3c63c000000000000000000000000000000000000000" +
                        "000000000000000000000000000000000000000000000000000000000000000000000000000000000cd0" +
                        "c59cd0f2a59cd0af059cd0c959000"
        };
        expect(transport, dialog);


        securityTokenConnection.connectToDevice(RuntimeEnvironment.application);


        verify(transport).connect();
        verifyDialog(transport, dialog);
    }

    @Test
    public void test_getTokenInfo() throws Exception {
        SecurityTokenConnection securityTokenConnection =
                new SecurityTokenConnection(transport, new Passphrase("123456"), new OpenPgpCommandApduFactory());
        OpenPgpCapabilities openPgpCapabilities = new OpenPgpCapabilities(
                Hex.decode(
                        "6e81de4f10d27600012401020000060364311500005f520f0073000080000000000000000000007381b7c00af" +
                                "00000ff04c000ff00ffc106010800001103c206010800001103c306010800001103c407007f7f7f03" +
                                "0303c53c4ec5fee25c4e89654d58cad8492510a89d3c3d8468da7b24e15bfc624c6a792794f15b759" +
                                "9915f703aab55ed25424d60b17026b7b06c6ad4b9be30a3c63c000000000000000000000000000000" +
                                "000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                                "000000000cd0c59cd0f2a59cd0af059cd0c95"
                                ));
        securityTokenConnection.setConnectionCapabilities(openPgpCapabilities);
        securityTokenConnection.determineTokenType();

        String[] dialog = {
                "00ca006500",
                "65095b005f2d005f3501399000",
                "00ca5f5000",
                "9000",
        };
        expect(transport, dialog);


        securityTokenConnection.getTokenInfo();


        verifyDialog(transport, dialog);
    }

    private void expect(Transport transport, String... dialog) throws IOException {
        for (int i = 0; i < dialog.length; i += 2) {
            CommandApdu command = CommandApdu.fromBytes(Hex.decode(dialog[i]));
            ResponseApdu response = ResponseApdu.fromBytes(Hex.decode(dialog[i + 1]));
            when(transport.transceive(eq(command))).thenReturn(response);
        }
    }

    private void verifyDialog(Transport transport, String... dialog) throws IOException {
        InOrder inOrder = inOrder(transport);
        for (int i = 0; i < dialog.length; i += 2) {
            CommandApdu command = CommandApdu.fromBytes(Hex.decode(dialog[i]));
            inOrder.verify(transport).transceive(eq(command));
        }
    }
}