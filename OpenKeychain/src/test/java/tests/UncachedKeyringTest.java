package tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.testsupport.*;
import org.sufficientlysecure.keychain.testsupport.KeyringBuilder;
import org.sufficientlysecure.keychain.testsupport.TestDataUtil;

import java.io.*;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class UncachedKeyringTest {

    @Test
    public void testVerifySuccess() throws Exception {
        UncachedKeyRing expectedKeyRing = KeyringBuilder.ring2();
        UncachedKeyRing inputKeyRing = KeyringBuilder.ring1();
//        Uncomment to dump the encoded key for manual inspection
//        inputKeyRing.getPublicKey().getPublicKey().encode(new FileOutputStream(new File("/tmp/key-encoded")));
        new UncachedKeyringTestingHelper().doTestCanonicalize(inputKeyRing, expectedKeyRing);
    }

    @Test
    public void testVerifyFromGpg() throws Exception {
        byte[] data = TestDataUtil.readAllFully(Collections.singleton( "/public-key-canonicalize.blob"));
        UncachedKeyRing inputKeyRing = UncachedKeyRing.decodeFromData(data);
        new UncachedKeyringTestingHelper().doTestCanonicalize(inputKeyRing, KeyringBuilder.ring2());
    }


    /**
     * Just testing my own test code. Should really be using a library for this.
     */
    @Test
    public void testConcat() throws Exception {
        byte[] actual = TestDataUtil.concatAll(new byte[]{1}, new byte[]{2,-2}, new byte[]{5},new byte[]{3});
        byte[] expected = new byte[]{1,2,-2,5,3};
        Assert.assertEquals(java.util.Arrays.toString(expected), java.util.Arrays.toString(actual));
    }


}
