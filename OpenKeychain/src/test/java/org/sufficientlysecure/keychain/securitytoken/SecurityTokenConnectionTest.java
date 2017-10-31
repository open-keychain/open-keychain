package org.sufficientlysecure.keychain.securitytoken;


import java.io.IOException;
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
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
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

        expect("00ca006500", "65095b005f2d005f3501399000");
        expect("00ca5f5000", "9000");


        securityTokenConnection.getTokenInfo();


        verifyDialog();
    }

    @Test
    public void testPutKey() throws Exception {
        /*
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        ShadowLog.stream = System.out;

        SaveKeyringParcel.Builder builder = SaveKeyringParcel.buildNewKeyringParcel();
        builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                Algorithm.RSA, 2048, null, KeyFlags.CERTIFY_OTHER | KeyFlags.SIGN_DATA, 0L));

        builder.addUserId("gnuk");
        builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(new Passphrase()));

        PgpKeyOperation op = new PgpKeyOperation(null);

        PgpEditKeyResult result = op.createSecretKeyRing(builder.build());
        Assert.assertTrue("initial test key creation must succeed", result.success());
        Assert.assertNotNull("initial test key creation must succeed", result.getRing());
        */

        UncachedKeyRing staticRing = readRingFromResource("/test-keys/token-import-rsa.sec");

        CanonicalizedSecretKeyRing canRing = (CanonicalizedSecretKeyRing) staticRing.canonicalize(new OperationLog(), 0);
        CanonicalizedSecretKey signKey = canRing.getSecretKey();
        signKey.unlock(null);

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

        expect("00200083083132333435363738", "9000");
        expect("10db3fffff4d8203a2b6007f48159103928180938180948180958180968180978201005f48820383010001be5e36a094" +
                "58313f9594f3f76a972989dfa1d4a416f7f461c8a4ccf9b9de8ee59447e44b4a4833a00c655ae5516214a72efa5c140" +
                "fd7d429d9b15805c77c881e6ad10711b4614d2183497a5a6d36ed221146301ce6ccf42581004c313d533d14c57abc32" +
                "886419665b67091d652aa6cb074da682135115657d90752fb9d2e69fffd7580adddf1d7822d9d40220401056674b933" +
                "efeb3bc51eafe2c5a5162ec2b466591b689d9af07004bb81a509c7601043a2da871f5989e4e338b901afb9ba8f2b8bc" +
                "18ac3300e750bda2a0886459363542bb5b59cab2ed93", "9000");
        expect("10db3fffff4388c486b602a3f6a63164afe55139ad9f4ed230421b4039b09f5c0b7c609ba9927f1f7c4db7c3895bbe8e" +
                "58787ddae295468d034a0c29964b80f3551d308722de7ac698e840eb68c81c75d140ac347e35d8167bd9ac175610f81" +
                "1f049061b72ebc491d89610fc6ba1344c8d03e2871181f0408f87779149a1b1835705b407baf28c30e94da82c8e15b8" +
                "45f2473deee6987f29a2d25232361818fd83283a09592345ac56d9a56408ef5b19065d6d5252aeff1469c85686c61c4" +
                "e62b541461320dbbb532d4a28e2d5a6da2c3e7c4d100204efd33b92a2ed85e2f2576eb6ee9a58415ea446ccad86dff4" +
                "97a45917080bbea1c0406647e1b16ba623b3f7913f14", "9000");
        expect("10db3fffff538db405cb9f108add09f9b3557b7d45b49c8d6cf7c69cb582ce3e3674b9a58b71ed49d2c7a2027955ba0a" +
                "be596a11add7bfb5d2a08bd6ed9cdf2e0fc5b8e4396ecc8c801715569d89912f2a4336b5f75a9a04ae8ca460c6266c7" +
                "830213f724c5957dc44054699fa1a9adc2c48472ede53a7b77ea3353ccf75394f1e65100eb49ccbdc603de36f2f11ce" +
                "ce6e36a2587d4338466917d28edf0e75a8706748ddf64af3d3b4f129f991be3ffb024c13038806fb6d32f0dc20adb28" +
                "8fc190985dc9d0a976e108dcecffdf94b97a0de2f94ff6c8996fa6aaeeb97cc9d466fa8f92e2dd179c24b46bd165a68" +
                "efbdce4e397e841e44ffa48991fa23abbd6ff4d0c387", "9000");
        expect("00db3fffa9a048a9ca323b4867c504d61af02048b4af787b0994fd71b9bc39dda6a4f3b610297c8b35affde21a53ec49" +
                "54c6b1da6403a7cb627555686acc779ca19fb14d7ec2ca3655571f36587e025bdc0ce053193a7c4be1db17e9ba5788e" +
                "db43f81f02ef07cc7c1d967e8435fba00e986ab30fa69746857fc082b5b797d5eea3c6fb1be4a1a12bba89d4ca8c204" +
                "e2702d93e9316b6176726121dd29c8a8b75ec19e1deb09e4cc3b95b054541d", "9000");


        securityTokenConnection.putKey(KeyType.SIGN, signKey, new Passphrase("123456"), new Passphrase("12345678"));


        verifyDialog();
    }

    private void debugTransceive() throws IOException {
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

    UncachedKeyRing readRingFromResource(String name) throws Exception {
        return UncachedKeyRing.fromStream(SecurityTokenConnectionTest.class.getResourceAsStream(name)).next();
    }
}