/*
 * Copyright (C) Art O Cathain, Vincent Breitmoser
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

package tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.service.OperationResultParcel;
import org.sufficientlysecure.keychain.testsupport.*;
import org.sufficientlysecure.keychain.testsupport.KeyringBuilder;
import org.sufficientlysecure.keychain.testsupport.KeyringTestingHelper;
import org.sufficientlysecure.keychain.testsupport.TestDataUtil;

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

        OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();
        UncachedKeyRing canonicalizedRing = inputKeyRing.canonicalize(log, 0);

        if (canonicalizedRing == null) {
            throw new AssertionError("Canonicalization failed; messages: [" + log.toString() + "]");
        }

        HashSet onlyA = new HashSet<KeyringTestingHelper.Packet>();
        HashSet onlyB = new HashSet<KeyringTestingHelper.Packet>();
        Assert.assertTrue(KeyringTestingHelper.diffKeyrings(
                canonicalizedRing.getEncoded(), expectedKeyRing.getEncoded(), onlyA, onlyB));


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
