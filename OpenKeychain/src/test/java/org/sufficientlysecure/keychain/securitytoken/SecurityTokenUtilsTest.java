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
import org.sufficientlysecure.keychain.util.SecurityTokenUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.interfaces.RSAPrivateCrtKey;


@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 21, manifest = "src/main/AndroidManifest.xml")
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
}
