package org.sufficientlysecure.keychain.pgp.linked;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.linked.resources.DnsResource;
import org.sufficientlysecure.keychain.pgp.linked.resources.GenericHttpsResource;
import org.sufficientlysecure.keychain.pgp.linked.resources.TwitterResource;
import org.sufficientlysecure.keychain.util.Log;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.DrawableRes;

public abstract class LinkedResource {

    protected final URI mSubUri;
    protected final Set<String> mFlags;
    protected final HashMap<String,String> mParams;

    static Pattern magicPattern =
            Pattern.compile("\\[Verifying my PGP key: openpgp4fpr:([a-zA-Z0-9]+)]");

    protected LinkedResource(Set<String> flags, HashMap<String, String> params, URI uri) {
        mFlags = flags;
        mParams = params;
        mSubUri = uri;
    }

    public Set<String> getFlags () {
        return new HashSet<>(mFlags);
    }

    public HashMap<String,String> getParams () {
        return new HashMap<>(mParams);
    }

    protected static LinkedCookieResource fromUri (URI uri) {

        if (!"pgpid+cookie".equals(uri.getScheme())) {
            Log.e(Constants.TAG, "unknown uri scheme in (suspected) linked id packet");
            return null;
        }

        if (!uri.isOpaque()) {
            Log.e(Constants.TAG, "non-opaque uri in (suspected) linked id packet");
            return null;
        }

        String specific = uri.getSchemeSpecificPart();
        if (!specific.contains("@")) {
            Log.e(Constants.TAG, "unknown uri scheme in linked id packet");
            return null;
        }

        String[] pieces = specific.split("@", 2);
        URI subUri = URI.create(pieces[1]);

        Set<String> flags = new HashSet<>();
        HashMap<String,String> params = new HashMap<>();
        if (!pieces[0].isEmpty()) {
            String[] rawParams = pieces[0].split(";");
            for (String param : rawParams) {
                String[] p = param.split("=", 2);
                if (p.length == 1) {
                    flags.add(param);
                } else {
                    params.put(p[0], p[1]);
                }
            }
        }

        return findResourceType(flags, params, subUri);

    }

    protected static LinkedCookieResource findResourceType (Set<String> flags,
                                                            HashMap<String,String> params,
                                                            URI  subUri) {

        LinkedCookieResource res;

        res = GenericHttpsResource.create(flags, params, subUri);
        if (res != null) {
            return res;
        }
        res = DnsResource.create(flags, params, subUri);
        if (res != null) {
            return res;
        }
        res = TwitterResource.create(flags, params, subUri);
        if (res != null) {
            return res;
        }

        return null;

    }

    public abstract @DrawableRes int getDisplayIcon();
    public abstract String getDisplayTitle(Context context);
    public abstract String getDisplayComment(Context context);
    public boolean isViewable() {
        return false;
    }
    public Intent getViewIntent() {
        return null;
    }

}
