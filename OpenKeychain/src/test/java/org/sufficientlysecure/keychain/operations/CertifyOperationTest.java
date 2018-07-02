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
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.operations.results.CertifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.daos.KeyWritableRepository;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel.CertifyAction;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.TestingUtils;

@RunWith(KeychainTestRunner.class)
public class CertifyOperationTest {

    static UncachedKeyRing mStaticRing1, mStaticRing2;
    static Passphrase mKeyPhrase1 = TestingUtils.genPassphrase(true);
    static Passphrase mKeyPhrase2 = TestingUtils.genPassphrase(true);

    static PrintStream oldShadowStream;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        oldShadowStream = ShadowLog.stream;
        // ShadowLog.stream = System.out;

        Random random = new Random();

        PgpKeyOperation op = new PgpKeyOperation(null);

        {
            SaveKeyringParcel.Builder builder = SaveKeyringParcel.buildNewKeyringParcel();
            builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER, 0L));
            builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.SIGN_DATA, 0L));
            builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                    Algorithm.ECDH, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.ENCRYPT_COMMS, 0L));
            builder.addUserId("derp");
            builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(mKeyPhrase1));

            PgpEditKeyResult result = op.createSecretKeyRing(builder.build());
            Assert.assertTrue("initial test key creation must succeed", result.success());
            Assert.assertNotNull("initial test key creation must succeed", result.getRing());

            mStaticRing1 = result.getRing();
        }

        {
            SaveKeyringParcel.Builder builder = SaveKeyringParcel.buildNewKeyringParcel();
            builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER, 0L));
            builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.SIGN_DATA, 0L));
            builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                    Algorithm.ECDH, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.ENCRYPT_COMMS, 0L));

            builder.addUserId("ditz");
            byte[] uatdata = new byte[random.nextInt(150)+10];
            random.nextBytes(uatdata);
            builder.addUserAttribute(WrappedUserAttribute.fromSubpacket(random.nextInt(100)+1, uatdata));

            builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(mKeyPhrase2));

            PgpEditKeyResult result = op.createSecretKeyRing(builder.build());
            Assert.assertTrue("initial test key creation must succeed", result.success());
            Assert.assertNotNull("initial test key creation must succeed", result.getRing());

            mStaticRing2 = result.getRing();
        }

    }

    @Before
    public void setUp() throws Exception {
        KeyWritableRepository databaseInteractor =
                KeyWritableRepository.create(RuntimeEnvironment.application);

        // don't log verbosely here, we're not here to test imports
        ShadowLog.stream = oldShadowStream;

        databaseInteractor.saveSecretKeyRing(mStaticRing1);
        databaseInteractor.savePublicKeyRing(mStaticRing2.extractPublicKeyRing(), null);

        // ok NOW log verbosely!
        ShadowLog.stream = System.out;
    }

    @Test
    public void testSelfCertifyFlag() throws Exception {

        CanonicalizedPublicKeyRing ring = KeyWritableRepository.create(RuntimeEnvironment.application)
                .getCanonicalizedPublicKeyRing(mStaticRing1.getMasterKeyId());
        Assert.assertEquals("secret key must be marked self-certified in database",
                // TODO this should be more correctly be VERIFIED_SELF at some point!
                VerificationStatus.VERIFIED_SECRET, ring.getVerified());

    }

    @Test
    public void testCertifyId() throws Exception {
        CertifyOperation op = new CertifyOperation(RuntimeEnvironment.application,
                KeyWritableRepository.create(RuntimeEnvironment.application), null, null);

        {
            CanonicalizedPublicKeyRing ring = KeyWritableRepository.create(RuntimeEnvironment.application)
                    .getCanonicalizedPublicKeyRing(mStaticRing2.getMasterKeyId());
            Assert.assertNull("public key must not be marked verified prior to certification",
                    ring.getVerified());
        }

        CertifyActionsParcel.Builder actions = CertifyActionsParcel.builder(mStaticRing1.getMasterKeyId());
        actions.addAction(CertifyAction.createForUserIds(mStaticRing2.getMasterKeyId(),
                mStaticRing2.getPublicKey().getUnorderedUserIds()));
        CertifyResult result = op.execute(actions.build(), CryptoInputParcel.createCryptoInputParcel(new Date(), mKeyPhrase1));

        Assert.assertTrue("certification must succeed", result.success());

        {
            CanonicalizedPublicKeyRing ring = KeyWritableRepository.create(RuntimeEnvironment.application)
                    .getCanonicalizedPublicKeyRing(mStaticRing2.getMasterKeyId());
            Assert.assertEquals("new key must be verified now",
                    VerificationStatus.VERIFIED_SECRET, ring.getVerified());
        }

    }

    @Test
    public void testCertifyAttribute() throws Exception {
        KeyWritableRepository keyWritableRepository = KeyWritableRepository.create(RuntimeEnvironment.application);
        CertifyOperation op = new CertifyOperation(RuntimeEnvironment.application, keyWritableRepository, null, null);

        {
            CanonicalizedPublicKeyRing ring = keyWritableRepository.getCanonicalizedPublicKeyRing(mStaticRing2.getMasterKeyId());
            Assert.assertNull("public key must not be marked verified prior to certification",
                    ring.getVerified());
        }

        CertifyActionsParcel.Builder actions = CertifyActionsParcel.builder(mStaticRing1.getMasterKeyId());
        actions.addAction(CertifyAction.createForUserAttributes(mStaticRing2.getMasterKeyId(),
                mStaticRing2.getPublicKey().getUnorderedUserAttributes()));
        CertifyResult result = op.execute(actions.build(), CryptoInputParcel.createCryptoInputParcel(new Date(), mKeyPhrase1));

        Assert.assertTrue("certification must succeed", result.success());

        {
            CanonicalizedPublicKeyRing ring = keyWritableRepository.getCanonicalizedPublicKeyRing(mStaticRing2.getMasterKeyId());
            Assert.assertEquals("new key must be verified now",
                    VerificationStatus.VERIFIED_SECRET, ring.getVerified());
        }

    }


    @Test
    public void testCertifySelf() throws Exception {
        CertifyOperation op = new CertifyOperation(RuntimeEnvironment.application,
                KeyWritableRepository.create(RuntimeEnvironment.application), null, null);

        CertifyActionsParcel.Builder actions = CertifyActionsParcel.builder(mStaticRing1.getMasterKeyId());
        actions.addAction(CertifyAction.createForUserIds(mStaticRing1.getMasterKeyId(),
                mStaticRing2.getPublicKey().getUnorderedUserIds()));

        CertifyResult result = op.execute(actions.build(), CryptoInputParcel.createCryptoInputParcel(new Date(), mKeyPhrase1));

        Assert.assertFalse("certification with itself must fail!", result.success());
        Assert.assertTrue("error msg must be about self certification",
                result.getLog().containsType(LogType.MSG_CRT_ERROR_SELF));
    }

    @Test
    public void testCertifyNonexistent() throws Exception {

        CertifyOperation op = new CertifyOperation(RuntimeEnvironment.application,
                KeyWritableRepository.create(RuntimeEnvironment.application), null, null);

        {
            CertifyActionsParcel.Builder actions = CertifyActionsParcel.builder(mStaticRing1.getMasterKeyId());
            ArrayList<String> uids = new ArrayList<String>();
            uids.add("nonexistent");
            actions.addAction(CertifyAction.createForUserIds(1234L, uids));

            CertifyResult result = op.execute(actions.build(), CryptoInputParcel.createCryptoInputParcel(new Date(),
                    mKeyPhrase1));

            Assert.assertFalse("certification of nonexistent key must fail", result.success());
            Assert.assertTrue("must contain error msg about not found",
                    result.getLog().containsType(LogType.MSG_CRT_WARN_NOT_FOUND));
        }

        {
            CertifyActionsParcel.Builder actions = CertifyActionsParcel.builder(1234L);
            actions.addAction(CertifyAction.createForUserIds(mStaticRing1.getMasterKeyId(),
                    mStaticRing2.getPublicKey().getUnorderedUserIds()));

            CertifyResult result = op.execute(actions.build(), CryptoInputParcel.createCryptoInputParcel(new Date(),
                    mKeyPhrase1));

            Assert.assertFalse("certification of nonexistent key must fail", result.success());
            Assert.assertTrue("must contain error msg about not found",
                    result.getLog().containsType(LogType.MSG_CRT_ERROR_MASTER_NOT_FOUND));
        }

    }

}
