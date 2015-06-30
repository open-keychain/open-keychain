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
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing.IteratorWithIOThrow;
import org.sufficientlysecure.keychain.pgp.WrappedSignature;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ProgressScaler;
import org.sufficientlysecure.keychain.util.TestingUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.Security;
import java.util.Iterator;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 21, manifest = "src/main/AndroidManifest.xml")
public class ExportTest {

    static Passphrase mPassphrase = TestingUtils.genPassphrase(true);

    static UncachedKeyRing mStaticRing1, mStaticRing2;
    static Passphrase mKeyPhrase1 = TestingUtils.genPassphrase(true);
    static Passphrase mKeyPhrase2 = TestingUtils.genPassphrase(true);

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
            parcel.mAddUserIds.add("snips");
            parcel.mNewUnlock = new ChangeUnlockParcel(mKeyPhrase1);

            PgpEditKeyResult result = op.createSecretKeyRing(parcel);
            Assert.assertTrue("initial test key creation must succeed", result.success());
            Assert.assertNotNull("initial test key creation must succeed", result.getRing());

            mStaticRing1 = result.getRing();
        }

        {
            SaveKeyringParcel parcel = new SaveKeyringParcel();
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.RSA, 1024, null, KeyFlags.CERTIFY_OTHER, 0L));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.DSA, 1024, null, KeyFlags.SIGN_DATA, 0L));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.ELGAMAL, 1024, null, KeyFlags.ENCRYPT_COMMS, 0L));
            parcel.mAddUserIds.add("snails");
            parcel.mNewUnlock = new ChangeUnlockParcel(null, new Passphrase("1234"));

            PgpEditKeyResult result = op.createSecretKeyRing(parcel);
            Assert.assertTrue("initial test key creation must succeed", result.success());
            Assert.assertNotNull("initial test key creation must succeed", result.getRing());

            mStaticRing2 = result.getRing();
        }

    }

    @Before
    public void setUp() {
        ProviderHelper providerHelper = new ProviderHelper(RuntimeEnvironment.application);

        // don't log verbosely here, we're not here to test imports
        ShadowLog.stream = oldShadowStream;

        providerHelper.saveSecretKeyRing(mStaticRing1, new ProgressScaler());
        providerHelper.saveSecretKeyRing(mStaticRing2, new ProgressScaler());

        // ok NOW log verbosely!
        ShadowLog.stream = System.out;
    }

    @Test
    public void testExportAll() throws Exception {
        ExportOperation op = new ExportOperation(RuntimeEnvironment.application,
                new ProviderHelper(RuntimeEnvironment.application), null);

        // make sure there is a local cert (so the later checks that there are none are meaningful)
        Assert.assertTrue("second keyring has local certification", checkForLocal(mStaticRing2));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExportResult result = op.exportKeyRings(new OperationLog(), null, false, out);

        Assert.assertTrue("export must be a success", result.success());

        long masterKeyId1, masterKeyId2;
        if (mStaticRing1.getMasterKeyId() < mStaticRing2.getMasterKeyId()) {
            masterKeyId1 = mStaticRing1.getMasterKeyId();
            masterKeyId2 = mStaticRing2.getMasterKeyId();
        } else {
            masterKeyId2 = mStaticRing1.getMasterKeyId();
            masterKeyId1 = mStaticRing2.getMasterKeyId();
        }

        IteratorWithIOThrow<UncachedKeyRing> unc =
                UncachedKeyRing.fromStream(new ByteArrayInputStream(out.toByteArray()));

        {
            Assert.assertTrue("export must have two keys (1/2)", unc.hasNext());
            UncachedKeyRing ring = unc.next();
            Assert.assertEquals("first exported key has correct masterkeyid",
                    masterKeyId1, ring.getMasterKeyId());
            Assert.assertFalse("first exported key must not be secret", ring.isSecret());
            Assert.assertFalse("there must be no local signatures in an exported keyring",
                    checkForLocal(ring));
        }

        {
            Assert.assertTrue("export must have two keys (2/2)", unc.hasNext());
            UncachedKeyRing ring = unc.next();
            Assert.assertEquals("second exported key has correct masterkeyid",
                    masterKeyId2, ring.getMasterKeyId());
            Assert.assertFalse("second exported key must not be secret", ring.isSecret());
            Assert.assertFalse("there must be no local signatures in an exported keyring",
                    checkForLocal(ring));
        }

        out = new ByteArrayOutputStream();
        result = op.exportKeyRings(new OperationLog(), null, true, out);

        Assert.assertTrue("export must be a success", result.success());

        unc = UncachedKeyRing.fromStream(new ByteArrayInputStream(out.toByteArray()));

        {
            Assert.assertTrue("export must have four keys (1/4)", unc.hasNext());
            UncachedKeyRing ring = unc.next();
            Assert.assertEquals("1/4 exported key has correct masterkeyid",
                    masterKeyId1, ring.getMasterKeyId());
            Assert.assertFalse("1/4 exported key must not be public", ring.isSecret());
            Assert.assertFalse("there must be no local signatures in an exported keyring",
                    checkForLocal(ring));

            Assert.assertTrue("export must have four keys (2/4)", unc.hasNext());
            ring = unc.next();
            Assert.assertEquals("2/4 exported key has correct masterkeyid",
                    masterKeyId1, ring.getMasterKeyId());
            Assert.assertTrue("2/4 exported key must be public", ring.isSecret());
            Assert.assertFalse("there must be no local signatures in an exported keyring",
                    checkForLocal(ring));
        }

        {
            Assert.assertTrue("export must have four keys (3/4)", unc.hasNext());
            UncachedKeyRing ring = unc.next();
            Assert.assertEquals("3/4 exported key has correct masterkeyid",
                    masterKeyId2, ring.getMasterKeyId());
            Assert.assertFalse("3/4 exported key must not be public", ring.isSecret());
            Assert.assertFalse("there must be no local signatures in an exported keyring",
                    checkForLocal(ring));

            Assert.assertTrue("export must have four keys (4/4)", unc.hasNext());
            ring = unc.next();
            Assert.assertEquals("4/4 exported key has correct masterkeyid",
                    masterKeyId2, ring.getMasterKeyId());
            Assert.assertTrue("4/4 exported key must be public", ring.isSecret());
            Assert.assertFalse("there must be no local signatures in an exported keyring",
                    checkForLocal(ring));
        }

    }

    /** This function checks whether or not there are any local signatures in a keyring. */
    private boolean checkForLocal(UncachedKeyRing ring) {
        Iterator<WrappedSignature> sigs = ring.getPublicKey().getSignatures();
        while (sigs.hasNext()) {
            if (sigs.next().isLocal()) {
                return true;
            }
        }
        return false;
    }

}
