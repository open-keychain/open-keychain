package org.sufficientlysecure.keychain.linked.resources;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import org.apache.http.client.methods.HttpGet;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.linked.LinkedTokenResource;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class GenericHttpsResource extends LinkedTokenResource {

    GenericHttpsResource(Set<String> flags, HashMap<String,String> params, URI uri) {
        super(flags, params, uri);
    }

    public static String generateText (Context context, byte[] fingerprint) {
        String token = LinkedTokenResource.generate(fingerprint);

        return String.format(context.getResources().getString(R.string.linked_id_generic_text),
                token, "0x" + KeyFormattingUtils.convertFingerprintToHex(fingerprint).substring(24));
    }

    @SuppressWarnings("deprecation") // HttpGet is deprecated
    @Override
    protected String fetchResource (Context context, OperationLog log, int indent)
            throws HttpStatusException, IOException {

        log.add(LogType.MSG_LV_FETCH, indent, mSubUri.toString());
        HttpGet httpGet = new HttpGet(mSubUri);
        return getResponseBody(context, httpGet);

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
        return R.drawable.linked_https;
    }

    @Override
    public @StringRes
    int getVerifiedText(boolean isSecret) {
        return isSecret ? R.string.linked_verified_secret_https : R.string.linked_verified_https;
    }

    @Override
    public String getDisplayTitle(Context context) {
        return context.getString(R.string.linked_title_https);
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
