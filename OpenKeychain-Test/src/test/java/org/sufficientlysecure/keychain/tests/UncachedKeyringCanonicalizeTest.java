package org.sufficientlysecure.keychain.tests;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.bcpg.BCPGInputStream;
import org.spongycastle.bcpg.Packet;
import org.spongycastle.bcpg.PacketTags;
import org.spongycastle.bcpg.UserIDPacket;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.pgp.WrappedSignature;
import org.sufficientlysecure.keychain.service.OperationResultParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper.RawPacket;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Iterator;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class UncachedKeyringCanonicalizeTest {

    static UncachedKeyRing staticRing;
    static int totalPackets;
    UncachedKeyRing ring;
    ArrayList<RawPacket> onlyA = new ArrayList<RawPacket>();
    ArrayList<RawPacket> onlyB = new ArrayList<RawPacket>();
    OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();

    @BeforeClass
    public static void setUpOnce() throws Exception {
        ShadowLog.stream = System.out;

        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Constants.choice.algorithm.rsa, 1024, KeyFlags.CERTIFY_OTHER, null));
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Constants.choice.algorithm.rsa, 1024, KeyFlags.SIGN_DATA, null));
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Constants.choice.algorithm.rsa, 1024, KeyFlags.ENCRYPT_COMMS, null));

        parcel.mAddUserIds.add("twi");
        parcel.mAddUserIds.add("pink");
        // passphrase is tested in PgpKeyOperationTest, just use empty here
        parcel.mNewPassphrase = "";
        PgpKeyOperation op = new PgpKeyOperation(null);

        OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();
        staticRing = op.createSecretKeyRing(parcel, log, 0);

        Assert.assertNotNull("initial test key creation must succeed", staticRing);

        // just for later reference
        totalPackets = 9;

        // we sleep here for a second, to make sure all new certificates have different timestamps
        Thread.sleep(1000);
    }

    @Before public void setUp() throws Exception {
        // show Log.x messages in system.out
        ShadowLog.stream = System.out;
        ring = staticRing;
    }

    @Test public void testGeneratedRingStructure() throws Exception {

        Iterator<RawPacket> it = KeyringTestingHelper.parseKeyring(ring.getEncoded());

        Assert.assertEquals("packet #1 should be secret key",
                PacketTags.SECRET_KEY, it.next().tag);

        Assert.assertEquals("packet #2 should be user id",
                PacketTags.USER_ID, it.next().tag);
        Assert.assertEquals("packet #3 should be signature",
                PacketTags.SIGNATURE, it.next().tag);

        Assert.assertEquals("packet #4 should be user id",
                PacketTags.USER_ID, it.next().tag);
        Assert.assertEquals("packet #5 should be signature",
                PacketTags.SIGNATURE, it.next().tag);

        Assert.assertEquals("packet #6 should be secret subkey",
                PacketTags.SECRET_SUBKEY, it.next().tag);
        Assert.assertEquals("packet #7 should be signature",
                PacketTags.SIGNATURE, it.next().tag);

        Assert.assertEquals("packet #8 should be secret subkey",
                PacketTags.SECRET_SUBKEY, it.next().tag);
        Assert.assertEquals("packet #9 should be signature",
                PacketTags.SIGNATURE, it.next().tag);

        Assert.assertFalse("exactly 9 packets total", it.hasNext());

        Assert.assertArrayEquals("created keyring should be constant through canonicalization",
                ring.getEncoded(), ring.canonicalize(log, 0).getEncoded());

    }

    @Test public void testBrokenSignature() throws Exception {

        byte[] brokenSig;
        {
            UncachedPublicKey masterKey = ring.getPublicKey();
            WrappedSignature sig = masterKey.getSignaturesForId("twi").next();
            brokenSig = sig.getEncoded();
            // break the signature
            brokenSig[brokenSig.length - 5] += 1;
        }

        byte[] reng = ring.getEncoded();
        for(int i = 0; i < totalPackets; i++) {

            byte[] brokenBytes = KeyringTestingHelper.injectPacket(reng, brokenSig, i);
            Assert.assertEquals("broken ring must be original + injected size",
                    reng.length + brokenSig.length, brokenBytes.length);

            try {
                UncachedKeyRing brokenRing = UncachedKeyRing.decodeFromData(brokenBytes);

                brokenRing = brokenRing.canonicalize(log, 0);
                if (brokenRing == null) {
                    System.out.println("ok, canonicalization failed.");
                    continue;
                }

                Assert.assertArrayEquals("injected bad signature must be gone after canonicalization",
                        ring.getEncoded(), brokenRing.getEncoded());

            } catch (Exception e) {
                System.out.println("ok, rejected with: " + e.getMessage());
            }
        }

    }

    @Test public void testUidSignature() throws Exception {

        UncachedPublicKey masterKey = ring.getPublicKey();
        final WrappedSignature sig = masterKey.getSignaturesForId("twi").next();

        byte[] raw = sig.getEncoded();
        // destroy the signature
        raw[raw.length - 5] += 1;
        final WrappedSignature brokenSig = WrappedSignature.fromBytes(raw);

        { // bad certificates get stripped
            UncachedKeyRing modified = KeyringTestingHelper.injectPacket(ring, brokenSig.getEncoded(), 3);
            modified = modified.canonicalize(log, 0);

            Assert.assertTrue("canonicalized keyring with invalid extra sig must be same as original one",
                    !KeyringTestingHelper.diffKeyrings(
                        ring.getEncoded(), modified.getEncoded(), onlyA, onlyB));
        }

        // remove user id certificate for one user
        final UncachedKeyRing base = KeyringTestingHelper.removePacket(ring, 2);

        { // user id without certificate should be removed
            UncachedKeyRing modified = base.canonicalize(log, 0);
            Assert.assertTrue("canonicalized keyring must differ", KeyringTestingHelper.diffKeyrings(
                    ring.getEncoded(), modified.getEncoded(), onlyA, onlyB));

            Assert.assertEquals("two packets should be stripped after canonicalization", 2, onlyA.size());
            Assert.assertEquals("no new packets after canonicalization", 0, onlyB.size());

            Packet p = new BCPGInputStream(new ByteArrayInputStream(onlyA.get(0).buf)).readPacket();
            Assert.assertTrue("first stripped packet must be user id", p instanceof UserIDPacket);
            Assert.assertEquals("missing user id must be the expected one",
                    "twi", ((UserIDPacket) p).getID());

            Assert.assertArrayEquals("second stripped packet must be signature we removed",
                    sig.getEncoded(), onlyA.get(1).buf);

        }

        { // add error to signature

            UncachedKeyRing modified = KeyringTestingHelper.injectPacket(base, brokenSig.getEncoded(), 3);
            modified = modified.canonicalize(log, 0);

            Assert.assertTrue("canonicalized keyring must differ", KeyringTestingHelper.diffKeyrings(
                    ring.getEncoded(), modified.getEncoded(), onlyA, onlyB));

            Assert.assertEquals("two packets should be missing after canonicalization", 2, onlyA.size());
            Assert.assertEquals("no new packets after canonicalization", 0, onlyB.size());

            Packet p = new BCPGInputStream(new ByteArrayInputStream(onlyA.get(0).buf)).readPacket();
            Assert.assertTrue("first stripped packet must be user id", p instanceof UserIDPacket);
            Assert.assertEquals("missing user id must be the expected one",
                    "twi", ((UserIDPacket) p).getID());

            Assert.assertArrayEquals("second stripped packet must be signature we removed",
                    sig.getEncoded(), onlyA.get(1).buf);
        }

    }

}
