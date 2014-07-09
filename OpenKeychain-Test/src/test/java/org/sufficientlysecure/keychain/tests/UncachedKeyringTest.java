package org.sufficientlysecure.keychain.tests;

import android.app.Activity;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.service.OperationResultParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.testsupport.KeyringBuilder;
import org.sufficientlysecure.keychain.testsupport.KeyringTestingHelper;
import org.sufficientlysecure.keychain.testsupport.TestDataUtil;
import org.sufficientlysecure.keychain.ui.KeyListActivity;

import java.util.HashSet;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class UncachedKeyringTest {

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testCreateKey() throws Exception {
        Activity activity = Robolectric.buildActivity(KeyListActivity.class).create().get();

        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.addSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Constants.choice.algorithm.rsa, 1024, KeyFlags.CERTIFY_OTHER, null));
        // parcel.addSubKeys.add(new SubkeyAdd(algorithm.rsa, 1024, KeyFlags.SIGN_DATA, null));
        parcel.addUserIds.add("swagerinho");
        parcel.newPassphrase = "swag";
        PgpKeyOperation op = new PgpKeyOperation(null);

        OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();
        UncachedKeyRing ring = op.createSecretKeyRing(parcel, log, 0);

        if (ring == null) {
            log.print(activity);
            throw new AssertionError("oh no");
        }

        if (!"swagerinho".equals(ring.getMasterKeyId())) {
            log.print(activity);
            throw new AssertionError("oh noo");
        }

    }

    @Test
    public void testVerifySuccess() throws Exception {
        UncachedKeyRing expectedKeyRing = KeyringBuilder.ring2();
        UncachedKeyRing inputKeyRing = KeyringBuilder.ring1();
        // new UncachedKeyringTestingHelper().doTestCanonicalize(inputKeyRing, expectedKeyRing);

        OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();
        UncachedKeyRing canonicalizedRing = inputKeyRing.canonicalize(log, 0);

        if (canonicalizedRing == null) {
            throw new AssertionError("Canonicalization failed; messages: [" + log.toString() + "]");
        }

        HashSet onlyA = new HashSet<KeyringTestingHelper.Packet>();
        HashSet onlyB = new HashSet<KeyringTestingHelper.Packet>();
        Assert.assertTrue(KeyringTestingHelper.diffKeyrings(
                expectedKeyRing.getEncoded(), expectedKeyRing.getEncoded(), onlyA, onlyB));

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
