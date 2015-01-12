package org.sufficientlysecure.keychain.pgp.affirmation.resources;

import android.content.Context;

import com.textuality.keybase.lib.Search;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerify;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.affirmation.AffirmationResource;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

public class GenericHttpsResource extends AffirmationResource {

    GenericHttpsResource(Set<String> flags, HashMap<String,String> params, URI uri) {
        super(flags, params, uri);
    }

    @Override
    public boolean verify() {
        return false;
    }

    public static String generate (byte[] fingerprint, String uri) {
        long nonce = generateNonce();

        StringBuilder b = new StringBuilder();
        b.append("---\r\n");

        b.append("fingerprint=");
        b.append(KeyFormattingUtils.convertFingerprintToHex(fingerprint));
        b.append('\r').append('\n');

        b.append("nonce=");
        b.append(nonce);
        b.append('\r').append('\n');

        if (uri != null) {
            b.append("uri=");
            b.append(uri);
            b.append('\r').append('\n');
        }
        b.append("---\r\n");

        return b.toString();
    }

    public DecryptVerifyResult verify
            (Context context, ProviderHelper providerHelper, Progressable progress)
            throws IOException {

        byte[] data = fetchResource(mUri).getBytes();
        InputData input = new InputData(new ByteArrayInputStream(data), data.length);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PgpDecryptVerify.Builder b =
                new PgpDecryptVerify.Builder(context, providerHelper, progress, input, out);
        PgpDecryptVerify op = b.build();

        Log.d(Constants.TAG, new String(out.toByteArray()));

        return op.execute();
    }

    protected static String fetchResource (URI uri) throws IOException {

        try {
            HttpsURLConnection conn = null;
            URL url = uri.toURL();
            int status = 0;
            int redirects = 0;
            while (redirects < 5) {
                conn = (HttpsURLConnection) url.openConnection();
                conn.addRequestProperty("User-Agent", "OpenKeychain");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(25000);
                conn.connect();
                status = conn.getResponseCode();
                if (status == 301) {
                    redirects++;
                    url = new URL(conn.getHeaderFields().get("Location").get(0));
                } else {
                    break;
                }
            }
            if (status >= 200 && status < 300) {
                return Search.snarf(conn.getInputStream());
            } else {
                throw new IOException("Fetch failed, status " + status + ": " + Search.snarf(conn.getErrorStream()));
            }

        } catch (MalformedURLException e) {
            throw new IOException(e);
        }

    }

    public static GenericHttpsResource createNew (URI uri) {
        HashSet<String> flags = new HashSet<String>();
        flags.add("generic");
        HashMap<String,String> params = new HashMap<String,String>();
        return create(flags, params, uri);
    }

    public static GenericHttpsResource create(Set<String> flags, HashMap<String,String> params, URI uri) {
        if ( ! ("https".equals(uri.getScheme())
                && flags != null && flags.size() == 1 && flags.contains("generic")
                && (params == null || params.isEmpty()))) {
            return null;
        }
        return new GenericHttpsResource(flags, params, uri);
    }

}
