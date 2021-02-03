package org.sufficientlysecure.keychain.securitytoken;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLog;
import org.sufficientlysecure.keychain.KeychainTestRunner;

@RunWith(KeychainTestRunner.class)
public class KdfCalculatorTest {

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testCalculateKdf() {
        KdfCalculator.KdfCalculatorArguments arguments = new KdfCalculator.KdfCalculatorArguments();
        arguments.digestAlgorithm = KdfParameters.HashType.SHA256;
        arguments.salt = Hex.decode("3031323334353637");
        arguments.iterations = 100000;

        byte[] pin = Hex.decode("313233343536");
        byte[] expected = Hex.decode(
                "773784A602B6C81E3F092F4D7D00E17CC822D88F7360FCF2D2EF2D9D901F44B6");

        byte[] result = KdfCalculator.calculateKdf(arguments, pin);

        Assert.assertArrayEquals(
                "Result of iterated & salted S2K KDF not equal to test vector"
                , result
                , expected);
    }
}
