package org.sufficientlysecure.keychain.testsupport;

import android.content.Context;
import android.net.Uri;

import org.sufficientlysecure.keychain.pgp.PgpDecryptVerify;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyResult;
import org.sufficientlysecure.keychain.pgp.WrappedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.InputData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * For functional tests of PgpDecryptVerify
 */
public class PgpVerifyTestingHelper {

    private final Context context;

    public PgpVerifyTestingHelper(Context robolectricContext) {
            this.context=robolectricContext;
    }

    public int doTestFile(String testFileName) throws Exception {
        ProviderHelper providerHelper = new ProviderHelperStub(context);

        PgpDecryptVerify.PassphraseCache passphraseCache = new PgpDecryptVerify.PassphraseCache() {
            public String getCachedPassphrase(long masterKeyId) {
                return "I am a passphrase";
            }
        };

        byte[] sampleInputBytes = readFully(getClass().getResourceAsStream(testFileName));

        InputStream sampleInput = new ByteArrayInputStream(sampleInputBytes);

        InputData data = new InputData(sampleInput, sampleInputBytes.length);
        OutputStream outStream = new ByteArrayOutputStream();

        PgpDecryptVerify verify = new PgpDecryptVerify.Builder(providerHelper, passphraseCache, data, outStream).build();
        PgpDecryptVerifyResult result = verify.execute();

        return result.getSignatureResult().getStatus();
    }


    static class ProviderHelperStub extends ProviderHelper {
        public ProviderHelperStub(Context context) {
            super(context);
        }

        @Override
        public WrappedPublicKeyRing getWrappedPublicKeyRing(Uri id) throws NotFoundException {
            try {
                byte[] data = readFully(getClass().getResourceAsStream("/public-key-for-sample.blob"));
                return new WrappedPublicKeyRing(data, false, 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static byte[] readFully(InputStream input) throws IOException
    {
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesRead = input.read(buffer)) != -1)
        {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }




}
