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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.service.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class ProviderHelperSaveTest {

    ProviderHelper mProviderHelper = new ProviderHelper(Robolectric.application);

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
        result = new ProviderHelper(Robolectric.application).savePublicKeyRing(first);
        Assert.assertTrue("first keyring import should succeed", result.success());
        result = new ProviderHelper(Robolectric.application).savePublicKeyRing(second);
        Assert.assertFalse("second keyring import should fail", result.success());

        new KeychainDatabase(Robolectric.application).clearDatabase();

        // and the other way around
        result = new ProviderHelper(Robolectric.application).savePublicKeyRing(second);
        Assert.assertTrue("first keyring import should succeed", result.success());
        result = new ProviderHelper(Robolectric.application).savePublicKeyRing(first);
        Assert.assertFalse("second keyring import should fail", result.success());

    }

    @Test public void testImportNoFlagKey() throws Exception {

        UncachedKeyRing pub = readRingFromResource("/test-keys/mailvelope_07_no_key_flags.asc");
        long keyId = pub.getMasterKeyId();
        Assert.assertNull("key flags should be null", pub.getPublicKey().getKeyUsage());

        mProviderHelper.savePublicKeyRing(pub);

        CachedPublicKeyRing cachedRing = mProviderHelper.getCachedPublicKeyRing(keyId);
        CanonicalizedPublicKeyRing pubRing = mProviderHelper.getCanonicalizedPublicKeyRing(keyId);

        Assert.assertEquals("master key should be signing key", pubRing.getSignId(), keyId);
        Assert.assertEquals("master key should be signing key (cached)", cachedRing.getSignId(), keyId);
        Assert.assertEquals("master key should be encryption key", pubRing.getEncryptId(), keyId);
        Assert.assertEquals("master key should be signing key (cached)", cachedRing.getEncryptId(), keyId);

        Assert.assertNull("canonicalized key flags should be null", pubRing.getPublicKey().getKeyUsage());
        Assert.assertTrue("master key should be able to certify", pubRing.getPublicKey().canCertify());
        Assert.assertTrue("master key should be able to sign", pubRing.getPublicKey().canSign());
        Assert.assertTrue("master key should be able to encrypt", pubRing.getPublicKey().canEncrypt());

    }

    @Test public void testImportDivertToCard() throws Exception {

        UncachedKeyRing sec = readRingFromResource("/test-keys/divert_to_card_sec.asc");
        long keyId = sec.getMasterKeyId();

        SaveKeyringResult result;

        result = mProviderHelper.saveSecretKeyRing(sec, new ProgressScaler());
        Assert.assertTrue("import of secret keyring should succeed", result.success());

        // make sure both the CanonicalizedSecretKeyRing as well as the CachedPublicKeyRing correctly
        // indicate the secret key type
        CachedPublicKeyRing cachedRing = mProviderHelper.getCachedPublicKeyRing(keyId);
        CanonicalizedSecretKeyRing secRing = mProviderHelper.getCanonicalizedSecretKeyRing(keyId);
        for (CanonicalizedSecretKey key : secRing.secretKeyIterator()) {
            Assert.assertEquals("all subkeys from CanonicalizedSecretKeyRing should be divert-to-key",
                    SecretKeyType.DIVERT_TO_CARD, key.getSecretKeyType());
            Assert.assertEquals("all subkeys from CachedPublicKeyRing should be divert-to-key",
                    SecretKeyType.DIVERT_TO_CARD, cachedRing.getSecretKeyType(key.getKeyId()));
        }

    }

    @Test public void testImportBadEncodedUserId() throws Exception {

        UncachedKeyRing key = readRingFromResource("/test-keys/bad_user_id_encoding.asc");
        long keyId = key.getMasterKeyId();

        SaveKeyringResult result;

        result = mProviderHelper.savePublicKeyRing(key, new ProgressScaler());
        Assert.assertTrue("import of keyring should succeed", result.success());

        CanonicalizedPublicKeyRing ring = mProviderHelper.getCanonicalizedPublicKeyRing(keyId);
        boolean found = false;
        byte[] badUserId = Hex.decode("436c61757320467261656e6b656c203c436c6175732e4672e46e6b656c4068616c696661782e727774682d61616368656e2e64653e");
        for (byte[] rawUserId : new IterableIterator<byte[]>(
                ring.getUnorderedRawUserIds().iterator())) {
            if (Arrays.equals(rawUserId, badUserId)) {
                found = true;
            }
        }

        Assert.assertTrue("import of the badly encoded user id should succeed", found);
    }

    UncachedKeyRing readRingFromResource(String name) throws Exception {
        return UncachedKeyRing.fromStream(ProviderHelperSaveTest.class.getResourceAsStream(name)).next();
    }

}