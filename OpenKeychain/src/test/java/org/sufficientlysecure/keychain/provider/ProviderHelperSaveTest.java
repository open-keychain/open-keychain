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

package org.sufficientlysecure.keychain.provider;

import android.content.ContentResolver;
import android.database.Cursor;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.KeyringPassphrases;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ProgressScaler;
import org.sufficientlysecure.keychain.util.TestingUtils;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingData;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 23, manifest = "src/main/AndroidManifest.xml")
public class ProviderHelperSaveTest {

    ProviderHelper mProviderHelper = new ProviderHelper(RuntimeEnvironment.application);

    @BeforeClass
    public static void setUpOnce() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test public void testImportCooperPair() throws Exception {

        // insert two keys with same long key id, make sure the second one gets rejected either way!
        UncachedKeyRing first =
                readRingFromResource("/test-keys/cooperpair/9E669861368BCA0BE42DAF7DDDA252EBB8EBE1AF.asc");
        UncachedKeyRing second =
                readRingFromResource("/test-keys/cooperpair/A55120427374F3F7AA5F1166DDA252EBB8EBE1AF.asc");

        SaveKeyringResult result;

        // insert both keys, second should fail
        result = new ProviderHelper(RuntimeEnvironment.application).write().savePublicKeyRing(first);
        Assert.assertTrue("first keyring import should succeed", result.success());
        result = new ProviderHelper(RuntimeEnvironment.application).write().savePublicKeyRing(second);
        Assert.assertFalse("second keyring import should fail", result.success());

        new KeychainDatabase(RuntimeEnvironment.application).clearDatabase();

        // and the other way around
        result = new ProviderHelper(RuntimeEnvironment.application).write().savePublicKeyRing(second);
        Assert.assertTrue("first keyring import should succeed", result.success());
        result = new ProviderHelper(RuntimeEnvironment.application).write().savePublicKeyRing(first);
        Assert.assertFalse("second keyring import should fail", result.success());

    }

    @Test public void testImportSymantec() throws Exception {

        // symantec pgp desktop exports secret keys without self certificates. we don't support
        // those on their own, but if they are imported together with their public key (or if
        // the public key is already known), the self certs info will be merged in as a special
        // case.
        UncachedKeyRing seckey =
                readRingFromResource("/test-keys/symantec_secret.asc");
        UncachedKeyRing pubkey =
                readRingFromResource("/test-keys/symantec_public.asc");

        SaveKeyringResult result;

        // insert secret, this should fail because of missing self-cert
        result = new ProviderHelper(RuntimeEnvironment.application).write().saveSecretKeyRingForTest(seckey);
        Assert.assertFalse("secret keyring import before pubring import should fail", result.success());

        // insert pubkey, then seckey - both should succeed
        result = new ProviderHelper(RuntimeEnvironment.application).write().savePublicKeyRing(pubkey);
        Assert.assertTrue("public keyring import should succeed", result.success());
        result = new ProviderHelper(RuntimeEnvironment.application).write().saveSecretKeyRingForTest(seckey);
        Assert.assertTrue("secret keyring import after pubring import should succeed", result.success());

    }

    @Test public void testImportNoFlagKey() throws Exception {

        UncachedKeyRing pub = readRingFromResource("/test-keys/mailvelope_07_no_key_flags.asc");
        long keyId = pub.getMasterKeyId();
        Assert.assertEquals("key flags should be zero",
                0, (long) pub.canonicalize(new OperationLog(), 0).getPublicKey().getKeyUsage());

        mProviderHelper.write().savePublicKeyRing(pub);

        CachedPublicKeyRing cachedRing = mProviderHelper.read().getCachedPublicKeyRing(keyId);
        CanonicalizedPublicKeyRing pubRing = mProviderHelper.read().getCanonicalizedPublicKeyRing(keyId);

        Assert.assertEquals("master key should be encryption key", keyId, pubRing.getEncryptId());
        Assert.assertEquals("master key should be encryption key (cached)", keyId, cachedRing.getEncryptId());

        Assert.assertEquals("canonicalized key flags should be zero",
                0, (long) pubRing.getPublicKey().getKeyUsage());
        Assert.assertTrue("master key should be able to certify", pubRing.getPublicKey().canCertify());
        Assert.assertTrue("master key should be allowed to sign", pubRing.getPublicKey().canSign());
        Assert.assertTrue("master key should be able to encrypt", pubRing.getPublicKey().canEncrypt());

    }

    @Test public void testImportDivertToCard() throws Exception {

        UncachedKeyRing sec = readRingFromResource("/test-keys/divert_to_card_sec.asc");
        long keyId = sec.getMasterKeyId();

        SaveKeyringResult result;

        result = mProviderHelper.write().saveSecretKeyRingForTest(sec);
        Assert.assertTrue("import of secret keyring should succeed", result.success());

        // make sure both the CanonicalizedSecretKeyRing as well as the CachedPublicKeyRing correctly
        // indicate the secret key type
        CachedPublicKeyRing cachedRing = mProviderHelper.read().getCachedPublicKeyRing(keyId);
        CanonicalizedSecretKeyRing secRing = mProviderHelper.read().getCanonicalizedSecretKeyRingForTest(keyId);

        Iterator<CanonicalizedSecretKey> it = secRing.secretKeyIterator().iterator();

        { // first subkey
            Assert.assertTrue("keyring should have 3 subkeys (1)", it.hasNext());
            CanonicalizedSecretKey key = it.next();
            Assert.assertEquals("first subkey should be of type sign+certify",
                    KeyFlags.CERTIFY_OTHER | KeyFlags.SIGN_DATA, (int) key.getKeyUsage());
            Assert.assertEquals("first subkey should be divert-to-card",
                    SecretKeyType.DIVERT_TO_CARD, key.getSecretKeyTypeSuperExpensive());
            Assert.assertTrue("canCertify() should be true", key.canCertify());
            Assert.assertTrue("canSign() should be true", key.canSign());

            // cached
            Assert.assertEquals("all subkeys from CachedPublicKeyRing should be divert-to-key",
                    SecretKeyType.DIVERT_TO_CARD, cachedRing.getSecretKeyType(key.getKeyId()));
        }

        { // second subkey
            Assert.assertTrue("keyring should have 3 subkeys (2)", it.hasNext());
            CanonicalizedSecretKey key = it.next();
            Assert.assertEquals("second subkey should be of type authenticate",
                    KeyFlags.AUTHENTICATION, (int) key.getKeyUsage());
            Assert.assertEquals("second subkey should be divert-to-card",
                    SecretKeyType.DIVERT_TO_CARD, key.getSecretKeyTypeSuperExpensive());
            Assert.assertTrue("canAuthenticate() should be true", key.canAuthenticate());

            // cached
            Assert.assertEquals("all subkeys from CachedPublicKeyRing should be divert-to-key",
                    SecretKeyType.DIVERT_TO_CARD, cachedRing.getSecretKeyType(key.getKeyId()));
        }

        { // third subkey
            Assert.assertTrue("keyring should have 3 subkeys (3)", it.hasNext());
            CanonicalizedSecretKey key = it.next();
            Assert.assertEquals("first subkey should be of type encrypt (both types)",
                    KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE, (int) key.getKeyUsage());
            Assert.assertEquals("third subkey should be divert-to-card",
                    SecretKeyType.DIVERT_TO_CARD, key.getSecretKeyTypeSuperExpensive());
            Assert.assertTrue("canEncrypt() should be true", key.canEncrypt());

            // cached
            Assert.assertEquals("all subkeys from CachedPublicKeyRing should be divert-to-key",
                    SecretKeyType.DIVERT_TO_CARD, cachedRing.getSecretKeyType(key.getKeyId()));
        }

        Assert.assertFalse("keyring should have 3 subkeys (4)", it.hasNext());

    }

    @Test public void testImportBadEncodedUserId() throws Exception {

        UncachedKeyRing key = readRingFromResource("/test-keys/bad_user_id_encoding.asc");
        long keyId = key.getMasterKeyId();

        SaveKeyringResult result;

        result = mProviderHelper.write().savePublicKeyRing(key);
        Assert.assertTrue("import of keyring should succeed", result.success());

        CanonicalizedPublicKeyRing ring = mProviderHelper.read().getCanonicalizedPublicKeyRing(keyId);
        boolean found = false;
        byte[] badUserId = Hex.decode("436c61757320467261656e6b656c203c436c6175732e4672e46e6b656c4068616c696661782e727774682d61616368656e2e64653e");
        for (byte[] rawUserId : new IterableIterator<>(
                ring.getUnorderedRawUserIds().iterator())) {
            if (Arrays.equals(rawUserId, badUserId)) {
                found = true;
            }
        }

        Assert.assertTrue("import of the badly encoded user id should succeed", found);
    }

    @Test
    /** Tests a master key which may sign, but is stripped. In this case, if there is a different
     * subkey available which can sign, that one should be selected.
     */
    public void testImportStrippedFlags() throws Exception {

        UncachedKeyRing key = readRingFromResource("/test-keys/stripped_flags.asc");
        long masterKeyId = key.getMasterKeyId();

        SaveKeyringResult result;

        result = mProviderHelper.write().saveSecretKeyRingForTest(key);
        Assert.assertTrue("import of keyring should succeed", result.success());

        long signId;
        {
            CanonicalizedSecretKeyRing ring = mProviderHelper.read().getCanonicalizedSecretKeyRingForTest(masterKeyId);
            Assert.assertTrue("master key should have sign flag", ring.getPublicKey().canSign());
            Assert.assertTrue("master key should have encrypt flag", ring.getPublicKey().canEncrypt());

            signId = mProviderHelper.read().getCachedPublicKeyRing(masterKeyId).getSecretSignId();
            Assert.assertNotEquals("encrypt id should not be 0", 0, signId);
            Assert.assertNotEquals("encrypt key should be different from master key", masterKeyId, signId);
        }

        {
            CachedPublicKeyRing ring = mProviderHelper.read().getCachedPublicKeyRing(masterKeyId);
            Assert.assertEquals("signing key should be same id cached as uncached", signId, ring.getSecretSignId());
        }

    }

    @Test
    public void testImportAndRetrieve() throws Exception {
        Passphrase keyringPassphrase = TestingUtils.genPassphrase();

        // save key
        UncachedKeyRing secKey = readRingFromResource("/test-keys/basickey/empty-passphrase-gpg-sec.asc");
        CanonicalizedSecretKeyRing canSecKeyRing = (CanonicalizedSecretKeyRing) secKey.canonicalize(new OperationLog(), 0);
        KeyringPassphrases passphrases = TestingUtils.generateImportPassphrases(secKey, new Passphrase(), keyringPassphrase);
        OperationResult saveResult = mProviderHelper.write().saveSecretKeyRing(secKey, passphrases, new ProgressScaler());
        Assert.assertTrue("Failed to insert secret key", saveResult.success());

        // retrieve key
        CanonicalizedSecretKeyRing retrievedSecKeyRing =
                mProviderHelper.read().getCanonicalizedSecretKeyRing(secKey.getMasterKeyId(), keyringPassphrase);

        for (CanonicalizedSecretKey key : canSecKeyRing.secretKeyIterator()) {
            Assert.assertNotNull("Failed to save a key & re-encrypt",
                    retrievedSecKeyRing.getSecretKey(key.getKeyId()).unlock(new Passphrase()));
        }
    }

    @Test
    public void testImportPublicWithDelayedMerge() throws Exception {
        Passphrase keyringPassphrase = TestingUtils.genPassphrase();

        UncachedKeyRing secKey = readRingFromResource("/test-keys/basickey/empty-passphrase-gpg-sec.asc");
        KeyringPassphrases passphrases = TestingUtils.generateImportPassphrases(secKey, new Passphrase(), keyringPassphrase);
        OperationResult saveResult = mProviderHelper.write().saveSecretKeyRing(secKey, passphrases, new ProgressScaler());
        Assert.assertTrue("Failed to insert secret key", saveResult.success());

        long masterKeyId = secKey.getMasterKeyId();

        UncachedKeyRing updatedPubKey = readRingFromResource("/test-keys/basickey/with-new-key.pub.asc");
        SaveKeyringResult pubSaveResult = mProviderHelper.write().savePublicKeyRing(updatedPubKey);
        int resultCode = pubSaveResult.getResult();

        Assert.assertTrue("Failed to insert public key",
                (resultCode & SaveKeyringResult.SAVED_PUBLIC) == SaveKeyringResult.SAVED_PUBLIC);
        Assert.assertTrue("Failed to insert secret key",
                (resultCode & SaveKeyringResult.SAVED_SECRET) == SaveKeyringResult.SAVED_SECRET);

        ContentResolver resolver = RuntimeEnvironment.application.getContentResolver();

        // Check if key types are as expected, should have 2 passphrase_empty & 1 dummy
        Cursor cursor = resolver.query(Keys.buildKeysUri(masterKeyId),
                new String[]{ Keys.HAS_SECRET }, null, null, null);
        ArrayList<SecretKeyType> expectedKeyTypes = new ArrayList<>();
        expectedKeyTypes.add(SecretKeyType.PASSPHRASE_EMPTY);
        expectedKeyTypes.add(SecretKeyType.PASSPHRASE_EMPTY);
        expectedKeyTypes.add(SecretKeyType.GNU_DUMMY);

        Assert.assertTrue("Incomplete import of secret data", cursor != null);
        while (cursor.moveToNext()) {
            SecretKeyType keyType = SecretKeyType.fromNum(cursor.getInt(0));
            Assert.assertTrue("Incomplete import of secret data", expectedKeyTypes.contains(keyType));
            expectedKeyTypes.remove(keyType);
        }
        cursor.close();

        // Check if the merge flag is set
        cursor = resolver.query( KeyRingData.buildSecretKeyRingUri(masterKeyId),
                new String[]{ KeyRingData.AWAITING_MERGE }, null, null, null);
        Assert.assertTrue("Incomplete import of secret data", cursor != null);
        Assert.assertTrue("Failed to set pending merge flag", cursor.moveToFirst() && cursor.getInt(0) == 1);
        cursor.close();

        // Force the merge by retrieving secret key
        CanonicalizedSecretKeyRing mergedSecRing =
                mProviderHelper.read().getCanonicalizedSecretKeyRingWithMerge(masterKeyId, keyringPassphrase);

        int keyCount = 0;
        Iterator<UncachedPublicKey> keys = mergedSecRing.getUncachedKeyRing().getPublicKeys();
        for (; keys.hasNext(); keys.next()) {
            keyCount++;
        }
        Assert.assertTrue("Merge has failed", keyCount == 3);
    }

    UncachedKeyRing readRingFromResource(String name) throws Exception {
        return UncachedKeyRing.fromStream(ProviderHelperSaveTest.class.getResourceAsStream(name)).next();
    }

}