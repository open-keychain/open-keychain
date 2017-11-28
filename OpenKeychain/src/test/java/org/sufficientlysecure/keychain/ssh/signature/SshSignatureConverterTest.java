package org.sufficientlysecure.keychain.ssh.signature;

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
public class SshSignatureConverterTest {

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
