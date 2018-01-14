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
import org.mockito.AdditionalMatchers;
import org.robolectric.shadows.ShadowLog;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.securitytoken.CommandApdu;
import org.sufficientlysecure.keychain.securitytoken.KeyType;
import org.sufficientlysecure.keychain.securitytoken.OpenPgpCapabilities;
import org.sufficientlysecure.keychain.securitytoken.OpenPgpCommandApduFactory;
import org.sufficientlysecure.keychain.securitytoken.ResponseApdu;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenConnection;
import org.sufficientlysecure.keychain.util.Passphrase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("WeakerAccess")
@RunWith(KeychainTestRunner.class)
public class SecurityTokenChangeKeyTokenOpTest {
    SecurityTokenChangeKeyTokenOp useCase;
    OpenPgpCommandApduFactory commandFactory;
    SecurityTokenConnection securityTokenConnection;

    CommandApdu dummyCommandApdu = mock(CommandApdu.class);

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;

        securityTokenConnection = mock(SecurityTokenConnection.class);

        commandFactory = mock(OpenPgpCommandApduFactory.class);
        when(securityTokenConnection.getCommandFactory()).thenReturn(commandFactory);

        useCase = SecurityTokenChangeKeyTokenOp.create(securityTokenConnection);

    }

    @Test
    public void testPutKey() throws Exception {
        OpenPgpCapabilities openPgpCapabilities = OpenPgpCapabilities.fromBytes(
                Hex.decode("6e81de4f10d27600012401020000060364311500005f520f0073000080000000000000000000007381b7c00af" +
                        "00000ff04c000ff00ffc106010800001103c206010800001103c306010800001103c407007f7f7f03" +
                        "0303c53c4ec5fee25c4e89654d58cad8492510a89d3c3d8468da7b24e15bfc624c6a792794f15b759" +
                        "9915f703aab55ed25424d60b17026b7b06c6ad4b9be30a3c63c000000000000000000000000000000" +
                        "000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                        "000000000cd0c59cd0f2a59cd0af059cd0c95"
                ));
        when(securityTokenConnection.getOpenPgpCapabilities()).thenReturn(openPgpCapabilities);

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

        byte[] expectedKeyData = Hex.decode(
                "4d8203a2b6007f48159103928180938180948180958180968180978201005f48820383010001be5e36a09458313f95" +
                        "94f3f76a972989dfa1d4a416f7f461c8a4ccf9b9de8ee59447e44b4a4833a00c655ae5516214a72efa5c140fd7d4" +
                        "29d9b15805c77c881e6ad10711b4614d2183497a5a6d36ed221146301ce6ccf42581004c313d533d14c57abc3288" +
                        "6419665b67091d652aa6cb074da682135115657d90752fb9d2e69fffd7580adddf1d7822d9d40220401056674b93" +
                        "3efeb3bc51eafe2c5a5162ec2b466591b689d9af07004bb81a509c7601043a2da871f5989e4e338b901afb9ba8f2" +
                        "b8bc18ac3300e750bda2a0886459363542bb5b59cab2ed934388c486b602a3f6a63164afe55139ad9f4ed230421b" +
                        "4039b09f5c0b7c609ba9927f1f7c4db7c3895bbe8e58787ddae295468d034a0c29964b80f3551d308722de7ac698" +
                        "e840eb68c81c75d140ac347e35d8167bd9ac175610f811f049061b72ebc491d89610fc6ba1344c8d03e2871181f0" +
                        "408f87779149a1b1835705b407baf28c30e94da82c8e15b845f2473deee6987f29a2d25232361818fd83283a0959" +
                        "2345ac56d9a56408ef5b19065d6d5252aeff1469c85686c61c4e62b541461320dbbb532d4a28e2d5a6da2c3e7c4d" +
                        "100204efd33b92a2ed85e2f2576eb6ee9a58415ea446ccad86dff497a45917080bbea1c0406647e1b16ba623b3f7" +
                        "913f14538db405cb9f108add09f9b3557b7d45b49c8d6cf7c69cb582ce3e3674b9a58b71ed49d2c7a2027955ba0a" +
                        "be596a11add7bfb5d2a08bd6ed9cdf2e0fc5b8e4396ecc8c801715569d89912f2a4336b5f75a9a04ae8ca460c626" +
                        "6c7830213f724c5957dc44054699fa1a9adc2c48472ede53a7b77ea3353ccf75394f1e65100eb49ccbdc603de36f" +
                        "2f11cece6e36a2587d4338466917d28edf0e75a8706748ddf64af3d3b4f129f991be3ffb024c13038806fb6d32f0" +
                        "dc20adb288fc190985dc9d0a976e108dcecffdf94b97a0de2f94ff6c8996fa6aaeeb97cc9d466fa8f92e2dd179c2" +
                        "4b46bd165a68efbdce4e397e841e44ffa48991fa23abbd6ff4d0c387a048a9ca323b4867c504d61af02048b4af78" +
                        "7b0994fd71b9bc39dda6a4f3b610297c8b35affde21a53ec4954c6b1da6403a7cb627555686acc779ca19fb14d7e" +
                        "c2ca3655571f36587e025bdc0ce053193a7c4be1db17e9ba5788edb43f81f02ef07cc7c1d967e8435fba00e986ab" +
                        "30fa69746857fc082b5b797d5eea3c6fb1be4a1a12bba89d4ca8c204e2702d93e9316b6176726121dd29c8a8b75e" +
                        "c19e1deb09e4cc3b95b054541d");

        ResponseApdu dummyResponseApdu = ResponseApdu.fromBytes(Hex.decode("9000"));

        when(commandFactory.createPutKeyCommand(AdditionalMatchers.aryEq(expectedKeyData))).thenReturn(dummyCommandApdu);
        when(securityTokenConnection.communicate(dummyCommandApdu)).thenReturn(dummyResponseApdu);

        Passphrase adminPin = new Passphrase("12345678");


        useCase.putKey(KeyType.SIGN, signKey, new Passphrase("123456"), adminPin);


        verify(securityTokenConnection).verifyAdminPin(adminPin);
        verify(securityTokenConnection).communicate(dummyCommandApdu);
    }

    UncachedKeyRing readRingFromResource(String name) throws Exception {
        return UncachedKeyRing.fromStream(SecurityTokenChangeKeyTokenOpTest.class.getResourceAsStream(name)).next();
    }
}