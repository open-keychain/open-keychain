package tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.testsupport.*;

import java.util.*;
import java.io.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class UncachedKeyringTest {

    @Test
    public void testCanonicalizeNoChanges() throws Exception {
        UncachedKeyRing expectedKeyRing = KeyringBuilder.correctRing();
        UncachedKeyRing inputKeyRing = KeyringBuilder.correctRing();
//        Uncomment to dump the encoded key for manual inspection
//        TestDataUtil.appendToOutput(new ByteArrayInputStream(inputKeyRing.getEncoded()), new FileOutputStream(new File("/tmp/key-encoded")));
        new UncachedKeyringTestingHelper().doTestCanonicalize(inputKeyRing, expectedKeyRing);
    }


    @Test
    public void testCanonicalizeExtraIncorrectSignature() throws Exception {
        UncachedKeyRing expectedKeyRing = KeyringBuilder.correctRing();
        UncachedKeyRing inputKeyRing = KeyringBuilder.ringWithExtraIncorrectSignature();
        new UncachedKeyringTestingHelper().doTestCanonicalize(inputKeyRing, expectedKeyRing);
    }

    /**
     * Check the original GnuPG-generated public key is OK
     */
    @Test
    public void testCanonicalizeOriginalGpg() throws Exception {
        byte[] data = TestDataUtil.readAllFully(Collections.singleton("/public-key-canonicalize.blob"));
        UncachedKeyRing inputKeyRing = UncachedKeyRing.decodeFromData(data);
        new UncachedKeyringTestingHelper().doTestCanonicalize(inputKeyRing, KeyringBuilder.correctRing());
    }


    /**
     * Just testing my own test code. Should really be using a library for this.
     */
    @Test
    public void testConcat() throws Exception {
        byte[] actual = TestDataUtil.concatAll(new byte[]{1}, new byte[]{2, -2}, new byte[]{5}, new byte[]{3});
        byte[] expected = new byte[]{1, 2, -2, 5, 3};
        Assert.assertEquals(java.util.Arrays.toString(expected), java.util.Arrays.toString(actual));
    }


}
