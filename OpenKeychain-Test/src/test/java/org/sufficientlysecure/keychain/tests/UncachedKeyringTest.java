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
public class UncachedKeyringTest {

    static UncachedKeyRing staticRing;
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
        parcel.mNewPassphrase = "swag";
        PgpKeyOperation op = new PgpKeyOperation(null);

        OperationResultParcel.OperationLog log = new OperationResultParcel.OperationLog();
        staticRing = op.createSecretKeyRing(parcel, log, 0);

        Assert.assertNotNull("initial test key creation must succeed", staticRing);

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

        Assert.assertEquals("packet #2 should be secret key",
                PacketTags.USER_ID, it.next().tag);
        Assert.assertEquals("packet #3 should be secret key",
                PacketTags.SIGNATURE, it.next().tag);

        Assert.assertEquals("packet #4 should be secret key",
                PacketTags.USER_ID, it.next().tag);
        Assert.assertEquals("packet #5 should be secret key",
                PacketTags.SIGNATURE, it.next().tag);

        Assert.assertEquals("packet #6 should be secret key",
                PacketTags.SECRET_SUBKEY, it.next().tag);
        Assert.assertEquals("packet #7 should be secret key",
                PacketTags.SIGNATURE, it.next().tag);

        Assert.assertEquals("packet #8 should be secret key",
                PacketTags.SECRET_SUBKEY, it.next().tag);
        Assert.assertEquals("packet #9 should be secret key",
                PacketTags.SIGNATURE, it.next().tag);

        Assert.assertFalse("exactly 9 packets total", it.hasNext());

        Assert.assertArrayEquals("created keyring should be constant through canonicalization",
                ring.getEncoded(), ring.canonicalize(log, 0).getEncoded());

    }

}
