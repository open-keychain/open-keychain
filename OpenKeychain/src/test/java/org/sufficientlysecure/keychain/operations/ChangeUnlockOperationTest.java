package org.sufficientlysecure.keychain.operations;

import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderReader;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.util.KeyringPassphrases;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ProgressScaler;
import org.sufficientlysecure.keychain.util.TestingUtils;

import java.io.PrintStream;
import java.security.Security;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 21, manifest = "src/main/AndroidManifest.xml")
public class ChangeUnlockOperationTest {

    static Passphrase mPassphrase = TestingUtils.genPassphrase(true);
    static UncachedKeyRing mStaticRing;

    static PrintStream oldShadowStream;

    @BeforeClass
    public static void setup() throws Exception {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        oldShadowStream = ShadowLog.stream;

        // ShadowLog.stream = System.out;

        PgpKeyOperation op = new PgpKeyOperation(null);

        {
            SaveKeyringParcel parcel = new SaveKeyringParcel();
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    SaveKeyringParcel.Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER, 0L));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    SaveKeyringParcel.Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.SIGN_DATA, 0L));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    SaveKeyringParcel.Algorithm.ECDH, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.ENCRYPT_COMMS, 0L));
            parcel.mAddUserIds.add("snips");

            PgpEditKeyResult result = op.createSecretKeyRing(parcel);
            assertTrue("initial test key creation must succeed", result.success());
            Assert.assertNotNull("initial test key creation must succeed", result.getRing());

            mStaticRing = result.getRing();
        }
    }

    @Before
    public void setUp() {
        ProviderHelper providerHelper = new ProviderHelper(RuntimeEnvironment.application);

        // don't log verbosely yet, we're not here to test imports
        ShadowLog.stream = oldShadowStream;

        providerHelper.mWriter.saveSecretKeyRing(mStaticRing,
                new KeyringPassphrases(mStaticRing.getMasterKeyId(), mPassphrase), new ProgressScaler());
        // ok NOW log verbosely!
        ShadowLog.stream = System.out;
    }


    @Test
    public void testChangeUnlockEmpty() {
        // non-empty to empty
        testChangeUnlockHelper(mPassphrase, new Passphrase());
        // empty to non-empty
        testChangeUnlockHelper(new Passphrase(), TestingUtils.genPassphrase(true));
    }

    @Test
    public void testChangeUnlockNonEmpty() {
        // non-empty to non-empty
        testChangeUnlockHelper(mPassphrase, TestingUtils.genPassphrase(true));
    }

    public void testChangeUnlockHelper(Passphrase currentPasphrase, Passphrase newPassphrase) {
        ProviderHelper providerHelper = new ProviderHelper(RuntimeEnvironment.application);
        ChangeUnlockOperation op = new ChangeUnlockOperation(RuntimeEnvironment.application,
                providerHelper, new ProgressScaler());

        OperationResult result = op.execute(
                new ChangeUnlockParcel(
                        mStaticRing.getMasterKeyId(),
                        mStaticRing.getFingerprint(),
                        newPassphrase),
                new CryptoInputParcel(currentPasphrase)
        );
        assertTrue("change unlock should succeed", result.success());

        try {
            providerHelper.mReader.getCanonicalizedSecretKeyRingWithMerge(mStaticRing.getMasterKeyId(), newPassphrase);
        } catch (ProviderReader.NotFoundException | ByteArrayEncryptor.EncryptDecryptException e) {
            Assert.fail("IO error when retrieving key!");
        } catch (ByteArrayEncryptor.IncorrectPassphraseException e){
            Assert.fail("Keyring has an unknown passphrase!");
        } catch (ProviderReader.FailedMergeException e) {
            Assert.fail("merge should not fail (if any)");
        }
    }
}
