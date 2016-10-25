/*
 * Copyright (C) 2016 Nikita Mikhailov <nikita.s.mikhailov@gmail.com>
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


import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransportException;
import org.sufficientlysecure.keychain.util.SecurityTokenUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.interfaces.RSAPrivateCrtKey;


@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 23, manifest = "src/main/AndroidManifest.xml")
public class SecurityTokenUtilsTest extends Mockito {
    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testEncodeLength() throws Exception {
        // One byte
        Assert.assertArrayEquals(new byte[]{0x00}, SecurityTokenUtils.encodeLength(0));
        Assert.assertArrayEquals(new byte[]{0x01}, SecurityTokenUtils.encodeLength(1));
        Assert.assertArrayEquals(new byte[]{0x7f}, SecurityTokenUtils.encodeLength(127));

        // Two bytes
        Assert.assertArrayEquals(new byte[]{(byte) 0x81, (byte) 0x80},
                SecurityTokenUtils.encodeLength(128));
        Assert.assertArrayEquals(new byte[]{(byte) 0x81, (byte) 0xFF},
                SecurityTokenUtils.encodeLength(255));

        // Three bytes
        Assert.assertArrayEquals(new byte[]{(byte) 0x82, (byte) 0x01, 0x00},
                SecurityTokenUtils.encodeLength(256));
        Assert.assertArrayEquals(new byte[]{(byte) 0x82, (byte) 0xFF, (byte) 0xFF},
                SecurityTokenUtils.encodeLength(65535));

        // Four bytes
        Assert.assertArrayEquals(new byte[]{(byte) 0x83, (byte) 0x01, (byte) 0x00, (byte) 0x00},
                SecurityTokenUtils.encodeLength(65536));
        Assert.assertArrayEquals(new byte[]{(byte) 0x83, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF},
                SecurityTokenUtils.encodeLength(16777215));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeLengthNegative() throws Exception {
        SecurityTokenUtils.encodeLength(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeLengthTooBig() throws Exception {
        SecurityTokenUtils.encodeLength(256 * 256 * 256);
    }

    @Test
    public void testWriteBits() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        SecurityTokenUtils.writeBits(stream, new BigInteger("0"), 10);
        Assert.assertArrayEquals(new byte[10], stream.toByteArray());

        stream.reset();
        SecurityTokenUtils.writeBits(stream, new BigInteger("0"), 1);
        Assert.assertArrayEquals(new byte[1], stream.toByteArray());

        stream.reset();
        SecurityTokenUtils.writeBits(stream, new BigInteger("65537"), 3);
        Assert.assertArrayEquals(new byte[]{1, 0, 1}, stream.toByteArray());
        stream.reset();
        SecurityTokenUtils.writeBits(stream, new BigInteger("128"), 1);
        Assert.assertArrayEquals(new byte[]{(byte) 128}, stream.toByteArray());

        stream.reset();
        SecurityTokenUtils.writeBits(stream, new BigInteger("65537"), 4);
        Assert.assertArrayEquals(new byte[]{0, 1, 0, 1}, stream.toByteArray());

        stream.reset();
        SecurityTokenUtils.writeBits(stream, new BigInteger("65537"), 11);
        Assert.assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1}, stream.toByteArray());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteBitsInvalidValue() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        SecurityTokenUtils.writeBits(stream, new BigInteger("-1"), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteBitsInvalidBits() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        SecurityTokenUtils.writeBits(stream, new BigInteger("1"), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteBitsDoesntFit() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        SecurityTokenUtils.writeBits(stream, new BigInteger("65537"), 2);
    }

    @Test
    public void testPrivateKeyTemplateSimple2048() throws Exception {
        KeyFormat format = new KeyFormat(Hex.decode("010000001800"));
        RSAPrivateCrtKey key2048 = mock(RSAPrivateCrtKey.class);
        byte[] tmp = new byte[128];
        Arrays.fill(tmp, (byte) 0x11);
        when(key2048.getPrimeP()).thenReturn(new BigInteger(tmp));

        Arrays.fill(tmp, (byte) 0x12);
        when(key2048.getPrimeQ()).thenReturn(new BigInteger(tmp));

        when(key2048.getPublicExponent()).thenReturn(new BigInteger("65537"));

        Assert.assertArrayEquals(
                Hex.decode("4d820115" + // Header TL
                        "a400" + // CRT
                        "7f4808" + // 8 bytes
                        "9103" + // e
                        "928180" + // p
                        "938180" + // q
                        "5f48820103" +

                        "010001" +

                        "1111111111111111111111111111111111111111111111111111111111111111" +
                        "1111111111111111111111111111111111111111111111111111111111111111" +
                        "1111111111111111111111111111111111111111111111111111111111111111" +
                        "1111111111111111111111111111111111111111111111111111111111111111" +

                        "1212121212121212121212121212121212121212121212121212121212121212" +
                        "1212121212121212121212121212121212121212121212121212121212121212" +
                        "1212121212121212121212121212121212121212121212121212121212121212" +
                        "1212121212121212121212121212121212121212121212121212121212121212"
                ),
                SecurityTokenUtils.createPrivKeyTemplate(key2048, KeyType.AUTH, format));
    }

    @Test
    public void testCardCapabilities() throws UsbTransportException {
        CardCapabilities capabilities;

        // Yk neo
        capabilities = new CardCapabilities(Hex.decode("007300008000000000000000000000"));
        Assert.assertEquals(capabilities.hasChaining(), true);
        Assert.assertEquals(capabilities.hasExtended(), false);

        // Yk 4
        capabilities = new CardCapabilities(Hex.decode("0073000080059000"));
        Assert.assertEquals(capabilities.hasChaining(), true);
        Assert.assertEquals(capabilities.hasExtended(), false);

        // Nitrokey pro
        capabilities = new CardCapabilities(Hex.decode("0031c573c00140059000"));
        Assert.assertEquals(capabilities.hasChaining(), false);
        Assert.assertEquals(capabilities.hasExtended(), true);
    }

    @Test
    public void testOpenPgpCapabilities() throws IOException {
        byte[] data;

        // Yk-neo applet
        data = Hex.decode("6e81de4f10d27600012401020000000000000100005f520f0073000080000000" +
                "000000000000007381b7c00af00000ff04c000ff00ffc106010800001103c206" +
                "010800001103c306010800001103c407007f7f7f030303c53cce1d5a2158a4f1" +
                "8a7d853394e9e4c9efb468055fae77ab8ea3c68f053930e35f658fb62176e901" +
                "df03d249cac1e82f8289c7ffabe1a1af868620fa56c63c000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000cd0c5741e8695741e8695741e8" +
                "69");

        OpenPgpCapabilities caps = new OpenPgpCapabilities(data);

        Assert.assertEquals(caps.isHasSM(), true);
    }
    /*

yk-neo

6e81de4f10d27600012401020000000000000100005f520f0073000080000000000000000000007381b7c00af00000ff04c000ff00ffc106010800001103c206010800001103c306010800001103c407007f7f7f030303c53cce1d5a2158a4f18a7d853394e9e4c9efb468055fae77ab8ea3c68f053930e35f658fb62176e901df03d249cac1e82f8289c7ffabe1a1af868620fa56c63c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000cd0c5741e8695741e8695741e869

6e81de4f10d27600012401020000000000000100005f520f0073000080000000000000000000007381b7c00af00000ff04c000ff00ffc106010800001103c206010800001103c306010800001103c407007f7f7f030303c53c1a2f6436e422cd4f37f9e95775195c4984609678fbc5dd767789f1b304c9fba6f68a68ac563f71ae0000000000000000000000000000000000000000c63c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000cd0c5744793c5744793c00000000


007300008000000000000000000000


yk neo ng
6e81de4f10d27600012401020000060364703400005f520f0073000080000000000000000000007381b7c00af00000ff04c000ff00ffc106010800001103c206010800001103c306010800001103c407007f7f7f030302c53c7fdb876b9ebc534d674e63b7400207a0f9c34a50413b851137ced1b45d1b66526b4e93a9d5c4eed3585c34e7d38ec07d50f26e0554baa1867a038ed2c63c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000cd0c5711219f5711219f5711219f



6e81de4f10d27600012401020000060364703400005f520f0073000080000000000000000000007381b7c00af00000ff04c000ff00ffc106010800001103c206010800001103c306010800001103c407007f7f7f030302c53c7fdb876b9ebc534d674e63b7400207a0f9c34a50413b851137ced1b45d1b66526b4e93a9d5c4eed3585c34e7d38ec07d50f26e0554baa1867a038ed2c63c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000cd0c5711219f5711219f5711219f

6e81dd4f10d27600012401020000060301402200005f520f0073000080000000000000000000007300c00af00000ff04c000ff00ffc106010800001103c206010800001103c306010800001103c407007f7f7f030303c53c98b23401029d2666e129e0b97e2f7e6d1026a77c198dd46ed68a107aada3a267a82e3157a024f7b4be59004e379abaf8fe7abf28deacdd05542a5acac63c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000cd0c5741f0165741f0165741f016

yk4
6e81dd4f10d27600012401020100060417430400005f520800730000800590007f74038101207381b7c00a3c00000004c000ff00ffc106010800001100c206010800001100c306010800001100c407ff7f7f7f030003c53c3a9150768282a1ccf13ee80c4ef0217876ba06001ee6fc5c633b764583cc2d7144b76c0c83f3bdf6671d04d2b6ca89fbbd3940ea7203396c7b1ff1bac63c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000cd0c5741ef475741ef475741ef47
0073000080059000

nitrokey pro

4f10d2760001240102010005000038a100005f520a0031c573c001400590007381b7c00a7c000800080008000800c106010800002000c206010800002000c306010800002000c40700202020030003c53c5dcf5fc947201ef20636c448131e71ffecf7cbf7d3ed4826257b1340c5aedd2b15530eecf173fa890859306d64641ab01be054c3d6795052cedc3c38c63c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000cd0c5741eec65741eec65741eec6
0031c573c00140059000
     */
}
