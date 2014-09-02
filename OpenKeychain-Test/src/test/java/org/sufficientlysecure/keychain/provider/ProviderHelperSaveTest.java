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
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.service.OperationResults.SaveKeyringResult;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.io.IOException;
import java.util.Iterator;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class ProviderHelperSaveTest {

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

    @Test public void testImportDivertToCard() throws Exception {

        UncachedKeyRing pub =
                readRingFromResource("/test-keys/divert_to_card_pub.asc");
        UncachedKeyRing sec =
                readRingFromResource("/test-keys/divert_to_card_sec.asc");
        long keyId = sec.getMasterKeyId();

        SaveKeyringResult result;

        // insert both keys, second should fail
        result = new ProviderHelper(Robolectric.application).savePublicKeyRing(pub);
        Assert.assertTrue("import of public keyring should succeed", result.success());
        result = new ProviderHelper(Robolectric.application).saveSecretKeyRing(sec,
                new ProgressScaler());
        Assert.assertTrue("import of secret keyring should succeed", result.success());

        CanonicalizedSecretKeyRing secRing =
                new ProviderHelper(Robolectric.application).getCanonicalizedSecretKeyRing(keyId);
        for (CanonicalizedSecretKey key : secRing.secretKeyIterator()) {
            Assert.assertEquals("all subkeys should be divert-to-key",
                    SecretKeyType.DIVERT_TO_CARD, key.getSecretKeyType());
        }

        /*
        CachedPublicKeyRing cachedRing =
                new ProviderHelper(Robolectric.application).getCachedPublicKeyRing(keyId);
        for (CanonicalizedSecretKey key : cachedRing.()) {
            Assert.assertEquals("all subkeys should be divert-to-key",
                    SecretKeyType.DIVERT_TO_CARD, key.getSecretKeyType());
        }
        */

    }

    UncachedKeyRing readRingFromResource(String name) throws Exception {
        return UncachedKeyRing.fromStream(ProviderHelperSaveTest.class.getResourceAsStream(name)).next();
    }

}