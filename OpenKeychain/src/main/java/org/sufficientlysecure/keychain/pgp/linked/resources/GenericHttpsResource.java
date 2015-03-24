package org.sufficientlysecure.keychain.pgp.linked.resources;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import com.textuality.keybase.lib.Search;

import org.apache.http.client.methods.HttpGet;
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

    public static String generateText (Context context, byte[] fingerprint) {
        String cookie = LinkedCookieResource.generate(context, fingerprint);

        return String.format(context.getResources().getString(R.string.linked_id_generic_text),
                cookie, "0x" + KeyFormattingUtils.convertFingerprintToHex(fingerprint).substring(24));
    }

    @Override
    protected String fetchResource (OperationLog log, int indent) throws HttpStatusException, IOException {

        log.add(LogType.MSG_LV_FETCH, indent, mSubUri.toString());
        indent += 1;

        HttpGet httpGet = new HttpGet(mSubUri);
        return getResponseBody(httpGet);

        // log.add(LogType.MSG_LV_FETCH_REDIR, indent, url.toString());

    }

    public static GenericHttpsResource createNew (URI uri) {
        HashSet<String> flags = new HashSet<>();
        flags.add("generic");
        HashMap<String,String> params = new HashMap<>();
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

    @Override
    public @DrawableRes
    int getDisplayIcon() {
        return R.drawable.ssl_lock;
    }

    @Override
    public @StringRes
    int getVerifiedText() {
        return R.string.linked_verified_https;
    }

    @Override
    public String getDisplayTitle(Context context) {
        return "Website (HTTPS)";
    }

    @Override
    public String getDisplayComment(Context context) {
        return mSubUri.toString();
    }

    @Override
    public boolean isViewable() {
        return true;
    }

    @Override
    public Intent getViewIntent() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(mSubUri.toString()));
        return intent;
    }
}
