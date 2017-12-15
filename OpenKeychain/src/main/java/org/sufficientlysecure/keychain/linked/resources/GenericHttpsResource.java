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

package org.sufficientlysecure.keychain.linked.resources;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import okhttp3.Request;
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

    @Override
    protected String fetchResource (Context context, OperationLog log, int indent)
            throws HttpStatusException, IOException {

        log.add(LogType.MSG_LV_FETCH, indent, mSubUri.toString());
        Request request = new Request.Builder()
                .url(mSubUri.toURL())
                .addHeader("User-Agent", "OpenKeychain")
                .build();
        return getResponseBody(request);

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
