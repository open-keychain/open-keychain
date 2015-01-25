/*
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

package org.sufficientlysecure.keychain.operations;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.operations.results.PromoteKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.util.ProgressScaler;
import org.sufficientlysecure.keychain.util.TestingUtils;

import java.io.PrintStream;
import java.security.Security;
import java.util.Iterator;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class PromoteKeyOperationTest {

    static UncachedKeyRing mStaticRing;
    static String mKeyPhrase1 = TestingUtils.genPassphrase(true);

    static PrintStream oldShadowStream;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        oldShadowStream = ShadowLog.stream;
        // ShadowLog.stream = System.out;

        PgpKeyOperation op = new PgpKeyOperation(null);

        {
            SaveKeyringParcel parcel = new SaveKeyringParcel();
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.RSA, 1024, null, KeyFlags.CERTIFY_OTHER, 0L));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.DSA, 1024, null, KeyFlags.SIGN_DATA, 0L));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.ELGAMAL, 1024, null, KeyFlags.ENCRYPT_COMMS, 0L));
            parcel.mAddUserIds.add("derp");
            parcel.mNewUnlock = new ChangeUnlockParcel(mKeyPhrase1);

            PgpEditKeyResult result = op.createSecretKeyRing(parcel);
            Assert.assertTrue("initial test key creation must succeed", result.success());
            Assert.assertNotNull("initial test key creation must succeed", result.getRing());

            mStaticRing = result.getRing();
        }

    }

    @Before
    public void setUp() throws Exception {
        ProviderHelper providerHelper = new ProviderHelper(Robolectric.application);

        // don't log verbosely here, we're not here to test imports
        ShadowLog.stream = oldShadowStream;

        providerHelper.savePublicKeyRing(mStaticRing.extractPublicKeyRing(), new ProgressScaler());

        // ok NOW log verbosely!
        ShadowLog.stream = System.out;
    }

    @Test
    public void testPromote() throws Exception {
        PromoteKeyOperation op = new PromoteKeyOperation(Robolectric.application,
                new ProviderHelper(Robolectric.application), null, null);

        PromoteKeyResult result = op.execute(mStaticRing.getMasterKeyId());

        Assert.assertTrue("promotion must succeed", result.success());

        {
            CachedPublicKeyRing ring = new ProviderHelper(Robolectric.application)
                    .getCachedPublicKeyRing(mStaticRing.getMasterKeyId());
            Assert.assertTrue("key must have a secret now", ring.hasAnySecret());

            Iterator<UncachedPublicKey> it = mStaticRing.getPublicKeys();
            while (it.hasNext()) {
                long keyId = it.next().getKeyId();
                Assert.assertEquals("all subkeys must be divert-to-card",
                        SecretKeyType.GNU_DUMMY, ring.getSecretKeyType(keyId));
            }
        }

        // second attempt should fail
        result = op.execute(mStaticRing.getMasterKeyId());
        Assert.assertFalse("promotion of secret key must fail", result.success());

    }

}
