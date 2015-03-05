package org.sufficientlysecure.keychain.pgp.linked.resources;

import android.content.Context;

import com.textuality.keybase.lib.Search;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.linked.LinkedCookieResource;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

public class GenericHttpsResource extends LinkedCookieResource {

    GenericHttpsResource(Set<String> flags, HashMap<String,String> params, URI uri) {
        super(flags, params, uri);
    }

    public static String generateText (Context context, byte[] fingerprint, int nonce) {
        String cookie = LinkedCookieResource.generate(context, fingerprint, nonce);

        return String.format(context.getResources().getString(R.string.linked_id_generic_text),
                cookie, "0x" + KeyFormattingUtils.convertFingerprintToHex(fingerprint).substring(24));
    }

    @Override
    protected String fetchResource (OperationLog log, int indent) {

        log.add(LogType.MSG_LV_FETCH, indent, mSubUri.toString());
        indent += 1;

        try {

            HttpsURLConnection conn = null;
            URL url = mSubUri.toURL();
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
                    log.add(LogType.MSG_LV_FETCH_REDIR, indent, url.toString());
                } else {
                    break;
                }
            }

            if (status >= 200 && status < 300) {
                log.add(LogType.MSG_LV_FETCH_OK, indent, Integer.toString(status));
                return Search.snarf(conn.getInputStream());
            } else {
                // log verbose output to logcat
                Log.e(Constants.TAG, Search.snarf(conn.getErrorStream()));
                log.add(LogType.MSG_LV_FETCH_ERROR, indent, Integer.toString(status));
                return null;
            }

        } catch (MalformedURLException e) {
            log.add(LogType.MSG_LV_FETCH_ERROR_URL, indent);
            return null;
        } catch (IOException e) {
            log.add(LogType.MSG_LV_FETCH_ERROR_IO, indent);
            return null;
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
