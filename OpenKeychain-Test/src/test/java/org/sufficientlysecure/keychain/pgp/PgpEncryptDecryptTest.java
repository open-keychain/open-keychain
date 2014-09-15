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

package org.sufficientlysecure.keychain.pgp;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerify.NoSecretKeyException;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerify.PassphraseCache;
import org.sufficientlysecure.keychain.pgp.PgpSignEncrypt.Builder;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.service.results.SignEncryptResult;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.TestingUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Security;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class PgpEncryptDecryptTest {

    String mPassphrase = TestingUtils.genPassphrase(true);

    @BeforeClass
    public static void setUpOnce() throws Exception {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        ShadowLog.stream = System.out;
    }

    @Test
    public void testSymmetricEncryptDecrypt() {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        {
            ByteArrayInputStream in = new ByteArrayInputStream("dies ist ein plaintext ☭".getBytes());

            InputData data = new InputData(in, in.available());
            Builder b = new PgpSignEncrypt.Builder(new ProviderHelper(Robolectric.application), data, out);

            b.setSymmetricPassphrase(mPassphrase);
            b.setSymmetricEncryptionAlgorithm(PGPEncryptedData.AES_128);
            SignEncryptResult result = b.build().execute();
            Assert.assertTrue("encryption must succeed", result.success());
        }

        {
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            InputData data = new InputData(in, in.available());

            out.reset();

            PgpDecryptVerify.Builder b = new PgpDecryptVerify.Builder(
                    new ProviderHelper(Robolectric.application),
                    new DummyPassphraseCache(mPassphrase), data, out);
            b.setPassphrase(mPassphrase);
            DecryptVerifyResult result = b.build().execute();
            Assert.assertTrue("decryption must succeed", result.success());

        }

    }

    static class DummyPassphraseCache implements PassphraseCache {

        String mPassphrase;
        public DummyPassphraseCache(String passphrase) {
            mPassphrase = passphrase;
        }

        @Override
        public String getCachedPassphrase(long masterKeyId) throws NoSecretKeyException {
            Assert.assertEquals("requested passphrase must be for symmetric id", 0L, masterKeyId);
            return mPassphrase;
        }
    }

}
