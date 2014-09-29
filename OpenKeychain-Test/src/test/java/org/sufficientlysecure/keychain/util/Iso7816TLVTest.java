/* Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.util.Iso7816TLV.Iso7816CompositeTLV;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class Iso7816TLVTest {

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testDecode() throws Exception {

        // this is an Application Related Data packet, received from my Yubikey
        String input = "6e81dd4f10d27600012401020000000000000100005f520f0073000080000000000000000000007300c00af00000ff04c000ff00ffc106010800001103c206010800001103c306010800001103c407007f7f7f030303c53c1efdb4845ca242ca6977fddb1f788094fd3b430af1114c28a08d8c5afda81191cc50ca9bf51bc99fe8e6ca03a9d4d40e7b5925cd154813df381655b2c63c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000cd0c5423590e5423590e5423590e9000";
        byte[] data = Hex.decode(input);

        Iso7816TLV tlv = Iso7816TLV.readSingle(data, true);
        Assert.assertNotNull("tlv parse must succeed", tlv);

        Assert.assertEquals("top packet must be 'application related data' tag", 0x6e, tlv.mT);
        Assert.assertEquals("length must be correct", 221, tlv.mL);

        Assert.assertTrue("top packet must be composite", tlv instanceof Iso7816CompositeTLV);

        Iso7816CompositeTLV ctlv = (Iso7816CompositeTLV) tlv;

        Assert.assertEquals("top packet must have 11 sub packets", 11, ctlv.mSubs.length);

        Assert.assertEquals("sub packet #1 must have expected tag", 0x4f, ctlv.mSubs[0].mT);
        Assert.assertEquals("sub packet #1 must have expected length", 16, ctlv.mSubs[0].mL);

        Assert.assertEquals("sub packet #2 must have expected tag", 0x5f52, ctlv.mSubs[1].mT);
        Assert.assertEquals("sub packet #2 must have expected length", 15, ctlv.mSubs[1].mL);

        Assert.assertEquals("sub packet #3 must have expected tag", 0x73, ctlv.mSubs[2].mT);
        Assert.assertEquals("sub packet #3 must have expected length", 0, ctlv.mSubs[2].mL);
        Assert.assertTrue("sub packet #3 muse be composite", ctlv.mSubs[2] instanceof Iso7816CompositeTLV);

        Assert.assertEquals("sub packet #4 must have expected tag", 0xc0, ctlv.mSubs[3].mT);
        Assert.assertEquals("sub packet #4 must have expected length", 10, ctlv.mSubs[3].mL);

        Assert.assertEquals("sub packet #5 must have expected tag", 0xc1, ctlv.mSubs[4].mT);
        Assert.assertEquals("sub packet #5 must have expected length", 6, ctlv.mSubs[4].mL);

        Assert.assertEquals("sub packet #6 must have expected tag", 0xc2, ctlv.mSubs[5].mT);
        Assert.assertEquals("sub packet #6 must have expected length", 6, ctlv.mSubs[5].mL);

        Assert.assertEquals("sub packet #7 must have expected tag", 0xc3, ctlv.mSubs[6].mT);
        Assert.assertEquals("sub packet #7 must have expected length", 6, ctlv.mSubs[6].mL);

        Assert.assertEquals("sub packet #8 must have expected tag", 0xc4, ctlv.mSubs[7].mT);
        Assert.assertEquals("sub packet #8 must have expected length", 7, ctlv.mSubs[7].mL);

        Assert.assertEquals("sub packet #9 must have expected tag", 0xc5, ctlv.mSubs[8].mT);
        Assert.assertEquals("sub packet #9 must have expected length", 60, ctlv.mSubs[8].mL);

        {
            // this is my pubkey fingerprint
            String fingerprint = "1efdb4845ca242ca6977fddb1f788094fd3b430a";
            byte[] V1 = new byte[20];
            System.arraycopy(ctlv.mSubs[8].mV, 0, V1, 0, 20);
            Assert.assertArrayEquals("fingerprint must match", V1, Hex.decode(fingerprint));
        }

        Assert.assertEquals("sub packet #10 must have expected tag", 0xc6, ctlv.mSubs[9].mT);
        Assert.assertEquals("sub packet #10 must have expected length", 60, ctlv.mSubs[9].mL);

        Assert.assertEquals("sub packet #11 must have expected tag", 0xcd, ctlv.mSubs[10].mT);
        Assert.assertEquals("sub packet #11 must have expected length", 12, ctlv.mSubs[10].mL);

    }

}
