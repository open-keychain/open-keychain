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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.openpgp.OpenPgpMetadata;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.bcpg.BCPGInputStream;
import org.spongycastle.bcpg.Packet;
import org.spongycastle.bcpg.PacketTags;
import org.spongycastle.bcpg.PublicKeyEncSessionPacket;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.operations.results.PgpSignEncryptResult;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingData;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel.RequiredInputType;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper.RawPacket;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ProgressScaler;
import org.sufficientlysecure.keychain.util.TestingUtils;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;


@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 21, manifest = "src/main/AndroidManifest.xml")
public class PgpEncryptDecryptTest {

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
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.RSA, 1024, null, KeyFlags.ENCRYPT_COMMS, 0L));
            parcel.mAddUserIds.add("bloom");
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
            parcel.mAddUserIds.add("belle");
            parcel.mNewUnlock = new ChangeUnlockParcel(mKeyPhrase2);

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
    public void testSymmetricEncryptDecrypt() {

        String plaintext = "dies ist ein plaintext ☭" + TestingUtils.genPassphrase(true);
        byte[] ciphertext;

        { // encrypt data with a given passphrase
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(plaintext.getBytes());

            PgpSignEncryptOperation op = new PgpSignEncryptOperation(RuntimeEnvironment.application,
                    new ProviderHelper(RuntimeEnvironment.application), null);

            InputData data = new InputData(in, in.available());

            PgpSignEncryptInputParcel b = new PgpSignEncryptInputParcel();
            b.setSymmetricPassphrase(mPassphrase);
            b.setSymmetricEncryptionAlgorithm(PGPEncryptedData.AES_128);

            PgpSignEncryptResult result = op.execute(b, new CryptoInputParcel(), data, out);

            Assert.assertTrue("encryption must succeed", result.success());

            ciphertext = out.toByteArray();
        }

        { // decryption with same passphrase should yield the same result

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify op = new PgpDecryptVerify(RuntimeEnvironment.application,
                    new ProviderHelper(RuntimeEnvironment.application), null);
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
            input.setAllowSymmetricDecryption(true);
            DecryptVerifyResult result = op.execute(
                    input, new CryptoInputParcel(mPassphrase), data, out);

            Assert.assertTrue("decryption must succeed", result.success());
            Assert.assertArrayEquals("decrypted ciphertext should equal plaintext",
                    out.toByteArray(), plaintext.getBytes());
            Assert.assertNull("signature should be an error", result.getSignatureResult());

            CryptoInputParcel cryptoInput = result.getCachedCryptoInputParcel();
            Assert.assertEquals("cached session keys must be empty",
                    0, cryptoInput.getCryptoData().size());

            OpenPgpMetadata metadata = result.getDecryptMetadata();
            Assert.assertEquals("filesize must be correct",
                    out.toByteArray().length, metadata.getOriginalSize());
        }

        { // decryption with a bad passphrase should fail

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify op = new PgpDecryptVerify(RuntimeEnvironment.application,
                    new ProviderHelper(RuntimeEnvironment.application), null);
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
            input.setAllowSymmetricDecryption(true);
            DecryptVerifyResult result = op.execute(input,
                    new CryptoInputParcel(new Passphrase(new String(mPassphrase.getCharArray()) + "x")),
                    data, out);

            Assert.assertFalse("decryption must fail", result.success());
            Assert.assertEquals("decrypted plaintext should be empty", 0, out.size());
            Assert.assertNull("signature should be an error", result.getSignatureResult());
        }

        { // decryption with an unset passphrase should fail

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify op = new PgpDecryptVerify(RuntimeEnvironment.application,
                    new ProviderHelper(RuntimeEnvironment.application), null);
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
            input.setAllowSymmetricDecryption(true);
            DecryptVerifyResult result = op.execute(input,
                    new CryptoInputParcel(), data, out);

            Assert.assertFalse("decryption must fail", result.success());
            Assert.assertEquals("decrypted plaintext should be empty", 0, out.size());
            Assert.assertNull("signature should be an error", result.getSignatureResult());
        }

        { // decryption if symmetric decryption isn't allowed should fail

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify op = new PgpDecryptVerify(RuntimeEnvironment.application,
                    new ProviderHelper(RuntimeEnvironment.application), null);
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
            input.setAllowSymmetricDecryption(false);
            DecryptVerifyResult result = op.execute(input,
                    new CryptoInputParcel(), data, out);

            Assert.assertFalse("decryption must fail", result.success());
            Assert.assertEquals("decrypted plaintext should be empty", 0, out.size());
            Assert.assertNull("signature should be an error", result.getSignatureResult());
        }

    }

    @Test
    public void testAsymmetricEncryptDecrypt() {

        String plaintext = "dies ist ein plaintext ☭" + TestingUtils.genPassphrase(true);
        byte[] ciphertext;

        { // encrypt data with key
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(plaintext.getBytes());

            PgpSignEncryptOperation op = new PgpSignEncryptOperation(RuntimeEnvironment.application,
                    new ProviderHelper(RuntimeEnvironment.application), null);

            InputData data = new InputData(in, in.available());
            PgpSignEncryptInputParcel input = new PgpSignEncryptInputParcel();

            input.setEncryptionMasterKeyIds(new long[] { mStaticRing1.getMasterKeyId() });
            input.setSymmetricEncryptionAlgorithm(PGPEncryptedData.AES_128);
            PgpSignEncryptResult result = op.execute(input, new CryptoInputParcel(), data, out);
            Assert.assertTrue("encryption must succeed", result.success());

            ciphertext = out.toByteArray();
        }

        { // decryption with provided passphrase should yield the same result

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify op = operationWithFakePassphraseCache(null, null, null);
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
            DecryptVerifyResult result = op.execute(input, new CryptoInputParcel(mKeyPhrase1), data, out);

            Assert.assertTrue("decryption with provided passphrase must succeed", result.success());
            Assert.assertArrayEquals("decrypted ciphertext with provided passphrase should equal plaintext",
                    out.toByteArray(), plaintext.getBytes());
            Assert.assertNull("signature be empty", result.getSignatureResult());

            CryptoInputParcel cryptoInput = result.getCachedCryptoInputParcel();
            Assert.assertEquals("must have one cached session key",
                    1, cryptoInput.getCryptoData().size());

            OpenPgpMetadata metadata = result.getDecryptMetadata();
            Assert.assertEquals("filesize must be correct",
                    out.toByteArray().length, metadata.getOriginalSize());

        }

        { // decryption with passphrase cached should succeed

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify op = operationWithFakePassphraseCache(
                    mKeyPhrase1, mStaticRing1.getMasterKeyId(), null);
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
            DecryptVerifyResult result = op.execute(input, new CryptoInputParcel(), data, out);

            CryptoInputParcel cryptoInput = result.getCachedCryptoInputParcel();
            Assert.assertEquals("must have one cached session key",
                    1, cryptoInput.getCryptoData().size());

            Assert.assertTrue("decryption with cached passphrase must succeed", result.success());
            Assert.assertArrayEquals("decrypted ciphertext with cached passphrase  should equal plaintext",
                    out.toByteArray(), plaintext.getBytes());
            Assert.assertNull("signature should be empty", result.getSignatureResult());
        }

        { // decryption with no passphrase provided should return status pending

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify op = operationWithFakePassphraseCache(
                    null, mStaticRing1.getMasterKeyId(), null);
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
            DecryptVerifyResult result = op.execute(input, new CryptoInputParcel(), data, out);

            Assert.assertFalse("decryption with no passphrase must return pending", result.success());
            Assert.assertTrue("decryption with no passphrase should return pending", result.isPending());
            Assert.assertEquals("decryption with no passphrase should return pending passphrase",
                    RequiredInputType.PASSPHRASE, result.getRequiredInputParcel().mType);
        }

    }

    @Test
    public void testAsymmetricMultiSubkeyEncrypt() throws Exception {

        String plaintext = "dies ist ein plaintext ☭" + TestingUtils.genPassphrase(true);

        { // encrypt data with key
            byte[] ciphertext;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(plaintext.getBytes());

            PgpSignEncryptOperation op = new PgpSignEncryptOperation(RuntimeEnvironment.application,
                    new ProviderHelper(RuntimeEnvironment.application), null);

            InputData data = new InputData(in, in.available());
            PgpSignEncryptInputParcel input = new PgpSignEncryptInputParcel();

            input.setEncryptionMasterKeyIds(new long[] { mStaticRing1.getMasterKeyId() });
            input.setSymmetricEncryptionAlgorithm(PGPEncryptedData.AES_128);
            PgpSignEncryptResult result = op.execute(input, new CryptoInputParcel(), data, out);
            Assert.assertTrue("encryption must succeed", result.success());

            ciphertext = out.toByteArray();

            Iterator<RawPacket> packets = KeyringTestingHelper.parseKeyring(ciphertext);

            RawPacket enc1 = packets.next(), enc2 = packets.next();
            Assert.assertEquals("last packet must be encrypted data packet",
                    PacketTags.SYM_ENC_INTEGRITY_PRO, packets.next().tag);
            Assert.assertFalse("no further packets", packets.hasNext());

            Packet p;
            p = new BCPGInputStream(new ByteArrayInputStream(enc1.buf)).readPacket();
            Assert.assertTrue("first packet must be session packet", p instanceof PublicKeyEncSessionPacket);
            long encKeyId1 = ((PublicKeyEncSessionPacket) p).getKeyID();

            p = new BCPGInputStream(new ByteArrayInputStream(enc2.buf)).readPacket();
            Assert.assertTrue("second packet must be session packet", p instanceof PublicKeyEncSessionPacket);
            long encKeyId2 = ((PublicKeyEncSessionPacket) p).getKeyID();

            Assert.assertNotEquals("encrypted-to subkey ids must not be equal",
                    encKeyId1, encKeyId2);
            Assert.assertThat("first packet must be encrypted to one of the subkeys",
                    KeyringTestingHelper.getSubkeyId(mStaticRing1, 2), anyOf(is(encKeyId1), is(encKeyId2)));
            Assert.assertThat("second packet must be encrypted to one of the subkeys",
                    KeyringTestingHelper.getSubkeyId(mStaticRing1, 3), anyOf(is(encKeyId1), is(encKeyId2)));

        }

        { // revoke first encryption subkey of keyring in database
            SaveKeyringParcel parcel = new SaveKeyringParcel(mStaticRing1.getMasterKeyId(), mStaticRing1.getFingerprint());
            parcel.mRevokeSubKeys.add(KeyringTestingHelper.getSubkeyId(mStaticRing1, 2));
            UncachedKeyRing modified = PgpKeyOperationTest.applyModificationWithChecks(parcel, mStaticRing1,
                    new ArrayList<RawPacket>(), new ArrayList<RawPacket>(), new CryptoInputParcel(mKeyPhrase1));

            ProviderHelper providerHelper = new ProviderHelper(RuntimeEnvironment.application);
            providerHelper.saveSecretKeyRing(modified, new ProgressScaler());
        }

        { // encrypt to this keyring, make sure it's not encrypted to the revoked subkey
            byte[] ciphertext;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(plaintext.getBytes());

            PgpSignEncryptOperation op = new PgpSignEncryptOperation(RuntimeEnvironment.application,
                    new ProviderHelper(RuntimeEnvironment.application), null);

            InputData data = new InputData(in, in.available());
            PgpSignEncryptInputParcel input = new PgpSignEncryptInputParcel();

            input.setEncryptionMasterKeyIds(new long[] { mStaticRing1.getMasterKeyId() });
            input.setSymmetricEncryptionAlgorithm(PGPEncryptedData.AES_128);
            PgpSignEncryptResult result = op.execute(input, new CryptoInputParcel(), data, out);
            Assert.assertTrue("encryption must succeed", result.success());

            ciphertext = out.toByteArray();

            Iterator<RawPacket> packets = KeyringTestingHelper.parseKeyring(ciphertext);

            RawPacket enc1 = packets.next();
            Assert.assertEquals("last packet must be encrypted data packet",
                    PacketTags.SYM_ENC_INTEGRITY_PRO, packets.next().tag);
            Assert.assertFalse("no further packets", packets.hasNext());

            Packet p;
            p = new BCPGInputStream(new ByteArrayInputStream(enc1.buf)).readPacket();
            Assert.assertTrue("first packet must be session packet", p instanceof PublicKeyEncSessionPacket);
            Assert.assertEquals("first packet must be encrypted to second enc subkey",
                    KeyringTestingHelper.getSubkeyId(mStaticRing1, 3), ((PublicKeyEncSessionPacket) p).getKeyID());

        }

    }

    @Test
    public void testMultiAsymmetricEncryptDecrypt() {

        String plaintext = "dies ist ein plaintext ☭" + TestingUtils.genPassphrase(true);
        byte[] ciphertext;

        { // encrypt data with a given passphrase
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(plaintext.getBytes());

            PgpSignEncryptOperation op = new PgpSignEncryptOperation(RuntimeEnvironment.application,
                    new ProviderHelper(RuntimeEnvironment.application), null);

            InputData data = new InputData(in, in.available());

            PgpSignEncryptInputParcel b = new PgpSignEncryptInputParcel();
            b.setEncryptionMasterKeyIds(new long[] {
                    mStaticRing1.getMasterKeyId(),
                    mStaticRing2.getMasterKeyId()
            });
            b.setSymmetricEncryptionAlgorithm(PGPEncryptedData.AES_128);

            PgpSignEncryptResult result = op.execute(b, new CryptoInputParcel(), data, out);
            Assert.assertTrue("encryption must succeed", result.success());

            ciphertext = out.toByteArray();
        }

        { // decryption with passphrase cached should succeed for the first key

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify op = operationWithFakePassphraseCache(
                    mKeyPhrase1, mStaticRing1.getMasterKeyId(), null);
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
            DecryptVerifyResult result = op.execute(input, new CryptoInputParcel(), data, out);

            Assert.assertTrue("decryption with cached passphrase must succeed for the first key", result.success());
            Assert.assertArrayEquals("decrypted ciphertext with cached passphrase  should equal plaintext",
                    out.toByteArray(), plaintext.getBytes());
            Assert.assertNull("signature should be empty", result.getSignatureResult());

            OpenPgpMetadata metadata = result.getDecryptMetadata();
            Assert.assertEquals("filesize must be correct",
                    out.toByteArray().length, metadata.getOriginalSize());
        }

        { // decryption should succeed if key is allowed

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            // allow only the second to decrypt
            HashSet<Long> allowed = new HashSet<>();
            allowed.add(mStaticRing2.getMasterKeyId());

            // provide passphrase for the second, and check that the first is never asked for!
            PgpDecryptVerify op = operationWithFakePassphraseCache(
                    mKeyPhrase2, mStaticRing2.getMasterKeyId(), null);
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
            input.setAllowedKeyIds(allowed);
            DecryptVerifyResult result = op.execute(input, new CryptoInputParcel(), data, out);

            Assert.assertTrue("decryption with cached passphrase must succeed for allowed key", result.success());
            Assert.assertArrayEquals("decrypted ciphertext with cached passphrase  should equal plaintext",
                    out.toByteArray(), plaintext.getBytes());
            Assert.assertTrue("other key was skipped", result.getLog().containsType(LogType.MSG_DC_ASKIP_NOT_ALLOWED));
            Assert.assertNull("signature should be empty", result.getSignatureResult());
        }

        { // decryption should fail if no key is allowed

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            // provide passphrase for the second, and check that the first is never asked for!
            PgpDecryptVerify op = operationWithFakePassphraseCache(
                    mKeyPhrase2, mStaticRing2.getMasterKeyId(), null);
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
            input.setAllowedKeyIds(new HashSet<Long>());
            DecryptVerifyResult result = op.execute(input, new CryptoInputParcel(), data, out);

            Assert.assertFalse("decryption must fail if no key allowed", result.success());
            Assert.assertEquals("decryption must fail with key disllowed status",
                    DecryptVerifyResult.RESULT_KEY_DISALLOWED, result.getResult());

        }

        { // decryption with passphrase cached should succeed for the other key if first is gone

            // delete first key from database
            new ProviderHelper(RuntimeEnvironment.application).getContentResolver().delete(
                    KeyRingData.buildPublicKeyRingUri(mStaticRing1.getMasterKeyId()), null, null
            );

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify op = operationWithFakePassphraseCache(
                    mKeyPhrase2, mStaticRing2.getMasterKeyId(), null);
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
            DecryptVerifyResult result = op.execute(input, new CryptoInputParcel(), data, out);

            Assert.assertTrue("decryption with cached passphrase must succeed", result.success());
            Assert.assertArrayEquals("decrypted ciphertext with cached passphrase  should equal plaintext",
                    out.toByteArray(), plaintext.getBytes());
            Assert.assertNull("signature should be empty", result.getSignatureResult());
        }

    }

    @Test
    public void testMultiAsymmetricSignEncryptDecryptVerify() {

        String plaintext = "dies ist ein plaintext ☭" + TestingUtils.genPassphrase(true);
        byte[] ciphertext;

        { // encrypt data with a given passphrase
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(plaintext.getBytes());

            PgpSignEncryptOperation op = new PgpSignEncryptOperation(RuntimeEnvironment.application,
                    new ProviderHelper(RuntimeEnvironment.application), null);

            InputData data = new InputData(in, in.available());
            PgpSignEncryptInputParcel b = new PgpSignEncryptInputParcel();

            b.setEncryptionMasterKeyIds(new long[] {
                    mStaticRing1.getMasterKeyId(),
                    mStaticRing2.getMasterKeyId()
            });
            b.setSignatureMasterKeyId(mStaticRing1.getMasterKeyId());
            b.setSignatureSubKeyId(KeyringTestingHelper.getSubkeyId(mStaticRing1, 1));
            b.setSymmetricEncryptionAlgorithm(PGPEncryptedData.AES_128);

            PgpSignEncryptResult result = op.execute(b, new CryptoInputParcel(mKeyPhrase1), data, out);
            Assert.assertTrue("encryption must succeed", result.success());

            ciphertext = out.toByteArray();
        }

        { // decryption with passphrase cached should succeed for the first key

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify op = operationWithFakePassphraseCache(
                    mKeyPhrase1, mStaticRing1.getMasterKeyId(), null);
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
            DecryptVerifyResult result = op.execute(input, new CryptoInputParcel(), data, out);

            Assert.assertTrue("decryption with cached passphrase must succeed for the first key", result.success());
            Assert.assertArrayEquals("decrypted ciphertext with cached passphrase  should equal plaintext",
                    out.toByteArray(), plaintext.getBytes());
            Assert.assertEquals("signature should be verified and certified",
                    OpenPgpSignatureResult.SIGNATURE_SUCCESS_CERTIFIED, result.getSignatureResult().getStatus());

            OpenPgpMetadata metadata = result.getDecryptMetadata();
            Assert.assertEquals("filesize must be correct",
                    out.toByteArray().length, metadata.getOriginalSize());
        }

        { // decryption with passphrase cached should succeed for the other key if first is gone

            // delete first key from database
            new ProviderHelper(RuntimeEnvironment.application).getContentResolver().delete(
                    KeyRingData.buildPublicKeyRingUri(mStaticRing1.getMasterKeyId()), null, null
            );

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify op = operationWithFakePassphraseCache(
                    mKeyPhrase2, mStaticRing2.getMasterKeyId(), null);
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
            DecryptVerifyResult result = op.execute(input, new CryptoInputParcel(), data, out);

            Assert.assertTrue("decryption with cached passphrase must succeed", result.success());
            Assert.assertArrayEquals("decrypted ciphertext with cached passphrase  should equal plaintext",
                    out.toByteArray(), plaintext.getBytes());
            Assert.assertEquals("signature key should be missing",
                    OpenPgpSignatureResult.SIGNATURE_KEY_MISSING,
                    result.getSignatureResult().getStatus());
        }

    }

    @Test
    public void testForeignEncoding() throws Exception {
        String plaintext = "ウィキペディア";
        byte[] plaindata = plaintext.getBytes("iso-2022-jp");

        { // some quick sanity checks
            Assert.assertEquals(plaintext, new String(plaindata, "iso-2022-jp"));
            Assert.assertNotEquals(plaintext, new String(plaindata, "utf-8"));
        }

        byte[] ciphertext;
        { // encrypt data with a given passphrase
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(plaindata);

            PgpSignEncryptOperation op = new PgpSignEncryptOperation(RuntimeEnvironment.application,
                    new ProviderHelper(RuntimeEnvironment.application), null);

            InputData data = new InputData(in, in.available());
            PgpSignEncryptInputParcel b = new PgpSignEncryptInputParcel();

            b.setEncryptionMasterKeyIds(new long[] { mStaticRing1.getMasterKeyId() });
            b.setSymmetricEncryptionAlgorithm(PGPEncryptedData.AES_128);
            // this only works with ascii armored output!
            b.setEnableAsciiArmorOutput(true);
            b.setCharset("iso-2022-jp");
            PgpSignEncryptResult result = op.execute(b, new CryptoInputParcel(), data, out);
            Assert.assertTrue("encryption must succeed", result.success());

            ciphertext = out.toByteArray();
        }

        { // decryption with provided passphrase should yield the same result

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
            InputData data = new InputData(in, in.available());

            PgpDecryptVerify op = operationWithFakePassphraseCache(null, null, null);
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
            DecryptVerifyResult result = op.execute(input, new CryptoInputParcel(mKeyPhrase1), data, out);

            Assert.assertTrue("decryption with provided passphrase must succeed", result.success());
            Assert.assertArrayEquals("decrypted ciphertext should equal plaintext bytes",
                    out.toByteArray(), plaindata);
            Assert.assertEquals("charset should be read correctly",
                    "iso-2022-jp", result.getCharset());
            Assert.assertEquals("decrypted ciphertext should equal plaintext",
                    new String(out.toByteArray(), result.getCharset()), plaintext);
            Assert.assertNull("signature be empty", result.getSignatureResult());
        }

    }

    private PgpDecryptVerify operationWithFakePassphraseCache(
            final Passphrase passphrase, final Long checkMasterKeyId, final Long checkSubKeyId) {

        return new PgpDecryptVerify(RuntimeEnvironment.application,
                new ProviderHelper(RuntimeEnvironment.application), null) {
            @Override
            public Passphrase getCachedPassphrase(long masterKeyId, long subKeyId)
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
}