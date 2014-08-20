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
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.service.OperationResults.SaveKeyringResult;

import java.io.IOException;
import java.util.Iterator;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class ProviderHelperSaveTest {

    @BeforeClass
    public static void setUpOnce() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testLongKeyIdCollision() throws Exception {

        UncachedKeyRing first =
                readRingFromResource("/cooperpair/9E669861368BCA0BE42DAF7DDDA252EBB8EBE1AF.asc");
        UncachedKeyRing second =
                readRingFromResource("/cooperpair/A55120427374F3F7AA5F1166DDA252EBB8EBE1AF.asc");

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

    UncachedKeyRing readRingFromResource(String name) throws Exception {
        return UncachedKeyRing.fromStream(ProviderHelperSaveTest.class.getResourceAsStream(name)).next();
    }

}