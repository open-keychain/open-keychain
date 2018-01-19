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

package org.sufficientlysecure.keychain.securitytoken;


import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.securitytoken.operations.PsoDecryptTokenOp;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;


@RunWith(KeychainTestRunner.class)
//@Ignore("Only for reference right now")
public class SecurityTokenConnectionCompatTest {
    private byte[] encryptedSessionKey;
    private OpenPgpCommandApduFactory openPgpCommandApduFactory;

    @Before
    public void setUp() throws Exception {
        encryptedSessionKey = Hex.decode("07ff7b9ff36f70da1fe7a6b59168c24a7e5b48a938c4f970de46524a06ebf4a9175a9737cf2e6f30957110b31db70e9a2992401b1d5e99389f976356f4e3a28ff537362e7ce14b81200e21d4f0e77d46bd89f3a54ca06062289148a59387488ac01d30d2baf58e6b35e32434720473604a9f7d5083ca6d40e4a2dadedd68033a4d4bbdb06d075d6980c0c0ca19078dcdfb9d8cbcb34f28d0b968b6e09eda0e1d3ab6b251eb09f9fb9d9abfeaf9010001733b9015e9e4b6c9df61bbc76041f439d1273e41f5d0e8414a2b8d6d4c7e86f30b94cfba308b38de53d694a8ca15382301ace806c8237641b03525b3e3e8cbb017e251265229bcbb0da5d5aeb4eafbad9779");

        openPgpCommandApduFactory = new OpenPgpCommandApduFactory();
    }

    /* we have a report of breaking compatibility on some earlier version.
        this test checks what was sent in that version to what we send now.
    // see https://github.com/open-keychain/open-keychain/issues/2049
    // see https://github.com/open-keychain/open-keychain/commit/ee8cd3862f65de580ed949bc838628610e22cd98
    */

    @Test
    public void testPrePostEquals() throws Exception {
        List<String> preApdus = decryptPre_ee8cd38();
        List<String> postApdus = decryptNow();

        assertEquals(preApdus, postApdus);
    }

    public List<String> decryptPre_ee8cd38() {
        final int MAX_APDU_DATAFIELD_SIZE = 254;

        int offset = 1; // Skip first byte
        List<String> apduData = new ArrayList<>();

        // Transmit
        while (offset < encryptedSessionKey.length) {
            boolean isLastCommand = offset + MAX_APDU_DATAFIELD_SIZE < encryptedSessionKey.length;
            String cla = isLastCommand ? "10" : "00";

            int len = Math.min(MAX_APDU_DATAFIELD_SIZE, encryptedSessionKey.length - offset);
            apduData.add(cla + "2a8086" + Hex.toHexString(new byte[]{(byte) len})
                    + Hex.toHexString(encryptedSessionKey, offset, len));

            offset += MAX_APDU_DATAFIELD_SIZE;
        }

        return apduData;
    }

    public List<String> decryptNow() throws Exception {
        PsoDecryptTokenOp psoDecryptTokenOp = PsoDecryptTokenOp.create(mock(SecurityTokenConnection.class));
        byte[] psoDecipherPayload = psoDecryptTokenOp.getRsaOperationPayload(encryptedSessionKey);

        CommandApdu command = openPgpCommandApduFactory.createDecipherCommand(psoDecipherPayload);
        List<CommandApdu> chainedApdus = openPgpCommandApduFactory.createChainedApdus(command);

        List<String> apduData = new ArrayList<>();
        for (CommandApdu chainCommand : chainedApdus) {
            apduData.add(Hex.toHexString(chainCommand.toBytes()));
        }

        return apduData;
    }
}