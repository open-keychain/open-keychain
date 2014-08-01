package org.sufficientlysecure.keychain.pgp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.service.OperationResults.EditKeyResult;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper.RawPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class UncachedKeyringTest {

    static UncachedKeyRing staticRing, staticPubRing;
    UncachedKeyRing ring, pubRing;
    ArrayList<RawPacket> onlyA = new ArrayList<RawPacket>();
    ArrayList<RawPacket> onlyB = new ArrayList<RawPacket>();
    PgpKeyOperation op;
    SaveKeyringParcel parcel;

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

        EditKeyResult result = op.createSecretKeyRing(parcel);
        staticRing = result.getRing();
        staticPubRing = staticRing.extractPublicKeyRing();

        Assert.assertNotNull("initial test key creation must succeed", staticRing);

        // we sleep here for a second, to make sure all new certificates have different timestamps
        Thread.sleep(1000);
    }


    @Before
    public void setUp() throws Exception {
        // show Log.x messages in system.out
        ShadowLog.stream = System.out;
        ring = staticRing;
        pubRing = staticPubRing;
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPublicKeyItRemove() throws Exception {
        Iterator<UncachedPublicKey> it = ring.getPublicKeys();
        it.remove();
    }

    @Test(expected = PgpGeneralException.class)
    public void testDecodeFromEmpty() throws Exception {
        UncachedKeyRing.decodeFromData(new byte[0]);
    }

    @Test
    public void testArmorIdentity() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ring.encodeArmored(out, "OpenKeychain");

        Assert.assertArrayEquals("armor encoded and decoded ring should be identical to original",
            ring.getEncoded(),
            UncachedKeyRing.decodeFromData(out.toByteArray()).getEncoded());
    }

    @Test(expected = PgpGeneralException.class)
    public void testDecodeEncodeMulti() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // encode secret and public ring in here
        ring.encodeArmored(out, "OpenKeychain");
        pubRing.encodeArmored(out, "OpenKeychain");

        Iterator<UncachedKeyRing> it =
                UncachedKeyRing.fromStream(new ByteArrayInputStream(out.toByteArray()));
        Assert.assertTrue("there should be two rings in the stream", it.hasNext());
        Assert.assertArrayEquals("first ring should be the first we put in",
                ring.getEncoded(), it.next().getEncoded());
        Assert.assertTrue("there should be two rings in the stream", it.hasNext());
        Assert.assertArrayEquals("second ring should be the second we put in",
                pubRing.getEncoded(), it.next().getEncoded());
        Assert.assertFalse("there should be two rings in the stream", it.hasNext());

        // this should fail with PgpGeneralException, since it expects exactly one ring
        UncachedKeyRing.decodeFromData(out.toByteArray());
    }

    @Test(expected = RuntimeException.class)
    public void testPublicExtractPublic() throws Exception {
        // can't do this, either!
        pubRing.extractPublicKeyRing();
    }

}
