/*
 * Copyright (C) 2017 Christian Hagau <ach@hagau.se>
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

package org.sufficientlysecure.keychain.ssh.signature;


import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sufficientlysecure.keychain.KeychainTestRunner;


@RunWith(KeychainTestRunner.class)
public class SshSignatureConverterTest {

    private final static String CURVE_OID_NIST_P_256 = "1.2.840.10045.3.1.7";

    private final static byte[] RAW_ECDSA_SIGNATURE = Hex.decode(
            "3046" +
                    "0221" +
                    "00949fa9151d71495d9c020635dcedac6a" +
                    "8665d079b4f721b05a9408771e455fe2" +
                    "0221" +
                    "00df9c7a5ae59e5d2e42d3767e1525c825" +
                    "7cff82ac2664b6419ff66f6e8b9669a0");

    private final static byte[] SSH_ECDSA_SIGNATURE = Hex.decode(
            "00000013" +
                    "65636473612d736861322d6e6973747032" +
                    "3536" +
                    "0000004a" +
                    "00000021" +
                    "00949fa9151d71495d9c020635dcedac6a" +
                    "8665d079b4f721b05a9408771e455fe2" +
                    "00000021" +
                    "00df9c7a5ae59e5d2e42d3767e1525c825" +
                    "7cff82ac2664b6419ff66f6e8b9669a0"
    );

    private final static byte[] RAW_RSA_SIGNATURE_SHA512 = Hex.decode(
            "4aef4be2d8edaed6faf2798c28685970" +
                    "ef19528e534ab3961d4e1b86ce5cf52a" +
                    "2bc7008d5e6738783d799779daf23714" +
                    "d688761ddf537eae9edab5a3a6b4e913" +
                    "04b7c2ed434c0a9ebbe3ea747a8c9b89" +
                    "e1cfc44007c1a12a6f4401e951c4b1ac" +
                    "9add2f49251f12effa31540448d12fec" +
                    "70188e4844597d73af3fbf9cca65d182" +
                    "1809f4c41a453e01a2f86bedcc691ec0" +
                    "831ec0fa6af47927f60b2559c2d95235" +
                    "0ad91d12cd94acb44f33e6039de00368" +
                    "8a729ccc045a367108af4fa89d8ae049" +
                    "e5c75872ee6ff30d7edf7fea2fcf7fca" +
                    "88aab94388b752abbab04b937ad77282" +
                    "a8a15c20005cf24f4b5d9174955a7e86" +
                    "6214a25f7f66d39ef31a6503da43d5dc"
    );

    private final static byte[] SSH_RSA_SIGNATURE_SHA512 = Hex.decode(
            "0000000c7273612d736861322d353132" +
                    "000001004aef4be2d8edaed6faf2798c" +
                    "28685970ef19528e534ab3961d4e1b86" +
                    "ce5cf52a2bc7008d5e6738783d799779" +
                    "daf23714d688761ddf537eae9edab5a3" +
                    "a6b4e91304b7c2ed434c0a9ebbe3ea74" +
                    "7a8c9b89e1cfc44007c1a12a6f4401e9" +
                    "51c4b1ac9add2f49251f12effa315404" +
                    "48d12fec70188e4844597d73af3fbf9c" +
                    "ca65d1821809f4c41a453e01a2f86bed" +
                    "cc691ec0831ec0fa6af47927f60b2559" +
                    "c2d952350ad91d12cd94acb44f33e603" +
                    "9de003688a729ccc045a367108af4fa8" +
                    "9d8ae049e5c75872ee6ff30d7edf7fea" +
                    "2fcf7fca88aab94388b752abbab04b93" +
                    "7ad77282a8a15c20005cf24f4b5d9174" +
                    "955a7e866214a25f7f66d39ef31a6503" +
                    "da43d5dc"
    );

    private final static byte[] RAW_RSA_SIGNATURE_SHA256 = Hex.decode(
            "904abb6965d075584d03e3d31aec58bc" +
                    "3738388b199c6aef55ec7e7f18daeaff" +
                    "6ff41d0e5dbd47c3a4cceb4a59d24cdb" +
                    "3d0041bc64324ae9e955232fb788f180" +
                    "ed885814760e18f73572cdf15a0fcc3b" +
                    "05c534e110e75a2093d27c96a8d122f3" +
                    "b30590003c5d90fd8029ab940d4ce3cf" +
                    "6cdeac92490cc0c93fbc9998e1d1fd31" +
                    "b2478f8cdf0e3af80a570212aa06bc7d" +
                    "d92af482e8826bae92bb4df637d073bd" +
                    "75647911981051d8e146a2ceffa86f02" +
                    "3ccd5746525e9599f215bcd3940e980a" +
                    "9190b435bd308b464e9799f3c186beee" +
                    "d5536f577e21177405059ebc2fe7bb43" +
                    "d014a96bd1221fbc821a7f5fda223d5d" +
                    "1be231260b237f88ef89738891e7c768"
    );

    private final static byte[] SSH_RSA_SIGNATURE_SHA256 = Hex.decode(
            "0000000c7273612d736861322d323536" +
                    "00000100904abb6965d075584d03e3d3" +
                    "1aec58bc3738388b199c6aef55ec7e7f" +
                    "18daeaff6ff41d0e5dbd47c3a4cceb4a" +
                    "59d24cdb3d0041bc64324ae9e955232f" +
                    "b788f180ed885814760e18f73572cdf1" +
                    "5a0fcc3b05c534e110e75a2093d27c96" +
                    "a8d122f3b30590003c5d90fd8029ab94" +
                    "0d4ce3cf6cdeac92490cc0c93fbc9998" +
                    "e1d1fd31b2478f8cdf0e3af80a570212" +
                    "aa06bc7dd92af482e8826bae92bb4df6" +
                    "37d073bd75647911981051d8e146a2ce" +
                    "ffa86f023ccd5746525e9599f215bcd3" +
                    "940e980a9190b435bd308b464e9799f3" +
                    "c186beeed5536f577e21177405059ebc" +
                    "2fe7bb43d014a96bd1221fbc821a7f5f" +
                    "da223d5d1be231260b237f88ef897388" +
                    "91e7c768"
    );

    private final static byte[] RAW_RSA_SIGNATURE_SHA1 = Hex.decode(
            "1c975c37a4137e9c861d20d9d40b6db16d" +
                    "1da8b17e360311b6a4ebcb3f1ff51d4906" +
                    "28b80de0dece08a1b5ebe8a5894ea2fea7" +
                    "40741e7c83c241a0d2bd9bdb3a2f3942ca" +
                    "e8ccc3bda7a17b40b00a0e214a5da76542" +
                    "11f5fc49b45d16b1e46fa80ce777969c51" +
                    "9f09bb45e312e4109b3af0c3133ffa221d" +
                    "a9e3c9e03fa2fdb70df03e6c83ee71f106" +
                    "b8f24fd72bad5e4e68123dda656ddba8ee" +
                    "11f9106154d1e1370bff3ba22e3c25b7d9" +
                    "334d903e4dd79a7389da41e9437e79ddd8" +
                    "a3335d2c217f01059bde2f3450f8933f38" +
                    "be10cd59467e9c9332c7794ccb9d19cb65" +
                    "a179b0166cd0e583e17f8f312222259ae3" +
                    "1b13e61fcae4da5c5554e2355218a0eb07" +
                    "19"
    );

    private final static byte[] SSH_RSA_SIGNATURE_SHA1 = Hex.decode(
            "00000007" +
                    "7373682d727361" +
                    "00000100" +
                    "1c975c37a4137e9c861d20d9d40b6db16d" +
                    "1da8b17e360311b6a4ebcb3f1ff51d4906" +
                    "28b80de0dece08a1b5ebe8a5894ea2fea7" +
                    "40741e7c83c241a0d2bd9bdb3a2f3942ca" +
                    "e8ccc3bda7a17b40b00a0e214a5da76542" +
                    "11f5fc49b45d16b1e46fa80ce777969c51" +
                    "9f09bb45e312e4109b3af0c3133ffa221d" +
                    "a9e3c9e03fa2fdb70df03e6c83ee71f106" +
                    "b8f24fd72bad5e4e68123dda656ddba8ee" +
                    "11f9106154d1e1370bff3ba22e3c25b7d9" +
                    "334d903e4dd79a7389da41e9437e79ddd8" +
                    "a3335d2c217f01059bde2f3450f8933f38" +
                    "be10cd59467e9c9332c7794ccb9d19cb65" +
                    "a179b0166cd0e583e17f8f312222259ae3" +
                    "1b13e61fcae4da5c5554e2355218a0eb07" +
                    "19"
    );

    private final static byte[] RAW_EDDSA_SIGNATURE = Hex.decode(
            "554946e827c6fd4b21b7a81a977a745331" +
                    "0e18c005403bfa4ddd87158b56b140fd61" +
                    "0bf15d7f38a32b55713fd38087ac8612dc" +
                    "1456cec315e4643b6d2489070a"
    );

    private final static byte[] SSH_EDDSA_SIGNATURE = Hex.decode(
            "0000000b" +
                    "7373682d65643235353139" +
                    "00000040" +
                    "554946e827c6fd4b21b7a81a977a745331" +
                    "0e18c005403bfa4ddd87158b56b140fd61" +
                    "0bf15d7f38a32b55713fd38087ac8612dc" +
                    "1456cec315e4643b6d2489070a"
    );

    private final static byte[] RAW_DSA_SIGNATURE = Hex.decode(
            "3046" +
                    "0221" +
                    "00defdb8a25fb8660a3cab24510a200a01" +
                    "3eb9c677e4caed19a349a9af3b8c971a" +
                    "0221" +
                    "00e7e4d5b8f08ab5cb4d445f03c458ccce" +
                    "3dff26a20fb314508604d1ca3e2c9125"
    );

    private final static byte[] SSH_DSA_SIGNATURE = Hex.decode(
            "00000007" +
                    "7373682d647373" +
                    "00000040" +
                    "defdb8a25fb8660a3cab24510a200a013e" +
                    "b9c677e4caed19a349a9af3b8c971ae7e4" +
                    "d5b8f08ab5cb4d445f03c458ccce3dff26" +
                    "a20fb314508604d1ca3e2c9125"
    );

    @Test
    public void testEcDsa() throws Exception {
        byte[] out = SshSignatureConverter.getSshSignatureEcDsa(RAW_ECDSA_SIGNATURE, CURVE_OID_NIST_P_256);

        Assert.assertArrayEquals(SSH_ECDSA_SIGNATURE, out);
    }

    @Test
    public void testRsaSha1() throws Exception {
        byte[] out = SshSignatureConverter.getSshSignatureRsa(RAW_RSA_SIGNATURE_SHA1, HashAlgorithmTags.SHA1);

        Assert.assertArrayEquals(SSH_RSA_SIGNATURE_SHA1, out);
    }

    @Test
    public void testRsaSha256() throws Exception {
        byte[] out = SshSignatureConverter.getSshSignatureRsa(RAW_RSA_SIGNATURE_SHA256, HashAlgorithmTags.SHA256);

        Assert.assertArrayEquals(SSH_RSA_SIGNATURE_SHA256, out);
    }

    @Test
    public void testRsaSha512() throws Exception {
        byte[] out = SshSignatureConverter.getSshSignatureRsa(RAW_RSA_SIGNATURE_SHA512, HashAlgorithmTags.SHA512);

        Assert.assertArrayEquals(SSH_RSA_SIGNATURE_SHA512, out);
    }

    @Test
    public void testEdDsa() throws Exception {
        byte[] out = SshSignatureConverter.getSshSignatureEdDsa(RAW_EDDSA_SIGNATURE);

        Assert.assertArrayEquals(SSH_EDDSA_SIGNATURE, out);
    }

    @Test
    public void testDsa() throws Exception {
        byte[] out = SshSignatureConverter.getSshSignatureDsa(RAW_DSA_SIGNATURE);

        Assert.assertArrayEquals(SSH_DSA_SIGNATURE, out);
    }
}
