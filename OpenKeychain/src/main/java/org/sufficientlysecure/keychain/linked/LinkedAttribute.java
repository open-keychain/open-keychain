package org.sufficientlysecure.keychain.linked;

import java.net.URI;

import android.content.Context;
import android.support.annotation.DrawableRes;

public class LinkedAttribute extends UriAttribute {

    public final LinkedResource mResource;

    protected LinkedAttribute(URI uri, LinkedResource resource) {
        super(uri);
        if (resource == null) {
            throw new AssertionError("resource must not be null in a LinkedIdentity!");
        }
        mResource = resource;
    }

    public @DrawableRes int getDisplayIcon() {
        return mResource.getDisplayIcon();
    }

    public String getDisplayTitle(Context context) {
        return mResource.getDisplayTitle(context);
    }

    public String getDisplayComment(Context context) {
        return mResource.getDisplayComment(context);
    }

}
