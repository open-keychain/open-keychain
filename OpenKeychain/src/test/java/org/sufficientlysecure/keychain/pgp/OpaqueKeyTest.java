/*
 * Copyright (C) 2017 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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


import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLog;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.support.TestDataUtil;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;


@SuppressWarnings("WeakerAccess")
@RunWith(KeychainTestRunner.class)
public class OpaqueKeyTest {
    @BeforeClass
    public static void setUpOnce() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testOpaqueSubKey__canonicalize__shouldFail() throws Exception {
        UncachedKeyRing ring = readRingFromResource("/test-keys/unknown-sample-1.pub");

        OperationLog log = new OperationLog();
        ring.canonicalize(log, 0);

        assertTrue(log.containsType(LogType.MSG_KC_ERROR_MASTER_ALGO));
    }

    @Test
    public void testOpaqueOidSubKey__canonicalize__shouldFail() throws Exception {
        // key from GnuPG's test suite, sample msg generated using GnuPG v2.1.18
        // TODO use for actual tests once eddsa is supported!
        UncachedKeyRing ring = readRingFromResource("/test-keys/ed25519-cv25519-sample-1.asc");

        OperationLog log = new OperationLog();
        ring.canonicalize(log, 0);

        assertTrue(log.containsType(LogType.MSG_KC_SUB_BAD));
    }

    @Test
    public void testOpaqueSubKey__canonicalize__shouldStrip() throws Exception {
        UncachedKeyRing ring = readRingFromResource("/test-keys/unknown-subkey.pub.asc");

        OperationLog log = new OperationLog();
        CanonicalizedKeyRing canonicalizedKeyRing = ring.canonicalize(log, 0);

        assertNotNull(canonicalizedKeyRing);
        assertTrue(log.containsType(LogType.MSG_KC_SUB_UNKNOWN_ALGO));
    }

    @Test
    public void testOpaqueSubKey__reencode__shouldBeIdentical() throws Exception {
        byte[] rawKeyData = TestDataUtil.readFully(
                OpaqueKeyTest.class.getResourceAsStream("/test-keys/unknown-subkey.pub.asc"));

        UncachedKeyRing ring = UncachedKeyRing.decodeFromData(rawKeyData);

        assertArrayEquals(rawKeyData, ring.getEncoded());
    }

    UncachedKeyRing readRingFromResource(String name) throws Exception {
        return UncachedKeyRing.fromStream(OpaqueKeyTest.class.getResourceAsStream(name)).next();
    }

}