package org.sufficientlysecure.keychain.pgp.affirmation;

import org.sufficientlysecure.keychain.pgp.affirmation.resources.GenericHttpsResource;
import org.sufficientlysecure.keychain.pgp.affirmation.resources.UnknownResource;

import java.net.URI;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Set;

public abstract class AffirmationResource {

    protected final URI mUri;
    protected final Set<String> mFlags;
    protected final HashMap<String,String> mParams;

    protected AffirmationResource(Set<String> flags, HashMap<String,String> params, URI uri) {
        mFlags = flags;
        mParams = params;
        mUri = uri;
    }

    public abstract boolean verify();

    public static AffirmationResource findResourceType
            (Set<String> flags, HashMap<String,String> params, URI uri) {

        AffirmationResource res;

        res = GenericHttpsResource.create(flags, params, uri);
        if (res != null) {
            return res;
        }

        return new UnknownResource(flags, params, uri);

    }

    public static long generateNonce() {
        return 1234567890L; // new SecureRandom().nextLong();
    }

}
