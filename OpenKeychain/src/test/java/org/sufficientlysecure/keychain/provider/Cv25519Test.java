/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import android.app.Application;
import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.Util;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.daos.KeyWritableRepository;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.PgpSignEncryptResult;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyOperation;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptData;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptOperation;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;


@SuppressWarnings("WeakerAccess")
@RunWith(KeychainTestRunner.class)
public class Cv25519Test {
    public static final byte[] ENCRYPTED_PLAINTEXT = "hi\n".getBytes();
    private KeyWritableRepository keyRepository;
    private Application context;


    @BeforeClass
    public static void setUpOnce() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.application;
        keyRepository = KeyWritableRepository.create(context);

    }

    @Test
    public void testDecryptX25519() throws Exception {
        loadSecretKeyringFromResource("/test-keys/cv25519-key.sec.asc");

        byte[] encryptedText = readBytesFromResource("/test-keys/cv25519-encrypted.asc");
        DecryptVerifyResult result = simpleDecryptText(encryptedText);

        assertArrayEquals(ENCRYPTED_PLAINTEXT, result.getOutputBytes());
    }

    @Test
    public void testEncryptX25519() throws Exception {
        UncachedKeyRing uncachedKeyRing = loadSecretKeyringFromResource("/test-keys/cv25519-key.sec.asc");

        PgpSignEncryptData data = PgpSignEncryptData.builder()
                .setEncryptionMasterKeyIds(new long[] { uncachedKeyRing.getMasterKeyId() })
                .build();
        PgpSignEncryptInputParcel inputParcel = PgpSignEncryptInputParcel.createForBytes(
                data, null, ENCRYPTED_PLAINTEXT);

        PgpSignEncryptOperation op = new PgpSignEncryptOperation(context, keyRepository, null);
        PgpSignEncryptResult result = op.execute(inputParcel, CryptoInputParcel.createCryptoInputParcel());

        assertTrue(result.success());

        DecryptVerifyResult decryptResult = simpleDecryptText(result.getOutputBytes());

        assertTrue(decryptResult.success());
        assertArrayEquals(ENCRYPTED_PLAINTEXT, decryptResult.getOutputBytes());
    }

    private UncachedKeyRing loadSecretKeyringFromResource(String name) throws Exception {
        UncachedKeyRing ring = readRingFromResource(name);
        SaveKeyringResult saveKeyringResult = keyRepository.saveSecretKeyRing(ring);
        assertTrue(saveKeyringResult.success());
        assertFalse(saveKeyringResult.getLog().containsWarnings());
        return ring;
    }

    @NonNull
    private DecryptVerifyResult simpleDecryptText(byte[] encryptedText) {
        PgpDecryptVerifyInputParcel pgpDecryptVerifyInputParcel =
                PgpDecryptVerifyInputParcel.builder().setInputBytes(encryptedText).build();

        PgpDecryptVerifyOperation decryptVerifyOperation = new PgpDecryptVerifyOperation(context, keyRepository, null);
        DecryptVerifyResult result = decryptVerifyOperation.execute(pgpDecryptVerifyInputParcel, CryptoInputParcel.createCryptoInputParcel());

        assertTrue(result.success());
        assertEquals(OpenPgpSignatureResult.RESULT_NO_SIGNATURE, result.getSignatureResult().getResult());
        return result;
    }

    private byte[] readBytesFromResource(String name) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream input = Cv25519Test.class.getResourceAsStream(name);

        Util.copy(input, baos);

        return baos.toByteArray();
    }

    UncachedKeyRing readRingFromResource(String name) throws Exception {
        return UncachedKeyRing.fromStream(Cv25519Test.class.getResourceAsStream(name)).next();
    }

}