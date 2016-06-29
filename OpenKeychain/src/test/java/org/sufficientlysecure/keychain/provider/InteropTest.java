/*
 * Copyright (C) 2016 Google Inc.
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

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.openpgp.OpenPgpMetadata;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.security.Security;
import java.util.ArrayList;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 23, manifest = "src/main/AndroidManifest.xml")
public class InteropTest {
    // TODO: wip
    /*
    @BeforeClass
    public static void setUpOnce() throws Exception {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        ShadowLog.stream = System.out;
    }

    @Test
    public void testInterop() throws Exception {
        URL baseURL = InteropTest.class.getResource("/openpgp-interop/testcases");
        Assert.assertNotNull(baseURL);
        File baseFile = new File(baseURL.toURI());
        walkTests(baseFile);
    }

    private void walkTests(File root) throws Exception {
        File children[] = root.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.getName().startsWith(".")) {
                continue;
            }
            if (child.isDirectory()) {
                walkTests(child);
            } else if (child.getName().endsWith(".json")) {
                runTest(child);
            }
        }
    }

    private void runTest(File base) throws Exception {
        JSONObject config = new JSONObject(asString(base));
        String testType = config.getString("type");
        if (testType.equals("import")) {
            runImportTest(config, base);
        } else if (testType.equals("decrypt")) {
            runDecryptTest(config, base);
        } else {
            Assert.fail(base + ": unexpected test type");
        }
    }

    private static final String asString(File json) throws Exception {
        return new String(asBytes(json), "utf-8");
    }

    private static final byte[] asBytes(File f) throws Exception {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(f);
            byte data[] = new byte[fin.available()];
            fin.read(data);
            return data;
        } finally {
            close(fin);
        }
    }

    private void runDecryptTest(JSONObject config, File base) throws Exception {
        File root = base.getParentFile();
        String baseName = getBaseName(base);
        CanonicalizedPublicKeyRing verify;
        if (config.has("verifyKey")) {
            verify = (CanonicalizedPublicKeyRing)
                    readRingFromFile(new File(root, config.getString("verifyKey")));
        } else {
            verify = null;
        }

        CanonicalizedSecretKeyRing decrypt = (CanonicalizedSecretKeyRing)
                readRingFromFile(new File(root, config.getString("decryptKey")));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in =
                new ByteArrayInputStream(asBytes(new File(root, baseName + ".asc")));

        InputData data = new InputData(in, in.available());

        Passphrase pass = new Passphrase(config.getString("passphrase"));

        PgpDecryptVerifyOperation op = makeOperation(base.toString(), pass, decrypt, verify);
        PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel();
        CryptoInputParcel cip = new CryptoInputParcel(pass);
        DecryptVerifyResult result = op.execute(input, cip, data, out);
        byte[] plaintext = config.getString("textcontent").getBytes("utf-8");
        String filename = config.getString("filename");
        Assert.assertTrue(base + ": decryption must succeed", result.success());
        byte[] decrypted = out.toByteArray();
        Assert.assertArrayEquals(base + ": plaintext should be correct", decrypted, plaintext);
        if (verify != null) {
            // Certain keys are too short, so we check appropriately.
            int code = result.getSignatureResult().getResult();
            Assert.assertTrue(base + ": should have a signature",
                    (code == OpenPgpSignatureResult.RESULT_INVALID_INSECURE) ||
                    (code == OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED));
        }
        OpenPgpMetadata metadata = result.getDecryptionMetadata();
        Assert.assertEquals(base + ": filesize must be correct",
                decrypted.length, metadata.getOriginalSize());
        Assert.assertEquals(base + ": filename must be correct",
                filename, metadata.getFilename());
    }

    private void runImportTest(JSONObject config, File base) throws Exception {
        File root = base.getParentFile();
        String baseName = getBaseName(base);
        CanonicalizedKeyRing pkr =
                readRingFromFile(new File(root, baseName + ".asc"));

        // Check we have the correct uids.
        ArrayList<String> expected = new ArrayList<String>();
        JSONArray uids = config.getJSONArray("expected_uids");
        for (int i = 0; i < uids.length(); i++) {
            expected.add(uids.getString(i));
        }
        check(base + ": incorrect uids", expected, pkr.getUnorderedUserIds());

        // Check we have the correct main and subkey fingerprints.
        expected.clear();
        expected.add(config.getString("expected_fingerprint"));
        JSONArray subkeys = config.optJSONArray("expected_subkeys");
        if (subkeys != null) {
            for (int i = 0; i < subkeys.length(); i++) {
                expected.add(subkeys.getJSONObject(i).getString("expected_fingerprint"));
            }
        }
        ArrayList<String> actual = new ArrayList<String>();
        for (CanonicalizedPublicKey pk: pkr.publicKeyIterator()) {
            if (pk.isValid()) {
                actual.add(KeyFormattingUtils.convertFingerprintToHex(pk.getFingerprint()));
            }
        }
        check(base + ": incorrect fingerprints", expected, actual);
    }

    private void check(String msg, ArrayList<String> a, ArrayList<String> b) {
        Assert.assertEquals(msg, a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            Assert.assertEquals(msg, a.get(i), b.get(i));
        }
    }

    UncachedKeyRing readUncachedRingFromFile(File path) throws Exception {
        BufferedInputStream bin = null;
        try {
            bin = new BufferedInputStream(new FileInputStream(path));
            return UncachedKeyRing.fromStream(bin).next();
        } finally {
            close(bin);
        }
    }

    CanonicalizedKeyRing readRingFromFile(File path) throws Exception {
        UncachedKeyRing ukr = readUncachedRingFromFile(path);
        OperationLog log = new OperationLog();
        return ukr.canonicalize(log, 0);
    }

    private  static final void close(Closeable v) {
        if (v != null) {
            try {
                v.close();
            } catch (Throwable any) {
            }
        }
    }

    private static final String getBaseName(File base) {
        String name = base.getName();
        return name.substring(0, name.length() - ".json".length());
    }

    private PgpDecryptVerifyOperation makeOperation(final String msg, final Passphrase passphrase,
            final CanonicalizedSecretKeyRing decrypt, final CanonicalizedPublicKeyRing verify)
            throws Exception {

        final long decryptId = decrypt.getEncryptId();
        final Uri decryptUri = KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(decryptId);
        final Uri verifyUri =  verify != null ?
                KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(verify.getMasterKeyId()) : null;

        ProviderHelper helper = new ProviderHelper(RuntimeEnvironment.application) {

            @Override
            public CachedPublicKeyRing getCachedPublicKeyRing(Uri queryUri) throws PgpKeyNotFoundException {
                Assert.assertEquals(msg + ": query should be for the decryption key", queryUri, decryptUri);
                return new CachedPublicKeyRing(this, queryUri) {
                    @Override
                    public long getMasterKeyId() throws PgpKeyNotFoundException {
                        return decrypt.getMasterKeyId();
                    }

                    @Override
                    public SecretKeyType getSecretKeyType(long keyId) throws NotFoundException {
                        return decrypt.getSecretKey(keyId).getSecretKeyTypeSuperExpensive();
                    }
                };
            }

            @Override
            public CanonicalizedPublicKeyRing getCanonicalizedPublicKeyRing(Uri q)
                    throws NotFoundException {
                Assert.assertEquals(msg + ": query should be for verification key", q, verifyUri);
                return verify;
            }

            // TODO: wip
            @Override
            public CanonicalizedSecretKeyRing getCanonicalizedSecretKeyRing(Uri q)
                    throws NotFoundException {
                Assert.assertEquals(msg + ": query should be for the decryption key", q, decryptUri);
                return decrypt;
            }

            // TODO: wip
            @Override
            public CanonicalizedSecretKeyRing getCanonicalizedSecretKeyRing(long masterKeyId)
                    throws NotFoundException {
                Assert.assertEquals(msg + ": query should be for the decryption key",
                        masterKeyId, decrypt.getMasterKeyId());
                return decrypt;
            }
        };

        return new PgpDecryptVerifyOperation(RuntimeEnvironment.application, helper, null) {
            @Override
            public Passphrase getCachedPassphrase(long masterKeyId, long subKeyId)
                    throws NoSecretKeyException {
                Assert.assertEquals(msg + ": passphrase should be for the secret key",
                        masterKeyId, decrypt.getMasterKeyId());
                Assert.assertEquals(msg + ": passphrase should refer to the decryption subkey",
                        subKeyId, decryptId);
                return passphrase;
            }
        };
    }
    */
}
