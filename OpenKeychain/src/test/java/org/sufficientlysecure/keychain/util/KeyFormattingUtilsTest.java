package org.sufficientlysecure.keychain.util;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;


@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 21, manifest = "src/main/AndroidManifest.xml")
public class KeyFormattingUtilsTest {

    static final byte[] fp = new byte[] {
        (byte) 0xD4, (byte) 0xAB, (byte) 0x19, (byte) 0x29, (byte) 0x64,
        (byte) 0xF7, (byte) 0x6A, (byte) 0x7F, (byte) 0x8F, (byte) 0x8A,
        (byte) 0x9B, (byte) 0x35, (byte) 0x7B, (byte) 0xD1, (byte) 0x83,
        (byte) 0x20, (byte) 0xDE, (byte) 0xAD, (byte) 0xFA, (byte) 0x11
    };
    static final long keyId = 0x7bd18320deadfa11L;

    @Test
    public void testStuff() {
        Assert.assertEquals(KeyFormattingUtils.convertFingerprintToKeyId(fp), keyId);

        Assert.assertEquals(
            "d4ab192964f76a7f8f8a9b357bd18320deadfa11",
            KeyFormattingUtils.convertFingerprintToHex(fp)
        );

        Assert.assertEquals(
            "0x7bd18320deadfa11",
            KeyFormattingUtils.convertKeyIdToHex(keyId)
        );

        Assert.assertEquals(
                "0xdeadfa11",
                KeyFormattingUtils.convertKeyIdToHexShort(keyId)
        );

    }

}
