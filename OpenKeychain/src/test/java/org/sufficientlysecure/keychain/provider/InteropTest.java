/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.provider;


import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.security.Security;
import java.util.ArrayList;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.openpgp.OpenPgpMetadata;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.daos.KeyWritableRepository;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Passphrase;

@RunWith(KeychainTestRunner.class)
public class InteropTest {

    @BeforeClass
    public static void setUpOnce() {
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

    private static String asString(File json) throws Exception {
        return new String(asBytes(json), "utf-8");
    }

    private static byte[] asBytes(File f) throws Exception {
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
        UncachedKeyRing verify;
        if (config.has("verifyKey")) {
            verify = readUncachedRingFromFile(new File(root, config.getString("verifyKey")));
        } else {
            verify = null;
        }

        UncachedKeyRing decrypt = readUncachedRingFromFile(new File(root, config.getString("decryptKey")));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in =
                new ByteArrayInputStream(asBytes(new File(root, baseName + ".asc")));

        InputData data = new InputData(in, in.available());

        Passphrase pass = new Passphrase(config.getString("passphrase"));

        PgpDecryptVerifyOperation op = makeOperation(base.toString(), pass, decrypt, verify);
        PgpDecryptVerifyInputParcel input = PgpDecryptVerifyInputParcel.builder().build();
        CryptoInputParcel cip = CryptoInputParcel.createCryptoInputParcel(pass);
        DecryptVerifyResult result = op.execute(input, cip, data, out);
        byte[] plaintext = config.getString("textcontent").getBytes("utf-8");
        String filename = config.getString("filename");
        Assert.assertTrue(base + ": decryption must succeed", result.success());
        byte[] decrypted = out.toByteArray();
        Assert.assertArrayEquals(base + ": plaintext should be correct", decrypted, plaintext);
        if (verify != null) {
            // Certain keys are too short, so we check appropriately.
            int code = result.getSignatureResult().getResult();
            Assert.assertTrue(base + ": should have a signature (code: " + code + ")",
                    code == OpenPgpSignatureResult.RESULT_INVALID_KEY_INSECURE ||
                            code == OpenPgpSignatureResult.RESULT_VALID_KEY_UNCONFIRMED
                            || code == OpenPgpSignatureResult.RESULT_VALID_KEY_CONFIRMED);
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
        CanonicalizedKeyRing pkr = readRingFromFile(new File(root, baseName + ".asc"));

        // Check we have the correct uids.
        ArrayList<String> expected = new ArrayList<>();
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
        ArrayList<String> actual = new ArrayList<>();
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

    private UncachedKeyRing readUncachedRingFromFile(File path) throws Exception {
        BufferedInputStream bin = null;
        try {
            bin = new BufferedInputStream(new FileInputStream(path));
            return UncachedKeyRing.fromStream(bin).next();
        } finally {
            close(bin);
        }
    }

    private CanonicalizedKeyRing readRingFromFile(File path) throws Exception {
        UncachedKeyRing ukr = readUncachedRingFromFile(path);
        OperationLog log = new OperationLog();
        return ukr.canonicalize(log, 0);
    }

    private  static void close(Closeable v) {
        if (v != null) {
            try {
                v.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static String getBaseName(File base) {
        String name = base.getName();
        return name.substring(0, name.length() - ".json".length());
    }

    private PgpDecryptVerifyOperation makeOperation(final String msg, final Passphrase passphrase,
            UncachedKeyRing decrypt, UncachedKeyRing verify) {
        KeyWritableRepository keyRepository = KeyWritableRepository.create(RuntimeEnvironment.application);

        Assert.assertTrue(keyRepository.saveSecretKeyRing(decrypt).success());
        if (verify != null) {
            Assert.assertTrue(keyRepository.savePublicKeyRing(verify).success());
        }

        return new PgpDecryptVerifyOperation(RuntimeEnvironment.application, keyRepository, null) {
            @Override
            public Passphrase getCachedPassphrase(long masterKeyId, long subKeyId) {
                Assert.assertEquals(msg + ": passphrase should be for the secret key",
                        masterKeyId, decrypt.getMasterKeyId());
                return passphrase;
            }
        };
    }
}
