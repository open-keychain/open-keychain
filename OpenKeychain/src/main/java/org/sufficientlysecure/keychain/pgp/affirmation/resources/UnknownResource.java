package org.sufficientlysecure.keychain.pgp.affirmation.resources;

import org.sufficientlysecure.keychain.pgp.affirmation.AffirmationResource;

import java.net.URI;
import java.util.HashMap;
import java.util.Set;

public class UnknownResource extends AffirmationResource {

    public UnknownResource(Set<String> flags, HashMap<String,String> params, URI uri) {
        super(flags, params, uri);
    }

    @Override
    public boolean verify() {
        return false;
    }

}
