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
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor.IncorrectPassphraseException;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
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
