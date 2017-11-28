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

    private final static byte[] RAW_RSA_SIGNATURE = Hex.decode(
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

    private final static byte[] SSH_RSA_SIGNATURE = Hex.decode(
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
    public void testRsa() throws Exception {
        byte[] out = SshSignatureConverter.getSshSignature(RAW_RSA_SIGNATURE, PublicKeyAlgorithmTags.RSA_SIGN);

        Assert.assertArrayEquals(SSH_RSA_SIGNATURE, out);
    }

    @Test
    public void testEdDsa() throws Exception {
        byte[] out = SshSignatureConverter.getSshSignature(RAW_EDDSA_SIGNATURE, PublicKeyAlgorithmTags.EDDSA);

        Assert.assertArrayEquals(SSH_EDDSA_SIGNATURE, out);
    }

    @Test
    public void testDsa() throws Exception {
        byte[] out = SshSignatureConverter.getSshSignature(RAW_DSA_SIGNATURE, PublicKeyAlgorithmTags.DSA);

        Assert.assertArrayEquals(SSH_DSA_SIGNATURE, out);
    }
}
