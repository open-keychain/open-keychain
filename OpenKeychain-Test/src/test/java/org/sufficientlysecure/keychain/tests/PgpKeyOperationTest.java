package org.sufficientlysecure.keychain.tests;

import junit.framework.AssertionFailedError;

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
import org.spongycastle.bcpg.SecretSubkeyPacket;
import org.spongycastle.bcpg.SignaturePacket;
import org.spongycastle.bcpg.UserIDPacket;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPSignature;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Constants.choice.algorithm;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.pgp.WrappedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.WrappedSignature;
import org.sufficientlysecure.keychain.service.OperationResultParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;
import org.sufficientlysecure.keychain.support.KeyringBuilder;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper.RawPacket;
import org.sufficientlysecure.keychain.support.TestDataUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class PgpKeyOperationTest {

    static UncachedKeyRing staticRing;
    UncachedKeyRing ring;
    PgpKeyOperation op;

    @BeforeClass public static void setUpOnce() throws Exception {
        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.addSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Constants.choice.algorithm.rsa, 1024, KeyFlags.CERTIFY_OTHER, null));
        parcel.addSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Constants.choice.algorithm.rsa, 1024, KeyFlags.SIGN_DATA, null));
        parcel.addSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Constants.choice.algorithm.rsa, 1024, KeyFlags.ENCRYPT_COMMS, null));

        parcel.addUserIds.add("twi");
        parcel.addUserIds.add("pink");
        parcel.newPassphrase = "swag";
        PgpKeyOperation op = new PgpKeyOperation(null);

        OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();
        staticRing = op.createSecretKeyRing(parcel, log, 0);
    }

    @Before public void setUp() throws Exception {
        // show Log.x messages in system.out
        ShadowLog.stream = System.out;
        ring = staticRing;

        // setting up some parameters just to reduce code duplication
        op = new PgpKeyOperation(null);

    }

    @Test
    // this is a special case since the flags are in user id certificates rather than
    // subkey binding certificates
    public void testMasterFlags() throws Exception {
        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.addSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Constants.choice.algorithm.rsa, 1024, KeyFlags.CERTIFY_OTHER | KeyFlags.SIGN_DATA, null));
        parcel.addUserIds.add("luna");
        OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();
        ring = op.createSecretKeyRing(parcel, log, 0);

        Assert.assertEquals("the keyring should contain only the master key",
                1, ring.getAvailableSubkeys().size());
        Assert.assertEquals("first (master) key must have both flags",
                KeyFlags.CERTIFY_OTHER | KeyFlags.SIGN_DATA, ring.getPublicKey().getKeyUsage());

    }

    @Test
    public void testCreatedKey() throws Exception {

        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.mMasterKeyId = ring.getMasterKeyId();
        parcel.mFingerprint = ring.getFingerprint();

        // an empty modification should change nothing. this also ensures the keyring
        // is constant through canonicalization.
        applyModificationWithChecks(parcel, ring);

        Assert.assertNotNull("key creation failed", ring);

        Assert.assertNull("primary user id must be empty",
                ring.getPublicKey().getPrimaryUserId());

        Assert.assertEquals("number of user ids must be two",
                2, ring.getPublicKey().getUnorderedUserIds().size());

        Assert.assertNull("expiry must be none",
                ring.getPublicKey().getExpiryTime());

        Assert.assertEquals("number of subkeys must be three",
                3, ring.getAvailableSubkeys().size());

        Iterator<UncachedPublicKey> it = ring.getPublicKeys();

        Assert.assertEquals("first (master) key can certify",
                KeyFlags.CERTIFY_OTHER, it.next().getKeyUsage());

        UncachedPublicKey signingKey = it.next();
        Assert.assertEquals("second key can sign",
                KeyFlags.SIGN_DATA, signingKey.getKeyUsage());
        ArrayList<WrappedSignature> sigs = signingKey.getSignatures().next().getEmbeddedSignatures();
        Assert.assertEquals("signing key signature should have one embedded signature",
                1, sigs.size());
        Assert.assertEquals("embedded signature should be of primary key binding type",
                PGPSignature.PRIMARYKEY_BINDING, sigs.get(0).getSignatureType());
        Assert.assertEquals("primary key binding signature issuer should be signing subkey",
                signingKey.getKeyId(), sigs.get(0).getKeyId());

        Assert.assertEquals("third key can encrypt",
                KeyFlags.ENCRYPT_COMMS, it.next().getKeyUsage());

    }

    @Test
    public void testSubkeyAdd() throws Exception {

        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.mMasterKeyId = ring.getMasterKeyId();
        parcel.mFingerprint = ring.getFingerprint();
        parcel.addSubKeys.add(new SubkeyAdd(algorithm.rsa, 1024, KeyFlags.SIGN_DATA, null));

        UncachedKeyRing modified = applyModificationWithChecks(parcel, ring);

        ArrayList<RawPacket> onlyA = new ArrayList<RawPacket>();
        ArrayList<RawPacket> onlyB = new ArrayList<RawPacket>();

        Assert.assertTrue("keyring must differ from original", KeyringTestingHelper.diffKeyrings(
                ring.getEncoded(), modified.getEncoded(), onlyA, onlyB));

        Assert.assertEquals("no extra packets in original", 0, onlyA.size());
        Assert.assertEquals("exactly two extra packets in modified", 2, onlyB.size());

        Iterator<RawPacket> it = onlyB.iterator();
        Packet p;

        p = new BCPGInputStream(new ByteArrayInputStream(it.next().buf)).readPacket();
        Assert.assertTrue("first new packet must be secret subkey", p instanceof SecretSubkeyPacket);

        p = new BCPGInputStream(new ByteArrayInputStream(it.next().buf)).readPacket();
        Assert.assertTrue("second new packet must be signature", p instanceof SignaturePacket);
        Assert.assertEquals("signature type must be subkey binding certificate",
                PGPSignature.SUBKEY_BINDING, ((SignaturePacket) p).getSignatureType());
        Assert.assertEquals("signature must have been created by master key",
                ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

    }

    @Test
    public void testUserIdAdd() throws Exception {

        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.mMasterKeyId = ring.getMasterKeyId();
        parcel.mFingerprint = ring.getFingerprint();
        parcel.addUserIds.add("rainbow");

        UncachedKeyRing modified = applyModificationWithChecks(parcel, ring);

        Assert.assertTrue("keyring must contain added user id",
                modified.getPublicKey().getUnorderedUserIds().contains("rainbow"));

        ArrayList<RawPacket> onlyA = new ArrayList<RawPacket>();
        ArrayList<RawPacket> onlyB = new ArrayList<RawPacket>();

        Assert.assertTrue("keyring must differ from original", KeyringTestingHelper.diffKeyrings(
                ring.getEncoded(), modified.getEncoded(), onlyA, onlyB));

        Assert.assertEquals("no extra packets in original", 0, onlyA.size());
        Assert.assertEquals("exactly two extra packets in modified", 2, onlyB.size());

        Iterator<RawPacket> it = onlyB.iterator();
        Packet p;

        Assert.assertTrue("keyring must contain added user id",
                modified.getPublicKey().getUnorderedUserIds().contains("rainbow"));

        p = new BCPGInputStream(new ByteArrayInputStream(it.next().buf)).readPacket();
        Assert.assertTrue("first new packet must be user id", p instanceof UserIDPacket);
        Assert.assertEquals("user id packet must match added user id",
                "rainbow", ((UserIDPacket) p).getID());

        p = new BCPGInputStream(new ByteArrayInputStream(it.next().buf)).readPacket();
        System.out.println(p.getClass().getName());
        Assert.assertTrue("second new packet must be signature", p instanceof SignaturePacket);
        Assert.assertEquals("signature type must be positive certification",
                PGPSignature.POSITIVE_CERTIFICATION, ((SignaturePacket) p).getSignatureType());

    }

    @Test
    public void testUserIdPrimary() throws Exception {

        UncachedKeyRing modified = ring;

        { // first part, add new user id which is also primary
            SaveKeyringParcel parcel = new SaveKeyringParcel();
            parcel.mMasterKeyId = modified.getMasterKeyId();
            parcel.mFingerprint = modified.getFingerprint();
            parcel.addUserIds.add("jack");
            parcel.changePrimaryUserId = "jack";

            modified = applyModificationWithChecks(parcel, modified);

            Assert.assertEquals("primary user id must be the one added",
                    "jack", modified.getPublicKey().getPrimaryUserId());
        }

        { // second part, change primary to a different one
            SaveKeyringParcel parcel = new SaveKeyringParcel();
            parcel.mMasterKeyId = ring.getMasterKeyId();
            parcel.mFingerprint = ring.getFingerprint();
            parcel.changePrimaryUserId = "pink";

            modified = applyModificationWithChecks(parcel, modified);

            ArrayList<RawPacket> onlyA = new ArrayList<RawPacket>();
            ArrayList<RawPacket> onlyB = new ArrayList<RawPacket>();

            Assert.assertTrue("keyring must differ from original", KeyringTestingHelper.diffKeyrings(
                    ring.getEncoded(), modified.getEncoded(), onlyA, onlyB));

            Assert.assertEquals("old keyring must have one outdated certificate", 1, onlyA.size());
            Assert.assertEquals("new keyring must have three new packets", 3, onlyB.size());

            Assert.assertEquals("primary user id must be the one changed to",
                    "pink", modified.getPublicKey().getPrimaryUserId());
        }

    }

    // applies a parcel modification while running some integrity checks
    private static UncachedKeyRing applyModificationWithChecks(SaveKeyringParcel parcel,
                                                               UncachedKeyRing ring) {
        try {

            Assert.assertTrue("modified keyring must be secret", ring.isSecret());
            WrappedSecretKeyRing secretRing = new WrappedSecretKeyRing(ring.getEncoded(), false, 0);

            PgpKeyOperation op = new PgpKeyOperation(null);
            OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();
            UncachedKeyRing rawModified = op.modifySecretKeyRing(secretRing, parcel, "swag", log, 0);
            Assert.assertNotNull("key modification failed", rawModified);
            UncachedKeyRing modified = rawModified.canonicalize(log, 0);

            ArrayList<RawPacket> onlyA = new ArrayList<RawPacket>();
            ArrayList<RawPacket> onlyB = new ArrayList<RawPacket>();
                Assert.assertTrue("key must be constant through canonicalization",
                        !KeyringTestingHelper.diffKeyrings(
                                modified.getEncoded(), rawModified.getEncoded(), onlyA, onlyB)
                );

            return modified;

        } catch (IOException e) {
            throw new AssertionFailedError("error during encoding!");
        }
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

        ArrayList onlyA = new ArrayList<RawPacket>();
        ArrayList onlyB = new ArrayList<RawPacket>();
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
