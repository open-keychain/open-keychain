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

package org.sufficientlysecure.keychain.pgp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.sufficientlysecure.keychain.pgp.PgpSignEncrypt.Builder;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingData;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.ProgressScaler;
import org.sufficientlysecure.keychain.util.TestingUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.Security;
import java.util.HashSet;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class PgpEncryptDecryptTest {

    static String mPassphrase = TestingUtils.genPassphrase(true);

    static UncachedKeyRing mStaticRing1, mStaticRing2;
    static String mKeyPhrase1 = TestingUtils.genPassphrase(true);
    static String mKeyPhrase2 = TestingUtils.genPassphrase(true);

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
            parcel.mAddUserIds.add("bloom");
            parcel.mNewUnlock = new ChangeUnlockParcel(mKeyPhrase1);

            EditKeyResult result = op.createSecretKeyRing(parcel);
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
            parcel.mAddUserIds.add("belle");
            parcel.mNewUnlock = new ChangeUnlockParcel(mKeyPhrase2);

            EditKeyResult result = op.createSecretKeyRing(parcel);
            Assert.assertTrue("initial test key creation must succeed", result.success());
            Assert.assertNotNull("initial test key creation must succeed", result.getRing());

            mStaticRing2 = result.getRing();
        }

    }

    @Before
    public void setUp() {
        ProviderHelper providerHelper = new ProviderHelper(Robolectric.application);

        // don't log verbosely here, we're not here to test imports
        ShadowLog.stream = oldShadowStream;

        providerHelper.saveSecretKeyRing(mStaticRing1, new ProgressScaler());
        providerHelper.saveSecretKeyRing(mStaticRing2, new ProgressScaler());

        // ok NOW log verbosely!
        ShadowLog.stream = System.out;
    }

    @Test
    public void testSymmetricEncryptDecrypt() {

        String plaintext = "dies ist ein plaintext ☭" + TestingUtils.genPassphrase(true);
        byte[] ciphertext;

        { // encrypt data with a given passphrase
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(plaintext.getBytes());

            InputData data = new InputData(in, in.available());
            Builder b = new PgpSignEncrypt.Builder(Robolectric.application,
                    new ProviderHelper(Robolectric.application),
                    null,
                    data, out);

            b.setSymmetricPassphrase(mPassphrase);
            b.setSymmetricEncryptionAlgorithm(PGPEncryptedData.AES_128);
            SignEncryptResult result = b.build().execute();
            Assert.assertTrue("encryption must succeed", result.success());

            ciphertext = out.toByteArray();
        }

        { // decryption with same passphrase should yield the same result

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify.Builder b = new PgpDecryptVerify.Builder(Robolectric.application,
                    new ProviderHelper(Robolectric.application),
                    null, // new DummyPassphraseCache(mPassphrase, 0L),
                    data, out);
            b.setPassphrase(mPassphrase);
            DecryptVerifyResult result = b.build().execute();
            Assert.assertTrue("decryption must succeed", result.success());
            Assert.assertArrayEquals("decrypted ciphertext should equal plaintext",
                    out.toByteArray(), plaintext.getBytes());
            Assert.assertNull("signature should be an error", result.getSignatureResult());
        }

        { // decryption with a bad passphrase should fail

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify.Builder b = new PgpDecryptVerify.Builder(
                    Robolectric.application,
                    new ProviderHelper(Robolectric.application),
                    null, // new DummyPassphraseCache(mPassphrase, 0L),
                    data, out);
            b.setPassphrase(mPassphrase + "x");
            DecryptVerifyResult result = b.build().execute();
            Assert.assertFalse("decryption must succeed", result.success());
            Assert.assertEquals("decrypted plaintext should be empty", 0, out.size());
            Assert.assertNull("signature should be an error", result.getSignatureResult());
        }

        { // decryption with an unset passphrase should fail

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify.Builder b = new PgpDecryptVerify.Builder(
                    Robolectric.application,
                    new ProviderHelper(Robolectric.application),
                    null, // new DummyPassphraseCache(mPassphrase, 0L),
                    data, out);
            DecryptVerifyResult result = b.build().execute();
            Assert.assertFalse("decryption must succeed", result.success());
            Assert.assertEquals("decrypted plaintext should be empty", 0, out.size());
            Assert.assertNull("signature should be an error", result.getSignatureResult());
        }

    }

    @Test
    public void testAsymmetricEncryptDecrypt() {

        String plaintext = "dies ist ein plaintext ☭" + TestingUtils.genPassphrase(true);
        byte[] ciphertext;

        { // encrypt data with a given passphrase
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(plaintext.getBytes());

            InputData data = new InputData(in, in.available());
            Builder b = new PgpSignEncrypt.Builder(
                    Robolectric.application,
                    new ProviderHelper(Robolectric.application),
                    null, // new DummyPassphraseCache(mPassphrase, 0L),
                    data, out);

            b.setEncryptionMasterKeyIds(new long[]{ mStaticRing1.getMasterKeyId() });
            b.setSymmetricEncryptionAlgorithm(PGPEncryptedData.AES_128);
            SignEncryptResult result = b.build().execute();
            Assert.assertTrue("encryption must succeed", result.success());

            ciphertext = out.toByteArray();
        }

        { // decryption with provided passphrase should yield the same result

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());


            PgpDecryptVerify.Builder b = builderWithFakePassphraseCache(data, out, null, null, null);
            b.setPassphrase(mKeyPhrase1);
            DecryptVerifyResult result = b.build().execute();
            Assert.assertTrue("decryption with provided passphrase must succeed", result.success());
            Assert.assertArrayEquals("decrypted ciphertext with provided passphrase should equal plaintext",
                    out.toByteArray(), plaintext.getBytes());
            Assert.assertNull("signature be empty", result.getSignatureResult());
        }

        // TODO how to test passphrase cache?

        { // decryption with passphrase cached should succeed

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify.Builder b = builderWithFakePassphraseCache(data, out,
                    mKeyPhrase1, mStaticRing1.getMasterKeyId(), null);

            DecryptVerifyResult result = b.build().execute();
            Assert.assertTrue("decryption with cached passphrase must succeed", result.success());
            Assert.assertArrayEquals("decrypted ciphertext with cached passphrase  should equal plaintext",
                    out.toByteArray(), plaintext.getBytes());
            Assert.assertNull("signature should be empty", result.getSignatureResult());
        }

        { // decryption with no passphrase provided should return status pending

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify.Builder b = builderWithFakePassphraseCache(data, out,
                    null, mStaticRing1.getMasterKeyId(), null);
            DecryptVerifyResult result = b.build().execute();
            Assert.assertFalse("decryption with no passphrase must return pending", result.success());
            Assert.assertTrue("decryption with no passphrase should return pending", result.isPending());
            Assert.assertEquals("decryption with no passphrase should return pending passphrase",
                    DecryptVerifyResult.RESULT_PENDING_ASYM_PASSPHRASE, result.getResult());
        }

    }

    @Test
    public void testMultiAsymmetricEncryptDecrypt() {

        String plaintext = "dies ist ein plaintext ☭" + TestingUtils.genPassphrase(true);
        byte[] ciphertext;

        { // encrypt data with a given passphrase
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(plaintext.getBytes());

            InputData data = new InputData(in, in.available());
            Builder b = new PgpSignEncrypt.Builder(
                    Robolectric.application,
                    new ProviderHelper(Robolectric.application),
                    null, // new DummyPassphraseCache(mPassphrase, 0L),
                    data, out);

            b.setEncryptionMasterKeyIds(new long[] {
                    mStaticRing1.getMasterKeyId(),
                    mStaticRing2.getMasterKeyId()
            });
            b.setSymmetricEncryptionAlgorithm(PGPEncryptedData.AES_128);
            SignEncryptResult result = b.build().execute();
            Assert.assertTrue("encryption must succeed", result.success());

            ciphertext = out.toByteArray();
        }

        { // decryption with passphrase cached should succeed for the first key

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify.Builder b = builderWithFakePassphraseCache(data, out,
                    mKeyPhrase1, mStaticRing1.getMasterKeyId(), null);

            DecryptVerifyResult result = b.build().execute();
            Assert.assertTrue("decryption with cached passphrase must succeed for the first key", result.success());
            Assert.assertArrayEquals("decrypted ciphertext with cached passphrase  should equal plaintext",
                    out.toByteArray(), plaintext.getBytes());
            Assert.assertNull("signature should be empty", result.getSignatureResult());
        }

        { // decryption with passphrase cached should succeed for the first key

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            // allow only the second to decrypt
            HashSet<Long> allowed = new HashSet<Long>();
            allowed.add(mStaticRing2.getMasterKeyId());

            // provide passphrase for the second, and check that the first is never asked for!
            PgpDecryptVerify.Builder b = builderWithFakePassphraseCache(data, out,
                    mKeyPhrase2, mStaticRing2.getMasterKeyId(), null);
            b.setAllowedKeyIds(allowed);

            DecryptVerifyResult result = b.build().execute();
            Assert.assertTrue("decryption with cached passphrase must succeed for the first key", result.success());
            Assert.assertArrayEquals("decrypted ciphertext with cached passphrase  should equal plaintext",
                    out.toByteArray(), plaintext.getBytes());
            Assert.assertNull("signature should be empty", result.getSignatureResult());
        }

        { // decryption with passphrase cached should succeed for the other key if first is gone

            // delete first key from database
            new ProviderHelper(Robolectric.application).getContentResolver().delete(
                    KeyRingData.buildPublicKeyRingUri(mStaticRing1.getMasterKeyId()), null, null
            );

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify.Builder b = builderWithFakePassphraseCache(data, out,
                    mKeyPhrase2, mStaticRing2.getMasterKeyId(), null);

            DecryptVerifyResult result = b.build().execute();
            Assert.assertTrue("decryption with cached passphrase must succeed", result.success());
            Assert.assertArrayEquals("decrypted ciphertext with cached passphrase  should equal plaintext",
                    out.toByteArray(), plaintext.getBytes());
            Assert.assertNull("signature should be empty", result.getSignatureResult());
        }

    }

    private PgpDecryptVerify.Builder builderWithFakePassphraseCache (
            InputData data, OutputStream out,
            final String passphrase, final Long checkMasterKeyId, final Long checkSubKeyId) {

        return new PgpDecryptVerify.Builder(Robolectric.application,
                new ProviderHelper(Robolectric.application),
                null,
                data, out) {
            public PgpDecryptVerify build() {
                return new PgpDecryptVerify(this) {
                    @Override
                    public String getCachedPassphrase(long masterKeyId, long subKeyId)
                            throws NoSecretKeyException {
                        if (checkMasterKeyId != null) {
                            Assert.assertEquals("requested passphrase should be for expected master key id",
                                    (long) checkMasterKeyId, masterKeyId);
                        }
                        if (checkSubKeyId != null) {
                            Assert.assertEquals("requested passphrase should be for expected sub key id",
                                    (long) checkSubKeyId, subKeyId);
                        }
                        if (passphrase == null) {
                            return null;
                        }
                        return passphrase;
                    }
                };
            }
        };
    }

}
