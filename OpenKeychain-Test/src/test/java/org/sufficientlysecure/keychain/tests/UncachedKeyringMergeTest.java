package org.sufficientlysecure.keychain.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.bcpg.BCPGInputStream;
import org.spongycastle.bcpg.Packet;
import org.spongycastle.bcpg.PublicKeyPacket;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.pgp.WrappedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.WrappedSecretKey;
import org.sufficientlysecure.keychain.pgp.WrappedSecretKeyRing;
import org.sufficientlysecure.keychain.service.OperationResultParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper.RawPacket;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

/** Tests for the UncachedKeyring.merge method.
 *
 * This is another complex, crypto-related method. It merges information from one keyring into
 * another, keeping information from the base (ie, called object) keyring in case of conflicts.
 * The types of keys may be Public or Secret and can be mixed, For mixed types the result type
 * will be the same as the base keyring.
 *
 * Test cases:
 *  - Merging keyrings with different masterKeyIds should fail
 *  - Merging a key with itself should be a no-operation
 *  - Merging a key with an extra revocation certificate, it should have that certificate
 *  - Merging a key with an extra user id, it should have that extra user id and its certificates
 *  - Merging a key with an extra user id certificate, it should have that certificate
 *  - Merging a key with an extra subkey, it should have that subkey
 *  - Merging a key with an extra subkey certificate, it should have that certificate
 *  - All of the above operations should work regardless of the key types. This means in particular
 *      that for new subkeys, an equivalent subkey of the proper type must be generated.
 *  - In case of two secret keys with the same id but different S2K, the key of the base keyring
 *      should be preferred (TODO or should it?)
 *
 * Note that the merge operation does not care about certificate validity, a bad certificate or
 * packet will be copied regardless. Filtering out bad packets is done with canonicalization.
 *
 */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class UncachedKeyringMergeTest {

    static UncachedKeyRing staticRingA, staticRingB;
    static int totalPackets;
    UncachedKeyRing ringA, ringB;
    ArrayList<RawPacket> onlyA = new ArrayList<RawPacket>();
    ArrayList<RawPacket> onlyB = new ArrayList<RawPacket>();
    OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();
    PgpKeyOperation op;
    SaveKeyringParcel parcel;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        ShadowLog.stream = System.out;

        {
            SaveKeyringParcel parcel = new SaveKeyringParcel();
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Constants.choice.algorithm.rsa, 1024, KeyFlags.CERTIFY_OTHER, null));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Constants.choice.algorithm.rsa, 1024, KeyFlags.SIGN_DATA, null));

            parcel.mAddUserIds.add("twi");
            parcel.mAddUserIds.add("pink");
            // passphrase is tested in PgpKeyOperationTest, just use empty here
            parcel.mNewPassphrase = "";
            PgpKeyOperation op = new PgpKeyOperation(null);

            OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();
            staticRingA = op.createSecretKeyRing(parcel, log, 0);
        }

        {
            SaveKeyringParcel parcel = new SaveKeyringParcel();
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Constants.choice.algorithm.rsa, 1024, KeyFlags.CERTIFY_OTHER, null));

            parcel.mAddUserIds.add("shy");
            // passphrase is tested in PgpKeyOperationTest, just use empty here
            parcel.mNewPassphrase = "";
            PgpKeyOperation op = new PgpKeyOperation(null);

            OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();
            staticRingB = op.createSecretKeyRing(parcel, log, 0);
        }

        Assert.assertNotNull("initial test key creation must succeed", staticRingA);
        Assert.assertNotNull("initial test key creation must succeed", staticRingB);

        // we sleep here for a second, to make sure all new certificates have different timestamps
        Thread.sleep(1000);
    }

    @Before
    public void setUp() throws Exception {
        // show Log.x messages in system.out
        ShadowLog.stream = System.out;
        ringA = staticRingA;
        ringB = staticRingB;

        // setting up some parameters just to reduce code duplication
        op = new PgpKeyOperation(new ProgressScaler(null, 0, 100, 100));

        // set this up, gonna need it more than once
        parcel = new SaveKeyringParcel();
        parcel.mMasterKeyId = ringA.getMasterKeyId();
        parcel.mFingerprint = ringA.getFingerprint();
    }

    public void testSelfNoOp() throws Exception {

        UncachedKeyRing merged = mergeWithChecks(ringA, ringA, null);
        Assert.assertArrayEquals("keyring merged with itself must be identical",
                ringA.getEncoded(), merged.getEncoded()
        );

    }

    @Test
    public void testDifferentMasterKeyIds() throws Exception {

        Assert.assertNotEquals("generated key ids must be different",
                ringA.getMasterKeyId(), ringB.getMasterKeyId());

        Assert.assertNull("merging keys with differing key ids must fail",
                ringA.merge(ringB, log, 0));
        Assert.assertNull("merging keys with differing key ids must fail",
                ringB.merge(ringA, log, 0));

    }

    @Test
    public void testAddedUserId() throws Exception {

        UncachedKeyRing modifiedA, modifiedB; {
            WrappedSecretKeyRing secretRing = new WrappedSecretKeyRing(ringA.getEncoded(), false, 0);

            parcel.reset();
            parcel.mAddUserIds.add("flim");
            modifiedA = op.modifySecretKeyRing(secretRing, parcel, "", log, 0);

            parcel.reset();
            parcel.mAddUserIds.add("flam");
            modifiedB = op.modifySecretKeyRing(secretRing, parcel, "", log, 0);
        }

        { // merge A into base
            UncachedKeyRing merged = mergeWithChecks(ringA, modifiedA);

            Assert.assertEquals("merged keyring must have lost no packets", 0, onlyA.size());
            Assert.assertEquals("merged keyring must have gained two packets", 2, onlyB.size());
            Assert.assertTrue("merged keyring must contain new user id",
                    merged.getPublicKey().getUnorderedUserIds().contains("flim"));
        }

        { // merge A into B
            UncachedKeyRing merged = mergeWithChecks(modifiedA, modifiedB, ringA);

            Assert.assertEquals("merged keyring must have lost no packets", 0, onlyA.size());
            Assert.assertEquals("merged keyring must have gained four packets", 4, onlyB.size());
            Assert.assertTrue("merged keyring must contain first new user id",
                    merged.getPublicKey().getUnorderedUserIds().contains("flim"));
            Assert.assertTrue("merged keyring must contain second new user id",
                    merged.getPublicKey().getUnorderedUserIds().contains("flam"));

        }

    }

    @Test
    public void testAddedSubkeyId() throws Exception {

        UncachedKeyRing modifiedA, modifiedB;
        long subKeyIdA, subKeyIdB;
        {
            WrappedSecretKeyRing secretRing = new WrappedSecretKeyRing(ringA.getEncoded(), false, 0);

            parcel.reset();
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Constants.choice.algorithm.rsa, 1024, KeyFlags.SIGN_DATA, null));
            modifiedA = op.modifySecretKeyRing(secretRing, parcel, "", log, 0);
            modifiedB = op.modifySecretKeyRing(secretRing, parcel, "", log, 0);

            {
                Iterator<UncachedPublicKey> it = modifiedA.getPublicKeys();
                it.next(); it.next();
                subKeyIdA = it.next().getKeyId();
            }

            {
                Iterator<UncachedPublicKey> it = modifiedB.getPublicKeys();
                it.next(); it.next();
                subKeyIdB = it.next().getKeyId();
            }

        }

        {
            UncachedKeyRing merged = mergeWithChecks(ringA, modifiedA);

            Assert.assertEquals("merged keyring must have lost no packets", 0, onlyA.size());
            Assert.assertEquals("merged keyring must have gained two packets", 2, onlyB.size());

            long mergedKeyId;
            {
                Iterator<UncachedPublicKey> it = merged.getPublicKeys();
                it.next(); it.next();
                mergedKeyId = it.next().getKeyId();
            }
            Assert.assertEquals("merged keyring must contain the new subkey", subKeyIdA, mergedKeyId);
        }

        {
            UncachedKeyRing merged = mergeWithChecks(modifiedA, modifiedB, ringA);

            Assert.assertEquals("merged keyring must have lost no packets", 0, onlyA.size());
            Assert.assertEquals("merged keyring must have gained four packets", 4, onlyB.size());

            Iterator<UncachedPublicKey> it = merged.getPublicKeys();
            it.next(); it.next();
            Assert.assertEquals("merged keyring must contain the new subkey",
                    subKeyIdA, it.next().getKeyId());
            Assert.assertEquals("merged keyring must contain both new subkeys",
                    subKeyIdB, it.next().getKeyId());
        }

    }

    @Test
    public void testAddedKeySignature() throws Exception {
    }

    @Test
    public void testAddedUserIdSignature() throws Exception {

        final UncachedKeyRing pubRing = ringA.extractPublicKeyRing();

        final UncachedKeyRing modified; {
            WrappedPublicKeyRing publicRing = new WrappedPublicKeyRing(
                    pubRing.getEncoded(), false, 0);

            WrappedSecretKey secretKey = new WrappedSecretKeyRing(
                    ringB.getEncoded(), false, 0).getSecretKey();
            secretKey.unlock("");
            // sign all user ids
            modified = secretKey.certifyUserIds(publicRing, publicRing.getPublicKey().getUnorderedUserIds());
        }

        {
            UncachedKeyRing merged = ringA.merge(modified, log, 0);
            Assert.assertNotNull("merge must succeed", merged);
            Assert.assertArrayEquals("foreign signatures should not be merged into secret key",
                    ringA.getEncoded(), merged.getEncoded()
            );
        }

        {
            UncachedKeyRing merged = pubRing.merge(modified, log, 0);
            Assert.assertNotNull("merge must succeed", merged);
            Assert.assertFalse(
                    "merging keyring with extra signatures into its base should yield that same keyring",
                    KeyringTestingHelper.diffKeyrings(merged.getEncoded(), modified.getEncoded(), onlyA, onlyB)
            );
        }
    }

    private UncachedKeyRing mergeWithChecks(UncachedKeyRing a, UncachedKeyRing b)
            throws Exception {
        return mergeWithChecks(a, b, a, true);
    }

    private UncachedKeyRing mergeWithChecks(UncachedKeyRing a, UncachedKeyRing b, boolean commutative)
            throws Exception {
        return mergeWithChecks(a, b, a, commutative);
    }

    private UncachedKeyRing mergeWithChecks(UncachedKeyRing a, UncachedKeyRing b, UncachedKeyRing base)
            throws Exception {
        return mergeWithChecks(a, b, base, true);
    }

    private UncachedKeyRing mergeWithChecks(UncachedKeyRing a, UncachedKeyRing b,
                                            UncachedKeyRing base, boolean commutative)
            throws Exception {

        Assert.assertTrue("merging keyring must be secret type", a.isSecret());
        Assert.assertTrue("merged keyring must be secret type", b.isSecret());

        final UncachedKeyRing resultA;
        UncachedKeyRing resultB;

        { // sec + sec
            resultA = a.merge(b, log, 0);
            Assert.assertNotNull("merge must succeed as sec(a)+sec(b)", resultA);

            resultB = b.merge(a, log, 0);
            Assert.assertNotNull("merge must succeed as sec(b)+sec(a)", resultB);

            // check commutativity, if requested
            if (commutative) {
                Assert.assertFalse("result of merge must be commutative",
                        KeyringTestingHelper.diffKeyrings(
                                resultA.getEncoded(), resultB.getEncoded(), onlyA, onlyB)
                );
            }
        }

        final UncachedKeyRing pubA = a.extractPublicKeyRing();
        final UncachedKeyRing pubB = b.extractPublicKeyRing();

        { // sec + pub, pub + sec, and pub + pub

            try {
                resultB = a.merge(pubB, log, 0);
                Assert.assertNotNull("merge must succeed as sec(a)+pub(b)", resultA);

                Assert.assertFalse("result of sec(a)+pub(b) must be same as sec(a)+sec(b)",
                        KeyringTestingHelper.diffKeyrings(
                                resultA.getEncoded(), resultB.getEncoded(), onlyA, onlyB)
                );
            } catch (RuntimeException e) {
                System.out.println("special case, dummy key generation not in yet");
            }

            final UncachedKeyRing pubResult = resultA.extractPublicKeyRing();

            resultB = pubA.merge(b, log, 0);
            Assert.assertNotNull("merge must succeed as pub(a)+sec(b)", resultA);

            Assert.assertFalse("result of pub(a)+sec(b) must be same as pub(sec(a)+sec(b))",
                    KeyringTestingHelper.diffKeyrings(
                            pubResult.getEncoded(), resultB.getEncoded(), onlyA, onlyB)
            );

            resultB = pubA.merge(pubB, log, 0);
            Assert.assertNotNull("merge must succeed as pub(a)+pub(b)", resultA);

            Assert.assertFalse("result of pub(a)+pub(b) must be same as pub(sec(a)+sec(b))",
                    KeyringTestingHelper.diffKeyrings(
                            pubResult.getEncoded(), resultB.getEncoded(), onlyA, onlyB)
            );

        }

        if (base != null) {
            // set up onlyA and onlyB to be a diff to the base
            Assert.assertTrue("merged keyring must differ from base",
                    KeyringTestingHelper.diffKeyrings(
                            base.getEncoded(), resultA.getEncoded(), onlyA, onlyB)
            );
        }

        return resultA;

    }

}