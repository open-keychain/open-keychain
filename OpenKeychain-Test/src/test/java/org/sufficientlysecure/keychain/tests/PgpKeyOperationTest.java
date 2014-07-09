package org.sufficientlysecure.keychain.tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.bcpg.PacketTags;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Constants.choice.algorithm;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.WrappedSecretKeyRing;
import org.sufficientlysecure.keychain.service.OperationResultParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;
import org.sufficientlysecure.keychain.support.KeyringBuilder;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper.Packet;
import org.sufficientlysecure.keychain.support.TestDataUtil;

import java.util.Iterator;
import java.util.TreeSet;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class PgpKeyOperationTest {

    static WrappedSecretKeyRing staticRing;
    WrappedSecretKeyRing ring;
    PgpKeyOperation op;

    @BeforeClass public static void setUpOnce() throws Exception {
        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.addSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Constants.choice.algorithm.rsa, 1024, KeyFlags.CERTIFY_OTHER, null));

        parcel.addUserIds.add("swagerinho");
        parcel.newPassphrase = "swag";
        PgpKeyOperation op = new PgpKeyOperation(null);

        OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();
        UncachedKeyRing ring = op.createSecretKeyRing(parcel, log, 0);
        staticRing = new WrappedSecretKeyRing(ring.getEncoded(), false, 0);
    }

    @Before public void setUp() throws Exception {
        // show Log.x messages in system.out
        ShadowLog.stream = System.out;
        ring = staticRing;

        op = new PgpKeyOperation(null);
    }

    @Test
    public void testCreatedKey() throws Exception {

        // parcel.addSubKeys.add(new SubkeyAdd(algorithm.rsa, 1024, KeyFlags.SIGN_DATA, null));

        Assert.assertNotNull("key creation failed", ring);

        Assert.assertEquals("incorrect primary user id",
                "swagerinho", ring.getPrimaryUserId());

        Assert.assertEquals("wrong number of subkeys",
                1, ring.getUncachedKeyRing().getAvailableSubkeys().size());

    }

    @Test
    public void testSubkeyAdd() throws Exception {

        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.mMasterKeyId = ring.getMasterKeyId();
        parcel.mFingerprint = ring.getUncached().getFingerprint();
        parcel.addSubKeys.add(new SubkeyAdd(algorithm.rsa, 1024, KeyFlags.SIGN_DATA, null));

        OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();
        UncachedKeyRing modified = op.modifySecretKeyRing(ring, parcel, "swag", log, 0);

        Assert.assertNotNull("key modification failed", modified);

        TreeSet<Packet> onlyA = new TreeSet<Packet>();
        TreeSet<Packet> onlyB = new TreeSet<Packet>();
        Assert.assertTrue("keyrings do not differ", KeyringTestingHelper.diffKeyrings(
                ring.getUncached().getEncoded(), modified.getEncoded(), onlyA, onlyB));

        Assert.assertEquals("no extra packets in original", onlyA.size(), 0);
        Assert.assertEquals("two extra packets in modified", onlyB.size(), 2);
        Iterator<Packet> it = onlyB.iterator();
        Assert.assertEquals("first new packet must be secret subkey", it.next().tag, PacketTags.SECRET_SUBKEY);
        Assert.assertEquals("second new packet must be signature", it.next().tag, PacketTags.SIGNATURE);

    }

    @Test
    public void testVerifySuccess() throws Exception {

        UncachedKeyRing expectedKeyRing = KeyringBuilder.correctRing();
        UncachedKeyRing inputKeyRing = KeyringBuilder.ringWithExtraIncorrectSignature();

        OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();
        UncachedKeyRing canonicalizedRing = inputKeyRing.canonicalize(log, 0);

        if (canonicalizedRing == null) {
            throw new AssertionError("Canonicalization failed; messages: [" + log + "]");
        }

        TreeSet onlyA = new TreeSet<KeyringTestingHelper.Packet>();
        TreeSet onlyB = new TreeSet<KeyringTestingHelper.Packet>();
        Assert.assertTrue("keyrings differ", !KeyringTestingHelper.diffKeyrings(
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
