/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.pgp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing.IteratorWithIOThrow;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Security;
import java.util.Iterator;
import java.util.Random;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 23, manifest = "src/main/AndroidManifest.xml")
public class UncachedKeyringTest {

    static UncachedKeyRing staticRing, staticPubRing;
    UncachedKeyRing ring, pubRing;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        ShadowLog.stream = System.out;

        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER, 0L));
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.SIGN_DATA, 0L));
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Algorithm.ECDH, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.ENCRYPT_COMMS, 0L));

        parcel.mAddUserIds.add("twi");
        parcel.mAddUserIds.add("pink");
        {
            Random r = new Random();
            int type = r.nextInt(110)+1;
            byte[] data = new byte[r.nextInt(2000)];
            new Random().nextBytes(data);

            WrappedUserAttribute uat = WrappedUserAttribute.fromSubpacket(type, data);
            parcel.mAddUserAttribute.add(uat);
        }
        // passphrase is tested in PgpKeyOperationTest, just use empty here
        parcel.mPassphrase = new Passphrase();
        PgpKeyOperation op = new PgpKeyOperation(null);

        PgpEditKeyResult result = op.createSecretKeyRing(parcel);
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

        IteratorWithIOThrow<UncachedKeyRing> it =
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

    @Test(expected = IOException.class)
    public void testBrokenVersionCert() throws Throwable {
        // this is a test for one of the patches we use on top of stock bouncycastle, which
        // returns an IOException rather than a RuntimeException in case of a bad certificate
        // version byte
        readRingFromResource("/test-keys/broken_cert_version.asc");
    }

    UncachedKeyRing readRingFromResource(String name) throws Throwable {
        return UncachedKeyRing.fromStream(UncachedKeyringTest.class.getResourceAsStream(name)).next();
    }

}
