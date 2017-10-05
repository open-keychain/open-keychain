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

package org.sufficientlysecure.keychain.operations;


import org.bouncycastle.bcpg.HashAlgorithmTags;
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
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeyWritableRepository;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ssh.AuthenticationData;
import org.sufficientlysecure.keychain.ssh.AuthenticationOperation;
import org.sufficientlysecure.keychain.ssh.AuthenticationParcel;
import org.sufficientlysecure.keychain.ssh.AuthenticationResult;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.PrintStream;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.util.ArrayList;

@RunWith(KeychainTestRunner.class)
public class AuthenticationOperationTest {

    private static UncachedKeyRing mStaticRing;
    private static Passphrase mKeyPhrase;

    private static PrintStream oldShadowStream;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        oldShadowStream = ShadowLog.stream;
        // ShadowLog.stream = System.out;

        /* keyring generation:
        PgpKeyOperation op = new PgpKeyOperation(null);
        SaveKeyringParcel.Builder builder = SaveKeyringParcel.buildNewKeyringParcel();

        builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER, 0L));
        builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.AUTHENTICATION, 0L));
        builder.addUserId("blah");
        builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(new Passphrase("x")));

        PgpEditKeyResult result = op.createSecretKeyRing(builder.build());
        new FileOutputStream("/tmp/authenticate.sec").write(result.getRing().getEncoded());
        */

        mKeyPhrase = new Passphrase("x");
        mStaticRing = KeyringTestingHelper.readRingFromResource("/test-keys/authenticate.sec");

    }

    @Before
    public void setUp() {
        KeyWritableRepository databaseInteractor =
                KeyWritableRepository.create(RuntimeEnvironment.application);

        // don't log verbosely here, we're not here to test imports
        ShadowLog.stream = oldShadowStream;

        databaseInteractor.saveSecretKeyRing(mStaticRing);

        // ok NOW log verbosely!
        ShadowLog.stream = System.out;
    }

    @Test
    public void testAuthenticate() throws Exception {

        byte[] challenge = "dies ist ein challenge ☭".getBytes();
        byte[] signature;

        KeyRepository keyRepository = KeyRepository.create(RuntimeEnvironment.application);

        long masterKeyId = mStaticRing.getMasterKeyId();
        Long authSubKeyId = keyRepository.getCachedPublicKeyRing(masterKeyId).getSecretAuthenticationId();

        { // sign challenge
            AuthenticationOperation op = new AuthenticationOperation(RuntimeEnvironment.application,
                    keyRepository);

            AuthenticationData.Builder authData = AuthenticationData.builder();
            authData.setAuthenticationMasterKeyId(masterKeyId);
            authData.setAuthenticationSubKeyId(authSubKeyId);
            authData.setHashAlgorithm(HashAlgorithmTags.SHA512);

//            ArrayList<Long> allowedKeyIds = new ArrayList<>(1);
//            allowedKeyIds.add(mStaticRing.getMasterKeyId());
//            authData.setAllowedAuthenticationKeyIds(allowedKeyIds);

            AuthenticationParcel authenticationParcel = AuthenticationParcel
                    .createAuthenticationParcel(authData.build(), challenge);

            CryptoInputParcel inputParcel = CryptoInputParcel.createCryptoInputParcel();
            inputParcel = inputParcel.withPassphrase(mKeyPhrase);

            AuthenticationResult result = op.execute(authData.build(), inputParcel, authenticationParcel);

            Assert.assertTrue("authentication must succeed", result.success());

            signature = result.getSignature();
        }
        { // verify signature
            CanonicalizedPublicKey canonicalizedPublicKey = keyRepository.getCanonicalizedPublicKeyRing(masterKeyId)
                                                                         .getPublicKey(authSubKeyId);
            PublicKey publicKey = canonicalizedPublicKey.getJcaPublicKey();

            Signature signatureVerifier = Signature.getInstance("SHA512withECDSA");
            signatureVerifier.initVerify(publicKey);
            signatureVerifier.update(challenge);
            boolean isSignatureValid = signatureVerifier.verify(signature);

            Assert.assertTrue("signature must be valid", isSignatureValid);
        }
    }

    @Test
    public void testAccessControl() throws Exception {

        byte[] challenge = "dies ist ein challenge ☭".getBytes();

        KeyRepository keyRepository = KeyRepository.create(RuntimeEnvironment.application);

        long masterKeyId = mStaticRing.getMasterKeyId();
        Long authSubKeyId = keyRepository.getCachedPublicKeyRing(masterKeyId).getSecretAuthenticationId();

        { // sign challenge - should succeed with selected key allowed
            AuthenticationOperation op = new AuthenticationOperation(RuntimeEnvironment.application,
                    keyRepository);

            AuthenticationData.Builder authData = AuthenticationData.builder();
            authData.setAuthenticationMasterKeyId(masterKeyId);
            authData.setAuthenticationSubKeyId(authSubKeyId);
            authData.setHashAlgorithm(HashAlgorithmTags.SHA512);

            ArrayList<Long> allowedKeyIds = new ArrayList<>(1);
            allowedKeyIds.add(mStaticRing.getMasterKeyId());
            authData.setAllowedAuthenticationKeyIds(allowedKeyIds);

            AuthenticationParcel authenticationParcel = AuthenticationParcel
                    .createAuthenticationParcel(authData.build(), challenge);

            CryptoInputParcel inputParcel = CryptoInputParcel.createCryptoInputParcel();
            inputParcel = inputParcel.withPassphrase(mKeyPhrase);

            AuthenticationResult result = op.execute(authData.build(), inputParcel, authenticationParcel);

            Assert.assertTrue("authentication must succeed with selected key allowed", result.success());
        }
        { // sign challenge - should fail with selected key disallowed
            AuthenticationOperation op = new AuthenticationOperation(RuntimeEnvironment.application,
                    keyRepository);

            AuthenticationData.Builder authData = AuthenticationData.builder();
            authData.setAuthenticationMasterKeyId(masterKeyId);
            authData.setAuthenticationSubKeyId(authSubKeyId);
            authData.setHashAlgorithm(HashAlgorithmTags.SHA512);

            ArrayList<Long> allowedKeyIds = new ArrayList<>(1);
            authData.setAllowedAuthenticationKeyIds(allowedKeyIds);


            AuthenticationParcel authenticationParcel = AuthenticationParcel
                    .createAuthenticationParcel(authData.build(), challenge);

            CryptoInputParcel inputParcel = CryptoInputParcel.createCryptoInputParcel();
            inputParcel = inputParcel.withPassphrase(mKeyPhrase);

            AuthenticationResult result = op.execute(authData.build(), inputParcel, authenticationParcel);

            Assert.assertFalse("authentication must fail with selected key disallowed", result.success());
        }
    }

}
