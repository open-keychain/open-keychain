package org.sufficientlysecure.keychain.tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.bcpg.BCPGInputStream;
import org.spongycastle.bcpg.Packet;
import org.spongycastle.bcpg.SecretKeyPacket;
import org.spongycastle.bcpg.SignaturePacket;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPSignature;
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
import org.sufficientlysecure.keychain.support.KeyringTestingHelper.RawPacket;
import org.sufficientlysecure.keychain.support.TestDataUtil;

import java.io.ByteArrayInputStream;
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
        UncachedKeyRing rawModified = op.modifySecretKeyRing(ring, parcel, "swag", log, 0);

        Assert.assertNotNull("key modification failed", rawModified);

        UncachedKeyRing modified = rawModified.canonicalize(log, 0);

        TreeSet<RawPacket> onlyA = new TreeSet<RawPacket>();
        TreeSet<RawPacket> onlyB = new TreeSet<RawPacket>();
        Assert.assertTrue("key must be constant through canonicalization",
                !KeyringTestingHelper.diffKeyrings(
                        modified.getEncoded(), rawModified.getEncoded(), onlyA, onlyB));

        Assert.assertTrue("keyring must differ from original", KeyringTestingHelper.diffKeyrings(
                ring.getUncached().getEncoded(), modified.getEncoded(), onlyA, onlyB));

        Assert.assertEquals("no extra packets in original", 0, onlyA.size());
        Assert.assertEquals("exactly two extra packets in modified", 2, onlyB.size());

        Iterator<RawPacket> it = onlyB.iterator();
        Packet p;

        p = new BCPGInputStream(new ByteArrayInputStream(it.next().buf)).readPacket();
        Assert.assertTrue("first new packet must be secret subkey", p instanceof SecretKeyPacket);

        p = new BCPGInputStream(new ByteArrayInputStream(it.next().buf)).readPacket();
        Assert.assertTrue("second new packet must be signature", p instanceof SignaturePacket);
        Assert.assertEquals("signature type must be subkey binding certificate",
                PGPSignature.SUBKEY_BINDING, ((SignaturePacket) p).getSignatureType());
        Assert.assertEquals("signature must have been created by master key",
                ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

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

        TreeSet onlyA = new TreeSet<RawPacket>();
        TreeSet onlyB = new TreeSet<RawPacket>();
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
