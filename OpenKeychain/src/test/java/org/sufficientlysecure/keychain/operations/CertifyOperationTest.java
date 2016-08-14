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
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.operations.results.CertifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel.CertifyAction;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.util.KeyringPassphrases;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ProgressScaler;
import org.sufficientlysecure.keychain.util.TestingUtils;

import java.io.PrintStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 23, manifest = "src/main/AndroidManifest.xml")
public class CertifyOperationTest {

    static UncachedKeyRing mStaticRing1, mStaticRing2;
    static Passphrase mKeyPhrase1 = TestingUtils.genPassphrase(true);

    static PrintStream oldShadowStream;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        oldShadowStream = ShadowLog.stream;
        // ShadowLog.stream = System.out;

        Random random = new Random();

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

            PgpEditKeyResult result = op.createSecretKeyRing(parcel);
            Assert.assertTrue("initial test key creation must succeed", result.success());
            Assert.assertNotNull("initial test key creation must succeed", result.getRing());

            mStaticRing1 = result.getRing();
        }

        {
            SaveKeyringParcel parcel = new SaveKeyringParcel();
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER, 0L));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.SIGN_DATA, 0L));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.ECDH, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.ENCRYPT_COMMS, 0L));

            parcel.mAddUserIds.add("ditz");
            byte[] uatdata = new byte[random.nextInt(150)+10];
            random.nextBytes(uatdata);
            parcel.mAddUserAttribute.add(
                    WrappedUserAttribute.fromSubpacket(random.nextInt(100)+1, uatdata));

            PgpEditKeyResult result = op.createSecretKeyRing(parcel);
            Assert.assertTrue("initial test key creation must succeed", result.success());
            Assert.assertNotNull("initial test key creation must succeed", result.getRing());

            mStaticRing2 = result.getRing();
        }

    }

    @Before
    public void setUp() throws Exception {
        ProviderHelper providerHelper = new ProviderHelper(RuntimeEnvironment.application);

        // don't log verbosely here, we're not here to test imports
        ShadowLog.stream = oldShadowStream;

        providerHelper.mWriter.saveSecretKeyRing(mStaticRing1,
                new KeyringPassphrases(mStaticRing1.getMasterKeyId(), mKeyPhrase1), new ProgressScaler());
        providerHelper.mWriter.savePublicKeyRing(mStaticRing2.extractPublicKeyRing());

        // ok NOW log verbosely!
        ShadowLog.stream = System.out;
    }

    @Test
    public void testSelfCertifyFlag() throws Exception {

        CanonicalizedPublicKeyRing ring = new ProviderHelper(RuntimeEnvironment.application)
                .mReader.getCanonicalizedPublicKeyRing(mStaticRing1.getMasterKeyId());
        Assert.assertEquals("secret key must be marked self-certified in database",
                // TODO this should be more correctly be VERIFIED_SELF at some point!
                Certs.VERIFIED_SECRET, ring.getVerified());

    }

    @Test
    public void testCertifyId() throws Exception {
        CertifyOperation op = new CertifyOperation(RuntimeEnvironment.application,
                new ProviderHelper(RuntimeEnvironment.application), null, null);

        {
            CanonicalizedPublicKeyRing ring = new ProviderHelper(RuntimeEnvironment.application)
                    .mReader.getCanonicalizedPublicKeyRing(mStaticRing2.getMasterKeyId());
            Assert.assertEquals("public key must not be marked verified prior to certification",
                    Certs.UNVERIFIED, ring.getVerified());
        }

        CertifyActionsParcel actions = new CertifyActionsParcel(mStaticRing1.getMasterKeyId());
        actions.add(new CertifyAction(mStaticRing2.getMasterKeyId(),
                mStaticRing2.getPublicKey().getUnorderedUserIds(), null));
        CertifyResult result = op.execute(actions, new CryptoInputParcel(new Date(), mKeyPhrase1));

        Assert.assertTrue("certification must succeed", result.success());

        {
            CanonicalizedPublicKeyRing ring = new ProviderHelper(RuntimeEnvironment.application)
                    .mReader.getCanonicalizedPublicKeyRing(mStaticRing2.getMasterKeyId());
            Assert.assertEquals("new key must be verified now",
                    Certs.VERIFIED_SECRET, ring.getVerified());
        }

    }

    @Test
    public void testCertifyAttribute() throws Exception {
        CertifyOperation op = new CertifyOperation(RuntimeEnvironment.application,
                new ProviderHelper(RuntimeEnvironment.application), null, null);

        {
            CanonicalizedPublicKeyRing ring = new ProviderHelper(RuntimeEnvironment.application)
                    .mReader.getCanonicalizedPublicKeyRing(mStaticRing2.getMasterKeyId());
            Assert.assertEquals("public key must not be marked verified prior to certification",
                    Certs.UNVERIFIED, ring.getVerified());
        }

        CertifyActionsParcel actions = new CertifyActionsParcel(mStaticRing1.getMasterKeyId());
        actions.add(new CertifyAction(mStaticRing2.getMasterKeyId(), null,
                mStaticRing2.getPublicKey().getUnorderedUserAttributes()));
        CertifyResult result = op.execute(actions, new CryptoInputParcel(new Date(), mKeyPhrase1));

        Assert.assertTrue("certification must succeed", result.success());

        {
            CanonicalizedPublicKeyRing ring = new ProviderHelper(RuntimeEnvironment.application)
                    .mReader.getCanonicalizedPublicKeyRing(mStaticRing2.getMasterKeyId());
            Assert.assertEquals("new key must be verified now",
                    Certs.VERIFIED_SECRET, ring.getVerified());
        }

    }


    @Test
    public void testCertifySelf() throws Exception {
        CertifyOperation op = new CertifyOperation(RuntimeEnvironment.application,
                new ProviderHelper(RuntimeEnvironment.application), null, null);

        CertifyActionsParcel actions = new CertifyActionsParcel(mStaticRing1.getMasterKeyId());
        actions.add(new CertifyAction(mStaticRing1.getMasterKeyId(),
                mStaticRing2.getPublicKey().getUnorderedUserIds(), null));

        CertifyResult result = op.execute(actions, new CryptoInputParcel(new Date(), mKeyPhrase1));

        Assert.assertFalse("certification with itself must fail!", result.success());
        Assert.assertTrue("error msg must be about self certification",
                result.getLog().containsType(LogType.MSG_CRT_ERROR_SELF));
    }

    @Test
    public void testCertifyNonexistent() throws Exception {

        CertifyOperation op = new CertifyOperation(RuntimeEnvironment.application,
                new ProviderHelper(RuntimeEnvironment.application), null, null);

        {
            CertifyActionsParcel actions = new CertifyActionsParcel(mStaticRing1.getMasterKeyId());
            ArrayList<String> uids = new ArrayList<String>();
            uids.add("nonexistent");
            actions.add(new CertifyAction(1234L, uids, null));

            CertifyResult result = op.execute(actions, new CryptoInputParcel(new Date(),
                    mKeyPhrase1));

            Assert.assertFalse("certification of nonexistent key must fail", result.success());
            Assert.assertTrue("must contain error msg about not found",
                    result.getLog().containsType(LogType.MSG_CRT_WARN_NOT_FOUND));
        }

        {
            CertifyActionsParcel actions = new CertifyActionsParcel(1234L);
            actions.add(new CertifyAction(mStaticRing1.getMasterKeyId(),
                    mStaticRing2.getPublicKey().getUnorderedUserIds(), null));

            CertifyResult result = op.execute(actions, new CryptoInputParcel(new Date(),
                    mKeyPhrase1));

            Assert.assertFalse("certification of nonexistent key must fail", result.success());
            Assert.assertTrue("must contain error msg about not found",
                    result.getLog().containsType(LogType.MSG_CRT_ERROR_MASTER_NOT_FOUND));
        }

    }

}
