/*
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2017 Christian Hagau <ach@hagau.se>
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

package org.sufficientlysecure.keychain.ssh;


import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.SshPublicKey;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeyWritableRepository;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.PrintStream;
import java.security.Security;

@RunWith(KeychainTestRunner.class)
public class SshPublicKeyTest {

    private static UncachedKeyRing mStaticRingEcDsa;
    private static Passphrase mKeyPhrase;

    private static PrintStream oldShadowStream;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        oldShadowStream = ShadowLog.stream;
        // ShadowLog.stream = System.out;

        mKeyPhrase = new Passphrase("x");
        mStaticRingEcDsa = KeyringTestingHelper.readRingFromResource("/test-keys/authenticate_ecdsa.sec");
    }

    @Before
    public void setUp() {
        KeyWritableRepository databaseInteractor =
                KeyWritableRepository.create(RuntimeEnvironment.application);

        // don't log verbosely here, we're not here to test imports
        ShadowLog.stream = oldShadowStream;

        databaseInteractor.saveSecretKeyRing(mStaticRingEcDsa);

        // ok NOW log verbosely!
        ShadowLog.stream = System.out;
    }

    @Test
    public void testECDSA() throws Exception {
        KeyRepository keyRepository = KeyRepository.create(RuntimeEnvironment.application);

        long masterKeyId = mStaticRingEcDsa.getMasterKeyId();
        long authSubKeyId = keyRepository.getCachedPublicKeyRing(masterKeyId).getSecretAuthenticationId();
        CanonicalizedPublicKey canonicalizedPublicKey = keyRepository.getCanonicalizedPublicKeyRing(masterKeyId)
                                                                     .getPublicKey(authSubKeyId);

        SshPublicKey publicKeyUtils = new SshPublicKey(canonicalizedPublicKey);
        String publicKeyBlob = publicKeyUtils.getEncodedKey();

        String publicKeyBlobExpected = "ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTY"
                + "AAABBBJm2rlv9/8dgVm6VbN9OJDK1pA1Cb7HjJZv+zyiZGbpUrNWN81L1z45mnOfYafAzZMZ9SBy4J954wjp4d/pICIg=";

        Assert.assertEquals("Public key blobs must be equal", publicKeyBlobExpected, publicKeyBlob);

    }
}
