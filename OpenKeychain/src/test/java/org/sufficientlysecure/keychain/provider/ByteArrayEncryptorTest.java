/*
 * Copyright (C) 2016 Alex Fong Jie Wen <alexfongg@gmail.com>
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

package org.sufficientlysecure.keychain.provider;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor.IncorrectPassphraseException;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.TestingUtils;

import java.security.Security;
import java.util.Arrays;
import java.util.Random;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 21, manifest = "src/main/AndroidManifest.xml")
public class ByteArrayEncryptorTest {
    private static final int ARRAY_SIZE = 100;

    private byte[] mData = new byte[ARRAY_SIZE];
    private Passphrase mPassphrase;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        ShadowLog.stream = System.out;
    }

    @Before
    public void setup() {
        Random random = new Random();
        random.nextBytes(mData);
        mPassphrase = TestingUtils.genPassphrase();

    }

    @Test
    public void testEncrypt() throws Exception {
        byte[] encrypted;

        encrypted = ByteArrayEncryptor.encryptByteArray(mData, mPassphrase.getCharArray());
        Assert.assertFalse("byte mData should be different after encryption", Arrays.equals(mData, encrypted));
    }

    @Test
    public void testEncryptWithEmptyPassphrase() throws Exception {
        byte[] encrypted;

        encrypted = ByteArrayEncryptor.encryptByteArray(mData, new Passphrase().getCharArray());
        Assert.assertFalse("byte mData should be different after encryption", Arrays.equals(mData, encrypted));
    }

    @Test
    public void testDecrypt() throws Exception {
        byte[] encrypted;
        byte[] decrypted;

        // encrypt
        encrypted = ByteArrayEncryptor.encryptByteArray(mData, mPassphrase.getCharArray());
        Assert.assertFalse("byte data should be different after encryption", Arrays.equals(mData, encrypted));

        // try decrypting
        decrypted = ByteArrayEncryptor.decryptByteArray(encrypted, mPassphrase.getCharArray());
        Assert.assertTrue("byte data should be equal after decryption", Arrays.equals(mData, decrypted));
    }

    @Test
    public void testDecryptWithEmptyPassphrase() throws Exception {
        byte[] encrypted;
        byte[] decrypted;

        // encrypt
        encrypted = ByteArrayEncryptor.encryptByteArray(mData, new Passphrase().getCharArray());
        Assert.assertFalse("byte data should be different after encryption", Arrays.equals(mData, encrypted));

        // try decrypting
        decrypted = ByteArrayEncryptor.decryptByteArray(encrypted, new Passphrase().getCharArray());
        Assert.assertTrue("byte data should be equal after decryption", Arrays.equals(mData, decrypted));
    }

    @Test(expected = IncorrectPassphraseException.class)
    public void testDecryptWithWrongPassphrase() throws Exception {
        Passphrase otherPassphrase;
        do {
            otherPassphrase = TestingUtils.genPassphrase();
        } while(mPassphrase.equals(otherPassphrase));

        byte[] encrypted;

        // encrypt
        encrypted = ByteArrayEncryptor.encryptByteArray(mData, mPassphrase.getCharArray());
        Assert.assertFalse("byte data should be different after encryption", Arrays.equals(mData, encrypted));

        // try decrypting with a wrong passphrase
        ByteArrayEncryptor.decryptByteArray(encrypted, otherPassphrase.getCharArray());
    }

}
