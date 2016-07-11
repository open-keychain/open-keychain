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

package org.sufficientlysecure.keychain.operations;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.security.Security;
import java.util.Iterator;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

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
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyOperation;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing.IteratorWithIOThrow;
import org.sufficientlysecure.keychain.pgp.WrappedSignature;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.BackupKeyringParcel;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.TestingUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 23, manifest = "src/main/AndroidManifest.xml")
public class BackupOperationTest {

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
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER, 0L));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.SIGN_DATA, 0L));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.ECDH, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.ENCRYPT_COMMS, 0L));
            parcel.mAddUserIds.add("snips");
            parcel.mPassphrase = mKeyPhrase1;

            PgpEditKeyResult result = op.createSecretKeyRing(parcel);
            assertTrue("initial test key creation must succeed", result.success());
            Assert.assertNotNull("initial test key creation must succeed", result.getRing());

            mStaticRing1 = result.getRing();
        }

        {
            SaveKeyringParcel parcel = new SaveKeyringParcel();
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER, 0L));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.SIGN_DATA, 0L));
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    Algorithm.ECDH, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.ENCRYPT_COMMS, 0L));
            parcel.mAddUserIds.add("snails");
            parcel.mPassphrase = new Passphrase("1234");

            PgpEditKeyResult result = op.createSecretKeyRing(parcel);
            assertTrue("initial test key creation must succeed", result.success());
            Assert.assertNotNull("initial test key creation must succeed", result.getRing());

            mStaticRing2 = result.getRing();
            mStaticRing2 = UncachedKeyRing.forTestingOnlyAddDummyLocalSignature(mStaticRing2, "1234");
        }

    }

    @Before
    public void setUp() {
        ProviderHelper providerHelper = new ProviderHelper(RuntimeEnvironment.application);

        // don't log verbosely here, we're not here to test imports
        ShadowLog.stream = oldShadowStream;

        providerHelper.saveSecretKeyRingForTest(mStaticRing1);
        providerHelper.saveSecretKeyRingForTest(mStaticRing2);

        // ok NOW log verbosely!
        ShadowLog.stream = System.out;
    }

    @Test
    public void testExportAllLocalStripped() throws Exception {
        BackupOperation op = new BackupOperation(RuntimeEnvironment.application,
                new ProviderHelper(RuntimeEnvironment.application), null);

        // make sure there is a local cert (so the later checks that there are none are meaningful)
        assertTrue("second keyring has local certification", checkForLocal(mStaticRing2));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean result = op.exportKeysToStream(new OperationLog(), null, false, out);

        assertTrue("export must be a success", result);

        long masterKeyId1, masterKeyId2;
        if (mStaticRing1.getMasterKeyId() < mStaticRing2.getMasterKeyId()) {
            masterKeyId1 = mStaticRing1.getMasterKeyId();
            masterKeyId2 = mStaticRing2.getMasterKeyId();
        } else {
            masterKeyId2 = mStaticRing1.getMasterKeyId();
            masterKeyId1 = mStaticRing2.getMasterKeyId();
        }

        IteratorWithIOThrow<UncachedKeyRing> unc =
                UncachedKeyRing.fromStream(new ByteArrayInputStream(out.toByteArray()));

        {
            assertTrue("export must have two keys (1/2)", unc.hasNext());
            UncachedKeyRing ring = unc.next();
            Assert.assertEquals("first exported key has correct masterkeyid",
                    masterKeyId1, ring.getMasterKeyId());
            assertFalse("first exported key must not be secret", ring.isSecret());
            assertFalse("there must be no local signatures in an exported keyring",
                    checkForLocal(ring));
        }

        {
            assertTrue("export must have two keys (2/2)", unc.hasNext());
            UncachedKeyRing ring = unc.next();
            Assert.assertEquals("second exported key has correct masterkeyid",
                    masterKeyId2, ring.getMasterKeyId());
            assertFalse("second exported key must not be secret", ring.isSecret());
            assertFalse("there must be no local signatures in an exported keyring",
                    checkForLocal(ring));
        }

        out = new ByteArrayOutputStream();
        result = op.exportKeysToStream(new OperationLog(), null, true, out);

        assertTrue("export must be a success", result);

        unc = UncachedKeyRing.fromStream(new ByteArrayInputStream(out.toByteArray()));

        {
            assertTrue("export must have four keys (1/4)", unc.hasNext());
            UncachedKeyRing ring = unc.next();
            Assert.assertEquals("1/4 exported key has correct masterkeyid",
                    masterKeyId1, ring.getMasterKeyId());
            assertFalse("1/4 exported key must not be public", ring.isSecret());
            assertFalse("there must be no local signatures in an exported keyring",
                    checkForLocal(ring));

            assertTrue("export must have four keys (2/4)", unc.hasNext());
            ring = unc.next();
            Assert.assertEquals("2/4 exported key has correct masterkeyid",
                    masterKeyId1, ring.getMasterKeyId());
            assertTrue("2/4 exported key must be public", ring.isSecret());
            assertFalse("there must be no local signatures in an exported keyring",
                    checkForLocal(ring));
        }

        {
            assertTrue("export must have four keys (3/4)", unc.hasNext());
            UncachedKeyRing ring = unc.next();
            Assert.assertEquals("3/4 exported key has correct masterkeyid",
                    masterKeyId2, ring.getMasterKeyId());
            assertFalse("3/4 exported key must not be public", ring.isSecret());
            assertFalse("there must be no local signatures in an exported keyring",
                    checkForLocal(ring));

            assertTrue("export must have four keys (4/4)", unc.hasNext());
            ring = unc.next();
            Assert.assertEquals("4/4 exported key has correct masterkeyid",
                    masterKeyId2, ring.getMasterKeyId());
            assertTrue("4/4 exported key must be public", ring.isSecret());
            assertFalse("there must be no local signatures in an exported keyring",
                    checkForLocal(ring));
        }

    }

    @Test
    public void testExportUnencrypted() throws Exception {
        ContentResolver mockResolver = mock(ContentResolver.class);

        Uri fakeOutputUri = Uri.parse("content://fake/out/1");
        ByteArrayOutputStream outStream1 = new ByteArrayOutputStream();
        when(mockResolver.openOutputStream(fakeOutputUri)).thenReturn(outStream1);

        Application spyApplication = spy(RuntimeEnvironment.application);
        when(spyApplication.getContentResolver()).thenReturn(mockResolver);

        BackupOperation op = new BackupOperation(spyApplication,
                new ProviderHelper(RuntimeEnvironment.application), null);

        BackupKeyringParcel parcel = new BackupKeyringParcel(
                new long[] { mStaticRing1.getMasterKeyId() }, false, false, fakeOutputUri);

        ExportResult result = op.execute(parcel, null);

        verify(mockResolver).openOutputStream(fakeOutputUri);

        assertTrue("export must succeed", result.success());

        TestingUtils.assertArrayEqualsPrefix("exported data must start with ascii armor header",
                "-----BEGIN PGP PUBLIC KEY BLOCK-----\n".getBytes(), outStream1.toByteArray());
        TestingUtils.assertArrayEqualsSuffix("exported data must end with ascii armor header",
                "-----END PGP PUBLIC KEY BLOCK-----\n".getBytes(), outStream1.toByteArray());

        {
            IteratorWithIOThrow<UncachedKeyRing> unc
                    = UncachedKeyRing.fromStream(new ByteArrayInputStream(outStream1.toByteArray()));

            assertTrue("export must have one key", unc.hasNext());
            UncachedKeyRing ring = unc.next();
            Assert.assertEquals("exported key has correct masterkeyid",
                    mStaticRing1.getMasterKeyId(), ring.getMasterKeyId());
            assertFalse("export must have exactly one key", unc.hasNext());
        }

    }

    @Test
    public void testExportEncrypted() throws Exception {
        Application spyApplication;
        ContentResolver mockResolver = mock(ContentResolver.class);

        Uri fakePipedUri, fakeOutputUri;
        ByteArrayOutputStream outStream; {

            fakePipedUri = Uri.parse("content://fake/pipe/1");
            PipedInputStream pipedInStream = new PipedInputStream(8192);
            PipedOutputStream pipedOutStream = new PipedOutputStream(pipedInStream);
            when(mockResolver.openOutputStream(fakePipedUri)).thenReturn(pipedOutStream);
            when(mockResolver.openInputStream(fakePipedUri)).thenReturn(pipedInStream);
            when(mockResolver.insert(eq(TemporaryFileProvider.CONTENT_URI), any(ContentValues.class)))
                    .thenReturn(fakePipedUri);

            fakeOutputUri = Uri.parse("content://fake/out/1");
            outStream = new ByteArrayOutputStream();
            when(mockResolver.openOutputStream(fakeOutputUri)).thenReturn(outStream);

            spyApplication = spy(RuntimeEnvironment.application);
            when(spyApplication.getContentResolver()).thenReturn(mockResolver);
        }

        Passphrase passphrase = new Passphrase("abcde");

        { // export encrypted
            BackupOperation op = new BackupOperation(spyApplication,
                    new ProviderHelper(RuntimeEnvironment.application), null);

            BackupKeyringParcel parcel = new BackupKeyringParcel(
                    new long[] { mStaticRing1.getMasterKeyId() }, false, true, fakeOutputUri);
            CryptoInputParcel inputParcel = new CryptoInputParcel(passphrase);
            ExportResult result = op.execute(parcel, inputParcel);

            verify(mockResolver).openOutputStream(fakePipedUri);
            verify(mockResolver).openInputStream(fakePipedUri);
            verify(mockResolver).openOutputStream(fakeOutputUri);

            assertTrue("export must succeed", result.success());
            TestingUtils.assertArrayEqualsPrefix("exported data must start with ascii armor header",
                    "-----BEGIN PGP MESSAGE-----\n".getBytes(), outStream.toByteArray());
        }

        {
            PgpDecryptVerifyOperation op = new PgpDecryptVerifyOperation(RuntimeEnvironment.application,
                    new ProviderHelper(RuntimeEnvironment.application), null);

            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel(outStream.toByteArray());
            input.setAllowSymmetricDecryption(true);

            {
                DecryptVerifyResult result = op.execute(input, new CryptoInputParcel());
                assertTrue("decryption must return pending without passphrase", result.isPending());
                Assert.assertTrue("should contain pending passphrase log entry",
                        result.getLog().containsType(LogType.MSG_DC_PENDING_PASSPHRASE));
            }
            {
                DecryptVerifyResult result = op.execute(input, new CryptoInputParcel(new Passphrase("bad")));
                assertFalse("decryption must fail with bad passphrase", result.success());
                Assert.assertTrue("should contain bad passphrase log entry",
                        result.getLog().containsType(LogType.MSG_DC_ERROR_SYM_PASSPHRASE));
            }

            DecryptVerifyResult result = op.execute(input, new CryptoInputParcel(passphrase));
            assertTrue("decryption must succeed with passphrase", result.success());

            assertEquals("backup filename should be backup_keyid.pub.asc",
                    "backup_" + KeyFormattingUtils.convertKeyIdToHex(mStaticRing1.getMasterKeyId()) + ".pub.asc",
                    result.getDecryptionMetadata().getFilename());

            assertEquals("mime type for pgp keys must be correctly detected",
                    "application/pgp-keys", result.getDecryptionMetadata().getMimeType());

            TestingUtils.assertArrayEqualsPrefix("exported data must start with ascii armor header",
                    "-----BEGIN PGP PUBLIC KEY BLOCK-----\n".getBytes(), result.getOutputBytes());
            TestingUtils.assertArrayEqualsSuffix("exported data must end with ascii armor header",
                    "-----END PGP PUBLIC KEY BLOCK-----\n".getBytes(), result.getOutputBytes());

            {
                IteratorWithIOThrow<UncachedKeyRing> unc
                        = UncachedKeyRing.fromStream(new ByteArrayInputStream(result.getOutputBytes()));

                assertTrue("export must have one key", unc.hasNext());
                UncachedKeyRing ring = unc.next();
                Assert.assertEquals("exported key has correct masterkeyid",
                        mStaticRing1.getMasterKeyId(), ring.getMasterKeyId());
                assertFalse("export must have exactly one key", unc.hasNext());
            }

        }

    }


    /** This function checks whether or not there are any local signatures in a keyring. */
    private boolean checkForLocal(UncachedKeyRing ring) {
        Iterator<WrappedSignature> sigs = ring.getPublicKey().getSignatures();
        while (sigs.hasNext()) {
            if (sigs.next().isLocal()) {
                return true;
            }
        }
        return false;
    }

}
