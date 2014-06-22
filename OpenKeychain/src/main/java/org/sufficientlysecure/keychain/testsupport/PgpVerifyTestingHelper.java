package org.sufficientlysecure.keychain.testsupport;

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
