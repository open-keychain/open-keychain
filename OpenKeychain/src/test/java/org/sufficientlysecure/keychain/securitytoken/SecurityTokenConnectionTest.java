package org.sufficientlysecure.keychain.securitytoken;


import java.util.LinkedList;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TokenType;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TransportType;
import org.sufficientlysecure.keychain.util.Passphrase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(KeychainTestRunner.class)
public class SecurityTokenConnectionTest {

    private Transport transport;

    LinkedList<CommandApdu> expectCommands;
    LinkedList<ResponseApdu> expectReplies;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;

        transport = mock(Transport.class);
        when(transport.getTransportType()).thenReturn(TransportType.USB);
        when(transport.getTokenTypeIfAvailable()).thenReturn(TokenType.YUBIKEY_NEO);

        expectCommands = new LinkedList<>();
        expectReplies = new LinkedList<>();
        when(transport.transceive(any(CommandApdu.class))).thenAnswer(new Answer<ResponseApdu>() {
            @Override
            public ResponseApdu answer(InvocationOnMock invocation) throws Throwable {
                CommandApdu commandApdu = invocation.getArgumentAt(0, CommandApdu.class);
                System.out.println("<< " + commandApdu);
                System.out.println("<< " + Hex.toHexString(commandApdu.toBytes()));

                CommandApdu expectedApdu = expectCommands.poll();
                assertEquals(expectedApdu, commandApdu);

                ResponseApdu responseApdu = expectReplies.poll();
                System.out.println(">> " + responseApdu);
                System.out.println(">> " + Hex.toHexString(responseApdu.toBytes()));
                return responseApdu;
            }
        });
    }

    @Test
    public void test_connectToDevice() throws Exception {
        SecurityTokenConnection securityTokenConnection =
                new SecurityTokenConnection(transport, new Passphrase("123456"), new OpenPgpCommandApduFactory());
        expect("00a4040006d27600012401", "9000"); // select openpgp applet
        expect("00ca006e00",
                "6e81de4f10d27600012401020000060364311500005f520f0073000080000000000000000000007381b7c00af" +
                        "00000ff04c000ff00ffc106010800001103c206010800001103c306010800001103c407007f7f7f03030" +
                        "3c53c4ec5fee25c4e89654d58cad8492510a89d3c3d8468da7b24e15bfc624c6a792794f15b7599915f7" +
                        "03aab55ed25424d60b17026b7b06c6ad4b9be30a3c63c000000000000000000000000000000000000000" +
                        "000000000000000000000000000000000000000000000000000000000000000000000000000000000cd0" +
                        "c59cd0f2a59cd0af059cd0c959000"); // get application related data


        securityTokenConnection.connectToDevice(RuntimeEnvironment.application);


        verify(transport).connect();
        verifyDialog();
    }

    @Test
    public void test_getTokenInfo() throws Exception {
        SecurityTokenConnection securityTokenConnection =
                new SecurityTokenConnection(transport, new Passphrase("123456"), new OpenPgpCommandApduFactory());
        OpenPgpCapabilities openPgpCapabilities = OpenPgpCapabilities.fromBytes(
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

        expect("00ca006500", "65095b005f2d005f3501399000");
        expect("00ca5f5000", "9000");


        securityTokenConnection.readTokenInfo();

        verifyDialog();
    }

    private void expect(String commandApdu, String responseApdu) {
        expect(CommandApdu.fromBytes(Hex.decode(commandApdu)), ResponseApdu.fromBytes(Hex.decode(responseApdu)));
    }

    private void expect(CommandApdu commandApdu, ResponseApdu responseApdu) {
        expectCommands.add(commandApdu);
        expectReplies.add(responseApdu);
    }

    private void verifyDialog() {
        assertTrue(expectCommands.isEmpty());
        assertTrue(expectReplies.isEmpty());
    }
}