package org.sufficientlysecure.keychain.support;
/*
 * Copyright (C) Art O Cathain
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

import android.content.Context;

import org.sufficientlysecure.keychain.pgp.PgpDecryptVerify;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyResult;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.InputData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * For functional tests of PgpDecryptVerify
 */
public class PgpVerifyTestingHelper {

    private final Context context;

    public PgpVerifyTestingHelper(Context robolectricContext) {
        this.context = robolectricContext;
    }

    public int doTestFile(String testFileName) throws Exception {
        ProviderHelper providerHelper = new ProviderHelperStub(context);

        PgpDecryptVerify.PassphraseCache passphraseCache = new PgpDecryptVerify.PassphraseCache() {
            public String getCachedPassphrase(long masterKeyId) {
                return "I am a passphrase";
            }
        };

        byte[] sampleInputBytes = TestDataUtil.readFully(getClass().getResourceAsStream(testFileName));

        InputStream sampleInput = new ByteArrayInputStream(sampleInputBytes);

        InputData data = new InputData(sampleInput, sampleInputBytes.length);
        OutputStream outStream = new ByteArrayOutputStream();

        PgpDecryptVerify verify = new PgpDecryptVerify.Builder(providerHelper, passphraseCache, data, outStream).build();
        PgpDecryptVerifyResult result = verify.execute();

        return result.getSignatureResult().getStatus();
    }


}
