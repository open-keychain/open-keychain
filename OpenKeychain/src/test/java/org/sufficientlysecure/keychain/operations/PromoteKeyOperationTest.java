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


import java.io.PrintStream;
import java.security.Security;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.operations.results.PromoteKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.PromoteKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.TestingUtils;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 23, manifest = "src/main/AndroidManifest.xml")
public class PromoteKeyOperationTest {

    static UncachedKeyRing mStaticRing;
    static Passphrase mKeyPhrase1 = TestingUtils.genPassphrase(true);

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
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER, 0L));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.SIGN_DATA, 0L));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.ECDH, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.ENCRYPT_COMMS, 0L));
            parcel.mAddUserIds.add("derp");
            parcel.mPassphrase = mKeyPhrase1;

            PgpEditKeyResult result = op.createSecretKeyRing(parcel);
            Assert.assertTrue("initial test key creation must succeed", result.success());
            Assert.assertNotNull("initial test key creation must succeed", result.getRing());

            mStaticRing = result.getRing();
        }

    }

    @Before
    public void setUp() throws Exception {
        ProviderHelper providerHelper = new ProviderHelper(RuntimeEnvironment.application);

        // don't log verbosely here, we're not here to test imports
        ShadowLog.stream = oldShadowStream;

        providerHelper.savePublicKeyRing(mStaticRing.extractPublicKeyRing());

        // ok NOW log verbosely!
        ShadowLog.stream = System.out;
    }

    @Test
    public void testPromote() throws Exception {
        PromoteKeyOperation op = new PromoteKeyOperation(RuntimeEnvironment.application,
                new ProviderHelper(RuntimeEnvironment.application), null, null);

        PromoteKeyResult result = op.execute(new PromoteKeyringParcel(mStaticRing.getMasterKeyId(), null, null), null);

        Assert.assertTrue("promotion must succeed", result.success());

        {
            CachedPublicKeyRing ring = new ProviderHelper(RuntimeEnvironment.application)
                    .getCachedPublicKeyRing(mStaticRing.getMasterKeyId());
            Assert.assertTrue("key must have a secret now", ring.hasAnySecret());

            Iterator<UncachedPublicKey> it = mStaticRing.getPublicKeys();
            while (it.hasNext()) {
                long keyId = it.next().getKeyId();
                Assert.assertEquals("all subkeys must be gnu dummy",
                        SecretKeyType.GNU_DUMMY, ring.getSecretKeyType(keyId));
            }
        }

    }

    @Test
    public void testPromoteDivert() throws Exception {
        PromoteKeyOperation op = new PromoteKeyOperation(RuntimeEnvironment.application,
                new ProviderHelper(RuntimeEnvironment.application), null, null);

        byte[] aid = Hex.decode("D2760001240102000000012345670000");

        PromoteKeyResult result = op.execute(new PromoteKeyringParcel(mStaticRing.getMasterKeyId(), aid, null), null);

        Assert.assertTrue("promotion must succeed", result.success());

        {
            CanonicalizedSecretKeyRing ring = new ProviderHelper(RuntimeEnvironment.application)
                    .getCanonicalizedSecretKeyRing(mStaticRing.getMasterKeyId(), new Passphrase());

            for (CanonicalizedSecretKey key : ring.secretKeyIterator()) {
                Assert.assertEquals("all subkeys must be divert-to-card",
                        SecretKeyType.DIVERT_TO_CARD, key.getSecretKeyTypeSuperExpensive());
                Assert.assertArrayEquals("all subkeys must have correct iv",
                        aid, key.getIv());
            }

        }
    }

    @Test
    public void testPromoteDivertSpecific() throws Exception {
        PromoteKeyOperation op = new PromoteKeyOperation(RuntimeEnvironment.application,
                new ProviderHelper(RuntimeEnvironment.application), null, null);

        byte[] aid = Hex.decode("D2760001240102000000012345670000");

        // only promote the first, rest stays dummy
        long keyId = KeyringTestingHelper.getSubkeyId(mStaticRing, 1);

        PromoteKeyResult result = op.execute(new PromoteKeyringParcel(mStaticRing.getMasterKeyId(), aid, new long[] {
            keyId
        }), null);

        Assert.assertTrue("promotion must succeed", result.success());

        {
            CanonicalizedSecretKeyRing ring = new ProviderHelper(RuntimeEnvironment.application)
                    .getCanonicalizedSecretKeyRing(mStaticRing.getMasterKeyId(), new Passphrase());

            for (CanonicalizedSecretKey key : ring.secretKeyIterator()) {
                if (key.getKeyId() == keyId) {
                    Assert.assertEquals("subkey must be divert-to-card",
                            SecretKeyType.DIVERT_TO_CARD, key.getSecretKeyTypeSuperExpensive());
                    Assert.assertArrayEquals("subkey must have correct iv",
                            aid, key.getIv());
                } else {
                    Assert.assertEquals("some subkeys must be gnu dummy",
                            SecretKeyType.GNU_DUMMY, key.getSecretKeyTypeSuperExpensive());
                }
            }

        }
    }

}
